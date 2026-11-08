package com.financemanage.mobile.feature.loan.di

import com.financemanage.mobile.feature.loan.data.LoanRepositoryImpl
import com.financemanage.mobile.feature.loan.domain.repository.LoanRepository
import com.financemanage.mobile.feature.loan.domain.usecase.ChangeLoanRateUseCase
import com.financemanage.mobile.feature.loan.domain.usecase.CreateLoanUseCase
import com.financemanage.mobile.feature.loan.domain.usecase.DeleteLoanUseCase
import com.financemanage.mobile.feature.loan.domain.usecase.GetLoanUseCase
import com.financemanage.mobile.feature.loan.domain.usecase.GetLoansUseCase
import com.financemanage.mobile.feature.loan.domain.usecase.PrepayLoanUseCase
import com.financemanage.mobile.feature.loan.domain.usecase.PreviewLoanScheduleUseCase
import com.financemanage.mobile.feature.loan.domain.usecase.RepayLoanUseCase
import com.financemanage.mobile.feature.loan.presentation.LoanDetailViewModel
import com.financemanage.mobile.feature.loan.presentation.LoanFormViewModel
import com.financemanage.mobile.feature.loan.presentation.LoanListViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val loanModule =
    module {
        // Data
        single<LoanRepository> { LoanRepositoryImpl(get()) }
        // Domain
        factoryOf(::GetLoansUseCase)
        factoryOf(::GetLoanUseCase)
        factoryOf(::PreviewLoanScheduleUseCase)
        factoryOf(::CreateLoanUseCase)
        factoryOf(::DeleteLoanUseCase)
        factoryOf(::RepayLoanUseCase)
        factoryOf(::PrepayLoanUseCase)
        factoryOf(::ChangeLoanRateUseCase)
        // Presentation
        viewModelOf(::LoanListViewModel)
        viewModelOf(::LoanFormViewModel)
        viewModel { params -> LoanDetailViewModel(params.get<String>(0), get(), get(), get(), get(), get(), get()) }
    }
