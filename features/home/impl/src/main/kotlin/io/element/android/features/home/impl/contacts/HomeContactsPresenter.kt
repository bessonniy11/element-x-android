/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.contacts

import android.Manifest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Inject
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.permissions.api.PermissionStateProvider
import io.element.android.libraries.usersearch.api.PhonebookMatrixContactsProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

@Inject
class HomeContactsPresenter(
    private val phonebookMatrixContactsProvider: PhonebookMatrixContactsProvider,
    private val permissionStateProvider: PermissionStateProvider,
    private val coroutineDispatchers: CoroutineDispatchers,
) : Presenter<HomeContactsState> {
    @Composable
    override fun present(): HomeContactsState {
        var hasOpenedContactsTab by rememberSaveable { mutableStateOf(false) }
        var refreshToken by rememberSaveable { mutableIntStateOf(0) }
        var hasContactsPermission by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }
        var contacts: ImmutableList<HomeContact> by remember { mutableStateOf(persistentListOf()) }

        LaunchedEffect(hasOpenedContactsTab, refreshToken) {
            if (!hasOpenedContactsTab) {
                contacts = persistentListOf()
                isLoading = false
                return@LaunchedEffect
            }

            hasContactsPermission = permissionStateProvider.isPermissionGranted(Manifest.permission.READ_CONTACTS)
            if (!hasContactsPermission) {
                contacts = persistentListOf()
                isLoading = false
                return@LaunchedEffect
            }

            isLoading = true
            try {
                contacts = withContext(coroutineDispatchers.io) {
                    phonebookMatrixContactsProvider.getPhonebookContacts()
                        .map { contact ->
                            HomeContact(
                                displayName = contact.displayName,
                                phoneNumber = contact.phoneNumber,
                                matrixUser = contact.matrixUser,
                            )
                        }
                        .toImmutableList()
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Throwable) {
                contacts = persistentListOf()
            } finally {
                isLoading = false
            }
        }

        fun handleEvent(event: HomeContactsEvents) {
            when (event) {
                HomeContactsEvents.ScreenOpened -> {
                    hasOpenedContactsTab = true
                    refreshToken++
                }
                HomeContactsEvents.Refresh -> {
                    hasOpenedContactsTab = true
                    refreshToken++
                }
            }
        }

        return HomeContactsState(
            hasContactsPermission = hasContactsPermission,
            isLoading = isLoading,
            contacts = contacts,
            eventSink = ::handleEvent,
        )
    }
}
