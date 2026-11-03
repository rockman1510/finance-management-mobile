package com.financemanage.mobile.feature.report.di

import com.financemanage.mobile.feature.report.data.ReportRepository
import com.financemanage.mobile.feature.report.presentation.NetWorthViewModel
import com.financemanage.mobile.feature.report.presentation.ReportsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val reportModule =
    module {
        single { ReportRepository(get(), get()) }
        viewModel { ReportsViewModel(get(), get()) }
        viewModel { NetWorthViewModel(get(), get()) }
    }
