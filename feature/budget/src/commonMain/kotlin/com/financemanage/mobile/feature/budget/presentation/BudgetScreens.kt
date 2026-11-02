package com.financemanage.mobile.feature.budget.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financemanage.mobile.core.model.CommonCurrencies
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.domain.Wallet
import com.financemanage.mobile.core.model.format.MoneyFormatter
import com.financemanage.mobile.core.model.time.toDisplayString
import com.financemanage.mobile.core.model.toChartValue
import com.financemanage.mobile.core.ui.components.AmountTextField
import com.financemanage.mobile.core.ui.components.BarChart
import com.financemanage.mobile.core.ui.components.BarGroup
import com.financemanage.mobile.core.ui.components.CategoryIcon
import com.financemanage.mobile.core.ui.components.ConfirmDialog
import com.financemanage.mobile.core.ui.components.DateField
import com.financemanage.mobile.core.ui.components.EmptyState
import com.financemanage.mobile.core.ui.components.FinanceProgressBar
import com.financemanage.mobile.core.ui.components.FinanceScaffold
import com.financemanage.mobile.core.ui.components.FormColumn
import com.financemanage.mobile.core.ui.components.FormTextField
import com.financemanage.mobile.core.ui.components.ListSkeleton
import com.financemanage.mobile.core.ui.components.LoadingButton
import com.financemanage.mobile.core.ui.components.MoneyText
import com.financemanage.mobile.core.ui.components.OptionPicker
import com.financemanage.mobile.core.ui.components.RefreshOnResume
import com.financemanage.mobile.core.ui.components.ResourceContent
import com.financemanage.mobile.core.ui.components.SectionHeader
import com.financemanage.mobile.core.ui.components.SwitchRow
import com.financemanage.mobile.core.ui.components.currencyLabel
import com.financemanage.mobile.core.ui.components.walletLabel
import com.financemanage.mobile.core.ui.error.userMessage
import com.financemanage.mobile.core.ui.resources.action_save
import com.financemanage.mobile.core.ui.resources.all_wallets
import com.financemanage.mobile.core.ui.resources.delete_confirm_message
import com.financemanage.mobile.core.ui.resources.field_amount
import com.financemanage.mobile.core.ui.resources.field_currency
import com.financemanage.mobile.core.ui.resources.field_invalid_amount
import com.financemanage.mobile.core.ui.resources.field_name
import com.financemanage.mobile.core.ui.resources.field_required
import com.financemanage.mobile.core.ui.resources.field_wallet
import com.financemanage.mobile.core.ui.theme.FinanceTheme
import com.financemanage.mobile.core.ui.theme.LocalHideBalances
import com.financemanage.mobile.core.ui.theme.Spacing
import com.financemanage.mobile.feature.budget.data.Budget
import com.financemanage.mobile.feature.budget.data.BudgetPeriod
import com.financemanage.mobile.feature.budget.data.BudgetState
import com.financemanage.mobile.feature.budget.resources.Res
import com.financemanage.mobile.feature.budget.resources.add_budget
import com.financemanage.mobile.feature.budget.resources.budget_transactions
import com.financemanage.mobile.feature.budget.resources.categories
import com.financemanage.mobile.feature.budget.resources.daily_spending
import com.financemanage.mobile.feature.budget.resources.delete_budget
import com.financemanage.mobile.feature.budget.resources.edit_budget
import com.financemanage.mobile.feature.budget.resources.end_date
import com.financemanage.mobile.feature.budget.resources.error_end_date
import com.financemanage.mobile.feature.budget.resources.error_period_required
import com.financemanage.mobile.feature.budget.resources.next_period
import com.financemanage.mobile.feature.budget.resources.no_budgets
import com.financemanage.mobile.feature.budget.resources.no_budgets_message
import com.financemanage.mobile.feature.budget.resources.per_day_left
import com.financemanage.mobile.feature.budget.resources.period
import com.financemanage.mobile.feature.budget.resources.period_CUSTOM
import com.financemanage.mobile.feature.budget.resources.period_MONTHLY
import com.financemanage.mobile.feature.budget.resources.period_QUARTERLY
import com.financemanage.mobile.feature.budget.resources.period_WEEKLY
import com.financemanage.mobile.feature.budget.resources.period_YEARLY
import com.financemanage.mobile.feature.budget.resources.previous_period
import com.financemanage.mobile.feature.budget.resources.repeat
import com.financemanage.mobile.feature.budget.resources.spent_of
import com.financemanage.mobile.feature.budget.resources.start_date
import com.financemanage.mobile.feature.budget.resources.state_ENDED
import com.financemanage.mobile.feature.budget.resources.state_EXCEEDED
import com.financemanage.mobile.feature.budget.resources.state_NOT_STARTED
import com.financemanage.mobile.feature.budget.resources.state_ON_TRACK
import com.financemanage.mobile.feature.budget.resources.state_WARNING
import com.financemanage.mobile.feature.budget.resources.unconverted
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.financemanage.mobile.core.ui.resources.Res as CoreRes

/** Bar colour by state: ON_TRACK green, WARNING yellow (≥ 80%), EXCEEDED red, others grey (F13). */
@Composable
fun budgetColor(state: BudgetState?): Color =
    when (state) {
        BudgetState.ON_TRACK -> FinanceTheme.colors.income
        BudgetState.WARNING -> FinanceTheme.colors.warning
        BudgetState.EXCEEDED -> FinanceTheme.colors.expense
        else -> FinanceTheme.colors.neutral
    }

@Composable
private fun stateLabel(state: BudgetState): String =
    stringResource(
        when (state) {
            BudgetState.NOT_STARTED -> Res.string.state_NOT_STARTED
            BudgetState.WARNING -> Res.string.state_WARNING
            BudgetState.EXCEEDED -> Res.string.state_EXCEEDED
            BudgetState.ENDED -> Res.string.state_ENDED
            BudgetState.ON_TRACK, BudgetState.UNKNOWN -> Res.string.state_ON_TRACK
        },
    )

@Composable
private fun periodLabel(period: BudgetPeriod): String =
    stringResource(
        when (period) {
            BudgetPeriod.WEEKLY -> Res.string.period_WEEKLY
            BudgetPeriod.QUARTERLY -> Res.string.period_QUARTERLY
            BudgetPeriod.YEARLY -> Res.string.period_YEARLY
            BudgetPeriod.CUSTOM -> Res.string.period_CUSTOM
            BudgetPeriod.MONTHLY, BudgetPeriod.UNKNOWN -> Res.string.period_MONTHLY
        },
    )

private fun shortRange(budget: Budget): String {
    val status = budget.current ?: return ""

    fun short(date: kotlinx.datetime.LocalDate?) =
        date
            ?.let {
                "${it.dayOfMonth.toString().padStart(
                    2,
                    '0',
                )}/${it.monthNumber.toString().padStart(2, '0')}"
            }.orEmpty()
    return "${short(status.periodStart)}–${short(status.periodEnd)}"
}

/** Budgets tab of the Plan screen (F13). */
@Composable
fun BudgetsTab(
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    viewModel: BudgetListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RefreshOnResume(viewModel::refresh)
    Box(Modifier.fillMaxSize()) {
        ResourceContent(
            resource = state,
            onRefresh = viewModel::refresh,
            isEmpty = { it.isEmpty() },
            empty = {
                EmptyState(
                    title = stringResource(Res.string.no_budgets),
                    message = stringResource(Res.string.no_budgets_message),
                    actionLabel = stringResource(Res.string.add_budget),
                    onAction = onAdd,
                )
            },
        ) { budgets ->
            LazyColumn(
                contentPadding = PaddingValues(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(budgets, key = { it.id }) { budget -> BudgetCard(budget) { onOpen(budget.id) } }
            }
        }
        FloatingActionButton(onClick = onAdd, modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.md)) {
            Icon(Icons.Outlined.Add, contentDescription = stringResource(Res.string.add_budget))
        }
    }
}

@Composable
fun BudgetCard(
    budget: Budget,
    onClick: () -> Unit,
) {
    val status = budget.current
    val hidden = LocalHideBalances.current
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(budget.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    shortRange(budget),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (status != null) {
                Text(
                    stringResource(
                        Res.string.spent_of,
                        MoneyFormatter.format(status.spent, hidden),
                        MoneyFormatter.format(budget.amount, hidden),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                FinanceProgressBar(status.percent, budgetColor(status.state))
                Row {
                    Text(
                        stateLabel(status.state),
                        style = MaterialTheme.typography.labelSmall,
                        color = budgetColor(status.state),
                        modifier = Modifier.weight(1f),
                    )
                    status.dailySuggestion?.let { daily ->
                        if (status.daysLeft > 0) {
                            Text(
                                stringResource(
                                    Res.string.per_day_left,
                                    MoneyFormatter.format(daily, hidden),
                                    status.daysLeft.toInt(),
                                ),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                if (status.unconvertedCurrencies.isNotEmpty()) {
                    Text(
                        stringResource(Res.string.unconverted, status.unconvertedCurrencies.joinToString()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetDetailScreen(
    id: String,
    onEdit: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: BudgetDetailViewModel = koinViewModel(key = "budget-$id") { parametersOf(id) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    RefreshOnResume(viewModel::refresh)
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    val budget = state.budget
    FinanceScaffold(
        title = budget?.name.orEmpty(),
        onBack = onBack,
        actions = {
            IconButton(
                onClick = onEdit,
            ) { Icon(Icons.Outlined.Edit, contentDescription = stringResource(Res.string.edit_budget)) }
            IconButton(onClick = {
                confirmDelete = true
            }) { Icon(Icons.Outlined.Delete, contentDescription = stringResource(Res.string.delete_budget)) }
        },
    ) { padding ->
        if (budget == null) {
            ListSkeleton(Modifier.padding(padding))
            return@FinanceScaffold
        }
        LazyColumn(Modifier.padding(padding)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = viewModel::previousPeriod) {
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                            contentDescription = stringResource(Res.string.previous_period),
                        )
                    }
                    Text(
                        shortRange(budget),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    IconButton(onClick = viewModel::nextPeriod) {
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = stringResource(Res.string.next_period),
                        )
                    }
                }
            }
            item { BudgetCard(budget) {} }
            item {
                SectionHeader(stringResource(Res.string.daily_spending))
                val byDay =
                    state.transactions
                        .mapNotNull { tx -> tx.occurredOn?.let { it.dayOfMonth to (tx.amount?.toChartValue() ?: 0f) } }
                        .groupBy({ it.first }, { it.second })
                        .entries
                        .sortedBy { it.key }
                        .map { (day, values) -> BarGroup(day.toString(), listOf(values.sum())) }
                BarChart(byDay, listOf(budgetColor(budget.current?.state)), Modifier.padding(horizontal = Spacing.md))
            }
            item { SectionHeader(stringResource(Res.string.budget_transactions)) }
            items(state.transactions, key = { it.id }) { tx ->
                val category = state.categories.firstOrNull { it.id == tx.categoryId }
                ListItem(
                    headlineContent = { Text(category?.name.orEmpty()) },
                    supportingContent = {
                        Text(
                            listOfNotNull(tx.occurredOn?.toDisplayString(), tx.note).joinToString(" · "),
                        )
                    },
                    leadingContent = { CategoryIcon(category?.code, category?.icon) },
                    trailingContent = {
                        MoneyText(
                            Money(
                                tx.amount ?: com.ionspin.kotlin.bignum.decimal.BigDecimal.ZERO,
                                budget.amount.currency,
                            ),
                        )
                    },
                    modifier = Modifier.clickable { onOpenTransaction(tx.id) },
                )
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(Res.string.delete_budget),
            message = stringResource(CoreRes.string.delete_confirm_message),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetFormScreen(
    id: String?,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: BudgetFormViewModel = koinViewModel(key = "budget-form-$id") { parametersOf(id) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    FinanceScaffold(
        title = stringResource(if (state.isEdit) Res.string.edit_budget else Res.string.add_budget),
        onBack = onBack,
    ) { padding ->
        FormColumn(padding) {
            FormTextField(
                state.name,
                viewModel::onName,
                stringResource(CoreRes.string.field_name),
                maxLength = 60,
                error =
                    if (BudgetProblem.NAME in
                        state.problems
                    ) {
                        stringResource(CoreRes.string.field_required)
                    } else {
                        null
                    },
            )
            AmountTextField(
                state.amount,
                viewModel::onAmount,
                state.currency,
                stringResource(CoreRes.string.field_amount),
                error =
                    if (BudgetProblem.AMOUNT in
                        state.problems
                    ) {
                        stringResource(CoreRes.string.field_invalid_amount)
                    } else {
                        null
                    },
            )
            OptionPicker(
                stringResource(CoreRes.string.field_currency),
                (CommonCurrencies + state.currency).distinct(),
                state.currency,
                { currencyLabel(it) },
                viewModel::onCurrency,
                enabled = !state.isEdit,
            )
            OptionPicker(
                stringResource(Res.string.period),
                BudgetPeriod.entries.filter { it != BudgetPeriod.UNKNOWN },
                state.period,
                { periodLabel(it) },
                viewModel::onPeriod,
                enabled = !state.isEdit,
            )
            if (!state.isEdit) {
                DateField(stringResource(Res.string.start_date), state.startDate, viewModel::onStart)
                if (state.period == BudgetPeriod.CUSTOM) {
                    DateField(
                        stringResource(Res.string.end_date),
                        state.endDate,
                        viewModel::onEnd,
                        error =
                            when {
                                BudgetProblem.PERIOD_DATES in state.problems ->
                                    stringResource(
                                        Res.string.error_period_required,
                                    )
                                BudgetProblem.END_BEFORE_START in state.problems ->
                                    stringResource(
                                        Res.string.error_end_date,
                                    )
                                else -> null
                            },
                    )
                }
            }
            SwitchRow(stringResource(Res.string.repeat), state.repeat, viewModel::onRepeat)
            val walletOptions: List<Wallet?> = listOf(null) + state.wallets
            OptionPicker(
                stringResource(CoreRes.string.field_wallet),
                walletOptions,
                walletOptions.firstOrNull { it?.id == state.walletId },
                { it?.let(::walletLabel) ?: stringResource(CoreRes.string.all_wallets) },
                viewModel::onWallet,
            )
            Text(stringResource(Res.string.categories), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                state.categories.filter { it.parentId == null }.forEach { category ->
                    FilterChip(
                        selected = category.id in state.categoryIds,
                        onClick = { viewModel.toggleCategory(category.id) },
                        label = { Text(category.name) },
                        leadingIcon = { CategoryIcon(category.code, category.icon, size = 20.dp) },
                    )
                }
            }
            state.error?.let { Text(it.userMessage(), color = MaterialTheme.colorScheme.error) }
            LoadingButton(stringResource(CoreRes.string.action_save), onClick = viewModel::save, loading = state.saving)
        }
    }
}
