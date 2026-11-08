package com.financemanage.mobile.feature.deposit.domain.repository

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.PollResult
import com.financemanage.mobile.feature.deposit.domain.model.CreateDepositRequest
import com.financemanage.mobile.feature.deposit.domain.model.DepositDto
import com.financemanage.mobile.feature.deposit.domain.model.DepositPreviewDto
import com.financemanage.mobile.feature.deposit.domain.model.SettlementQuoteDto
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Term deposits (API §12). */
interface DepositRepository {
    val deposits: Flow<Cached<List<DepositDto>>?>

    suspend fun refresh(): AppResult<List<DepositDto>>

    suspend fun get(id: String): AppResult<DepositDto>

    suspend fun preview(request: CreateDepositRequest): AppResult<DepositPreviewDto>

    suspend fun create(request: CreateDepositRequest): AppResult<DepositDto>

    suspend fun updateNote(
        id: String,
        note: String,
    ): AppResult<DepositDto>

    suspend fun quote(
        id: String,
        date: LocalDate?,
    ): AppResult<SettlementQuoteDto>

    suspend fun settle(
        id: String,
        date: LocalDate?,
        toWalletId: String?,
    ): AppResult<SettlementQuoteDto>

    /** Polls the wallet side (funding/payout) until it leaves PENDING (API §2.1). */
    suspend fun awaitWallet(id: String): PollResult<DepositDto>

    suspend fun delete(id: String): AppResult<Unit>
}
