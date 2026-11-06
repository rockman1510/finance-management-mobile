package com.financemanage.mobile.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.domain.model.WalletTotal
import com.financemanage.mobile.core.domain.usecase.ObserveCategoriesUseCase
import com.financemanage.mobile.core.domain.usecase.ObservePreferencesUseCase
import com.financemanage.mobile.core.domain.usecase.ObserveProfileUseCase
import com.financemanage.mobile.core.domain.usecase.ObservePushReceivedUseCase
import com.financemanage.mobile.core.domain.usecase.ObserveWalletsUseCase
import com.financemanage.mobile.core.domain.usecase.ToggleHideBalancesUseCase
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.domain.Category
import com.financemanage.mobile.core.model.domain.Profile
import com.financemanage.mobile.core.model.domain.Wallet
import com.financemanage.mobile.core.ui.components.PendingAction
import com.financemanage.mobile.core.ui.state.Resource
import com.financemanage.mobile.core.ui.state.ResourceHolder
import com.financemanage.mobile.feature.home.domain.model.HomeBill
import com.financemanage.mobile.feature.home.domain.model.HomeBudget
import com.financemanage.mobile.feature.home.domain.model.HomeGoal
import com.financemanage.mobile.feature.home.domain.model.HomeSummary
import com.financemanage.mobile.feature.home.domain.model.HomeTransaction
import com.financemanage.mobile.feature.home.domain.usecase.HomeUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val home: HomeUseCases,
    observeProfile: ObserveProfileUseCase,
    observeWallets: ObserveWalletsUseCase,
    observeCategories: ObserveCategoriesUseCase,
    observePreferences: ObservePreferencesUseCase,
    private val toggleHideBalancesUseCase: ToggleHideBalancesUseCase,
    observePushReceived: ObservePushReceivedUseCase,
) : ViewModel() {
    private val summary = ResourceHolder(viewModelScope, home.observeSummary()) { home.refreshSummary() }
    private val budgets = ResourceHolder(viewModelScope, home.observeBudgets()) { home.refreshBudgets() }
    private val bills = ResourceHolder(viewModelScope, home.observeBills()) { home.refreshBills() }
    private val goals = ResourceHolder(viewModelScope, home.observeGoals()) { home.refreshGoals() }
    private val transactions =
        ResourceHolder(viewModelScope, home.observeTransactions()) { home.refreshTransactions() }
    private val total = MutableStateFlow<WalletTotal?>(null)
    private val unread = MutableStateFlow(0L)
    private val payState = MutableStateFlow(Triple<String?, PendingAction?, AppError?>(null, null, null))

    private val extras =
        combine(
            combine(observeProfile().map { it?.value }, total, unread) { p, t, u -> Triple(p, t, u) },
            observePreferences().map { it.hideBalances },
            observeWallets().map { it?.value.orEmpty() },
            observeCategories().map { it?.value.orEmpty() },
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
            observePushReceived().collect {
                (home.getUnreadCount() as? AppResult.Success)?.let { unread.value = it.value }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val overview = home.refreshOverview()
            overview.total?.let { total.value = it }
            overview.unread?.let { unread.value = it }
        }
        listOf(summary, budgets, bills, goals, transactions).forEach { it.refresh() }
    }

    fun toggleHideBalances() {
        viewModelScope.launch { toggleHideBalancesUseCase() }
    }

    fun payBill(bill: HomeBill) {
        if (payState.value.first != null) return
        payState.value = Triple(bill.id, PendingAction.Processing, null)
        viewModelScope.launch {
            when (val result = home.payBill(bill.id)) {
                is AppResult.Failure -> payState.value = Triple(null, null, result.error)
                is AppResult.Success ->
                    payState.value =
                        Triple(null, PendingAction.from(result.value, { it.status }, { it.failureReason }), null)
            }
            transactions.refresh()
        }
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}
