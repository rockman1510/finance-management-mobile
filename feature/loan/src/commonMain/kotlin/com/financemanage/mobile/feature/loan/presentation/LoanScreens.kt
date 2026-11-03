package com.financemanage.mobile.feature.loan.presentation

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
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
import com.financemanage.mobile.core.model.CommonCurrencies
import com.financemanage.mobile.core.model.format.MoneyFormatter
import com.financemanage.mobile.core.model.safeDivide
import com.financemanage.mobile.core.model.time.toDisplayString
import com.financemanage.mobile.core.model.toBigDecimalOrNull
import com.financemanage.mobile.core.model.toChartValue
import com.financemanage.mobile.core.ui.components.AmountTextField
import com.financemanage.mobile.core.ui.components.BarChart
import com.financemanage.mobile.core.ui.components.BarGroup
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
import com.financemanage.mobile.core.ui.components.RefreshOnResume
import com.financemanage.mobile.core.ui.components.ResourceContent
import com.financemanage.mobile.core.ui.components.SectionHeader
import com.financemanage.mobile.core.ui.components.currencyLabel
import com.financemanage.mobile.core.ui.components.parseAmountInput
import com.financemanage.mobile.core.ui.error.userMessage
import com.financemanage.mobile.core.ui.resources.action_save
import com.financemanage.mobile.core.ui.resources.delete_confirm_message
import com.financemanage.mobile.core.ui.resources.field_amount
import com.financemanage.mobile.core.ui.resources.field_currency
import com.financemanage.mobile.core.ui.resources.field_date
import com.financemanage.mobile.core.ui.resources.field_invalid_amount
import com.financemanage.mobile.core.ui.resources.field_note
import com.financemanage.mobile.core.ui.theme.FinanceTheme
import com.financemanage.mobile.core.ui.theme.LocalHideBalances
import com.financemanage.mobile.core.ui.theme.Spacing
import com.financemanage.mobile.feature.loan.data.InstallmentDto
import com.financemanage.mobile.feature.loan.data.LoanSummaryDto
import com.financemanage.mobile.feature.loan.resources.Res
import com.financemanage.mobile.feature.loan.resources.active
import com.financemanage.mobile.feature.loan.resources.add_loan
import com.financemanage.mobile.feature.loan.resources.annual_rate
import com.financemanage.mobile.feature.loan.resources.change_rate
import com.financemanage.mobile.feature.loan.resources.counterparty
import com.financemanage.mobile.feature.loan.resources.current_rate
import com.financemanage.mobile.feature.loan.resources.delete_loan
import com.financemanage.mobile.feature.loan.resources.direction
import com.financemanage.mobile.feature.loan.resources.effective_from
import com.financemanage.mobile.feature.loan.resources.error_interest_free
import com.financemanage.mobile.feature.loan.resources.error_not_active
import com.financemanage.mobile.feature.loan.resources.error_overpayment
import com.financemanage.mobile.feature.loan.resources.error_settle_first
import com.financemanage.mobile.feature.loan.resources.fee_estimate
import com.financemanage.mobile.feature.loan.resources.fee_part
import com.financemanage.mobile.feature.loan.resources.i_borrowed
import com.financemanage.mobile.feature.loan.resources.i_lent
import com.financemanage.mobile.feature.loan.resources.inst_OVERDUE
import com.financemanage.mobile.feature.loan.resources.inst_PAID
import com.financemanage.mobile.feature.loan.resources.inst_PARTIAL
import com.financemanage.mobile.feature.loan.resources.inst_PENDING
import com.financemanage.mobile.feature.loan.resources.installments
import com.financemanage.mobile.feature.loan.resources.interest_part
import com.financemanage.mobile.feature.loan.resources.loan_type
import com.financemanage.mobile.feature.loan.resources.loans
import com.financemanage.mobile.feature.loan.resources.method
import com.financemanage.mobile.feature.loan.resources.method_ANNUITY
import com.financemanage.mobile.feature.loan.resources.method_BULLET
import com.financemanage.mobile.feature.loan.resources.method_EQUAL_PRINCIPAL
import com.financemanage.mobile.feature.loan.resources.method_INTEREST_FREE
import com.financemanage.mobile.feature.loan.resources.new_rate
import com.financemanage.mobile.feature.loan.resources.next_due
import com.financemanage.mobile.feature.loan.resources.no_loans
import com.financemanage.mobile.feature.loan.resources.no_loans_message
import com.financemanage.mobile.feature.loan.resources.outstanding
import com.financemanage.mobile.feature.loan.resources.overdue_count
import com.financemanage.mobile.feature.loan.resources.paid_off
import com.financemanage.mobile.feature.loan.resources.period_no
import com.financemanage.mobile.feature.loan.resources.prepay
import com.financemanage.mobile.feature.loan.resources.prepay_fee
import com.financemanage.mobile.feature.loan.resources.preview_schedule
import com.financemanage.mobile.feature.loan.resources.principal
import com.financemanage.mobile.feature.loan.resources.principal_part
import com.financemanage.mobile.feature.loan.resources.rate_periods
import com.financemanage.mobile.feature.loan.resources.repay
import com.financemanage.mobile.feature.loan.resources.repayments
import com.financemanage.mobile.feature.loan.resources.start_date
import com.financemanage.mobile.feature.loan.resources.term_months
import com.financemanage.mobile.feature.loan.resources.total_interest
import com.financemanage.mobile.feature.loan.resources.total_payment
import com.financemanage.mobile.feature.loan.resources.type_INSTALLMENT
import com.financemanage.mobile.feature.loan.resources.type_MORTGAGE
import com.financemanage.mobile.feature.loan.resources.type_PERSONAL
import com.financemanage.mobile.feature.loan.resources.type_UNSECURED
import com.financemanage.mobile.feature.loan.resources.wallet_note
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.financemanage.mobile.core.ui.resources.Res as CoreRes

private val LoanErrors: Map<String, StringResource> =
    mapOf(
        "OVERPAYMENT" to Res.string.error_overpayment,
        "SETTLE_DUE_INSTALLMENTS_FIRST" to Res.string.error_settle_first,
        "LOAN_NOT_ACTIVE" to Res.string.error_not_active,
        "INTEREST_FREE_LOAN" to Res.string.error_interest_free,
    )
private val TypeLabels =
    mapOf(
        "MORTGAGE" to Res.string.type_MORTGAGE,
        "UNSECURED" to Res.string.type_UNSECURED,
        "INSTALLMENT" to Res.string.type_INSTALLMENT,
        "PERSONAL" to Res.string.type_PERSONAL,
    )
private val MethodLabels =
    mapOf(
        "EQUAL_PRINCIPAL" to Res.string.method_EQUAL_PRINCIPAL,
        "ANNUITY" to Res.string.method_ANNUITY,
        "BULLET" to Res.string.method_BULLET,
        "INTEREST_FREE" to Res.string.method_INTEREST_FREE,
    )
private val InstallmentLabels =
    mapOf(
        "PENDING" to Res.string.inst_PENDING,
        "PARTIAL" to Res.string.inst_PARTIAL,
        "PAID" to Res.string.inst_PAID,
        "OVERDUE" to Res.string.inst_OVERDUE,
    )

@Composable
private fun label(
    map: Map<String, StringResource>,
    key: String?,
) = map[key]?.let { stringResource(it) } ?: key.orEmpty()

@Composable
fun LoanListScreen(
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    viewModel: LoanListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var direction by rememberSaveable { mutableStateOf("BORROW") }
    var status by rememberSaveable { mutableStateOf("ACTIVE") }
    RefreshOnResume(viewModel::refresh)
    FinanceScaffold(
        title = stringResource(Res.string.loans),
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Outlined.Add, stringResource(Res.string.add_loan)) }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = if (direction == "BORROW") 0 else 1) {
                Tab(
                    direction == "BORROW",
                    { direction = "BORROW" },
                    text = { Text(stringResource(Res.string.i_borrowed)) },
                )
                Tab(direction == "LEND", { direction = "LEND" }, text = { Text(stringResource(Res.string.i_lent)) })
            }
            Row(Modifier.padding(Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                FilterChip(
                    status == "ACTIVE",
                    { status = "ACTIVE" },
                    label = { Text(stringResource(Res.string.active)) },
                )
                FilterChip(
                    status == "PAID_OFF",
                    { status = "PAID_OFF" },
                    label = { Text(stringResource(Res.string.paid_off)) },
                )
            }
            ResourceContent(
                resource = state,
                onRefresh = viewModel::refresh,
                isEmpty = { it.isEmpty() },
                empty = {
                    EmptyState(
                        stringResource(Res.string.no_loans),
                        message = stringResource(Res.string.no_loans_message),
                        actionLabel = stringResource(Res.string.add_loan),
                        onAction = onAdd,
                    )
                },
            ) { loans ->
                LazyColumn {
                    items(
                        loans.filter {
                            it.direction == direction && it.status == status
                        },
                        key = { it.id },
                    ) { loan -> LoanRow(loan) { onOpen(loan.id) } }
                }
            }
        }
    }
}

@Composable
private fun LoanRow(
    loan: LoanSummaryDto,
    onClick: () -> Unit,
) {
    val hidden = LocalHideBalances.current
    ListItem(
        headlineContent = { Text(loan.counterpartyName) },
        supportingContent = {
            Column {
                loan.nextDueDate?.let {
                    Text(
                        stringResource(
                            Res.string.next_due,
                            it.toDisplayString(),
                            MoneyFormatter.format(loan.money(loan.nextDueAmount), hidden),
                        ),
                    )
                }
                if (loan.overdueInstallments > 0) {
                    Text(
                        stringResource(Res.string.overdue_count, loan.overdueInstallments),
                        color = FinanceTheme.colors.expense,
                    )
                }
            }
        },
        trailingContent = { MoneyText(loan.money(loan.outstandingPrincipal)) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun ScheduleTable(
    installments: List<InstallmentDto>,
    money: (BigDecimal?) -> com.financemanage.mobile.core.model.Money,
) {
    val groups =
        installments.map {
            BarGroup(
                it.periodNo.toString(),
                listOf(
                    (it.principalDue ?: BigDecimal.ZERO).toChartValue(),
                    (
                        it.interestDue
                            ?: BigDecimal.ZERO
                    ).toChartValue(),
                ),
            )
        }
    BarChart(groups, listOf(FinanceTheme.colors.income, FinanceTheme.colors.expense))
    installments.forEach { item ->
        ListItem(
            headlineContent = {
                Text(
                    "${stringResource(
                        Res.string.period_no,
                        item.periodNo,
                    )} · ${item.dueDate?.toDisplayString().orEmpty()}",
                )
            },
            supportingContent = {
                Text(
                    "${stringResource(
                        Res.string.principal_part,
                    )} ${MoneyFormatter.format(money(item.principalDue), LocalHideBalances.current)} · " +
                        "${stringResource(
                            Res.string.interest_part,
                        )} ${MoneyFormatter.format(money(item.interestDue), LocalHideBalances.current)} · " +
                        label(InstallmentLabels, item.status),
                )
            },
            trailingContent = { MoneyText(money(item.totalDue)) },
        )
    }
}

@Composable
fun LoanCreateScreen(
    onCreated: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: LoanFormViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.createdId) { state.createdId?.let(onCreated) }
    FinanceScaffold(title = stringResource(Res.string.add_loan), onBack = onBack) { padding ->
        FormColumn(padding) {
            FormTextField(state.counterparty, { v ->
                viewModel.update {
                    it.copy(counterparty = v)
                }
            }, stringResource(Res.string.counterparty))
            OptionPicker(stringResource(Res.string.direction), listOf("BORROW", "LEND"), state.direction, {
                stringResource(if (it == "BORROW") Res.string.i_borrowed else Res.string.i_lent)
            }, { v -> viewModel.update { it.copy(direction = v) } })
            OptionPicker(stringResource(Res.string.loan_type), TypeLabels.keys.toList(), state.type, {
                label(TypeLabels, it)
            }, { v -> viewModel.update { it.copy(type = v) } })
            AmountTextField(state.principal, { v ->
                viewModel.update {
                    it.copy(principal = v)
                }
            }, state.currency, stringResource(Res.string.principal))
            OptionPicker(
                stringResource(
                    CoreRes.string.field_currency,
                ),
                (CommonCurrencies + state.currency).distinct(),
                state.currency,
                {
                    currencyLabel(it)
                },
                { v ->
                    viewModel.update { it.copy(currency = v) }
                },
            )
            OptionPicker(stringResource(Res.string.method), MethodLabels.keys.toList(), state.method, {
                label(MethodLabels, it)
            }, { v -> viewModel.update { it.copy(method = v) } })
            if (state.method != "INTEREST_FREE") {
                FormTextField(state.rate, { v ->
                    viewModel.update {
                        it.copy(rate = v)
                    }
                }, stringResource(Res.string.annual_rate), keyboardType = KeyboardType.Decimal)
            }
            FormTextField(state.termMonths, { v ->
                viewModel.update {
                    it.copy(termMonths = v.filter(Char::isDigit))
                }
            }, stringResource(Res.string.term_months), keyboardType = KeyboardType.Number)
            DateField(
                stringResource(Res.string.start_date),
                state.startDate,
                { v -> viewModel.update { it.copy(startDate = v) } },
            )
            FormTextField(state.feeRate, { v ->
                viewModel.update {
                    it.copy(feeRate = v)
                }
            }, stringResource(Res.string.prepay_fee), keyboardType = KeyboardType.Decimal)
            FormTextField(state.note, { v ->
                viewModel.update {
                    it.copy(note = v)
                }
            }, stringResource(CoreRes.string.field_note), maxLength = 500)
            Text(stringResource(Res.string.wallet_note), style = MaterialTheme.typography.bodySmall)
            if (state.invalid) {
                Text(
                    stringResource(CoreRes.string.field_invalid_amount),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            state.error?.let { Text(it.userMessage(LoanErrors), color = MaterialTheme.colorScheme.error) }
            OutlinedButton(onClick = viewModel::preview, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.preview_schedule))
            }
            state.preview?.let { preview ->
                val currency = state.currency
                val money = { value: BigDecimal? ->
                    com.financemanage.mobile.core.model
                        .Money(value ?: BigDecimal.ZERO, currency)
                }
                InfoRow(stringResource(Res.string.total_interest)) { MoneyText(money(preview.totalInterest)) }
                InfoRow(stringResource(Res.string.total_payment)) { MoneyText(money(preview.totalPayment)) }
                ScheduleTable(preview.installments, money)
            }
            LoadingButton(
                stringResource(CoreRes.string.action_save),
                onClick = viewModel::save,
                loading = state.loading,
            )
        }
    }
}

@Composable
fun LoanDetailScreen(
    id: String,
    onBack: () -> Unit,
    viewModel: LoanDetailViewModel = koinViewModel(key = "loan-$id") { parametersOf(id) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var action by remember { mutableStateOf<LoanAction?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    RefreshOnResume(viewModel::refresh)
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    val loan = state.loan
    FinanceScaffold(
        title = loan?.summary?.counterpartyName.orEmpty(),
        onBack = onBack,
        actions = {
            IconButton(onClick = {
                confirmDelete = true
            }) { Icon(Icons.Outlined.Delete, stringResource(Res.string.delete_loan)) }
        },
    ) { padding ->
        if (loan == null) {
            ListSkeleton(Modifier.padding(padding))
            return@FinanceScaffold
        }
        val summary = loan.summary
        LazyColumn(Modifier.padding(padding)) {
            item {
                Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(stringResource(Res.string.outstanding), style = MaterialTheme.typography.labelLarge)
                    MoneyText(
                        summary.money(summary.outstandingPrincipal),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    InfoRow(
                        stringResource(Res.string.current_rate),
                        "${summary.currentRate?.toStringExpanded().orEmpty()}%",
                    )
                    InfoRow(stringResource(Res.string.method), label(MethodLabels, summary.repaymentMethod))
                    InfoRow(
                        stringResource(Res.string.total_interest),
                    ) { MoneyText(summary.money(loan.totalScheduledInterest)) }
                    Text(stringResource(Res.string.wallet_note), style = MaterialTheme.typography.bodySmall)
                    state.error?.let { Text(it.userMessage(LoanErrors), color = MaterialTheme.colorScheme.error) }
                    if (summary.status == "ACTIVE") {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Button(onClick = {
                                viewModel.newAction()
                                action = LoanAction.REPAY
                            }, enabled = !state.busy) { Text(stringResource(Res.string.repay)) }
                            OutlinedButton(onClick = {
                                viewModel.newAction()
                                action = LoanAction.PREPAY
                            }, enabled = !state.busy) { Text(stringResource(Res.string.prepay)) }
                        }
                        if (summary.repaymentMethod != "INTEREST_FREE") {
                            OutlinedButton(onClick = {
                                action = LoanAction.RATE
                            }, enabled = !state.busy) { Text(stringResource(Res.string.change_rate)) }
                        }
                    }
                }
            }
            item { SectionHeader(stringResource(Res.string.installments)) }
            item { Column { ScheduleTable(loan.installments) { summary.money(it) } } }
            item { SectionHeader(stringResource(Res.string.repayments)) }
            items(loan.repayments, key = { it.id }) { repayment ->
                val hidden = LocalHideBalances.current
                ListItem(
                    headlineContent = { Text(repayment.paidOn?.toDisplayString().orEmpty()) },
                    supportingContent = {
                        Text(
                            "${stringResource(
                                Res.string.principal_part,
                            )} ${MoneyFormatter.format(summary.money(repayment.principalPortion), hidden)} · " +
                                "${stringResource(
                                    Res.string.interest_part,
                                )} ${MoneyFormatter.format(summary.money(repayment.interestPortion), hidden)} · " +
                                "${stringResource(
                                    Res.string.fee_part,
                                )} ${MoneyFormatter.format(summary.money(repayment.feePortion), hidden)}",
                        )
                    },
                    trailingContent = { MoneyText(summary.money(repayment.amount)) },
                )
            }
            item { SectionHeader(stringResource(Res.string.rate_periods)) }
            items(loan.ratePeriods) { period ->
                ListItem(headlineContent = {
                    Text("${period.annualRate?.toStringExpanded().orEmpty()}%")
                }, supportingContent = { Text(period.effectiveFrom?.toDisplayString().orEmpty()) })
            }
        }
    }
    val currentAction = action
    if (currentAction != null && loan != null) {
        ActionSheet(currentAction, loan, state.today, onConfirm = { amount, date ->
            action = null
            viewModel.perform(currentAction, amount, date)
        }, onDismiss = { action = null })
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(Res.string.delete_loan),
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
private fun ActionSheet(
    action: LoanAction,
    loan: com.financemanage.mobile.feature.loan.data.LoanDetailDto,
    today: LocalDate?,
    onConfirm: (BigDecimal, LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    val summary = loan.summary
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(today) }
    var invalid by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            val title =
                when (action) {
                    LoanAction.REPAY -> Res.string.repay
                    LoanAction.PREPAY -> Res.string.prepay
                    LoanAction.RATE -> Res.string.change_rate
                }
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            if (action == LoanAction.RATE) {
                FormTextField(
                    amount,
                    { amount = it },
                    stringResource(Res.string.new_rate),
                    keyboardType = KeyboardType.Decimal,
                )
                DateField(stringResource(Res.string.effective_from), date, { date = it })
            } else {
                AmountTextField(
                    amount,
                    {
                        amount = it
                        invalid = false
                    },
                    summary.currencyCode,
                    stringResource(
                        CoreRes.string.field_amount,
                    ),
                    error = if (invalid) stringResource(CoreRes.string.field_invalid_amount) else null,
                )
                DateField(stringResource(CoreRes.string.field_date), date, { date = it })
                if (action == LoanAction.PREPAY) {
                    val fee =
                        parseAmountInput(amount, summary.currencyCode.fractionDigits)?.let { principal ->
                            loan.earlyRepaymentFeeRate?.let { rate ->
                                (principal * rate).safeDivide(BigDecimal.fromInt(PERCENT))
                            }
                        }
                    fee?.let {
                        Text(
                            stringResource(
                                Res.string.fee_estimate,
                                MoneyFormatter.format(summary.money(it), LocalHideBalances.current),
                            ),
                        )
                    }
                }
            }
            Button(onClick = {
                val value =
                    if (action == LoanAction.RATE) {
                        amount.replace(",", ".").toBigDecimalOrNull()
                    } else {
                        parseAmountInput(amount, summary.currencyCode.fractionDigits)
                    }
                if (value == null || value.signum() <= 0) invalid = true else onConfirm(value, date)
            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(title)) }
        }
    }
}

private const val PERCENT = 100
