package com.financemanage.mobile.feature.currency.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financemanage.mobile.core.model.CommonCurrencies
import com.financemanage.mobile.core.model.time.toDisplayString
import com.financemanage.mobile.core.ui.components.DateField
import com.financemanage.mobile.core.ui.components.EmptyState
import com.financemanage.mobile.core.ui.components.FinanceScaffold
import com.financemanage.mobile.core.ui.components.FormColumn
import com.financemanage.mobile.core.ui.components.FormTextField
import com.financemanage.mobile.core.ui.components.LoadingButton
import com.financemanage.mobile.core.ui.components.OptionPicker
import com.financemanage.mobile.core.ui.components.RefreshOnResume
import com.financemanage.mobile.core.ui.components.ResourceContent
import com.financemanage.mobile.core.ui.components.currencyLabel
import com.financemanage.mobile.core.ui.error.userMessage
import com.financemanage.mobile.core.ui.resources.action_save
import com.financemanage.mobile.core.ui.resources.field_amount
import com.financemanage.mobile.feature.currency.resources.Res
import com.financemanage.mobile.feature.currency.resources.add_rate
import com.financemanage.mobile.feature.currency.resources.base
import com.financemanage.mobile.feature.currency.resources.convert
import com.financemanage.mobile.feature.currency.resources.converter_title
import com.financemanage.mobile.feature.currency.resources.delete_rate
import com.financemanage.mobile.feature.currency.resources.effective_date
import com.financemanage.mobile.feature.currency.resources.error_invalid_rate
import com.financemanage.mobile.feature.currency.resources.error_rate_missing
import com.financemanage.mobile.feature.currency.resources.error_same_currency
import com.financemanage.mobile.feature.currency.resources.global
import com.financemanage.mobile.feature.currency.resources.no_rates
import com.financemanage.mobile.feature.currency.resources.no_rates_message
import com.financemanage.mobile.feature.currency.resources.quote
import com.financemanage.mobile.feature.currency.resources.rate
import com.financemanage.mobile.feature.currency.resources.rate_example
import com.financemanage.mobile.feature.currency.resources.rates_title
import com.financemanage.mobile.feature.currency.resources.result
import com.financemanage.mobile.feature.currency.resources.used_rate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.financemanage.mobile.core.ui.resources.Res as CoreRes

private val CurrencyErrors: Map<String, StringResource> =
    mapOf(
        "EXCHANGE_RATE_MISSING" to Res.string.error_rate_missing,
        "SAME_CURRENCY" to Res.string.error_same_currency,
    )

@Composable
fun RatesScreen(
    onAdd: () -> Unit,
    onConverter: () -> Unit,
    onBack: () -> Unit,
    viewModel: RatesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RefreshOnResume(viewModel::refresh)
    FinanceScaffold(
        title = stringResource(Res.string.rates_title),
        onBack = onBack,
        actions = {
            IconButton(onClick = onConverter) {
                Icon(Icons.Outlined.Calculate, contentDescription = stringResource(Res.string.converter_title))
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(Res.string.add_rate))
            }
        },
    ) { padding ->
        ResourceContent(
            resource = state.rates,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding),
            isEmpty = { it.isEmpty() },
            empty = {
                EmptyState(
                    title = stringResource(Res.string.no_rates),
                    message = stringResource(Res.string.no_rates_message),
                    actionLabel = stringResource(Res.string.add_rate),
                    onAction = onAdd,
                )
            },
        ) { rates ->
            LazyColumn {
                state.actionError?.let { error ->
                    item { Text(error.userMessage(CurrencyErrors), color = MaterialTheme.colorScheme.error) }
                }
                items(rates, key = { it.id }) { rate ->
                    ListItem(
                        headlineContent = {
                            Text(
                                "1 ${rate.base.value} = ${rate.rate.toStringExpanded()} ${rate.quote.value}",
                            )
                        },
                        supportingContent = {
                            Text(
                                listOfNotNull(
                                    rate.effectiveDate?.toDisplayString(),
                                    if (rate.global) stringResource(Res.string.global) else null,
                                ).joinToString(" · "),
                            )
                        },
                        trailingContent = {
                            if (!rate.global) {
                                IconButton(onClick = { viewModel.delete(rate) }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = stringResource(Res.string.delete_rate),
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun AddRateScreen(
    base: String?,
    quote: String?,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddRateViewModel = koinViewModel(key = "add-rate-$base-$quote") { parametersOf(base, quote) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    val currencies = (CommonCurrencies + state.base + state.quote).distinct()
    FinanceScaffold(title = stringResource(Res.string.add_rate), onBack = onBack) { padding ->
        FormColumn(padding) {
            OptionPicker(
                stringResource(Res.string.base),
                currencies,
                state.base,
                { currencyLabel(it) },
                viewModel::onBase,
            )
            OptionPicker(
                stringResource(Res.string.quote),
                currencies,
                state.quote,
                { currencyLabel(it) },
                viewModel::onQuote,
                error =
                    if (state.problem ==
                        RateProblem.SAME_CURRENCY
                    ) {
                        stringResource(Res.string.error_same_currency)
                    } else {
                        null
                    },
            )
            FormTextField(
                value = state.rate,
                onValueChange = viewModel::onRate,
                label = stringResource(Res.string.rate),
                keyboardType = KeyboardType.Decimal,
                supportingText = stringResource(Res.string.rate_example, state.base.value, state.quote.value),
                error =
                    if (state.problem ==
                        RateProblem.INVALID_RATE
                    ) {
                        stringResource(Res.string.error_invalid_rate)
                    } else {
                        null
                    },
            )
            DateField(stringResource(Res.string.effective_date), state.date, viewModel::onDate)
            state.error?.let { Text(it.userMessage(CurrencyErrors), color = MaterialTheme.colorScheme.error) }
            LoadingButton(stringResource(CoreRes.string.action_save), onClick = viewModel::save, loading = state.saving)
        }
    }
}

@Composable
fun ConverterScreen(
    onBack: () -> Unit,
    viewModel: ConverterViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currencies = (CommonCurrencies + state.from + state.to).distinct()
    FinanceScaffold(title = stringResource(Res.string.converter_title), onBack = onBack) { padding ->
        FormColumn(padding) {
            FormTextField(
                state.amount,
                viewModel::onAmount,
                stringResource(CoreRes.string.field_amount),
                keyboardType = KeyboardType.Decimal,
            )
            OptionPicker(
                stringResource(Res.string.base),
                currencies,
                state.from,
                { currencyLabel(it) },
                viewModel::onFrom,
            )
            OptionPicker(stringResource(Res.string.quote), currencies, state.to, { currencyLabel(it) }, viewModel::onTo)
            DateField(stringResource(Res.string.effective_date), state.date, viewModel::onDate)
            LoadingButton(stringResource(Res.string.convert), onClick = viewModel::convert, loading = state.loading)
            state.result?.let { result ->
                Text(
                    stringResource(
                        Res.string.result,
                        "${result.amount?.toStringExpanded().orEmpty()} ${result.from.orEmpty()}",
                        "${result.converted?.toStringExpanded().orEmpty()} ${result.to.orEmpty()}",
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    stringResource(
                        Res.string.used_rate,
                        result.rate?.toStringExpanded().orEmpty(),
                        result.date?.toDisplayString().orEmpty(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            state.error?.let { Text(it.userMessage(CurrencyErrors), color = MaterialTheme.colorScheme.error) }
        }
    }
}
