package com.financemanage.mobile.feature.bill.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financemanage.mobile.core.model.AsyncStatus
import com.financemanage.mobile.core.model.domain.CategoryType
import com.financemanage.mobile.core.model.domain.Frequency
import com.financemanage.mobile.core.model.time.toDisplayString
import com.financemanage.mobile.core.model.toPlainString
import com.financemanage.mobile.core.ui.components.AmountTextField
import com.financemanage.mobile.core.ui.components.CategoryField
import com.financemanage.mobile.core.ui.components.CategoryIcon
import com.financemanage.mobile.core.ui.components.CategoryPickerSheet
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
import com.financemanage.mobile.core.ui.components.parseAmountInput
import com.financemanage.mobile.core.ui.components.walletLabel
import com.financemanage.mobile.core.ui.error.userMessage
import com.financemanage.mobile.core.ui.resources.action_save
import com.financemanage.mobile.core.ui.resources.delete_confirm_message
import com.financemanage.mobile.core.ui.resources.field_amount
import com.financemanage.mobile.core.ui.resources.field_category
import com.financemanage.mobile.core.ui.resources.field_date
import com.financemanage.mobile.core.ui.resources.field_invalid_amount
import com.financemanage.mobile.core.ui.resources.field_name
import com.financemanage.mobile.core.ui.resources.field_note
import com.financemanage.mobile.core.ui.resources.field_required
import com.financemanage.mobile.core.ui.resources.field_wallet
import com.financemanage.mobile.core.ui.theme.FinanceTheme
import com.financemanage.mobile.core.ui.theme.LocalIsOnline
import com.financemanage.mobile.core.ui.theme.Spacing
import com.financemanage.mobile.feature.bill.data.Bill
import com.financemanage.mobile.feature.bill.resources.Res
import com.financemanage.mobile.feature.bill.resources.add_bill
import com.financemanage.mobile.feature.bill.resources.days_left
import com.financemanage.mobile.feature.bill.resources.days_overdue
import com.financemanage.mobile.feature.bill.resources.delete_bill
import com.financemanage.mobile.feature.bill.resources.due_today
import com.financemanage.mobile.feature.bill.resources.edit_bill
import com.financemanage.mobile.feature.bill.resources.end_date
import com.financemanage.mobile.feature.bill.resources.error_bill_finished
import com.financemanage.mobile.feature.bill.resources.error_choose_category
import com.financemanage.mobile.feature.bill.resources.error_choose_wallet
import com.financemanage.mobile.feature.bill.resources.first_due_date
import com.financemanage.mobile.feature.bill.resources.freq_MONTHLY
import com.financemanage.mobile.feature.bill.resources.freq_QUARTERLY
import com.financemanage.mobile.feature.bill.resources.freq_WEEKLY
import com.financemanage.mobile.feature.bill.resources.freq_YEARLY
import com.financemanage.mobile.feature.bill.resources.frequency
import com.financemanage.mobile.feature.bill.resources.group_due_soon
import com.financemanage.mobile.feature.bill.resources.group_later
import com.financemanage.mobile.feature.bill.resources.group_overdue
import com.financemanage.mobile.feature.bill.resources.group_paused
import com.financemanage.mobile.feature.bill.resources.history
import com.financemanage.mobile.feature.bill.resources.interval
import com.financemanage.mobile.feature.bill.resources.next_due
import com.financemanage.mobile.feature.bill.resources.no_bills
import com.financemanage.mobile.feature.bill.resources.no_bills_message
import com.financemanage.mobile.feature.bill.resources.pause
import com.financemanage.mobile.feature.bill.resources.pay
import com.financemanage.mobile.feature.bill.resources.pay_now
import com.financemanage.mobile.feature.bill.resources.remind_days
import com.financemanage.mobile.feature.bill.resources.resume
import com.financemanage.mobile.feature.bill.resources.skip_period
import com.financemanage.mobile.feature.bill.resources.status_FAILED
import com.financemanage.mobile.feature.bill.resources.status_PENDING
import com.financemanage.mobile.feature.bill.resources.status_RECORDED
import com.financemanage.mobile.feature.bill.resources.status_SKIPPED
import com.financemanage.mobile.feature.bill.resources.template
import com.financemanage.mobile.feature.bill.resources.template_electricity
import com.financemanage.mobile.feature.bill.resources.template_internet
import com.financemanage.mobile.feature.bill.resources.template_phone
import com.financemanage.mobile.feature.bill.resources.template_rent
import com.financemanage.mobile.feature.bill.resources.template_water
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.financemanage.mobile.core.ui.resources.Res as CoreRes

private val BillErrors: Map<String, StringResource> = mapOf("BILL_FINISHED" to Res.string.error_bill_finished)
private val BillFrequencies = listOf(Frequency.WEEKLY, Frequency.MONTHLY, Frequency.QUARTERLY, Frequency.YEARLY)

@Composable
private fun frequencyLabel(frequency: Frequency): String =
    stringResource(
        when (frequency) {
            Frequency.WEEKLY -> Res.string.freq_WEEKLY
            Frequency.QUARTERLY -> Res.string.freq_QUARTERLY
            Frequency.YEARLY -> Res.string.freq_YEARLY
            else -> Res.string.freq_MONTHLY
        },
    )

@Composable
private fun dueText(bill: Bill): String {
    val days = bill.daysUntilDue ?: return ""
    return when {
        days < 0 -> stringResource(Res.string.days_overdue, (-days).toInt())
        days == 0L -> stringResource(Res.string.due_today)
        else -> stringResource(Res.string.days_left, days.toInt())
    }
}

@Composable
private fun dueColor(bill: Bill): Color =
    when {
        bill.overdue -> FinanceTheme.colors.expense
        bill.dueSoon -> FinanceTheme.colors.warning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

/** Bills tab of the Plan screen (F14): Overdue / Due soon / Later, with a quick Pay button. */
@Composable
fun BillsTab(
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    viewModel: BillListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RefreshOnResume(viewModel::refresh)
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            PendingBanner(state.pending)
            state.error?.let {
                Text(
                    it.userMessage(BillErrors),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(Spacing.md),
                )
            }
            ResourceContent(
                resource = state.bills,
                onRefresh = viewModel::refresh,
                isEmpty = { it.isEmpty() },
                empty = {
                    EmptyState(
                        title = stringResource(Res.string.no_bills),
                        message = stringResource(Res.string.no_bills_message),
                        actionLabel = stringResource(Res.string.add_bill),
                        onAction = onAdd,
                    )
                },
            ) { bills ->
                val active = bills.filter { it.active }
                val groups =
                    listOf(
                        Res.string.group_overdue to active.filter { it.overdue },
                        Res.string.group_due_soon to active.filter { it.dueSoon },
                        Res.string.group_later to active.filter { !it.overdue && !it.dueSoon },
                        Res.string.group_paused to bills.filter { !it.active },
                    )
                LazyColumn {
                    groups.filter { it.second.isNotEmpty() }.forEach { (title, items) ->
                        item(key = title.key) { SectionHeader(stringResource(title)) }
                        items(items.sortedBy { it.nextDueDate }, key = { it.id }) { bill ->
                            ListItem(
                                headlineContent = { Text(bill.name) },
                                supportingContent = { Text(dueText(bill), color = dueColor(bill)) },
                                trailingContent = {
                                    Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                        MoneyText(bill.amount)
                                        if (bill.active) {
                                            Button(
                                                onClick = { viewModel.pay(bill) },
                                                enabled = state.payingId == null && LocalIsOnline.current,
                                            ) { Text(stringResource(Res.string.pay)) }
                                        }
                                    }
                                },
                                modifier = Modifier.clickable { onOpen(bill.id) },
                            )
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd).padding(Spacing.md),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = stringResource(Res.string.add_bill))
        }
    }
}

@Composable
private fun paymentStatusLabel(status: AsyncStatus): String =
    stringResource(
        when (status) {
            AsyncStatus.PENDING -> Res.string.status_PENDING
            AsyncStatus.FAILED -> Res.string.status_FAILED
            AsyncStatus.SKIPPED -> Res.string.status_SKIPPED
            else -> Res.string.status_RECORDED
        },
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailScreen(
    id: String,
    onEdit: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: BillDetailViewModel = koinViewModel(key = "bill-$id") { parametersOf(id) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var paySheet by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    RefreshOnResume(viewModel::refresh)
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    val bill = state.bill
    FinanceScaffold(
        title = bill?.name.orEmpty(),
        onBack = onBack,
        actions = {
            IconButton(
                onClick = onEdit,
            ) { Icon(Icons.Outlined.Edit, contentDescription = stringResource(Res.string.edit_bill)) }
            IconButton(onClick = {
                confirmDelete = true
            }) { Icon(Icons.Outlined.Delete, contentDescription = stringResource(Res.string.delete_bill)) }
        },
    ) { padding ->
        if (bill == null) {
            ListSkeleton(Modifier.padding(padding))
            return@FinanceScaffold
        }
        LazyColumn(Modifier.padding(padding)) {
            item { PendingBanner(state.pending) }
            item {
                Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    MoneyText(bill.amount, style = MaterialTheme.typography.headlineMedium)
                    InfoRow(stringResource(Res.string.next_due), bill.nextDueDate?.toDisplayString().orEmpty())
                    Text(dueText(bill), color = dueColor(bill))
                    InfoRow(stringResource(Res.string.frequency), frequencyLabel(bill.frequency))
                    InfoRow(
                        stringResource(CoreRes.string.field_wallet),
                        state.wallets
                            .firstOrNull {
                                it.id ==
                                    bill.walletId
                            }?.name
                            .orEmpty(),
                    )
                    InfoRow(
                        stringResource(CoreRes.string.field_category),
                        state.categories
                            .firstOrNull {
                                it.id ==
                                    bill.categoryId
                            }?.name
                            .orEmpty(),
                    )
                    bill.note?.takeIf { it.isNotBlank() }?.let {
                        InfoRow(
                            stringResource(CoreRes.string.field_note),
                            it,
                        )
                    }
                    state.error?.let { Text(it.userMessage(BillErrors), color = MaterialTheme.colorScheme.error) }
                    if (bill.active) {
                        LoadingButton(
                            stringResource(Res.string.pay_now),
                            onClick = { paySheet = true },
                            loading = state.busy,
                            enabled = LocalIsOnline.current,
                        )
                        OutlinedButton(
                            onClick = viewModel::skip,
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(Res.string.skip_period))
                        }
                    }
                    OutlinedButton(onClick = {
                        viewModel.setActive(!bill.active)
                    }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(if (bill.active) Res.string.pause else Res.string.resume))
                    }
                }
            }
            item { SectionHeader(stringResource(Res.string.history)) }
            items(bill.payments, key = { it.id }) { payment ->
                ListItem(
                    headlineContent = { Text(payment.dueDate?.toDisplayString().orEmpty()) },
                    supportingContent = {
                        Text(
                            listOfNotNull(
                                paymentStatusLabel(payment.status),
                                payment.paidOn?.toDisplayString(),
                                payment.failureReason,
                            ).joinToString(" · "),
                        )
                    },
                    trailingContent = { MoneyText(payment.amount) },
                    modifier =
                        Modifier.clickable(enabled = payment.transactionId != null) {
                            payment.transactionId?.let(onOpenTransaction)
                        },
                )
            }
        }
    }
    if (paySheet && bill != null) {
        PaySheet(
            bill = bill,
            wallets = state.wallets.filter { !it.archived && it.currency == bill.amount.currency },
            onPay = { amount, walletId, date ->
                paySheet = false
                viewModel.pay(amount, walletId, date)
            },
            onDismiss = { paySheet = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(Res.string.delete_bill),
            message = stringResource(CoreRes.string.delete_confirm_message),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaySheet(
    bill: Bill,
    wallets: List<com.financemanage.mobile.core.model.domain.Wallet>,
    onPay: (com.ionspin.kotlin.bignum.decimal.BigDecimal?, String?, LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    val digits = bill.amount.currency.fractionDigits
    var amount by remember { mutableStateOf(bill.amount.amount.toPlainString(digits)) }
    var walletId by remember { mutableStateOf(bill.walletId) }
    var date by remember { mutableStateOf<LocalDate?>(null) }
    var invalid by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AmountTextField(
                amount,
                {
                    amount = it
                    invalid = false
                },
                bill.amount.currency,
                stringResource(CoreRes.string.field_amount),
                error = if (invalid) stringResource(CoreRes.string.field_invalid_amount) else null,
            )
            OptionPicker(
                stringResource(CoreRes.string.field_wallet),
                wallets,
                wallets.firstOrNull { it.id == walletId },
                { walletLabel(it) },
                { walletId = it.id },
            )
            DateField(stringResource(CoreRes.string.field_date), date, { date = it })
            Button(
                onClick = {
                    val parsed = parseAmountInput(amount, digits)
                    if (parsed == null || parsed.signum() <= 0) invalid = true else onPay(parsed, walletId, date)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(Res.string.pay)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BillFormScreen(
    id: String?,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: BillFormViewModel = koinViewModel(key = "bill-form-$id") { parametersOf(id) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pickCategory by remember { mutableStateOf(false) }
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    val templates =
        listOf(
            BillTemplate.ELECTRICITY to Res.string.template_electricity,
            BillTemplate.WATER to Res.string.template_water,
            BillTemplate.INTERNET to Res.string.template_internet,
            BillTemplate.PHONE to Res.string.template_phone,
            BillTemplate.RENT to Res.string.template_rent,
        )
    FinanceScaffold(
        title = stringResource(if (state.isEdit) Res.string.edit_bill else Res.string.add_bill),
        onBack = onBack,
    ) { padding ->
        FormColumn(padding) {
            if (!state.isEdit) {
                Text(stringResource(Res.string.template), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    templates.forEach { (template, label) ->
                        val name = stringResource(label)
                        val category = state.categories.firstOrNull { it.code == template.categoryCode }
                        AssistChip(
                            onClick = { viewModel.applyTemplate(template, name) },
                            label = { Text(name) },
                            leadingIcon = { CategoryIcon(category?.code ?: template.categoryCode, null, size = 20.dp) },
                        )
                    }
                }
            }
            FormTextField(
                state.name,
                viewModel::onName,
                stringResource(CoreRes.string.field_name),
                maxLength = 100,
                error = if (BillProblem.NAME in state.problems) stringResource(CoreRes.string.field_required) else null,
            )
            OptionPicker(
                stringResource(CoreRes.string.field_wallet),
                state.wallets,
                state.wallet,
                { walletLabel(it) },
                viewModel::onWallet,
                error =
                    if (BillProblem.WALLET in
                        state.problems
                    ) {
                        stringResource(Res.string.error_choose_wallet)
                    } else {
                        null
                    },
            )
            state.wallet?.let { wallet ->
                AmountTextField(
                    state.amount,
                    viewModel::onAmount,
                    wallet.currency,
                    stringResource(CoreRes.string.field_amount),
                    error =
                        if (BillProblem.AMOUNT in
                            state.problems
                        ) {
                            stringResource(CoreRes.string.field_invalid_amount)
                        } else {
                            null
                        },
                )
            }
            CategoryField(
                stringResource(CoreRes.string.field_category),
                state.categories.firstOrNull { it.id == state.categoryId },
                state.categories,
                onClick = { pickCategory = true },
                error =
                    if (BillProblem.CATEGORY in
                        state.problems
                    ) {
                        stringResource(Res.string.error_choose_category)
                    } else {
                        null
                    },
            )
            OptionPicker(stringResource(Res.string.frequency), BillFrequencies, state.frequency, {
                frequencyLabel(it)
            }, viewModel::onFrequency, enabled = !state.isEdit)
            if (!state.isEdit) {
                FormTextField(
                    state.interval,
                    viewModel::onInterval,
                    stringResource(Res.string.interval),
                    keyboardType = KeyboardType.Number,
                )
                DateField(stringResource(Res.string.first_due_date), state.firstDueDate, viewModel::onFirstDue)
            }
            DateField(stringResource(Res.string.end_date), state.endDate, viewModel::onEnd)
            FormTextField(
                state.remindDays,
                viewModel::onRemind,
                stringResource(Res.string.remind_days),
                keyboardType = KeyboardType.Number,
            )
            FormTextField(state.note, viewModel::onNote, stringResource(CoreRes.string.field_note), maxLength = 500)
            state.error?.let { Text(it.userMessage(BillErrors), color = MaterialTheme.colorScheme.error) }
            LoadingButton(stringResource(CoreRes.string.action_save), onClick = viewModel::save, loading = state.saving)
        }
    }
    if (pickCategory) {
        CategoryPickerSheet(
            categories = state.categories,
            type = CategoryType.EXPENSE,
            recentIds = emptyList(),
            onSelect = {
                pickCategory = false
                viewModel.onCategory(it)
            },
            onDismiss = { pickCategory = false },
        )
    }
}
