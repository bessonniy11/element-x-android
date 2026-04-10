/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.registrationprofile

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.element.android.features.login.impl.accountprovider.AccountProviderDataSource
import io.element.android.features.login.impl.customauth.CustomAuthService
import io.element.android.features.login.impl.registrationdraft.RegistrationDraftStep
import io.element.android.features.login.impl.registrationdraft.RegistrationDraftStore
import io.element.android.libraries.androidutils.file.getMimeType
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.di.annotations.ApplicationContext
import io.element.android.libraries.matrix.api.auth.MatrixAuthenticationService
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.mediapickers.api.PickerProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AssistedInject
class RegistrationProfilePresenter(
    @Assisted private val verifiedToken: String,
    @Assisted private val email: String,
    private val accountProviderDataSource: AccountProviderDataSource,
    private val customAuthService: CustomAuthService,
    private val registrationDraftStore: RegistrationDraftStore,
    private val authenticationService: MatrixAuthenticationService,
    private val mediaPickerProvider: PickerProvider,
    @ApplicationContext private val context: Context,
) : Presenter<RegistrationProfileState> {
    @AssistedFactory
    interface Factory {
        fun create(
            verifiedToken: String,
            email: String,
        ): RegistrationProfilePresenter
    }

    @Composable
    override fun present(): RegistrationProfileState {
        val localCoroutineScope = rememberCoroutineScope()
        val completeAction: MutableState<AsyncData<SessionId>> = remember {
            mutableStateOf(AsyncData.Uninitialized)
        }
        val avatarUploadAction: MutableState<AsyncData<Unit>> = remember {
            mutableStateOf(AsyncData.Uninitialized)
        }
        val draft = registrationDraftStore.draftFlow().collectAsState(initial = null).value
        var displayName by rememberSaveable(draft?.registrationSessionId) {
            mutableStateOf(draft?.displayName.orEmpty())
        }
        var phone by rememberSaveable(draft?.registrationSessionId) {
            mutableStateOf(formatRuPhoneInput(draft?.phone.orEmpty()))
        }
        var avatarUploadRef by rememberSaveable(draft?.registrationSessionId) {
            mutableStateOf(draft?.avatarUploadRef)
        }
        val accountProvider by accountProviderDataSource.flow.collectAsState()

        val galleryImagePicker = mediaPickerProvider.registerGalleryImagePicker { selectedUri ->
            if (selectedUri != null) {
                localCoroutineScope.uploadAvatar(
                    registrationSessionId = draft?.registrationSessionId,
                    avatarUri = selectedUri,
                    avatarUploadAction = avatarUploadAction,
                ) { uploadedRef ->
                    avatarUploadRef = uploadedRef
                    localCoroutineScope.launch {
                        saveDraftProfileData(
                            avatarUploadRef = uploadedRef,
                            shouldUpdateAvatarUploadRef = true,
                        )
                    }
                }
            }
        }

        fun handleEvent(event: RegistrationProfileEvents) {
            when (event) {
                is RegistrationProfileEvents.SetDisplayName -> {
                    displayName = event.displayName
                    localCoroutineScope.launch {
                        saveDraftProfileData(displayName = event.displayName)
                    }
                }
                is RegistrationProfileEvents.SetPhone -> {
                    phone = event.phone
                    localCoroutineScope.launch {
                        saveDraftProfileData(phone = event.phone)
                    }
                }
                RegistrationProfileEvents.PickAvatar -> {
                    galleryImagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
                RegistrationProfileEvents.RemoveAvatar -> {
                    avatarUploadRef = null
                    avatarUploadAction.value = AsyncData.Uninitialized
                    localCoroutineScope.launch {
                        saveDraftProfileData(
                            avatarUploadRef = null,
                            shouldUpdateAvatarUploadRef = true,
                        )
                    }
                }
                RegistrationProfileEvents.ClearAvatarError -> {
                    avatarUploadAction.value = AsyncData.Uninitialized
                }
                RegistrationProfileEvents.Submit -> {
                    val normalizedPhone = normalizeRuPhoneForBackend(phone) ?: return
                    localCoroutineScope.completeRegistration(
                        homeserverUrl = accountProvider.url,
                        verifiedToken = verifiedToken,
                        displayName = displayName.trim(),
                        phone = normalizedPhone,
                        avatarUploadRef = avatarUploadRef,
                        completeAction = completeAction,
                    )
                }
                RegistrationProfileEvents.ClearError -> {
                    completeAction.value = AsyncData.Uninitialized
                }
            }
        }

        return RegistrationProfileState(
            accountProvider = accountProvider,
            email = email,
            displayName = displayName,
            phone = phone,
            hasAvatar = !avatarUploadRef.isNullOrBlank(),
            avatarUploadAction = avatarUploadAction.value,
            completeAction = completeAction.value,
            eventSink = ::handleEvent,
        )
    }

    private fun CoroutineScope.uploadAvatar(
        registrationSessionId: String?,
        avatarUri: Uri,
        avatarUploadAction: MutableState<AsyncData<Unit>>,
        onAvatarUploaded: (String) -> Unit,
    ) = launch {
        val sessionId = registrationSessionId?.trim().orEmpty()
        if (sessionId.isBlank()) {
            avatarUploadAction.value = AsyncData.Failure(IllegalStateException("registration session is missing"))
            return@launch
        }

        avatarUploadAction.value = AsyncData.Loading()
        val bytes = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(avatarUri)?.use { it.readBytes() }
        } ?: run {
            avatarUploadAction.value = AsyncData.Failure(IllegalArgumentException("avatar is empty"))
            return@launch
        }
        if (bytes.isEmpty()) {
            avatarUploadAction.value = AsyncData.Failure(IllegalArgumentException("avatar is empty"))
            return@launch
        }

        val mimeType = context.getMimeType(avatarUri)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
        val contentBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

        customAuthService.uploadRegistrationAvatar(
            registrationSessionId = sessionId,
            contentType = mimeType,
            contentBase64 = contentBase64,
        ).onSuccess { uploadAcceptance ->
            onAvatarUploaded(uploadAcceptance.avatarUploadRef)
            avatarUploadAction.value = AsyncData.Success(Unit)
        }.onFailure { failure ->
            avatarUploadAction.value = AsyncData.Failure(failure)
        }
    }

    private fun CoroutineScope.completeRegistration(
        homeserverUrl: String,
        verifiedToken: String,
        displayName: String,
        phone: String,
        avatarUploadRef: String?,
        completeAction: MutableState<AsyncData<SessionId>>,
    ) = launch {
        if (displayName.isBlank() || phone.isBlank()) return@launch
        completeAction.value = AsyncData.Loading()
        val result = customAuthService.completeRegistration(
            homeserverUrl = homeserverUrl,
            verifiedToken = verifiedToken,
            displayName = displayName,
            phone = phone,
            avatarUploadRef = avatarUploadRef,
        ).fold(
            onSuccess = { externalSession ->
                authenticationService.importCreatedSession(externalSession)
            },
            onFailure = { throwable ->
                Result.failure<SessionId>(throwable)
            }
        )

        result.onSuccess { sessionId ->
            registrationDraftStore.clear()
            completeAction.value = AsyncData.Success(sessionId)
        }.onFailure { failure ->
            completeAction.value = AsyncData.Failure(failure)
        }
    }

    private suspend fun saveDraftProfileData(
        displayName: String? = null,
        phone: String? = null,
        avatarUploadRef: String? = null,
        shouldUpdateAvatarUploadRef: Boolean = false,
    ) {
        val currentDraft = registrationDraftStore.draftFlow().firstOrNull() ?: return
        if (currentDraft.step != RegistrationDraftStep.Profile) return
        registrationDraftStore.saveDraft(
            currentDraft.copy(
                displayName = displayName ?: currentDraft.displayName,
                phone = phone ?: currentDraft.phone,
                avatarUploadRef = if (shouldUpdateAvatarUploadRef) {
                    avatarUploadRef
                } else {
                    currentDraft.avatarUploadRef
                },
            )
        )
    }
}
