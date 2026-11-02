package com.financemanage.mobile.feature.home.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financemanage.mobile.core.data.cache.Resource
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.domain.TransactionType
import com.financemanage.mobile.core.model.format.MoneyFormatter
import com.financemanage.mobile.core.model.time.toDisplayString
import com.financemanage.mobile.core.ui.components.CategoryIcon
import com.financemanage.mobile.core.ui.components.FinanceProgressBar
import com.financemanage.mobile.core.ui.components.MoneyText
import com.financemanage.mobile.core.ui.components.PendingBanner
import com.financemanage.mobile.core.ui.components.ProgressRing
import com.financemanage.mobile.core.ui.components.RefreshOnResume
import com.financemanage.mobile.core.ui.components.SectionHeader
import com.financemanage.mobile.core.ui.components.SkeletonBlock
import com.financemanage.mobile.core.ui.error.userMessage
import com.financemanage.mobile.core.ui.theme.FinanceTheme
import com.financemanage.mobile.core.ui.theme.Spacing
import com.financemanage.mobile.feature.home.data.HomeBill
import com.financemanage.mobile.feature.home.data.HomeTransaction
import com.financemanage.mobile.feature.home.resources.Res
import com.financemanage.mobile.feature.home.resources.bills_due
import com.financemanage.mobile.feature.home.resources.block_error
import com.financemanage.mobile.feature.home.resources.budgets
import com.financemanage.mobile.feature.home.resources.days_left
import com.financemanage.mobile.feature.home.resources.expense
import com.financemanage.mobile.feature.home.resources.greeting
import com.financemanage.mobile.feature.home.resources.greeting_plain
import com.financemanage.mobile.feature.home.resources.hide_balances
import com.financemanage.mobile.feature.home.resources.income
import com.financemanage.mobile.feature.home.resources.latest_transactions
import com.financemanage.mobile.feature.home.resources.no_bills_due
import com.financemanage.mobile.feature.home.resources.no_transactions
import com.financemanage.mobile.feature.home.resources.notifications
import com.financemanage.mobile.feature.home.resources.overdue
import com.financemanage.mobile.feature.home.resources.pay
import com.financemanage.mobile.feature.home.resources.saving_goal
import com.financemanage.mobile.feature.home.resources.see_all
import com.financemanage.mobile.feature.home.resources.settings
import com.financemanage.mobile.feature.home.resources.show_balances
import com.financemanage.mobile.feature.home.resources.this_month
import com.financemanage.mobile.feature.home.resources.total_balance
import com.financemanage.mobile.feature.home.resources.vs_last_month
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Navigation requested by Home; the app maps each to a route. */
sealed interface HomeDestination {
    data object Notifications : HomeDestination

    data object Settings : HomeDestination

    data object Reports : HomeDestination

    data object Wallets : HomeDestination

    data class Plan(
        val tab: Int,
    ) : HomeDestination

    data object Transactions : HomeDestination

    data class Budget(
        val id: String,
    ) : HomeDestination

    data class Bill(
        val id: String,
    ) : HomeDestination

    data class Goal(
        val id: String,
    ) : HomeDestination

    data class Transaction(
        val id: String,
    ) : HomeDestination
}

private fun money(
    amount: BigDecimal?,
    currency: String?,
    fallback: CurrencyCode,
) = Money(amount ?: BigDecimal.ZERO, currency?.let { CurrencyCode.parse(it) } ?: fallback)

@Composable
fun HomeScreen(
    onNavigate: (HomeDestination) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val extras = state.extras
    RefreshOnResume(viewModel::refresh)
    val refreshing = state.summary.loading && state.summary.data != null
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                val name = extras.profile?.fullName?.takeIf { it.isNotBlank() }
                Text(
                    if (name !=
                        null
                    ) {
                        stringResource(Res.string.greeting, name)
                    } else {
                        stringResource(Res.string.greeting_plain)
                    },
                )
            },
            actions = {
                IconButton(onClick = { onNavigate(HomeDestination.Notifications) }) {
                    BadgedBox(badge = { if (extras.unread > 0) Badge { Text(extras.unread.toString()) } }) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = stringResource(Res.string.notifications),
                        )
                    }
                }
                IconButton(onClick = { onNavigate(HomeDestination.Settings) }) {
                    Icon(Icons.Outlined.AccountCircle, contentDescription = stringResource(Res.string.settings))
                }
            },
        )
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = viewModel::refresh, modifier = Modifier.weight(1f)) {
            LazyColumn(
                contentPadding = PaddingValues(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                item { BalanceCard(state, viewModel::toggleHideBalances) { onNavigate(HomeDestination.Wallets) } }
                item { MonthCard(state) { onNavigate(HomeDestination.Reports) } }
                item { BudgetsBlock(state, onNavigate) }
                item { BillsBlock(state, viewModel::payBill, onNavigate) }
                item { GoalBlock(state, onNavigate) }
                item { TransactionsBlock(state, onNavigate) }
            }
        }
    }
}

@Composable
private fun <T> Block(
    title: String,
    resource: Resource<T>,
    onSeeAll: (() -> Unit)?,
    content: @Composable (T) -> Unit,
) {
    Column {
        SectionHeader(
            title,
            action =
                onSeeAll?.let {
                    { TextButton(onClick = it) { Text(stringResource(Res.string.see_all)) } }
                },
        )
        val data = resource.data
        when {
            data != null -> content(data)
            resource.error != null ->
                Text(
                    "${stringResource(Res.string.block_error)} – ${resource.error?.userMessage().orEmpty()}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = Spacing.md),
                )
            else -> SkeletonBlock(Modifier.fillMaxWidth().padding(horizontal = Spacing.md))
        }
    }
}

@Composable
private fun BalanceCard(
    state: HomeUiState,
    onToggleHide: () -> Unit,
    onClick: () -> Unit,
) {
    val hidden = state.extras.hideBalances
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.total_balance), style = MaterialTheme.typography.labelLarge)
                val total = state.extras.total?.total
                if (total !=
                    null
                ) {
                    MoneyText(total, style = MaterialTheme.typography.headlineMedium)
                } else {
                    SkeletonBlock(Modifier.fillMaxWidth(0.5f))
                }
            }
            IconButton(onClick = onToggleHide) {
                Icon(
                    if (hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription =
                        stringResource(
                            if (hidden) Res.string.show_balances else Res.string.hide_balances,
                        ),
                )
            }
        }
    }
}

@Composable
private fun MonthCard(
    state: HomeUiState,
    onClick: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(Res.string.this_month), style = MaterialTheme.typography.titleSmall)
            val summary = state.summary.data
            if (summary == null) {
                SkeletonBlock(Modifier.fillMaxWidth())
                return@Column
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ChangeCell(
                    stringResource(Res.string.income),
                    money(summary.income, summary.currency, state.currency),
                    summary.incomeChange,
                    FinanceTheme.colors.income,
                )
                ChangeCell(
                    stringResource(Res.string.expense),
                    money(summary.expense, summary.currency, state.currency),
                    summary.expenseChange,
                    FinanceTheme.colors.expense,
                )
            }
        }
    }
}

@Composable
private fun ChangeCell(
    label: String,
    value: Money,
    change: BigDecimal?,
    color: Color,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyText(value, style = MaterialTheme.typography.titleMedium, color = color)
        change?.let {
            val arrow = if (it.signum() >= 0) "▲" else "▼"
            Text(
                stringResource(Res.string.vs_last_month, "$arrow ${it.abs().toStringExpanded()}%"),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun budgetColor(state: String): Color =
    when (state) {
        "ON_TRACK" -> FinanceTheme.colors.income
        "WARNING" -> FinanceTheme.colors.warning
        "EXCEEDED" -> FinanceTheme.colors.expense
        else -> FinanceTheme.colors.neutral
    }

@Composable
private fun BudgetsBlock(
    state: HomeUiState,
    onNavigate: (HomeDestination) -> Unit,
) {
    Block(stringResource(Res.string.budgets), state.budgets, { onNavigate(HomeDestination.Plan(0)) }) {
        Column(Modifier.padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            state.topBudgets.forEach { budget ->
                val status = budget.current
                Column(Modifier.fillMaxWidth().clickable { onNavigate(HomeDestination.Budget(budget.id)) }) {
                    Row {
                        Text(budget.name, modifier = Modifier.weight(1f))
                        Text("${status?.percent ?: 0}%", color = budgetColor(status?.state.orEmpty()))
                    }
                    FinanceProgressBar(status?.percent ?: 0, budgetColor(status?.state.orEmpty()))
                }
            }
        }
    }
}

@Composable
private fun BillsBlock(
    state: HomeUiState,
    onPay: (HomeBill) -> Unit,
    onNavigate: (HomeDestination) -> Unit,
) {
    Block(stringResource(Res.string.bills_due), state.bills, { onNavigate(HomeDestination.Plan(1)) }) { bills ->
        Column {
            PendingBanner(state.extras.payResult)
            state.extras.payError?.let { Text(it.userMessage(), color = MaterialTheme.colorScheme.error) }
            if (bills.isEmpty()) {
                Text(
                    stringResource(Res.string.no_bills_due),
                    modifier = Modifier.padding(horizontal = Spacing.md),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            bills.forEach { bill ->
                ListItem(
                    headlineContent = { Text(bill.name) },
                    supportingContent = {
                        Text(
                            if (bill.overdue) {
                                stringResource(Res.string.overdue)
                            } else {
                                stringResource(Res.string.days_left, (bill.daysUntilDue ?: 0).toInt())
                            },
                            color = if (bill.overdue) FinanceTheme.colors.expense else FinanceTheme.colors.warning,
                        )
                    },
                    trailingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            MoneyText(money(bill.amount, bill.currency, state.currency))
                            Button(
                                onClick = { onPay(bill) },
                                enabled = state.extras.payingBillId == null,
                            ) { Text(stringResource(Res.string.pay)) }
                        }
                    },
                    modifier = Modifier.clickable { onNavigate(HomeDestination.Bill(bill.id)) },
                )
            }
        }
    }
}

@Composable
private fun GoalBlock(
    state: HomeUiState,
    onNavigate: (HomeDestination) -> Unit,
) {
    val goal = state.featuredGoal
    if (state.goals.data != null && goal == null) return
    Block(stringResource(Res.string.saving_goal), state.goals, { onNavigate(HomeDestination.Plan(2)) }) {
        goal?.let { featured ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        onNavigate(
                            HomeDestination.Goal(featured.id),
                        )
                    }.padding(horizontal = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                ProgressRing(featured.percent, MaterialTheme.colorScheme.primary)
                Column {
                    Text(featured.name, style = MaterialTheme.typography.titleMedium)
                    val hidden = state.extras.hideBalances
                    Text(
                        "${MoneyFormatter.format(
                            money(featured.saved, featured.currency, state.currency),
                            hidden,
                        )} / " +
                            MoneyFormatter.format(money(featured.target, featured.currency, state.currency), hidden),
                    )
                    featured.deadline?.let { Text(it.toDisplayString(), style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

@Composable
private fun TransactionsBlock(
    state: HomeUiState,
    onNavigate: (HomeDestination) -> Unit,
) {
    Block(stringResource(Res.string.latest_transactions), state.transactions, {
        onNavigate(HomeDestination.Transactions)
    }) { items ->
        Column {
            if (items.isEmpty()) {
                Text(
                    stringResource(Res.string.no_transactions),
                    modifier = Modifier.padding(horizontal = Spacing.md),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            items.forEach { tx -> TransactionRow(tx, state) { onNavigate(HomeDestination.Transaction(tx.id)) } }
        }
    }
}

@Composable
private fun TransactionRow(
    tx: HomeTransaction,
    state: HomeUiState,
    onClick: () -> Unit,
) {
    val category = state.extras.categories.firstOrNull { it.id == tx.categoryId }
    val wallet = state.extras.wallets.firstOrNull { it.id == tx.walletId }
    val amount = money(tx.amount, tx.currency, state.currency)
    val inflow = tx.type.isInflow || tx.type == TransactionType.INCOME
    ListItem(
        headlineContent = { Text(category?.name ?: tx.note.orEmpty()) },
        supportingContent = { Text(listOfNotNull(tx.occurredOn?.toDisplayString(), wallet?.name).joinToString(" · ")) },
        leadingContent = { CategoryIcon(category?.code, category?.icon) },
        trailingContent = { MoneyText(if (inflow) amount else -amount, colorBySign = true, showPlusSign = true) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
