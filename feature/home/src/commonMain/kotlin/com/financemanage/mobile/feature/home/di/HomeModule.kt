package com.financemanage.mobile.feature.home.di

import com.financemanage.mobile.feature.home.data.HomeRepository
import com.financemanage.mobile.feature.home.presentation.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val homeModule =
    module {
        single { HomeRepository(get(), get(), get()) }
        viewModel { HomeViewModel(get(), get(), get(), get(), get()) }
    }
