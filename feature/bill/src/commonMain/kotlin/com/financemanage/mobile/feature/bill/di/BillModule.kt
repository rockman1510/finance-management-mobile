package com.financemanage.mobile.feature.bill.di

import com.financemanage.mobile.feature.bill.data.BillRepositoryImpl
import com.financemanage.mobile.feature.bill.domain.repository.BillRepository
import com.financemanage.mobile.feature.bill.domain.usecase.CreateBillUseCase
import com.financemanage.mobile.feature.bill.domain.usecase.DeleteBillUseCase
import com.financemanage.mobile.feature.bill.domain.usecase.GetBillUseCase
import com.financemanage.mobile.feature.bill.domain.usecase.GetUpcomingBillsUseCase
import com.financemanage.mobile.feature.bill.domain.usecase.ObserveBillsUseCase
import com.financemanage.mobile.feature.bill.domain.usecase.PayBillUseCase
import com.financemanage.mobile.feature.bill.domain.usecase.RefreshBillsUseCase
import com.financemanage.mobile.feature.bill.domain.usecase.SkipBillUseCase
import com.financemanage.mobile.feature.bill.domain.usecase.UpdateBillUseCase
import com.financemanage.mobile.feature.bill.presentation.BillDetailViewModel
import com.financemanage.mobile.feature.bill.presentation.BillFormViewModel
import com.financemanage.mobile.feature.bill.presentation.BillListViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val billModule =
    module {
        // Data
        single<BillRepository> { BillRepositoryImpl(get(), get(), get()) }
        // Domain
        factoryOf(::ObserveBillsUseCase)
        factoryOf(::RefreshBillsUseCase)
        factoryOf(::GetUpcomingBillsUseCase)
        factoryOf(::GetBillUseCase)
        factoryOf(::CreateBillUseCase)
        factoryOf(::UpdateBillUseCase)
        factoryOf(::DeleteBillUseCase)
        factoryOf(::PayBillUseCase)
        factoryOf(::SkipBillUseCase)
        // Presentation
        viewModelOf(::BillListViewModel)
        viewModel { params ->
            BillDetailViewModel(params.get<String>(0), get(), get(), get(), get(), get(), get(), get())
        }
        viewModel { params ->
            BillFormViewModel(params.values.getOrNull(0) as String?, get(), get(), get(), get(), get(), get(), get())
        }
    }
