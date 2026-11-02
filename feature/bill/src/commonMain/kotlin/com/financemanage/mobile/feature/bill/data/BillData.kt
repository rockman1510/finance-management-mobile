package com.financemanage.mobile.feature.bill.data

import com.financemanage.mobile.core.data.cache.Cached
import com.financemanage.mobile.core.data.cache.JsonCache
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.AsyncStatus
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.domain.Frequency
import com.financemanage.mobile.core.model.map
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

@Serializable
data class CreateBillRequest(
    val name: String,
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal,
    val walletId: String,
    val categoryId: String,
    val frequency: Frequency,
    val interval: Int? = null,
    val firstDueDate: LocalDate,
    val endDate: LocalDate? = null,
    val remindDaysBefore: Int? = null,
    val note: String? = null,
)

@Serializable
data class UpdateBillRequest(
    val name: String? = null,
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val walletId: String? = null,
    val categoryId: String? = null,
    val endDate: LocalDate? = null,
    val remindDaysBefore: Int? = null,
    val note: String? = null,
    val active: Boolean? = null,
)

@Serializable
data class PayBillRequest(
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val walletId: String? = null,
    val paidOn: LocalDate? = null,
)

data class Payment(
    val id: String,
    val dueDate: LocalDate?,
    val amount: Money,
    val paidOn: LocalDate?,
    val status: AsyncStatus,
    val transactionId: String?,
    val failureReason: String?,
)

data class Bill(
    val id: String,
    val name: String,
    val amount: Money,
    val walletId: String?,
    val categoryId: String?,
    val frequency: Frequency,
    val interval: Int,
    val firstDueDate: LocalDate?,
    val endDate: LocalDate?,
    val remindDaysBefore: Int,
    val note: String?,
    val nextDueDate: LocalDate?,
    val daysUntilDue: Long?,
    val overdue: Boolean,
    val active: Boolean,
    val payments: List<Payment>,
) {
    val dueSoon: Boolean get() = !overdue && (daysUntilDue ?: Long.MAX_VALUE) <= remindDaysBefore
}

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

class BillRepository(
    private val client: HttpClient,
    private val cache: JsonCache,
    private val poller: PendingPoller,
) {
    private val listSerializer = ListSerializer(BillDto.serializer())

    val bills: Flow<Cached<List<Bill>>?> =
        cache.observe(KEY, listSerializer).map { cached ->
            cached?.let { Cached(it.value.map(BillDto::toDomain), it.updatedAt) }
        }

    suspend fun refresh(): AppResult<List<Bill>> =
        apiCall { client.get(api("bills")).body<List<BillDto>>() }
            .onSuccess { cache.put(KEY, listSerializer, it) }
            .map { list -> list.map(BillDto::toDomain) }

    suspend fun upcoming(days: Int): AppResult<List<Bill>> =
        apiCall { client.get(api("bills/upcoming")) { parameter("days", days) }.body<List<BillDto>>() }.map { list ->
            list.map(BillDto::toDomain)
        }

    suspend fun get(id: String): AppResult<Bill> =
        apiCall {
            client.get(api("bills/$id")).body<BillDto>()
        }.map(BillDto::toDomain)

    suspend fun create(request: CreateBillRequest): AppResult<Bill> =
        apiCall {
            client
                .post(
                    api("bills"),
                ) { json(request) }
                .body<BillDto>()
        }.onSuccess { refresh() }.map(BillDto::toDomain)

    suspend fun update(
        id: String,
        request: UpdateBillRequest,
    ): AppResult<Bill> =
        apiCall {
            client.patch(api("bills/$id")) { json(request) }.body<BillDto>()
        }.onSuccess { refresh() }.map(BillDto::toDomain)

    suspend fun delete(id: String): AppResult<Unit> =
        apiCall {
            client.delete(api("bills/$id"))
            Unit
        }.onSuccess { refresh() }

    /**
     * Pays the period currently due (202). The expense is recorded asynchronously: the payment is polled until it
     * leaves PENDING (API §2.1).
     */
    suspend fun pay(
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

    suspend fun skip(id: String): AppResult<Unit> =
        apiCall {
            client.post(api("bills/$id/skip"))
            Unit
        }.onSuccess { refresh() }

    private fun HttpRequestBuilder.json(body: Any) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private companion object {
        const val KEY = "bills"
    }
}
