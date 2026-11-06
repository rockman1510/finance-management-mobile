package com.financemanage.mobile.feature.deposit.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.domain.usecase.GetCachedWalletsUseCase
import com.financemanage.mobile.core.domain.usecase.GetDefaultCurrencyUseCase
import com.financemanage.mobile.core.domain.usecase.GetTodayUseCase
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.AsyncStatus
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.PollResult
import com.financemanage.mobile.core.model.domain.Wallet
import com.financemanage.mobile.core.model.toBigDecimalOrNull
import com.financemanage.mobile.core.ui.components.PendingAction
import com.financemanage.mobile.core.ui.components.parseAmountInput
import com.financemanage.mobile.core.ui.state.Resource
import com.financemanage.mobile.core.ui.state.ResourceHolder
import com.financemanage.mobile.feature.deposit.domain.model.CreateDepositRequest
import com.financemanage.mobile.feature.deposit.domain.model.DepositDto
import com.financemanage.mobile.feature.deposit.domain.model.DepositPreviewDto
import com.financemanage.mobile.feature.deposit.domain.model.SettlementQuoteDto
import com.financemanage.mobile.feature.deposit.domain.usecase.AwaitDepositRecordedUseCase
import com.financemanage.mobile.feature.deposit.domain.usecase.CreateDepositUseCase
import com.financemanage.mobile.feature.deposit.domain.usecase.DeleteDepositUseCase
import com.financemanage.mobile.feature.deposit.domain.usecase.GetDepositUseCase
import com.financemanage.mobile.feature.deposit.domain.usecase.GetSettlementQuoteUseCase
import com.financemanage.mobile.feature.deposit.domain.usecase.ObserveDepositsUseCase
import com.financemanage.mobile.feature.deposit.domain.usecase.PreviewDepositUseCase
import com.financemanage.mobile.feature.deposit.domain.usecase.RefreshDepositsUseCase
import com.financemanage.mobile.feature.deposit.domain.usecase.SettleDepositUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class DepositListViewModel(
    observeDeposits: ObserveDepositsUseCase,
    refreshDeposits: RefreshDepositsUseCase,
) : ViewModel() {
    private val holder = ResourceHolder(viewModelScope, observeDeposits()) { refreshDeposits() }
    val state: StateFlow<Resource<List<DepositDto>>> = holder.state

    fun refresh() = holder.refresh()
}

private fun pendingFrom(
    deposit: DepositDto?,
    timedOut: Boolean,
): PendingAction? =
    when {
        deposit == null -> null
        timedOut -> PendingAction.WillUpdate
        deposit.fundingStatus == AsyncStatus.FAILED || deposit.payoutStatus == AsyncStatus.FAILED ->
            PendingAction
                .Failed(
                    null,
                )
        else -> PendingAction.Recorded
    }

data class DepositFormUiState(
    val institution: String = "",
    val accountNumber: String = "",
    val type: String = "TERM",
    val principal: String = "",
    val currency: CurrencyCode = CurrencyCode.DEFAULT,
    val annualRate: String = "",
    val earlyRate: String = "",
    val termMonths: String = "6",
    val openDate: LocalDate? = null,
    val interestPayment: String = "AT_MATURITY",
    val rollover: String = "NONE",
    val calculation: String = "SIMPLE_ACTUAL_365",
    val note: String = "",
    val fromWalletId: String? = null,
    val wallets: List<Wallet> = emptyList(),
    val preview: DepositPreviewDto? = null,
    val invalid: Boolean = false,
    val saving: Boolean = false,
    val pending: PendingAction? = null,
    val createdId: String? = null,
    val error: AppError? = null,
)

/** Create a deposit (F16) with a preview block updated while typing. */
class DepositFormViewModel(
    private val previewDeposit: PreviewDepositUseCase,
    private val createDeposit: CreateDepositUseCase,
    private val awaitDepositRecorded: AwaitDepositRecordedUseCase,
    getCachedWallets: GetCachedWalletsUseCase,
    getDefaultCurrency: GetDefaultCurrencyUseCase,
    getToday: GetTodayUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DepositFormUiState())
    val state: StateFlow<DepositFormUiState> = mutableState.asStateFlow()
    private var previewJob: Job? = null

    init {
        viewModelScope.launch {
            val currency = getDefaultCurrency()
            val openDate = getToday()
            val walletList = getCachedWallets().filter { w -> !w.archived }
            mutableState.update {
                it.copy(
                    currency = currency,
                    openDate = openDate,
                    wallets = walletList,
                )
            }
        }
    }

    fun update(transform: (DepositFormUiState) -> DepositFormUiState) {
        mutableState.update { transform(it).copy(invalid = false, error = null) }
        schedulePreview()
    }

    private fun request(form: DepositFormUiState): CreateDepositRequest? {
        val principal = parseAmountInput(form.principal, form.currency.fractionDigits) ?: return null
        val rate = form.annualRate.replace(",", ".").toBigDecimalOrNull() ?: return null
        val open = form.openDate ?: return null
        if (form.institution.isBlank() || principal.signum() <= 0) return null
        val term = form.termMonths.toIntOrNull()
        if (form.type == "TERM" && (term == null || term <= 0)) return null
        return CreateDepositRequest(
            institutionName = form.institution.trim(),
            accountNumber = form.accountNumber.trim().ifEmpty { null },
            type = form.type,
            principal = principal,
            currency = form.currency.value,
            annualRate = rate,
            earlyWithdrawalRate = form.earlyRate.replace(",", ".").toBigDecimalOrNull(),
            termMonths = if (form.type == "TERM") term else null,
            openDate = open,
            interestPaymentMethod = form.interestPayment,
            rolloverPolicy = form.rollover,
            calculationMethod = form.calculation,
            note = form.note.trim().ifEmpty { null },
            fromWalletId = form.fromWalletId,
        )
    }

    private fun schedulePreview() {
        previewJob?.cancel()
        previewJob =
            viewModelScope.launch {
                delay(PREVIEW_DEBOUNCE_MS)
                val request = request(mutableState.value) ?: return@launch
                (previewDeposit(request) as? AppResult.Success)?.let { result ->
                    mutableState.update { it.copy(preview = result.value) }
                }
            }
    }

    fun save() {
        val request = request(mutableState.value)
        if (request == null) {
            mutableState.update { it.copy(invalid = true) }
            return
        }
        mutableState.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val result = createDeposit(request)) {
                is AppResult.Failure -> mutableState.update { it.copy(saving = false, error = result.error) }
                is AppResult.Success -> {
                    var pending: PendingAction? = null
                    if (result.value.anyPending) {
                        mutableState.update { it.copy(pending = PendingAction.Processing) }
                        val poll = awaitDepositRecorded(result.value.id)
                        pending =
                            when (poll) {
                                is PollResult.Settled -> pendingFrom(poll.value, false)
                                is PollResult.TimedOut -> PendingAction.WillUpdate
                            }
                    }
                    mutableState.update { it.copy(saving = false, pending = pending, createdId = result.value.id) }
                }
            }
        }
    }

    private companion object {
        const val PREVIEW_DEBOUNCE_MS = 500L
    }
}

data class DepositDetailUiState(
    val deposit: DepositDto? = null,
    val wallets: List<Wallet> = emptyList(),
    val today: LocalDate? = null,
    val quote: SettlementQuoteDto? = null,
    val pending: PendingAction? = null,
    val busy: Boolean = false,
    val error: AppError? = null,
    val deleted: Boolean = false,
)

class DepositDetailViewModel(
    private val id: String,
    private val getDeposit: GetDepositUseCase,
    private val getSettlementQuote: GetSettlementQuoteUseCase,
    private val settleDeposit: SettleDepositUseCase,
    private val awaitDepositRecorded: AwaitDepositRecordedUseCase,
    private val deleteDeposit: DeleteDepositUseCase,
    getCachedWallets: GetCachedWalletsUseCase,
    getToday: GetTodayUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DepositDetailUiState())
    val state: StateFlow<DepositDetailUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    wallets =
                        getCachedWallets().filter { w ->
                            !w.archived
                        },
                    today = getToday(),
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            when (val result = getDeposit(id)) {
                is AppResult.Success -> mutableState.update { it.copy(deposit = result.value) }
                is AppResult.Failure -> mutableState.update { it.copy(error = result.error) }
            }
        }
    }

    fun loadQuote(date: LocalDate?) {
        viewModelScope.launch {
            when (val result = getSettlementQuote(id, date)) {
                is AppResult.Success -> mutableState.update { it.copy(quote = result.value, error = null) }
                is AppResult.Failure -> mutableState.update { it.copy(quote = null, error = result.error) }
            }
        }
    }

    fun settle(
        date: LocalDate?,
        toWalletId: String?,
    ) {
        mutableState.update {
            it.copy(
                busy = true,
                error = null,
                pending =
                    if (toWalletId !=
                        null
                    ) {
                        PendingAction.Processing
                    } else {
                        null
                    },
            )
        }
        viewModelScope.launch {
            when (val result = settleDeposit(id, date, toWalletId)) {
                is AppResult.Failure ->
                    mutableState.update {
                        it.copy(
                            busy = false,
                            pending = null,
                            error = result.error,
                        )
                    }
                is AppResult.Success -> {
                    val pending =
                        if (toWalletId != null) {
                            when (val poll = awaitDepositRecorded(id)) {
                                is PollResult.Settled -> pendingFrom(poll.value, false)
                                is PollResult.TimedOut -> PendingAction.WillUpdate
                            }
                        } else {
                            PendingAction.Recorded
                        }
                    mutableState.update { it.copy(busy = false, pending = pending) }
                    refresh()
                }
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            val result = deleteDeposit(id)
            mutableState.update {
                it.copy(
                    deleted = result is AppResult.Success,
                    error = (result as? AppResult.Failure)?.error,
                )
            }
        }
    }
}
