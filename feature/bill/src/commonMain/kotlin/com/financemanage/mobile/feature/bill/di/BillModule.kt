package com.financemanage.mobile.feature.bill.di

import com.financemanage.mobile.feature.bill.data.BillRepository
import com.financemanage.mobile.feature.bill.presentation.BillDetailViewModel
import com.financemanage.mobile.feature.bill.presentation.BillFormViewModel
import com.financemanage.mobile.feature.bill.presentation.BillListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val billModule =
    module {
        single { BillRepository(get(), get(), get()) }
        viewModel { BillListViewModel(get()) }
        viewModel { params -> BillDetailViewModel(params.get<String>(0), get(), get(), get()) }
        viewModel { params -> BillFormViewModel(params.values.getOrNull(0) as String?, get(), get(), get(), get()) }
    }
