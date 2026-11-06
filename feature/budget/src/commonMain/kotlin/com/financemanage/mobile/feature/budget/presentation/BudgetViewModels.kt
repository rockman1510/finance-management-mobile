package com.financemanage.mobile.feature.budget.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.domain.usecase.GetCachedCategoriesUseCase
import com.financemanage.mobile.core.domain.usecase.GetCachedWalletsUseCase
import com.financemanage.mobile.core.domain.usecase.GetDefaultCurrencyUseCase
import com.financemanage.mobile.core.domain.usecase.GetTodayUseCase
import com.financemanage.mobile.core.domain.usecase.RequestNotificationPermissionUseCase
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.domain.Category
import com.financemanage.mobile.core.model.domain.CategoryType
import com.financemanage.mobile.core.model.domain.Wallet
import com.financemanage.mobile.core.model.toPlainString
import com.financemanage.mobile.core.ui.components.parseAmountInput
import com.financemanage.mobile.core.ui.state.Resource
import com.financemanage.mobile.core.ui.state.ResourceHolder
import com.financemanage.mobile.feature.budget.domain.model.Budget
import com.financemanage.mobile.feature.budget.domain.model.BudgetPeriod
import com.financemanage.mobile.feature.budget.domain.model.BudgetTransactionDto
import com.financemanage.mobile.feature.budget.domain.model.CreateBudgetRequest
import com.financemanage.mobile.feature.budget.domain.model.UpdateBudgetRequest
import com.financemanage.mobile.feature.budget.domain.usecase.CreateBudgetUseCase
import com.financemanage.mobile.feature.budget.domain.usecase.DeleteBudgetUseCase
import com.financemanage.mobile.feature.budget.domain.usecase.GetBudgetTransactionsUseCase
import com.financemanage.mobile.feature.budget.domain.usecase.GetBudgetUseCase
import com.financemanage.mobile.feature.budget.domain.usecase.ObserveBudgetsUseCase
import com.financemanage.mobile.feature.budget.domain.usecase.RefreshBudgetsUseCase
import com.financemanage.mobile.feature.budget.domain.usecase.UpdateBudgetUseCase
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class BudgetListViewModel(
    observeBudgets: ObserveBudgetsUseCase,
    refreshBudgets: RefreshBudgetsUseCase,
) : ViewModel() {
    private val holder = ResourceHolder(viewModelScope, observeBudgets()) { refreshBudgets() }
    val state: StateFlow<Resource<List<Budget>>> = holder.state

    fun refresh() = holder.refresh()
}

data class BudgetDetailUiState(
    val budget: Budget? = null,
    val date: LocalDate? = null,
    val transactions: List<BudgetTransactionDto> = emptyList(),
    val categories: List<Category> = emptyList(),
    val loading: Boolean = true,
    val error: AppError? = null,
    val deleted: Boolean = false,
)

class BudgetDetailViewModel(
    private val id: String,
    private val getBudget: GetBudgetUseCase,
    private val getBudgetTransactions: GetBudgetTransactionsUseCase,
    private val deleteBudget: DeleteBudgetUseCase,
    getCachedCategories: GetCachedCategoriesUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(BudgetDetailUiState())
    val state: StateFlow<BudgetDetailUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    categories = getCachedCategories(),
                )
            }
        }
    }

    fun refresh() = load(mutableState.value.date)

    fun previousPeriod() {
        mutableState.value.budget
            ?.current
            ?.periodStart
            ?.let { load(it.minus(DatePeriod(days = 1))) }
    }

    fun nextPeriod() {
        mutableState.value.budget
            ?.current
            ?.periodEnd
            ?.let { load(it.plus(DatePeriod(days = 1))) }
    }

    private fun load(date: LocalDate?) {
        mutableState.update { it.copy(loading = true, date = date) }
        viewModelScope.launch {
            when (val result = getBudget(id, date)) {
                is AppResult.Success -> {
                    val transactions = (getBudgetTransactions(result.value) as? AppResult.Success)?.value.orEmpty()
                    mutableState.update {
                        it.copy(
                            budget = result.value,
                            transactions = transactions,
                            loading = false,
                            error = null,
                        )
                    }
                }
                is AppResult.Failure -> mutableState.update { it.copy(loading = false, error = result.error) }
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            val result = deleteBudget(id)
            mutableState.update {
                it.copy(
                    deleted = result is AppResult.Success,
                    error = (result as? AppResult.Failure)?.error,
                )
            }
        }
    }
}

enum class BudgetProblem { NAME, AMOUNT, PERIOD_DATES, END_BEFORE_START }

data class BudgetFormUiState(
    val isEdit: Boolean = false,
    val name: String = "",
    val amount: String = "",
    val currency: CurrencyCode = CurrencyCode.DEFAULT,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val repeat: Boolean = true,
    val categoryIds: Set<String> = emptySet(),
    val walletId: String? = null,
    val categories: List<Category> = emptyList(),
    val wallets: List<Wallet> = emptyList(),
    val problems: Set<BudgetProblem> = emptySet(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: AppError? = null,
)

class BudgetFormViewModel(
    private val id: String?,
    getBudget: GetBudgetUseCase,
    private val createBudget: CreateBudgetUseCase,
    private val updateBudget: UpdateBudgetUseCase,
    getCachedCategories: GetCachedCategoriesUseCase,
    getCachedWallets: GetCachedWalletsUseCase,
    getDefaultCurrency: GetDefaultCurrencyUseCase,
    getToday: GetTodayUseCase,
    private val requestNotificationPermission: RequestNotificationPermissionUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(BudgetFormUiState(isEdit = id != null))
    val state: StateFlow<BudgetFormUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val currency = getDefaultCurrency()
            val today = getToday()
            val categoryList = getCachedCategories()
            val walletList = getCachedWallets()
            mutableState.update {
                it.copy(
                    currency = currency,
                    startDate = today,
                    categories =
                        categoryList.filter { c ->
                            c.type ==
                                CategoryType.EXPENSE
                        },
                    wallets = walletList.filter { w -> !w.archived },
                )
            }
            if (id != null) {
                (getBudget(id, null) as? AppResult.Success)?.value?.let { budget ->
                    mutableState.update {
                        it.copy(
                            name = budget.name,
                            amount = budget.amount.amount.toPlainString(budget.amount.currency.fractionDigits),
                            currency = budget.amount.currency,
                            period = budget.period,
                            startDate = budget.startDate,
                            endDate = budget.endDate,
                            repeat = budget.repeat,
                            categoryIds = budget.categoryIds.toSet(),
                            walletId = budget.walletId,
                        )
                    }
                }
            }
        }
    }

    fun onName(value: String) = edit { it.copy(name = value) }

    fun onAmount(value: String) = edit { it.copy(amount = value) }

    fun onCurrency(value: CurrencyCode) = edit { it.copy(currency = value) }

    fun onPeriod(value: BudgetPeriod) = edit { it.copy(period = value) }

    fun onStart(value: LocalDate) = edit { it.copy(startDate = value) }

    fun onEnd(value: LocalDate) = edit { it.copy(endDate = value) }

    fun onRepeat(value: Boolean) = edit { it.copy(repeat = value) }

    fun toggleCategory(id: String) =
        edit {
            it.copy(
                categoryIds =
                    if (id in
                        it.categoryIds
                    ) {
                        it.categoryIds - id
                    } else {
                        it.categoryIds + id
                    },
            )
        }

    fun onWallet(value: Wallet?) = edit { it.copy(walletId = value?.id) }

    private fun edit(transform: (BudgetFormUiState) -> BudgetFormUiState) =
        mutableState.update { transform(it).copy(problems = emptySet(), error = null) }

    fun save() {
        val form = mutableState.value
        val amount = parseAmountInput(form.amount, form.currency.fractionDigits)
        val start = form.startDate
        val end = form.endDate
        val custom = form.period == BudgetPeriod.CUSTOM
        val problems = problemsOf(form, amount)
        if (problems.isNotEmpty() || amount == null) {
            mutableState.update { it.copy(problems = problems) }
            return
        }
        mutableState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result =
                if (id == null) {
                    createBudget(
                        CreateBudgetRequest(
                            name = form.name.trim(),
                            amount = amount,
                            currency = form.currency.value,
                            categoryIds = form.categoryIds.toList(),
                            walletId = form.walletId,
                            period = form.period,
                            startDate = start,
                            endDate = if (custom) end else null,
                            repeat = form.repeat,
                        ),
                    )
                } else {
                    updateBudget(
                        id,
                        UpdateBudgetRequest(
                            name = form.name.trim(),
                            amount = amount,
                            categoryIds = form.categoryIds.toList(),
                            walletId = form.walletId,
                            allWallets = form.walletId == null,
                            repeat = form.repeat,
                        ),
                    )
                }
            // Ask for the notification permission after the first budget / bill (F20; asked once).
            if (id == null && result is AppResult.Success) requestNotificationPermission()
            mutableState.update {
                it.copy(
                    saving = false,
                    saved = result is AppResult.Success,
                    error = (result as? AppResult.Failure)?.error,
                )
            }
        }
    }
}

private fun problemsOf(
    form: BudgetFormUiState,
    amount: BigDecimal?,
): Set<BudgetProblem> {
    val start = form.startDate
    val end = form.endDate
    return buildSet {
        if (form.name.isBlank()) add(BudgetProblem.NAME)
        if (amount == null || amount.signum() <= 0) add(BudgetProblem.AMOUNT)
        if (form.period == BudgetPeriod.CUSTOM && (start == null || end == null)) add(BudgetProblem.PERIOD_DATES)
        if (start != null && end != null && end < start) add(BudgetProblem.END_BEFORE_START)
    }
}
