package com.financemanage.mobile.feature.home.di

import com.financemanage.mobile.feature.home.data.HomeRepositoryImpl
import com.financemanage.mobile.feature.home.domain.repository.HomeRepository
import com.financemanage.mobile.feature.home.domain.usecase.GetHomeCurrencyUseCase
import com.financemanage.mobile.feature.home.domain.usecase.GetUnreadNotificationCountUseCase
import com.financemanage.mobile.feature.home.domain.usecase.HomeUseCases
import com.financemanage.mobile.feature.home.domain.usecase.ObserveHomeBillsUseCase
import com.financemanage.mobile.feature.home.domain.usecase.ObserveHomeBudgetsUseCase
import com.financemanage.mobile.feature.home.domain.usecase.ObserveHomeGoalsUseCase
import com.financemanage.mobile.feature.home.domain.usecase.ObserveHomeSummaryUseCase
import com.financemanage.mobile.feature.home.domain.usecase.ObserveHomeTransactionsUseCase
import com.financemanage.mobile.feature.home.domain.usecase.PayHomeBillUseCase
import com.financemanage.mobile.feature.home.domain.usecase.RefreshHomeBillsUseCase
import com.financemanage.mobile.feature.home.domain.usecase.RefreshHomeBudgetsUseCase
import com.financemanage.mobile.feature.home.domain.usecase.RefreshHomeGoalsUseCase
import com.financemanage.mobile.feature.home.domain.usecase.RefreshHomeOverviewUseCase
import com.financemanage.mobile.feature.home.domain.usecase.RefreshHomeSummaryUseCase
import com.financemanage.mobile.feature.home.domain.usecase.RefreshHomeTransactionsUseCase
import com.financemanage.mobile.feature.home.presentation.HomeViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeModule =
    module {
        // Data
        single<HomeRepository> { HomeRepositoryImpl(get(), get(), get()) }
        // Domain
        factoryOf(::GetHomeCurrencyUseCase)
        factoryOf(::ObserveHomeSummaryUseCase)
        factoryOf(::RefreshHomeSummaryUseCase)
        factoryOf(::ObserveHomeBudgetsUseCase)
        factoryOf(::RefreshHomeBudgetsUseCase)
        factoryOf(::ObserveHomeBillsUseCase)
        factoryOf(::RefreshHomeBillsUseCase)
        factoryOf(::ObserveHomeGoalsUseCase)
        factoryOf(::RefreshHomeGoalsUseCase)
        factoryOf(::ObserveHomeTransactionsUseCase)
        factoryOf(::RefreshHomeTransactionsUseCase)
        factoryOf(::GetUnreadNotificationCountUseCase)
        factoryOf(::PayHomeBillUseCase)
        factoryOf(::RefreshHomeOverviewUseCase)
        factoryOf(::HomeUseCases)
        // Presentation
        viewModelOf(::HomeViewModel)
    }
