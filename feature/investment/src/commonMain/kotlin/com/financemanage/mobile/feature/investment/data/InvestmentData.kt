package com.financemanage.mobile.feature.investment.data

import com.financemanage.mobile.core.data.cache.JsonCache
import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.AsyncStatus
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.PollResult
import com.financemanage.mobile.core.model.onSuccess
import com.financemanage.mobile.core.model.serialization.MoneySerializer
import com.financemanage.mobile.core.network.api
import com.financemanage.mobile.core.network.error.apiCall
import com.financemanage.mobile.core.network.pending.PendingPoller
import com.financemanage.mobile.feature.investment.domain.model.AssetDto
import com.financemanage.mobile.feature.investment.domain.model.AssetRequest
import com.financemanage.mobile.feature.investment.domain.model.IncomeRequest
import com.financemanage.mobile.feature.investment.domain.model.PortfolioDto
import com.financemanage.mobile.feature.investment.domain.model.TradeDto
import com.financemanage.mobile.feature.investment.domain.model.TradeKind
import com.financemanage.mobile.feature.investment.domain.model.TradeRequest
import com.financemanage.mobile.feature.investment.domain.repository.InvestmentRepository
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

@Serializable
private data class PriceRequest(
    @Serializable(with = MoneySerializer::class) val price: BigDecimal,
    val date: LocalDate? = null,
)

class InvestmentRepositoryImpl(
    private val client: HttpClient,
    private val cache: JsonCache,
    private val poller: PendingPoller,
) : InvestmentRepository {
    private val listSerializer = ListSerializer(AssetDto.serializer())

    override val assets: Flow<Cached<List<AssetDto>>?> = cache.observe(KEY, listSerializer)

    override suspend fun refresh(): AppResult<List<AssetDto>> =
        apiCall {
            client
                .get(
                    api("investments"),
                ).body<List<AssetDto>>()
        }.onSuccess { cache.put(KEY, listSerializer, it) }

    override suspend fun portfolio(currency: CurrencyCode): AppResult<PortfolioDto> =
        apiCall {
            client
                .get(
                    api("investments/portfolio"),
                ) { parameter("currency", currency.value) }
                .body<PortfolioDto>()
        }

    override suspend fun get(id: String): AppResult<AssetDto> =
        apiCall {
            client.get(api("investments/$id")).body<AssetDto>()
        }

    override suspend fun create(request: AssetRequest): AppResult<AssetDto> =
        apiCall { client.post(api("investments")) { json(request) }.body<AssetDto>() }.onSuccess { refresh() }

    override suspend fun update(
        id: String,
        request: AssetRequest,
    ): AppResult<AssetDto> =
        apiCall {
            client
                .patch(api("investments/$id")) {
                    json(request.copy(type = null, currency = null))
                }.body<AssetDto>()
        }.onSuccess { refresh() }

    override suspend fun delete(id: String): AppResult<Unit> =
        apiCall {
            client.delete(api("investments/$id"))
            Unit
        }.onSuccess { refresh() }

    override suspend fun updatePrice(
        id: String,
        price: BigDecimal,
        date: LocalDate?,
    ): AppResult<AssetDto> =
        apiCall {
            client.put(api("investments/$id/price")) { json(PriceRequest(price, date)) }.body<AssetDto>()
        }

    /** Buy / sell / dividend (201). With a wallet the cash is recorded asynchronously and polled (API §2.1). */
    override suspend fun trade(
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
