package com.financemanage.mobile.feature.investment.data

import com.financemanage.mobile.core.data.cache.Cached
import com.financemanage.mobile.core.data.cache.JsonCache
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.AsyncStatus
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.onSuccess
import com.financemanage.mobile.core.model.serialization.MoneySerializer
import com.financemanage.mobile.core.network.api
import com.financemanage.mobile.core.network.error.apiCall
import com.financemanage.mobile.core.network.pending.PendingPoller
import com.financemanage.mobile.core.network.pending.PollResult
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

val AssetTypes = listOf("STOCK", "FUND", "GOLD", "CRYPTO", "BOND", "REAL_ESTATE", "OTHER")

@Serializable
data class TradeDto(
    val id: String,
    val type: String = "BUY",
    @Serializable(with = MoneySerializer::class) val quantity: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val price: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val fee: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val cash: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val realized: BigDecimal? = null,
    val date: LocalDate? = null,
    val walletId: String? = null,
    val cashStatus: AsyncStatus = AsyncStatus.NONE,
    val transactionId: String? = null,
)

@Serializable
data class AssetDto(
    val id: String,
    val name: String = "",
    val symbol: String? = null,
    val type: String = "OTHER",
    val currency: String? = null,
    val note: String? = null,
    @Serializable(with = MoneySerializer::class) val quantity: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val averageCost: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val costBasis: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val lastPrice: BigDecimal? = null,
    val priceDate: LocalDate? = null,
    val priced: Boolean = false,
    @Serializable(with = MoneySerializer::class) val marketValue: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val unrealized: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val unrealizedPercent: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val realized: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val totalReturn: BigDecimal? = null,
    val trades: List<TradeDto> = emptyList(),
) {
    val currencyCode: CurrencyCode get() = currency?.let { CurrencyCode.parse(it) } ?: CurrencyCode.DEFAULT

    fun money(value: BigDecimal?) = Money(value ?: BigDecimal.ZERO, currencyCode)
}

@Serializable
data class AllocationDto(
    val type: String = "OTHER",
    @Serializable(with = MoneySerializer::class) val value: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val percent: BigDecimal? = null,
)

@Serializable
data class PortfolioDto(
    val currency: String? = null,
    @Serializable(with = MoneySerializer::class) val marketValue: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val costBasis: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val unrealized: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val unrealizedPercent: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val realized: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val totalReturn: BigDecimal? = null,
    val allocation: List<AllocationDto> = emptyList(),
) {
    val currencyCode: CurrencyCode get() = currency?.let { CurrencyCode.parse(it) } ?: CurrencyCode.DEFAULT

    fun money(value: BigDecimal?) = Money(value ?: BigDecimal.ZERO, currencyCode)
}

@Serializable
data class AssetRequest(
    val name: String,
    val symbol: String? = null,
    val type: String? = null,
    val currency: String? = null,
    val note: String? = null,
)

@Serializable
data class TradeRequest(
    @Serializable(with = MoneySerializer::class) val quantity: BigDecimal,
    @Serializable(with = MoneySerializer::class) val price: BigDecimal,
    @Serializable(with = MoneySerializer::class) val fee: BigDecimal? = null,
    val walletId: String? = null,
    val date: LocalDate? = null,
)

@Serializable
data class IncomeRequest(
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal,
    val walletId: String? = null,
    val categoryId: String? = null,
    val date: LocalDate? = null,
)

@Serializable
private data class PriceRequest(
    @Serializable(with = MoneySerializer::class) val price: BigDecimal,
    val date: LocalDate? = null,
)

enum class TradeKind(
    val path: String,
) {
    BUY("buys"),
    SELL("sells"),
    INCOME("incomes"),
}

class InvestmentRepository(
    private val client: HttpClient,
    private val cache: JsonCache,
    private val poller: PendingPoller,
) {
    private val listSerializer = ListSerializer(AssetDto.serializer())

    val assets: Flow<Cached<List<AssetDto>>?> = cache.observe(KEY, listSerializer)

    suspend fun refresh(): AppResult<List<AssetDto>> =
        apiCall {
            client
                .get(
                    api("investments"),
                ).body<List<AssetDto>>()
        }.onSuccess { cache.put(KEY, listSerializer, it) }

    suspend fun portfolio(currency: CurrencyCode): AppResult<PortfolioDto> =
        apiCall {
            client
                .get(
                    api("investments/portfolio"),
                ) { parameter("currency", currency.value) }
                .body<PortfolioDto>()
        }

    suspend fun get(id: String): AppResult<AssetDto> = apiCall { client.get(api("investments/$id")).body<AssetDto>() }

    suspend fun create(request: AssetRequest): AppResult<AssetDto> =
        apiCall { client.post(api("investments")) { json(request) }.body<AssetDto>() }.onSuccess { refresh() }

    suspend fun update(
        id: String,
        request: AssetRequest,
    ): AppResult<AssetDto> =
        apiCall {
            client
                .patch(api("investments/$id")) {
                    json(request.copy(type = null, currency = null))
                }.body<AssetDto>()
        }.onSuccess { refresh() }

    suspend fun delete(id: String): AppResult<Unit> =
        apiCall {
            client.delete(api("investments/$id"))
            Unit
        }.onSuccess { refresh() }

    suspend fun updatePrice(
        id: String,
        price: BigDecimal,
        date: LocalDate?,
    ): AppResult<AssetDto> =
        apiCall {
            client.put(api("investments/$id/price")) { json(PriceRequest(price, date)) }.body<AssetDto>()
        }

    /** Buy / sell / dividend (201). With a wallet the cash is recorded asynchronously and polled (API §2.1). */
    suspend fun trade(
        id: String,
        kind: TradeKind,
        body: Any,
    ): AppResult<PollResult<TradeDto>> {
        val created =
            apiCall {
                client
                    .post(api("investments/$id/${kind.path}")) {
                        contentType(ContentType.Application.Json)
                        when (body) {
                            is TradeRequest -> setBody(body)
                            is IncomeRequest -> setBody(body)
                        }
                    }.body<TradeDto>()
            }
        if (created is AppResult.Failure) return created
        val trade = (created as AppResult.Success).value
        val result =
            if (trade.cashStatus != AsyncStatus.PENDING) {
                PollResult.Settled(trade)
            } else {
                poller.poll(
                    fetch = {
                        when (val asset = get(id)) {
                            is AppResult.Failure -> asset
                            is AppResult.Success ->
                                asset.value.trades
                                    .firstOrNull { it.id == trade.id }
                                    ?.let { AppResult.Success(it) }
                                    ?: AppResult.Failure(AppError.Unexpected())
                        }
                    },
                    isPending = { it.cashStatus.isPending },
                )
            }
        refresh()
        return AppResult.Success(result)
    }

    private inline fun <reified T> HttpRequestBuilder.json(body: T) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private companion object {
        const val KEY = "investments"
    }
}
