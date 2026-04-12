/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.loginpassword

import com.google.common.truth.Truth.assertThat
import io.element.android.appconfig.AuthenticationConfig
import io.element.android.features.enterprise.test.FakeEnterpriseService
import io.element.android.features.login.impl.accountprovider.AccountProviderDataSource
import io.element.android.features.login.impl.customauth.CustomAuthService
import io.element.android.features.login.impl.customauth.AvatarUploadAcceptance
import io.element.android.features.login.impl.customauth.CustomAuthLegalLinks
import io.element.android.features.login.impl.customauth.PasswordResetAcceptance
import io.element.android.features.login.impl.customauth.RegistrationStartAcceptance
import io.element.android.features.login.impl.customauth.RegistrationStatus
import io.element.android.features.login.impl.customauth.RegistrationVerifyAcceptance
import io.element.android.features.login.impl.customauth.defaultCustomAuthLegalLinks
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.matrix.api.auth.external.ExternalSession
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.test.AN_EXCEPTION
import io.element.android.libraries.matrix.test.A_PASSWORD
import io.element.android.libraries.matrix.test.A_SESSION_ID
import io.element.android.libraries.matrix.test.A_USER_NAME
import io.element.android.libraries.matrix.test.auth.FakeMatrixAuthenticationService
import io.element.android.libraries.matrix.test.auth.aMatrixHomeServerDetails
import io.element.android.tests.testutils.WarmUpRule
import io.element.android.tests.testutils.test
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class LoginPasswordPresenterTest {
    @get:Rule
    val warmUpRule = WarmUpRule()

    @Test
    fun `present - initial state`() = runTest {
        createLoginPasswordPresenter().test {
            val initialState = awaitItem()
            assertThat(initialState.accountProvider.url).isEqualTo(AuthenticationConfig.DEFAULT_HOMESERVER_URL)
            assertThat(initialState.formState).isEqualTo(LoginFormState.Default)
            assertThat(initialState.loginAction).isEqualTo(AsyncData.Uninitialized)
            assertThat(initialState.passwordResetAction).isEqualTo(AsyncData.Uninitialized)
            assertThat(initialState.forgotPasswordCooldownEndsAtEpochMillis).isEqualTo(0L)
            assertThat(initialState.submitEnabled).isFalse()
        }
    }

    @Test
    fun `present - enter login and password`() = runTest {
        val authenticationService = FakeMatrixAuthenticationService(
            setHomeserverResult = {
                Result.success(aMatrixHomeServerDetails())
            },
        )
        createLoginPasswordPresenter(
            authenticationService = authenticationService,
        ).test {
            val initialState = awaitItem()
            initialState.eventSink.invoke(LoginPasswordEvents.SetLogin(A_USER_NAME))
            val loginState = awaitItem()
            assertThat(loginState.formState).isEqualTo(LoginFormState(login = A_USER_NAME, password = ""))
            assertThat(loginState.submitEnabled).isFalse()
            initialState.eventSink.invoke(LoginPasswordEvents.SetPassword(A_PASSWORD))
            val loginAndPasswordState = awaitItem()
            assertThat(loginAndPasswordState.formState).isEqualTo(LoginFormState(login = A_USER_NAME, password = A_PASSWORD))
            assertThat(loginAndPasswordState.submitEnabled).isTrue()
        }
    }

    @Test
    fun `present - submit`() = runTest {
        val authenticationService = FakeMatrixAuthenticationService(
            setHomeserverResult = {
                Result.success(aMatrixHomeServerDetails())
            },
        )
        createLoginPasswordPresenter(
            authenticationService = authenticationService,
        ).test {
            val initialState = awaitItem()
            initialState.eventSink.invoke(LoginPasswordEvents.SetLogin(A_USER_NAME))
            initialState.eventSink.invoke(LoginPasswordEvents.SetPassword(A_PASSWORD))
            skipItems(1)
            val loginAndPasswordState = awaitItem()
            loginAndPasswordState.eventSink.invoke(LoginPasswordEvents.Submit)
            val submitState = awaitItem()
            assertThat(submitState.loginAction).isInstanceOf(AsyncData.Loading::class.java)
            val loggedInState = awaitItem()
            assertThat(loggedInState.loginAction).isEqualTo(AsyncData.Success(A_SESSION_ID))
        }
    }

    @Test
    fun `present - submit with error`() = runTest {
        val authenticationService = FakeMatrixAuthenticationService(
            setHomeserverResult = {
                Result.success(aMatrixHomeServerDetails())
            },
        )
        createLoginPasswordPresenter(
            authenticationService = authenticationService,
        ).test {
            val initialState = awaitItem()
            initialState.eventSink.invoke(LoginPasswordEvents.SetLogin(A_USER_NAME))
            initialState.eventSink.invoke(LoginPasswordEvents.SetPassword(A_PASSWORD))
            skipItems(1)
            val loginAndPasswordState = awaitItem()
            authenticationService.givenLoginError(AN_EXCEPTION)
            loginAndPasswordState.eventSink.invoke(LoginPasswordEvents.Submit)
            val submitState = awaitItem()
            assertThat(submitState.loginAction).isInstanceOf(AsyncData.Loading::class.java)
            val loggedInState = awaitItem()
            assertThat(loggedInState.loginAction).isEqualTo(AsyncData.Failure<SessionId>(AN_EXCEPTION))
        }
    }

    @Test
    fun `present - clear error`() = runTest {
        val authenticationService = FakeMatrixAuthenticationService(
            setHomeserverResult = {
                Result.success(aMatrixHomeServerDetails())
            },
        )
        createLoginPasswordPresenter(
            authenticationService = authenticationService,
        ).test {
            val initialState = awaitItem()
            initialState.eventSink.invoke(LoginPasswordEvents.SetLogin(A_USER_NAME))
            initialState.eventSink.invoke(LoginPasswordEvents.SetPassword(A_PASSWORD))
            skipItems(1)
            val loginAndPasswordState = awaitItem()
            authenticationService.givenLoginError(AN_EXCEPTION)
            loginAndPasswordState.eventSink.invoke(LoginPasswordEvents.Submit)
            val submitState = awaitItem()
            assertThat(submitState.loginAction).isInstanceOf(AsyncData.Loading::class.java)
            val loggedInState = awaitItem()
            // Check an error was returned
            assertThat(loggedInState.loginAction).isEqualTo(AsyncData.Failure<SessionId>(AN_EXCEPTION))
            // Assert the error is then cleared
            loggedInState.eventSink(LoginPasswordEvents.ClearError)
            val clearedState = awaitItem()
            assertThat(clearedState.loginAction).isEqualTo(AsyncData.Uninitialized)
        }
    }

    @Test
    fun `present - submit uses custom auth for managed homeserver`() = runTest {
        val customAuthService = FakeCustomAuthService(
            managedHomeserverPredicate = { true },
            loginByIdentifierResult = Result.success(aCustomExternalSession()),
        )
        val authenticationService = FakeMatrixAuthenticationService(
            importCreatedSessionLambda = { Result.success(A_SESSION_ID) },
            setHomeserverResult = {
                Result.success(aMatrixHomeServerDetails())
            },
        )
        createLoginPasswordPresenter(
            authenticationService = authenticationService,
            customAuthService = customAuthService,
        ).test {
            val initialState = awaitItem()
            initialState.eventSink.invoke(LoginPasswordEvents.SetLogin(A_USER_NAME))
            initialState.eventSink.invoke(LoginPasswordEvents.SetPassword(A_PASSWORD))
            skipItems(1)
            val loginAndPasswordState = awaitItem()
            loginAndPasswordState.eventSink.invoke(LoginPasswordEvents.Submit)
            val submitState = awaitItem()
            assertThat(submitState.loginAction).isInstanceOf(AsyncData.Loading::class.java)
            val loggedInState = awaitItem()
            assertThat(loggedInState.loginAction).isEqualTo(AsyncData.Success(A_SESSION_ID))
            assertThat(customAuthService.loginByIdentifierCalls).isEqualTo(1)
        }
    }

    @Test
    fun `present - submit falls back to legacy login when custom auth fails`() = runTest {
        val customAuthService = FakeCustomAuthService(
            managedHomeserverPredicate = { true },
            loginByIdentifierResult = Result.failure(AN_EXCEPTION),
        )
        createLoginPasswordPresenter(
            customAuthService = customAuthService,
        ).test {
            val initialState = awaitItem()
            initialState.eventSink.invoke(LoginPasswordEvents.SetLogin(A_USER_NAME))
            initialState.eventSink.invoke(LoginPasswordEvents.SetPassword(A_PASSWORD))
            skipItems(1)
            val loginAndPasswordState = awaitItem()
            loginAndPasswordState.eventSink.invoke(LoginPasswordEvents.Submit)
            val submitState = awaitItem()
            assertThat(submitState.loginAction).isInstanceOf(AsyncData.Loading::class.java)
            val loggedInState = awaitItem()
            assertThat(loggedInState.loginAction).isInstanceOf(AsyncData.Success::class.java)
            assertThat(customAuthService.loginByIdentifierCalls).isEqualTo(1)
        }
    }

    @Test
    fun `present - request password reset returns uniform success`() = runTest {
        val customAuthService = FakeCustomAuthService(
            managedHomeserverPredicate = { true },
            requestPasswordResetResult = Result.failure(AN_EXCEPTION),
        )
        createLoginPasswordPresenter(
            customAuthService = customAuthService,
        ).test {
            val initialState = awaitItem()
            initialState.eventSink.invoke(LoginPasswordEvents.SetLogin(A_USER_NAME))
            val loginState = awaitItem()
            loginState.eventSink.invoke(LoginPasswordEvents.RequestPasswordReset)
            val stateAfterRequest = awaitItem()
            val successState = if (stateAfterRequest.passwordResetAction is AsyncData.Loading) {
                awaitItem()
            } else {
                stateAfterRequest
            }
            assertThat(successState.passwordResetAction).isEqualTo(AsyncData.Success(Unit))
            assertThat(customAuthService.requestPasswordResetCalls).isEqualTo(1)
            assertThat(successState.forgotPasswordCooldownEndsAtEpochMillis).isGreaterThan(0L)
            assertThat(successState.forgotPasswordEnabled).isFalse()
            successState.eventSink.invoke(LoginPasswordEvents.ClearPasswordResetNotice)
            val clearedState = awaitItem()
            assertThat(clearedState.passwordResetAction).isEqualTo(AsyncData.Uninitialized)
        }
    }

    @Test
    fun `present - request password reset in cooldown is ignored`() = runTest {
        val customAuthService = FakeCustomAuthService(
            managedHomeserverPredicate = { true },
        )
        createLoginPasswordPresenter(
            customAuthService = customAuthService,
        ).test {
            val initialState = awaitItem()
            initialState.eventSink.invoke(LoginPasswordEvents.SetLogin(A_USER_NAME))
            val loginState = awaitItem()
            loginState.eventSink.invoke(LoginPasswordEvents.RequestPasswordReset)
            val stateAfterRequest = awaitItem()
            val successState = if (stateAfterRequest.passwordResetAction is AsyncData.Loading) {
                awaitItem()
            } else {
                stateAfterRequest
            }
            assertThat(customAuthService.requestPasswordResetCalls).isEqualTo(1)

            successState.eventSink.invoke(LoginPasswordEvents.RequestPasswordReset)
            expectNoEvents()
            assertThat(customAuthService.requestPasswordResetCalls).isEqualTo(1)
        }
    }

    private fun createLoginPasswordPresenter(
        authenticationService: FakeMatrixAuthenticationService = FakeMatrixAuthenticationService(),
        accountProviderDataSource: AccountProviderDataSource = AccountProviderDataSource(FakeEnterpriseService()),
        customAuthService: CustomAuthService = FakeCustomAuthService(),
    ): LoginPasswordPresenter = LoginPasswordPresenter(
        authenticationService = authenticationService,
        accountProviderDataSource = accountProviderDataSource,
        customAuthService = customAuthService,
    )
}

private fun aCustomExternalSession(): ExternalSession = ExternalSession(
    userId = "@user:matrix.ibessonniy.ru",
    deviceId = "DEVICE_ID",
    accessToken = "ACCESS_TOKEN",
    refreshToken = null,
    homeserverUrl = AuthenticationConfig.DEFAULT_HOMESERVER_URL,
)

private class FakeCustomAuthService(
    private val managedHomeserverPredicate: (String) -> Boolean = { false },
    private val loginByIdentifierResult: Result<ExternalSession> = Result.failure(IllegalStateException("not configured")),
    private val requestPasswordResetResult: Result<PasswordResetAcceptance> = Result.success(
        PasswordResetAcceptance(accepted = true, retryAfterSeconds = 120),
    ),
) : CustomAuthService {
    var loginByIdentifierCalls: Int = 0
        private set
    var requestPasswordResetCalls: Int = 0
        private set

    override fun isManagedHomeserver(homeserverUrl: String): Boolean = managedHomeserverPredicate(homeserverUrl)

    override suspend fun loginByIdentifier(
        homeserverUrl: String,
        identifier: String,
        password: String,
    ): Result<ExternalSession> {
        loginByIdentifierCalls++
        return loginByIdentifierResult
    }

    override suspend fun requestPasswordReset(
        homeserverUrl: String,
        identifier: String,
    ): Result<PasswordResetAcceptance> {
        requestPasswordResetCalls++
        return requestPasswordResetResult
    }

    override suspend fun startRegistration(
        homeserverUrl: String,
        login: String,
        email: String,
    ): Result<RegistrationStartAcceptance> {
        return Result.failure(IllegalStateException("not used in this test"))
    }

    override suspend fun verifyRegistrationPassword(
        registrationSessionId: String,
        password: String,
    ): Result<RegistrationVerifyAcceptance> {
        return Result.failure(IllegalStateException("not used in this test"))
    }

    override suspend fun getRegistrationStatus(registrationSessionId: String): Result<RegistrationStatus> {
        return Result.failure(IllegalStateException("not used in this test"))
    }

    override suspend fun completeRegistration(
        homeserverUrl: String,
        verifiedToken: String,
        displayName: String,
        phone: String,
        avatarUploadRef: String?,
    ): Result<ExternalSession> {
        return Result.failure(IllegalStateException("not used in this test"))
    }

    override suspend fun uploadRegistrationAvatar(
        registrationSessionId: String,
        contentType: String,
        contentBase64: String,
    ): Result<AvatarUploadAcceptance> {
        return Result.failure(IllegalStateException("not used in this test"))
    }

    override suspend fun getLegalLinks(homeserverUrl: String): CustomAuthLegalLinks {
        return defaultCustomAuthLegalLinks()
    }
}
