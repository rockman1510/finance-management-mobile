package com.financemanage.mobile.feature.settings.di

import com.financemanage.mobile.feature.settings.data.SecurityRepository
import com.financemanage.mobile.feature.settings.presentation.DeleteAccountViewModel
import com.financemanage.mobile.feature.settings.presentation.EmailVerificationViewModel
import com.financemanage.mobile.feature.settings.presentation.PasswordViewModel
import com.financemanage.mobile.feature.settings.presentation.ProfileEditViewModel
import com.financemanage.mobile.feature.settings.presentation.SessionsViewModel
import com.financemanage.mobile.feature.settings.presentation.SettingsViewModel
import com.financemanage.mobile.feature.settings.presentation.TwoFactorViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule =
    module {
        single { SecurityRepository(get(), get()) }
        viewModel { SettingsViewModel(get(), get(), get(), get()) }
        viewModel { ProfileEditViewModel(get()) }
        viewModel { EmailVerificationViewModel(get(), get()) }
        viewModel { PasswordViewModel(get(), get()) }
        viewModel { params -> TwoFactorViewModel(get(), get(), params.get()) }
        viewModel { SessionsViewModel(get()) }
        viewModel { DeleteAccountViewModel(get(), get(), get()) }
    }
