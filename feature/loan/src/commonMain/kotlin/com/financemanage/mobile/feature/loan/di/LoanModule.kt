package com.financemanage.mobile.feature.loan.di

import com.financemanage.mobile.feature.loan.data.LoanRepository
import com.financemanage.mobile.feature.loan.presentation.LoanDetailViewModel
import com.financemanage.mobile.feature.loan.presentation.LoanFormViewModel
import com.financemanage.mobile.feature.loan.presentation.LoanListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val loanModule =
    module {
        single { LoanRepository(get()) }
        viewModel { LoanListViewModel(get()) }
        viewModel { LoanFormViewModel(get(), get()) }
        viewModel { params -> LoanDetailViewModel(params.get<String>(0), get(), get()) }
    }
