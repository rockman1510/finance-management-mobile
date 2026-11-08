package com.financemanage.mobile.feature.report.di

import com.financemanage.mobile.feature.report.data.ReportRepositoryImpl
import com.financemanage.mobile.feature.report.domain.repository.ReportRepository
import com.financemanage.mobile.feature.report.domain.usecase.ExportTransactionsCsvUseCase
import com.financemanage.mobile.feature.report.domain.usecase.GetCashFlowUseCase
import com.financemanage.mobile.feature.report.domain.usecase.GetCategoryReportUseCase
import com.financemanage.mobile.feature.report.domain.usecase.GetDashboardUseCase
import com.financemanage.mobile.feature.report.domain.usecase.GetReportSummaryUseCase
import com.financemanage.mobile.feature.report.domain.usecase.ObserveNetWorthUseCase
import com.financemanage.mobile.feature.report.domain.usecase.RefreshNetWorthUseCase
import com.financemanage.mobile.feature.report.presentation.NetWorthViewModel
import com.financemanage.mobile.feature.report.presentation.ReportsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val reportModule =
    module {
        // Data
        single<ReportRepository> { ReportRepositoryImpl(get(), get()) }
        // Domain
        factoryOf(::ObserveNetWorthUseCase)
        factoryOf(::GetReportSummaryUseCase)
        factoryOf(::GetCategoryReportUseCase)
        factoryOf(::GetCashFlowUseCase)
        factoryOf(::RefreshNetWorthUseCase)
        factoryOf(::GetDashboardUseCase)
        factoryOf(::ExportTransactionsCsvUseCase)
        // Presentation
        viewModelOf(::ReportsViewModel)
        viewModelOf(::NetWorthViewModel)
    }
