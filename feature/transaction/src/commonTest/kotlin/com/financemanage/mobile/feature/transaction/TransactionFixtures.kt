package com.financemanage.mobile.feature.transaction

import com.financemanage.mobile.core.data.outbox.OutboxRepositoryImpl
import com.financemanage.mobile.core.data.outbox.OutboxSync
import com.financemanage.mobile.core.data.settings.AppSettingsRepositoryImpl
import com.financemanage.mobile.core.data.shared.CategoryRepositoryImpl
import com.financemanage.mobile.core.data.shared.ExchangeRateRepositoryImpl
import com.financemanage.mobile.core.data.shared.ProfileRepositoryImpl
import com.financemanage.mobile.core.data.shared.WalletRepositoryImpl
import com.financemanage.mobile.core.domain.usecase.ConvertCurrencyUseCase
import com.financemanage.mobile.core.domain.usecase.DiscardPendingWriteUseCase
import com.financemanage.mobile.core.domain.usecase.GetCachedCategoriesUseCase
import com.financemanage.mobile.core.domain.usecase.GetCachedWalletsUseCase
import com.financemanage.mobile.core.domain.usecase.GetCategoriesUseCase
import com.financemanage.mobile.core.domain.usecase.GetPreferencesUseCase
import com.financemanage.mobile.core.domain.usecase.GetTodayUseCase
import com.financemanage.mobile.core.domain.usecase.GetWalletsUseCase
import com.financemanage.mobile.core.domain.usecase.ObserveCategoriesUseCase
import com.financemanage.mobile.core.domain.usecase.ObservePendingWritesUseCase
import com.financemanage.mobile.core.domain.usecase.ObserveWalletsUseCase
import com.financemanage.mobile.core.domain.usecase.RememberFormChoicesUseCase
import com.financemanage.mobile.core.domain.usecase.RetryPendingWriteUseCase
import com.financemanage.mobile.core.network.NetworkJson
import com.financemanage.mobile.core.testing.FakeNetworkMonitor
import com.financemanage.mobile.core.testing.FakeOutboxDao
import com.financemanage.mobile.core.testing.InMemoryPreferencesDataStore
import com.financemanage.mobile.core.testing.MockBackend
import com.financemanage.mobile.core.testing.testJsonCache
import com.financemanage.mobile.feature.transaction.data.TransactionOutboxSender
import com.financemanage.mobile.feature.transaction.data.TransactionRepositoryImpl
import com.financemanage.mobile.feature.transaction.data.TransferOutboxSender
import com.financemanage.mobile.feature.transaction.domain.model.TransactionFilter
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
import kotlinx.coroutines.CoroutineScope

internal const val CASH = "w-cash"
internal const val USD_CARD = "w-usd"

internal const val WALLETS =
    """[{"id":"$CASH","name":"Tiền mặt","type":"CASH","currency":"VND","initialBalance":0,"balance":500000},
    {"id":"$USD_CARD","name":"Visa USD","type":"CREDIT_CARD","currency":"USD","initialBalance":0,"balance":10.5},
    {"id":"w-old","name":"Cũ","type":"CASH","currency":"VND","balance":0,"archived":true}]"""

internal const val CATEGORIES =
    """[{"id":"food","name":"Ăn uống","type":"EXPENSE"},{"id":"salary","name":"Lương","type":"INCOME"}]"""

internal fun transactionJson(
    id: String = "t1",
    type: String = "EXPENSE",
    amount: String = "45000",
    wallet: String = CASH,
    currency: String = "VND",
    category: String? = "food",
    extra: String = "",
) = """{"id":"$id","walletId":"$wallet","type":"$type","amount":$amount,"currency":"$currency",
    "categoryId":${category?.let { "\"$it\"" } ?: "null"},"occurredOn":"2026-11-01","note":"Phở"$extra}"""

internal fun page(vararg items: String) =
    """{"items":[${items.joinToString(",")}],"page":0,"size":30,"totalElements":${items.size},"totalPages":1}"""

/** Every repository of the transaction feature over one [MockBackend]. */
internal class TransactionTestEnv(
    scope: CoroutineScope,
) {
    val backend = MockBackend()
    val cache = testJsonCache()
    val outboxDao = FakeOutboxDao()
    val network = FakeNetworkMonitor(online = false)
    val wallets = WalletRepositoryImpl(backend.client, cache)
    val categories = CategoryRepositoryImpl(backend.client, cache)
    val profiles = ProfileRepositoryImpl(backend.client, cache)
    val rates = ExchangeRateRepositoryImpl(backend.client, cache)
    val settings = AppSettingsRepositoryImpl(InMemoryPreferencesDataStore())
    lateinit var repository: TransactionRepositoryImpl
    val outbox =
        OutboxSync(
            outboxDao,
            {
                listOf(
                    TransactionOutboxSender(repository, NetworkJson),
                    TransferOutboxSender(repository, NetworkJson),
                )
            },
            network,
            scope,
        )

    init {
        repository = TransactionRepositoryImpl(backend.client, cache, NetworkJson, outbox, wallets)
        backend.get("wallets", WALLETS)
        backend.get("categories", CATEGORIES)
        backend.get("transactions", page())
        backend.get("transactions/summary", """[{"currency":"VND","income":0,"expense":45000,"net":-45000}]""")
    }

    val outboxRepository = OutboxRepositoryImpl(outboxDao, outbox)

    fun listViewModel(filter: TransactionFilter = TransactionFilter()) =
        TransactionListViewModel(
            filter,
            TransactionListUseCases(
                SearchTransactionsUseCase(repository),
                GetTransactionTotalsUseCase(repository),
                ObserveRecentTransactionsUseCase(repository),
                DeleteTransactionUseCase(repository),
            ),
            ObserveWalletsUseCase(wallets),
            ObserveCategoriesUseCase(categories),
            GetWalletsUseCase(wallets),
            GetCategoriesUseCase(categories),
            ObservePendingWritesUseCase(outboxRepository),
            RetryPendingWriteUseCase(outboxRepository),
            DiscardPendingWriteUseCase(outboxRepository),
        )

    fun quickAddViewModel(
        kind: String,
        id: String?,
    ) = QuickAddViewModel(
        kind,
        id,
        LoadTransactionFormUseCase(
            GetWalletsUseCase(wallets),
            GetCategoriesUseCase(categories),
            GetPreferencesUseCase(settings),
            GetTodayUseCase(profiles),
        ),
        GetTransactionUseCase(repository),
        CreateTransactionUseCase(repository),
        UpdateTransactionUseCase(repository),
        CreateRecurringUseCase(repository),
        UploadReceiptUseCase(repository),
        RememberFormChoicesUseCase(settings),
    )

    fun detailViewModel(id: String) =
        TransactionDetailViewModel(
            id,
            GetTransactionUseCase(repository),
            DownloadReceiptUseCase(repository),
            UploadReceiptUseCase(repository),
            DeleteReceiptUseCase(repository),
            DeleteTransactionOrTransferUseCase(repository),
            GetCachedWalletsUseCase(wallets),
            GetCachedCategoriesUseCase(categories),
        )

    fun transferViewModel() =
        TransferViewModel(
            CreateTransferUseCase(repository),
            ConvertCurrencyUseCase(rates),
            GetWalletsUseCase(wallets),
            GetTodayUseCase(profiles),
        )

    fun recurringListViewModel() =
        RecurringListViewModel(
            ObserveRecurringUseCase(repository),
            RefreshRecurringUseCase(repository),
            UpdateRecurringUseCase(repository),
            ObserveWalletsUseCase(wallets),
            ObserveCategoriesUseCase(categories),
        )

    fun recurringEditViewModel(id: String) =
        RecurringEditViewModel(
            id,
            GetRecurringUseCase(repository),
            UpdateRecurringUseCase(repository),
            DeleteRecurringUseCase(repository),
            GetCachedWalletsUseCase(wallets),
            GetCachedCategoriesUseCase(categories),
        )

    fun outboxViewModel() =
        OutboxViewModel(
            ObservePendingWritesUseCase(outboxRepository),
            RetryPendingWriteUseCase(outboxRepository),
            DiscardPendingWriteUseCase(outboxRepository),
        )
}
