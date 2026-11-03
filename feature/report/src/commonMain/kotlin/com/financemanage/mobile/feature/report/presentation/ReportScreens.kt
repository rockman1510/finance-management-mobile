package com.financemanage.mobile.feature.report.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financemanage.mobile.core.model.CommonCurrencies
import com.financemanage.mobile.core.model.time.toDisplayString
import com.financemanage.mobile.core.model.toChartValue
import com.financemanage.mobile.core.ui.components.BarChart
import com.financemanage.mobile.core.ui.components.BarGroup
import com.financemanage.mobile.core.ui.components.ChartPalette
import com.financemanage.mobile.core.ui.components.ChartSlice
import com.financemanage.mobile.core.ui.components.DateField
import com.financemanage.mobile.core.ui.components.DonutChart
import com.financemanage.mobile.core.ui.components.FinanceScaffold
import com.financemanage.mobile.core.ui.components.InfoRow
import com.financemanage.mobile.core.ui.components.LegendRow
import com.financemanage.mobile.core.ui.components.ListSkeleton
import com.financemanage.mobile.core.ui.components.MoneyText
import com.financemanage.mobile.core.ui.components.OptionPicker
import com.financemanage.mobile.core.ui.components.RefreshOnResume
import com.financemanage.mobile.core.ui.components.ResourceContent
import com.financemanage.mobile.core.ui.components.SectionHeader
import com.financemanage.mobile.core.ui.components.SkeletonBlock
import com.financemanage.mobile.core.ui.components.currencyLabel
import com.financemanage.mobile.core.ui.error.userMessage
import com.financemanage.mobile.core.ui.theme.FinanceTheme
import com.financemanage.mobile.core.ui.theme.Spacing
import com.financemanage.mobile.feature.report.data.CategoryTotalDto
import com.financemanage.mobile.feature.report.data.DashboardDto
import com.financemanage.mobile.feature.report.data.NetWorthDto
import com.financemanage.mobile.feature.report.data.moneyOf
import com.financemanage.mobile.feature.report.resources.Res
import com.financemanage.mobile.feature.report.resources.active_deposits
import com.financemanage.mobile.feature.report.resources.active_loans
import com.financemanage.mobile.feature.report.resources.assets
import com.financemanage.mobile.feature.report.resources.by_category
import com.financemanage.mobile.feature.report.resources.cash_flow
import com.financemanage.mobile.feature.report.resources.cash_flow_12
import com.financemanage.mobile.feature.report.resources.custom
import com.financemanage.mobile.feature.report.resources.dashboard
import com.financemanage.mobile.feature.report.resources.error_period
import com.financemanage.mobile.feature.report.resources.error_period_too_long
import com.financemanage.mobile.feature.report.resources.expense
import com.financemanage.mobile.feature.report.resources.export_csv
import com.financemanage.mobile.feature.report.resources.exported
import com.financemanage.mobile.feature.report.resources.external
import com.financemanage.mobile.feature.report.resources.from_date
import com.financemanage.mobile.feature.report.resources.group_BORROWED
import com.financemanage.mobile.feature.report.resources.group_DEPOSIT
import com.financemanage.mobile.feature.report.resources.group_INVESTMENT
import com.financemanage.mobile.feature.report.resources.group_LENT
import com.financemanage.mobile.feature.report.resources.group_SAVING
import com.financemanage.mobile.feature.report.resources.group_WALLET
import com.financemanage.mobile.feature.report.resources.income
import com.financemanage.mobile.feature.report.resources.last_month
import com.financemanage.mobile.feature.report.resources.liabilities
import com.financemanage.mobile.feature.report.resources.net
import com.financemanage.mobile.feature.report.resources.net_worth
import com.financemanage.mobile.feature.report.resources.no_data
import com.financemanage.mobile.feature.report.resources.overdue_installments
import com.financemanage.mobile.feature.report.resources.overview
import com.financemanage.mobile.feature.report.resources.report_currency
import com.financemanage.mobile.feature.report.resources.reports
import com.financemanage.mobile.feature.report.resources.savings_rate
import com.financemanage.mobile.feature.report.resources.this_month
import com.financemanage.mobile.feature.report.resources.this_quarter
import com.financemanage.mobile.feature.report.resources.this_year
import com.financemanage.mobile.feature.report.resources.to_date
import com.financemanage.mobile.feature.report.resources.upcoming_30
import com.financemanage.mobile.feature.report.resources.vs_previous
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.github.vinceglb.filekit.compose.rememberFileSaverLauncher
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val ReportErrors: Map<String, StringResource> =
    mapOf("INVALID_PERIOD" to Res.string.error_period, "PERIOD_TOO_LONG" to Res.string.error_period_too_long)

private val PresetLabels =
    listOf(
        PeriodPreset.THIS_MONTH to Res.string.this_month,
        PeriodPreset.LAST_MONTH to Res.string.last_month,
        PeriodPreset.THIS_QUARTER to Res.string.this_quarter,
        PeriodPreset.THIS_YEAR to Res.string.this_year,
        PeriodPreset.CUSTOM to Res.string.custom,
    )

/** Reports (F19). Tapping a category opens its sub-categories, then its transactions. */
@Composable
fun ReportsScreen(
    onOpenCategoryTransactions: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ReportsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var expanded by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val saver =
        rememberFileSaverLauncher { file ->
            if (file !=
                null
            ) {
                scope.launch { snackbar.showSnackbar(getString(Res.string.exported)) }
            }
        }
    RefreshOnResume(viewModel::refresh)
    LaunchedEffect(state.csv) {
        state.csv?.let { bytes ->
            saver.launch(bytes, "transactions_${state.from}_${state.to}", "csv")
            viewModel.csvHandled()
        }
    }
    FinanceScaffold(
        title = stringResource(Res.string.reports),
        onBack = onBack,
        snackbarHostState = snackbar,
        actions = {
            IconButton(
                onClick = viewModel::exportCsv,
            ) { Icon(Icons.Outlined.Download, stringResource(Res.string.export_csv)) }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            item {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    PresetLabels.forEach { (preset, label) ->
                        FilterChip(
                            state.preset == preset,
                            { viewModel.setPreset(preset) },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.shiftMonth(-1) },
                    ) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null) }
                    Text(
                        "${state.from?.toDisplayString().orEmpty()} – ${state.to?.toDisplayString().orEmpty()}",
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { viewModel.shiftMonth(1) },
                    ) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) }
                }
                if (state.preset == PeriodPreset.CUSTOM) {
                    Column(Modifier.padding(horizontal = Spacing.md)) {
                        DateField(stringResource(Res.string.from_date), state.from, { viewModel.setRange(it, null) })
                        DateField(stringResource(Res.string.to_date), state.to, { viewModel.setRange(null, it) })
                    }
                }
                Column(Modifier.padding(horizontal = Spacing.md)) {
                    OptionPicker(
                        stringResource(
                            Res.string.report_currency,
                        ),
                        (CommonCurrencies + state.currency).distinct(),
                        state.currency,
                        {
                            currencyLabel(it)
                        },
                        viewModel::setCurrency,
                    )
                }
            }
            state.error?.let { error ->
                item {
                    Text(
                        error.userMessage(ReportErrors),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(Spacing.md),
                    )
                }
            }
            if (state.loading && state.summary == null) item { ListSkeleton() }
            state.summary?.let { summary ->
                item {
                    SectionHeader(stringResource(Res.string.overview))
                    Card(Modifier.fillMaxWidth().padding(horizontal = Spacing.md)) {
                        Column(Modifier.padding(Spacing.md)) {
                            InfoRow(
                                stringResource(Res.string.income),
                            ) { MoneyText(moneyOf(summary.income, summary.currency), colorBySign = true) }
                            summary.incomeChange?.let {
                                Text(
                                    stringResource(Res.string.vs_previous, arrow(it.toStringExpanded())),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                            InfoRow(stringResource(Res.string.expense)) {
                                MoneyText(-moneyOf(summary.expense, summary.currency), colorBySign = true)
                            }
                            summary.expenseChange?.let {
                                Text(
                                    stringResource(Res.string.vs_previous, arrow(it.toStringExpanded())),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                            InfoRow(stringResource(Res.string.net)) {
                                MoneyText(
                                    moneyOf(summary.net, summary.currency),
                                    colorBySign = true,
                                    showPlusSign = true,
                                )
                            }
                            summary.savingsRate?.let {
                                InfoRow(
                                    stringResource(Res.string.savings_rate),
                                    "${it.toStringExpanded()}%",
                                )
                            }
                        }
                    }
                }
            }
            item {
                SectionHeader(stringResource(Res.string.by_category))
                PrimaryTabRow(selectedTabIndex = if (state.categoryType == "EXPENSE") 0 else 1) {
                    Tab(state.categoryType == "EXPENSE", {
                        viewModel.setCategoryType("EXPENSE")
                    }, text = { Text(stringResource(Res.string.expense)) })
                    Tab(state.categoryType == "INCOME", {
                        viewModel.setCategoryType("INCOME")
                    }, text = { Text(stringResource(Res.string.income)) })
                }
            }
            state.categories?.let { report ->
                if (report.categories.isEmpty()) {
                    item { Text(stringResource(Res.string.no_data), modifier = Modifier.padding(Spacing.md)) }
                } else {
                    item {
                        val slices =
                            report.categories.mapIndexed { index, c ->
                                ChartSlice(
                                    c.name,
                                    (c.amount ?: BigDecimal.ZERO).toChartValue(),
                                    ChartPalette[
                                        index %
                                            ChartPalette.size,
                                    ],
                                )
                            }
                        Row(
                            Modifier.fillMaxWidth().padding(Spacing.md),
                            horizontalArrangement = Arrangement.Center,
                        ) { DonutChart(slices) }
                    }
                    report.categories.forEachIndexed { index, category ->
                        item(key = "cat-${category.categoryId}-$index") {
                            CategoryRow(category, report.currency, ChartPalette[index % ChartPalette.size]) {
                                if (category.children.isNotEmpty() && expanded != category.categoryId) {
                                    expanded = category.categoryId
                                } else {
                                    category.categoryId?.let(onOpenCategoryTransactions)
                                }
                            }
                            if (expanded == category.categoryId) {
                                category.children.forEach { child ->
                                    ListItem(
                                        headlineContent = { Text(child.name) },
                                        trailingContent = { MoneyText(moneyOf(child.amount, report.currency)) },
                                        supportingContent = { Text("${child.percent?.toStringExpanded().orEmpty()}%") },
                                        modifier =
                                            Modifier
                                                .padding(
                                                    start = Spacing.xl,
                                                ).clickable { child.categoryId?.let(onOpenCategoryTransactions) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            state.cashFlow?.let { flow ->
                item {
                    SectionHeader(stringResource(Res.string.cash_flow))
                    val groups =
                        flow.points.map { p ->
                            BarGroup(
                                p.period.takeLast(2),
                                listOf(
                                    (p.income ?: BigDecimal.ZERO).toChartValue(),
                                    (p.expense ?: BigDecimal.ZERO).toChartValue(),
                                    (p.externalOut ?: BigDecimal.ZERO).toChartValue(),
                                ),
                            )
                        }
                    val colors =
                        listOf(FinanceTheme.colors.income, FinanceTheme.colors.expense, FinanceTheme.colors.neutral)
                    Column(Modifier.padding(Spacing.md)) {
                        BarChart(groups, colors)
                        LegendRow(colors[0], stringResource(Res.string.income)) {}
                        LegendRow(colors[1], stringResource(Res.string.expense)) {}
                        LegendRow(colors[2], stringResource(Res.string.external)) {}
                    }
                }
            }
        }
    }
}

private fun arrow(value: String) = if (value.startsWith("-")) "▼ ${value.removePrefix("-")}%" else "▲ $value%"

@Composable
private fun CategoryRow(
    category: CategoryTotalDto,
    currency: String?,
    color: Color,
    onClick: () -> Unit,
) {
    LegendRow(
        color = color,
        label = category.name,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = Spacing.md),
    ) {
        Column(horizontalAlignment = Alignment.End) {
            MoneyText(moneyOf(category.amount, currency))
            Text("${category.percent?.toStringExpanded().orEmpty()}%", style = MaterialTheme.typography.labelSmall)
        }
    }
}

private val GroupLabels =
    mapOf(
        "WALLET" to Res.string.group_WALLET,
        "SAVING" to Res.string.group_SAVING,
        "INVESTMENT" to Res.string.group_INVESTMENT,
        "DEPOSIT" to Res.string.group_DEPOSIT,
        "LENT" to Res.string.group_LENT,
        "BORROWED" to Res.string.group_BORROWED,
    )

/** Net worth summary used at the top of the Assets tab (F19). */
@Composable
fun NetWorthCard(
    onOpen: () -> Unit,
    viewModel: NetWorthViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RefreshOnResume(viewModel::refresh)
    Card(Modifier.fillMaxWidth().padding(Spacing.md).clickable(onClick = onOpen)) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(Res.string.net_worth), style = MaterialTheme.typography.labelLarge)
            val worth = state.netWorth.data
            if (worth == null) {
                SkeletonBlock(Modifier.fillMaxWidth(0.5f))
            } else {
                MoneyText(
                    moneyOf(worth.netWorth, worth.currency),
                    style = MaterialTheme.typography.headlineMedium,
                    colorBySign = true,
                )
                InfoRow(stringResource(Res.string.assets)) { MoneyText(moneyOf(worth.assets, worth.currency)) }
                InfoRow(
                    stringResource(Res.string.liabilities),
                ) { MoneyText(moneyOf(worth.liabilities, worth.currency)) }
            }
            state.dashboard?.let { DashboardSummary(it) }
        }
    }
}

@Composable
private fun DashboardSummary(dashboard: DashboardDto) {
    SectionHeader(stringResource(Res.string.dashboard))
    Text(
        stringResource(Res.string.active_deposits, dashboard.activeDeposits),
        style = MaterialTheme.typography.bodySmall,
    )
    Text(stringResource(Res.string.active_loans, dashboard.activeLoans), style = MaterialTheme.typography.bodySmall)
    if (dashboard.overdueInstallments > 0) {
        Text(
            stringResource(Res.string.overdue_installments, dashboard.overdueInstallments),
            color = FinanceTheme.colors.expense,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (dashboard.upcoming.isNotEmpty()) {
        Text(stringResource(Res.string.upcoming_30), style = MaterialTheme.typography.labelLarge)
        dashboard.upcoming.take(UPCOMING_SHOWN).forEach { item ->
            InfoRow("${item.date?.toDisplayString().orEmpty()} · ${item.title}") {
                val money = moneyOf(item.amount, item.currency)
                MoneyText(if (item.direction == "OUT") -money else money, colorBySign = true, showPlusSign = true)
            }
        }
    }
    if (dashboard.monthlyCashFlow.isNotEmpty()) {
        Text(stringResource(Res.string.cash_flow_12), style = MaterialTheme.typography.labelLarge)
        BarGroups(dashboard)
    }
}

@Composable
private fun BarGroups(dashboard: DashboardDto) {
    val groups =
        dashboard.monthlyCashFlow.map {
            BarGroup(it.month.takeLast(2), listOf((it.net ?: BigDecimal.ZERO).toChartValue()))
        }
    BarChart(groups, listOf(MaterialTheme.colorScheme.primary))
}

private const val UPCOMING_SHOWN = 5

/** Net worth details: assets − liabilities by group with every item (original + converted). */
@Composable
fun NetWorthScreen(
    onBack: () -> Unit,
    viewModel: NetWorthViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RefreshOnResume(viewModel::refresh)
    FinanceScaffold(title = stringResource(Res.string.net_worth), onBack = onBack) { padding ->
        ResourceContent(
            resource = state.netWorth,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding),
        ) { worth: NetWorthDto ->
            LazyColumn {
                item {
                    Column(Modifier.padding(Spacing.md)) {
                        MoneyText(
                            moneyOf(worth.netWorth, worth.currency),
                            style = MaterialTheme.typography.displaySmall,
                            colorBySign = true,
                        )
                        InfoRow(stringResource(Res.string.assets)) { MoneyText(moneyOf(worth.assets, worth.currency)) }
                        InfoRow(
                            stringResource(Res.string.liabilities),
                        ) { MoneyText(moneyOf(worth.liabilities, worth.currency)) }
                    }
                }
                worth.groups.forEach { group ->
                    item(key = group.kind) {
                        SectionHeader(
                            GroupLabels[group.kind]?.let {
                                stringResource(it)
                            } ?: group.kind,
                            action = { MoneyText(moneyOf(group.total, worth.currency)) },
                        )
                        group.items.forEach { item ->
                            ListItem(
                                headlineContent = { Text(item.name) },
                                supportingContent = {
                                    if (item.currency !=
                                        worth.currency
                                    ) {
                                        MoneyText(moneyOf(item.amount, item.currency))
                                    }
                                },
                                trailingContent = {
                                    MoneyText(
                                        moneyOf(item.converted, worth.currency),
                                        colorBySign = true,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
