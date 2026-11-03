package com.financemanage.mobile.feature.deposit.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.financemanage.mobile.core.navigation.DepositCreateRoute
import com.financemanage.mobile.core.navigation.DepositDetailRoute
import com.financemanage.mobile.core.navigation.DepositsRoute

/** Bank deposit destinations (F16). */
fun NavGraphBuilder.depositGraph(navController: NavController) {
    composable<DepositsRoute> {
        DepositListScreen(
            onOpen = { navController.navigate(DepositDetailRoute(it)) },
            onAdd = { navController.navigate(DepositCreateRoute) },
            onBack = { navController.popBackStack() },
        )
    }
    composable<DepositCreateRoute> {
        DepositCreateScreen(
            onCreated = { id -> navController.navigate(DepositDetailRoute(id)) { popUpTo(DepositsRoute) } },
            onBack = { navController.popBackStack() },
        )
    }
    composable<DepositDetailRoute> { entry ->
        DepositDetailScreen(entry.toRoute<DepositDetailRoute>().id, onBack = { navController.popBackStack() })
    }
}
