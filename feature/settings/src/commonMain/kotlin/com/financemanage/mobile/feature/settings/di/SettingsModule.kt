package com.financemanage.mobile.feature.settings.di

import com.financemanage.mobile.feature.settings.data.SecurityRepositoryImpl
import com.financemanage.mobile.feature.settings.domain.repository.SecurityRepository
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
import com.financemanage.mobile.feature.settings.presentation.AppLockViewModel
import com.financemanage.mobile.feature.settings.presentation.DeleteAccountViewModel
import com.financemanage.mobile.feature.settings.presentation.EmailVerificationViewModel
import com.financemanage.mobile.feature.settings.presentation.PasswordViewModel
import com.financemanage.mobile.feature.settings.presentation.ProfileEditViewModel
import com.financemanage.mobile.feature.settings.presentation.SessionsViewModel
import com.financemanage.mobile.feature.settings.presentation.SettingsViewModel
import com.financemanage.mobile.feature.settings.presentation.TwoFactorViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsModule =
    module {
        // Data
        single<SecurityRepository> { SecurityRepositoryImpl(get(), get()) }
        // Domain
        factoryOf(::SendEmailVerificationCodeUseCase)
        factoryOf(::VerifyEmailUseCase)
        factoryOf(::ChangePasswordUseCase)
        factoryOf(::SetupTwoFactorUseCase)
        factoryOf(::EnableTwoFactorUseCase)
        factoryOf(::DisableTwoFactorUseCase)
        factoryOf(::GetDeviceSessionsUseCase)
        factoryOf(::RevokeDeviceSessionUseCase)
        factoryOf(::RevokeOtherDeviceSessionsUseCase)
        factoryOf(::DeleteAccountUseCase)
        // Presentation
        viewModelOf(::SettingsViewModel)
        viewModelOf(::AppLockViewModel)
        viewModelOf(::ProfileEditViewModel)
        viewModelOf(::EmailVerificationViewModel)
        viewModelOf(::PasswordViewModel)
        viewModel { params -> TwoFactorViewModel(get(), get(), get(), get(), params.get()) }
        viewModelOf(::SessionsViewModel)
        viewModelOf(::DeleteAccountViewModel)
    }
