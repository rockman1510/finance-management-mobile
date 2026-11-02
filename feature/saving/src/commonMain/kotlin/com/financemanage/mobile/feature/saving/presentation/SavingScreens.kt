package com.financemanage.mobile.feature.saving.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financemanage.mobile.core.model.CommonCurrencies
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.domain.Frequency
import com.financemanage.mobile.core.model.domain.Wallet
import com.financemanage.mobile.core.model.format.MoneyFormatter
import com.financemanage.mobile.core.model.time.toDisplayString
import com.financemanage.mobile.core.ui.components.AmountTextField
import com.financemanage.mobile.core.ui.components.ConfirmDialog
import com.financemanage.mobile.core.ui.components.DateField
import com.financemanage.mobile.core.ui.components.EmptyState
import com.financemanage.mobile.core.ui.components.FinanceScaffold
import com.financemanage.mobile.core.ui.components.FormColumn
import com.financemanage.mobile.core.ui.components.FormTextField
import com.financemanage.mobile.core.ui.components.ListSkeleton
import com.financemanage.mobile.core.ui.components.LoadingButton
import com.financemanage.mobile.core.ui.components.MoneyText
import com.financemanage.mobile.core.ui.components.OptionPicker
import com.financemanage.mobile.core.ui.components.PendingBanner
import com.financemanage.mobile.core.ui.components.ProgressRing
import com.financemanage.mobile.core.ui.components.RefreshOnResume
import com.financemanage.mobile.core.ui.components.ResourceContent
import com.financemanage.mobile.core.ui.components.SectionHeader
import com.financemanage.mobile.core.ui.components.currencyLabel
import com.financemanage.mobile.core.ui.components.parseAmountInput
import com.financemanage.mobile.core.ui.components.walletLabel
import com.financemanage.mobile.core.ui.error.userMessage
import com.financemanage.mobile.core.ui.resources.action_save
import com.financemanage.mobile.core.ui.resources.delete_confirm_message
import com.financemanage.mobile.core.ui.resources.field_amount
import com.financemanage.mobile.core.ui.resources.field_currency
import com.financemanage.mobile.core.ui.resources.field_date
import com.financemanage.mobile.core.ui.resources.field_invalid_amount
import com.financemanage.mobile.core.ui.resources.field_name
import com.financemanage.mobile.core.ui.resources.field_required
import com.financemanage.mobile.core.ui.resources.field_wallet
import com.financemanage.mobile.core.ui.theme.FinanceTheme
import com.financemanage.mobile.core.ui.theme.LocalHideBalances
import com.financemanage.mobile.core.ui.theme.Spacing
import com.financemanage.mobile.feature.saving.data.GoalDto
import com.financemanage.mobile.feature.saving.resources.Res
import com.financemanage.mobile.feature.saving.resources.add_goal
import com.financemanage.mobile.feature.saving.resources.auto_contribution
import com.financemanage.mobile.feature.saving.resources.auto_next
import com.financemanage.mobile.feature.saving.resources.clear_deadline
import com.financemanage.mobile.feature.saving.resources.deadline
import com.financemanage.mobile.feature.saving.resources.delete_goal
import com.financemanage.mobile.feature.saving.resources.deposit
import com.financemanage.mobile.feature.saving.resources.edit_goal
import com.financemanage.mobile.feature.saving.resources.error_currency_mismatch
import com.financemanage.mobile.feature.saving.resources.error_insufficient
import com.financemanage.mobile.feature.saving.resources.error_not_empty
import com.financemanage.mobile.feature.saving.resources.error_round_up_taken
import com.financemanage.mobile.feature.saving.resources.freq_MONTHLY
import com.financemanage.mobile.feature.saving.resources.freq_WEEKLY
import com.financemanage.mobile.feature.saving.resources.frequency
import com.financemanage.mobile.feature.saving.resources.history
import com.financemanage.mobile.feature.saving.resources.label_auto
import com.financemanage.mobile.feature.saving.resources.label_round_up
import com.financemanage.mobile.feature.saving.resources.milestone_half
import com.financemanage.mobile.feature.saving.resources.no_goals
import com.financemanage.mobile.feature.saving.resources.no_goals_message
import com.financemanage.mobile.feature.saving.resources.reached
import com.financemanage.mobile.feature.saving.resources.round_up
import com.financemanage.mobile.feature.saving.resources.round_up_example
import com.financemanage.mobile.feature.saving.resources.round_up_unit
import com.financemanage.mobile.feature.saving.resources.save_per_month
import com.financemanage.mobile.feature.saving.resources.saved_of
import com.financemanage.mobile.feature.saving.resources.source_AUTO
import com.financemanage.mobile.feature.saving.resources.source_MANUAL
import com.financemanage.mobile.feature.saving.resources.source_ROUND_UP
import com.financemanage.mobile.feature.saving.resources.start_date
import com.financemanage.mobile.feature.saving.resources.target
import com.financemanage.mobile.feature.saving.resources.turn_off
import com.financemanage.mobile.feature.saving.resources.turn_on
import com.financemanage.mobile.feature.saving.resources.withdraw
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.financemanage.mobile.core.ui.resources.Res as CoreRes

private val SavingErrors: Map<String, StringResource> =
    mapOf(
        "INSUFFICIENT_SAVINGS" to Res.string.error_insufficient,
        "GOAL_NOT_EMPTY" to Res.string.error_not_empty,
        "ROUND_UP_TAKEN" to Res.string.error_round_up_taken,
        "CURRENCY_MISMATCH" to Res.string.error_currency_mismatch,
    )
private val RoundUpUnits = listOf(1_000, 5_000, 10_000)

/** Savings tab of the Plan screen (F15). */
@Composable
fun SavingsTab(
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    viewModel: GoalListViewModel = koinViewModel(),
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
                    title = stringResource(Res.string.no_goals),
                    message = stringResource(Res.string.no_goals_message),
                    actionLabel = stringResource(Res.string.add_goal),
                    onAction = onAdd,
                )
            },
        ) { goals ->
            LazyColumn(
                contentPadding = PaddingValues(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(goals, key = { it.id }) { goal -> GoalCard(goal) { onOpen(goal.id) } }
            }
        }
        FloatingActionButton(onClick = onAdd, modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.md)) {
            Icon(Icons.Outlined.Add, contentDescription = stringResource(Res.string.add_goal))
        }
    }
}

@Composable
fun GoalCard(
    goal: GoalDto,
    onClick: () -> Unit,
) {
    val hidden = LocalHideBalances.current
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ProgressRing(
                goal.percent,
                if (goal.reached) FinanceTheme.colors.income else MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(goal.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(
                        Res.string.saved_of,
                        MoneyFormatter.format(goal.money(goal.saved), hidden),
                        MoneyFormatter.format(goal.money(goal.target), hidden),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                goal.deadline?.let {
                    Text(
                        "${stringResource(Res.string.deadline)}: ${it.toDisplayString()}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                goal.monthlySuggestion?.takeIf { !goal.reached && it.signum() > 0 }?.let {
                    Text(
                        stringResource(Res.string.save_per_month, MoneyFormatter.format(goal.money(it), hidden)),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                val labels =
                    listOfNotNull(
                        goal.autoContribution?.let { stringResource(Res.string.label_auto) },
                        goal.roundUp?.let { stringResource(Res.string.label_round_up) },
                        if (goal.reached) stringResource(Res.string.reached) else null,
                    )
                if (labels.isNotEmpty()) {
                    Text(
                        labels.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    id: String,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    viewModel: GoalDetailViewModel = koinViewModel(key = "goal-$id") { parametersOf(id) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var moveSheet by remember { mutableStateOf<Boolean?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    RefreshOnResume(viewModel::refresh)
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    val goal = state.goal
    FinanceScaffold(
        title = goal?.name.orEmpty(),
        onBack = onBack,
        actions = {
            IconButton(
                onClick = onEdit,
            ) { Icon(Icons.Outlined.Edit, contentDescription = stringResource(Res.string.edit_goal)) }
            IconButton(onClick = {
                confirmDelete = true
            }) { Icon(Icons.Outlined.Delete, contentDescription = stringResource(Res.string.delete_goal)) }
        },
    ) { padding ->
        if (goal == null) {
            ListSkeleton(Modifier.padding(padding))
            return@FinanceScaffold
        }
        val goalWallets = state.wallets.filter { it.currency == goal.currencyCode }
        LazyColumn(Modifier.padding(padding)) {
            item { PendingBanner(state.pending) }
            item {
                Column(
                    Modifier.fillMaxWidth().padding(Spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    ProgressRing(goal.percent, MaterialTheme.colorScheme.primary, size = 120.dp)
                    MoneyText(goal.money(goal.saved), style = MaterialTheme.typography.headlineMedium)
                    Text(
                        stringResource(
                            Res.string.saved_of,
                            MoneyFormatter.format(goal.money(goal.saved), LocalHideBalances.current),
                            MoneyFormatter.format(goal.money(goal.target), LocalHideBalances.current),
                        ),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Button(
                            onClick = { moveSheet = true },
                            enabled = !state.busy,
                        ) { Text(stringResource(Res.string.deposit)) }
                        OutlinedButton(
                            onClick = { moveSheet = false },
                            enabled = !state.busy,
                        ) { Text(stringResource(Res.string.withdraw)) }
                    }
                    state.error?.let { Text(it.userMessage(SavingErrors), color = MaterialTheme.colorScheme.error) }
                }
            }
            item { AutoContributionSection(goal, goalWallets, state.today, viewModel) }
            item { RoundUpSection(goal, goalWallets, viewModel) }
            item { SectionHeader(stringResource(Res.string.history)) }
            items(goal.movements, key = { it.id }) { movement ->
                val source =
                    when (movement.source) {
                        "AUTO" -> Res.string.source_AUTO
                        "ROUND_UP" -> Res.string.source_ROUND_UP
                        else -> Res.string.source_MANUAL
                    }
                val signed = goal.money(movement.amount).let { if (movement.type == "WITHDRAWAL") -it else it }
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(
                                if (movement.type ==
                                    "WITHDRAWAL"
                                ) {
                                    Res.string.withdraw
                                } else {
                                    Res.string.deposit
                                },
                            ),
                        )
                    },
                    supportingContent = {
                        Text(
                            listOfNotNull(
                                movement.date?.toDisplayString(),
                                stringResource(source),
                                movement.failureReason,
                            ).joinToString(" · "),
                        )
                    },
                    trailingContent = { MoneyText(signed, colorBySign = true, showPlusSign = true) },
                )
            }
        }
    }
    moveSheet?.let { deposit ->
        goal?.let {
            MoveSheet(
                deposit = deposit,
                currency = it.currencyCode,
                wallets = state.wallets.filter { w -> w.currency == it.currencyCode },
                onConfirm = { amount, walletId, date ->
                    moveSheet = null
                    viewModel.move(deposit, amount, walletId, date)
                },
                onDismiss = { moveSheet = null },
            )
        }
    }
    when (state.milestone) {
        Milestone.HALF, Milestone.REACHED ->
            AlertDialog(
                onDismissRequest = viewModel::dismissMilestone,
                title = { Text("🎉") },
                text = {
                    Text(
                        stringResource(
                            if (state.milestone ==
                                Milestone.REACHED
                            ) {
                                Res.string.reached
                            } else {
                                Res.string.milestone_half
                            },
                        ),
                    )
                },
                confirmButton = { TextButton(onClick = viewModel::dismissMilestone) { Text("OK") } },
            )
        null -> Unit
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(Res.string.delete_goal),
            message = stringResource(CoreRes.string.delete_confirm_message),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun AutoContributionSection(
    goal: GoalDto,
    wallets: List<Wallet>,
    today: LocalDate?,
    viewModel: GoalDetailViewModel,
) {
    val auto = goal.autoContribution
    var amount by remember(auto) { mutableStateOf(auto?.amount?.toStringExpanded().orEmpty()) }
    var walletId by remember(auto) { mutableStateOf(auto?.walletId ?: wallets.firstOrNull()?.id) }
    var frequency by remember(auto) { mutableStateOf(auto?.frequency ?: Frequency.MONTHLY) }
    var start by remember(today) { mutableStateOf(today) }
    Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(stringResource(Res.string.auto_contribution), style = MaterialTheme.typography.titleSmall)
        auto?.nextDate?.let {
            Text(stringResource(Res.string.auto_next, it.toDisplayString()), style = MaterialTheme.typography.bodySmall)
        }
        AmountTextField(amount, { amount = it }, goal.currencyCode, stringResource(CoreRes.string.field_amount))
        OptionPicker(
            stringResource(CoreRes.string.field_wallet),
            wallets,
            wallets.firstOrNull {
                it.id == walletId
            },
            { walletLabel(it) },
            {
                walletId =
                    it.id
            },
        )
        OptionPicker(
            stringResource(Res.string.frequency),
            listOf(Frequency.WEEKLY, Frequency.MONTHLY),
            frequency,
            { stringResource(if (it == Frequency.WEEKLY) Res.string.freq_WEEKLY else Res.string.freq_MONTHLY) },
            { frequency = it },
        )
        if (auto == null) DateField(stringResource(Res.string.start_date), start, { start = it })
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Button(onClick = {
                val value = parseAmountInput(amount, goal.currencyCode.fractionDigits)
                val wallet = walletId
                if (value != null &&
                    value.signum() > 0 &&
                    wallet != null
                ) {
                    viewModel.setAutoContribution(value, wallet, frequency, start)
                }
            }) { Text(stringResource(if (auto == null) Res.string.turn_on else CoreRes.string.action_save)) }
            if (auto !=
                null
            ) {
                OutlinedButton(
                    onClick = viewModel::stopAutoContribution,
                ) { Text(stringResource(Res.string.turn_off)) }
            }
        }
    }
}

@Composable
private fun RoundUpSection(
    goal: GoalDto,
    wallets: List<Wallet>,
    viewModel: GoalDetailViewModel,
) {
    val roundUp = goal.roundUp
    var walletId by remember(roundUp) { mutableStateOf(roundUp?.walletId ?: wallets.firstOrNull()?.id) }
    var unit by remember(roundUp) { mutableStateOf(roundUp?.unit?.intValue(false) ?: RoundUpUnits.last()) }
    Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(stringResource(Res.string.round_up), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(Res.string.round_up_example), style = MaterialTheme.typography.bodySmall)
        OptionPicker(
            stringResource(CoreRes.string.field_wallet),
            wallets,
            wallets.firstOrNull {
                it.id == walletId
            },
            { walletLabel(it) },
            {
                walletId =
                    it.id
            },
        )
        OptionPicker(
            stringResource(Res.string.round_up_unit),
            RoundUpUnits,
            unit,
            { MoneyFormatter.formatNumber(BigDecimal.fromInt(it), 0) },
            { unit = it },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Button(onClick = { walletId?.let { viewModel.setRoundUp(it, BigDecimal.fromInt(unit)) } }) {
                Text(stringResource(if (roundUp == null) Res.string.turn_on else CoreRes.string.action_save))
            }
            if (roundUp !=
                null
            ) {
                OutlinedButton(onClick = viewModel::stopRoundUp) { Text(stringResource(Res.string.turn_off)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveSheet(
    deposit: Boolean,
    currency: CurrencyCode,
    wallets: List<Wallet>,
    onConfirm: (BigDecimal, String, LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var walletId by remember { mutableStateOf(wallets.firstOrNull()?.id) }
    var date by remember { mutableStateOf<LocalDate?>(null) }
    var invalid by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                stringResource(if (deposit) Res.string.deposit else Res.string.withdraw),
                style = MaterialTheme.typography.titleMedium,
            )
            AmountTextField(
                amount,
                {
                    amount = it
                    invalid = false
                },
                currency,
                stringResource(CoreRes.string.field_amount),
                error = if (invalid) stringResource(CoreRes.string.field_invalid_amount) else null,
            )
            OptionPicker(
                stringResource(CoreRes.string.field_wallet),
                wallets,
                wallets.firstOrNull {
                    it.id == walletId
                },
                { walletLabel(it) },
                {
                    walletId =
                        it.id
                },
            )
            DateField(stringResource(CoreRes.string.field_date), date, { date = it })
            Button(
                onClick = {
                    val value = parseAmountInput(amount, currency.fractionDigits)
                    val wallet = walletId
                    if (value == null ||
                        value.signum() <= 0 ||
                        wallet == null
                    ) {
                        invalid = true
                    } else {
                        onConfirm(value, wallet, date)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(if (deposit) Res.string.deposit else Res.string.withdraw)) }
        }
    }
}

@Composable
fun GoalFormScreen(
    id: String?,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: GoalFormViewModel = koinViewModel(key = "goal-form-$id") { parametersOf(id) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    FinanceScaffold(
        title = stringResource(if (state.isEdit) Res.string.edit_goal else Res.string.add_goal),
        onBack = onBack,
    ) { padding ->
        FormColumn(padding) {
            FormTextField(
                state.name,
                viewModel::onName,
                stringResource(CoreRes.string.field_name),
                maxLength = 100,
                error = if (state.nameMissing) stringResource(CoreRes.string.field_required) else null,
            )
            AmountTextField(
                state.target,
                viewModel::onTarget,
                state.currency,
                stringResource(Res.string.target),
                error = if (state.invalidTarget) stringResource(CoreRes.string.field_invalid_amount) else null,
            )
            OptionPicker(
                stringResource(CoreRes.string.field_currency),
                (CommonCurrencies + state.currency).distinct(),
                state.currency,
                { currencyLabel(it) },
                viewModel::onCurrency,
                enabled = !state.isEdit,
            )
            DateField(stringResource(Res.string.deadline), state.deadline, viewModel::onDeadline)
            if (state.deadline != null) {
                TextButton(onClick = { viewModel.onDeadline(null) }) { Text(stringResource(Res.string.clear_deadline)) }
            }
            state.error?.let { Text(it.userMessage(SavingErrors), color = MaterialTheme.colorScheme.error) }
            LoadingButton(stringResource(CoreRes.string.action_save), onClick = viewModel::save, loading = state.saving)
        }
    }
}
