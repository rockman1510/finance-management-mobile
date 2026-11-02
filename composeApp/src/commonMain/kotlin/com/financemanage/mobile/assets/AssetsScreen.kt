package com.financemanage.mobile.assets

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.financemanage.mobile.core.ui.resources.Res
import com.financemanage.mobile.core.ui.resources.assets_deposits
import com.financemanage.mobile.core.ui.resources.assets_investments
import com.financemanage.mobile.core.ui.resources.assets_loans
import com.financemanage.mobile.core.ui.resources.assets_net_worth
import com.financemanage.mobile.core.ui.resources.assets_wallets
import com.financemanage.mobile.core.ui.resources.coming_soon_title
import com.financemanage.mobile.core.ui.resources.tab_assets
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private data class AssetSection(
    val label: StringResource,
    val icon: ImageVector,
)

private val Sections =
    listOf(
        AssetSection(Res.string.assets_net_worth, Icons.Outlined.Savings),
        AssetSection(Res.string.assets_wallets, Icons.Outlined.AccountBalanceWallet),
        AssetSection(Res.string.assets_deposits, Icons.Outlined.AccountBalance),
        AssetSection(Res.string.assets_loans, Icons.Outlined.Handshake),
        AssetSection(Res.string.assets_investments, Icons.AutoMirrored.Outlined.ShowChart),
    )

/** Assets tab: Net worth (F19), Wallets (F05), Deposits (F16), Loans (F17), Investments (F18). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen() {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(title = { Text(stringResource(Res.string.tab_assets)) }, windowInsets = WindowInsets(0.dp))
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(Sections) { section ->
                ListItem(
                    headlineContent = { Text(stringResource(section.label)) },
                    supportingContent = { Text(stringResource(Res.string.coming_soon_title)) },
                    leadingContent = { Icon(section.icon, contentDescription = null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                )
                HorizontalDivider()
            }
        }
    }
}
