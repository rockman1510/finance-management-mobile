package com.financemanage.mobile.di

import com.financemanage.mobile.AppViewModel
import com.financemanage.mobile.core.data.di.dataModule
import com.financemanage.mobile.core.data.di.dataPlatformModule
import com.financemanage.mobile.core.data.outbox.OutboxSync
import com.financemanage.mobile.core.data.push.PushRegistrar
import com.financemanage.mobile.core.data.session.SessionManager
import com.financemanage.mobile.core.network.ApiConfig
import com.financemanage.mobile.core.network.di.networkModule
import com.financemanage.mobile.feature.auth.di.AuthConfig
import com.financemanage.mobile.feature.auth.di.authModule
import com.financemanage.mobile.feature.bill.di.billModule
import com.financemanage.mobile.feature.budget.di.budgetModule
import com.financemanage.mobile.feature.category.di.categoryModule
import com.financemanage.mobile.feature.currency.di.currencyModule
import com.financemanage.mobile.feature.deposit.di.depositModule
import com.financemanage.mobile.feature.home.di.homeModule
import com.financemanage.mobile.feature.investment.di.investmentModule
import com.financemanage.mobile.feature.loan.di.loanModule
import com.financemanage.mobile.feature.notification.di.notificationModule
import com.financemanage.mobile.feature.onboarding.di.onboardingModule
import com.financemanage.mobile.feature.report.di.reportModule
import com.financemanage.mobile.feature.saving.di.savingModule
import com.financemanage.mobile.feature.settings.di.settingsModule
import com.financemanage.mobile.feature.transaction.di.transactionModule
import com.financemanage.mobile.feature.wallet.di.walletModule
import com.financemanage.mobile.platformGoogleWebClientId
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

private val appModule =
    module {
        single { AuthConfig(googleWebClientId = platformGoogleWebClientId()) }
        viewModel { AppViewModel(settings = get(), networkMonitor = get(), sessionState = get<SessionManager>().state) }
    }

/** Every Koin module of the app: core, then one per feature. */
fun appModules(config: ApiConfig): List<Module> =
    listOf(
        networkModule(config),
        dataPlatformModule,
        dataModule,
        appModule,
        authModule,
        onboardingModule,
        settingsModule,
        walletModule,
        categoryModule,
        transactionModule,
        currencyModule,
        budgetModule,
        billModule,
        savingModule,
        homeModule,
        depositModule,
        loanModule,
        investmentModule,
        reportModule,
        notificationModule,
    )

fun initKoin(
    config: ApiConfig,
    platformDeclaration: KoinAppDeclaration = {},
) {
    val koin =
        startKoin {
            platformDeclaration()
            modules(appModules(config))
        }.koin
    // Offline writes (F21) are sent whenever the device is online.
    koin.get<OutboxSync>().start()
    // Push token registration (F20).
    koin.get<PushRegistrar>().start()
}
