package com.financemanage.mobile.feature.transaction

import com.financemanage.mobile.core.data.outbox.OutboxSync
import com.financemanage.mobile.core.data.settings.AppSettingsRepository
import com.financemanage.mobile.core.data.shared.CategoryRepository
import com.financemanage.mobile.core.data.shared.ExchangeRateRepository
import com.financemanage.mobile.core.data.shared.ProfileRepository
import com.financemanage.mobile.core.data.shared.WalletRepository
import com.financemanage.mobile.core.network.NetworkJson
import com.financemanage.mobile.core.testing.FakeNetworkMonitor
import com.financemanage.mobile.core.testing.FakeOutboxDao
import com.financemanage.mobile.core.testing.InMemoryPreferencesDataStore
import com.financemanage.mobile.core.testing.MockBackend
import com.financemanage.mobile.core.testing.testJsonCache
import com.financemanage.mobile.feature.transaction.data.TransactionOutboxSender
import com.financemanage.mobile.feature.transaction.data.TransactionRepository
import com.financemanage.mobile.feature.transaction.data.TransferOutboxSender
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
    val wallets = WalletRepository(backend.client, cache)
    val categories = CategoryRepository(backend.client, cache)
    val profiles = ProfileRepository(backend.client, cache)
    val rates = ExchangeRateRepository(backend.client, cache)
    val settings = AppSettingsRepository(InMemoryPreferencesDataStore())
    lateinit var repository: TransactionRepository
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
        repository = TransactionRepository(backend.client, cache, NetworkJson, outbox, outboxDao, wallets)
        backend.get("wallets", WALLETS)
        backend.get("categories", CATEGORIES)
        backend.get("transactions", page())
        backend.get("transactions/summary", """[{"currency":"VND","income":0,"expense":45000,"net":-45000}]""")
    }
}
