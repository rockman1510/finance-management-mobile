package com.financemanage.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.financemanage.mobile.assets.AssetsScreen
import com.financemanage.mobile.core.navigation.AssetsRoute
import com.financemanage.mobile.core.navigation.HomeRoute
import com.financemanage.mobile.core.navigation.NotificationSettingsRoute
import com.financemanage.mobile.core.navigation.PlanRoute
import com.financemanage.mobile.core.navigation.SettingsRoute
import com.financemanage.mobile.feature.bill.presentation.billGraph
import com.financemanage.mobile.feature.budget.presentation.budgetGraph
import com.financemanage.mobile.feature.category.presentation.categoryGraph
import com.financemanage.mobile.feature.currency.presentation.currencyGraph
import com.financemanage.mobile.feature.saving.presentation.savingGraph
import com.financemanage.mobile.feature.settings.presentation.settingsGraph
import com.financemanage.mobile.feature.transaction.presentation.transactionGraph
import com.financemanage.mobile.feature.wallet.presentation.walletGraph
import com.financemanage.mobile.home.HomeScreen
import com.financemanage.mobile.plan.PlanScreen

/** Every destination of the signed-in app. Each feature contributes its own graph. */
fun NavGraphBuilder.appGraph(navController: NavHostController) {
    composable<HomeRoute> { HomeScreen(onOpenSettings = { navController.navigate(SettingsRoute) }) }
    composable<PlanRoute> { entry ->
        PlanScreen(initialTab = entry.toRoute<PlanRoute>().tab, navController = navController)
    }
    composable<AssetsRoute> { AssetsScreen() }

    settingsGraph(navController)
    walletGraph(navController)
    categoryGraph(navController)
    transactionGraph(navController)
    currencyGraph(navController)
    budgetGraph(navController)
    billGraph(navController)
    savingGraph(navController)

    // Destinations of features not implemented yet.
    comingSoon<NotificationSettingsRoute>(navController)
}

private inline fun <reified T : Any> NavGraphBuilder.comingSoon(navController: NavHostController) {
    composable<T> { ComingSoonScreen(onBack = { navController.popBackStack() }) }
}
