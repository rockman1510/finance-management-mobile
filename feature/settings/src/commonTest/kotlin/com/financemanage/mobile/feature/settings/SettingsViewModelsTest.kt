package com.financemanage.mobile.feature.settings

import com.financemanage.mobile.core.data.outbox.OutboxEntity
import com.financemanage.mobile.core.data.outbox.OutboxRepositoryImpl
import com.financemanage.mobile.core.data.outbox.OutboxSync
import com.financemanage.mobile.core.data.settings.AppSettingsRepositoryImpl
import com.financemanage.mobile.core.data.shared.ProfileRepositoryImpl
import com.financemanage.mobile.core.domain.model.SessionState
import com.financemanage.mobile.core.domain.model.ThemeMode
import com.financemanage.mobile.core.domain.usecase.GetCachedProfileUseCase
import com.financemanage.mobile.core.domain.usecase.GetProfileUseCase
import com.financemanage.mobile.core.domain.usecase.HideTwoFactorCardUseCase
import com.financemanage.mobile.core.domain.usecase.ObservePendingWritesUseCase
import com.financemanage.mobile.core.domain.usecase.ObservePreferencesUseCase
import com.financemanage.mobile.core.domain.usecase.ObserveProfileUseCase
import com.financemanage.mobile.core.domain.usecase.RefreshProfileUseCase
import com.financemanage.mobile.core.domain.usecase.SetAppLockUseCase
import com.financemanage.mobile.core.domain.usecase.SetHideBalancesUseCase
import com.financemanage.mobile.core.domain.usecase.SetThemeModeUseCase
import com.financemanage.mobile.core.domain.usecase.SignOutUseCase
import com.financemanage.mobile.core.domain.usecase.UpdateProfileUseCase
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.network.auth.AuthTokens
import com.financemanage.mobile.core.testing.FakeNetworkMonitor
import com.financemanage.mobile.core.testing.FakeOutboxDao
import com.financemanage.mobile.core.testing.FakeTokenStore
import com.financemanage.mobile.core.testing.InMemoryPreferencesDataStore
import com.financemanage.mobile.core.testing.MockBackend
import com.financemanage.mobile.core.testing.SampleJson
import com.financemanage.mobile.core.testing.awaitValue
import com.financemanage.mobile.core.testing.bodyField
import com.financemanage.mobile.core.testing.keepCollecting
import com.financemanage.mobile.core.testing.runViewModelTest
import com.financemanage.mobile.core.testing.testJsonCache
import com.financemanage.mobile.core.testing.testSessionManager
import com.financemanage.mobile.feature.settings.data.SecurityRepositoryImpl
import com.financemanage.mobile.feature.settings.domain.usecase.ChangePasswordUseCase
import com.financemanage.mobile.feature.settings.domain.usecase.DeleteAccountUseCase
import com.financemanage.mobile.feature.settings.domain.usecase.DisableTwoFactorUseCase
import com.financemanage.mobile.feature.settings.domain.usecase.EnableTwoFactorUseCase
import com.financemanage.mobile.feature.settings.domain.usecase.GetDeviceSessionsUseCase
import com.financemanage.mobile.feature.settings.domain.usecase.RevokeDeviceSessionUseCase
import com.financemanage.mobile.feature.settings.domain.usecase.RevokeOtherDeviceSessionsUseCase
import com.financemanage.mobile.feature.settings.domain.usecase.SendEmailVerificationCodeUseCase
import com.financemanage.mobile.feature.settings.domain.usecase.SetupTwoFactorUseCase
import com.financemanage.mobile.feature.settings.domain.usecase.VerifyEmailUseCase
import com.financemanage.mobile.feature.settings.presentation.DeleteAccountViewModel
import com.financemanage.mobile.feature.settings.presentation.EmailVerificationViewModel
import com.financemanage.mobile.feature.settings.presentation.PasswordViewModel
import com.financemanage.mobile.feature.settings.presentation.ProfileEditViewModel
import com.financemanage.mobile.feature.settings.presentation.SessionsViewModel
import com.financemanage.mobile.feature.settings.presentation.SettingsProblem
import com.financemanage.mobile.feature.settings.presentation.SettingsViewModel
import com.financemanage.mobile.feature.settings.presentation.TwoFactorStage
import com.financemanage.mobile.feature.settings.presentation.TwoFactorViewModel
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val ME_2FA_ON = SampleJson.ME.replace("\"twoFactorEnabled\":false", "\"twoFactorEnabled\":true")
private val ME_GOOGLE_ONLY = SampleJson.ME.replace("\"hasPassword\":true", "\"hasPassword\":false")

class SettingsViewModelsTest {
    private val backend = MockBackend(FakeTokenStore(AuthTokens("access", "refresh")))
    private val cache = testJsonCache()
    private val profiles = ProfileRepositoryImpl(backend.client, cache)
    private val security = SecurityRepositoryImpl(backend.client, profiles)

    private fun twoFactorViewModel() =
        TwoFactorViewModel(
            SetupTwoFactorUseCase(security),
            EnableTwoFactorUseCase(security),
            DisableTwoFactorUseCase(security),
            GetProfileUseCase(profiles),
            startSetupImmediately = true,
        )

    private fun passwordViewModel() =
        PasswordViewModel(ChangePasswordUseCase(security), GetCachedProfileUseCase(profiles))

    private val settings = AppSettingsRepositoryImpl(InMemoryPreferencesDataStore())

    @Test
    fun homeShowsProfilePendingWritesAndTheTwoFactorCardUntilSnoozed() =
        runViewModelTest {
            backend.get("me", SampleJson.ME)
            backend.post("auth/logout", "", HttpStatusCode.NoContent)
            val outbox = FakeOutboxDao()
            outbox.upsert(OutboxEntity("1", "transaction", "{}", "k1"))
            val session = testSessionManager(backend, backgroundScope, cache = cache, outbox = outbox)
            val outboxRepository =
                OutboxRepositoryImpl(outbox, OutboxSync(outbox, { emptyList() }, FakeNetworkMonitor(), backgroundScope))
            val viewModel =
                SettingsViewModel(
                    ObserveProfileUseCase(profiles),
                    RefreshProfileUseCase(profiles),
                    ObservePreferencesUseCase(settings),
                    ObservePendingWritesUseCase(outboxRepository),
                    HideTwoFactorCardUseCase(settings),
                    SetHideBalancesUseCase(settings),
                    SetThemeModeUseCase(settings),
                    SetAppLockUseCase(settings),
                    SignOutUseCase(session),
                )
            keepCollecting(viewModel.state)

            viewModel.refresh()
            val loaded = viewModel.state.awaitValue { it.profile.data != null }
            assertEquals(1, loaded.pendingWrites)
            assertTrue(loaded.showTwoFactorCard)

            viewModel.snoozeTwoFactorCard()
            viewModel.state.awaitValue { !it.showTwoFactorCard }

            viewModel.setTheme(ThemeMode.DARK)
            viewModel.setHideBalances(true)
            viewModel.setAppLock(enabled = true, minutes = 1)
            val prefs = settings.preferences.first { it.appLockEnabled }
            assertEquals(ThemeMode.DARK, prefs.themeMode)
            assertTrue(prefs.hideBalances)
            assertEquals(1, prefs.relockAfterMinutes)

            viewModel.signOut()
            session.state.awaitValue { it == SessionState.SIGNED_OUT }
            assertEquals(1, backend.requests(HttpMethod.Post, "auth/logout").size)
            assertTrue(outbox.rows.value.isEmpty())
        }

    @Test
    fun profileEditValidatesTheMonthStartDay() =
        runViewModelTest {
            backend.get("me", SampleJson.ME)
            backend.patch("me", SampleJson.ME)
            val viewModel = ProfileEditViewModel(GetProfileUseCase(profiles), UpdateProfileUseCase(profiles))
            val loaded = viewModel.state.awaitValue { it.loaded }
            assertEquals("Nguyễn Văn A", loaded.fullName)
            assertEquals("Asia/Ho_Chi_Minh", loaded.timeZone)

            viewModel.onMonthStartDay("29")
            viewModel.save()
            assertEquals(SettingsProblem.MONTH_DAY, viewModel.state.value.problem)

            viewModel.onName(" ")
            viewModel.save()
            assertEquals(SettingsProblem.REQUIRED, viewModel.state.value.problem)

            viewModel.onName("Trần Thị B")
            viewModel.onMonthStartDay("25")
            viewModel.onCurrency(CurrencyCode.USD)
            viewModel.save()
            viewModel.state.awaitValue { it.saved }
            val request = backend.single(HttpMethod.Patch, "me")
            assertEquals("Trần Thị B", request.bodyField("fullName"))
            assertEquals("25", request.bodyField("monthStartDay"))
            assertEquals("USD", request.bodyField("defaultCurrency"))
        }

    @Test
    fun emailVerificationHasAResendCountdownAndVerifies() =
        runViewModelTest {
            backend.get("me", SampleJson.ME)
            profiles.refresh()
            backend.post("me/email/verification-code", "", HttpStatusCode.NoContent)
            backend.post("me/email/verify", "", HttpStatusCode.NoContent)
            val viewModel =
                EmailVerificationViewModel(
                    SendEmailVerificationCodeUseCase(security),
                    VerifyEmailUseCase(security),
                    ObserveProfileUseCase(profiles),
                )
            assertEquals("doc9074@example.com", viewModel.state.awaitValue { it.email.isNotEmpty() }.email)

            viewModel.sendCode()
            viewModel.sendCode() // ignored while the first one is running
            viewModel.state.awaitValue { it.codeSent }
            assertEquals(1, backend.requests(HttpMethod.Post, "me/email/verification-code").size)

            // The 60 s countdown runs on virtual time; afterwards a new code can be requested.
            viewModel.state.awaitValue { it.resendIn == 0 }
            viewModel.sendCode()
            backend.await(HttpMethod.Post, "me/email/verification-code", count = 2)
            viewModel.state.awaitValue { !it.loading } // verify() is ignored while a request is running

            viewModel.onCode("123")
            viewModel.verify()
            assertTrue(backend.requests(HttpMethod.Post, "me/email/verify").isEmpty())
            viewModel.onCode("123456")
            viewModel.verify()
            viewModel.state.awaitValue { it.verified }
            assertEquals("123456", backend.single(HttpMethod.Post, "me/email/verify").bodyField("code"))
        }

    @Test
    fun passwordNeedsTheCurrentOneOnlyWhenTheAccountHasOne() =
        runViewModelTest {
            backend.get("me", ME_GOOGLE_ONLY)
            profiles.refresh()
            backend.post("me/password", "", HttpStatusCode.NoContent)
            val viewModel = passwordViewModel()
            viewModel.state.awaitValue { !it.hasPassword }

            viewModel.onNew("short")
            viewModel.save()
            assertEquals(SettingsProblem.PASSWORD_LENGTH, viewModel.state.value.problem)

            viewModel.onNew("NewSecret1")
            viewModel.save()
            viewModel.state.awaitValue { it.done }
            val request = backend.single(HttpMethod.Post, "me/password")
            assertNull(request.bodyField("currentPassword"))
            assertEquals("NewSecret1", request.bodyField("newPassword"))
        }

    @Test
    fun passwordAccountMustTypeTheCurrentPassword() =
        runViewModelTest {
            backend.get("me", SampleJson.ME)
            profiles.refresh()
            backend.fail(HttpMethod.Post, "me/password", HttpStatusCode.BadRequest, "WRONG_PASSWORD")
            val viewModel = passwordViewModel()
            viewModel.onNew("NewSecret1")
            viewModel.save()
            assertEquals(SettingsProblem.REQUIRED, viewModel.state.value.problem)

            viewModel.onCurrent("old")
            viewModel.save()
            val failed = viewModel.state.awaitValue { it.error != null }
            assertEquals("WRONG_PASSWORD", (failed.error as AppError.Api).code)
            assertFalse(failed.done)
        }

    @Test
    fun twoFactorSetupEnableAndRecoveryCodes() =
        runViewModelTest {
            backend.get("me", SampleJson.ME)
            backend.post("me/2fa/setup", """{"secret":"JBSWY3DPEHPK3PXP","otpauthUri":"otpauth://totp/x"}""")
            backend.fail(HttpMethod.Post, "me/2fa/enable", HttpStatusCode.Unauthorized, "INVALID_CODE")
            val viewModel = twoFactorViewModel()
            val setup = viewModel.state.awaitValue { it.stage == TwoFactorStage.SETUP }
            assertEquals("JBSWY3DPEHPK3PXP", setup.setup?.secret)

            viewModel.onCode("12345")
            viewModel.enable()
            assertTrue(backend.requests(HttpMethod.Post, "me/2fa/enable").isEmpty())

            viewModel.onCode("123456")
            viewModel.enable()
            val wrong = viewModel.state.awaitValue { it.error != null }
            assertEquals("", wrong.code)
            assertEquals(TwoFactorStage.SETUP, wrong.stage)

            backend.post("me/2fa/enable", """{"recoveryCodes":["a1","b2"]}""")
            backend.get("me", ME_2FA_ON)
            viewModel.onCode("654321")
            viewModel.enable()
            val codes = viewModel.state.awaitValue { it.stage == TwoFactorStage.RECOVERY_CODES }
            assertEquals(listOf("a1", "b2"), codes.recoveryCodes)

            viewModel.onSavedConfirmed(true)
            viewModel.finishRecoveryCodes()
            assertEquals(TwoFactorStage.ON, viewModel.state.value.stage)
            assertTrue(
                viewModel.state.value.recoveryCodes
                    .isEmpty(),
            )
        }

    @Test
    fun twoFactorDisable() =
        runViewModelTest {
            backend.get("me", ME_2FA_ON)
            backend.post("me/2fa/disable", "", HttpStatusCode.NoContent)
            val viewModel = twoFactorViewModel()
            viewModel.state.awaitValue { it.stage == TwoFactorStage.ON }
            assertTrue(backend.requests(HttpMethod.Post, "me/2fa/setup").isEmpty())

            viewModel.onCode(" 123456 ")
            viewModel.disable()
            viewModel.state.awaitValue { it.stage == TwoFactorStage.OFF_INTRO }
            assertEquals("123456", backend.single(HttpMethod.Post, "me/2fa/disable").bodyField("code"))
        }

    @Test
    fun sessionsListAndRevoke() =
        runViewModelTest {
            backend.get(
                "me/sessions",
                """[{"id":"s1","deviceName":"Pixel 8","current":true},
                {"id":"s2","deviceName":"iPhone","current":false}]""",
            )
            backend.delete("me/sessions/s2")
            backend.delete("me/sessions")
            val viewModel =
                SessionsViewModel(
                    GetDeviceSessionsUseCase(security),
                    RevokeDeviceSessionUseCase(security),
                    RevokeOtherDeviceSessionsUseCase(security),
                )
            viewModel.refresh()
            assertEquals(
                2,
                viewModel.state
                    // Wait for the refresh to end, not just for the data: a refresh still running would swallow the
                    // one triggered by revoke().
                    .awaitValue { it.sessions.data != null && !it.sessions.loading }
                    .sessions.data
                    ?.size,
            )

            viewModel.revoke("s2")
            backend.await(HttpMethod.Get, "me/sessions", count = 2)
            viewModel.revokeOthers()
            backend.await(HttpMethod.Delete, "me/sessions")
            backend.await(HttpMethod.Get, "me/sessions", count = 3)
            viewModel.state.awaitValue { !it.sessions.loading }
        }

    @Test
    fun deleteAccountNeedsTheEmailAndPasswordThenWipesEverything() =
        runViewModelTest {
            backend.get("me", SampleJson.ME)
            profiles.refresh()
            backend.delete("me")
            settings.setOnboardingCompleted(true)
            val session = testSessionManager(backend, backgroundScope, cache = cache, settings = settings)
            val viewModel =
                DeleteAccountViewModel(DeleteAccountUseCase(security, session), GetCachedProfileUseCase(profiles))
            viewModel.state.awaitValue { it.email.isNotEmpty() }

            viewModel.onConfirmEmail("DOC9074@example.com ")
            assertFalse(viewModel.canDelete)
            viewModel.delete()
            assertTrue(backend.requests(HttpMethod.Delete, "me").isEmpty())

            viewModel.onPassword("Secret123")
            assertTrue(viewModel.canDelete)
            viewModel.delete()
            session.state.awaitValue { it == SessionState.SIGNED_OUT }
            val request = backend.single(HttpMethod.Delete, "me")
            assertEquals("DOC9074@example.com", request.bodyField("confirmEmail"))
            assertEquals("Secret123", request.bodyField("password"))
            assertNull(backend.tokenStore.tokens.value)
            assertFalse(settings.preferences.first().onboardingCompleted)
            assertNull(profiles.current())
        }
}
