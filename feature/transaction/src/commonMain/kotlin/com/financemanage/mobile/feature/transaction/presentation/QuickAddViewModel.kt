package com.financemanage.mobile.feature.transaction.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.data.settings.AppSettingsRepository
import com.financemanage.mobile.core.data.shared.CategoryRepository
import com.financemanage.mobile.core.data.shared.ProfileRepository
import com.financemanage.mobile.core.data.shared.WalletRepository
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.domain.Category
import com.financemanage.mobile.core.model.domain.CategoryType
import com.financemanage.mobile.core.model.domain.Frequency
import com.financemanage.mobile.core.model.domain.TransactionType
import com.financemanage.mobile.core.model.domain.Wallet
import com.financemanage.mobile.core.model.time.today
import com.financemanage.mobile.core.model.toPlainString
import com.financemanage.mobile.core.network.IdempotencyKeyHolder
import com.financemanage.mobile.core.ui.components.AmountInput
import com.financemanage.mobile.core.ui.components.KeypadKey
import com.financemanage.mobile.feature.transaction.data.CreateRecurringRequest
import com.financemanage.mobile.feature.transaction.data.CreateTransactionRequest
import com.financemanage.mobile.feature.transaction.data.TransactionRepository
import com.financemanage.mobile.feature.transaction.data.UpdateTransactionRequest
import com.financemanage.mobile.feature.transaction.data.WriteOutcome
import com.financemanage.mobile.feature.transaction.platform.PickedFile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

enum class QuickAddProblem { AMOUNT, CATEGORY, WALLET }

data class RepeatSettings(
    val enabled: Boolean = false,
    val frequency: Frequency = Frequency.MONTHLY,
    val interval: Int = 1,
    val endDate: LocalDate? = null,
)

data class QuickAddUiState(
    val isEdit: Boolean = false,
    val isIncome: Boolean = false,
    val amount: AmountInput = AmountInput(),
    val wallets: List<Wallet> = emptyList(),
    val categories: List<Category> = emptyList(),
    val recentCategoryIds: List<String> = emptyList(),
    val walletId: String? = null,
    val categoryId: String? = null,
    val date: LocalDate? = null,
    val today: LocalDate? = null,
    val note: String = "",
    val withPerson: String = "",
    val excludeFromReport: Boolean = false,
    val repeat: RepeatSettings = RepeatSettings(),
    val files: List<PickedFile> = emptyList(),
    val addAnother: Boolean = false,
    val problems: Set<QuickAddProblem> = emptySet(),
    val saving: Boolean = false,
    val uploadProgress: Pair<Int, Int>? = null,
    val error: AppError? = null,
) {
    val wallet: Wallet? get() = wallets.firstOrNull { it.id == walletId }
    val category: Category? get() = categories.firstOrNull { it.id == categoryId }
}

sealed interface QuickAddEvent {
    data object Saved : QuickAddEvent

    /** Saved offline: will be sent later. [droppedFiles] when receipts could not be attached. */
    data class Queued(
        val droppedFiles: Boolean,
    ) : QuickAddEvent

    /** "Add another" is on: the form was cleared for the next entry. */
    data object SavedAndCleared : QuickAddEvent

    data object Done : QuickAddEvent
}

/**
 * Quick add (F07) – also edits an existing income/expense when [transactionId] is set.
 * One Idempotency-Key per save action, kept for every retry (API §2).
 */
class QuickAddViewModel(
    initialKind: String,
    private val transactionId: String?,
    private val repository: TransactionRepository,
    private val wallets: WalletRepository,
    private val categories: CategoryRepository,
    private val settings: AppSettingsRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {
    private val mutableState =
        MutableStateFlow(QuickAddUiState(isEdit = transactionId != null, isIncome = initialKind == "INCOME"))
    val state: StateFlow<QuickAddUiState> = mutableState.asStateFlow()
    private val eventChannel = Channel<QuickAddEvent>(Channel.BUFFERED)
    val events: Flow<QuickAddEvent> = eventChannel.receiveAsFlow()
    private val idempotency = IdempotencyKeyHolder()

    init {
        viewModelScope.launch {
            val walletList =
                wallets.wallets.first()?.value ?: (wallets.refresh() as? AppResult.Success)?.value.orEmpty()
            val categoryList =
                categories.categories.first()?.value ?: (categories.refresh() as? AppResult.Success)?.value.orEmpty()
            val preferences = settings.preferences.first()
            val today = today(profiles.current()?.timeZone)
            val usable = walletList.filter { !it.archived }
            val walletId =
                preferences.lastWalletId?.takeIf { id -> usable.any { it.id == id } } ?: usable.firstOrNull()?.id
            mutableState.update {
                it.copy(
                    wallets = usable,
                    categories = categoryList,
                    recentCategoryIds = preferences.recentCategoryIds,
                    walletId = walletId,
                    date = today,
                    today = today,
                    amount =
                        AmountInput(
                            maxFractionDigits =
                                usable.firstOrNull { w -> w.id == walletId }?.currency?.fractionDigits ?: 0,
                        ),
                )
            }
            if (transactionId != null) loadExisting(transactionId, walletList)
        }
    }

    private suspend fun loadExisting(
        id: String,
        allWallets: List<Wallet>,
    ) {
        when (val result = repository.get(id)) {
            is AppResult.Success -> {
                val transaction = result.value
                val digits = transaction.amount.currency.fractionDigits
                mutableState.update {
                    it.copy(
                        // Archived wallets are kept selectable when editing an existing transaction.
                        wallets =
                            (
                                it.wallets +
                                    allWallets.filter { w ->
                                        w.id == transaction.walletId
                                    }
                            ).distinctBy { w -> w.id },
                        isIncome = transaction.type == TransactionType.INCOME,
                        amount =
                            AmountInput(
                                transaction.amount.amount
                                    .toPlainString(digits)
                                    .toKeypadRaw(),
                                digits,
                            ),
                        walletId = transaction.walletId,
                        categoryId = transaction.categoryId,
                        date = transaction.occurredOn ?: it.date,
                        note = transaction.note.orEmpty(),
                        withPerson = transaction.withPerson.orEmpty(),
                        excludeFromReport = transaction.excludeFromReport,
                    )
                }
            }
            is AppResult.Failure -> mutableState.update { it.copy(error = result.error) }
        }
    }

    fun setIncome(income: Boolean) =
        edit {
            val keepCategory = it.category?.type == (if (income) CategoryType.INCOME else CategoryType.EXPENSE)
            it.copy(isIncome = income, categoryId = if (keepCategory) it.categoryId else null)
        }

    fun onKey(key: KeypadKey) = edit { it.copy(amount = it.amount.press(key)) }

    fun onWallet(wallet: Wallet) =
        edit {
            it.copy(
                walletId = wallet.id,
                amount = it.amount.copy(maxFractionDigits = wallet.currency.fractionDigits),
            )
        }

    fun onCategory(category: Category) = edit { it.copy(categoryId = category.id) }

    fun onDate(date: LocalDate) = edit { it.copy(date = date) }

    fun setToday() = edit { it.copy(date = it.today) }

    fun setYesterday() = edit { it.copy(date = it.today?.minus(DatePeriod(days = 1))) }

    fun onNote(value: String) = edit { it.copy(note = value) }

    fun onPerson(value: String) = edit { it.copy(withPerson = value) }

    fun onExclude(value: Boolean) = edit { it.copy(excludeFromReport = value) }

    fun onRepeat(value: RepeatSettings) = edit { it.copy(repeat = value) }

    fun onAddAnother(value: Boolean) = mutableState.update { it.copy(addAnother = value) }

    fun addFile(file: PickedFile) =
        mutableState.update {
            if (it.files.size <
                MAX_FILES
            ) {
                it.copy(files = it.files + file)
            } else {
                it
            }
        }

    fun removeFile(file: PickedFile) = mutableState.update { it.copy(files = it.files - file) }

    /** Any change after a failed attempt starts a new action, so a new key is used. */
    private fun edit(transform: (QuickAddUiState) -> QuickAddUiState) {
        mutableState.update { transform(it).copy(problems = emptySet(), error = null) }
        idempotency.reset()
    }

    fun save() {
        val form = mutableState.value
        if (form.saving) return
        val problems =
            buildSet {
                if (form.amount.value.signum() <= 0) add(QuickAddProblem.AMOUNT)
                if (form.categoryId == null) add(QuickAddProblem.CATEGORY)
                if (form.walletId == null) add(QuickAddProblem.WALLET)
            }
        if (problems.isNotEmpty()) {
            mutableState.update { it.copy(problems = problems) }
            return
        }
        mutableState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            when {
                transactionId != null -> saveEdit(transactionId, form)
                form.repeat.enabled -> saveRecurring(form)
                else -> saveNew(form)
            }
        }
    }

    private suspend fun saveEdit(
        id: String,
        form: QuickAddUiState,
    ) {
        val result =
            repository.update(
                id,
                UpdateTransactionRequest(
                    walletId = form.walletId,
                    categoryId = form.categoryId,
                    amount = form.amount.value,
                    occurredOn = form.date,
                    note = form.note.trim(),
                    withPerson = form.withPerson.trim(),
                    excludeFromReport = form.excludeFromReport,
                ),
            )
        finish(result) { eventChannel.send(QuickAddEvent.Done) }
    }

    private suspend fun saveRecurring(form: QuickAddUiState) {
        val result =
            repository.createRecurring(
                CreateRecurringRequest(
                    walletId = requireNotNull(form.walletId),
                    categoryId = requireNotNull(form.categoryId),
                    amount = form.amount.value,
                    note = form.note.trim().ifEmpty { null },
                    withPerson = form.withPerson.trim().ifEmpty { null },
                    frequency = form.repeat.frequency,
                    interval = form.repeat.interval,
                    startDate = requireNotNull(form.date),
                    endDate = form.repeat.endDate,
                ),
            )
        finish(result) { afterCreate(form) }
    }

    private suspend fun saveNew(form: QuickAddUiState) {
        val request =
            CreateTransactionRequest(
                walletId = requireNotNull(form.walletId),
                categoryId = requireNotNull(form.categoryId),
                amount = form.amount.value,
                occurredOn = form.date,
                note = form.note.trim().ifEmpty { null },
                withPerson = form.withPerson.trim().ifEmpty { null },
                excludeFromReport = form.excludeFromReport.takeIf { it },
            )
        val currencyCode =
            form.wallet
                ?.currency
                ?.value
                .orEmpty()
        val summary = "${form.category?.name.orEmpty()} · ${form.amount.display} $currencyCode"
        when (val result = repository.create(request, idempotency.current(), summary)) {
            is AppResult.Failure -> mutableState.update { it.copy(saving = false, error = result.error) }
            is AppResult.Success -> {
                val (outcome, transaction) = result.value
                if (outcome == WriteOutcome.Queued) {
                    eventChannel.send(QuickAddEvent.Queued(droppedFiles = form.files.isNotEmpty()))
                } else if (transaction != null && form.files.isNotEmpty()) {
                    uploadFiles(transaction.id, form.files)
                }
                afterCreate(form)
            }
        }
    }

    private suspend fun uploadFiles(
        transactionId: String,
        files: List<PickedFile>,
    ) {
        files.forEachIndexed { index, file ->
            mutableState.update { it.copy(uploadProgress = index + 1 to files.size) }
            val result = repository.uploadAttachment(transactionId, file.bytes, file.fileName, file.contentType)
            if (result is AppResult.Failure) mutableState.update { it.copy(error = result.error) }
        }
        mutableState.update { it.copy(uploadProgress = null) }
    }

    private suspend fun afterCreate(form: QuickAddUiState) {
        idempotency.reset()
        form.categoryId?.let { settings.rememberCategory(it) }
        form.walletId?.let { settings.setLastWallet(it) }
        if (form.addAnother) {
            mutableState.update {
                it.copy(
                    saving = false,
                    amount = AmountInput(maxFractionDigits = it.amount.maxFractionDigits),
                    note = "",
                    withPerson = "",
                    files = emptyList(),
                    repeat = RepeatSettings(),
                    recentCategoryIds = settings.preferences.first().recentCategoryIds,
                )
            }
            eventChannel.send(QuickAddEvent.SavedAndCleared)
        } else {
            mutableState.update { it.copy(saving = false) }
            eventChannel.send(QuickAddEvent.Done)
        }
    }

    private suspend fun finish(
        result: AppResult<*>,
        onSuccess: suspend () -> Unit,
    ) {
        when (result) {
            is AppResult.Success -> onSuccess()
            is AppResult.Failure -> mutableState.update { it.copy(saving = false, error = result.error) }
        }
    }

    private companion object {
        const val MAX_FILES = 5
    }
}

/** "12.50" -> "12.5", "45000" -> "45000": the keypad keeps only the typed decimals. */
internal fun String.toKeypadRaw(): String = if (contains('.')) trimEnd('0').trimEnd('.') else this
