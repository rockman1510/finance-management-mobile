package com.financemanage.mobile.feature.investment.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.financemanage.mobile.core.navigation.InvestmentDetailRoute
import com.financemanage.mobile.core.navigation.InvestmentEditRoute
import com.financemanage.mobile.core.navigation.InvestmentsRoute

/** Investment destinations (F18). */
fun NavGraphBuilder.investmentGraph(navController: NavController) {
    composable<InvestmentsRoute> {
        PortfolioScreen(
            onOpen = { navController.navigate(InvestmentDetailRoute(it)) },
            onAdd = { navController.navigate(InvestmentEditRoute()) },
            onBack = { navController.popBackStack() },
        )
    }
    composable<InvestmentDetailRoute> { entry ->
        val id = entry.toRoute<InvestmentDetailRoute>().id
        AssetDetailScreen(id, onEdit = {
            navController.navigate(InvestmentEditRoute(id))
        }, onBack = { navController.popBackStack() })
    }
    composable<InvestmentEditRoute> { entry ->
        AssetFormScreen(entry.toRoute<InvestmentEditRoute>().id, onDone = {
            navController.popBackStack()
        }, onBack = { navController.popBackStack() })
    }
}
