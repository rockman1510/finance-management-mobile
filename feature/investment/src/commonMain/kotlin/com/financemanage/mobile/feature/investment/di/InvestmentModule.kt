package com.financemanage.mobile.feature.investment.di

import com.financemanage.mobile.feature.investment.data.InvestmentRepository
import com.financemanage.mobile.feature.investment.presentation.AssetDetailViewModel
import com.financemanage.mobile.feature.investment.presentation.AssetFormViewModel
import com.financemanage.mobile.feature.investment.presentation.PortfolioViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val investmentModule =
    module {
        single { InvestmentRepository(get(), get(), get()) }
        viewModel { PortfolioViewModel(get(), get()) }
        viewModel { params -> AssetDetailViewModel(params.get<String>(0), get(), get(), get(), get()) }
        viewModel { params -> AssetFormViewModel(params.values.getOrNull(0) as String?, get(), get()) }
    }
