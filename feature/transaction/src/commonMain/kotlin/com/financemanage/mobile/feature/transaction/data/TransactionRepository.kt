package com.financemanage.mobile.feature.transaction.data

import com.financemanage.mobile.core.data.cache.Cached
import com.financemanage.mobile.core.data.cache.JsonCache
import com.financemanage.mobile.core.data.outbox.OutboxDao
import com.financemanage.mobile.core.data.outbox.OutboxEntity
import com.financemanage.mobile.core.data.outbox.OutboxSender
import com.financemanage.mobile.core.data.outbox.OutboxSync
import com.financemanage.mobile.core.data.shared.WalletRepository
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.map
import com.financemanage.mobile.core.model.onSuccess
import com.financemanage.mobile.core.network.IdempotencyKey
import com.financemanage.mobile.core.network.api
import com.financemanage.mobile.core.network.error.apiCall
import com.financemanage.mobile.core.network.idempotencyKey
import com.financemanage.mobile.feature.transaction.domain.Recurring
import com.financemanage.mobile.feature.transaction.domain.Transaction
import com.financemanage.mobile.feature.transaction.domain.TransactionFilter
import com.financemanage.mobile.feature.transaction.domain.TransactionPage
import com.financemanage.mobile.feature.transaction.domain.TransactionTotals
import com.financemanage.mobile.feature.transaction.domain.toDomain
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid

/** Result of a create call: saved on the server, or queued because the device is offline (F21). */
sealed interface WriteOutcome {
    data object Saved : WriteOutcome

    data object Queued : WriteOutcome
}

internal const val OUTBOX_TRANSACTION = "transaction"
internal const val OUTBOX_TRANSFER = "transfer"

class TransactionRepository(
    private val client: HttpClient,
    private val cache: JsonCache,
    private val json: Json,
    private val outbox: OutboxSync,
    private val outboxDao: OutboxDao,
    private val wallets: WalletRepository,
) {
    private val pageSerializer = ListSerializer(TransactionDto.serializer())
    private val recurringSerializer = ListSerializer(RecurringDto.serializer())
    private val attachmentCache = mutableMapOf<String, ByteArray>()

    /** First page of the unfiltered list, cached for offline display. */
    val recentTransactions: Flow<Cached<List<Transaction>>?> =
        cache.observe(KEY_FIRST_PAGE, pageSerializer).map { cached ->
            cached?.let { Cached(it.value.map(TransactionDto::toDomain), it.updatedAt) }
        }

    /** Writes waiting to be sent ("Waiting to send"). */
    val pendingWrites: Flow<List<OutboxEntity>> = outboxDao.observeAll()

    suspend fun search(
        filter: TransactionFilter,
        page: Int,
        size: Int,
    ): AppResult<TransactionPage> =
        apiCall {
            client
                .get(api("transactions")) {
                    applyFilter(filter)
                    parameter("page", page)
                    parameter("size", size)
                }.body<TransactionPageDto>()
        }.onSuccess { dto ->
            if (filter.isEmpty && page == 0) cache.put(KEY_FIRST_PAGE, pageSerializer, dto.items)
        }.map { dto -> TransactionPage(dto.items.map(TransactionDto::toDomain), dto.page, dto.totalPages) }

    suspend fun summary(filter: TransactionFilter): AppResult<List<TransactionTotals>> =
        apiCall { client.get(api("transactions/summary")) { applyFilter(filter) }.body<List<TotalsDto>>() }
            .map { list -> list.map(TotalsDto::toDomain) }

    suspend fun get(id: String): AppResult<Transaction> =
        apiCall { client.get(api("transactions/$id")).body<TransactionDto>() }.map(TransactionDto::toDomain)

    /**
     * Records an income/expense. When the device is offline the write is queued with its Idempotency-Key and sent
     * later, so retrying never creates a duplicate.
     */
    suspend fun create(
        request: CreateTransactionRequest,
        key: IdempotencyKey,
        summary: String,
    ): AppResult<Pair<WriteOutcome, Transaction?>> =
        when (val result = sendTransaction(request, key)) {
            is AppResult.Success -> {
                afterWrite()
                AppResult.Success(WriteOutcome.Saved to result.value.toDomain())
            }
            is AppResult.Failure ->
                if (result.error is AppError.Network) {
                    outbox.enqueue(
                        OutboxEntity(
                            id = Uuid.random().toString(),
                            kind = OUTBOX_TRANSACTION,
                            payload = json.encodeToString(CreateTransactionRequest.serializer(), request),
                            idempotencyKey = key.value,
                            summary = summary,
                        ),
                    )
                    AppResult.Success(WriteOutcome.Queued to null)
                } else {
                    result
                }
        }

    suspend fun update(
        id: String,
        request: UpdateTransactionRequest,
    ): AppResult<Transaction> =
        apiCall {
            client
                .patch(api("transactions/$id")) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body<TransactionDto>()
        }.onSuccess { afterWrite() }.map(TransactionDto::toDomain)

    suspend fun delete(id: String): AppResult<Unit> =
        apiCall {
            client.delete(api("transactions/$id"))
            Unit
        }.onSuccess { afterWrite() }

    suspend fun transfer(
        request: TransferRequest,
        key: IdempotencyKey,
        summary: String,
    ): AppResult<WriteOutcome> =
        when (val result = sendTransfer(request, key)) {
            is AppResult.Success -> {
                afterWrite()
                AppResult.Success(WriteOutcome.Saved)
            }
            is AppResult.Failure ->
                if (result.error is AppError.Network) {
                    outbox.enqueue(
                        OutboxEntity(
                            id = Uuid.random().toString(),
                            kind = OUTBOX_TRANSFER,
                            payload = json.encodeToString(TransferRequest.serializer(), request),
                            idempotencyKey = key.value,
                            summary = summary,
                        ),
                    )
                    AppResult.Success(WriteOutcome.Queued)
                } else {
                    result
                }
        }

    suspend fun deleteTransfer(transferId: String): AppResult<Unit> =
        apiCall {
            client.delete(api("transfers/$transferId"))
            Unit
        }.onSuccess { afterWrite() }

    internal suspend fun sendTransaction(
        request: CreateTransactionRequest,
        key: IdempotencyKey,
    ): AppResult<TransactionDto> =
        apiCall {
            client
                .post(api("transactions")) {
                    contentType(ContentType.Application.Json)
                    idempotencyKey(key)
                    setBody(request)
                }.body<TransactionDto>()
        }

    internal suspend fun sendTransfer(
        request: TransferRequest,
        key: IdempotencyKey,
    ): AppResult<TransferResponseDto> =
        apiCall {
            client
                .post(api("transfers")) {
                    contentType(ContentType.Application.Json)
                    idempotencyKey(key)
                    setBody(request)
                }.body<TransferResponseDto>()
        }

    /** Balances and the cached first page change after every write. */
    internal suspend fun afterWrite() {
        wallets.refresh()
        search(TransactionFilter(), page = 0, size = FIRST_PAGE_SIZE)
    }

    // ------------------------------------------------------------ Receipt photos (F10)

    suspend fun uploadAttachment(
        transactionId: String,
        bytes: ByteArray,
        fileName: String,
        contentType: String,
    ): AppResult<AttachmentDto> =
        apiCall {
            client
                .submitFormWithBinaryData(
                    url = api("transactions/$transactionId/attachments"),
                    formData =
                        formData {
                            append(
                                "file",
                                bytes,
                                Headers.build {
                                    append(HttpHeaders.ContentType, contentType)
                                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                                },
                            )
                        },
                ).body<AttachmentDto>()
        }

    /** Downloads with the Authorization header (image libraries would not send it). Kept in memory. */
    suspend fun downloadAttachment(id: String): AppResult<ByteArray> {
        attachmentCache[id]?.let { return AppResult.Success(it) }
        return apiCall { client.get(api("attachments/$id")).body<ByteArray>() }
            .onSuccess { bytes -> if (attachmentCache.size < MAX_CACHED_ATTACHMENTS) attachmentCache[id] = bytes }
    }

    suspend fun deleteAttachment(id: String): AppResult<Unit> =
        apiCall {
            client.delete(api("attachments/$id"))
            Unit
        }.onSuccess { attachmentCache.remove(id) }

    // ------------------------------------------------------------ Recurring (F09)

    val recurring: Flow<Cached<List<Recurring>>?> =
        cache.observe(KEY_RECURRING, recurringSerializer).map { cached ->
            cached?.let { Cached(it.value.map(RecurringDto::toDomain), it.updatedAt) }
        }

    suspend fun refreshRecurring(): AppResult<List<Recurring>> =
        apiCall { client.get(api("recurring-transactions")).body<List<RecurringDto>>() }
            .onSuccess { cache.put(KEY_RECURRING, recurringSerializer, it) }
            .map { list -> list.map(RecurringDto::toDomain) }

    suspend fun getRecurring(id: String): AppResult<Recurring> =
        apiCall { client.get(api("recurring-transactions/$id")).body<RecurringDto>() }.map(RecurringDto::toDomain)

    suspend fun createRecurring(request: CreateRecurringRequest): AppResult<Recurring> =
        apiCall {
            client
                .post(api("recurring-transactions")) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body<RecurringDto>()
        }.onSuccess { refreshRecurring() }.map(RecurringDto::toDomain)

    suspend fun updateRecurring(
        id: String,
        request: UpdateRecurringRequest,
    ): AppResult<Recurring> =
        apiCall {
            client
                .patch(api("recurring-transactions/$id")) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body<RecurringDto>()
        }.onSuccess { refreshRecurring() }.map(RecurringDto::toDomain)

    suspend fun deleteRecurring(id: String): AppResult<Unit> =
        apiCall {
            client.delete(api("recurring-transactions/$id"))
            Unit
        }.onSuccess { refreshRecurring() }

    // ------------------------------------------------------------ Outbox

    suspend fun retryPending(entry: OutboxEntity) = outbox.retry(entry)

    suspend fun discardPending(id: String) = outbox.discard(id)

    private fun HttpRequestBuilder.applyFilter(filter: TransactionFilter) {
        filter.walletId?.let { parameter("walletId", it) }
        filter.categoryIds.forEach { parameter("categoryId", it) }
        filter.types.forEach { parameter("type", it.name) }
        filter.from?.let { parameter("from", it.toString()) }
        filter.to?.let { parameter("to", it.toString()) }
        filter.minAmount?.let { parameter("minAmount", it.toStringExpanded()) }
        filter.maxAmount?.let { parameter("maxAmount", it.toStringExpanded()) }
        filter.query
            .trim()
            .takeIf { it.isNotEmpty() }
            ?.let { parameter("q", it) }
    }

    companion object {
        const val PAGE_SIZE = 30
        private const val FIRST_PAGE_SIZE = 30
        private const val MAX_CACHED_ATTACHMENTS = 40
        private const val KEY_FIRST_PAGE = "transactions:first"
        private const val KEY_RECURRING = "recurring"
    }
}

/** Sends queued income/expense transactions (F21). */
internal class TransactionOutboxSender(
    private val repository: TransactionRepository,
    private val json: Json,
) : OutboxSender {
    override val kind: String = OUTBOX_TRANSACTION

    override suspend fun send(entry: OutboxEntity): AppResult<Unit> {
        val request = json.decodeFromString(CreateTransactionRequest.serializer(), entry.payload)
        return repository
            .sendTransaction(request, IdempotencyKey(entry.idempotencyKey))
            .map { }
            .onSuccess { repository.afterWrite() }
    }
}

/** Sends queued transfers (F21). */
internal class TransferOutboxSender(
    private val repository: TransactionRepository,
    private val json: Json,
) : OutboxSender {
    override val kind: String = OUTBOX_TRANSFER

    override suspend fun send(entry: OutboxEntity): AppResult<Unit> {
        val request = json.decodeFromString(TransferRequest.serializer(), entry.payload)
        return repository
            .sendTransfer(request, IdempotencyKey(entry.idempotencyKey))
            .map { }
            .onSuccess { repository.afterWrite() }
    }
}
