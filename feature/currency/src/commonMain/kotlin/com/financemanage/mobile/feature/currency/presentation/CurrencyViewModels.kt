package com.financemanage.mobile.feature.currency.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.domain.model.ConversionDto
import com.financemanage.mobile.core.domain.usecase.ConvertCurrencyUseCase
import com.financemanage.mobile.core.domain.usecase.DeleteExchangeRateUseCase
import com.financemanage.mobile.core.domain.usecase.GetTodayUseCase
import com.financemanage.mobile.core.domain.usecase.ObserveExchangeRatesUseCase
import com.financemanage.mobile.core.domain.usecase.RefreshExchangeRatesUseCase
import com.financemanage.mobile.core.domain.usecase.SaveExchangeRateUseCase
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.domain.ExchangeRate
import com.financemanage.mobile.core.model.toBigDecimalOrNull
import com.financemanage.mobile.core.ui.state.Resource
import com.financemanage.mobile.core.ui.state.ResourceHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class RatesUiState(
    val rates: Resource<List<ExchangeRate>> = Resource(loading = true),
    val actionError: AppError? = null,
)

class RatesViewModel(
    observeRates: ObserveExchangeRatesUseCase,
    refreshRates: RefreshExchangeRatesUseCase,
    private val deleteRate: DeleteExchangeRateUseCase,
) : ViewModel() {
    private val holder = ResourceHolder(viewModelScope, observeRates()) { refreshRates() }
    private val actionError = MutableStateFlow<AppError?>(null)

    val state: StateFlow<RatesUiState> =
        combine(holder.state, actionError) { rates, error -> RatesUiState(rates, error) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), RatesUiState())

    fun refresh() = holder.refresh()

    fun delete(rate: ExchangeRate) {
        viewModelScope.launch { actionError.value = (deleteRate(rate.id) as? AppResult.Failure)?.error }
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}

enum class RateProblem { SAME_CURRENCY, INVALID_RATE }

data class AddRateUiState(
    val base: CurrencyCode = CurrencyCode.USD,
    val quote: CurrencyCode = CurrencyCode.VND,
    val rate: String = "",
    val date: LocalDate? = null,
    val problem: RateProblem? = null,
    val saving: Boolean = false,
    val done: Boolean = false,
    val error: AppError? = null,
)

/** Add a rate; can be opened pre-filled from a "missing exchange rate" message (F11). */
class AddRateViewModel(
    base: String?,
    quote: String?,
    private val saveRate: SaveExchangeRateUseCase,
    getToday: GetTodayUseCase,
) : ViewModel() {
    private val mutableState =
        MutableStateFlow(
            AddRateUiState(
                base = base?.let { CurrencyCode.parse(it) } ?: CurrencyCode.USD,
                quote = quote?.let { CurrencyCode.parse(it) } ?: CurrencyCode.VND,
            ),
        )
    val state: StateFlow<AddRateUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val date = getToday()
            mutableState.update { it.copy(date = date) }
        }
    }

    fun onBase(value: CurrencyCode) = mutableState.update { it.copy(base = value, problem = null) }

    fun onQuote(value: CurrencyCode) = mutableState.update { it.copy(quote = value, problem = null) }

    fun onRate(value: String) = mutableState.update { it.copy(rate = value, problem = null, error = null) }

    fun onDate(value: LocalDate) = mutableState.update { it.copy(date = value) }

    fun save() {
        val form = mutableState.value
        val rate = form.rate.replace(",", ".").toBigDecimalOrNull()
        val problem =
            when {
                form.base == form.quote -> RateProblem.SAME_CURRENCY
                rate == null || rate.signum() <= 0 -> RateProblem.INVALID_RATE
                else -> null
            }
        if (problem != null || rate == null) {
            mutableState.update { it.copy(problem = problem) }
            return
        }
        mutableState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result = saveRate(form.base, form.quote, rate, form.date)
            mutableState.update {
                it.copy(
                    saving = false,
                    done = result is AppResult.Success,
                    error = (result as? AppResult.Failure)?.error,
                )
            }
        }
    }
}

data class ConverterUiState(
    val amount: String = "",
    val from: CurrencyCode = CurrencyCode.USD,
    val to: CurrencyCode = CurrencyCode.VND,
    val date: LocalDate? = null,
    val result: ConversionDto? = null,
    val loading: Boolean = false,
    val error: AppError? = null,
)

class ConverterViewModel(
    private val convertCurrency: ConvertCurrencyUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ConverterUiState())
    val state: StateFlow<ConverterUiState> = mutableState.asStateFlow()

    fun onAmount(value: String) = mutableState.update { it.copy(amount = value, result = null) }

    fun onFrom(value: CurrencyCode) = mutableState.update { it.copy(from = value, result = null) }

    fun onTo(value: CurrencyCode) = mutableState.update { it.copy(to = value, result = null) }

    fun onDate(value: LocalDate) = mutableState.update { it.copy(date = value, result = null) }

    fun convert() {
        val form = mutableState.value
        val amount = form.amount.replace(",", ".").toBigDecimalOrNull() ?: return
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = convertCurrency(amount, form.from, form.to, form.date)) {
                is AppResult.Success -> mutableState.update { it.copy(loading = false, result = result.value) }
                is AppResult.Failure -> mutableState.update { it.copy(loading = false, error = result.error) }
            }
        }
    }
}
