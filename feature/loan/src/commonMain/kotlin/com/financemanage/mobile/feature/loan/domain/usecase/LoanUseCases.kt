package com.financemanage.mobile.feature.loan.domain.usecase

import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.IdempotencyKey
import com.financemanage.mobile.feature.loan.domain.model.CreateLoanRequest
import com.financemanage.mobile.feature.loan.domain.model.LoanDetailDto
import com.financemanage.mobile.feature.loan.domain.model.LoanSummaryDto
import com.financemanage.mobile.feature.loan.domain.model.RepaymentDto
import com.financemanage.mobile.feature.loan.domain.model.SchedulePreviewDto
import com.financemanage.mobile.feature.loan.domain.repository.LoanRepository
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate

class GetLoansUseCase(
    private val repository: LoanRepository,
) {
    suspend operator fun invoke(): AppResult<List<LoanSummaryDto>> = repository.list()
}

class GetLoanUseCase(
    private val repository: LoanRepository,
) {
    suspend operator fun invoke(id: String): AppResult<LoanDetailDto> = repository.get(id)
}

/** Repayment schedule preview of a loan before it is created. */
class PreviewLoanScheduleUseCase(
    private val repository: LoanRepository,
) {
    suspend operator fun invoke(request: CreateLoanRequest): AppResult<SchedulePreviewDto> = repository.preview(request)
}

class CreateLoanUseCase(
    private val repository: LoanRepository,
) {
    suspend operator fun invoke(request: CreateLoanRequest): AppResult<LoanDetailDto> = repository.create(request)
}

class DeleteLoanUseCase(
    private val repository: LoanRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.delete(id)
}

/** Pays the next installment (Idempotency-Key required). */
class RepayLoanUseCase(
    private val repository: LoanRepository,
) {
    suspend operator fun invoke(
        id: String,
        amount: BigDecimal,
        paidOn: LocalDate?,
        key: IdempotencyKey,
    ): AppResult<RepaymentDto> = repository.repay(id, amount, paidOn, key)
}

/** Pays part of the principal early (Idempotency-Key required). */
class PrepayLoanUseCase(
    private val repository: LoanRepository,
) {
    suspend operator fun invoke(
        id: String,
        principal: BigDecimal,
        paidOn: LocalDate?,
        key: IdempotencyKey,
    ): AppResult<RepaymentDto> = repository.prepay(id, principal, paidOn, key)
}

/** Changes the interest rate from a date (floating-rate loans). */
class ChangeLoanRateUseCase(
    private val repository: LoanRepository,
) {
    suspend operator fun invoke(
        id: String,
        effectiveFrom: LocalDate,
        rate: BigDecimal,
    ): AppResult<LoanDetailDto> = repository.changeRate(id, effectiveFrom, rate)
}
