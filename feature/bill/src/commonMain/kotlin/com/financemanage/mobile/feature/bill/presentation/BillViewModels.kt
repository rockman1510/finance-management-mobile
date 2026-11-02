package com.financemanage.mobile.feature.bill.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.data.cache.Resource
import com.financemanage.mobile.core.data.cache.ResourceHolder
import com.financemanage.mobile.core.data.shared.CategoryRepository
import com.financemanage.mobile.core.data.shared.ProfileRepository
import com.financemanage.mobile.core.data.shared.WalletRepository
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.domain.Category
import com.financemanage.mobile.core.model.domain.CategoryType
import com.financemanage.mobile.core.model.domain.Frequency
import com.financemanage.mobile.core.model.domain.Wallet
import com.financemanage.mobile.core.model.time.today
import com.financemanage.mobile.core.model.toPlainString
import com.financemanage.mobile.core.ui.components.PendingAction
import com.financemanage.mobile.core.ui.components.parseAmountInput
import com.financemanage.mobile.feature.bill.data.Bill
import com.financemanage.mobile.feature.bill.data.BillRepository
import com.financemanage.mobile.feature.bill.data.CreateBillRequest
import com.financemanage.mobile.feature.bill.data.PayBillRequest
import com.financemanage.mobile.feature.bill.data.UpdateBillRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** Pays a bill and maps the PENDING flow to a [PendingAction]. Shared by the list and the detail. */
internal suspend fun payBill(
    repository: BillRepository,
    bill: Bill,
    amount: com.ionspin.kotlin.bignum.decimal.BigDecimal? = null,
    walletId: String? = null,
    paidOn: LocalDate? = null,
): Pair<PendingAction?, AppError?> =
    when (val result = repository.pay(bill.id, PayBillRequest(amount, walletId, paidOn))) {
        is AppResult.Failure -> null to result.error
        is AppResult.Success -> PendingAction.from(result.value, { it.status }, { it.failureReason }) to null
    }

data class BillListUiState(
    val bills: Resource<List<Bill>> = Resource(loading = true),
    val payingId: String? = null,
    val pending: PendingAction? = null,
    val error: AppError? = null,
)

class BillListViewModel(
    private val repository: BillRepository,
) : ViewModel() {
    private val holder = ResourceHolder(viewModelScope, repository.bills) { repository.refresh() }
    private val action = MutableStateFlow(Triple<String?, PendingAction?, AppError?>(null, null, null))

    val state: StateFlow<BillListUiState> =
        combine(
            holder.state,
            action,
        ) { bills, (paying, pending, error) -> BillListUiState(bills, paying, pending, error) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), BillListUiState())

    fun refresh() = holder.refresh()

    /** Quick Pay with the bill's default amount and wallet. */
    fun pay(bill: Bill) {
        if (action.value.first != null) return
        action.value = Triple(bill.id, PendingAction.Processing, null)
        viewModelScope.launch {
            val (pending, error) = payBill(repository, bill)
            action.value = Triple(null, pending, error)
        }
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}

data class BillDetailUiState(
    val bill: Bill? = null,
    val wallets: List<Wallet> = emptyList(),
    val categories: List<Category> = emptyList(),
    val pending: PendingAction? = null,
    val busy: Boolean = false,
    val error: AppError? = null,
    val deleted: Boolean = false,
)

class BillDetailViewModel(
    private val id: String,
    private val repository: BillRepository,
    wallets: WalletRepository,
    categories: CategoryRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(BillDetailUiState())
    val state: StateFlow<BillDetailUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    wallets =
                        wallets.wallets
                            .first()
                            ?.value
                            .orEmpty(),
                    categories =
                        categories.categories
                            .first()
                            ?.value
                            .orEmpty(),
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            when (val result = repository.get(id)) {
                is AppResult.Success -> mutableState.update { it.copy(bill = result.value) }
                is AppResult.Failure -> mutableState.update { it.copy(error = result.error) }
            }
        }
    }

    fun pay(
        amount: com.ionspin.kotlin.bignum.decimal.BigDecimal?,
        walletId: String?,
        paidOn: LocalDate?,
    ) {
        val bill = mutableState.value.bill ?: return
        mutableState.update { it.copy(busy = true, pending = PendingAction.Processing, error = null) }
        viewModelScope.launch {
            val (pending, error) = payBill(repository, bill, amount, walletId, paidOn)
            mutableState.update { it.copy(busy = false, pending = pending, error = error) }
            refresh()
        }
    }

    fun skip() = act { repository.skip(id) }

    fun setActive(active: Boolean) = act { repository.update(id, UpdateBillRequest(active = active)) }

    fun delete() =
        act {
            repository.delete(id).also {
                if (it is AppResult.Success) {
                    mutableState.update { s ->
                        s.copy(deleted = true)
                    }
                }
            }
        }

    private fun act(block: suspend () -> AppResult<*>) {
        mutableState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val result = block()
            mutableState.update { it.copy(busy = false, error = (result as? AppResult.Failure)?.error) }
            if (!mutableState.value.deleted) refresh()
        }
    }
}

/** Quick templates mapped to default category codes (F14). */
enum class BillTemplate(
    val categoryCode: String,
) {
    ELECTRICITY("BILLS_ELECTRICITY"),
    WATER("BILLS_WATER"),
    INTERNET("BILLS_INTERNET"),
    PHONE("BILLS_PHONE"),
    RENT("BILLS_RENT"),
}

enum class BillProblem { NAME, AMOUNT, WALLET, CATEGORY, DATE }

data class BillFormUiState(
    val isEdit: Boolean = false,
    val name: String = "",
    val amount: String = "",
    val walletId: String? = null,
    val categoryId: String? = null,
    val frequency: Frequency = Frequency.MONTHLY,
    val interval: String = "1",
    val firstDueDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val remindDays: String = "3",
    val note: String = "",
    val wallets: List<Wallet> = emptyList(),
    val categories: List<Category> = emptyList(),
    val problems: Set<BillProblem> = emptySet(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: AppError? = null,
) {
    val wallet: Wallet? get() = wallets.firstOrNull { it.id == walletId }
}

class BillFormViewModel(
    private val id: String?,
    private val repository: BillRepository,
    wallets: WalletRepository,
    categories: CategoryRepository,
    profiles: ProfileRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(BillFormUiState(isEdit = id != null))
    val state: StateFlow<BillFormUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val walletList =
                wallets.wallets
                    .first()
                    ?.value
                    .orEmpty()
                    .filter { !it.archived }
            mutableState.update {
                it.copy(
                    wallets = walletList,
                    walletId = walletList.firstOrNull()?.id,
                    categories =
                        categories.categories.first()?.value.orEmpty().filter { c ->
                            c.type ==
                                CategoryType.EXPENSE
                        },
                    firstDueDate = today(profiles.current()?.timeZone),
                )
            }
            if (id != null) {
                (repository.get(id) as? AppResult.Success)?.value?.let { bill ->
                    mutableState.update {
                        it.copy(
                            name = bill.name,
                            amount = bill.amount.amount.toPlainString(bill.amount.currency.fractionDigits),
                            walletId = bill.walletId,
                            categoryId = bill.categoryId,
                            frequency = bill.frequency,
                            interval = bill.interval.toString(),
                            firstDueDate = bill.firstDueDate,
                            endDate = bill.endDate,
                            remindDays = bill.remindDaysBefore.toString(),
                            note = bill.note.orEmpty(),
                        )
                    }
                }
            }
        }
    }

    fun applyTemplate(
        template: BillTemplate,
        name: String,
    ) = edit { state ->
        state.copy(
            name = name,
            categoryId =
                state.categories.firstOrNull { it.code == template.categoryCode }?.id ?: state.categoryId,
        )
    }

    fun onName(value: String) = edit { it.copy(name = value) }

    fun onAmount(value: String) = edit { it.copy(amount = value) }

    fun onWallet(value: Wallet) = edit { it.copy(walletId = value.id) }

    fun onCategory(value: Category) = edit { it.copy(categoryId = value.id) }

    fun onFrequency(value: Frequency) = edit { it.copy(frequency = value) }

    fun onInterval(value: String) = edit { it.copy(interval = value.filter(Char::isDigit).take(MAX_INTERVAL_DIGITS)) }

    fun onFirstDue(value: LocalDate) = edit { it.copy(firstDueDate = value) }

    fun onEnd(value: LocalDate) = edit { it.copy(endDate = value) }

    fun onRemind(value: String) = edit { it.copy(remindDays = value.filter(Char::isDigit).take(MAX_REMIND_DIGITS)) }

    fun onNote(value: String) = edit { it.copy(note = value) }

    private fun edit(transform: (BillFormUiState) -> BillFormUiState) =
        mutableState.update {
            transform(it).copy(problems = emptySet(), error = null)
        }

    /** Validated values ready to send, or the problems found. */
    private data class ValidBill(
        val amount: com.ionspin.kotlin.bignum.decimal.BigDecimal,
        val walletId: String,
        val categoryId: String,
        val firstDue: LocalDate,
    )

    private fun validate(form: BillFormUiState): ValidBill? {
        val amount =
            form.wallet?.let { parseAmountInput(form.amount, it.currency.fractionDigits) }?.takeIf {
                it.signum() >
                    0
            }
        val problems =
            buildSet {
                if (form.name.isBlank()) add(BillProblem.NAME)
                if (amount == null) add(BillProblem.AMOUNT)
                if (form.walletId == null) add(BillProblem.WALLET)
                if (form.categoryId == null) add(BillProblem.CATEGORY)
                if (form.firstDueDate == null) add(BillProblem.DATE)
            }
        mutableState.update { it.copy(problems = problems) }
        if (problems.isNotEmpty()) return null
        return ValidBill(
            requireNotNull(amount),
            requireNotNull(form.walletId),
            requireNotNull(form.categoryId),
            requireNotNull(form.firstDueDate),
        )
    }

    fun save() {
        val form = mutableState.value
        val valid = validate(form) ?: return
        mutableState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val remind = form.remindDays.toIntOrNull()
            val note = form.note.trim()
            val result =
                if (id == null) {
                    repository.create(
                        CreateBillRequest(
                            name = form.name.trim(),
                            amount = valid.amount,
                            walletId = valid.walletId,
                            categoryId = valid.categoryId,
                            frequency = form.frequency,
                            interval = form.interval.toIntOrNull()?.coerceAtLeast(1),
                            firstDueDate = valid.firstDue,
                            endDate = form.endDate,
                            remindDaysBefore = remind,
                            note = note.ifEmpty { null },
                        ),
                    )
                } else {
                    repository.update(
                        id,
                        UpdateBillRequest(
                            form.name.trim(),
                            valid.amount,
                            valid.walletId,
                            valid.categoryId,
                            form.endDate,
                            remind,
                            note,
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
}

private const val MAX_INTERVAL_DIGITS = 3
private const val MAX_REMIND_DIGITS = 2
