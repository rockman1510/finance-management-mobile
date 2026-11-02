package com.financemanage.mobile.feature.transaction.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.financemanage.mobile.core.model.domain.Category
import com.financemanage.mobile.core.model.domain.Wallet
import com.financemanage.mobile.core.model.toBigDecimalOrNull
import com.financemanage.mobile.core.ui.components.DateField
import com.financemanage.mobile.core.ui.components.FormTextField
import com.financemanage.mobile.core.ui.components.OptionPicker
import com.financemanage.mobile.core.ui.components.categoryLabel
import com.financemanage.mobile.core.ui.components.walletLabel
import com.financemanage.mobile.core.ui.resources.all_wallets
import com.financemanage.mobile.core.ui.resources.field_wallet
import com.financemanage.mobile.core.ui.theme.Spacing
import com.financemanage.mobile.feature.transaction.domain.TransactionFilter
import com.financemanage.mobile.feature.transaction.resources.Res
import com.financemanage.mobile.feature.transaction.resources.apply
import com.financemanage.mobile.feature.transaction.resources.categories
import com.financemanage.mobile.feature.transaction.resources.clear_all
import com.financemanage.mobile.feature.transaction.resources.filter
import com.financemanage.mobile.feature.transaction.resources.from_date
import com.financemanage.mobile.feature.transaction.resources.max_amount
import com.financemanage.mobile.feature.transaction.resources.min_amount
import com.financemanage.mobile.feature.transaction.resources.period_last_month
import com.financemanage.mobile.feature.transaction.resources.period_this_month
import com.financemanage.mobile.feature.transaction.resources.to_date
import com.financemanage.mobile.feature.transaction.resources.types
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.stringResource
import com.financemanage.mobile.core.ui.resources.Res as CoreRes

private fun monthRange(anyDay: LocalDate): Pair<LocalDate, LocalDate> {
    val first = LocalDate(anyDay.year, anyDay.monthNumber, 1)
    val last = first.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
    return first to last
}

/** Filter bottom sheet (F07): wallet, categories, types, date range, amount range. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun FilterSheet(
    filter: TransactionFilter,
    wallets: List<Wallet>,
    categories: List<Category>,
    onApply: (TransactionFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(filter) }
    var minText by remember { mutableStateOf(filter.minAmount?.toStringExpanded().orEmpty()) }
    var maxText by remember { mutableStateOf(filter.maxAmount?.toStringExpanded().orEmpty()) }
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(stringResource(Res.string.filter), style = MaterialTheme.typography.titleMedium)
            val walletOptions: List<Wallet?> = listOf(null) + wallets
            OptionPicker(
                label = stringResource(CoreRes.string.field_wallet),
                options = walletOptions,
                selected = walletOptions.firstOrNull { it?.id == draft.walletId },
                optionLabel = { it?.let(::walletLabel) ?: stringResource(CoreRes.string.all_wallets) },
                onSelect = { draft = draft.copy(walletId = it?.id) },
            )
            Text(stringResource(Res.string.types), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                FilterableTypes.forEach { type ->
                    val selected = type in draft.types
                    FilterChip(
                        selected = selected,
                        onClick = {
                            draft =
                                draft.copy(
                                    types = if (selected) draft.types - type else draft.types + type,
                                )
                        },
                        label = { Text(typeLabel(type)) },
                    )
                }
            }
            Text(stringResource(Res.string.categories), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                categories.forEach { category ->
                    val selected = category.id in draft.categoryIds
                    FilterChip(
                        selected = selected,
                        onClick = {
                            draft =
                                draft.copy(
                                    categoryIds =
                                        if (selected) {
                                            draft.categoryIds - category.id
                                        } else {
                                            draft.categoryIds +
                                                category.id
                                        },
                                )
                        },
                        label = { Text(categoryLabel(category, categories)) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                FilterChip(
                    selected = false,
                    onClick = { monthRange(today).let { (from, to) -> draft = draft.copy(from = from, to = to) } },
                    label = { Text(stringResource(Res.string.period_this_month)) },
                )
                FilterChip(
                    selected = false,
                    onClick = {
                        monthRange(today.minus(DatePeriod(months = 1))).let { (from, to) ->
                            draft =
                                draft.copy(from = from, to = to)
                        }
                    },
                    label = { Text(stringResource(Res.string.period_last_month)) },
                )
            }
            DateField(stringResource(Res.string.from_date), draft.from, { draft = draft.copy(from = it) })
            DateField(stringResource(Res.string.to_date), draft.to, { draft = draft.copy(to = it) })
            FormTextField(minText, { minText = it }, stringResource(Res.string.min_amount))
            FormTextField(maxText, { maxText = it }, stringResource(Res.string.max_amount))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(
                    onClick = { onApply(TransactionFilter(query = filter.query)) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.clear_all))
                }
                Button(
                    onClick = {
                        onApply(
                            draft.copy(
                                minAmount = minText.replace(',', '.').toBigDecimalOrNull(),
                                maxAmount = maxText.replace(',', '.').toBigDecimalOrNull(),
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(Res.string.apply)) }
            }
        }
    }
}
