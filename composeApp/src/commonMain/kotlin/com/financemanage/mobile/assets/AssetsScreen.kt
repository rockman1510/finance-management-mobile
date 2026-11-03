package com.financemanage.mobile.assets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.financemanage.mobile.core.navigation.DepositsRoute
import com.financemanage.mobile.core.navigation.InvestmentsRoute
import com.financemanage.mobile.core.navigation.LoansRoute
import com.financemanage.mobile.core.navigation.NetWorthRoute
import com.financemanage.mobile.core.navigation.ReportsRoute
import com.financemanage.mobile.core.navigation.WalletsRoute
import com.financemanage.mobile.core.ui.resources.Res
import com.financemanage.mobile.core.ui.resources.assets_deposits
import com.financemanage.mobile.core.ui.resources.assets_investments
import com.financemanage.mobile.core.ui.resources.assets_loans
import com.financemanage.mobile.core.ui.resources.assets_reports
import com.financemanage.mobile.core.ui.resources.assets_wallets
import com.financemanage.mobile.core.ui.resources.tab_assets
import com.financemanage.mobile.feature.report.presentation.NetWorthCard
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private data class AssetSection(
    val label: StringResource,
    val icon: ImageVector,
    val route: Any,
)

private val Sections =
    listOf(
        AssetSection(Res.string.assets_wallets, Icons.Outlined.AccountBalanceWallet, WalletsRoute),
        AssetSection(Res.string.assets_deposits, Icons.Outlined.AccountBalance, DepositsRoute),
        AssetSection(Res.string.assets_loans, Icons.Outlined.Handshake, LoansRoute),
        AssetSection(Res.string.assets_investments, Icons.AutoMirrored.Outlined.ShowChart, InvestmentsRoute),
        AssetSection(Res.string.assets_reports, Icons.Outlined.BarChart, ReportsRoute),
    )

/** Assets tab: net worth (F19) on top, then Wallets (F05), Deposits (F16), Loans (F17), Investments (F18), Reports. */
@Composable
fun AssetsScreen(onNavigate: (Any) -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(title = { Text(stringResource(Res.string.tab_assets)) }, windowInsets = WindowInsets(0.dp))
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            item { NetWorthCard(onOpen = { onNavigate(NetWorthRoute) }) }
            items(Sections) { section ->
                ListItem(
                    headlineContent = { Text(stringResource(section.label)) },
                    leadingContent = { Icon(section.icon, contentDescription = null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigate(section.route) },
                )
                HorizontalDivider()
            }
        }
    }
}
