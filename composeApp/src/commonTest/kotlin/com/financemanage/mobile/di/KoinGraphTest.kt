package com.financemanage.mobile.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import com.financemanage.mobile.AppViewModel
import com.financemanage.mobile.core.data.cache.JsonCacheDao
import com.financemanage.mobile.core.data.outbox.OutboxDao
import com.financemanage.mobile.core.domain.repository.NetworkMonitor
import com.financemanage.mobile.core.network.ApiConfig
import com.financemanage.mobile.core.network.auth.TokenStore
import com.financemanage.mobile.core.testing.FakeJsonCacheDao
import com.financemanage.mobile.core.testing.FakeNetworkMonitor
import com.financemanage.mobile.core.testing.FakeOutboxDao
import com.financemanage.mobile.core.testing.InMemoryPreferencesDataStore
import com.financemanage.mobile.core.testing.MockBackend
import com.financemanage.mobile.core.testing.runViewModelTest
import com.financemanage.mobile.core.testing.tracked
import com.financemanage.mobile.feature.auth.presentation.ForgotPasswordViewModel
import com.financemanage.mobile.feature.auth.presentation.ServerSettingsViewModel
import com.financemanage.mobile.feature.auth.presentation.SignInViewModel
import com.financemanage.mobile.feature.auth.presentation.SignUpViewModel
import com.financemanage.mobile.feature.bill.presentation.BillDetailViewModel
import com.financemanage.mobile.feature.bill.presentation.BillFormViewModel
import com.financemanage.mobile.feature.bill.presentation.BillListViewModel
import com.financemanage.mobile.feature.budget.presentation.BudgetDetailViewModel
import com.financemanage.mobile.feature.budget.presentation.BudgetFormViewModel
import com.financemanage.mobile.feature.budget.presentation.BudgetListViewModel
import com.financemanage.mobile.feature.category.presentation.CategoryFormViewModel
import com.financemanage.mobile.feature.category.presentation.CategoryListViewModel
import com.financemanage.mobile.feature.currency.presentation.AddRateViewModel
import com.financemanage.mobile.feature.currency.presentation.ConverterViewModel
import com.financemanage.mobile.feature.currency.presentation.RatesViewModel
import com.financemanage.mobile.feature.deposit.presentation.DepositDetailViewModel
import com.financemanage.mobile.feature.deposit.presentation.DepositFormViewModel
import com.financemanage.mobile.feature.deposit.presentation.DepositListViewModel
import com.financemanage.mobile.feature.home.presentation.HomeViewModel
import com.financemanage.mobile.feature.investment.presentation.AssetDetailViewModel
import com.financemanage.mobile.feature.investment.presentation.AssetFormViewModel
import com.financemanage.mobile.feature.investment.presentation.PortfolioViewModel
import com.financemanage.mobile.feature.loan.presentation.LoanDetailViewModel
import com.financemanage.mobile.feature.loan.presentation.LoanFormViewModel
import com.financemanage.mobile.feature.loan.presentation.LoanListViewModel
import com.financemanage.mobile.feature.notification.presentation.NotificationSettingsViewModel
import com.financemanage.mobile.feature.notification.presentation.NotificationsViewModel
import com.financemanage.mobile.feature.onboarding.presentation.OnboardingViewModel
import com.financemanage.mobile.feature.report.presentation.NetWorthViewModel
import com.financemanage.mobile.feature.report.presentation.ReportsViewModel
import com.financemanage.mobile.feature.saving.presentation.GoalDetailViewModel
import com.financemanage.mobile.feature.saving.presentation.GoalFormViewModel
import com.financemanage.mobile.feature.saving.presentation.GoalListViewModel
import com.financemanage.mobile.feature.settings.presentation.AppLockViewModel
import com.financemanage.mobile.feature.settings.presentation.DeleteAccountViewModel
import com.financemanage.mobile.feature.settings.presentation.EmailVerificationViewModel
import com.financemanage.mobile.feature.settings.presentation.PasswordViewModel
import com.financemanage.mobile.feature.settings.presentation.ProfileEditViewModel
import com.financemanage.mobile.feature.settings.presentation.SessionsViewModel
import com.financemanage.mobile.feature.settings.presentation.SettingsViewModel
import com.financemanage.mobile.feature.transaction.presentation.OutboxViewModel
import com.financemanage.mobile.feature.transaction.presentation.QuickAddViewModel
import com.financemanage.mobile.feature.transaction.presentation.RecurringEditViewModel
import com.financemanage.mobile.feature.transaction.presentation.RecurringListViewModel
import com.financemanage.mobile.feature.transaction.presentation.TransactionDetailViewModel
import com.financemanage.mobile.feature.transaction.presentation.TransactionListViewModel
import com.financemanage.mobile.feature.transaction.presentation.TransferViewModel
import com.financemanage.mobile.feature.wallet.presentation.AdjustBalanceViewModel
import com.financemanage.mobile.feature.wallet.presentation.WalletDetailViewModel
import com.financemanage.mobile.feature.wallet.presentation.WalletFormViewModel
import com.financemanage.mobile.feature.wallet.presentation.WalletListViewModel
import com.financemanage.mobile.navigation.PushViewModel
import io.ktor.client.HttpClient
import org.koin.core.Koin
import org.koin.core.parameter.ParametersHolder
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertTrue
import com.financemanage.mobile.feature.auth.presentation.TwoFactorViewModel as AuthTwoFactorViewModel
import com.financemanage.mobile.feature.settings.presentation.TwoFactorViewModel as SettingsTwoFactorViewModel

/**
 * Starts the real Koin modules of the app (platform pieces and HTTP replaced by fakes) and resolves every ViewModel
 * the screens ask for, with the same parameters, so a missing or mis-wired definition fails here, not at runtime.
 */
class KoinGraphTest {
    private val backend = MockBackend()

    private val testPlatformModule =
        module {
            single<TokenStore> { backend.tokenStore }
            single<HttpClient> { backend.client }
            single<NetworkMonitor> { FakeNetworkMonitor() }
            single<DataStore<Preferences>> { InMemoryPreferencesDataStore() }
            single<JsonCacheDao> { FakeJsonCacheDao() }
            single<OutboxDao> { FakeOutboxDao() }
        }

    private fun koin(): Koin =
        koinApplication {
            allowOverride(true)
            modules(appModules(ApiConfig(baseUrl = "http://localhost/")) + testPlatformModule)
        }.koin

    private val screens: List<Pair<KClass<out ViewModel>, ParametersHolder>> =
        listOf(
            AppViewModel::class to parametersOf(),
            PushViewModel::class to parametersOf(),
            SignInViewModel::class to parametersOf(),
            SignUpViewModel::class to parametersOf(),
            AuthTwoFactorViewModel::class to parametersOf("challenge", 300L),
            ForgotPasswordViewModel::class to parametersOf(),
            ServerSettingsViewModel::class to parametersOf(),
            OnboardingViewModel::class to parametersOf("Tiền mặt", "Chi tiêu"),
            SettingsViewModel::class to parametersOf(),
            AppLockViewModel::class to parametersOf(),
            ProfileEditViewModel::class to parametersOf(),
            EmailVerificationViewModel::class to parametersOf(),
            PasswordViewModel::class to parametersOf(),
            SettingsTwoFactorViewModel::class to parametersOf(false),
            SessionsViewModel::class to parametersOf(),
            DeleteAccountViewModel::class to parametersOf(),
            WalletListViewModel::class to parametersOf(),
            WalletDetailViewModel::class to parametersOf("w1"),
            WalletFormViewModel::class to parametersOf(null),
            AdjustBalanceViewModel::class to parametersOf("w1"),
            CategoryListViewModel::class to parametersOf(),
            CategoryFormViewModel::class to parametersOf(null, "EXPENSE", null),
            TransactionListViewModel::class to parametersOf(null, null),
            QuickAddViewModel::class to parametersOf("EXPENSE", null),
            TransactionDetailViewModel::class to parametersOf("t1"),
            TransferViewModel::class to parametersOf(),
            RecurringListViewModel::class to parametersOf(),
            RecurringEditViewModel::class to parametersOf("r1"),
            OutboxViewModel::class to parametersOf(),
            RatesViewModel::class to parametersOf(),
            AddRateViewModel::class to parametersOf(null, null),
            ConverterViewModel::class to parametersOf(),
            BudgetListViewModel::class to parametersOf(),
            BudgetDetailViewModel::class to parametersOf("b1"),
            BudgetFormViewModel::class to parametersOf(null),
            BillListViewModel::class to parametersOf(),
            BillDetailViewModel::class to parametersOf("b1"),
            BillFormViewModel::class to parametersOf(null),
            GoalListViewModel::class to parametersOf(),
            GoalDetailViewModel::class to parametersOf("g1"),
            GoalFormViewModel::class to parametersOf(null),
            HomeViewModel::class to parametersOf(),
            DepositListViewModel::class to parametersOf(),
            DepositFormViewModel::class to parametersOf(),
            DepositDetailViewModel::class to parametersOf("d1"),
            LoanListViewModel::class to parametersOf(),
            LoanFormViewModel::class to parametersOf(),
            LoanDetailViewModel::class to parametersOf("l1"),
            PortfolioViewModel::class to parametersOf(),
            AssetDetailViewModel::class to parametersOf("a1"),
            AssetFormViewModel::class to parametersOf(null),
            ReportsViewModel::class to parametersOf(),
            NetWorthViewModel::class to parametersOf(),
            NotificationsViewModel::class to parametersOf(),
            NotificationSettingsViewModel::class to parametersOf(),
        )

    @Test
    fun everyViewModelResolves() =
        runViewModelTest {
            val koin = koin()
            val failures =
                screens.mapNotNull { (type, params) ->
                    runCatching { koin.get<ViewModel>(type, null) { params }.tracked() }
                        .exceptionOrNull()
                        ?.let { "${type.simpleName}: $it" }
                }
            assertTrue(failures.isEmpty(), failures.joinToString("\n"))
        }
}
