package com.financemanage.mobile.feature.deposit.di

import com.financemanage.mobile.feature.deposit.data.DepositRepositoryImpl
import com.financemanage.mobile.feature.deposit.domain.repository.DepositRepository
import com.financemanage.mobile.feature.deposit.domain.usecase.AwaitDepositRecordedUseCase
import com.financemanage.mobile.feature.deposit.domain.usecase.CreateDepositUseCase
import com.financemanage.mobile.feature.deposit.domain.usecase.DeleteDepositUseCase
import com.financemanage.mobile.feature.deposit.domain.usecase.GetDepositUseCase
import com.financemanage.mobile.feature.deposit.domain.usecase.GetSettlementQuoteUseCase
import com.financemanage.mobile.feature.deposit.domain.usecase.ObserveDepositsUseCase
import com.financemanage.mobile.feature.deposit.domain.usecase.PreviewDepositUseCase
import com.financemanage.mobile.feature.deposit.domain.usecase.RefreshDepositsUseCase
import com.financemanage.mobile.feature.deposit.domain.usecase.SettleDepositUseCase
import com.financemanage.mobile.feature.deposit.domain.usecase.UpdateDepositNoteUseCase
import com.financemanage.mobile.feature.deposit.presentation.DepositDetailViewModel
import com.financemanage.mobile.feature.deposit.presentation.DepositFormViewModel
import com.financemanage.mobile.feature.deposit.presentation.DepositListViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val depositModule =
    module {
        // Data
        single<DepositRepository> { DepositRepositoryImpl(get(), get(), get()) }
        // Domain
        factoryOf(::ObserveDepositsUseCase)
        factoryOf(::RefreshDepositsUseCase)
        factoryOf(::GetDepositUseCase)
        factoryOf(::PreviewDepositUseCase)
        factoryOf(::CreateDepositUseCase)
        factoryOf(::UpdateDepositNoteUseCase)
        factoryOf(::GetSettlementQuoteUseCase)
        factoryOf(::SettleDepositUseCase)
        factoryOf(::AwaitDepositRecordedUseCase)
        factoryOf(::DeleteDepositUseCase)
        // Presentation
        viewModelOf(::DepositListViewModel)
        viewModelOf(::DepositFormViewModel)
        viewModel { params ->
            DepositDetailViewModel(params.get<String>(0), get(), get(), get(), get(), get(), get(), get())
        }
    }
