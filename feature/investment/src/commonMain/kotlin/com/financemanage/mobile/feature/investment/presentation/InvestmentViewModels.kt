package com.financemanage.mobile.feature.investment.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.data.cache.Resource
import com.financemanage.mobile.core.data.cache.ResourceHolder
import com.financemanage.mobile.core.data.shared.CategoryRepository
import com.financemanage.mobile.core.data.shared.ProfileRepository
import com.financemanage.mobile.core.data.shared.WalletRepository
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.AsyncStatus
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.domain.Category
import com.financemanage.mobile.core.model.domain.CategoryType
import com.financemanage.mobile.core.model.domain.Wallet
import com.financemanage.mobile.core.model.time.today
import com.financemanage.mobile.core.network.pending.PollResult
import com.financemanage.mobile.core.ui.components.PendingAction
import com.financemanage.mobile.feature.investment.data.AssetDto
import com.financemanage.mobile.feature.investment.data.AssetRequest
import com.financemanage.mobile.feature.investment.data.IncomeRequest
import com.financemanage.mobile.feature.investment.data.InvestmentRepository
import com.financemanage.mobile.feature.investment.data.PortfolioDto
import com.financemanage.mobile.feature.investment.data.TradeKind
import com.financemanage.mobile.feature.investment.data.TradeRequest
import com.ionspin.kotlin.bignum.decimal.BigDecimal
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

data class PortfolioUiState(
    val assets: Resource<List<AssetDto>> = Resource(loading = true),
    val portfolio: PortfolioDto? = null,
    val currency: CurrencyCode = CurrencyCode.DEFAULT,
    val error: AppError? = null,
)

class PortfolioViewModel(
    private val repository: InvestmentRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {
    private val holder = ResourceHolder(viewModelScope, repository.assets) { repository.refresh() }
    private val extra = MutableStateFlow(PortfolioUiState())

    val state: StateFlow<PortfolioUiState> =
        combine(
            holder.state,
            extra,
        ) { assets, e ->
            e.copy(
                assets = assets,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), PortfolioUiState())

    init {
        viewModelScope.launch {
            extra.update {
                it.copy(
                    currency =
                        profiles.current()?.defaultCurrency ?: CurrencyCode.DEFAULT,
                )
            }
        }
    }

    fun refresh() {
        holder.refresh()
        loadPortfolio(extra.value.currency)
    }

    fun setCurrency(currency: CurrencyCode) {
        extra.update { it.copy(currency = currency) }
        loadPortfolio(currency)
    }

    private fun loadPortfolio(currency: CurrencyCode) {
        viewModelScope.launch {
            when (val result = repository.portfolio(currency)) {
                is AppResult.Success -> extra.update { it.copy(portfolio = result.value, error = null) }
                is AppResult.Failure -> extra.update { it.copy(error = result.error) }
            }
        }
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}

data class AssetDetailUiState(
    val asset: AssetDto? = null,
    val wallets: List<Wallet> = emptyList(),
    val incomeCategories: List<Category> = emptyList(),
    val today: LocalDate? = null,
    val pending: PendingAction? = null,
    val busy: Boolean = false,
    val error: AppError? = null,
    val deleted: Boolean = false,
)

class AssetDetailViewModel(
    private val id: String,
    private val repository: InvestmentRepository,
    wallets: WalletRepository,
    categories: CategoryRepository,
    profiles: ProfileRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AssetDetailUiState())
    val state: StateFlow<AssetDetailUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    wallets =
                        wallets.wallets
                            .first()
                            ?.value
                            .orEmpty()
                            .filter { w -> !w.archived },
                    incomeCategories =
                        categories.categories.first()?.value.orEmpty().filter { c ->
                            c.type ==
                                CategoryType.INCOME
                        },
                    today = today(profiles.current()?.timeZone),
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            when (val result = repository.get(id)) {
                is AppResult.Success -> mutableState.update { it.copy(asset = result.value) }
                is AppResult.Failure -> mutableState.update { it.copy(error = result.error) }
            }
        }
    }

    fun updatePrice(
        price: BigDecimal,
        date: LocalDate?,
    ) {
        mutableState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.updatePrice(id, price, date)) {
                is AppResult.Success -> mutableState.update { it.copy(busy = false, asset = result.value) }
                is AppResult.Failure -> mutableState.update { it.copy(busy = false, error = result.error) }
            }
        }
    }

    fun trade(
        kind: TradeKind,
        quantityOrAmount: BigDecimal,
        price: BigDecimal?,
        fee: BigDecimal?,
        walletId: String?,
        categoryId: String?,
        date: LocalDate?,
    ) {
        val body: Any =
            if (kind == TradeKind.INCOME) {
                IncomeRequest(quantityOrAmount, walletId, categoryId, date)
            } else {
                TradeRequest(quantityOrAmount, price ?: BigDecimal.ZERO, fee, walletId, date)
            }
        mutableState.update {
            it.copy(
                busy = true,
                error = null,
                pending =
                    if (walletId !=
                        null
                    ) {
                        PendingAction.Processing
                    } else {
                        null
                    },
            )
        }
        viewModelScope.launch {
            when (val result = repository.trade(id, kind, body)) {
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
                        when (val poll = result.value) {
                            is PollResult.TimedOut -> PendingAction.WillUpdate
                            is PollResult.Settled ->
                                when (poll.value.cashStatus) {
                                    AsyncStatus.FAILED -> PendingAction.Failed(null)
                                    AsyncStatus.NONE -> null
                                    else -> PendingAction.Recorded
                                }
                        }
                    mutableState.update { it.copy(busy = false, pending = pending) }
                    refresh()
                }
            }
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

data class AssetFormUiState(
    val isEdit: Boolean = false,
    val name: String = "",
    val symbol: String = "",
    val type: String = "STOCK",
    val currency: CurrencyCode = CurrencyCode.DEFAULT,
    val note: String = "",
    val nameMissing: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: AppError? = null,
)

class AssetFormViewModel(
    private val id: String?,
    private val repository: InvestmentRepository,
    profiles: ProfileRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AssetFormUiState(isEdit = id != null))
    val state: StateFlow<AssetFormUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            mutableState.update { it.copy(currency = profiles.current()?.defaultCurrency ?: CurrencyCode.DEFAULT) }
            if (id != null) {
                (repository.get(id) as? AppResult.Success)?.value?.let { asset ->
                    mutableState.update {
                        it.copy(
                            name = asset.name,
                            symbol = asset.symbol.orEmpty(),
                            type = asset.type,
                            currency = asset.currencyCode,
                            note = asset.note.orEmpty(),
                        )
                    }
                }
            }
        }
    }

    fun update(transform: (AssetFormUiState) -> AssetFormUiState) =
        mutableState.update {
            transform(it).copy(nameMissing = false, error = null)
        }

    fun save() {
        val form = mutableState.value
        if (form.name.isBlank()) {
            mutableState.update { it.copy(nameMissing = true) }
            return
        }
        mutableState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val request =
                AssetRequest(
                    form.name.trim(),
                    form.symbol.trim().ifEmpty {
                        null
                    },
                    form.type,
                    form.currency.value,
                    form.note.trim().ifEmpty { null },
                )
            val result = if (id == null) repository.create(request) else repository.update(id, request)
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
