package com.financemanage.mobile.feature.deposit.presentation

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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financemanage.mobile.core.model.AsyncStatus
import com.financemanage.mobile.core.model.CommonCurrencies
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
import com.financemanage.mobile.core.ui.components.InfoRow
import com.financemanage.mobile.core.ui.components.ListSkeleton
import com.financemanage.mobile.core.ui.components.LoadingButton
import com.financemanage.mobile.core.ui.components.MoneyText
import com.financemanage.mobile.core.ui.components.OptionPicker
import com.financemanage.mobile.core.ui.components.PendingBanner
import com.financemanage.mobile.core.ui.components.RefreshOnResume
import com.financemanage.mobile.core.ui.components.ResourceContent
import com.financemanage.mobile.core.ui.components.SectionHeader
import com.financemanage.mobile.core.ui.components.currencyLabel
import com.financemanage.mobile.core.ui.components.walletLabel
import com.financemanage.mobile.core.ui.error.userMessage
import com.financemanage.mobile.core.ui.resources.action_save
import com.financemanage.mobile.core.ui.resources.delete_confirm_message
import com.financemanage.mobile.core.ui.resources.field_currency
import com.financemanage.mobile.core.ui.resources.field_note
import com.financemanage.mobile.core.ui.resources.field_required
import com.financemanage.mobile.core.ui.resources.processing
import com.financemanage.mobile.core.ui.theme.FinanceTheme
import com.financemanage.mobile.core.ui.theme.LocalHideBalances
import com.financemanage.mobile.core.ui.theme.LocalIsOnline
import com.financemanage.mobile.core.ui.theme.Spacing
import com.financemanage.mobile.feature.deposit.domain.model.DepositDto
import com.financemanage.mobile.feature.deposit.resources.Res
import com.financemanage.mobile.feature.deposit.resources.account_number
import com.financemanage.mobile.feature.deposit.resources.accrued_interest
import com.financemanage.mobile.feature.deposit.resources.add_deposit
import com.financemanage.mobile.feature.deposit.resources.annual_rate
import com.financemanage.mobile.feature.deposit.resources.calc_COMPOUND_MONTHLY
import com.financemanage.mobile.feature.deposit.resources.calc_SIMPLE_30_360
import com.financemanage.mobile.feature.deposit.resources.calc_SIMPLE_ACTUAL_365
import com.financemanage.mobile.feature.deposit.resources.calc_method
import com.financemanage.mobile.feature.deposit.resources.delete_deposit
import com.financemanage.mobile.feature.deposit.resources.deposits
import com.financemanage.mobile.feature.deposit.resources.early_rate
import com.financemanage.mobile.feature.deposit.resources.early_warning
import com.financemanage.mobile.feature.deposit.resources.error_already_settled
import com.financemanage.mobile.feature.deposit.resources.error_rate
import com.financemanage.mobile.feature.deposit.resources.error_settlement_date
import com.financemanage.mobile.feature.deposit.resources.error_term
import com.financemanage.mobile.feature.deposit.resources.expected_interest
import com.financemanage.mobile.feature.deposit.resources.from_wallet
import com.financemanage.mobile.feature.deposit.resources.funding
import com.financemanage.mobile.feature.deposit.resources.history
import com.financemanage.mobile.feature.deposit.resources.institution
import com.financemanage.mobile.feature.deposit.resources.interest_payment
import com.financemanage.mobile.feature.deposit.resources.maturity_date
import com.financemanage.mobile.feature.deposit.resources.no_deposits
import com.financemanage.mobile.feature.deposit.resources.no_deposits_message
import com.financemanage.mobile.feature.deposit.resources.no_wallet
import com.financemanage.mobile.feature.deposit.resources.open_date
import com.financemanage.mobile.feature.deposit.resources.pay_AT_MATURITY
import com.financemanage.mobile.feature.deposit.resources.pay_MONTHLY
import com.financemanage.mobile.feature.deposit.resources.pay_UPFRONT
import com.financemanage.mobile.feature.deposit.resources.payout
import com.financemanage.mobile.feature.deposit.resources.preview
import com.financemanage.mobile.feature.deposit.resources.principal
import com.financemanage.mobile.feature.deposit.resources.quote_total
import com.financemanage.mobile.feature.deposit.resources.roll_NONE
import com.financemanage.mobile.feature.deposit.resources.roll_PRINCIPAL_AND_INTEREST
import com.financemanage.mobile.feature.deposit.resources.roll_PRINCIPAL_ONLY
import com.financemanage.mobile.feature.deposit.resources.rollover
import com.financemanage.mobile.feature.deposit.resources.settle
import com.financemanage.mobile.feature.deposit.resources.settle_date
import com.financemanage.mobile.feature.deposit.resources.status_ACTIVE
import com.financemanage.mobile.feature.deposit.resources.status_MATURED
import com.financemanage.mobile.feature.deposit.resources.status_SETTLED
import com.financemanage.mobile.feature.deposit.resources.term_months
import com.financemanage.mobile.feature.deposit.resources.to_wallet
import com.financemanage.mobile.feature.deposit.resources.total_at_maturity
import com.financemanage.mobile.feature.deposit.resources.tx_INTEREST_PAID
import com.financemanage.mobile.feature.deposit.resources.tx_OPEN
import com.financemanage.mobile.feature.deposit.resources.tx_ROLLOVER
import com.financemanage.mobile.feature.deposit.resources.tx_SETTLE
import com.financemanage.mobile.feature.deposit.resources.type
import com.financemanage.mobile.feature.deposit.resources.type_DEMAND
import com.financemanage.mobile.feature.deposit.resources.type_TERM
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.financemanage.mobile.core.ui.resources.Res as CoreRes

private val DepositErrors: Map<String, StringResource> =
    mapOf(
        "DEPOSIT_ALREADY_SETTLED" to Res.string.error_already_settled,
        "INVALID_SETTLEMENT_DATE" to Res.string.error_settlement_date,
        "INVALID_TERM" to Res.string.error_term,
        "INVALID_RATE" to Res.string.error_rate,
    )

private val StatusLabels =
    mapOf(
        "ACTIVE" to Res.string.status_ACTIVE,
        "MATURED" to Res.string.status_MATURED,
        "SETTLED" to Res.string.status_SETTLED,
    )
private val PaymentLabels =
    mapOf(
        "AT_MATURITY" to Res.string.pay_AT_MATURITY,
        "MONTHLY" to Res.string.pay_MONTHLY,
        "UPFRONT" to Res.string.pay_UPFRONT,
    )
private val RolloverLabels =
    mapOf(
        "NONE" to Res.string.roll_NONE,
        "PRINCIPAL_ONLY" to Res.string.roll_PRINCIPAL_ONLY,
        "PRINCIPAL_AND_INTEREST" to Res.string.roll_PRINCIPAL_AND_INTEREST,
    )
private val CalcLabels =
    mapOf(
        "SIMPLE_ACTUAL_365" to Res.string.calc_SIMPLE_ACTUAL_365,
        "SIMPLE_30_360" to Res.string.calc_SIMPLE_30_360,
        "COMPOUND_MONTHLY" to Res.string.calc_COMPOUND_MONTHLY,
    )
private val TxLabels =
    mapOf(
        "OPEN" to Res.string.tx_OPEN,
        "ROLLOVER" to Res.string.tx_ROLLOVER,
        "INTEREST_PAID" to Res.string.tx_INTEREST_PAID,
        "SETTLE" to Res.string.tx_SETTLE,
    )

@Composable
private fun label(
    map: Map<String, StringResource>,
    key: String?,
): String = map[key]?.let { stringResource(it) } ?: key.orEmpty()

@Composable
fun DepositListScreen(
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    viewModel: DepositListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf("ACTIVE") }
    RefreshOnResume(viewModel::refresh)
    FinanceScaffold(
        title = stringResource(Res.string.deposits),
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Outlined.Add, stringResource(Res.string.add_deposit)) }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Row(Modifier.padding(horizontal = Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                StatusLabels.forEach { (status, labelRes) ->
                    FilterChip(
                        selected = filter == status,
                        onClick = { filter = status },
                        label = { Text(stringResource(labelRes)) },
                    )
                }
            }
            ResourceContent(
                resource = state,
                onRefresh = viewModel::refresh,
                isEmpty = { it.isEmpty() },
                empty = {
                    EmptyState(
                        stringResource(Res.string.no_deposits),
                        message = stringResource(Res.string.no_deposits_message),
                        actionLabel = stringResource(Res.string.add_deposit),
                        onAction = onAdd,
                    )
                },
            ) { deposits ->
                LazyColumn {
                    items(
                        deposits.filter {
                            it.status == filter
                        },
                        key = { it.id },
                    ) { deposit -> DepositRow(deposit) { onOpen(deposit.id) } }
                }
            }
        }
    }
}

@Composable
private fun DepositRow(
    deposit: DepositDto,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(deposit.institutionName) },
        supportingContent = {
            Text(
                listOfNotNull(
                    "${deposit.annualRate?.toStringExpanded().orEmpty()}%",
                    deposit.maturityDate?.let {
                        "${stringResource(
                            Res.string.maturity_date,
                        )}: ${it.toDisplayString()}"
                    },
                    deposit.accruedInterest?.let {
                        "+${MoneyFormatter.format(
                            deposit.money(it),
                            LocalHideBalances.current,
                        )}"
                    },
                ).joinToString(" · "),
            )
        },
        trailingContent = { MoneyText(deposit.money(deposit.principal)) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun PreviewCard(
    preview: com.financemanage.mobile.feature.deposit.domain.model.DepositPreviewDto,
    currency: com.financemanage.mobile.core.model.CurrencyCode,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.md)) {
            Text(stringResource(Res.string.preview), style = MaterialTheme.typography.titleSmall)
            preview.maturityDate?.let { InfoRow(stringResource(Res.string.maturity_date), it.toDisplayString()) }
            InfoRow(stringResource(Res.string.expected_interest)) {
                MoneyText(
                    com.financemanage.mobile.core.model.Money(
                        preview.expectedInterest ?: com.ionspin.kotlin.bignum.decimal.BigDecimal.ZERO,
                        currency,
                    ),
                )
            }
            InfoRow(stringResource(Res.string.total_at_maturity)) {
                MoneyText(
                    com.financemanage.mobile.core.model.Money(
                        preview.totalAtMaturity ?: com.ionspin.kotlin.bignum.decimal.BigDecimal.ZERO,
                        currency,
                    ),
                )
            }
        }
    }
}

@Composable
fun DepositCreateScreen(
    onCreated: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: DepositFormViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.createdId) { state.createdId?.let(onCreated) }
    FinanceScaffold(title = stringResource(Res.string.add_deposit), onBack = onBack) { padding ->
        FormColumn(padding) {
            FormTextField(
                state.institution,
                { v -> viewModel.update { it.copy(institution = v) } },
                stringResource(Res.string.institution),
                error =
                    if (state.invalid &&
                        state.institution.isBlank()
                    ) {
                        stringResource(CoreRes.string.field_required)
                    } else {
                        null
                    },
            )
            FormTextField(state.accountNumber, { v ->
                viewModel.update {
                    it.copy(accountNumber = v)
                }
            }, stringResource(Res.string.account_number))
            OptionPicker(stringResource(Res.string.type), listOf("TERM", "DEMAND"), state.type, {
                stringResource(if (it == "TERM") Res.string.type_TERM else Res.string.type_DEMAND)
            }, { v -> viewModel.update { it.copy(type = v) } })
            AmountTextField(state.principal, { v ->
                viewModel.update {
                    it.copy(principal = v)
                }
            }, state.currency, stringResource(Res.string.principal))
            OptionPicker(
                stringResource(CoreRes.string.field_currency),
                (CommonCurrencies + state.currency).distinct(),
                state.currency,
                { currencyLabel(it) },
                { v -> viewModel.update { it.copy(currency = v, fromWalletId = null) } },
            )
            FormTextField(state.annualRate, { v ->
                viewModel.update {
                    it.copy(annualRate = v)
                }
            }, stringResource(Res.string.annual_rate), keyboardType = KeyboardType.Decimal)
            FormTextField(state.earlyRate, { v ->
                viewModel.update {
                    it.copy(earlyRate = v)
                }
            }, stringResource(Res.string.early_rate), keyboardType = KeyboardType.Decimal)
            if (state.type == "TERM") {
                FormTextField(state.termMonths, { v ->
                    viewModel.update {
                        it.copy(termMonths = v.filter(Char::isDigit))
                    }
                }, stringResource(Res.string.term_months), keyboardType = KeyboardType.Number)
            }
            DateField(
                stringResource(Res.string.open_date),
                state.openDate,
                { v -> viewModel.update { it.copy(openDate = v) } },
            )
            OptionPicker(
                stringResource(
                    Res.string.interest_payment,
                ),
                PaymentLabels.keys.toList(),
                state.interestPayment,
                {
                    label(PaymentLabels, it)
                },
                { v ->
                    viewModel.update { it.copy(interestPayment = v) }
                },
            )
            OptionPicker(stringResource(Res.string.rollover), RolloverLabels.keys.toList(), state.rollover, {
                label(RolloverLabels, it)
            }, { v ->
                viewModel.update { it.copy(rollover = v) }
            })
            OptionPicker(stringResource(Res.string.calc_method), CalcLabels.keys.toList(), state.calculation, {
                label(CalcLabels, it)
            }, { v ->
                viewModel.update { it.copy(calculation = v) }
            })
            val walletOptions: List<Wallet?> = listOf(null) + state.wallets.filter { it.currency == state.currency }
            OptionPicker(
                stringResource(Res.string.from_wallet),
                walletOptions,
                walletOptions.firstOrNull { it?.id == state.fromWalletId },
                { it?.let(::walletLabel) ?: stringResource(Res.string.no_wallet) },
                { v -> viewModel.update { it.copy(fromWalletId = v?.id) } },
            )
            FormTextField(state.note, { v ->
                viewModel.update {
                    it.copy(note = v)
                }
            }, stringResource(CoreRes.string.field_note), maxLength = 500)
            state.preview?.let { PreviewCard(it, state.currency) }
            PendingBanner(state.pending)
            state.error?.let { Text(it.userMessage(DepositErrors), color = MaterialTheme.colorScheme.error) }
            LoadingButton(
                stringResource(CoreRes.string.action_save),
                onClick = viewModel::save,
                loading = state.saving,
                enabled = LocalIsOnline.current,
            )
        }
    }
}

@Composable
fun DepositDetailScreen(
    id: String,
    onBack: () -> Unit,
    viewModel: DepositDetailViewModel = koinViewModel(key = "deposit-$id") { parametersOf(id) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var settleSheet by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    RefreshOnResume(viewModel::refresh)
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    val deposit = state.deposit
    FinanceScaffold(
        title = deposit?.institutionName.orEmpty(),
        onBack = onBack,
        actions = {
            IconButton(onClick = {
                confirmDelete = true
            }) { Icon(Icons.Outlined.Delete, stringResource(Res.string.delete_deposit)) }
        },
    ) { padding ->
        if (deposit == null) {
            ListSkeleton(Modifier.padding(padding))
            return@FinanceScaffold
        }
        LazyColumn(Modifier.padding(padding)) {
            item { PendingBanner(state.pending) }
            item {
                Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    MoneyText(deposit.money(deposit.principal), style = MaterialTheme.typography.headlineMedium)
                    InfoRow(
                        stringResource(Res.string.annual_rate),
                        "${deposit.annualRate?.toStringExpanded().orEmpty()}%",
                    )
                    deposit.termMonths?.let { InfoRow(stringResource(Res.string.term_months), it.toString()) }
                    deposit.openDate?.let { InfoRow(stringResource(Res.string.open_date), it.toDisplayString()) }
                    deposit.maturityDate?.let {
                        InfoRow(
                            stringResource(Res.string.maturity_date),
                            it.toDisplayString(),
                        )
                    }
                    InfoRow(stringResource(Res.string.accrued_interest)) {
                        MoneyText(deposit.money(deposit.accruedInterest), color = FinanceTheme.colors.income)
                    }
                    InfoRow(
                        stringResource(Res.string.expected_interest),
                    ) { MoneyText(deposit.money(deposit.expectedInterestAtMaturity)) }
                    InfoRow(
                        stringResource(Res.string.interest_payment),
                        label(PaymentLabels, deposit.interestPaymentMethod),
                    )
                    InfoRow(stringResource(Res.string.rollover), label(RolloverLabels, deposit.rolloverPolicy))
                    deposit.fundingStatus?.let { InfoRow(stringResource(Res.string.funding), statusText(it)) }
                    deposit.payoutStatus?.let { InfoRow(stringResource(Res.string.payout), statusText(it)) }
                    state.error?.let { Text(it.userMessage(DepositErrors), color = MaterialTheme.colorScheme.error) }
                    if (deposit.status != "SETTLED") {
                        LoadingButton(stringResource(Res.string.settle), onClick = {
                            viewModel.loadQuote(state.today)
                            settleSheet = true
                        }, loading = state.busy, enabled = LocalIsOnline.current)
                    }
                }
            }
            item { SectionHeader(stringResource(Res.string.history)) }
            items(deposit.transactions, key = { it.id }) { tx ->
                ListItem(
                    headlineContent = { Text(label(TxLabels, tx.type)) },
                    supportingContent = { Text(tx.occurredOn?.toDisplayString().orEmpty()) },
                    trailingContent = { MoneyText(deposit.money(tx.amount)) },
                )
            }
        }
    }
    if (settleSheet && deposit != null) {
        SettleSheet(deposit, state, onDate = viewModel::loadQuote, onSettle = { date, wallet ->
            settleSheet = false
            viewModel.settle(date, wallet)
        }, onDismiss = { settleSheet = false })
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(Res.string.delete_deposit),
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
private fun statusText(status: AsyncStatus): String =
    if (status == AsyncStatus.PENDING) stringResource(CoreRes.string.processing) else status.name

@Composable
private fun SettleSheet(
    deposit: DepositDto,
    state: DepositDetailUiState,
    onDate: (LocalDate) -> Unit,
    onSettle: (LocalDate?, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var date by remember { mutableStateOf(state.today) }
    var walletId by remember { mutableStateOf<String?>(null) }
    val hidden = LocalHideBalances.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            DateField(stringResource(Res.string.settle_date), date, {
                date = it
                onDate(it)
            })
            val wallets: List<Wallet?> = listOf(null) + state.wallets.filter { it.currency == deposit.currencyCode }
            OptionPicker(
                stringResource(Res.string.to_wallet),
                wallets,
                wallets.firstOrNull { it?.id == walletId },
                { it?.let(::walletLabel) ?: stringResource(Res.string.no_wallet) },
                { walletId = it?.id },
            )
            state.quote?.let { quote ->
                Text(
                    stringResource(
                        Res.string.quote_total,
                        MoneyFormatter.format(deposit.money(quote.principal), hidden),
                        MoneyFormatter.format(deposit.money(quote.interest), hidden),
                        MoneyFormatter.format(deposit.money(quote.total), hidden),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (quote.early) {
                    Text(
                        stringResource(
                            Res.string.early_warning,
                            MoneyFormatter.format(deposit.money(quote.interestLostByEarlyWithdrawal), hidden),
                        ),
                        color = FinanceTheme.colors.warning,
                    )
                }
            }
            Button(onClick = {
                onSettle(date, walletId)
            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.settle)) }
        }
    }
}
