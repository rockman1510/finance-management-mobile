package com.financemanage.mobile.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.data.cache.Resource
import com.financemanage.mobile.core.data.cache.ResourceHolder
import com.financemanage.mobile.core.data.push.PushBridge
import com.financemanage.mobile.core.data.settings.AppSettingsRepository
import com.financemanage.mobile.core.data.shared.CategoryRepository
import com.financemanage.mobile.core.data.shared.ProfileRepository
import com.financemanage.mobile.core.data.shared.WalletRepository
import com.financemanage.mobile.core.data.shared.WalletTotal
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.domain.Category
import com.financemanage.mobile.core.model.domain.Profile
import com.financemanage.mobile.core.model.domain.Wallet
import com.financemanage.mobile.core.model.getOrNull
import com.financemanage.mobile.core.ui.components.PendingAction
import com.financemanage.mobile.feature.home.data.HomeBill
import com.financemanage.mobile.feature.home.data.HomeBudget
import com.financemanage.mobile.feature.home.data.HomeGoal
import com.financemanage.mobile.feature.home.data.HomeRepository
import com.financemanage.mobile.feature.home.data.HomeSummary
import com.financemanage.mobile.feature.home.data.HomeTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeExtras(
    val profile: Profile? = null,
    val total: WalletTotal? = null,
    val unread: Long = 0,
    val hideBalances: Boolean = false,
    val wallets: List<Wallet> = emptyList(),
    val categories: List<Category> = emptyList(),
    val payingBillId: String? = null,
    val payResult: PendingAction? = null,
    val payError: AppError? = null,
)

data class HomeUiState(
    val summary: Resource<HomeSummary> = Resource(loading = true),
    val budgets: Resource<List<HomeBudget>> = Resource(loading = true),
    val bills: Resource<List<HomeBill>> = Resource(loading = true),
    val goals: Resource<List<HomeGoal>> = Resource(loading = true),
    val transactions: Resource<List<HomeTransaction>> = Resource(loading = true),
    val extras: HomeExtras = HomeExtras(),
) {
    val currency: CurrencyCode get() = extras.profile?.defaultCurrency ?: CurrencyCode.DEFAULT

    /** The 3 budgets with the highest %. */
    val topBudgets: List<HomeBudget> get() =
        budgets.data
            .orEmpty()
            .sortedByDescending {
                it.current?.percent ?: 0
            }.take(TOP_BUDGETS)

    /** The goal closest to completion that is not reached yet. */
    val featuredGoal: HomeGoal? get() =
        goals.data
            .orEmpty()
            .filter { !it.reached }
            .maxByOrNull { it.percent }

    private companion object {
        const val TOP_BUDGETS = 3
    }
}

/** Home (F12): every block loads independently; cached data shows at once, then refreshes. */
class HomeViewModel(
    private val repository: HomeRepository,
    private val profiles: ProfileRepository,
    private val wallets: WalletRepository,
    categories: CategoryRepository,
    private val settings: AppSettingsRepository,
) : ViewModel() {
    private val summary =
        ResourceHolder(viewModelScope, repository.summary) { repository.refreshSummary(currencyCode().value) }
    private val budgets = ResourceHolder(viewModelScope, repository.budgets) { repository.refreshBudgets() }
    private val bills = ResourceHolder(viewModelScope, repository.bills) { repository.refreshBills() }
    private val goals = ResourceHolder(viewModelScope, repository.goals) { repository.refreshGoals() }
    private val transactions =
        ResourceHolder(viewModelScope, repository.transactions) { repository.refreshTransactions() }
    private val total = MutableStateFlow<WalletTotal?>(null)
    private val unread = MutableStateFlow(0L)
    private val payState = MutableStateFlow(Triple<String?, PendingAction?, AppError?>(null, null, null))

    private val extras =
        combine(
            combine(profiles.profile.map { it?.value }, total, unread) { p, t, u -> Triple(p, t, u) },
            settings.preferences.map { it.hideBalances },
            wallets.wallets.map { it?.value.orEmpty() },
            categories.categories.map { it?.value.orEmpty() },
            payState,
        ) { (profile, totalValue, unreadCount), hide, walletList, categoryList, (paying, result, error) ->
            HomeExtras(profile, totalValue, unreadCount, hide, walletList, categoryList, paying, result, error)
        }

    val state: StateFlow<HomeUiState> =
        combine(
            combine(summary.state, budgets.state, bills.state) { s, b, bi -> Triple(s, b, bi) },
            goals.state,
            transactions.state,
            extras,
        ) { (s, b, bi), g, t, e -> HomeUiState(s, b, bi, g, t, e) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), HomeUiState())

    init {
        // Foreground push (F20): refresh the bell badge.
        viewModelScope.launch {
            PushBridge.received.collect {
                (repository.unreadCount() as? AppResult.Success)?.let { unread.value = it.value }
            }
        }
    }

    /** The default currency; on a first launch the profile is not cached yet, so it is fetched first. */
    private suspend fun currencyCode(): CurrencyCode =
        (profiles.current() ?: profiles.refresh().getOrNull())?.defaultCurrency ?: CurrencyCode.DEFAULT

    fun refresh() {
        viewModelScope.launch {
            profiles.refresh()
            wallets.refresh()
            (wallets.total(currencyCode()) as? AppResult.Success)?.let { total.value = it.value }
            (repository.unreadCount() as? AppResult.Success)?.let { unread.value = it.value }
        }
        listOf(summary, budgets, bills, goals, transactions).forEach { it.refresh() }
    }

    fun toggleHideBalances() {
        viewModelScope.launch { settings.setHideBalances(!settings.preferences.first().hideBalances) }
    }

    fun payBill(bill: HomeBill) {
        if (payState.value.first != null) return
        payState.value = Triple(bill.id, PendingAction.Processing, null)
        viewModelScope.launch {
            when (val result = repository.payBill(bill.id)) {
                is AppResult.Failure -> payState.value = Triple(null, null, result.error)
                is AppResult.Success ->
                    payState.value =
                        Triple(null, PendingAction.from(result.value, { it.status }, { it.failureReason }), null)
            }
            wallets.refresh()
            transactions.refresh()
        }
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}
