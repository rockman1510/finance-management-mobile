package com.financemanage.mobile.feature.loan.domain.repository

import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.IdempotencyKey
import com.financemanage.mobile.feature.loan.domain.model.CreateLoanRequest
import com.financemanage.mobile.feature.loan.domain.model.LoanDetailDto
import com.financemanage.mobile.feature.loan.domain.model.LoanSummaryDto
import com.financemanage.mobile.feature.loan.domain.model.RepaymentDto
import com.financemanage.mobile.feature.loan.domain.model.SchedulePreviewDto
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate

/** Loans and their repayment schedules (API §13). */
interface LoanRepository {
    suspend fun list(): AppResult<List<LoanSummaryDto>>

    suspend fun get(id: String): AppResult<LoanDetailDto>

    suspend fun preview(request: CreateLoanRequest): AppResult<SchedulePreviewDto>

    suspend fun create(request: CreateLoanRequest): AppResult<LoanDetailDto>

    suspend fun delete(id: String): AppResult<Unit>

    /** Scheduled repayment (+ Idempotency-Key): interest before principal, oldest installment first. */
    suspend fun repay(
        id: String,
        amount: BigDecimal,
        paidOn: LocalDate?,
        key: IdempotencyKey,
    ): AppResult<RepaymentDto>

    suspend fun prepay(
        id: String,
        principal: BigDecimal,
        paidOn: LocalDate?,
        key: IdempotencyKey,
    ): AppResult<RepaymentDto>

    suspend fun changeRate(
        id: String,
        effectiveFrom: LocalDate,
        rate: BigDecimal,
    ): AppResult<LoanDetailDto>
}
