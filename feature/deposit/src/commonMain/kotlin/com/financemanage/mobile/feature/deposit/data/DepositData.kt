package com.financemanage.mobile.feature.deposit.data

import com.financemanage.mobile.core.data.cache.JsonCache
import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.PollResult
import com.financemanage.mobile.core.model.onSuccess
import com.financemanage.mobile.core.network.api
import com.financemanage.mobile.core.network.error.apiCall
import com.financemanage.mobile.core.network.pending.PendingPoller
import com.financemanage.mobile.feature.deposit.domain.model.CreateDepositRequest
import com.financemanage.mobile.feature.deposit.domain.model.DepositDto
import com.financemanage.mobile.feature.deposit.domain.model.DepositPreviewDto
import com.financemanage.mobile.feature.deposit.domain.model.SettlementQuoteDto
import com.financemanage.mobile.feature.deposit.domain.repository.DepositRepository
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
private data class SettleRequest(
    val date: LocalDate? = null,
    val toWalletId: String? = null,
)

class DepositRepositoryImpl(
    private val client: HttpClient,
    private val cache: JsonCache,
    private val poller: PendingPoller,
) : DepositRepository {
    private val listSerializer = ListSerializer(DepositDto.serializer())

    override val deposits: Flow<Cached<List<DepositDto>>?> = cache.observe(KEY, listSerializer)

    override suspend fun refresh(): AppResult<List<DepositDto>> =
        apiCall {
            client
                .get(
                    api("deposits"),
                ).body<List<DepositDto>>()
        }.onSuccess { cache.put(KEY, listSerializer, it) }

    override suspend fun get(id: String): AppResult<DepositDto> =
        apiCall {
            client.get(api("deposits/$id")).body<DepositDto>()
        }

    override suspend fun preview(request: CreateDepositRequest): AppResult<DepositPreviewDto> =
        apiCall { client.post(api("deposits/preview")) { json(request) }.body<DepositPreviewDto>() }

    override suspend fun create(request: CreateDepositRequest): AppResult<DepositDto> =
        apiCall { client.post(api("deposits")) { json(request) }.body<DepositDto>() }.onSuccess { refresh() }

    override suspend fun updateNote(
        id: String,
        note: String,
    ): AppResult<DepositDto> =
        apiCall {
            client.patch(api("deposits/$id")) { json(mapOf("note" to note)) }.body<DepositDto>()
        }

    override suspend fun quote(
        id: String,
        date: LocalDate?,
    ): AppResult<SettlementQuoteDto> =
        apiCall {
            client
                .get(api("deposits/$id/settlement-quote")) {
                    date?.let { parameter("date", it.toString()) }
                }.body<SettlementQuoteDto>()
        }

    override suspend fun settle(
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
    override suspend fun awaitWallet(id: String): PollResult<DepositDto> =
        poller.poll(fetch = { get(id) }, isPending = { it.anyPending })

    override suspend fun delete(id: String): AppResult<Unit> =
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
