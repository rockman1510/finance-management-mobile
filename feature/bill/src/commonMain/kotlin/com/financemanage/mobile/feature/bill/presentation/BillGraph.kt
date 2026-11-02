package com.financemanage.mobile.feature.bill.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.financemanage.mobile.core.navigation.BillDetailRoute
import com.financemanage.mobile.core.navigation.BillEditRoute
import com.financemanage.mobile.core.navigation.TransactionDetailRoute

/** Bill destinations (F14). The list is [BillsTab] inside the Plan screen. */
fun NavGraphBuilder.billGraph(navController: NavController) {
    composable<BillDetailRoute> { entry ->
        val id = entry.toRoute<BillDetailRoute>().id
        BillDetailScreen(
            id = id,
            onEdit = { navController.navigate(BillEditRoute(id)) },
            onOpenTransaction = { navController.navigate(TransactionDetailRoute(it)) },
            onBack = { navController.popBackStack() },
        )
    }
    composable<BillEditRoute> { entry ->
        BillFormScreen(entry.toRoute<BillEditRoute>().id, onDone = {
            navController.popBackStack()
        }, onBack = { navController.popBackStack() })
    }
}
