package com.financemanage.mobile.feature.wallet.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.financemanage.mobile.core.navigation.AdjustBalanceRoute
import com.financemanage.mobile.core.navigation.TransactionsRoute
import com.financemanage.mobile.core.navigation.WalletDetailRoute
import com.financemanage.mobile.core.navigation.WalletEditRoute
import com.financemanage.mobile.core.navigation.WalletsRoute

/** Wallet destinations (F05). */
fun NavGraphBuilder.walletGraph(navController: NavController) {
    composable<WalletsRoute> {
        WalletListScreen(
            onOpen = { navController.navigate(WalletDetailRoute(it)) },
            onAdd = { navController.navigate(WalletEditRoute()) },
            onBack = { navController.popBackStack() },
        )
    }
    composable<WalletDetailRoute> { entry ->
        val id = entry.toRoute<WalletDetailRoute>().id
        WalletDetailScreen(
            walletId = id,
            onEdit = { navController.navigate(WalletEditRoute(id)) },
            onAdjust = { navController.navigate(AdjustBalanceRoute(id)) },
            onTransactions = { navController.navigate(TransactionsRoute(walletId = id)) },
            onBack = { navController.popBackStack() },
        )
    }
    composable<WalletEditRoute> { entry ->
        WalletFormScreen(
            walletId = entry.toRoute<WalletEditRoute>().id,
            onDone = { navController.popBackStack() },
            onBack = { navController.popBackStack() },
        )
    }
    composable<AdjustBalanceRoute> { entry ->
        AdjustBalanceScreen(
            walletId = entry.toRoute<AdjustBalanceRoute>().id,
            onDone = { navController.popBackStack() },
            onBack = { navController.popBackStack() },
        )
    }
}
