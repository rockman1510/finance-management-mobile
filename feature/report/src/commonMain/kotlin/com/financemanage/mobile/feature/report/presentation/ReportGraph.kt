package com.financemanage.mobile.feature.report.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.financemanage.mobile.core.navigation.NetWorthRoute
import com.financemanage.mobile.core.navigation.ReportsRoute
import com.financemanage.mobile.core.navigation.TransactionsRoute

/** Report destinations (F19). */
fun NavGraphBuilder.reportGraph(navController: NavController) {
    composable<ReportsRoute> {
        ReportsScreen(
            onOpenCategoryTransactions = { navController.navigate(TransactionsRoute(categoryId = it)) },
            onBack = { navController.popBackStack() },
        )
    }
    composable<NetWorthRoute> { NetWorthScreen(onBack = { navController.popBackStack() }) }
}
