package com.financemanage.mobile.feature.onboarding.di

import com.financemanage.mobile.feature.onboarding.data.OnboardingRepository
import com.financemanage.mobile.feature.onboarding.presentation.OnboardingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val onboardingModule =
    module {
        single { OnboardingRepository(get(), get(), get(), get()) }
        viewModel { params -> OnboardingViewModel(get(), params.get<String>(0), params.get<String>(1)) }
    }
