package com.financemanage.mobile.feature.transaction.presentation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financemanage.mobile.core.data.outbox.OutboxEntity
import com.financemanage.mobile.core.data.outbox.OutboxStatus
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.domain.Category
import com.financemanage.mobile.core.model.domain.Wallet
import com.financemanage.mobile.core.model.time.toDisplayString
import com.financemanage.mobile.core.ui.components.CategoryIcon
import com.financemanage.mobile.core.ui.components.ConfirmDialog
import com.financemanage.mobile.core.ui.components.EmptyState
import com.financemanage.mobile.core.ui.components.ErrorState
import com.financemanage.mobile.core.ui.components.ListSkeleton
import com.financemanage.mobile.core.ui.components.MoneyText
import com.financemanage.mobile.core.ui.components.RefreshOnResume
import com.financemanage.mobile.core.ui.error.userMessage
import com.financemanage.mobile.core.ui.resources.delete_confirm_message
import com.financemanage.mobile.core.ui.resources.today
import com.financemanage.mobile.core.ui.resources.yesterday
import com.financemanage.mobile.core.ui.theme.FinanceTheme
import com.financemanage.mobile.core.ui.theme.Spacing
import com.financemanage.mobile.feature.transaction.domain.Transaction
import com.financemanage.mobile.feature.transaction.domain.TransactionTotals
import com.financemanage.mobile.feature.transaction.resources.Res
import com.financemanage.mobile.feature.transaction.resources.delete_transaction
import com.financemanage.mobile.feature.transaction.resources.discard
import com.financemanage.mobile.feature.transaction.resources.expense
import com.financemanage.mobile.feature.transaction.resources.filter
import com.financemanage.mobile.feature.transaction.resources.income
import com.financemanage.mobile.feature.transaction.resources.net
import com.financemanage.mobile.feature.transaction.resources.no_results
import com.financemanage.mobile.feature.transaction.resources.no_transactions
import com.financemanage.mobile.feature.transaction.resources.no_transactions_message
import com.financemanage.mobile.feature.transaction.resources.recurring_title
import com.financemanage.mobile.feature.transaction.resources.retry
import com.financemanage.mobile.feature.transaction.resources.search_hint
import com.financemanage.mobile.feature.transaction.resources.send_failed
import com.financemanage.mobile.feature.transaction.resources.transactions_title
import com.financemanage.mobile.feature.transaction.resources.waiting_to_send
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.financemanage.mobile.core.ui.resources.Res as CoreRes

private const val SEARCH_DEBOUNCE_MS = 400L
private const val LOAD_MORE_THRESHOLD = 5

/** Transactions tab (F07). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionListScreen(
    walletId: String?,
    categoryId: String?,
    onOpen: (String) -> Unit,
    onRecurring: () -> Unit,
    onBack: (() -> Unit)?,
    viewModel: TransactionListViewModel =
        koinViewModel(key = "tx-$walletId-$categoryId") { parametersOf(walletId, categoryId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showFilter by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf(state.filter.query) }
    var toDelete by remember { mutableStateOf<Transaction?>(null) }
    RefreshOnResume(viewModel::refresh)
    LaunchedEffect(query) {
        if (query != state.filter.query) {
            delay(SEARCH_DEBOUNCE_MS)
            viewModel.setQuery(query)
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(Res.string.transactions_title)) },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            },
            actions = {
                IconButton(onClick = onRecurring) {
                    Icon(Icons.Outlined.Repeat, contentDescription = stringResource(Res.string.recurring_title))
                }
                IconButton(onClick = {
                    showFilter = true
                }) { Icon(Icons.Outlined.FilterList, contentDescription = stringResource(Res.string.filter)) }
            },
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(Res.string.search_hint)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
        )
        ActiveFilterChips(state, onChange = viewModel::setFilter)
        TotalsCard(state.content.totals)

        val content = state.content
        when {
            content.loading && content.items.isEmpty() -> ListSkeleton()
            content.items.isEmpty() && state.pending.isEmpty() && content.error != null ->
                ErrorState(content.error.userMessage(TransactionErrors), onRetry = viewModel::refresh)
            content.items.isEmpty() && state.pending.isEmpty() ->
                EmptyState(
                    title =
                        stringResource(
                            if (state.filter.isEmpty) Res.string.no_transactions else Res.string.no_results,
                        ),
                    message =
                        if (state.filter.isEmpty) {
                            stringResource(
                                Res.string.no_transactions_message,
                            )
                        } else {
                            null
                        },
                )
            else ->
                TransactionList(
                    state = state,
                    onOpen = onOpen,
                    onDelete = { toDelete = it },
                    onLoadMore = viewModel::loadMore,
                    onRetryPending = viewModel::retryPending,
                    onDiscardPending = viewModel::discardPending,
                )
        }
    }
    if (showFilter) {
        FilterSheet(
            filter = state.filter,
            wallets = state.wallets,
            categories = state.categories,
            onApply = {
                showFilter = false
                viewModel.setFilter(it.copy(query = state.filter.query))
            },
            onDismiss = { showFilter = false },
        )
    }
    toDelete?.let { transaction ->
        ConfirmDialog(
            title = stringResource(Res.string.delete_transaction),
            message = stringResource(CoreRes.string.delete_confirm_message),
            onConfirm = {
                toDelete = null
                viewModel.delete(transaction)
            },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
private fun TotalsCard(totals: List<TransactionTotals>) {
    if (totals.isEmpty()) return
    Card(Modifier.fillMaxWidth().padding(Spacing.md)) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            totals.forEach { total ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TotalCell(stringResource(Res.string.income), total.income, positive = true)
                    TotalCell(stringResource(Res.string.expense), -total.expense, positive = false)
                    TotalCell(stringResource(Res.string.net), total.net, positive = null)
                }
            }
        }
    }
}

@Composable
private fun TotalCell(
    label: String,
    value: Money,
    positive: Boolean?,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyText(value, style = MaterialTheme.typography.titleSmall, colorBySign = positive != null || !value.isZero)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveFilterChips(
    state: TransactionListUiState,
    onChange: (com.financemanage.mobile.feature.transaction.domain.TransactionFilter) -> Unit,
) {
    val filter = state.filter
    if (filter.copy(query = "").isEmpty) return
    FlowRow(Modifier.padding(horizontal = Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        filter.walletId?.let { id ->
            Chip(state.wallets.firstOrNull { it.id == id }?.name ?: id) { onChange(filter.copy(walletId = null)) }
        }
        filter.categoryIds.forEach { id ->
            Chip(state.categories.firstOrNull { it.id == id }?.name ?: id) {
                onChange(
                    filter.copy(
                        categoryIds =
                            filter.categoryIds - id,
                    ),
                )
            }
        }
        filter.types.forEach { type -> Chip(typeLabel(type)) { onChange(filter.copy(types = filter.types - type)) } }
        if (filter.from != null || filter.to != null) {
            Chip("${filter.from?.toDisplayString().orEmpty()} – ${filter.to?.toDisplayString().orEmpty()}") {
                onChange(filter.copy(from = null, to = null))
            }
        }
        if (filter.minAmount != null || filter.maxAmount != null) {
            Chip(
                "${filter.minAmount?.toStringExpanded().orEmpty()} – ${filter.maxAmount?.toStringExpanded().orEmpty()}",
            ) {
                onChange(filter.copy(minAmount = null, maxAmount = null))
            }
        }
    }
}

@Composable
private fun Chip(
    label: String,
    onRemove: () -> Unit,
) {
    InputChip(selected = true, onClick = onRemove, label = { Text(label, maxLines = 1) }, trailingIcon = { Text("✕") })
}

@Composable
private fun TransactionList(
    state: TransactionListUiState,
    onOpen: (String) -> Unit,
    onDelete: (Transaction) -> Unit,
    onLoadMore: () -> Unit,
    onRetryPending: (OutboxEntity) -> Unit,
    onDiscardPending: (OutboxEntity) -> Unit,
) {
    val listState = rememberLazyListState()
    val content = state.content
    val shouldLoadMore by remember {
        derivedStateOf {
            val last =
                listState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(shouldLoadMore, content.hasMore) { if (shouldLoadMore && content.hasMore) onLoadMore() }
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val groups = content.items.groupBy { it.occurredOn }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = Spacing.xl),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (state.pending.isNotEmpty()) {
            item { DayHeader(stringResource(Res.string.waiting_to_send), emptyList()) }
            items(
                state.pending,
                key = { "pending-" + it.id },
            ) { entry -> PendingRow(entry, onRetryPending, onDiscardPending) }
        }
        groups.forEach { (date, transactions) ->
            item(key = "day-$date") { DayHeader(dayLabel(date, today), transactions) }
            items(transactions, key = { it.id }) { transaction ->
                SwipeToDelete(enabled = transaction.editable, onDelete = { onDelete(transaction) }) {
                    TransactionRow(transaction, state.wallets, state.categories) { onOpen(transaction.id) }
                }
            }
        }
        if (content.loadingMore) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(Spacing.md),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        }
    }
}

@Composable
private fun dayLabel(
    date: LocalDate?,
    today: LocalDate,
): String =
    when (date) {
        null -> ""
        today -> stringResource(CoreRes.string.today)
        today.minus(DatePeriod(days = 1)) -> stringResource(CoreRes.string.yesterday)
        else -> date.toDisplayString()
    }

@Composable
private fun DayHeader(
    label: String,
    transactions: List<Transaction>,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            // Day total per currency (the common case is a single currency).
            transactions.groupBy { it.amount.currency }.forEach { (_, items) ->
                val total = items.map { it.signedAmount }.reduce { a, b -> a + b }
                MoneyText(total, style = MaterialTheme.typography.labelLarge, colorBySign = true, showPlusSign = true)
            }
        }
    }
}

@Composable
internal fun TransactionRow(
    transaction: Transaction,
    wallets: List<Wallet>,
    categories: List<Category>,
    onClick: () -> Unit,
) {
    val category = categories.firstOrNull { it.id == transaction.categoryId }
    val wallet = wallets.firstOrNull { it.id == transaction.walletId }
    val source = sourceLabel(transaction)
    ListItem(
        headlineContent = {
            Text(category?.name ?: typeLabel(transaction.type), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                listOfNotNull(transaction.note?.takeIf { it.isNotBlank() }, wallet?.name, source).joinToString(" · "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = { CategoryIcon(category?.code, category?.icon) },
        trailingContent = { MoneyText(transaction.signedAmount, colorBySign = true, showPlusSign = true) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDelete(
    enabled: Boolean,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        content()
        return
    }
    val state =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) onDelete()
                false
            },
        )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(FinanceTheme.colors.expense).padding(horizontal = Spacing.md),
                contentAlignment = Alignment.CenterEnd,
            ) { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onError) }
        },
    ) { Surface { content() } }
}

@Composable
private fun PendingRow(
    entry: OutboxEntity,
    onRetry: (OutboxEntity) -> Unit,
    onDiscard: (OutboxEntity) -> Unit,
) {
    val failed = entry.status == OutboxStatus.FAILED.name
    ListItem(
        headlineContent = { Text(entry.summary) },
        supportingContent = {
            Text(
                if (failed) {
                    stringResource(
                        Res.string.send_failed,
                        entry.errorCode.orEmpty(),
                    )
                } else {
                    stringResource(Res.string.waiting_to_send)
                },
                color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = { Icon(Icons.Outlined.CloudUpload, contentDescription = null) },
        trailingContent = {
            if (failed) {
                Row {
                    TextButton(onClick = { onRetry(entry) }) { Text(stringResource(Res.string.retry)) }
                    TextButton(onClick = { onDiscard(entry) }) { Text(stringResource(Res.string.discard)) }
                }
            }
        },
    )
}
