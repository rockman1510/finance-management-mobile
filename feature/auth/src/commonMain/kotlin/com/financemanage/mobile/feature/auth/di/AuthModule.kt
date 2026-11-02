package com.financemanage.mobile.feature.auth.di

import com.financemanage.mobile.feature.auth.data.AuthRepositoryImpl
import com.financemanage.mobile.feature.auth.domain.AuthRepository
import com.financemanage.mobile.feature.auth.presentation.ForgotPasswordViewModel
import com.financemanage.mobile.feature.auth.presentation.SignInViewModel
import com.financemanage.mobile.feature.auth.presentation.SignUpViewModel
import com.financemanage.mobile.feature.auth.presentation.TwoFactorViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Google sign-in configuration: the OAuth **web** client id (empty → the Google button is hidden). */
data class AuthConfig(
    val googleWebClientId: String? = null,
)

val authModule =
    module {
        single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
        viewModel { SignInViewModel(get()) }
        viewModel { SignUpViewModel(get()) }
        viewModel { params -> TwoFactorViewModel(get(), params.get<String>(0), params.get<Long>(1)) }
        viewModel { ForgotPasswordViewModel(get()) }
    }
