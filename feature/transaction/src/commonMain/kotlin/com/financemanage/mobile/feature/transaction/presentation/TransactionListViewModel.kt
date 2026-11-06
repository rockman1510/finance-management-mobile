package com.financemanage.mobile.feature.transaction.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.domain.model.PendingWrite
import com.financemanage.mobile.core.domain.usecase.DiscardPendingWriteUseCase
import com.financemanage.mobile.core.domain.usecase.GetCategoriesUseCase
import com.financemanage.mobile.core.domain.usecase.GetWalletsUseCase
import com.financemanage.mobile.core.domain.usecase.ObserveCategoriesUseCase
import com.financemanage.mobile.core.domain.usecase.ObservePendingWritesUseCase
import com.financemanage.mobile.core.domain.usecase.ObserveWalletsUseCase
import com.financemanage.mobile.core.domain.usecase.RetryPendingWriteUseCase
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.domain.Category
import com.financemanage.mobile.core.model.domain.Wallet
import com.financemanage.mobile.feature.transaction.domain.model.Transaction
import com.financemanage.mobile.feature.transaction.domain.model.TransactionFilter
import com.financemanage.mobile.feature.transaction.domain.model.TransactionTotals
import com.financemanage.mobile.feature.transaction.domain.usecase.TransactionListUseCases
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionListContent(
    val items: List<Transaction> = emptyList(),
    val page: Int = -1,
    val hasMore: Boolean = false,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: AppError? = null,
    val totals: List<TransactionTotals> = emptyList(),
    /** Showing the cached first page because the network call failed (offline). */
    val fromCache: Boolean = false,
)

data class TransactionListUiState(
    val filter: TransactionFilter = TransactionFilter(),
    val content: TransactionListContent = TransactionListContent(),
    val wallets: List<Wallet> = emptyList(),
    val categories: List<Category> = emptyList(),
    val pending: List<PendingWrite> = emptyList(),
    val actionError: AppError? = null,
)

/** Transactions tab (F07): filters, totals, paged list newest first, pending offline writes on top. */
class TransactionListViewModel(
    initialFilter: TransactionFilter,
    private val transactions: TransactionListUseCases,
    observeWallets: ObserveWalletsUseCase,
    observeCategories: ObserveCategoriesUseCase,
    getWallets: GetWalletsUseCase,
    getCategories: GetCategoriesUseCase,
    observePendingWrites: ObservePendingWritesUseCase,
    private val retryPendingWrite: RetryPendingWriteUseCase,
    private val discardPendingWrite: DiscardPendingWriteUseCase,
) : ViewModel() {
    private val filter = MutableStateFlow(initialFilter)
    private val content = MutableStateFlow(TransactionListContent())
    private val actionError = MutableStateFlow<AppError?>(null)
    private var loadJob: Job? = null

    val state: StateFlow<TransactionListUiState> =
        combine(
            filter,
            content,
            observeWallets().map { it?.value.orEmpty() },
            observeCategories().map { it?.value.orEmpty() },
            combine(observePendingWrites(), actionError) { pending, error -> pending to error },
        ) { currentFilter, currentContent, walletList, categoryList, (pending, error) ->
            TransactionListUiState(currentFilter, currentContent, walletList, categoryList, pending, error)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_MS),
            TransactionListUiState(filter = initialFilter),
        )

    init {
        // Shared data used for labels; refreshed in the background.
        viewModelScope.launch { getWallets() }
        viewModelScope.launch { getCategories() }
    }

    fun refresh() = load(reset = true)

    fun loadMore() {
        val current = content.value
        if (current.hasMore && !current.loadingMore && !current.loading) load(reset = false)
    }

    fun setFilter(value: TransactionFilter) {
        filter.value = value
        load(reset = true)
    }

    fun setQuery(value: String) = setFilter(filter.value.copy(query = value))

    fun delete(transaction: Transaction) {
        viewModelScope.launch {
            val result = transactions.delete(transaction.id)
            actionError.value = (result as? AppResult.Failure)?.error
            if (result is AppResult.Success) {
                content.update { it.copy(items = it.items.filterNot { item -> item.id == transaction.id }) }
                loadTotals()
            }
        }
    }

    fun retryPending(entry: PendingWrite) {
        viewModelScope.launch { retryPendingWrite(entry) }
    }

    fun discardPending(entry: PendingWrite) {
        viewModelScope.launch { discardPendingWrite(entry) }
    }

    private fun load(reset: Boolean) {
        loadJob?.cancel()
        val currentFilter = filter.value
        val nextPage = if (reset) 0 else content.value.page + 1
        content.update { if (reset) it.copy(loading = true, error = null) else it.copy(loadingMore = true) }
        loadJob =
            viewModelScope.launch {
                if (reset) loadTotals()
                when (val result = transactions.search(currentFilter, nextPage)) {
                    is AppResult.Success ->
                        content.update {
                            it.copy(
                                items = if (reset) result.value.items else it.items + result.value.items,
                                page = result.value.page,
                                hasMore = result.value.hasMore,
                                loading = false,
                                loadingMore = false,
                                error = null,
                                fromCache = false,
                            )
                        }
                    is AppResult.Failure -> {
                        // Offline: show the cached first page when no filter is applied.
                        val cached =
                            if (reset &&
                                currentFilter.isEmpty
                            ) {
                                transactions.observeRecent().first()?.value
                            } else {
                                null
                            }
                        content.update {
                            it.copy(
                                items = cached ?: it.items,
                                loading = false,
                                loadingMore = false,
                                error = result.error,
                                fromCache = cached != null,
                                hasMore = if (cached != null) false else it.hasMore,
                            )
                        }
                    }
                }
            }
    }

    private suspend fun loadTotals() {
        (transactions.getTotals(filter.value) as? AppResult.Success)?.let { result ->
            content.update { it.copy(totals = result.value) }
        }
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}
