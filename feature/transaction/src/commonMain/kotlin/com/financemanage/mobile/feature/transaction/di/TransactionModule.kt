package com.financemanage.mobile.feature.transaction.di

import com.financemanage.mobile.core.data.outbox.OutboxSender
import com.financemanage.mobile.feature.transaction.data.TransactionOutboxSender
import com.financemanage.mobile.feature.transaction.data.TransactionRepositoryImpl
import com.financemanage.mobile.feature.transaction.data.TransferOutboxSender
import com.financemanage.mobile.feature.transaction.domain.model.TransactionFilter
import com.financemanage.mobile.feature.transaction.domain.repository.TransactionRepository
import com.financemanage.mobile.feature.transaction.domain.usecase.CreateRecurringUseCase
import com.financemanage.mobile.feature.transaction.domain.usecase.CreateTransactionUseCase
import com.financemanage.mobile.feature.transaction.domain.usecase.CreateTransferUseCase
import com.financemanage.mobile.feature.transaction.domain.usecase.DeleteReceiptUseCase
import com.financemanage.mobile.feature.transaction.domain.usecase.DeleteRecurringUseCase
import com.financemanage.mobile.feature.transaction.domain.usecase.DeleteTransactionOrTransferUseCase
import com.financemanage.mobile.feature.transaction.domain.usecase.DeleteTransactionUseCase
import com.financemanage.mobile.feature.transaction.domain.usecase.DownloadReceiptUseCase
import com.financemanage.mobile.feature.transaction.domain.usecase.GetRecurringUseCase
import com.financemanage.mobile.feature.transaction.domain.usecase.GetTransactionTotalsUseCase
import com.financemanage.mobile.feature.transaction.domain.usecase.GetTransactionUseCase
import com.financemanage.mobile.feature.transaction.domain.usecase.LoadTransactionFormUseCase
import com.financemanage.mobile.feature.transaction.domain.usecase.ObserveRecentTransactionsUseCase
import com.financemanage.mobile.feature.transaction.domain.usecase.ObserveRecurringUseCase
import com.financemanage.mobile.feature.transaction.domain.usecase.RefreshRecurringUseCase
import com.financemanage.mobile.feature.transaction.domain.usecase.SearchTransactionsUseCase
import com.financemanage.mobile.feature.transaction.domain.usecase.TransactionListUseCases
import com.financemanage.mobile.feature.transaction.domain.usecase.UpdateRecurringUseCase
import com.financemanage.mobile.feature.transaction.domain.usecase.UpdateTransactionUseCase
import com.financemanage.mobile.feature.transaction.domain.usecase.UploadReceiptUseCase
import com.financemanage.mobile.feature.transaction.presentation.OutboxViewModel
import com.financemanage.mobile.feature.transaction.presentation.QuickAddViewModel
import com.financemanage.mobile.feature.transaction.presentation.RecurringEditViewModel
import com.financemanage.mobile.feature.transaction.presentation.RecurringListViewModel
import com.financemanage.mobile.feature.transaction.presentation.TransactionDetailViewModel
import com.financemanage.mobile.feature.transaction.presentation.TransactionListViewModel
import com.financemanage.mobile.feature.transaction.presentation.TransferViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val transactionModule =
    module {
        // Data
        single { TransactionRepositoryImpl(get(), get(), get(), get(), get()) } bind TransactionRepository::class
        single { TransactionOutboxSender(get(), get()) } bind OutboxSender::class
        single { TransferOutboxSender(get(), get()) } bind OutboxSender::class
        // Domain
        factoryOf(::ObserveRecentTransactionsUseCase)
        factoryOf(::SearchTransactionsUseCase)
        factoryOf(::GetTransactionTotalsUseCase)
        factoryOf(::GetTransactionUseCase)
        factoryOf(::CreateTransactionUseCase)
        factoryOf(::UpdateTransactionUseCase)
        factoryOf(::DeleteTransactionUseCase)
        factoryOf(::DeleteTransactionOrTransferUseCase)
        factoryOf(::CreateTransferUseCase)
        factoryOf(::UploadReceiptUseCase)
        factoryOf(::DownloadReceiptUseCase)
        factoryOf(::DeleteReceiptUseCase)
        factoryOf(::ObserveRecurringUseCase)
        factoryOf(::RefreshRecurringUseCase)
        factoryOf(::GetRecurringUseCase)
        factoryOf(::CreateRecurringUseCase)
        factoryOf(::UpdateRecurringUseCase)
        factoryOf(::DeleteRecurringUseCase)
        factoryOf(::LoadTransactionFormUseCase)
        factoryOf(::TransactionListUseCases)
        // Presentation
        viewModel { params ->
            TransactionListViewModel(
                initialFilter =
                    TransactionFilter(
                        walletId = params.values.getOrNull(0) as String?,
                        categoryIds = listOfNotNull(params.values.getOrNull(1) as String?),
                    ),
                transactions = get(),
                observeWallets = get(),
                observeCategories = get(),
                getWallets = get(),
                getCategories = get(),
                observePendingWrites = get(),
                retryPendingWrite = get(),
                discardPendingWrite = get(),
            )
        }
        viewModel { params ->
            QuickAddViewModel(
                initialKind = params.get<String>(0),
                transactionId = params.values.getOrNull(1) as String?,
                loadTransactionForm = get(),
                getTransaction = get(),
                createTransaction = get(),
                updateTransaction = get(),
                createRecurring = get(),
                uploadReceipt = get(),
                rememberFormChoices = get(),
            )
        }
        viewModel { params ->
            TransactionDetailViewModel(params.get<String>(0), get(), get(), get(), get(), get(), get(), get())
        }
        viewModelOf(::TransferViewModel)
        viewModelOf(::RecurringListViewModel)
        viewModel { params -> RecurringEditViewModel(params.get<String>(0), get(), get(), get(), get(), get()) }
        viewModelOf(::OutboxViewModel)
    }
