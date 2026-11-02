package com.financemanage.mobile.feature.saving.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.financemanage.mobile.core.navigation.GoalDetailRoute
import com.financemanage.mobile.core.navigation.GoalEditRoute

/** Saving goal destinations (F15). The list is [SavingsTab] inside the Plan screen. */
fun NavGraphBuilder.savingGraph(navController: NavController) {
    composable<GoalDetailRoute> { entry ->
        val id = entry.toRoute<GoalDetailRoute>().id
        GoalDetailScreen(
            id,
            onEdit = { navController.navigate(GoalEditRoute(id)) },
            onBack = { navController.popBackStack() },
        )
    }
    composable<GoalEditRoute> { entry ->
        GoalFormScreen(entry.toRoute<GoalEditRoute>().id, onDone = {
            navController.popBackStack()
        }, onBack = { navController.popBackStack() })
    }
}
