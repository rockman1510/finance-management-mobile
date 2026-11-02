package com.financemanage.mobile.feature.wallet.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.data.cache.Resource
import com.financemanage.mobile.core.data.cache.ResourceHolder
import com.financemanage.mobile.core.data.shared.AdjustBalanceRequest
import com.financemanage.mobile.core.data.shared.CreateWalletRequest
import com.financemanage.mobile.core.data.shared.ProfileRepository
import com.financemanage.mobile.core.data.shared.UpdateWalletRequest
import com.financemanage.mobile.core.data.shared.WalletRepository
import com.financemanage.mobile.core.data.shared.WalletTotal
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.domain.Wallet
import com.financemanage.mobile.core.model.domain.WalletType
import com.financemanage.mobile.core.model.time.today
import com.financemanage.mobile.core.model.toPlainString
import com.financemanage.mobile.core.ui.components.parseAmountInput
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

private const val STOP_MS = 5_000L

// ---------------------------------------------------------------- List

data class WalletListUiState(
    val wallets: Resource<List<Wallet>> = Resource(loading = true),
    val total: WalletTotal? = null,
)

class WalletListViewModel(
    private val wallets: WalletRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {
    private val holder = ResourceHolder(viewModelScope, wallets.wallets) { refreshAll() }
    private val total = MutableStateFlow<WalletTotal?>(null)

    val state: StateFlow<WalletListUiState> =
        combine(holder.state, total) { list, sum -> WalletListUiState(list, sum) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), WalletListUiState())

    fun refresh() = holder.refresh()

    private suspend fun refreshAll(): AppResult<*> {
        val result = wallets.refresh()
        val currency = profiles.current()?.defaultCurrency ?: CurrencyCode.DEFAULT
        (wallets.total(currency) as? AppResult.Success)?.let { total.value = it.value }
        return result
    }
}

// ---------------------------------------------------------------- Detail

data class WalletDetailUiState(
    val wallet: Wallet? = null,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: AppError? = null,
    val deleted: Boolean = false,
)

class WalletDetailViewModel(
    private val walletId: String,
    private val wallets: WalletRepository,
) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val error = MutableStateFlow<AppError?>(null)
    private val deleted = MutableStateFlow(false)

    val state: StateFlow<WalletDetailUiState> =
        combine(
            wallets.wallets.map { cached ->
                cached?.value?.firstOrNull { it.id == walletId }
            },
            busy,
            error,
            deleted,
        ) { wallet, b, e, d ->
            WalletDetailUiState(wallet, loading = wallet == null && e == null && !d, busy = b, error = e, deleted = d)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), WalletDetailUiState())

    fun refresh() = act { wallets.refresh() }

    fun setArchived(archived: Boolean) = act { wallets.update(walletId, UpdateWalletRequest(archived = archived)) }

    fun delete() =
        act {
            wallets.delete(walletId).also { if (it is AppResult.Success) deleted.value = true }
        }

    private fun act(block: suspend () -> AppResult<*>) {
        if (busy.value) return
        busy.value = true
        error.value = null
        viewModelScope.launch {
            error.value = (block() as? AppResult.Failure)?.error
            busy.value = false
        }
    }
}

// ---------------------------------------------------------------- Create / edit

enum class WalletFormProblem { NAME, AMOUNT, CREDIT_LIMIT, STATEMENT_DAY, DUE_DAY }

data class WalletFormUiState(
    val isEdit: Boolean = false,
    val loaded: Boolean = false,
    val name: String = "",
    val type: WalletType = WalletType.CASH,
    val currency: CurrencyCode = CurrencyCode.DEFAULT,
    val initialBalance: String = "",
    val excludedFromTotal: Boolean = false,
    val creditLimit: String = "",
    val statementDay: String = "",
    val paymentDueDay: String = "",
    val problems: Set<WalletFormProblem> = emptySet(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: AppError? = null,
)

class WalletFormViewModel(
    private val walletId: String?,
    private val wallets: WalletRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(WalletFormUiState(isEdit = walletId != null))
    val state: StateFlow<WalletFormUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            if (walletId == null) {
                val currency = profiles.current()?.defaultCurrency ?: CurrencyCode.DEFAULT
                mutableState.update { it.copy(loaded = true, currency = currency) }
            } else {
                when (val result = wallets.get(walletId)) {
                    is AppResult.Success -> mutableState.update { result.value.toForm(it) }
                    is AppResult.Failure -> mutableState.update { it.copy(loaded = true, error = result.error) }
                }
            }
        }
    }

    private fun Wallet.toForm(state: WalletFormUiState): WalletFormUiState {
        val digits = currency.fractionDigits
        return state.copy(
            loaded = true,
            name = name,
            type = type,
            currency = currency,
            initialBalance = initialBalance.amount.toPlainString(digits),
            excludedFromTotal = excludedFromTotal,
            creditLimit = creditLimit?.amount?.toPlainString(digits).orEmpty(),
            statementDay = statementDay?.toString().orEmpty(),
            paymentDueDay = paymentDueDay?.toString().orEmpty(),
        )
    }

    fun onName(value: String) = edit { it.copy(name = value) }

    fun onType(value: WalletType) = edit { it.copy(type = value) }

    fun onCurrency(value: CurrencyCode) = edit { it.copy(currency = value) }

    fun onInitialBalance(value: String) = edit { it.copy(initialBalance = value) }

    fun onExcluded(value: Boolean) = edit { it.copy(excludedFromTotal = value) }

    fun onCreditLimit(value: String) = edit { it.copy(creditLimit = value) }

    fun onStatementDay(value: String) = edit { it.copy(statementDay = value.filter(Char::isDigit).take(2)) }

    fun onPaymentDueDay(value: String) = edit { it.copy(paymentDueDay = value.filter(Char::isDigit).take(2)) }

    private fun edit(transform: (WalletFormUiState) -> WalletFormUiState) =
        mutableState.update { transform(it).copy(problems = emptySet(), error = null) }

    /** Parsed, validated form values. */
    private class ParsedWallet(
        val balance: BigDecimal,
        val creditLimit: BigDecimal?,
        val statementDay: Int?,
        val paymentDueDay: Int?,
    )

    private fun parse(form: WalletFormUiState): Pair<ParsedWallet?, Set<WalletFormProblem>> {
        val digits = form.currency.fractionDigits
        val isCard = form.type == WalletType.CREDIT_CARD
        val balance =
            if (form.initialBalance.isBlank()) {
                BigDecimal.ZERO
            } else {
                parseAmountInput(
                    form.initialBalance,
                    digits,
                )
            }
        val limit = if (isCard && form.creditLimit.isNotBlank()) parseAmountInput(form.creditLimit, digits) else null
        val statementDay = form.statementDay.toIntOrNull().takeIf { isCard }
        val dueDay = form.paymentDueDay.toIntOrNull().takeIf { isCard }
        val problems =
            buildSet {
                if (form.name.isBlank()) add(WalletFormProblem.NAME)
                if (balance == null) add(WalletFormProblem.AMOUNT)
                if (isCard) addAll(cardProblems(form, limit, statementDay, dueDay))
            }
        val parsed =
            if (problems.isEmpty() &&
                balance != null
            ) {
                ParsedWallet(balance, limit, statementDay, dueDay)
            } else {
                null
            }
        return parsed to problems
    }

    private fun cardProblems(
        form: WalletFormUiState,
        limit: BigDecimal?,
        statementDay: Int?,
        dueDay: Int?,
    ): Set<WalletFormProblem> =
        buildSet {
            val limitInvalid = limit == null || limit.signum() < 0
            if (form.creditLimit.isNotBlank() && limitInvalid) add(WalletFormProblem.CREDIT_LIMIT)
            if (form.statementDay.isNotBlank() && statementDay !in DAY_RANGE) add(WalletFormProblem.STATEMENT_DAY)
            if (form.paymentDueDay.isNotBlank() && dueDay !in DAY_RANGE) add(WalletFormProblem.DUE_DAY)
        }

    fun save() {
        val form = mutableState.value
        val (parsed, problems) = parse(form)
        if (parsed == null) {
            mutableState.update { it.copy(problems = problems) }
            return
        }
        mutableState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result =
                if (walletId == null) {
                    wallets.create(
                        CreateWalletRequest(
                            name = form.name.trim(),
                            type = form.type,
                            currency = form.currency.value,
                            initialBalance = parsed.balance,
                            excludedFromTotal = form.excludedFromTotal,
                            creditLimit = parsed.creditLimit,
                            statementDay = parsed.statementDay,
                            paymentDueDay = parsed.paymentDueDay,
                        ),
                    )
                } else {
                    wallets.update(
                        walletId,
                        UpdateWalletRequest(
                            name = form.name.trim(),
                            initialBalance = parsed.balance,
                            excludedFromTotal = form.excludedFromTotal,
                            creditLimit = parsed.creditLimit,
                            statementDay = parsed.statementDay,
                            paymentDueDay = parsed.paymentDueDay,
                        ),
                    )
                }
            mutableState.update {
                it.copy(
                    saving = false,
                    saved = result is AppResult.Success,
                    error = (result as? AppResult.Failure)?.error,
                )
            }
        }
    }

    private companion object {
        val DAY_RANGE = 1..31
    }
}

// ---------------------------------------------------------------- Adjust balance

data class AdjustBalanceUiState(
    val wallet: Wallet? = null,
    val newBalance: String = "",
    val date: LocalDate? = null,
    val note: String = "",
    val invalidAmount: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: AppError? = null,
)

class AdjustBalanceViewModel(
    private val walletId: String,
    private val wallets: WalletRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AdjustBalanceUiState())
    val state: StateFlow<AdjustBalanceUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val date = today(profiles.current()?.timeZone)
            when (val result = wallets.get(walletId)) {
                is AppResult.Success ->
                    mutableState.update {
                        it.copy(
                            wallet = result.value,
                            date = date,
                            newBalance =
                                result.value.balance.amount.toPlainString(
                                    result.value.currency.fractionDigits,
                                ),
                        )
                    }
                is AppResult.Failure -> mutableState.update { it.copy(error = result.error, date = date) }
            }
        }
    }

    fun onBalance(value: String) =
        mutableState.update { it.copy(newBalance = value, invalidAmount = false, error = null) }

    fun onDate(value: LocalDate) = mutableState.update { it.copy(date = value) }

    fun onNote(value: String) = mutableState.update { it.copy(note = value) }

    fun save() {
        val current = mutableState.value
        val wallet = current.wallet ?: return
        val amount = parseAmountInput(current.newBalance, wallet.currency.fractionDigits)
        if (amount == null) {
            mutableState.update { it.copy(invalidAmount = true) }
            return
        }
        mutableState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result =
                wallets.adjustBalance(
                    walletId,
                    AdjustBalanceRequest(amount, current.date, current.note.ifBlank { null }),
                )
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
