package com.financemanage.mobile.feature.saving.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.data.cache.Resource
import com.financemanage.mobile.core.data.cache.ResourceHolder
import com.financemanage.mobile.core.data.shared.ProfileRepository
import com.financemanage.mobile.core.data.shared.WalletRepository
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.domain.Frequency
import com.financemanage.mobile.core.model.domain.Wallet
import com.financemanage.mobile.core.model.time.today
import com.financemanage.mobile.core.model.toPlainString
import com.financemanage.mobile.core.ui.components.PendingAction
import com.financemanage.mobile.core.ui.components.parseAmountInput
import com.financemanage.mobile.feature.saving.data.AutoContributionRequest
import com.financemanage.mobile.feature.saving.data.CreateGoalRequest
import com.financemanage.mobile.feature.saving.data.GoalDto
import com.financemanage.mobile.feature.saving.data.MoveRequest
import com.financemanage.mobile.feature.saving.data.RoundUpRequest
import com.financemanage.mobile.feature.saving.data.SavingRepository
import com.financemanage.mobile.feature.saving.data.UpdateGoalRequest
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class GoalListViewModel(
    private val repository: SavingRepository,
) : ViewModel() {
    private val holder = ResourceHolder(viewModelScope, repository.goals) { repository.refresh() }
    val state: StateFlow<Resource<List<GoalDto>>> = holder.state

    fun refresh() = holder.refresh()
}

/** A milestone just crossed by a deposit, for the celebration (F15). */
enum class Milestone { HALF, REACHED }

data class GoalDetailUiState(
    val goal: GoalDto? = null,
    val wallets: List<Wallet> = emptyList(),
    val today: LocalDate? = null,
    val pending: PendingAction? = null,
    val milestone: Milestone? = null,
    val busy: Boolean = false,
    val error: AppError? = null,
    val deleted: Boolean = false,
)

class GoalDetailViewModel(
    private val id: String,
    private val repository: SavingRepository,
    wallets: WalletRepository,
    profiles: ProfileRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(GoalDetailUiState())
    val state: StateFlow<GoalDetailUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    wallets =
                        wallets.wallets.first()?.value.orEmpty().filter { w ->
                            !w.archived
                        },
                    today = today(profiles.current()?.timeZone),
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            when (val result = repository.get(id)) {
                is AppResult.Success -> mutableState.update { it.copy(goal = result.value) }
                is AppResult.Failure -> mutableState.update { it.copy(error = result.error) }
            }
        }
    }

    fun move(
        deposit: Boolean,
        amount: BigDecimal,
        walletId: String,
        date: LocalDate?,
    ) {
        val before = mutableState.value.goal?.percent ?: 0
        mutableState.update { it.copy(busy = true, pending = PendingAction.Processing, error = null, milestone = null) }
        viewModelScope.launch {
            when (val result = repository.move(id, deposit, MoveRequest(amount, walletId, date))) {
                is AppResult.Failure ->
                    mutableState.update {
                        it.copy(
                            busy = false,
                            pending = null,
                            error = result.error,
                        )
                    }
                is AppResult.Success -> {
                    val pending = PendingAction.from(result.value, { it.status }, { it.failureReason })
                    mutableState.update { it.copy(busy = false, pending = pending) }
                    refreshAndCelebrate(before)
                }
            }
        }
    }

    private suspend fun refreshAndCelebrate(before: Int) {
        val goal = (repository.get(id) as? AppResult.Success)?.value ?: return
        val milestone =
            when {
                before < FULL && goal.percent >= FULL -> Milestone.REACHED
                before < HALF && goal.percent >= HALF -> Milestone.HALF
                else -> null
            }
        mutableState.update { it.copy(goal = goal, milestone = milestone) }
    }

    fun setAutoContribution(
        amount: BigDecimal,
        walletId: String,
        frequency: Frequency,
        startDate: LocalDate?,
    ) = act { repository.setAutoContribution(id, AutoContributionRequest(amount, walletId, frequency, startDate)) }

    fun stopAutoContribution() = act { repository.stopAutoContribution(id) }

    fun setRoundUp(
        walletId: String,
        unit: BigDecimal,
    ) = act { repository.setRoundUp(id, RoundUpRequest(walletId, unit)) }

    fun stopRoundUp() = act { repository.stopRoundUp(id) }

    fun delete() {
        viewModelScope.launch {
            val result = repository.delete(id)
            mutableState.update {
                it.copy(
                    deleted = result is AppResult.Success,
                    error = (result as? AppResult.Failure)?.error,
                )
            }
        }
    }

    fun dismissMilestone() = mutableState.update { it.copy(milestone = null) }

    private fun act(block: suspend () -> AppResult<GoalDto>) {
        mutableState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val result = block()) {
                is AppResult.Success -> mutableState.update { it.copy(busy = false, goal = result.value) }
                is AppResult.Failure -> mutableState.update { it.copy(busy = false, error = result.error) }
            }
            repository.refresh()
        }
    }

    private companion object {
        const val HALF = 50
        const val FULL = 100
    }
}

data class GoalFormUiState(
    val isEdit: Boolean = false,
    val name: String = "",
    val target: String = "",
    val currency: CurrencyCode = CurrencyCode.DEFAULT,
    val deadline: LocalDate? = null,
    val hadDeadline: Boolean = false,
    val nameMissing: Boolean = false,
    val invalidTarget: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: AppError? = null,
)

class GoalFormViewModel(
    private val id: String?,
    private val repository: SavingRepository,
    profiles: ProfileRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(GoalFormUiState(isEdit = id != null))
    val state: StateFlow<GoalFormUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            mutableState.update { it.copy(currency = profiles.current()?.defaultCurrency ?: CurrencyCode.DEFAULT) }
            if (id != null) {
                (repository.get(id) as? AppResult.Success)?.value?.let { goal ->
                    mutableState.update {
                        it.copy(
                            name = goal.name,
                            target = goal.target?.toPlainString(goal.currencyCode.fractionDigits).orEmpty(),
                            currency = goal.currencyCode,
                            deadline = goal.deadline,
                            hadDeadline = goal.deadline != null,
                        )
                    }
                }
            }
        }
    }

    fun onName(value: String) = mutableState.update { it.copy(name = value, nameMissing = false) }

    fun onTarget(value: String) = mutableState.update { it.copy(target = value, invalidTarget = false) }

    fun onCurrency(value: CurrencyCode) = mutableState.update { it.copy(currency = value) }

    fun onDeadline(value: LocalDate?) = mutableState.update { it.copy(deadline = value) }

    fun save() {
        val form = mutableState.value
        val target = parseAmountInput(form.target, form.currency.fractionDigits)
        if (form.name.isBlank() || target == null || target.signum() <= 0) {
            mutableState.update {
                it.copy(
                    nameMissing = form.name.isBlank(),
                    invalidTarget =
                        target == null || target.signum() <= 0,
                )
            }
            return
        }
        mutableState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result =
                if (id == null) {
                    repository.create(CreateGoalRequest(form.name.trim(), target, form.currency.value, form.deadline))
                } else {
                    val clear = form.hadDeadline && form.deadline == null
                    repository.update(
                        id,
                        UpdateGoalRequest(form.name.trim(), target, form.deadline, clear.takeIf { it }),
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
}
