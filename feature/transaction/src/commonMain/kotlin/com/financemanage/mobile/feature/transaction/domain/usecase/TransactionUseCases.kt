package com.financemanage.mobile.feature.transaction.domain.usecase

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.domain.usecase.GetCategoriesUseCase
import com.financemanage.mobile.core.domain.usecase.GetPreferencesUseCase
import com.financemanage.mobile.core.domain.usecase.GetTodayUseCase
import com.financemanage.mobile.core.domain.usecase.GetWalletsUseCase
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.IdempotencyKey
import com.financemanage.mobile.core.model.domain.Category
import com.financemanage.mobile.core.model.domain.Wallet
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
import com.financemanage.mobile.feature.transaction.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** First page of the unfiltered list, cached for offline display. */
class ObserveRecentTransactionsUseCase(
    private val repository: TransactionRepository,
) {
    operator fun invoke(): Flow<Cached<List<Transaction>>?> = repository.recentTransactions
}

/** One page of transactions matching a filter. */
class SearchTransactionsUseCase(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(
        filter: TransactionFilter,
        page: Int,
        size: Int = PAGE_SIZE,
    ): AppResult<TransactionPage> = repository.search(filter, page, size)

    companion object {
        const val PAGE_SIZE = 30
    }
}

/** Income / expense totals per currency for a filter. */
class GetTransactionTotalsUseCase(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(filter: TransactionFilter): AppResult<List<TransactionTotals>> =
        repository.summary(filter)
}

class GetTransactionUseCase(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Transaction> = repository.get(id)
}

class CreateTransactionUseCase(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(
        request: CreateTransactionRequest,
        key: IdempotencyKey,
        summary: String,
    ): AppResult<Pair<WriteOutcome, Transaction?>> = repository.create(request, key, summary)
}

class UpdateTransactionUseCase(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(
        id: String,
        request: UpdateTransactionRequest,
    ): AppResult<Transaction> = repository.update(id, request)
}

class DeleteTransactionUseCase(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.delete(id)
}

class CreateTransferUseCase(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(
        request: TransferRequest,
        key: IdempotencyKey,
        summary: String,
    ): AppResult<WriteOutcome> = repository.transfer(request, key, summary)
}

/** Deletes both sides of a transfer. */
class DeleteTransferUseCase(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(transferId: String): AppResult<Unit> = repository.deleteTransfer(transferId)
}

/** Attaches a receipt photo to a transaction (F10). */
class UploadReceiptUseCase(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(
        transactionId: String,
        bytes: ByteArray,
        fileName: String,
        contentType: String,
    ): AppResult<AttachmentDto> = repository.uploadAttachment(transactionId, bytes, fileName, contentType)
}

class DownloadReceiptUseCase(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(id: String): AppResult<ByteArray> = repository.downloadAttachment(id)
}

class DeleteReceiptUseCase(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.deleteAttachment(id)
}

/** Observes the cached recurring transactions. */
class ObserveRecurringUseCase(
    private val repository: TransactionRepository,
) {
    operator fun invoke(): Flow<Cached<List<Recurring>>?> = repository.recurring
}

class RefreshRecurringUseCase(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(): AppResult<List<Recurring>> = repository.refreshRecurring()
}

class GetRecurringUseCase(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Recurring> = repository.getRecurring(id)
}

class CreateRecurringUseCase(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(request: CreateRecurringRequest): AppResult<Recurring> =
        repository.createRecurring(request)
}

class UpdateRecurringUseCase(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(
        id: String,
        request: UpdateRecurringRequest,
    ): AppResult<Recurring> = repository.updateRecurring(id, request)
}

class DeleteRecurringUseCase(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.deleteRecurring(id)
}

/** What the Quick add form needs before the user types anything (F07). */
data class TransactionFormContext(
    /** Every wallet, archived included (an edited transaction may use an archived wallet). */
    val wallets: List<Wallet>,
    val categories: List<Category>,
    val recentCategoryIds: List<String>,
    val lastWalletId: String?,
    val today: LocalDate,
)

/** Loads the wallets and categories (cached, or fetched on a first launch), the recent choices and "today". */
class LoadTransactionFormUseCase(
    private val getWallets: GetWalletsUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val getPreferences: GetPreferencesUseCase,
    private val getToday: GetTodayUseCase,
) {
    suspend operator fun invoke(): TransactionFormContext {
        val wallets = getWallets()
        val categories = getCategories()
        val preferences = getPreferences()
        return TransactionFormContext(
            wallets,
            categories,
            preferences.recentCategoryIds,
            preferences.lastWalletId,
            getToday(),
        )
    }
}

/** The use cases behind the transaction list (F08), injected together. */
data class TransactionListUseCases(
    val search: SearchTransactionsUseCase,
    val getTotals: GetTransactionTotalsUseCase,
    val observeRecent: ObserveRecentTransactionsUseCase,
    val delete: DeleteTransactionUseCase,
)

/** Deletes a transaction; when it is one side of a transfer, the whole transfer is deleted. */
class DeleteTransactionOrTransferUseCase(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(transaction: Transaction): AppResult<Unit> {
        val transferId = transaction.transferId
        return if (transferId != null) repository.deleteTransfer(transferId) else repository.delete(transaction.id)
    }
}
