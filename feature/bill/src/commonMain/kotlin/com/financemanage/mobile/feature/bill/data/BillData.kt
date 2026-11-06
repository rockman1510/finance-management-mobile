package com.financemanage.mobile.feature.bill.data

import com.financemanage.mobile.core.data.cache.JsonCache
import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.AsyncStatus
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.PollResult
import com.financemanage.mobile.core.model.domain.Frequency
import com.financemanage.mobile.core.model.map
import com.financemanage.mobile.core.model.onSuccess
import com.financemanage.mobile.core.model.serialization.MoneySerializer
import com.financemanage.mobile.core.network.api
import com.financemanage.mobile.core.network.error.apiCall
import com.financemanage.mobile.core.network.pending.PendingPoller
import com.financemanage.mobile.feature.bill.domain.model.Bill
import com.financemanage.mobile.feature.bill.domain.model.CreateBillRequest
import com.financemanage.mobile.feature.bill.domain.model.PayBillRequest
import com.financemanage.mobile.feature.bill.domain.model.Payment
import com.financemanage.mobile.feature.bill.domain.model.UpdateBillRequest
import com.financemanage.mobile.feature.bill.domain.repository.BillRepository
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
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

@Serializable
data class PaymentDto(
    val id: String,
    val dueDate: LocalDate? = null,
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val walletId: String? = null,
    val paidOn: LocalDate? = null,
    val status: AsyncStatus = AsyncStatus.UNKNOWN,
    val transactionId: String? = null,
    val failureReason: String? = null,
)

@Serializable
data class BillDto(
    val id: String,
    val name: String = "",
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val currency: String? = null,
    val walletId: String? = null,
    val categoryId: String? = null,
    val frequency: Frequency = Frequency.UNKNOWN,
    val interval: Int = 1,
    val firstDueDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val remindDaysBefore: Int? = null,
    val note: String? = null,
    val nextDueDate: LocalDate? = null,
    val daysUntilDue: Long? = null,
    val overdue: Boolean = false,
    val active: Boolean = true,
    val payments: List<PaymentDto> = emptyList(),
)

private const val DEFAULT_REMIND_DAYS = 3

fun BillDto.toDomain(): Bill {
    val code = currency?.let { CurrencyCode.parse(it) } ?: CurrencyCode.DEFAULT
    return Bill(
        id = id,
        name = name,
        amount = Money(amount ?: BigDecimal.ZERO, code),
        walletId = walletId,
        categoryId = categoryId,
        frequency = frequency,
        interval = interval,
        firstDueDate = firstDueDate,
        endDate = endDate,
        remindDaysBefore = remindDaysBefore ?: DEFAULT_REMIND_DAYS,
        note = note,
        nextDueDate = nextDueDate,
        daysUntilDue = daysUntilDue,
        overdue = overdue,
        active = active,
        payments =
            payments.map {
                Payment(
                    it.id,
                    it.dueDate,
                    Money(it.amount ?: BigDecimal.ZERO, code),
                    it.paidOn,
                    it.status,
                    it.transactionId,
                    it.failureReason,
                )
            },
    )
}

class BillRepositoryImpl(
    private val client: HttpClient,
    private val cache: JsonCache,
    private val poller: PendingPoller,
) : BillRepository {
    private val listSerializer = ListSerializer(BillDto.serializer())

    override val bills: Flow<Cached<List<Bill>>?> =
        cache.observe(KEY, listSerializer).map { cached ->
            cached?.let { Cached(it.value.map(BillDto::toDomain), it.updatedAt) }
        }

    override suspend fun refresh(): AppResult<List<Bill>> =
        apiCall { client.get(api("bills")).body<List<BillDto>>() }
            .onSuccess { cache.put(KEY, listSerializer, it) }
            .map { list -> list.map(BillDto::toDomain) }

    override suspend fun upcoming(days: Int): AppResult<List<Bill>> =
        apiCall { client.get(api("bills/upcoming")) { parameter("days", days) }.body<List<BillDto>>() }.map { list ->
            list.map(BillDto::toDomain)
        }

    override suspend fun get(id: String): AppResult<Bill> =
        apiCall {
            client.get(api("bills/$id")).body<BillDto>()
        }.map(BillDto::toDomain)

    override suspend fun create(request: CreateBillRequest): AppResult<Bill> =
        apiCall {
            client
                .post(
                    api("bills"),
                ) { json(request) }
                .body<BillDto>()
        }.onSuccess { refresh() }.map(BillDto::toDomain)

    override suspend fun update(
        id: String,
        request: UpdateBillRequest,
    ): AppResult<Bill> =
        apiCall {
            client.patch(api("bills/$id")) { json(request) }.body<BillDto>()
        }.onSuccess { refresh() }.map(BillDto::toDomain)

    override suspend fun delete(id: String): AppResult<Unit> =
        apiCall {
            client.delete(api("bills/$id"))
            Unit
        }.onSuccess { refresh() }

    /**
     * Pays the period currently due (202). The expense is recorded asynchronously: the payment is polled until it
     * leaves PENDING (API §2.1).
     */
    override suspend fun pay(
        id: String,
        request: PayBillRequest,
    ): AppResult<PollResult<Payment>> {
        val accepted = apiCall { client.post(api("bills/$id/pay")) { json(request) }.body<PaymentDto>() }
        if (accepted is AppResult.Failure) return accepted
        val paymentId = (accepted as AppResult.Success).value.id
        val result =
            poller.poll(
                fetch = {
                    when (val bill = get(id)) {
                        is AppResult.Failure -> bill
                        is AppResult.Success ->
                            bill.value.payments
                                .firstOrNull { it.id == paymentId }
                                ?.let { AppResult.Success(it) }
                                ?: AppResult.Failure(AppError.Unexpected())
                    }
                },
                isPending = { it.status.isPending },
            )
        refresh()
        return AppResult.Success(result)
    }

    override suspend fun skip(id: String): AppResult<Unit> =
        apiCall {
            client.post(api("bills/$id/skip"))
            Unit
        }.onSuccess { refresh() }

    private inline fun <reified T> HttpRequestBuilder.json(body: T) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private companion object {
        const val KEY = "bills"
    }
}
