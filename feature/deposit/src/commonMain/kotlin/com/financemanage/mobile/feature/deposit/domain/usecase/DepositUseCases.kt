package com.financemanage.mobile.feature.deposit.domain.usecase

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.PollResult
import com.financemanage.mobile.feature.deposit.domain.model.CreateDepositRequest
import com.financemanage.mobile.feature.deposit.domain.model.DepositDto
import com.financemanage.mobile.feature.deposit.domain.model.DepositPreviewDto
import com.financemanage.mobile.feature.deposit.domain.model.SettlementQuoteDto
import com.financemanage.mobile.feature.deposit.domain.repository.DepositRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Observes the cached term deposits. */
class ObserveDepositsUseCase(
    private val repository: DepositRepository,
) {
    operator fun invoke(): Flow<Cached<List<DepositDto>>?> = repository.deposits
}

class RefreshDepositsUseCase(
    private val repository: DepositRepository,
) {
    suspend operator fun invoke(): AppResult<List<DepositDto>> = repository.refresh()
}

class GetDepositUseCase(
    private val repository: DepositRepository,
) {
    suspend operator fun invoke(id: String): AppResult<DepositDto> = repository.get(id)
}

/** Interest and maturity preview of a deposit before it is opened. */
class PreviewDepositUseCase(
    private val repository: DepositRepository,
) {
    suspend operator fun invoke(request: CreateDepositRequest): AppResult<DepositPreviewDto> =
        repository.preview(request)
}

class CreateDepositUseCase(
    private val repository: DepositRepository,
) {
    suspend operator fun invoke(request: CreateDepositRequest): AppResult<DepositDto> = repository.create(request)
}

/** What the user receives when settling the deposit on a date. */
class GetSettlementQuoteUseCase(
    private val repository: DepositRepository,
) {
    suspend operator fun invoke(
        id: String,
        date: LocalDate?,
    ): AppResult<SettlementQuoteDto> = repository.quote(id, date)
}

/** Settles (closes) a deposit; the payout is recorded asynchronously. */
class SettleDepositUseCase(
    private val repository: DepositRepository,
) {
    suspend operator fun invoke(
        id: String,
        date: LocalDate?,
        toWalletId: String?,
    ): AppResult<SettlementQuoteDto> = repository.settle(id, date, toWalletId)
}

/** Polls a deposit until its wallet transactions leave PENDING (API §2.1). */
class AwaitDepositRecordedUseCase(
    private val repository: DepositRepository,
) {
    suspend operator fun invoke(id: String): PollResult<DepositDto> = repository.awaitWallet(id)
}

class DeleteDepositUseCase(
    private val repository: DepositRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.delete(id)
}
