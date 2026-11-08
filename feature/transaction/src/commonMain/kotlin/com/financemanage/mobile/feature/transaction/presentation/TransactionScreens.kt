package com.financemanage.mobile.feature.transaction.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financemanage.mobile.core.model.domain.CategoryType
import com.financemanage.mobile.core.model.time.toDisplayString
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
import com.financemanage.mobile.core.ui.components.RefreshOnResume
import com.financemanage.mobile.core.ui.components.ResourceContent
import com.financemanage.mobile.core.ui.components.SwitchRow
import com.financemanage.mobile.core.ui.components.walletLabel
import com.financemanage.mobile.core.ui.error.userMessage
import com.financemanage.mobile.core.ui.resources.action_delete
import com.financemanage.mobile.core.ui.resources.action_edit
import com.financemanage.mobile.core.ui.resources.action_save
import com.financemanage.mobile.core.ui.resources.delete_confirm_message
import com.financemanage.mobile.core.ui.resources.field_amount
import com.financemanage.mobile.core.ui.resources.field_category
import com.financemanage.mobile.core.ui.resources.field_date
import com.financemanage.mobile.core.ui.resources.field_invalid_amount
import com.financemanage.mobile.core.ui.resources.field_note
import com.financemanage.mobile.core.ui.resources.field_wallet
import com.financemanage.mobile.core.ui.theme.Spacing
import com.financemanage.mobile.feature.transaction.domain.model.Attachment
import com.financemanage.mobile.feature.transaction.resources.Res
import com.financemanage.mobile.feature.transaction.resources.active
import com.financemanage.mobile.feature.transaction.resources.amount_received
import com.financemanage.mobile.feature.transaction.resources.amount_sent
import com.financemanage.mobile.feature.transaction.resources.delete_recurring
import com.financemanage.mobile.feature.transaction.resources.delete_transaction
import com.financemanage.mobile.feature.transaction.resources.delete_transfer
import com.financemanage.mobile.feature.transaction.resources.discard
import com.financemanage.mobile.feature.transaction.resources.edit_recurring
import com.financemanage.mobile.feature.transaction.resources.end_date
import com.financemanage.mobile.feature.transaction.resources.error_same_wallet
import com.financemanage.mobile.feature.transaction.resources.estimated_rate
import com.financemanage.mobile.feature.transaction.resources.every_n
import com.financemanage.mobile.feature.transaction.resources.from_wallet
import com.financemanage.mobile.feature.transaction.resources.generated
import com.financemanage.mobile.feature.transaction.resources.next_run
import com.financemanage.mobile.feature.transaction.resources.no_recurring
import com.financemanage.mobile.feature.transaction.resources.outbox_empty
import com.financemanage.mobile.feature.transaction.resources.outbox_title
import com.financemanage.mobile.feature.transaction.resources.paused
import com.financemanage.mobile.feature.transaction.resources.receipts
import com.financemanage.mobile.feature.transaction.resources.recurring_title
import com.financemanage.mobile.feature.transaction.resources.retry
import com.financemanage.mobile.feature.transaction.resources.send_failed
import com.financemanage.mobile.feature.transaction.resources.source
import com.financemanage.mobile.feature.transaction.resources.to_wallet
import com.financemanage.mobile.feature.transaction.resources.transaction_detail
import com.financemanage.mobile.feature.transaction.resources.transfer_title
import com.financemanage.mobile.feature.transaction.resources.type
import com.financemanage.mobile.feature.transaction.resources.waiting_to_send
import com.financemanage.mobile.feature.transaction.resources.with_person
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.financemanage.mobile.core.ui.resources.Res as CoreRes

private const val MAX_RECEIPTS = 5

@Composable
fun TransactionDetailScreen(
    transactionId: String,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    viewModel: TransactionDetailViewModel =
        koinViewModel(key = "tx-detail-$transactionId") { parametersOf(transactionId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    var deleteAttachment by remember { mutableStateOf<Attachment?>(null) }
    var viewing by remember { mutableStateOf<ImageBitmap?>(null) }
    RefreshOnResume(viewModel::refresh)
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    val transaction = state.transaction
    FinanceScaffold(
        title = stringResource(Res.string.transaction_detail),
        onBack = onBack,
        actions = {
            if (transaction?.editable == true) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = stringResource(CoreRes.string.action_edit))
                }
            }
            if (transaction != null && (transaction.editable || transaction.transferId != null)) {
                IconButton(
                    onClick = { confirmDelete = true },
                ) { Icon(Icons.Outlined.Delete, contentDescription = stringResource(CoreRes.string.action_delete)) }
            }
        },
    ) { padding ->
        if (transaction == null) {
            val error = state.error
            if (error != null) {
                com.financemanage.mobile.core.ui.components.ErrorState(
                    error.userMessage(TransactionErrors),
                    viewModel::refresh,
                )
            } else {
                ListSkeleton(Modifier.padding(padding))
            }
            return@FinanceScaffold
        }
        val category = state.categories.firstOrNull { it.id == transaction.categoryId }
        FormColumn(padding) {
            ListItem(
                headlineContent = {
                    Text(
                        category?.name ?: typeLabel(transaction.type),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                leadingContent = { CategoryIcon(category?.code, category?.icon) },
                trailingContent = {
                    MoneyText(
                        transaction.signedAmount,
                        style = MaterialTheme.typography.titleLarge,
                        colorBySign = true,
                        showPlusSign = true,
                    )
                },
            )
            InfoRow(stringResource(Res.string.type), typeLabel(transaction.type))
            InfoRow(
                stringResource(CoreRes.string.field_wallet),
                state.wallets
                    .firstOrNull {
                        it.id ==
                            transaction.walletId
                    }?.name
                    .orEmpty(),
            )
            transaction.occurredOn?.let { InfoRow(stringResource(CoreRes.string.field_date), it.toDisplayString()) }
            transaction.note?.takeIf { it.isNotBlank() }?.let { InfoRow(stringResource(CoreRes.string.field_note), it) }
            transaction.withPerson?.takeIf { it.isNotBlank() }?.let {
                InfoRow(
                    stringResource(Res.string.with_person),
                    it,
                )
            }
            sourceLabel(transaction)?.let { InfoRow(stringResource(Res.string.source), it) }
            Text(stringResource(Res.string.receipts), style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(transaction.attachments, key = { it.id }) { attachment ->
                    val bitmap = rememberImageBitmap(state.images[attachment.id])
                    ReceiptThumbnail(
                        image = bitmap,
                        isPdf = !attachment.isImage,
                        onClick = { bitmap?.let { viewing = it } },
                        onRemove = if (transaction.editable) ({ deleteAttachment = attachment }) else null,
                    )
                }
                if (transaction.editable && transaction.attachments.size < MAX_RECEIPTS) {
                    item { AddReceiptButton(enabled = !state.uploading, onPicked = viewModel::upload) }
                }
            }
            state.error?.let { Text(it.userMessage(TransactionErrors), color = MaterialTheme.colorScheme.error) }
        }
    }
    if (confirmDelete && transaction != null) {
        ConfirmDialog(
            title =
                stringResource(
                    if (transaction.transferId !=
                        null
                    ) {
                        Res.string.delete_transfer
                    } else {
                        Res.string.delete_transaction
                    },
                ),
            message = stringResource(CoreRes.string.delete_confirm_message),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
    deleteAttachment?.let { attachment ->
        ConfirmDialog(
            title = attachment.fileName,
            message = stringResource(CoreRes.string.delete_confirm_message),
            onConfirm = {
                deleteAttachment = null
                viewModel.deleteAttachment(attachment)
            },
            onDismiss = { deleteAttachment = null },
        )
    }
    viewing?.let { ImageViewer(it) { viewing = null } }
}

@Composable
fun TransferScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: TransferViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    FinanceScaffold(title = stringResource(Res.string.transfer_title), onBack = onBack) { padding ->
        FormColumn(padding) {
            OptionPicker(
                stringResource(Res.string.from_wallet),
                state.wallets,
                state.from,
                { walletLabel(it) },
                viewModel::onFrom,
            )
            OptionPicker(
                stringResource(Res.string.to_wallet),
                state.wallets,
                state.to,
                { walletLabel(it) },
                viewModel::onTo,
                error = if (state.sameWallet) stringResource(Res.string.error_same_wallet) else null,
            )
            val invalid = if (state.invalidAmount) stringResource(CoreRes.string.field_invalid_amount) else null
            state.from?.let { from ->
                AmountTextField(
                    state.amount,
                    viewModel::onAmount,
                    from.currency,
                    stringResource(Res.string.amount_sent),
                    error = invalid,
                )
            }
            val to = state.to
            if (state.crossCurrency && to != null) {
                state.estimatedRate?.let { rate ->
                    Text(
                        stringResource(
                            Res.string.estimated_rate,
                            state.from
                                ?.currency
                                ?.value
                                .orEmpty(),
                            rate.toStringExpanded(),
                            to.currency.value,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                AmountTextField(
                    state.received,
                    viewModel::onReceived,
                    to.currency,
                    stringResource(Res.string.amount_received),
                )
            }
            DateField(stringResource(CoreRes.string.field_date), state.date, viewModel::onDate)
            FormTextField(state.note, viewModel::onNote, stringResource(CoreRes.string.field_note), maxLength = 500)
            state.error?.let { Text(it.userMessage(TransactionErrors), color = MaterialTheme.colorScheme.error) }
            LoadingButton(stringResource(CoreRes.string.action_save), onClick = viewModel::save, loading = state.saving)
        }
    }
}

@Composable
fun RecurringListScreen(
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: RecurringListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RefreshOnResume(viewModel::refresh)
    FinanceScaffold(title = stringResource(Res.string.recurring_title), onBack = onBack) { padding ->
        ResourceContent(
            resource = state.items,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding),
            isEmpty = { it.isEmpty() },
            empty = { EmptyState(title = stringResource(Res.string.no_recurring)) },
        ) { items ->
            LazyColumn {
                items(items, key = { it.id }) { item ->
                    val category = state.categories.firstOrNull { it.id == item.categoryId }
                    ListItem(
                        headlineContent = { Text(item.note?.takeIf { it.isNotBlank() } ?: category?.name.orEmpty()) },
                        supportingContent = {
                            Column {
                                Text(
                                    "${frequencyLabel(
                                        item.frequency,
                                    )} · ${stringResource(Res.string.every_n, item.interval)}",
                                )
                                Text(
                                    listOfNotNull(
                                        item.nextOccurrence?.let {
                                            stringResource(
                                                Res.string.next_run,
                                                it.toDisplayString(),
                                            )
                                        },
                                        stringResource(Res.string.generated, item.generatedCount),
                                        stringResource(if (item.active) Res.string.active else Res.string.paused),
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        },
                        leadingContent = { CategoryIcon(category?.code, category?.icon) },
                        trailingContent = {
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                MoneyText(item.signedAmount(), colorBySign = true)
                                Switch(checked = item.active, onCheckedChange = { viewModel.setActive(item, it) })
                            }
                        },
                        modifier = Modifier.clickable { onOpen(item.id) },
                    )
                }
            }
        }
    }
}

private fun com.financemanage.mobile.feature.transaction.domain.model.Recurring.signedAmount() =
    if (type.isInflow) amount else -amount

@Composable
fun RecurringEditScreen(
    id: String,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: RecurringEditViewModel = koinViewModel(key = "recurring-$id") { parametersOf(id) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pickCategory by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(state.done) { if (state.done) onDone() }
    val item = state.recurring
    FinanceScaffold(
        title = stringResource(Res.string.edit_recurring),
        onBack = onBack,
        actions = {
            IconButton(
                onClick = { confirmDelete = true },
            ) { Icon(Icons.Outlined.Delete, contentDescription = stringResource(CoreRes.string.action_delete)) }
        },
    ) { padding ->
        FormColumn(padding) {
            if (item == null) {
                state.error?.let { Text(it.userMessage(TransactionErrors), color = MaterialTheme.colorScheme.error) }
                return@FormColumn
            }
            OptionPicker(
                stringResource(CoreRes.string.field_wallet),
                state.wallets.filter { it.currency == item.amount.currency },
                state.wallets.firstOrNull { it.id == state.walletId },
                { walletLabel(it) },
                viewModel::onWallet,
            )
            CategoryField(
                stringResource(CoreRes.string.field_category),
                state.categories.firstOrNull { it.id == state.categoryId },
                state.categories,
                onClick = { pickCategory = true },
            )
            AmountTextField(
                state.amount,
                viewModel::onAmount,
                item.amount.currency,
                stringResource(CoreRes.string.field_amount),
                error = if (state.invalidAmount) stringResource(CoreRes.string.field_invalid_amount) else null,
            )
            FormTextField(state.note, viewModel::onNote, stringResource(CoreRes.string.field_note), maxLength = 500)
            DateField(stringResource(Res.string.end_date), state.endDate, viewModel::onEndDate)
            SwitchRow(stringResource(Res.string.active), state.active, viewModel::onActive)
            state.error?.let { Text(it.userMessage(TransactionErrors), color = MaterialTheme.colorScheme.error) }
            LoadingButton(stringResource(CoreRes.string.action_save), onClick = viewModel::save, loading = state.saving)
        }
    }
    if (pickCategory && item != null) {
        CategoryPickerSheet(
            categories = state.categories,
            type = if (item.type.isInflow) CategoryType.INCOME else CategoryType.EXPENSE,
            recentIds = emptyList(),
            onSelect = {
                pickCategory = false
                viewModel.onCategory(it)
            },
            onDismiss = { pickCategory = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(Res.string.delete_recurring),
            message = stringResource(CoreRes.string.delete_confirm_message),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** "Waiting to send" (F21): writes created offline, sent automatically when back online. */
@Composable
fun OutboxScreen(
    onBack: () -> Unit,
    viewModel: OutboxViewModel = koinViewModel(),
) {
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    FinanceScaffold(title = stringResource(Res.string.outbox_title), onBack = onBack) { padding ->
        if (pending.isEmpty()) {
            EmptyState(title = stringResource(Res.string.outbox_empty), modifier = Modifier.padding(padding))
            return@FinanceScaffold
        }
        LazyColumn(Modifier.padding(padding)) {
            items(pending, key = { it.id }) { entry ->
                val failed = entry.failed
                val errorColor = MaterialTheme.colorScheme.error
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
                            color = if (failed) errorColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = { Icon(Icons.Outlined.CloudUpload, contentDescription = null) },
                    trailingContent = {
                        androidx.compose.foundation.layout.Row {
                            if (failed) {
                                TextButton(
                                    onClick = { viewModel.retry(entry) },
                                ) { Text(stringResource(Res.string.retry)) }
                            }
                            TextButton(
                                onClick = { viewModel.discard(entry) },
                            ) { Text(stringResource(Res.string.discard)) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
