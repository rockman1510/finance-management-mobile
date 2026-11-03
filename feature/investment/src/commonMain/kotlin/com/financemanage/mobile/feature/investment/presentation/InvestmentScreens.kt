package com.financemanage.mobile.feature.investment.presentation

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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financemanage.mobile.core.model.CommonCurrencies
import com.financemanage.mobile.core.model.domain.Category
import com.financemanage.mobile.core.model.domain.Wallet
import com.financemanage.mobile.core.model.format.MoneyFormatter
import com.financemanage.mobile.core.model.time.toDisplayString
import com.financemanage.mobile.core.model.toBigDecimalOrNull
import com.financemanage.mobile.core.model.toChartValue
import com.financemanage.mobile.core.ui.components.AmountTextField
import com.financemanage.mobile.core.ui.components.ChartPalette
import com.financemanage.mobile.core.ui.components.ChartSlice
import com.financemanage.mobile.core.ui.components.ConfirmDialog
import com.financemanage.mobile.core.ui.components.DateField
import com.financemanage.mobile.core.ui.components.DonutChart
import com.financemanage.mobile.core.ui.components.EmptyState
import com.financemanage.mobile.core.ui.components.FinanceScaffold
import com.financemanage.mobile.core.ui.components.FormColumn
import com.financemanage.mobile.core.ui.components.FormTextField
import com.financemanage.mobile.core.ui.components.InfoRow
import com.financemanage.mobile.core.ui.components.LegendRow
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
import com.financemanage.mobile.core.ui.resources.field_amount
import com.financemanage.mobile.core.ui.resources.field_currency
import com.financemanage.mobile.core.ui.resources.field_date
import com.financemanage.mobile.core.ui.resources.field_invalid_amount
import com.financemanage.mobile.core.ui.resources.field_name
import com.financemanage.mobile.core.ui.resources.field_note
import com.financemanage.mobile.core.ui.resources.field_required
import com.financemanage.mobile.core.ui.resources.field_wallet
import com.financemanage.mobile.core.ui.theme.LocalHideBalances
import com.financemanage.mobile.core.ui.theme.Spacing
import com.financemanage.mobile.feature.investment.data.AssetDto
import com.financemanage.mobile.feature.investment.data.AssetTypes
import com.financemanage.mobile.feature.investment.data.TradeKind
import com.financemanage.mobile.feature.investment.resources.Res
import com.financemanage.mobile.feature.investment.resources.add_asset
import com.financemanage.mobile.feature.investment.resources.allocation
import com.financemanage.mobile.feature.investment.resources.average_cost
import com.financemanage.mobile.feature.investment.resources.buy
import com.financemanage.mobile.feature.investment.resources.cash_FAILED
import com.financemanage.mobile.feature.investment.resources.cash_PENDING
import com.financemanage.mobile.feature.investment.resources.cost_basis
import com.financemanage.mobile.feature.investment.resources.current_price
import com.financemanage.mobile.feature.investment.resources.delete_asset
import com.financemanage.mobile.feature.investment.resources.display_currency
import com.financemanage.mobile.feature.investment.resources.dividend
import com.financemanage.mobile.feature.investment.resources.edit_asset
import com.financemanage.mobile.feature.investment.resources.error_fee_too_high
import com.financemanage.mobile.feature.investment.resources.error_insufficient_quantity
import com.financemanage.mobile.feature.investment.resources.error_stale_price
import com.financemanage.mobile.feature.investment.resources.fee
import com.financemanage.mobile.feature.investment.resources.income_category
import com.financemanage.mobile.feature.investment.resources.investments
import com.financemanage.mobile.feature.investment.resources.market_value
import com.financemanage.mobile.feature.investment.resources.no_assets
import com.financemanage.mobile.feature.investment.resources.no_assets_message
import com.financemanage.mobile.feature.investment.resources.no_price
import com.financemanage.mobile.feature.investment.resources.price
import com.financemanage.mobile.feature.investment.resources.price_date
import com.financemanage.mobile.feature.investment.resources.quantity
import com.financemanage.mobile.feature.investment.resources.realized
import com.financemanage.mobile.feature.investment.resources.sell
import com.financemanage.mobile.feature.investment.resources.symbol
import com.financemanage.mobile.feature.investment.resources.total_line
import com.financemanage.mobile.feature.investment.resources.total_return
import com.financemanage.mobile.feature.investment.resources.track_only
import com.financemanage.mobile.feature.investment.resources.trade_BUY
import com.financemanage.mobile.feature.investment.resources.trade_INCOME
import com.financemanage.mobile.feature.investment.resources.trade_SELL
import com.financemanage.mobile.feature.investment.resources.trades
import com.financemanage.mobile.feature.investment.resources.type
import com.financemanage.mobile.feature.investment.resources.type_BOND
import com.financemanage.mobile.feature.investment.resources.type_CRYPTO
import com.financemanage.mobile.feature.investment.resources.type_FUND
import com.financemanage.mobile.feature.investment.resources.type_GOLD
import com.financemanage.mobile.feature.investment.resources.type_OTHER
import com.financemanage.mobile.feature.investment.resources.type_REAL_ESTATE
import com.financemanage.mobile.feature.investment.resources.type_STOCK
import com.financemanage.mobile.feature.investment.resources.unrealized
import com.financemanage.mobile.feature.investment.resources.update_price
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.financemanage.mobile.core.ui.resources.Res as CoreRes

private val InvestmentErrors: Map<String, StringResource> =
    mapOf(
        "INSUFFICIENT_QUANTITY" to Res.string.error_insufficient_quantity,
        "FEE_TOO_HIGH" to Res.string.error_fee_too_high,
        "STALE_PRICE" to Res.string.error_stale_price,
    )
private val TypeLabels =
    mapOf(
        "STOCK" to Res.string.type_STOCK,
        "FUND" to Res.string.type_FUND,
        "GOLD" to Res.string.type_GOLD,
        "CRYPTO" to Res.string.type_CRYPTO,
        "BOND" to Res.string.type_BOND,
        "REAL_ESTATE" to Res.string.type_REAL_ESTATE,
        "OTHER" to Res.string.type_OTHER,
    )

@Composable
private fun typeLabel(type: String) = TypeLabels[type]?.let { stringResource(it) } ?: type

/** Quantities keep up to 8 decimals (API §12). */
private fun parseDecimal(text: String): BigDecimal? =
    text.trim().replace(",", ".").toBigDecimalOrNull()?.takeIf {
        it.signum() >
            0
    }

@Composable
fun PortfolioScreen(
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    viewModel: PortfolioViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RefreshOnResume(viewModel::refresh)
    FinanceScaffold(
        title = stringResource(Res.string.investments),
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Outlined.Add, stringResource(Res.string.add_asset)) }
        },
    ) { padding ->
        ResourceContent(
            resource = state.assets,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding),
            isEmpty = { it.isEmpty() },
            empty = {
                EmptyState(
                    stringResource(Res.string.no_assets),
                    message = stringResource(Res.string.no_assets_message),
                    actionLabel = stringResource(Res.string.add_asset),
                    onAction = onAdd,
                )
            },
        ) { assets ->
            LazyColumn {
                item {
                    Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        OptionPicker(
                            stringResource(Res.string.display_currency),
                            (CommonCurrencies + state.currency).distinct(),
                            state.currency,
                            { currencyLabel(it) },
                            viewModel::setCurrency,
                        )
                        state.portfolio?.let { portfolio -> PortfolioCard(portfolio) }
                        state.error?.let {
                            Text(
                                it.userMessage(InvestmentErrors),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                items(assets, key = { it.id }) { asset -> AssetRow(asset) { onOpen(asset.id) } }
            }
        }
    }
}

@Composable
private fun PortfolioCard(portfolio: com.financemanage.mobile.feature.investment.data.PortfolioDto) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            InfoRow(stringResource(Res.string.market_value)) {
                MoneyText(portfolio.money(portfolio.marketValue), style = MaterialTheme.typography.titleLarge)
            }
            InfoRow(stringResource(Res.string.cost_basis)) { MoneyText(portfolio.money(portfolio.costBasis)) }
            InfoRow(stringResource(Res.string.unrealized)) {
                Row {
                    MoneyText(portfolio.money(portfolio.unrealized), colorBySign = true, showPlusSign = true)
                    portfolio.unrealizedPercent?.let { Text(" (${it.toStringExpanded()}%)") }
                }
            }
            InfoRow(stringResource(Res.string.realized)) {
                MoneyText(portfolio.money(portfolio.realized), colorBySign = true, showPlusSign = true)
            }
            InfoRow(stringResource(Res.string.total_return)) {
                MoneyText(portfolio.money(portfolio.totalReturn), colorBySign = true, showPlusSign = true)
            }
            if (portfolio.allocation.isNotEmpty()) {
                SectionHeader(stringResource(Res.string.allocation))
                val slices =
                    portfolio.allocation.mapIndexed { index, item ->
                        ChartSlice(
                            item.type,
                            (item.value ?: BigDecimal.ZERO).toChartValue(),
                            ChartPalette[
                                index %
                                    ChartPalette.size,
                            ],
                        )
                    }
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    DonutChart(slices)
                    Column {
                        portfolio.allocation.forEachIndexed { index, item ->
                            LegendRow(ChartPalette[index % ChartPalette.size], typeLabel(item.type)) {
                                Text("${item.percent?.toStringExpanded().orEmpty()}%")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetRow(
    asset: AssetDto,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(listOfNotNull(asset.name, asset.symbol).joinToString(" · ")) },
        supportingContent = {
            Text(
                listOfNotNull(
                    "${stringResource(Res.string.quantity)}: ${asset.quantity?.toStringExpanded().orEmpty()}",
                    if (!asset.priced) {
                        stringResource(Res.string.no_price)
                    } else {
                        asset.unrealizedPercent?.let {
                            "${it.toStringExpanded()}%"
                        }
                    },
                ).joinToString(" · "),
            )
        },
        trailingContent = { MoneyText(asset.money(asset.marketValue)) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

private enum class Sheet { PRICE, BUY, SELL, INCOME }

@Composable
fun AssetDetailScreen(
    id: String,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    viewModel: AssetDetailViewModel = koinViewModel(key = "asset-$id") { parametersOf(id) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    RefreshOnResume(viewModel::refresh)
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    val asset = state.asset
    FinanceScaffold(
        title = asset?.name.orEmpty(),
        onBack = onBack,
        actions = {
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, stringResource(Res.string.edit_asset)) }
            IconButton(
                onClick = { confirmDelete = true },
            ) { Icon(Icons.Outlined.Delete, stringResource(Res.string.delete_asset)) }
        },
    ) { padding ->
        if (asset == null) {
            ListSkeleton(Modifier.padding(padding))
            return@FinanceScaffold
        }
        LazyColumn(Modifier.padding(padding)) {
            item { PendingBanner(state.pending) }
            item {
                Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    MoneyText(asset.money(asset.marketValue), style = MaterialTheme.typography.headlineMedium)
                    InfoRow(stringResource(Res.string.quantity), asset.quantity?.toStringExpanded().orEmpty())
                    InfoRow(stringResource(Res.string.average_cost)) { MoneyText(asset.money(asset.averageCost)) }
                    InfoRow(stringResource(Res.string.current_price)) {
                        if (asset.priced) {
                            MoneyText(
                                asset.money(asset.lastPrice),
                            )
                        } else {
                            Text(stringResource(Res.string.no_price))
                        }
                    }
                    asset.priceDate?.let { InfoRow(stringResource(Res.string.price_date), it.toDisplayString()) }
                    InfoRow(stringResource(Res.string.unrealized)) {
                        MoneyText(asset.money(asset.unrealized), colorBySign = true, showPlusSign = true)
                    }
                    InfoRow(stringResource(Res.string.realized)) {
                        MoneyText(asset.money(asset.realized), colorBySign = true, showPlusSign = true)
                    }
                    state.error?.let { Text(it.userMessage(InvestmentErrors), color = MaterialTheme.colorScheme.error) }
                    OutlinedButton(onClick = {
                        sheet = Sheet.PRICE
                    }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(Res.string.update_price),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Button(
                            onClick = { sheet = Sheet.BUY },
                            enabled = !state.busy,
                        ) { Text(stringResource(Res.string.buy)) }
                        OutlinedButton(
                            onClick = { sheet = Sheet.SELL },
                            enabled = !state.busy,
                        ) { Text(stringResource(Res.string.sell)) }
                        OutlinedButton(
                            onClick = { sheet = Sheet.INCOME },
                            enabled = !state.busy,
                        ) { Text(stringResource(Res.string.dividend)) }
                    }
                }
            }
            item { SectionHeader(stringResource(Res.string.trades)) }
            items(asset.trades, key = { it.id }) { trade ->
                val label =
                    when (trade.type) {
                        "SELL" -> Res.string.trade_SELL
                        "INCOME" -> Res.string.trade_INCOME
                        else -> Res.string.trade_BUY
                    }
                val status =
                    when (trade.cashStatus) {
                        com.financemanage.mobile.core.model.AsyncStatus.PENDING ->
                            stringResource(
                                Res.string.cash_PENDING,
                            )
                        com.financemanage.mobile.core.model.AsyncStatus.FAILED -> stringResource(Res.string.cash_FAILED)
                        else -> null
                    }
                ListItem(
                    headlineContent = { Text(stringResource(label)) },
                    supportingContent = {
                        Text(
                            listOfNotNull(
                                trade.date?.toDisplayString(),
                                trade.quantity?.let { q ->
                                    "${q.toStringExpanded()} × ${MoneyFormatter.format(
                                        asset.money(trade.price),
                                        LocalHideBalances.current,
                                    )}"
                                },
                                status,
                            ).joinToString(" · "),
                        )
                    },
                    trailingContent = { MoneyText(asset.money(trade.cash)) },
                )
            }
        }
    }
    val current = sheet
    if (current != null && asset != null) {
        TradeSheet(
            current,
            asset,
            state.wallets.filter {
                it.currency == asset.currencyCode
            },
            state.incomeCategories,
            state.today,
            onDismiss = {
                sheet =
                    null
            },
        ) { qty, price, fee, wallet, category, date ->
            sheet = null
            when (current) {
                Sheet.PRICE -> viewModel.updatePrice(qty, date)
                Sheet.BUY -> viewModel.trade(TradeKind.BUY, qty, price, fee, wallet, null, date)
                Sheet.SELL -> viewModel.trade(TradeKind.SELL, qty, price, fee, wallet, null, date)
                Sheet.INCOME -> viewModel.trade(TradeKind.INCOME, qty, null, null, wallet, category, date)
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(Res.string.delete_asset),
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
private fun TradeSheet(
    sheet: Sheet,
    asset: AssetDto,
    wallets: List<Wallet>,
    categories: List<Category>,
    today: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal, BigDecimal?, BigDecimal?, String?, String?, LocalDate?) -> Unit,
) {
    var first by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var fee by remember { mutableStateOf("") }
    var walletId by remember { mutableStateOf<String?>(null) }
    var categoryId by remember { mutableStateOf(categories.firstOrNull { it.code == "INTEREST" }?.id) }
    var date by remember { mutableStateOf(today) }
    var invalid by remember { mutableStateOf(false) }
    val currency = asset.currencyCode
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            val title =
                when (sheet) {
                    Sheet.PRICE -> Res.string.update_price
                    Sheet.BUY -> Res.string.buy
                    Sheet.SELL -> Res.string.sell
                    Sheet.INCOME -> Res.string.dividend
                }
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            when (sheet) {
                Sheet.PRICE -> AmountTextField(first, { first = it }, currency, stringResource(Res.string.price))
                Sheet.INCOME ->
                    AmountTextField(
                        first,
                        { first = it },
                        currency,
                        stringResource(CoreRes.string.field_amount),
                    )
                Sheet.BUY, Sheet.SELL -> {
                    FormTextField(
                        first,
                        { first = it },
                        stringResource(Res.string.quantity),
                        keyboardType = KeyboardType.Decimal,
                    )
                    AmountTextField(price, { price = it }, currency, stringResource(Res.string.price))
                    AmountTextField(fee, { fee = it }, currency, stringResource(Res.string.fee))
                    val qty = parseDecimal(first)
                    val unit = parseDecimal(price)
                    if (qty != null && unit != null) {
                        val gross = qty * unit
                        val feeValue = parseDecimal(fee) ?: BigDecimal.ZERO
                        val total = if (sheet == Sheet.BUY) gross + feeValue else gross - feeValue
                        Text(
                            stringResource(
                                Res.string.total_line,
                                MoneyFormatter.format(asset.money(total).rounded(), LocalHideBalances.current),
                            ),
                        )
                    }
                }
            }
            if (sheet != Sheet.PRICE) {
                val options: List<Wallet?> = listOf(null) + wallets
                OptionPicker(
                    stringResource(CoreRes.string.field_wallet),
                    options,
                    options.firstOrNull {
                        it?.id ==
                            walletId
                    },
                    {
                        it?.let(::walletLabel) ?: stringResource(Res.string.track_only)
                    },
                    { walletId = it?.id },
                )
            }
            if (sheet == Sheet.INCOME && walletId != null) {
                OptionPicker(
                    stringResource(Res.string.income_category),
                    categories,
                    categories.firstOrNull {
                        it.id ==
                            categoryId
                    },
                    { it.name },
                    { categoryId = it.id },
                )
            }
            DateField(stringResource(CoreRes.string.field_date), date, { date = it })
            if (invalid) {
                Text(
                    stringResource(CoreRes.string.field_invalid_amount),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(onClick = {
                val value = parseDecimal(first)
                val unitPrice = parseDecimal(price)
                val needsPrice = sheet == Sheet.BUY || sheet == Sheet.SELL
                if (value == null || (needsPrice && unitPrice == null)) {
                    invalid = true
                } else {
                    onConfirm(
                        value,
                        unitPrice,
                        parseDecimal(fee),
                        walletId,
                        if (walletId !=
                            null
                        ) {
                            categoryId
                        } else {
                            null
                        },
                        date,
                    )
                }
            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(title)) }
        }
    }
}

@Composable
fun AssetFormScreen(
    id: String?,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: AssetFormViewModel = koinViewModel(key = "asset-form-$id") { parametersOf(id) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    FinanceScaffold(
        title = stringResource(if (state.isEdit) Res.string.edit_asset else Res.string.add_asset),
        onBack = onBack,
    ) { padding ->
        FormColumn(padding) {
            FormTextField(
                state.name,
                { v -> viewModel.update { it.copy(name = v) } },
                stringResource(CoreRes.string.field_name),
                error = if (state.nameMissing) stringResource(CoreRes.string.field_required) else null,
            )
            FormTextField(state.symbol, { v ->
                viewModel.update {
                    it.copy(symbol = v.uppercase())
                }
            }, stringResource(Res.string.symbol))
            OptionPicker(stringResource(Res.string.type), AssetTypes, state.type, {
                typeLabel(it)
            }, { v -> viewModel.update { it.copy(type = v) } }, enabled = !state.isEdit)
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
                enabled = !state.isEdit,
            )
            FormTextField(state.note, { v ->
                viewModel.update {
                    it.copy(note = v)
                }
            }, stringResource(CoreRes.string.field_note), maxLength = 500)
            state.error?.let { Text(it.userMessage(InvestmentErrors), color = MaterialTheme.colorScheme.error) }
            LoadingButton(stringResource(CoreRes.string.action_save), onClick = viewModel::save, loading = state.saving)
        }
    }
}
