package com.financemanage.mobile.feature.loan.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.data.cache.Resource
import com.financemanage.mobile.core.data.cache.memoryResource
import com.financemanage.mobile.core.data.shared.ProfileRepository
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.time.today
import com.financemanage.mobile.core.model.toBigDecimalOrNull
import com.financemanage.mobile.core.network.IdempotencyKeyHolder
import com.financemanage.mobile.core.ui.components.parseAmountInput
import com.financemanage.mobile.feature.loan.data.CreateLoanRequest
import com.financemanage.mobile.feature.loan.data.LoanDetailDto
import com.financemanage.mobile.feature.loan.data.LoanRepository
import com.financemanage.mobile.feature.loan.data.LoanSummaryDto
import com.financemanage.mobile.feature.loan.data.SchedulePreviewDto
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class LoanListViewModel(
    repository: LoanRepository,
) : ViewModel() {
    private val holder = viewModelScope.memoryResource { repository.list() }
    val state: StateFlow<Resource<List<LoanSummaryDto>>> = holder.state

    fun refresh() = holder.refresh()
}

data class LoanFormUiState(
    val counterparty: String = "",
    val direction: String = "BORROW",
    val type: String = "PERSONAL",
    val principal: String = "",
    val currency: CurrencyCode = CurrencyCode.DEFAULT,
    val rate: String = "",
    val termMonths: String = "12",
    val startDate: LocalDate? = null,
    val method: String = "ANNUITY",
    val feeRate: String = "",
    val note: String = "",
    val preview: SchedulePreviewDto? = null,
    val invalid: Boolean = false,
    val loading: Boolean = false,
    val createdId: String? = null,
    val error: AppError? = null,
)

class LoanFormViewModel(
    private val repository: LoanRepository,
    profiles: ProfileRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(LoanFormUiState())
    val state: StateFlow<LoanFormUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = profiles.current()
            mutableState.update {
                it.copy(
                    currency = profile?.defaultCurrency ?: CurrencyCode.DEFAULT,
                    startDate = today(profile?.timeZone),
                )
            }
        }
    }

    fun update(transform: (LoanFormUiState) -> LoanFormUiState) =
        mutableState.update {
            transform(it).copy(invalid = false, error = null, preview = null)
        }

    private fun request(form: LoanFormUiState): CreateLoanRequest? {
        val principal =
            parseAmountInput(form.principal, form.currency.fractionDigits)?.takeIf { it.signum() > 0 } ?: return null
        val term = form.termMonths.toIntOrNull()?.takeIf { it > 0 } ?: return null
        val start = form.startDate ?: return null
        if (form.counterparty.isBlank()) return null
        return CreateLoanRequest(
            counterpartyName = form.counterparty.trim(),
            direction = form.direction,
            type = form.type,
            principal = principal,
            currency = form.currency.value,
            annualRate = if (form.method == "INTEREST_FREE") null else form.rate.replace(",", ".").toBigDecimalOrNull(),
            termMonths = term,
            startDate = start,
            repaymentMethod = form.method,
            earlyRepaymentFeeRate = form.feeRate.replace(",", ".").toBigDecimalOrNull(),
            note = form.note.trim().ifEmpty { null },
        )
    }

    fun preview() =
        call { request ->
            repository.preview(request).also { r ->
                (r as? AppResult.Success)?.let {
                    mutableState.update { it.copy(preview = r.value) }
                }
            }
        }

    fun save() =
        call { request ->
            repository.create(request).also { r ->
                (r as? AppResult.Success)?.let {
                    mutableState.update { it.copy(createdId = r.value.summary.id) }
                }
            }
        }

    private fun call(block: suspend (CreateLoanRequest) -> AppResult<*>) {
        val request = request(mutableState.value)
        if (request == null) {
            mutableState.update { it.copy(invalid = true) }
            return
        }
        mutableState.update { it.copy(loading = true) }
        viewModelScope.launch {
            val result = block(request)
            mutableState.update { it.copy(loading = false, error = (result as? AppResult.Failure)?.error) }
        }
    }
}

enum class LoanAction { REPAY, PREPAY, RATE }

data class LoanDetailUiState(
    val loan: LoanDetailDto? = null,
    val today: LocalDate? = null,
    val busy: Boolean = false,
    val error: AppError? = null,
    val deleted: Boolean = false,
)

class LoanDetailViewModel(
    private val id: String,
    private val repository: LoanRepository,
    profiles: ProfileRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(LoanDetailUiState())
    val state: StateFlow<LoanDetailUiState> = mutableState.asStateFlow()

    /** One key per repay/prepay action, reused on retries (API §2). */
    private val idempotency = IdempotencyKeyHolder()

    init {
        viewModelScope.launch { mutableState.update { it.copy(today = today(profiles.current()?.timeZone)) } }
    }

    fun refresh() {
        viewModelScope.launch {
            when (val result = repository.get(id)) {
                is AppResult.Success -> mutableState.update { it.copy(loan = result.value) }
                is AppResult.Failure -> mutableState.update { it.copy(error = result.error) }
            }
        }
    }

    fun newAction() = idempotency.reset()

    fun perform(
        action: LoanAction,
        amount: BigDecimal,
        date: LocalDate?,
    ) {
        mutableState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val result =
                when (action) {
                    LoanAction.REPAY -> repository.repay(id, amount, date, idempotency.current())
                    LoanAction.PREPAY -> repository.prepay(id, amount, date, idempotency.current())
                    LoanAction.RATE -> repository.changeRate(id, date ?: return@launch, amount)
                }
            if (result is AppResult.Success) idempotency.reset()
            mutableState.update { it.copy(busy = false, error = (result as? AppResult.Failure)?.error) }
            refresh()
        }
    }

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
}
