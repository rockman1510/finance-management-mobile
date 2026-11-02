package com.financemanage.mobile.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.domain.WalletType
import com.financemanage.mobile.core.ui.components.parseAmountInput
import com.financemanage.mobile.feature.onboarding.data.OnboardingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep { CHECKING, CURRENCY, WALLET, BUDGET, SECURITY, DONE }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.CHECKING,
    val currency: CurrencyCode = CurrencyCode.DEFAULT,
    val currencyQuery: String = "",
    val walletName: String = "",
    val walletType: WalletType = WalletType.CASH,
    val walletBalance: String = "",
    val budgetName: String = "",
    val budgetAmount: String = "",
    val amountInvalid: Boolean = false,
    val loading: Boolean = false,
    val error: AppError? = null,
) {
    /** Steps shown with a progress bar (1–4). */
    val stepNumber: Int get() = (step.ordinal).coerceIn(1, TOTAL_STEPS)

    companion object {
        const val TOTAL_STEPS = 4
    }
}

/**
 * Onboarding (F03): shown once. Skipped for accounts that already have a wallet (new device).
 * Steps 3 (budget) and 4 (2FA) can be skipped; skipping 2FA is never asked again.
 */
class OnboardingViewModel(
    private val repository: OnboardingRepository,
    defaultWalletName: String,
    defaultBudgetName: String,
) : ViewModel() {
    private val mutableState =
        MutableStateFlow(OnboardingUiState(walletName = defaultWalletName, budgetName = defaultBudgetName))
    val state: StateFlow<OnboardingUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val skip = repository.isCompleted() || (repository.hasWallets() as? AppResult.Success)?.value == true
            if (skip) {
                finish()
            } else {
                mutableState.update { it.copy(step = OnboardingStep.CURRENCY, currency = repository.defaultCurrency()) }
            }
        }
    }

    fun onCurrencyQuery(value: String) = mutableState.update { it.copy(currencyQuery = value) }

    fun onCurrencySelected(currency: CurrencyCode) = mutableState.update { it.copy(currency = currency) }

    fun onWalletName(value: String) = mutableState.update { it.copy(walletName = value) }

    fun onWalletType(value: WalletType) = mutableState.update { it.copy(walletType = value) }

    fun onWalletBalance(value: String) = mutableState.update { it.copy(walletBalance = value, amountInvalid = false) }

    fun onBudgetAmount(value: String) = mutableState.update { it.copy(budgetAmount = value, amountInvalid = false) }

    fun confirmCurrency() =
        call(next = OnboardingStep.WALLET) { repository.setDefaultCurrency(mutableState.value.currency) }

    fun confirmWallet() {
        val current = mutableState.value
        val balance =
            if (current.walletBalance.isBlank()) {
                com.ionspin.kotlin.bignum.decimal.BigDecimal.ZERO
            } else {
                parseAmountInput(current.walletBalance, current.currency.fractionDigits)
            }
        if (balance == null || current.walletName.isBlank()) {
            mutableState.update { it.copy(amountInvalid = balance == null) }
            return
        }
        call(next = OnboardingStep.BUDGET) {
            repository.createWallet(current.walletName.trim(), current.walletType, Money(balance, current.currency))
        }
    }

    fun confirmBudget() {
        val current = mutableState.value
        val amount = parseAmountInput(current.budgetAmount, current.currency.fractionDigits)
        if (amount == null || amount.signum() <= 0) {
            mutableState.update { it.copy(amountInvalid = true) }
            return
        }
        call(next = OnboardingStep.SECURITY) {
            repository.createMonthlyBudget(current.budgetName, Money(amount, current.currency))
        }
    }

    fun skipBudget() = mutableState.update { it.copy(step = OnboardingStep.SECURITY, error = null) }

    /** "Skip" and returning from the 2FA flow both end onboarding. */
    fun finish() {
        viewModelScope.launch {
            repository.markCompleted()
            mutableState.update { it.copy(step = OnboardingStep.DONE) }
        }
    }

    private fun call(
        next: OnboardingStep,
        block: suspend () -> AppResult<Unit>,
    ) {
        if (mutableState.value.loading) return
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = block()) {
                is AppResult.Success -> mutableState.update { it.copy(loading = false, step = next) }
                is AppResult.Failure -> mutableState.update { it.copy(loading = false, error = result.error) }
            }
        }
    }
}
