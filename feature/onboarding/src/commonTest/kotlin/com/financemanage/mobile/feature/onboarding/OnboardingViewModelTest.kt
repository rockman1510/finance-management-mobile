package com.financemanage.mobile.feature.onboarding

import com.financemanage.mobile.core.data.settings.AppSettingsRepository
import com.financemanage.mobile.core.data.shared.ProfileRepository
import com.financemanage.mobile.core.data.shared.WalletRepository
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.domain.WalletType
import com.financemanage.mobile.core.testing.InMemoryPreferencesDataStore
import com.financemanage.mobile.core.testing.MockBackend
import com.financemanage.mobile.core.testing.SampleJson
import com.financemanage.mobile.core.testing.awaitValue
import com.financemanage.mobile.core.testing.bodyField
import com.financemanage.mobile.core.testing.runViewModelTest
import com.financemanage.mobile.core.testing.testJsonCache
import com.financemanage.mobile.feature.onboarding.data.OnboardingRepository
import com.financemanage.mobile.feature.onboarding.presentation.OnboardingStep
import com.financemanage.mobile.feature.onboarding.presentation.OnboardingViewModel
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnboardingViewModelTest {
    private val backend = MockBackend()
    private val cache = testJsonCache()
    private val settings = AppSettingsRepository(InMemoryPreferencesDataStore())
    private val repository =
        OnboardingRepository(
            backend.client,
            settings,
            ProfileRepository(backend.client, cache),
            WalletRepository(backend.client, cache),
        )

    private fun viewModel() = OnboardingViewModel(repository, "Tiền mặt", "Chi tiêu hằng tháng")

    @Test
    fun skippedWhenTheAccountAlreadyHasWallets() =
        runViewModelTest {
            backend.get("wallets", "[${SampleJson.WALLET}]")
            viewModel().state.awaitValue { it.step == OnboardingStep.DONE }
            assertTrue(settings.preferences.first().onboardingCompleted)
        }

    @Test
    fun skippedWhenAlreadyCompletedOnThisDevice() =
        runViewModelTest {
            settings.setOnboardingCompleted(true)
            viewModel().state.awaitValue { it.step == OnboardingStep.DONE }
            assertTrue(backend.requests.isEmpty())
        }

    @Test
    fun walksThroughCurrencyWalletBudgetAndSecurity() =
        runViewModelTest {
            backend.get("wallets", "[]")
            backend.patch("me", SampleJson.ME.replace("\"VND\"", "\"USD\""))
            backend.post("wallets", SampleJson.WALLET, HttpStatusCode.Created)
            backend.post("budgets", "{}", HttpStatusCode.Created)
            val viewModel = viewModel()
            viewModel.state.awaitValue { it.step == OnboardingStep.CURRENCY }

            viewModel.onCurrencySelected(CurrencyCode.USD)
            viewModel.confirmCurrency()
            viewModel.state.awaitValue { it.step == OnboardingStep.WALLET }
            assertEquals("USD", backend.single(HttpMethod.Patch, "me").bodyField("defaultCurrency"))

            viewModel.onWalletType(WalletType.BANK_ACCOUNT)
            viewModel.onWalletBalance("1.250,5")
            viewModel.confirmWallet()
            viewModel.state.awaitValue { it.step == OnboardingStep.BUDGET }
            val wallet = backend.single(HttpMethod.Post, "wallets")
            assertEquals("Tiền mặt", wallet.bodyField("name"))
            assertEquals("BANK_ACCOUNT", wallet.bodyField("type"))
            assertEquals("USD", wallet.bodyField("currency"))
            assertEquals("1250.5", wallet.bodyField("initialBalance"))

            viewModel.onBudgetAmount("0")
            viewModel.confirmBudget()
            assertTrue(viewModel.state.value.amountInvalid)

            viewModel.onBudgetAmount("500")
            viewModel.confirmBudget()
            viewModel.state.awaitValue { it.step == OnboardingStep.SECURITY }
            val budget = backend.single(HttpMethod.Post, "budgets")
            assertEquals("Chi tiêu hằng tháng", budget.bodyField("name"))
            assertEquals("500", budget.bodyField("amount"))
            assertEquals("MONTHLY", budget.bodyField("period"))
            assertEquals("true", budget.bodyField("repeat"))

            viewModel.finish()
            viewModel.state.awaitValue { it.step == OnboardingStep.DONE }
            assertTrue(settings.preferences.first().onboardingCompleted)
        }

    @Test
    fun aFailedStepStaysOnTheStepWithTheError() =
        runViewModelTest {
            backend.get("wallets", "[]")
            backend.fail(HttpMethod.Post, "wallets", HttpStatusCode.BadRequest, "VALIDATION_FAILED")
            val viewModel = viewModel()
            viewModel.state.awaitValue { it.step == OnboardingStep.CURRENCY }
            backend.patch("me", SampleJson.ME)
            viewModel.confirmCurrency()
            viewModel.state.awaitValue { it.step == OnboardingStep.WALLET }

            viewModel.confirmWallet()
            val failed = viewModel.state.awaitValue { it.error != null }
            assertEquals(OnboardingStep.WALLET, failed.step)

            viewModel.skipBudget()
            assertEquals(OnboardingStep.SECURITY, viewModel.state.value.step)
        }
}
