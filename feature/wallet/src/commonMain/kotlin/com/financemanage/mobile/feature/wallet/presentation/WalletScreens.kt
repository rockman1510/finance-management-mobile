package com.financemanage.mobile.feature.wallet.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.CommonCurrencies
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.domain.Wallet
import com.financemanage.mobile.core.model.domain.WalletType
import com.financemanage.mobile.core.model.format.MoneyFormatter
import com.financemanage.mobile.core.model.percentOf
import com.financemanage.mobile.core.ui.components.AmountTextField
import com.financemanage.mobile.core.ui.components.ConfirmDialog
import com.financemanage.mobile.core.ui.components.CreatableWalletTypes
import com.financemanage.mobile.core.ui.components.DateField
import com.financemanage.mobile.core.ui.components.EmptyState
import com.financemanage.mobile.core.ui.components.FinanceProgressBar
import com.financemanage.mobile.core.ui.components.FinanceScaffold
import com.financemanage.mobile.core.ui.components.FormColumn
import com.financemanage.mobile.core.ui.components.FormTextField
import com.financemanage.mobile.core.ui.components.InfoRow
import com.financemanage.mobile.core.ui.components.ListSkeleton
import com.financemanage.mobile.core.ui.components.LoadingButton
import com.financemanage.mobile.core.ui.components.MoneyText
import com.financemanage.mobile.core.ui.components.OptionPicker
import com.financemanage.mobile.core.ui.components.RefreshOnResume
import com.financemanage.mobile.core.ui.components.ResourceContent
import com.financemanage.mobile.core.ui.components.SwitchRow
import com.financemanage.mobile.core.ui.components.UpdatedAtText
import com.financemanage.mobile.core.ui.components.currencyLabel
import com.financemanage.mobile.core.ui.components.walletTypeIcon
import com.financemanage.mobile.core.ui.components.walletTypeLabel
import com.financemanage.mobile.core.ui.error.userMessage
import com.financemanage.mobile.core.ui.resources.action_edit
import com.financemanage.mobile.core.ui.resources.action_save
import com.financemanage.mobile.core.ui.resources.delete_confirm_message
import com.financemanage.mobile.core.ui.resources.field_currency
import com.financemanage.mobile.core.ui.resources.field_date
import com.financemanage.mobile.core.ui.resources.field_invalid_amount
import com.financemanage.mobile.core.ui.resources.field_name
import com.financemanage.mobile.core.ui.resources.field_note
import com.financemanage.mobile.core.ui.resources.field_required
import com.financemanage.mobile.core.ui.theme.FinanceTheme
import com.financemanage.mobile.core.ui.theme.LocalHideBalances
import com.financemanage.mobile.core.ui.theme.Spacing
import com.financemanage.mobile.feature.wallet.resources.Res
import com.financemanage.mobile.feature.wallet.resources.add_wallet
import com.financemanage.mobile.feature.wallet.resources.adjust_balance
import com.financemanage.mobile.feature.wallet.resources.adjust_hint
import com.financemanage.mobile.feature.wallet.resources.archive
import com.financemanage.mobile.feature.wallet.resources.archived_label
import com.financemanage.mobile.feature.wallet.resources.archived_section
import com.financemanage.mobile.feature.wallet.resources.balance
import com.financemanage.mobile.feature.wallet.resources.credit_limit
import com.financemanage.mobile.feature.wallet.resources.delete_wallet
import com.financemanage.mobile.feature.wallet.resources.due_on_day
import com.financemanage.mobile.feature.wallet.resources.edit_wallet
import com.financemanage.mobile.feature.wallet.resources.error_credit_limit
import com.financemanage.mobile.feature.wallet.resources.error_day
import com.financemanage.mobile.feature.wallet.resources.error_has_transactions
import com.financemanage.mobile.feature.wallet.resources.error_nothing_to_adjust
import com.financemanage.mobile.feature.wallet.resources.excluded_from_total
import com.financemanage.mobile.feature.wallet.resources.excluded_label
import com.financemanage.mobile.feature.wallet.resources.initial_balance
import com.financemanage.mobile.feature.wallet.resources.missing_rates
import com.financemanage.mobile.feature.wallet.resources.new_balance
import com.financemanage.mobile.feature.wallet.resources.no_wallets_message
import com.financemanage.mobile.feature.wallet.resources.payment_due_day
import com.financemanage.mobile.feature.wallet.resources.statement_day
import com.financemanage.mobile.feature.wallet.resources.unarchive
import com.financemanage.mobile.feature.wallet.resources.used_of_limit
import com.financemanage.mobile.feature.wallet.resources.view_transactions
import com.financemanage.mobile.feature.wallet.resources.wallet_type
import com.financemanage.mobile.feature.wallet.resources.wallets_title
import com.financemanage.mobile.feature.wallet.resources.wallets_total
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.financemanage.mobile.core.ui.resources.Res as CoreRes

private val WalletErrors: Map<String, StringResource> =
    mapOf(
        "WALLET_HAS_TRANSACTIONS" to Res.string.error_has_transactions,
        "NOTHING_TO_ADJUST" to Res.string.error_nothing_to_adjust,
    )

@Composable
private fun ErrorLine(error: AppError?) {
    error?.let { Text(it.userMessage(WalletErrors), color = MaterialTheme.colorScheme.error) }
}

@Composable
fun WalletListScreen(
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    viewModel: WalletListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showArchived by rememberSaveable { mutableStateOf(false) }
    RefreshOnResume(viewModel::refresh)
    FinanceScaffold(
        title = stringResource(Res.string.wallets_title),
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(Res.string.add_wallet))
            }
        },
    ) { padding ->
        ResourceContent(
            resource = state.wallets,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding),
            isEmpty = { it.isEmpty() },
            empty = {
                EmptyState(
                    title = stringResource(Res.string.wallets_title),
                    message = stringResource(Res.string.no_wallets_message),
                    actionLabel = stringResource(Res.string.add_wallet),
                    onAction = onAdd,
                )
            },
        ) { wallets ->
            val active = wallets.filter { !it.archived }
            val archived = wallets.filter { it.archived }
            LazyColumn(
                contentPadding =
                    androidx.compose.foundation.layout
                        .PaddingValues(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                item {
                    TotalCard(state.total?.total, state.total?.missingRates.orEmpty())
                    UpdatedAtText(state.wallets.updatedAt, Modifier.padding(top = Spacing.xs))
                }
                items(active, key = { it.id }) { wallet -> WalletCard(wallet) { onOpen(wallet.id) } }
                if (archived.isNotEmpty()) {
                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(Res.string.archived_section, archived.size)) },
                            trailingContent = {
                                Icon(
                                    if (showArchived) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                    null,
                                )
                            },
                            modifier = Modifier.clickable { showArchived = !showArchived },
                        )
                    }
                    if (showArchived) {
                        items(
                            archived,
                            key = { it.id },
                        ) { wallet -> WalletCard(wallet) { onOpen(wallet.id) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalCard(
    total: Money?,
    missingRates: List<String>,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(Res.string.wallets_total), style = MaterialTheme.typography.labelLarge)
            if (total !=
                null
            ) {
                MoneyText(total, style = MaterialTheme.typography.headlineMedium, colorBySign = total.isNegative)
            }
            if (missingRates.isNotEmpty()) {
                Text(
                    stringResource(Res.string.missing_rates, missingRates.joinToString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun WalletCard(
    wallet: Wallet,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(walletTypeIcon(wallet.type), contentDescription = null)
                Column(Modifier.weight(1f)) {
                    Text(wallet.name, style = MaterialTheme.typography.titleMedium)
                    val tags =
                        listOfNotNull(
                            walletTypeLabel(wallet.type),
                            if (wallet.excludedFromTotal) stringResource(Res.string.excluded_label) else null,
                            if (wallet.archived) stringResource(Res.string.archived_label) else null,
                        )
                    Text(
                        tags.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MoneyText(
                    wallet.balance,
                    style = MaterialTheme.typography.titleMedium,
                    colorBySign = wallet.balance.isNegative,
                )
            }
            if (wallet.type == WalletType.CREDIT_CARD) CreditCardUsage(wallet)
        }
    }
}

@Composable
private fun CreditCardUsage(wallet: Wallet) {
    val limit = wallet.creditLimit ?: return
    val available = wallet.availableCredit ?: limit
    val used = limit - available
    val percent = used.amount.percentOf(limit.amount)
    val hidden = LocalHideBalances.current
    FinanceProgressBar(percent, if (percent >= 80) FinanceTheme.colors.expense else FinanceTheme.colors.income)
    Text(
        stringResource(
            Res.string.used_of_limit,
            MoneyFormatter.format(used, hidden),
            MoneyFormatter.format(limit, hidden),
        ),
        style = MaterialTheme.typography.bodySmall,
    )
    wallet.paymentDueDay?.let {
        Text(stringResource(Res.string.due_on_day, it), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun WalletDetailScreen(
    walletId: String,
    onEdit: () -> Unit,
    onAdjust: () -> Unit,
    onTransactions: () -> Unit,
    onBack: () -> Unit,
    viewModel: WalletDetailViewModel = koinViewModel(key = "wallet-$walletId") { parametersOf(walletId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    RefreshOnResume(viewModel::refresh)
    val wallet = state.wallet
    FinanceScaffold(title = wallet?.name.orEmpty(), onBack = onBack) { padding ->
        if (wallet == null) {
            ListSkeleton(Modifier.padding(padding))
            return@FinanceScaffold
        }
        FormColumn(padding) {
            Text(stringResource(Res.string.balance), style = MaterialTheme.typography.labelLarge)
            MoneyText(
                wallet.balance,
                style = MaterialTheme.typography.headlineMedium,
                colorBySign = wallet.balance.isNegative,
            )
            InfoRow(stringResource(Res.string.wallet_type), walletTypeLabel(wallet.type))
            InfoRow(stringResource(CoreRes.string.field_currency), wallet.currency.value)
            InfoRow(stringResource(Res.string.initial_balance)) { MoneyText(wallet.initialBalance) }
            if (wallet.type == WalletType.CREDIT_CARD) CreditCardUsage(wallet)
            ErrorLine(state.error)
            OutlinedButton(onClick = onTransactions, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.view_transactions))
            }
            if (!wallet.archived) {
                OutlinedButton(onClick = onAdjust, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.adjust_balance))
                }
            }
            OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(CoreRes.string.action_edit))
            }
            OutlinedButton(onClick = {
                viewModel.setArchived(!wallet.archived)
            }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (wallet.archived) Res.string.unarchive else Res.string.archive))
            }
            OutlinedButton(
                onClick = { confirmDelete = true },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.delete_wallet), color = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(Res.string.delete_wallet),
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
fun WalletFormScreen(
    walletId: String?,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: WalletFormViewModel = koinViewModel(key = "wallet-form-$walletId") { parametersOf(walletId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    val required = stringResource(CoreRes.string.field_required)
    val dayError = stringResource(Res.string.error_day)
    FinanceScaffold(
        title = stringResource(if (state.isEdit) Res.string.edit_wallet else Res.string.add_wallet),
        onBack = onBack,
    ) { padding ->
        FormColumn(padding) {
            FormTextField(
                value = state.name,
                onValueChange = viewModel::onName,
                label = stringResource(CoreRes.string.field_name),
                maxLength = 60,
                error = if (WalletFormProblem.NAME in state.problems) required else null,
            )
            OptionPicker(
                label = stringResource(Res.string.wallet_type),
                options = CreatableWalletTypes,
                selected = state.type,
                optionLabel = { walletTypeLabel(it) },
                onSelect = viewModel::onType,
                enabled = !state.isEdit,
            )
            OptionPicker(
                label = stringResource(CoreRes.string.field_currency),
                options = (CommonCurrencies + state.currency).distinct(),
                selected = state.currency,
                optionLabel = { currencyLabel(it) },
                onSelect = viewModel::onCurrency,
                // The currency of a wallet can never change (API §5.1).
                enabled = !state.isEdit,
            )
            AmountTextField(
                value = state.initialBalance,
                onValueChange = viewModel::onInitialBalance,
                currency = state.currency,
                label = stringResource(Res.string.initial_balance),
                allowNegative = true,
                error =
                    if (WalletFormProblem.AMOUNT in
                        state.problems
                    ) {
                        stringResource(CoreRes.string.field_invalid_amount)
                    } else {
                        null
                    },
            )
            SwitchRow(stringResource(Res.string.excluded_from_total), state.excludedFromTotal, viewModel::onExcluded)
            if (state.type == WalletType.CREDIT_CARD) {
                AmountTextField(
                    value = state.creditLimit,
                    onValueChange = viewModel::onCreditLimit,
                    currency = state.currency,
                    label = stringResource(Res.string.credit_limit),
                    error =
                        if (WalletFormProblem.CREDIT_LIMIT in
                            state.problems
                        ) {
                            stringResource(Res.string.error_credit_limit)
                        } else {
                            null
                        },
                )
                FormTextField(
                    value = state.statementDay,
                    onValueChange = viewModel::onStatementDay,
                    label = stringResource(Res.string.statement_day),
                    keyboardType = KeyboardType.Number,
                    error = if (WalletFormProblem.STATEMENT_DAY in state.problems) dayError else null,
                )
                FormTextField(
                    value = state.paymentDueDay,
                    onValueChange = viewModel::onPaymentDueDay,
                    label = stringResource(Res.string.payment_due_day),
                    keyboardType = KeyboardType.Number,
                    error = if (WalletFormProblem.DUE_DAY in state.problems) dayError else null,
                )
            }
            ErrorLine(state.error)
            LoadingButton(
                stringResource(CoreRes.string.action_save),
                onClick = viewModel::save,
                loading = state.saving,
                enabled = state.loaded,
            )
        }
    }
}

@Composable
fun AdjustBalanceScreen(
    walletId: String,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: AdjustBalanceViewModel = koinViewModel(key = "adjust-$walletId") { parametersOf(walletId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    FinanceScaffold(title = stringResource(Res.string.adjust_balance), onBack = onBack) { padding ->
        FormColumn(padding) {
            val wallet = state.wallet
            Text(stringResource(Res.string.adjust_hint), style = MaterialTheme.typography.bodyMedium)
            if (wallet != null) {
                InfoRow(stringResource(Res.string.balance)) { MoneyText(wallet.balance) }
                AmountTextField(
                    value = state.newBalance,
                    onValueChange = viewModel::onBalance,
                    currency = wallet.currency,
                    label = stringResource(Res.string.new_balance),
                    allowNegative = true,
                    error = if (state.invalidAmount) stringResource(CoreRes.string.field_invalid_amount) else null,
                )
            }
            DateField(stringResource(CoreRes.string.field_date), state.date, viewModel::onDate)
            FormTextField(state.note, viewModel::onNote, stringResource(CoreRes.string.field_note), maxLength = 500)
            ErrorLine(state.error)
            LoadingButton(
                stringResource(CoreRes.string.action_save),
                onClick = viewModel::save,
                loading = state.saving,
                enabled =
                    wallet != null,
            )
        }
    }
}
