package com.financemanage.mobile.feature.transaction.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.financemanage.mobile.core.navigation.OutboxRoute
import com.financemanage.mobile.core.navigation.QuickAddRoute
import com.financemanage.mobile.core.navigation.RecurringEditRoute
import com.financemanage.mobile.core.navigation.RecurringListRoute
import com.financemanage.mobile.core.navigation.TransactionDetailRoute
import com.financemanage.mobile.core.navigation.TransactionsRoute
import com.financemanage.mobile.core.navigation.TransferRoute

/** Transaction destinations (F07–F10, F21 outbox). The Transactions tab is [TransactionsRoute]. */
fun NavGraphBuilder.transactionGraph(navController: NavController) {
    composable<TransactionsRoute> { entry ->
        val route = entry.toRoute<TransactionsRoute>()
        val filtered = route.walletId != null || route.categoryId != null
        TransactionListScreen(
            walletId = route.walletId,
            categoryId = route.categoryId,
            onOpen = { navController.navigate(TransactionDetailRoute(it)) },
            onRecurring = { navController.navigate(RecurringListRoute) },
            // As a tab there is no back button; opened from a wallet/category it can go back.
            onBack = if (filtered) ({ navController.popBackStack() }) else null,
        )
    }
    composable<QuickAddRoute> { entry ->
        val route = entry.toRoute<QuickAddRoute>()
        QuickAddScreen(
            kind = route.kind,
            transactionId = route.transactionId,
            onDone = { navController.popBackStack() },
            onBack = { navController.popBackStack() },
        )
    }
    composable<TransactionDetailRoute> { entry ->
        val id = entry.toRoute<TransactionDetailRoute>().id
        TransactionDetailScreen(
            transactionId = id,
            onEdit = { navController.navigate(QuickAddRoute(transactionId = id)) },
            onBack = { navController.popBackStack() },
        )
    }
    composable<TransferRoute> {
        TransferScreen(onDone = { navController.popBackStack() }, onBack = { navController.popBackStack() })
    }
    composable<RecurringListRoute> {
        RecurringListScreen(onOpen = {
            navController.navigate(RecurringEditRoute(it))
        }, onBack = { navController.popBackStack() })
    }
    composable<RecurringEditRoute> { entry ->
        RecurringEditScreen(
            id = entry.toRoute<RecurringEditRoute>().id,
            onDone = { navController.popBackStack() },
            onBack = { navController.popBackStack() },
        )
    }
    composable<OutboxRoute> { OutboxScreen(onBack = { navController.popBackStack() }) }
}
