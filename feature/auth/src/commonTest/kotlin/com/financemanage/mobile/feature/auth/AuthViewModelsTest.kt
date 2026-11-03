package com.financemanage.mobile.feature.auth

import com.financemanage.mobile.core.data.session.SessionState
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.ErrorCodes
import com.financemanage.mobile.core.network.auth.AuthTokens
import com.financemanage.mobile.core.testing.MockBackend
import com.financemanage.mobile.core.testing.SampleJson
import com.financemanage.mobile.core.testing.awaitValue
import com.financemanage.mobile.core.testing.bodyField
import com.financemanage.mobile.core.testing.runViewModelTest
import com.financemanage.mobile.core.testing.testSessionManager
import com.financemanage.mobile.feature.auth.data.AuthRepositoryImpl
import com.financemanage.mobile.feature.auth.domain.AuthRules
import com.financemanage.mobile.feature.auth.presentation.FieldProblem
import com.financemanage.mobile.feature.auth.presentation.ForgotPasswordViewModel
import com.financemanage.mobile.feature.auth.presentation.SignInEvent
import com.financemanage.mobile.feature.auth.presentation.SignInViewModel
import com.financemanage.mobile.feature.auth.presentation.SignUpViewModel
import com.financemanage.mobile.feature.auth.presentation.TwoFactorViewModel
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthViewModelsTest {
    private val backend = MockBackend()

    private fun TestScope.repository() =
        AuthRepositoryImpl(backend.client, testSessionManager(backend, backgroundScope))

    @Test
    fun rulesMatchTheApi() {
        assertTrue(AuthRules.isEmailValid(" an@example.com "))
        assertFalse(AuthRules.isEmailValid("an@example"))
        assertFalse(AuthRules.isEmailValid("@example.com"))
        assertFalse(AuthRules.isEmailValid("an@example."))
        assertFalse(AuthRules.isPasswordValid("1234567"))
        assertTrue(AuthRules.isPasswordValid("12345678"))
        assertFalse(AuthRules.isPasswordValid("x".repeat(73)))
    }

    @Test
    fun signInValidatesThenStoresTheTokens() =
        runViewModelTest {
            backend.post("auth/login", SampleJson.TOKEN_PAIR)
            val viewModel = SignInViewModel(repository())

            viewModel.onEmailChange("an@")
            viewModel.signIn()
            assertEquals(FieldProblem.EMAIL_INVALID, viewModel.state.value.emailProblem)
            assertEquals(FieldProblem.REQUIRED, viewModel.state.value.passwordProblem)

            viewModel.onEmailChange(" doc9074@example.com ")
            viewModel.onPasswordChange("Secret123")
            viewModel.signIn()
            viewModel.state.awaitValue { !it.loading && backend.tokenStore.tokens.value != null }

            val request = backend.single(HttpMethod.Post, "auth/login")
            assertEquals("doc9074@example.com", request.bodyField("email"))
            assertEquals("Secret123", request.bodyField("password"))
            assertEquals(
                "4XBcRuJiTY30RjWSKpZBRqlFck33YbYRBtCIe-0KBF4",
                backend.tokenStore.tokens.value
                    ?.refreshToken,
            )
            assertNull(viewModel.state.value.error)
        }

    @Test
    fun signInWithTwoFactorEmitsTheChallenge() =
        runViewModelTest {
            backend.post("auth/login", SampleJson.LOGIN_TWO_FACTOR_REQUIRED)
            val viewModel = SignInViewModel(repository())
            viewModel.onEmailChange("an@example.com")
            viewModel.onPasswordChange("Secret123")
            viewModel.signIn()

            assertEquals(SignInEvent.TwoFactorRequired("challenge-1", 300), viewModel.events.first())
            assertNull(backend.tokenStore.tokens.value)
        }

    @Test
    fun wrongPasswordAndCancelledGoogleAreReported() =
        runViewModelTest {
            backend.fail(HttpMethod.Post, "auth/login", HttpStatusCode.Unauthorized, ErrorCodes.INVALID_CREDENTIALS)
            val viewModel = SignInViewModel(repository())
            viewModel.onEmailChange("an@example.com")
            viewModel.onPasswordChange("wrong")
            viewModel.signIn()
            val failed = viewModel.state.awaitValue { it.error != null }
            assertEquals(ErrorCodes.INVALID_CREDENTIALS, (failed.error as AppError.Api).code)

            viewModel.signInWithGoogle(null)
            assertTrue(viewModel.state.value.googleFailed)
        }

    @Test
    fun googleSignInSendsTheIdToken() =
        runViewModelTest {
            backend.post("auth/google", SampleJson.TOKEN_PAIR)
            val viewModel = SignInViewModel(repository())
            viewModel.signInWithGoogle("google-id-token")
            viewModel.state.awaitValue { !it.loading && backend.tokenStore.tokens.value != null }
            assertEquals("google-id-token", backend.single(HttpMethod.Post, "auth/google").bodyField("idToken"))
        }

    @Test
    fun signUpChecksEveryFieldAndSignsIn() =
        runViewModelTest {
            backend.post("auth/register", SampleJson.TOKEN_PAIR, HttpStatusCode.Created)
            val session = testSessionManager(backend, backgroundScope)
            val viewModel = SignUpViewModel(AuthRepositoryImpl(backend.client, session))

            viewModel.onPasswordChange("short")
            viewModel.signUp()
            with(viewModel.state.value) {
                assertEquals(FieldProblem.REQUIRED, nameProblem)
                assertEquals(FieldProblem.EMAIL_INVALID, emailProblem)
                assertEquals(FieldProblem.PASSWORD_LENGTH, passwordProblem)
            }

            viewModel.onNameChange(" Nguyễn Văn A ")
            viewModel.onEmailChange("a@example.com")
            viewModel.onPasswordChange("Secret123")
            viewModel.signUp()
            session.state.awaitValue { it == SessionState.SIGNED_IN }
            assertEquals("Nguyễn Văn A", backend.single(HttpMethod.Post, "auth/register").bodyField("fullName"))
        }

    @Test
    fun twoFactorClearsAWrongCodeAndExpires() =
        runViewModelTest {
            backend.fail(HttpMethod.Post, "auth/2fa/verify", HttpStatusCode.Unauthorized, ErrorCodes.INVALID_CODE)
            val viewModel = TwoFactorViewModel(repository(), "challenge-1", expiresInSeconds = 300)

            viewModel.verify()
            assertTrue(backend.requests.isEmpty())

            viewModel.onCodeChange(" 123456 ")
            viewModel.verify()
            val failed = viewModel.state.awaitValue { it.error != null }
            assertEquals("", failed.code)
            val request = backend.single(HttpMethod.Post, "auth/2fa/verify")
            assertEquals("123456", request.bodyField("code"))
            assertEquals("challenge-1", request.bodyField("challengeToken"))

            viewModel.toggleRecoveryCode()
            assertTrue(viewModel.state.value.useRecoveryCode)
            assertNull(viewModel.state.value.error)

            advanceTimeBy(301_000)
            assertTrue(viewModel.state.value.expired)
            viewModel.onCodeChange("ABCD-EFGH")
            viewModel.verify()
            assertEquals(1, backend.requests.size)
        }

    @Test
    fun twoFactorSuccessSignsIn() =
        runViewModelTest {
            backend.post("auth/2fa/verify", SampleJson.TOKEN_PAIR)
            val viewModel = TwoFactorViewModel(repository(), "challenge-1", expiresInSeconds = 300)
            viewModel.onCodeChange("123456")
            viewModel.verify()
            viewModel.state.awaitValue { !it.loading && backend.tokenStore.tokens.value != null }
            assertEquals(
                AuthTokens("eyJhbGciOiJIUzI1NiJ9.test", "4XBcRuJiTY30RjWSKpZBRqlFck33YbYRBtCIe-0KBF4"),
                backend.tokenStore.tokens.value,
            )
        }

    @Test
    fun forgotPasswordSendsACodeThenResets() =
        runViewModelTest {
            backend.post("auth/forgot-password", "", HttpStatusCode.Accepted)
            backend.post("auth/reset-password", "", HttpStatusCode.NoContent)
            val viewModel = ForgotPasswordViewModel(repository())

            viewModel.onEmailChange("bad")
            viewModel.sendCode()
            assertEquals(FieldProblem.EMAIL_INVALID, viewModel.state.value.emailProblem)

            viewModel.onEmailChange("a@example.com")
            viewModel.sendCode()
            viewModel.state.awaitValue { it.codeSent }

            viewModel.onNewPasswordChange("NewSecret1")
            viewModel.onCodeChange("123")
            viewModel.reset()
            assertEquals(1, backend.requests.size)

            viewModel.onCodeChange("123456")
            viewModel.reset()
            viewModel.state.awaitValue { it.done }
            val reset = backend.single(HttpMethod.Post, "auth/reset-password")
            assertEquals("a@example.com", reset.bodyField("email"))
            assertEquals("NewSecret1", reset.bodyField("newPassword"))
        }
}
