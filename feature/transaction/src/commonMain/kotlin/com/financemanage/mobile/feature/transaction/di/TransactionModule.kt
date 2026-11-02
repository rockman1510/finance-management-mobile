package com.financemanage.mobile.feature.transaction.di

import com.financemanage.mobile.core.data.outbox.OutboxSender
import com.financemanage.mobile.feature.transaction.data.TransactionOutboxSender
import com.financemanage.mobile.feature.transaction.data.TransactionRepository
import com.financemanage.mobile.feature.transaction.data.TransferOutboxSender
import com.financemanage.mobile.feature.transaction.domain.TransactionFilter
import com.financemanage.mobile.feature.transaction.presentation.OutboxViewModel
import com.financemanage.mobile.feature.transaction.presentation.QuickAddViewModel
import com.financemanage.mobile.feature.transaction.presentation.RecurringEditViewModel
import com.financemanage.mobile.feature.transaction.presentation.RecurringListViewModel
import com.financemanage.mobile.feature.transaction.presentation.TransactionDetailViewModel
import com.financemanage.mobile.feature.transaction.presentation.TransactionListViewModel
import com.financemanage.mobile.feature.transaction.presentation.TransferViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val transactionModule =
    module {
        single { TransactionRepository(get(), get(), get(), get(), get(), get()) }
        single { TransactionOutboxSender(get(), get()) } bind OutboxSender::class
        single { TransferOutboxSender(get(), get()) } bind OutboxSender::class
        viewModel { params ->
            TransactionListViewModel(
                initialFilter =
                    TransactionFilter(
                        walletId = params.values.getOrNull(0) as String?,
                        categoryIds = listOfNotNull(params.values.getOrNull(1) as String?),
                    ),
                repository = get(),
                wallets = get(),
                categories = get(),
            )
        }
        viewModel { params ->
            QuickAddViewModel(
                initialKind = params.get<String>(0),
                transactionId = params.values.getOrNull(1) as String?,
                repository = get(),
                wallets = get(),
                categories = get(),
                settings = get(),
                profiles = get(),
            )
        }
        viewModel { params -> TransactionDetailViewModel(params.get<String>(0), get(), get(), get()) }
        viewModel { TransferViewModel(get(), get(), get(), get()) }
        viewModel { RecurringListViewModel(get(), get(), get()) }
        viewModel { params -> RecurringEditViewModel(params.get<String>(0), get(), get(), get()) }
        viewModel { OutboxViewModel(get()) }
    }
