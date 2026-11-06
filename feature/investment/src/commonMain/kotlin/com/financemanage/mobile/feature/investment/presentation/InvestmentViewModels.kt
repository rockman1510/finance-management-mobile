package com.financemanage.mobile.feature.investment.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.domain.usecase.GetCachedCategoriesUseCase
import com.financemanage.mobile.core.domain.usecase.GetCachedWalletsUseCase
import com.financemanage.mobile.core.domain.usecase.GetDefaultCurrencyUseCase
import com.financemanage.mobile.core.domain.usecase.GetTodayUseCase
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.AsyncStatus
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.PollResult
import com.financemanage.mobile.core.model.domain.Category
import com.financemanage.mobile.core.model.domain.CategoryType
import com.financemanage.mobile.core.model.domain.Wallet
import com.financemanage.mobile.core.ui.components.PendingAction
import com.financemanage.mobile.core.ui.state.Resource
import com.financemanage.mobile.core.ui.state.ResourceHolder
import com.financemanage.mobile.feature.investment.domain.model.AssetDto
import com.financemanage.mobile.feature.investment.domain.model.AssetRequest
import com.financemanage.mobile.feature.investment.domain.model.IncomeRequest
import com.financemanage.mobile.feature.investment.domain.model.PortfolioDto
import com.financemanage.mobile.feature.investment.domain.model.TradeKind
import com.financemanage.mobile.feature.investment.domain.model.TradeRequest
import com.financemanage.mobile.feature.investment.domain.usecase.CreateAssetUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.DeleteAssetUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.GetAssetUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.GetPortfolioUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.ObserveAssetsUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.RecordTradeUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.RefreshAssetsUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.UpdateAssetPriceUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.UpdateAssetUseCase
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    observeAssets: ObserveAssetsUseCase,
    refreshAssets: RefreshAssetsUseCase,
    private val getPortfolio: GetPortfolioUseCase,
    getDefaultCurrency: GetDefaultCurrencyUseCase,
) : ViewModel() {
    private val holder = ResourceHolder(viewModelScope, observeAssets()) { refreshAssets() }
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
            val currency = getDefaultCurrency()
            extra.update { it.copy(currency = currency) }
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
            when (val result = getPortfolio(currency)) {
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
    private val getAsset: GetAssetUseCase,
    private val updateAssetPrice: UpdateAssetPriceUseCase,
    private val recordTrade: RecordTradeUseCase,
    private val deleteAsset: DeleteAssetUseCase,
    getCachedWallets: GetCachedWalletsUseCase,
    getCachedCategories: GetCachedCategoriesUseCase,
    getToday: GetTodayUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AssetDetailUiState())
    val state: StateFlow<AssetDetailUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val walletList = getCachedWallets().filter { w -> !w.archived }
            val categoryList = getCachedCategories()
            val today = getToday()
            mutableState.update {
                it.copy(
                    wallets = walletList,
                    incomeCategories =
                        categoryList.filter { c ->
                            c.type ==
                                CategoryType.INCOME
                        },
                    today = today,
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            when (val result = getAsset(id)) {
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
            when (val result = updateAssetPrice(id, price, date)) {
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
            when (val result = recordTrade(id, kind, body)) {
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
            val result = deleteAsset(id)
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
    getAsset: GetAssetUseCase,
    private val createAsset: CreateAssetUseCase,
    private val updateAsset: UpdateAssetUseCase,
    getDefaultCurrency: GetDefaultCurrencyUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AssetFormUiState(isEdit = id != null))
    val state: StateFlow<AssetFormUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val currency = getDefaultCurrency()
            mutableState.update { it.copy(currency = currency) }
            if (id != null) {
                (getAsset(id) as? AppResult.Success)?.value?.let { asset ->
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
            val result = if (id == null) createAsset(request) else updateAsset(id, request)
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
