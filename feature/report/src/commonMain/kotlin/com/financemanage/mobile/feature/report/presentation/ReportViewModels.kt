package com.financemanage.mobile.feature.report.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.data.cache.Resource
import com.financemanage.mobile.core.data.cache.ResourceHolder
import com.financemanage.mobile.core.data.shared.ProfileRepository
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.time.today
import com.financemanage.mobile.feature.report.data.CashFlowDto
import com.financemanage.mobile.feature.report.data.CategoryReportDto
import com.financemanage.mobile.feature.report.data.DashboardDto
import com.financemanage.mobile.feature.report.data.NetWorthDto
import com.financemanage.mobile.feature.report.data.ReportRepository
import com.financemanage.mobile.feature.report.data.SummaryDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

enum class PeriodPreset { THIS_MONTH, LAST_MONTH, THIS_QUARTER, THIS_YEAR, CUSTOM }

private const val MONTHS_PER_QUARTER = 3
private const val MAX_DAILY_DAYS = 31

fun presetRange(
    preset: PeriodPreset,
    today: LocalDate,
): Pair<LocalDate, LocalDate> {
    val monthStart = LocalDate(today.year, today.monthNumber, 1)
    return when (preset) {
        PeriodPreset.THIS_MONTH, PeriodPreset.CUSTOM ->
            monthStart to
                monthStart.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
        PeriodPreset.LAST_MONTH -> monthStart.minus(DatePeriod(months = 1)) to monthStart.minus(DatePeriod(days = 1))
        PeriodPreset.THIS_QUARTER -> {
            val firstMonth = (today.monthNumber - 1) / MONTHS_PER_QUARTER * MONTHS_PER_QUARTER + 1
            val start = LocalDate(today.year, firstMonth, 1)
            start to start.plus(DatePeriod(months = MONTHS_PER_QUARTER)).minus(DatePeriod(days = 1))
        }
        PeriodPreset.THIS_YEAR -> {
            val start = LocalDate(today.year, 1, 1)
            start to start.plus(DatePeriod(years = 1)).minus(DatePeriod(days = 1))
        }
    }
}

data class ReportsUiState(
    val preset: PeriodPreset = PeriodPreset.THIS_MONTH,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val currency: CurrencyCode = CurrencyCode.DEFAULT,
    val categoryType: String = "EXPENSE",
    val summary: SummaryDto? = null,
    val categories: CategoryReportDto? = null,
    val cashFlow: CashFlowDto? = null,
    val loading: Boolean = true,
    val error: AppError? = null,
    val csv: ByteArray? = null,
)

/** Reports (F19). Reports update a few hundred ms after a write: they are reloaded when the screen is shown. */
class ReportsViewModel(
    private val repository: ReportRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ReportsUiState())
    val state: StateFlow<ReportsUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = profiles.current()
            val (from, to) = presetRange(PeriodPreset.THIS_MONTH, today(profile?.timeZone))
            mutableState.update {
                it.copy(
                    currency = profile?.defaultCurrency ?: CurrencyCode.DEFAULT,
                    from = from,
                    to = to,
                )
            }
            refresh()
        }
    }

    fun setPreset(preset: PeriodPreset) {
        viewModelScope.launch {
            val (from, to) = presetRange(preset, today(profiles.current()?.timeZone))
            mutableState.update { it.copy(preset = preset, from = from, to = to) }
            refresh()
        }
    }

    fun shiftMonth(months: Int) {
        val from = mutableState.value.from ?: return
        val start = LocalDate(from.year, from.monthNumber, 1).plus(DatePeriod(months = months))
        mutableState.update {
            it.copy(
                preset = PeriodPreset.CUSTOM,
                from = start,
                to = start.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1)),
            )
        }
        refresh()
    }

    fun setRange(
        from: LocalDate?,
        to: LocalDate?,
    ) {
        mutableState.update { it.copy(preset = PeriodPreset.CUSTOM, from = from ?: it.from, to = to ?: it.to) }
        refresh()
    }

    fun setCurrency(currency: CurrencyCode) {
        mutableState.update { it.copy(currency = currency) }
        refresh()
    }

    fun setCategoryType(type: String) {
        mutableState.update { it.copy(categoryType = type) }
        refresh()
    }

    fun refresh() {
        val current = mutableState.value
        val from = current.from ?: return
        val to = current.to ?: return
        val granularity = if (from.daysUntil(to) < MAX_DAILY_DAYS) "DAY" else "MONTH"
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val summary = repository.summary(from, to, current.currency)
            val categories = repository.categories(from, to, current.currency, current.categoryType)
            val cashFlow = repository.cashFlow(from, to, current.currency, granularity)
            mutableState.update {
                it.copy(
                    loading = false,
                    summary = (summary as? AppResult.Success)?.value,
                    categories = (categories as? AppResult.Success)?.value,
                    cashFlow = (cashFlow as? AppResult.Success)?.value,
                    error =
                        listOf(
                            summary,
                            categories,
                            cashFlow,
                        ).filterIsInstance<AppResult.Failure>().firstOrNull()?.error,
                )
            }
        }
    }

    fun exportCsv() {
        val current = mutableState.value
        val from = current.from ?: return
        val to = current.to ?: return
        viewModelScope.launch {
            when (val result = repository.exportCsv(from, to)) {
                is AppResult.Success -> mutableState.update { it.copy(csv = result.value) }
                is AppResult.Failure -> mutableState.update { it.copy(error = result.error) }
            }
        }
    }

    fun csvHandled() = mutableState.update { it.copy(csv = null) }
}

data class NetWorthUiState(
    val netWorth: Resource<NetWorthDto> = Resource(loading = true),
    val dashboard: DashboardDto? = null,
)

class NetWorthViewModel(
    private val repository: ReportRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {
    private val holder =
        ResourceHolder(viewModelScope, repository.netWorth) {
            repository.refreshNetWorth(profiles.current()?.defaultCurrency ?: CurrencyCode.DEFAULT)
        }
    private val dashboard = MutableStateFlow<DashboardDto?>(null)

    val state: StateFlow<NetWorthUiState> =
        combine(holder.state, dashboard) { worth, dash -> NetWorthUiState(worth, dash) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), NetWorthUiState())

    fun refresh() {
        holder.refresh()
        viewModelScope.launch { (repository.dashboard() as? AppResult.Success)?.let { dashboard.value = it.value } }
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}
