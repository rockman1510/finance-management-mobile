package com.financemanage.mobile.feature.transaction.domain.repository

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.IdempotencyKey
import com.financemanage.mobile.feature.transaction.domain.model.AttachmentDto
import com.financemanage.mobile.feature.transaction.domain.model.CreateRecurringRequest
import com.financemanage.mobile.feature.transaction.domain.model.CreateTransactionRequest
import com.financemanage.mobile.feature.transaction.domain.model.Recurring
import com.financemanage.mobile.feature.transaction.domain.model.Transaction
import com.financemanage.mobile.feature.transaction.domain.model.TransactionFilter
import com.financemanage.mobile.feature.transaction.domain.model.TransactionPage
import com.financemanage.mobile.feature.transaction.domain.model.TransactionTotals
import com.financemanage.mobile.feature.transaction.domain.model.TransferRequest
import com.financemanage.mobile.feature.transaction.domain.model.UpdateRecurringRequest
import com.financemanage.mobile.feature.transaction.domain.model.UpdateTransactionRequest
import com.financemanage.mobile.feature.transaction.domain.model.WriteOutcome
import kotlinx.coroutines.flow.Flow

/** Transactions, transfers, receipts and recurring transactions (API §7–§8). */
interface TransactionRepository {
    /** First page of the unfiltered list, cached for offline display. */
    val recentTransactions: Flow<Cached<List<Transaction>>?>

    suspend fun search(
        filter: TransactionFilter,
        page: Int,
        size: Int,
    ): AppResult<TransactionPage>

    suspend fun summary(filter: TransactionFilter): AppResult<List<TransactionTotals>>

    suspend fun get(id: String): AppResult<Transaction>

    /**
     * Records an income/expense. When the device is offline the write is queued with its Idempotency-Key and sent
     * later, so retrying never creates a duplicate.
     */
    suspend fun create(
        request: CreateTransactionRequest,
        key: IdempotencyKey,
        summary: String,
    ): AppResult<Pair<WriteOutcome, Transaction?>>

    suspend fun update(
        id: String,
        request: UpdateTransactionRequest,
    ): AppResult<Transaction>

    suspend fun delete(id: String): AppResult<Unit>

    suspend fun transfer(
        request: TransferRequest,
        key: IdempotencyKey,
        summary: String,
    ): AppResult<WriteOutcome>

    suspend fun deleteTransfer(transferId: String): AppResult<Unit>

    suspend fun uploadAttachment(
        transactionId: String,
        bytes: ByteArray,
        fileName: String,
        contentType: String,
    ): AppResult<AttachmentDto>

    /** Downloads with the Authorization header (image libraries would not send it). Kept in memory. */
    suspend fun downloadAttachment(id: String): AppResult<ByteArray>

    suspend fun deleteAttachment(id: String): AppResult<Unit>

    val recurring: Flow<Cached<List<Recurring>>?>

    suspend fun refreshRecurring(): AppResult<List<Recurring>>

    suspend fun getRecurring(id: String): AppResult<Recurring>

    suspend fun createRecurring(request: CreateRecurringRequest): AppResult<Recurring>

    suspend fun updateRecurring(
        id: String,
        request: UpdateRecurringRequest,
    ): AppResult<Recurring>

    suspend fun deleteRecurring(id: String): AppResult<Unit>
}
