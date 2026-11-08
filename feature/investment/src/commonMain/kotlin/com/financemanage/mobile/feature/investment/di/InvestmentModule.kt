package com.financemanage.mobile.feature.investment.di

import com.financemanage.mobile.feature.investment.data.InvestmentRepositoryImpl
import com.financemanage.mobile.feature.investment.domain.repository.InvestmentRepository
import com.financemanage.mobile.feature.investment.domain.usecase.CreateAssetUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.DeleteAssetUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.GetAssetUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.GetPortfolioUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.ObserveAssetsUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.RecordTradeUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.RefreshAssetsUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.UpdateAssetPriceUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.UpdateAssetUseCase
import com.financemanage.mobile.feature.investment.presentation.AssetDetailViewModel
import com.financemanage.mobile.feature.investment.presentation.AssetFormViewModel
import com.financemanage.mobile.feature.investment.presentation.PortfolioViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val investmentModule =
    module {
        // Data
        single<InvestmentRepository> { InvestmentRepositoryImpl(get(), get(), get()) }
        // Domain
        factoryOf(::ObserveAssetsUseCase)
        factoryOf(::RefreshAssetsUseCase)
        factoryOf(::GetPortfolioUseCase)
        factoryOf(::GetAssetUseCase)
        factoryOf(::CreateAssetUseCase)
        factoryOf(::UpdateAssetUseCase)
        factoryOf(::DeleteAssetUseCase)
        factoryOf(::UpdateAssetPriceUseCase)
        factoryOf(::RecordTradeUseCase)
        // Presentation
        viewModelOf(::PortfolioViewModel)
        viewModel { params ->
            AssetDetailViewModel(params.get<String>(0), get(), get(), get(), get(), get(), get(), get())
        }
        viewModel { params -> AssetFormViewModel(params.values.getOrNull(0) as String?, get(), get(), get(), get()) }
    }
