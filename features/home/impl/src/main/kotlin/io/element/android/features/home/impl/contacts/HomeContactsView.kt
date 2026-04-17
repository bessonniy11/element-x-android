/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.contacts

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.home.impl.R
import io.element.android.libraries.designsystem.components.async.AsyncLoading
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.ButtonSize
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.SearchField
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun HomeContactsView(
    state: HomeContactsState,
    lazyListState: LazyListState,
    contentPadding: PaddingValues,
    onInviteClick: (String) -> Unit,
    onCallClick: (String) -> Unit,
    onMessageClick: (UserId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        state.eventSink(HomeContactsEvents.Refresh)
    }
    var autoPermissionRequestDone by rememberSaveable { mutableStateOf(false) }
    val searchQuery = rememberTextFieldState()
    val queryText = searchQuery.text.toString().trim()
    val filteredContacts = remember(state.contacts, queryText) {
        if (queryText.isEmpty()) {
            state.contacts
        } else {
            state.contacts.filter { it.matchesQuery(queryText) }
        }
    }

    LaunchedEffect(state.hasContactsPermission) {
        if (!state.hasContactsPermission && !autoPermissionRequestDone) {
            autoPermissionRequestDone = true
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    when {
        !state.hasContactsPermission -> {
            HomeContactsPermissionView(
                onRequestPermission = {
                    contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                },
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }
        state.isLoading -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                AsyncLoading()
            }
        }
        else -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                SearchField(
                    state = searchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    placeholder = stringResource(CommonStrings.action_search),
                )
                Box(modifier = Modifier.fillMaxSize()) {
                    if (filteredContacts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.screen_contacts_empty_title),
                                style = ElementTheme.typography.fontBodyLgRegular,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = lazyListState,
                        ) {
                            itemsIndexed(filteredContacts, key = { _, item -> item.key }) { index, contact ->
                                ContactRow(
                                    contact = contact,
                                    onInviteClick = onInviteClick,
                                    onCallClick = onCallClick,
                                    onMessageClick = onMessageClick,
                                )
                                if (index < filteredContacts.lastIndex) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: HomeContact,
    onInviteClick: (String) -> Unit,
    onCallClick: (String) -> Unit,
    onMessageClick: (UserId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            avatarData = AvatarData(
                id = contact.phoneNumber,
                name = contact.displayName,
                size = AvatarSize.UserListItem,
            ),
            avatarType = AvatarType.User,
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        ) {
            Text(
                text = contact.displayName,
                style = ElementTheme.typography.fontBodyLgRegular,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtext = if (contact.isInMatrix && contact.matrixUser != null) {
                "${contact.phoneNumber} | ${contact.matrixUser.userId.value}"
            } else {
                contact.phoneNumber
            }
            Text(
                text = subtext,
                style = ElementTheme.typography.fontBodySmRegular,
                color = ElementTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (contact.isInMatrix && contact.matrixUser != null) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.screen_contacts_has_app_badge),
                    style = ElementTheme.typography.fontBodySmMedium,
                    color = ElementTheme.colors.textSuccessPrimary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { onCallClick(contact.phoneNumber) }) {
                        Icon(
                            imageVector = CompoundIcons.VoiceCall(),
                            contentDescription = stringResource(CommonStrings.action_call),
                            tint = ElementTheme.colors.iconPrimary,
                        )
                    }
                    IconButton(onClick = { onMessageClick(contact.matrixUser.userId) }) {
                        Icon(
                            imageVector = CompoundIcons.Chat(),
                            contentDescription = stringResource(CommonStrings.action_message),
                            tint = ElementTheme.colors.iconPrimary,
                        )
                    }
                }
            }
        } else {
            TextButton(
                text = stringResource(CommonStrings.action_invite),
                size = ButtonSize.Small,
                onClick = { onInviteClick(contact.phoneNumber) },
            )
        }
    }
}

@Composable
private fun HomeContactsPermissionView(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.screen_contacts_permission_message),
            style = ElementTheme.typography.fontBodyLgRegular,
            textAlign = TextAlign.Center,
        )
        Button(
            text = stringResource(R.string.screen_contacts_permission_cta),
            onClick = onRequestPermission,
        )
    }
}

private fun HomeContact.matchesQuery(query: String): Boolean {
    if (query.isBlank()) return true
    val queryDigits = query.filter(Char::isDigit)
    val phoneDigits = phoneNumber.filter(Char::isDigit)
    return displayName.contains(query, ignoreCase = true) ||
        phoneNumber.contains(query, ignoreCase = true) ||
        (queryDigits.isNotEmpty() && phoneDigits.contains(queryDigits)) ||
        matrixUser?.displayName?.contains(query, ignoreCase = true) == true ||
        matrixUser?.userId?.value?.contains(query, ignoreCase = true) == true
}

@PreviewsDayNight
@Composable
internal fun HomeContactsViewPreview(
    @PreviewParameter(HomeContactsStateProvider::class) state: HomeContactsState,
) = ElementPreview {
    HomeContactsView(
        state = state,
        lazyListState = rememberLazyListState(),
        contentPadding = PaddingValues(bottom = 112.dp),
        onInviteClick = {},
        onCallClick = {},
        onMessageClick = {},
    )
}

