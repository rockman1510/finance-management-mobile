package com.financemanage.mobile.feature.auth.di

import com.financemanage.mobile.feature.auth.data.AuthRepositoryImpl
import com.financemanage.mobile.feature.auth.domain.repository.AuthRepository
import com.financemanage.mobile.feature.auth.domain.usecase.RequestPasswordResetUseCase
import com.financemanage.mobile.feature.auth.domain.usecase.ResetPasswordUseCase
import com.financemanage.mobile.feature.auth.domain.usecase.SignInUseCase
import com.financemanage.mobile.feature.auth.domain.usecase.SignInWithGoogleUseCase
import com.financemanage.mobile.feature.auth.domain.usecase.SignUpUseCase
import com.financemanage.mobile.feature.auth.domain.usecase.VerifyTwoFactorUseCase
import com.financemanage.mobile.feature.auth.presentation.ForgotPasswordViewModel
import com.financemanage.mobile.feature.auth.presentation.ServerSettingsViewModel
import com.financemanage.mobile.feature.auth.presentation.SignInViewModel
import com.financemanage.mobile.feature.auth.presentation.SignUpViewModel
import com.financemanage.mobile.feature.auth.presentation.TwoFactorViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Google sign-in configuration: the OAuth **web** client id (empty → the Google button is hidden). */
data class AuthConfig(
    val googleWebClientId: String? = null,
)

val authModule =
    module {
        // Data
        single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
        // Domain
        factoryOf(::SignInUseCase)
        factoryOf(::SignInWithGoogleUseCase)
        factoryOf(::SignUpUseCase)
        factoryOf(::VerifyTwoFactorUseCase)
        factoryOf(::RequestPasswordResetUseCase)
        factoryOf(::ResetPasswordUseCase)
        // Presentation
        viewModelOf(::SignInViewModel)
        viewModelOf(::SignUpViewModel)
        viewModel { params -> TwoFactorViewModel(get(), params.get<String>(0), params.get<Long>(1)) }
        viewModelOf(::ForgotPasswordViewModel)
        viewModelOf(::ServerSettingsViewModel)
    }
