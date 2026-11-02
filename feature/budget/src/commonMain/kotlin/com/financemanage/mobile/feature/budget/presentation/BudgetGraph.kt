package com.financemanage.mobile.feature.budget.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.financemanage.mobile.core.navigation.BudgetDetailRoute
import com.financemanage.mobile.core.navigation.BudgetEditRoute
import com.financemanage.mobile.core.navigation.TransactionDetailRoute

/** Budget destinations (F13). The list is [BudgetsTab] inside the Plan screen. */
fun NavGraphBuilder.budgetGraph(navController: NavController) {
    composable<BudgetDetailRoute> { entry ->
        val id = entry.toRoute<BudgetDetailRoute>().id
        BudgetDetailScreen(
            id = id,
            onEdit = { navController.navigate(BudgetEditRoute(id)) },
            onOpenTransaction = { navController.navigate(TransactionDetailRoute(it)) },
            onBack = { navController.popBackStack() },
        )
    }
    composable<BudgetEditRoute> { entry ->
        BudgetFormScreen(
            id = entry.toRoute<BudgetEditRoute>().id,
            onDone = { navController.popBackStack() },
            onBack = { navController.popBackStack() },
        )
    }
}
