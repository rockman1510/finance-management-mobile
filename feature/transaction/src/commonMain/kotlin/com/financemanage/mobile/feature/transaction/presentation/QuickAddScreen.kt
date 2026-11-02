package com.financemanage.mobile.feature.transaction.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financemanage.mobile.core.model.domain.CategoryType
import com.financemanage.mobile.core.model.format.MoneyFormatter
import com.financemanage.mobile.core.ui.components.AmountKeypad
import com.financemanage.mobile.core.ui.components.CategoryField
import com.financemanage.mobile.core.ui.components.CategoryPickerSheet
import com.financemanage.mobile.core.ui.components.DateField
import com.financemanage.mobile.core.ui.components.FinanceScaffold
import com.financemanage.mobile.core.ui.components.FormTextField
import com.financemanage.mobile.core.ui.components.LoadingButton
import com.financemanage.mobile.core.ui.components.OptionPicker
import com.financemanage.mobile.core.ui.components.SwitchRow
import com.financemanage.mobile.core.ui.components.walletLabel
import com.financemanage.mobile.core.ui.error.userMessage
import com.financemanage.mobile.core.ui.resources.action_save
import com.financemanage.mobile.core.ui.resources.field_category
import com.financemanage.mobile.core.ui.resources.field_date
import com.financemanage.mobile.core.ui.resources.field_note
import com.financemanage.mobile.core.ui.resources.field_wallet
import com.financemanage.mobile.core.ui.theme.FinanceTheme
import com.financemanage.mobile.core.ui.theme.Spacing
import com.financemanage.mobile.feature.transaction.resources.Res
import com.financemanage.mobile.feature.transaction.resources.add_another
import com.financemanage.mobile.feature.transaction.resources.edit_transaction
import com.financemanage.mobile.feature.transaction.resources.end_date
import com.financemanage.mobile.feature.transaction.resources.error_amount_positive
import com.financemanage.mobile.feature.transaction.resources.error_choose_category
import com.financemanage.mobile.feature.transaction.resources.error_choose_wallet
import com.financemanage.mobile.feature.transaction.resources.exclude_from_report
import com.financemanage.mobile.feature.transaction.resources.expense
import com.financemanage.mobile.feature.transaction.resources.frequency
import com.financemanage.mobile.feature.transaction.resources.income
import com.financemanage.mobile.feature.transaction.resources.interval
import com.financemanage.mobile.feature.transaction.resources.more_details
import com.financemanage.mobile.feature.transaction.resources.photos_not_uploaded_offline
import com.financemanage.mobile.feature.transaction.resources.quick_add_expense
import com.financemanage.mobile.feature.transaction.resources.quick_add_income
import com.financemanage.mobile.feature.transaction.resources.receipts
import com.financemanage.mobile.feature.transaction.resources.repeat
import com.financemanage.mobile.feature.transaction.resources.saved_ok
import com.financemanage.mobile.feature.transaction.resources.saved_queued
import com.financemanage.mobile.feature.transaction.resources.today_chip
import com.financemanage.mobile.feature.transaction.resources.uploading
import com.financemanage.mobile.feature.transaction.resources.with_person
import com.financemanage.mobile.feature.transaction.resources.yesterday_chip
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.financemanage.mobile.core.ui.resources.Res as CoreRes

/** Quick add (F07): expense in ≤ 3 taps – amount, category, Save. Also edits an existing transaction. */
@Composable
fun QuickAddScreen(
    kind: String,
    transactionId: String?,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: QuickAddViewModel =
        koinViewModel(key = "quick-add-$kind-$transactionId") { parametersOf(kind, transactionId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showDetails by rememberSaveable { mutableStateOf(transactionId != null) }
    var pickCategory by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                QuickAddEvent.Done -> onDone()
                QuickAddEvent.Saved, QuickAddEvent.SavedAndCleared ->
                    snackbar.showSnackbar(
                        getString(Res.string.saved_ok),
                    )
                is QuickAddEvent.Queued -> {
                    snackbar.showSnackbar(getString(Res.string.saved_queued))
                    if (event.droppedFiles) snackbar.showSnackbar(getString(Res.string.photos_not_uploaded_offline))
                }
            }
        }
    }

    val title =
        when {
            state.isEdit -> Res.string.edit_transaction
            state.isIncome -> Res.string.quick_add_income
            else -> Res.string.quick_add_expense
        }
    FinanceScaffold(title = stringResource(title), onBack = onBack, snackbarHostState = snackbar) { padding ->
        Column(
            Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (!state.isEdit) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !state.isIncome,
                        onClick = { viewModel.setIncome(false) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text(stringResource(Res.string.expense)) }
                    SegmentedButton(
                        selected = state.isIncome,
                        onClick = { viewModel.setIncome(true) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text(stringResource(Res.string.income)) }
                }
            }
            val currency = state.wallet?.currency
            val amountText = "${state.amount.display}${MoneyFormatter.SPACE}${currency?.let {
                MoneyFormatter.symbol(
                    it,
                )
            }.orEmpty()}"
            Text(
                text = amountText,
                style = MaterialTheme.typography.displaySmall,
                color = if (state.isIncome) FinanceTheme.colors.income else FinanceTheme.colors.expense,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = amountText },
            )
            if (QuickAddProblem.AMOUNT in state.problems) {
                Text(stringResource(Res.string.error_amount_positive), color = MaterialTheme.colorScheme.error)
            }
            AmountKeypad(onKey = viewModel::onKey, allowDecimal = (currency?.fractionDigits ?: 0) > 0)
            CategoryField(
                label = stringResource(CoreRes.string.field_category),
                selected = state.category,
                all = state.categories,
                onClick = { pickCategory = true },
                error =
                    if (QuickAddProblem.CATEGORY in
                        state.problems
                    ) {
                        stringResource(Res.string.error_choose_category)
                    } else {
                        null
                    },
            )
            OptionPicker(
                label = stringResource(CoreRes.string.field_wallet),
                options = state.wallets,
                selected = state.wallet,
                optionLabel = { walletLabel(it) },
                onSelect = viewModel::onWallet,
                error =
                    if (QuickAddProblem.WALLET in
                        state.problems
                    ) {
                        stringResource(Res.string.error_choose_wallet)
                    } else {
                        null
                    },
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                FilterChip(selected = state.date == state.today, onClick = viewModel::setToday, label = {
                    Text(stringResource(Res.string.today_chip))
                })
                val yesterday = state.today?.minus(DatePeriod(days = 1))
                FilterChip(selected = state.date == yesterday, onClick = viewModel::setYesterday, label = {
                    Text(stringResource(Res.string.yesterday_chip))
                })
            }
            DateField(stringResource(CoreRes.string.field_date), state.date, viewModel::onDate)
            TextButton(onClick = { showDetails = !showDetails }) {
                Text(stringResource(Res.string.more_details))
                Icon(
                    if (showDetails) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                )
            }
            if (showDetails) {
                FormTextField(
                    state.note,
                    viewModel::onNote,
                    stringResource(CoreRes.string.field_note),
                    maxLength = 500,
                    singleLine = false,
                )
                FormTextField(
                    state.withPerson,
                    viewModel::onPerson,
                    stringResource(Res.string.with_person),
                    maxLength = 120,
                )
                SwitchRow(stringResource(Res.string.exclude_from_report), state.excludeFromReport, viewModel::onExclude)
                if (!state.isEdit) {
                    RepeatEditor(state.repeat, viewModel::onRepeat)
                    if (!state.repeat.enabled) {
                        Text(stringResource(Res.string.receipts), style = MaterialTheme.typography.labelLarge)
                        PendingReceiptsRow(
                            files = state.files,
                            canAdd = state.files.size < MAX_RECEIPTS,
                            onAdd = viewModel::addFile,
                            onRemove = viewModel::removeFile,
                            onView = { viewing = it },
                        )
                    }
                }
            }
            if (!state.isEdit) {
                SwitchRow(
                    stringResource(Res.string.add_another),
                    state.addAnother,
                    viewModel::onAddAnother,
                )
            }
            state.uploadProgress?.let { (done, total) -> Text(stringResource(Res.string.uploading, done, total)) }
            state.error?.let { Text(it.userMessage(TransactionErrors), color = MaterialTheme.colorScheme.error) }
            LoadingButton(stringResource(CoreRes.string.action_save), onClick = viewModel::save, loading = state.saving)
        }
    }
    if (pickCategory) {
        CategoryPickerSheet(
            categories = state.categories,
            type = if (state.isIncome) CategoryType.INCOME else CategoryType.EXPENSE,
            recentIds = state.recentCategoryIds,
            onSelect = {
                pickCategory = false
                viewModel.onCategory(it)
            },
            onDismiss = { pickCategory = false },
        )
    }
    viewing?.let { ImageViewer(it) { viewing = null } }
}

private const val MAX_RECEIPTS = 5

/** "Repeat" switch with frequency, every N periods and optional end date (F09). */
@Composable
private fun RepeatEditor(
    repeat: RepeatSettings,
    onChange: (RepeatSettings) -> Unit,
) {
    SwitchRow(stringResource(Res.string.repeat), repeat.enabled, { onChange(repeat.copy(enabled = it)) })
    if (!repeat.enabled) return
    OptionPicker(
        label = stringResource(Res.string.frequency),
        options = RepeatFrequencies,
        selected = repeat.frequency,
        optionLabel = { frequencyLabel(it) },
        onSelect = { onChange(repeat.copy(frequency = it)) },
    )
    FormTextField(
        value = repeat.interval.toString(),
        onValueChange = { text ->
            onChange(
                repeat.copy(
                    interval =
                        text
                            .filter(Char::isDigit)
                            .take(3)
                            .toIntOrNull()
                            ?.coerceAtLeast(1) ?: 1,
                ),
            )
        },
        label = stringResource(Res.string.interval),
        keyboardType = KeyboardType.Number,
    )
    DateField(stringResource(Res.string.end_date), repeat.endDate, { onChange(repeat.copy(endDate = it)) })
}
