package com.financemanage.mobile.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.financemanage.mobile.core.ui.resources.Res
import com.financemanage.mobile.core.ui.resources.tab_assets
import com.financemanage.mobile.core.ui.resources.tab_home
import com.financemanage.mobile.core.ui.resources.tab_plan
import com.financemanage.mobile.core.ui.resources.tab_transactions
import org.jetbrains.compose.resources.StringResource
import kotlin.reflect.KClass

/**
 * Tabs of the bottom bar. The ➕ button sits between [TRANSACTIONS] and [PLAN] and opens a sheet instead of a
 * destination.
 */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val label: StringResource,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(HomeRoute, HomeRoute::class, Res.string.tab_home, Icons.Filled.Home, Icons.Outlined.Home),
    TRANSACTIONS(
        TransactionsRoute,
        TransactionsRoute::class,
        Res.string.tab_transactions,
        Icons.AutoMirrored.Filled.ReceiptLong,
        Icons.AutoMirrored.Outlined.ReceiptLong,
    ),
    PLAN(PlanRoute, PlanRoute::class, Res.string.tab_plan, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    ASSETS(
        AssetsRoute,
        AssetsRoute::class,
        Res.string.tab_assets,
        Icons.Filled.AccountBalanceWallet,
        Icons.Outlined.AccountBalanceWallet,
    ),
}
