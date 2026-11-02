package com.financemanage.mobile.feature.transaction.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.data.cache.Resource
import com.financemanage.mobile.core.data.cache.ResourceHolder
import com.financemanage.mobile.core.data.outbox.OutboxEntity
import com.financemanage.mobile.core.data.shared.CategoryRepository
import com.financemanage.mobile.core.data.shared.ExchangeRateRepository
import com.financemanage.mobile.core.data.shared.ProfileRepository
import com.financemanage.mobile.core.data.shared.WalletRepository
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.domain.Category
import com.financemanage.mobile.core.model.domain.Wallet
import com.financemanage.mobile.core.model.time.today
import com.financemanage.mobile.core.model.toPlainString
import com.financemanage.mobile.core.network.IdempotencyKeyHolder
import com.financemanage.mobile.core.ui.components.parseAmountInput
import com.financemanage.mobile.feature.transaction.data.TransactionRepository
import com.financemanage.mobile.feature.transaction.data.TransferRequest
import com.financemanage.mobile.feature.transaction.data.UpdateRecurringRequest
import com.financemanage.mobile.feature.transaction.data.WriteOutcome
import com.financemanage.mobile.feature.transaction.domain.Attachment
import com.financemanage.mobile.feature.transaction.domain.Recurring
import com.financemanage.mobile.feature.transaction.domain.Transaction
import com.financemanage.mobile.feature.transaction.platform.PickedFile
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

private const val STOP_MS = 5_000L

// ---------------------------------------------------------------- Detail (F07 + F10)

data class TransactionDetailUiState(
    val transaction: Transaction? = null,
    val wallets: List<Wallet> = emptyList(),
    val categories: List<Category> = emptyList(),
    val images: Map<String, ByteArray> = emptyMap(),
    val uploading: Boolean = false,
    val loading: Boolean = true,
    val error: AppError? = null,
    val deleted: Boolean = false,
)

class TransactionDetailViewModel(
    private val transactionId: String,
    private val repository: TransactionRepository,
    private val wallets: WalletRepository,
    private val categories: CategoryRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TransactionDetailUiState())
    val state: StateFlow<TransactionDetailUiState> = mutableState.asStateFlow()

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
            when (val result = repository.get(transactionId)) {
                is AppResult.Success -> {
                    mutableState.update { it.copy(transaction = result.value, loading = false, error = null) }
                    loadImages(result.value.attachments)
                }
                is AppResult.Failure -> mutableState.update { it.copy(loading = false, error = result.error) }
            }
        }
    }

    private suspend fun loadImages(attachments: List<Attachment>) {
        attachments.filter { it.isImage && it.id !in mutableState.value.images }.forEach { attachment ->
            (repository.downloadAttachment(attachment.id) as? AppResult.Success)?.let { result ->
                mutableState.update { it.copy(images = it.images + (attachment.id to result.value)) }
            }
        }
    }

    suspend fun download(attachment: Attachment): ByteArray? =
        (repository.downloadAttachment(attachment.id) as? AppResult.Success)?.value

    fun upload(file: PickedFile) {
        mutableState.update { it.copy(uploading = true, error = null) }
        viewModelScope.launch {
            val result = repository.uploadAttachment(transactionId, file.bytes, file.fileName, file.contentType)
            mutableState.update { it.copy(uploading = false, error = (result as? AppResult.Failure)?.error) }
            refresh()
        }
    }

    fun deleteAttachment(attachment: Attachment) {
        viewModelScope.launch {
            val result = repository.deleteAttachment(attachment.id)
            mutableState.update { it.copy(error = (result as? AppResult.Failure)?.error) }
            refresh()
        }
    }

    fun delete() {
        val transaction = mutableState.value.transaction ?: return
        viewModelScope.launch {
            val transferId = transaction.transferId
            val result =
                if (transferId !=
                    null
                ) {
                    repository.deleteTransfer(transferId)
                } else {
                    repository.delete(transaction.id)
                }
            mutableState.update {
                it.copy(
                    deleted = result is AppResult.Success,
                    error = (result as? AppResult.Failure)?.error,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Transfer (F08)

data class TransferUiState(
    val wallets: List<Wallet> = emptyList(),
    val fromId: String? = null,
    val toId: String? = null,
    val amount: String = "",
    val received: String = "",
    val receivedEdited: Boolean = false,
    val estimatedRate: BigDecimal? = null,
    val date: LocalDate? = null,
    val note: String = "",
    val invalidAmount: Boolean = false,
    val sameWallet: Boolean = false,
    val saving: Boolean = false,
    val done: Boolean = false,
    val queued: Boolean = false,
    val error: AppError? = null,
) {
    val from: Wallet? get() = wallets.firstOrNull { it.id == fromId }
    val to: Wallet? get() = wallets.firstOrNull { it.id == toId }
    val crossCurrency: Boolean get() = from != null && to != null && from?.currency != to?.currency
}

class TransferViewModel(
    private val repository: TransactionRepository,
    private val wallets: WalletRepository,
    private val rates: ExchangeRateRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TransferUiState())
    val state: StateFlow<TransferUiState> = mutableState.asStateFlow()
    private val idempotency = IdempotencyKeyHolder()

    init {
        viewModelScope.launch {
            val list =
                (wallets.wallets.first()?.value ?: (wallets.refresh() as? AppResult.Success)?.value.orEmpty())
                    .filter {
                        !it.archived
                    }
            mutableState.update {
                it.copy(
                    wallets = list,
                    fromId = list.getOrNull(0)?.id,
                    toId = list.getOrNull(1)?.id,
                    date = today(profiles.current()?.timeZone),
                )
            }
        }
    }

    fun onFrom(wallet: Wallet) = edit { it.copy(fromId = wallet.id, receivedEdited = false) }.also { estimate() }

    fun onTo(wallet: Wallet) = edit { it.copy(toId = wallet.id, receivedEdited = false) }.also { estimate() }

    fun onAmount(value: String) = edit { it.copy(amount = value) }.also { estimate() }

    fun onReceived(value: String) = edit { it.copy(received = value, receivedEdited = true) }

    fun onDate(value: LocalDate) = edit { it.copy(date = value) }

    fun onNote(value: String) = edit { it.copy(note = value) }

    private fun edit(transform: (TransferUiState) -> TransferUiState) {
        mutableState.update { transform(it).copy(invalidAmount = false, sameWallet = false, error = null) }
        idempotency.reset()
    }

    /** Estimated received amount from the user's exchange rates (F11); the user can overwrite it. */
    private fun estimate() {
        val current = mutableState.value
        val from = current.from ?: return
        val to = current.to ?: return
        if (from.currency == to.currency) return
        val amount = parseAmountInput(current.amount, from.currency.fractionDigits) ?: return
        viewModelScope.launch {
            val conversion =
                (
                    rates.convert(
                        amount,
                        from.currency,
                        to.currency,
                        current.date,
                    ) as? AppResult.Success
                )?.value
            mutableState.update {
                val converted = conversion?.converted
                it.copy(
                    estimatedRate = conversion?.rate,
                    received =
                        if (!it.receivedEdited &&
                            converted != null
                        ) {
                            converted.toPlainString(to.currency.fractionDigits)
                        } else {
                            it.received
                        },
                )
            }
        }
    }

    fun save() {
        val form = mutableState.value
        val from = form.from ?: return
        val to = form.to ?: return
        if (from.id == to.id) {
            mutableState.update { it.copy(sameWallet = true) }
            return
        }
        val amount = parseAmountInput(form.amount, from.currency.fractionDigits)
        val receivedTyped = form.crossCurrency && form.received.isNotBlank()
        val received = if (receivedTyped) parseAmountInput(form.received, to.currency.fractionDigits) else null
        val amountInvalid = amount == null || amount.signum() <= 0
        if (amountInvalid || (receivedTyped && received == null)) {
            mutableState.update { it.copy(invalidAmount = true) }
            return
        }
        mutableState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val request =
                TransferRequest(from.id, to.id, amount, received, form.date, form.note.trim().ifEmpty { null })
            val summary = "${from.name} → ${to.name} · ${form.amount} ${from.currency.value}"
            when (val result = repository.transfer(request, idempotency.current(), summary)) {
                is AppResult.Success ->
                    mutableState.update {
                        it.copy(
                            saving = false,
                            done = true,
                            queued =
                                result.value == WriteOutcome.Queued,
                        )
                    }
                is AppResult.Failure -> mutableState.update { it.copy(saving = false, error = result.error) }
            }
        }
    }
}

// ---------------------------------------------------------------- Recurring (F09)

data class RecurringListUiState(
    val items: Resource<List<Recurring>> = Resource(loading = true),
    val wallets: List<Wallet> = emptyList(),
    val categories: List<Category> = emptyList(),
)

class RecurringListViewModel(
    private val repository: TransactionRepository,
    wallets: WalletRepository,
    categories: CategoryRepository,
) : ViewModel() {
    private val holder = ResourceHolder(viewModelScope, repository.recurring) { repository.refreshRecurring() }

    val state: StateFlow<RecurringListUiState> =
        combine(
            holder.state,
            wallets.wallets.map {
                it?.value.orEmpty()
            },
            categories.categories.map { it?.value.orEmpty() },
        ) { items, w, c ->
            RecurringListUiState(items, w, c)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), RecurringListUiState())

    fun refresh() = holder.refresh()

    fun setActive(
        item: Recurring,
        active: Boolean,
    ) {
        viewModelScope.launch { repository.updateRecurring(item.id, UpdateRecurringRequest(active = active)) }
    }
}

data class RecurringEditUiState(
    val recurring: Recurring? = null,
    val wallets: List<Wallet> = emptyList(),
    val categories: List<Category> = emptyList(),
    val walletId: String? = null,
    val categoryId: String? = null,
    val amount: String = "",
    val note: String = "",
    val endDate: LocalDate? = null,
    val active: Boolean = true,
    val invalidAmount: Boolean = false,
    val saving: Boolean = false,
    val done: Boolean = false,
    val error: AppError? = null,
)

class RecurringEditViewModel(
    private val id: String,
    private val repository: TransactionRepository,
    private val wallets: WalletRepository,
    private val categories: CategoryRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(RecurringEditUiState())
    val state: StateFlow<RecurringEditUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val walletList =
                wallets.wallets
                    .first()
                    ?.value
                    .orEmpty()
                    .filter { !it.archived }
            val categoryList =
                categories.categories
                    .first()
                    ?.value
                    .orEmpty()
            when (val result = repository.getRecurring(id)) {
                is AppResult.Success -> {
                    val item = result.value
                    mutableState.update {
                        it.copy(
                            recurring = item,
                            wallets = walletList,
                            categories = categoryList,
                            walletId = item.walletId,
                            categoryId = item.categoryId,
                            amount = item.amount.amount.toPlainString(item.amount.currency.fractionDigits),
                            note = item.note.orEmpty(),
                            endDate = item.endDate,
                            active = item.active,
                        )
                    }
                }
                is AppResult.Failure ->
                    mutableState.update {
                        it.copy(error = result.error, wallets = walletList, categories = categoryList)
                    }
            }
        }
    }

    fun onWallet(wallet: Wallet) = mutableState.update { it.copy(walletId = wallet.id) }

    fun onCategory(category: Category) = mutableState.update { it.copy(categoryId = category.id) }

    fun onAmount(value: String) = mutableState.update { it.copy(amount = value, invalidAmount = false) }

    fun onNote(value: String) = mutableState.update { it.copy(note = value) }

    fun onEndDate(value: LocalDate) = mutableState.update { it.copy(endDate = value) }

    fun onActive(value: Boolean) = mutableState.update { it.copy(active = value) }

    fun save() {
        val form = mutableState.value
        val item = form.recurring ?: return
        val amount = parseAmountInput(form.amount, item.amount.currency.fractionDigits)
        if (amount == null || amount.signum() <= 0) {
            mutableState.update { it.copy(invalidAmount = true) }
            return
        }
        mutableState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result =
                repository.updateRecurring(
                    id,
                    UpdateRecurringRequest(
                        form.walletId,
                        form.categoryId,
                        amount,
                        form.note.trim(),
                        null,
                        form.endDate,
                        form.active,
                    ),
                )
            mutableState.update {
                it.copy(
                    saving = false,
                    done = result is AppResult.Success,
                    error = (result as? AppResult.Failure)?.error,
                )
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            val result = repository.deleteRecurring(id)
            mutableState.update {
                it.copy(
                    done = result is AppResult.Success,
                    error = (result as? AppResult.Failure)?.error,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Outbox (F21)

class OutboxViewModel(
    private val repository: TransactionRepository,
) : ViewModel() {
    val pending: StateFlow<List<OutboxEntity>> =
        repository.pendingWrites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

    fun retry(entry: OutboxEntity) {
        viewModelScope.launch { repository.retryPending(entry) }
    }

    fun discard(entry: OutboxEntity) {
        viewModelScope.launch { repository.discardPending(entry.id) }
    }
}
