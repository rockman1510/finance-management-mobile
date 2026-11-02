package com.financemanage.mobile.feature.currency.di

import com.financemanage.mobile.feature.currency.presentation.AddRateViewModel
import com.financemanage.mobile.feature.currency.presentation.ConverterViewModel
import com.financemanage.mobile.feature.currency.presentation.RatesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val currencyModule =
    module {
        viewModel { RatesViewModel(get()) }
        viewModel { params ->
            AddRateViewModel(params.values.getOrNull(0) as String?, params.values.getOrNull(1) as String?, get(), get())
        }
        viewModel { ConverterViewModel(get()) }
    }
