package com.financemanage.mobile.feature.wallet.di

import com.financemanage.mobile.feature.wallet.presentation.AdjustBalanceViewModel
import com.financemanage.mobile.feature.wallet.presentation.WalletDetailViewModel
import com.financemanage.mobile.feature.wallet.presentation.WalletFormViewModel
import com.financemanage.mobile.feature.wallet.presentation.WalletListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val walletModule =
    module {
        viewModel { WalletListViewModel(get(), get()) }
        viewModel { params -> WalletDetailViewModel(params.get(), get()) }
        viewModel { params -> WalletFormViewModel(params.values.getOrNull(0) as String?, get(), get()) }
        viewModel { params -> AdjustBalanceViewModel(params.get(), get(), get()) }
    }
