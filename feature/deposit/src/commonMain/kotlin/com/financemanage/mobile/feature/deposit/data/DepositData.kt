package com.financemanage.mobile.feature.deposit.data

import com.financemanage.mobile.core.data.cache.Cached
import com.financemanage.mobile.core.data.cache.JsonCache
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.AsyncStatus
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.onSuccess
import com.financemanage.mobile.core.model.serialization.MoneySerializer
import com.financemanage.mobile.core.network.api
import com.financemanage.mobile.core.network.error.apiCall
import com.financemanage.mobile.core.network.pending.PendingPoller
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

@Serializable
data class DepositTxDto(
    val id: String,
    val type: String = "",
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val occurredOn: LocalDate? = null,
)

@Serializable
data class DepositDto(
    val id: String,
    val institutionName: String = "",
    val accountNumber: String? = null,
    val type: String = "TERM",
    @Serializable(with = MoneySerializer::class) val principal: BigDecimal? = null,
    val currency: String? = null,
    @Serializable(with = MoneySerializer::class) val annualRate: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val earlyWithdrawalRate: BigDecimal? = null,
    val termMonths: Int? = null,
    val openDate: LocalDate? = null,
    val maturityDate: LocalDate? = null,
    val interestPaymentMethod: String? = null,
    val rolloverPolicy: String? = null,
    val calculationMethod: String? = null,
    val status: String = "ACTIVE",
    val note: String? = null,
    val settledOn: LocalDate? = null,
    @Serializable(with = MoneySerializer::class) val settledAmount: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val accruedInterest: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val expectedInterestAtMaturity: BigDecimal? = null,
    val transactions: List<DepositTxDto> = emptyList(),
    val fundingWalletId: String? = null,
    val fundingStatus: AsyncStatus? = null,
    val payoutWalletId: String? = null,
    val payoutStatus: AsyncStatus? = null,
) {
    val currencyCode: CurrencyCode get() = currency?.let { CurrencyCode.parse(it) } ?: CurrencyCode.DEFAULT

    fun money(value: BigDecimal?) = Money(value ?: BigDecimal.ZERO, currencyCode)

    val anyPending: Boolean get() = fundingStatus == AsyncStatus.PENDING || payoutStatus == AsyncStatus.PENDING
}

@Serializable
data class CreateDepositRequest(
    val institutionName: String,
    val accountNumber: String? = null,
    val type: String,
    @Serializable(with = MoneySerializer::class) val principal: BigDecimal,
    val currency: String? = null,
    @Serializable(with = MoneySerializer::class) val annualRate: BigDecimal,
    @Serializable(with = MoneySerializer::class) val earlyWithdrawalRate: BigDecimal? = null,
    val termMonths: Int? = null,
    val openDate: LocalDate,
    val interestPaymentMethod: String? = null,
    val rolloverPolicy: String? = null,
    val calculationMethod: String? = null,
    val note: String? = null,
    val fromWalletId: String? = null,
)

@Serializable
data class DepositPreviewDto(
    val maturityDate: LocalDate? = null,
    @Serializable(with = MoneySerializer::class) val expectedInterest: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val totalAtMaturity: BigDecimal? = null,
    val currency: String? = null,
)

@Serializable
data class SettlementQuoteDto(
    val date: LocalDate? = null,
    @Serializable(with = MoneySerializer::class) val principal: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val interest: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val total: BigDecimal? = null,
    val early: Boolean = false,
    @Serializable(with = MoneySerializer::class) val interestLostByEarlyWithdrawal: BigDecimal? = null,
    val currency: String? = null,
)

@Serializable
private data class SettleRequest(
    val date: LocalDate? = null,
    val toWalletId: String? = null,
)

class DepositRepository(
    private val client: HttpClient,
    private val cache: JsonCache,
    private val poller: PendingPoller,
) {
    private val listSerializer = ListSerializer(DepositDto.serializer())

    val deposits: Flow<Cached<List<DepositDto>>?> = cache.observe(KEY, listSerializer)

    suspend fun refresh(): AppResult<List<DepositDto>> =
        apiCall {
            client
                .get(
                    api("deposits"),
                ).body<List<DepositDto>>()
        }.onSuccess { cache.put(KEY, listSerializer, it) }

    suspend fun get(id: String): AppResult<DepositDto> = apiCall { client.get(api("deposits/$id")).body<DepositDto>() }

    suspend fun preview(request: CreateDepositRequest): AppResult<DepositPreviewDto> =
        apiCall { client.post(api("deposits/preview")) { json(request) }.body<DepositPreviewDto>() }

    suspend fun create(request: CreateDepositRequest): AppResult<DepositDto> =
        apiCall { client.post(api("deposits")) { json(request) }.body<DepositDto>() }.onSuccess { refresh() }

    suspend fun updateNote(
        id: String,
        note: String,
    ): AppResult<DepositDto> =
        apiCall {
            client.patch(api("deposits/$id")) { json(mapOf("note" to note)) }.body<DepositDto>()
        }

    suspend fun quote(
        id: String,
        date: LocalDate?,
    ): AppResult<SettlementQuoteDto> =
        apiCall {
            client
                .get(api("deposits/$id/settlement-quote")) {
                    date?.let { parameter("date", it.toString()) }
                }.body<SettlementQuoteDto>()
        }

    suspend fun settle(
        id: String,
        date: LocalDate?,
        toWalletId: String?,
    ): AppResult<SettlementQuoteDto> =
        apiCall {
            client
                .post(
                    api("deposits/$id/settle"),
                ) { json(SettleRequest(date, toWalletId)) }
                .body<SettlementQuoteDto>()
        }.onSuccess { refresh() }

    /** Polls the wallet side (funding/payout) until it leaves PENDING (API §2.1). */
    suspend fun awaitWallet(id: String) = poller.poll(fetch = { get(id) }, isPending = { it.anyPending })

    suspend fun delete(id: String): AppResult<Unit> =
        apiCall {
            client.delete(api("deposits/$id"))
            Unit
        }.onSuccess { refresh() }

    private inline fun <reified T> HttpRequestBuilder.json(body: T) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private companion object {
        const val KEY = "deposits"
    }
}
