package com.financemanage.mobile.feature.onboarding.di

import com.financemanage.mobile.feature.onboarding.data.OnboardingRepositoryImpl
import com.financemanage.mobile.feature.onboarding.domain.repository.OnboardingRepository
import com.financemanage.mobile.feature.onboarding.domain.usecase.CreateFirstWalletUseCase
import com.financemanage.mobile.feature.onboarding.domain.usecase.CreateMonthlyBudgetUseCase
import com.financemanage.mobile.feature.onboarding.domain.usecase.SetDefaultCurrencyUseCase
import com.financemanage.mobile.feature.onboarding.domain.usecase.ShouldSkipOnboardingUseCase
import com.financemanage.mobile.feature.onboarding.presentation.OnboardingViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val onboardingModule =
    module {
        // Data
        single<OnboardingRepository> { OnboardingRepositoryImpl(get()) }
        // Domain
        factoryOf(::ShouldSkipOnboardingUseCase)
        factoryOf(::SetDefaultCurrencyUseCase)
        factoryOf(::CreateFirstWalletUseCase)
        factoryOf(::CreateMonthlyBudgetUseCase)
        // Presentation
        viewModel { params ->
            OnboardingViewModel(params.get<String>(0), params.get<String>(1), get(), get(), get(), get(), get(), get())
        }
    }
