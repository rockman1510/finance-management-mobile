package com.financemanage.mobile.feature.loan.data

import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.serialization.MoneySerializer
import com.financemanage.mobile.core.network.IdempotencyKey
import com.financemanage.mobile.core.network.api
import com.financemanage.mobile.core.network.error.apiCall
import com.financemanage.mobile.core.network.idempotencyKey
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class LoanSummaryDto(
    val id: String,
    val counterpartyName: String = "",
    val direction: String = "BORROW",
    val type: String? = null,
    @Serializable(with = MoneySerializer::class) val principal: BigDecimal? = null,
    val currency: String? = null,
    @Serializable(with = MoneySerializer::class) val currentRate: BigDecimal? = null,
    val termMonths: Int? = null,
    val startDate: LocalDate? = null,
    val repaymentMethod: String? = null,
    val status: String = "ACTIVE",
    @Serializable(with = MoneySerializer::class) val outstandingPrincipal: BigDecimal? = null,
    val nextDueDate: LocalDate? = null,
    @Serializable(with = MoneySerializer::class) val nextDueAmount: BigDecimal? = null,
    val overdueInstallments: Int = 0,
    val note: String? = null,
) {
    val currencyCode: CurrencyCode get() = currency?.let { CurrencyCode.parse(it) } ?: CurrencyCode.DEFAULT

    fun money(value: BigDecimal?) = Money(value ?: BigDecimal.ZERO, currencyCode)
}

@Serializable
data class InstallmentDto(
    val id: String? = null,
    val periodNo: Int = 0,
    val dueDate: LocalDate? = null,
    @Serializable(with = MoneySerializer::class) val principalDue: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val interestDue: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val totalDue: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val remaining: BigDecimal? = null,
    val status: String = "PENDING",
)

@Serializable
data class RepaymentDto(
    val id: String,
    val type: String = "REGULAR",
    val paidOn: LocalDate? = null,
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val principalPortion: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val interestPortion: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val feePortion: BigDecimal? = null,
)

@Serializable
data class RatePeriodDto(
    val effectiveFrom: LocalDate? = null,
    @Serializable(with = MoneySerializer::class) val annualRate: BigDecimal? = null,
)

@Serializable
data class LoanDetailDto(
    val summary: LoanSummaryDto,
    @Serializable(with = MoneySerializer::class) val earlyRepaymentFeeRate: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val totalScheduledInterest: BigDecimal? = null,
    val ratePeriods: List<RatePeriodDto> = emptyList(),
    val installments: List<InstallmentDto> = emptyList(),
    val repayments: List<RepaymentDto> = emptyList(),
)

@Serializable
data class SchedulePreviewDto(
    val installments: List<InstallmentDto> = emptyList(),
    @Serializable(with = MoneySerializer::class) val totalInterest: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val totalPayment: BigDecimal? = null,
    val currency: String? = null,
)

@Serializable
data class CreateLoanRequest(
    val counterpartyName: String,
    val direction: String,
    val type: String? = null,
    @Serializable(with = MoneySerializer::class) val principal: BigDecimal,
    val currency: String? = null,
    @Serializable(with = MoneySerializer::class) val annualRate: BigDecimal? = null,
    val termMonths: Int,
    val startDate: LocalDate,
    val repaymentMethod: String,
    @Serializable(with = MoneySerializer::class) val earlyRepaymentFeeRate: BigDecimal? = null,
    val note: String? = null,
)

@Serializable
private data class RepaymentRequest(
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal,
    val paidOn: LocalDate? = null,
)

@Serializable
private data class PrepaymentRequest(
    @Serializable(with = MoneySerializer::class) val principal: BigDecimal,
    val paidOn: LocalDate? = null,
)

@Serializable
private data class RateChangeRequest(
    val effectiveFrom: LocalDate,
    @Serializable(with = MoneySerializer::class) val annualRate: BigDecimal,
)

class LoanRepository(
    private val client: HttpClient,
) {
    suspend fun list(): AppResult<List<LoanSummaryDto>> =
        apiCall { client.get(api("loans")).body<List<LoanSummaryDto>>() }

    suspend fun get(id: String): AppResult<LoanDetailDto> =
        apiCall { client.get(api("loans/$id")).body<LoanDetailDto>() }

    suspend fun preview(request: CreateLoanRequest): AppResult<SchedulePreviewDto> =
        apiCall { client.post(api("loans/preview-schedule")) { json(request) }.body<SchedulePreviewDto>() }

    suspend fun create(request: CreateLoanRequest): AppResult<LoanDetailDto> =
        apiCall { client.post(api("loans")) { json(request) }.body<LoanDetailDto>() }

    suspend fun delete(id: String): AppResult<Unit> =
        apiCall {
            client.delete(api("loans/$id"))
            Unit
        }

    /** Scheduled repayment (+ Idempotency-Key): interest before principal, oldest installment first. */
    suspend fun repay(
        id: String,
        amount: BigDecimal,
        paidOn: LocalDate?,
        key: IdempotencyKey,
    ): AppResult<RepaymentDto> =
        apiCall {
            client
                .post(api("loans/$id/repayments")) {
                    idempotencyKey(key)
                    json(RepaymentRequest(amount, paidOn))
                }.body<RepaymentDto>()
        }

    suspend fun prepay(
        id: String,
        principal: BigDecimal,
        paidOn: LocalDate?,
        key: IdempotencyKey,
    ): AppResult<RepaymentDto> =
        apiCall {
            client
                .post(api("loans/$id/prepayments")) {
                    idempotencyKey(key)
                    json(PrepaymentRequest(principal, paidOn))
                }.body<RepaymentDto>()
        }

    suspend fun changeRate(
        id: String,
        effectiveFrom: LocalDate,
        rate: BigDecimal,
    ): AppResult<LoanDetailDto> =
        apiCall {
            client
                .post(
                    api("loans/$id/rate-changes"),
                ) { json(RateChangeRequest(effectiveFrom, rate)) }
                .body<LoanDetailDto>()
        }

    private inline fun <reified T> HttpRequestBuilder.json(body: T) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }
}
