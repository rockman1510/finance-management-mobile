package com.financemanage.mobile.feature.deposit.di

import com.financemanage.mobile.feature.deposit.data.DepositRepository
import com.financemanage.mobile.feature.deposit.presentation.DepositDetailViewModel
import com.financemanage.mobile.feature.deposit.presentation.DepositFormViewModel
import com.financemanage.mobile.feature.deposit.presentation.DepositListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val depositModule =
    module {
        single { DepositRepository(get(), get(), get()) }
        viewModel { DepositListViewModel(get()) }
        viewModel { DepositFormViewModel(get(), get(), get()) }
        viewModel { params -> DepositDetailViewModel(params.get<String>(0), get(), get(), get()) }
    }
