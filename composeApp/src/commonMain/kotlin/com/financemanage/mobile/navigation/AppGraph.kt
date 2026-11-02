package com.financemanage.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.financemanage.mobile.assets.AssetsScreen
import com.financemanage.mobile.core.navigation.AssetsRoute
import com.financemanage.mobile.core.navigation.ExchangeRatesRoute
import com.financemanage.mobile.core.navigation.HomeRoute
import com.financemanage.mobile.core.navigation.NotificationSettingsRoute
import com.financemanage.mobile.core.navigation.OutboxRoute
import com.financemanage.mobile.core.navigation.PlanRoute
import com.financemanage.mobile.core.navigation.QuickAddRoute
import com.financemanage.mobile.core.navigation.RecurringListRoute
import com.financemanage.mobile.core.navigation.SettingsRoute
import com.financemanage.mobile.core.navigation.TransactionsRoute
import com.financemanage.mobile.core.navigation.TransferRoute
import com.financemanage.mobile.feature.category.presentation.categoryGraph
import com.financemanage.mobile.feature.settings.presentation.settingsGraph
import com.financemanage.mobile.feature.wallet.presentation.walletGraph
import com.financemanage.mobile.home.HomeScreen
import com.financemanage.mobile.plan.PlanScreen
import com.financemanage.mobile.transactions.TransactionsScreen

/** Every destination of the signed-in app. Each feature contributes its own graph. */
fun NavGraphBuilder.appGraph(navController: NavHostController) {
    composable<HomeRoute> { HomeScreen(onOpenSettings = { navController.navigate(SettingsRoute) }) }
    composable<TransactionsRoute> { TransactionsScreen() }
    composable<PlanRoute> { PlanScreen() }
    composable<AssetsRoute> { AssetsScreen() }

    settingsGraph(navController)
    walletGraph(navController)
    categoryGraph(navController)

    // Destinations of features not implemented yet.
    comingSoon<QuickAddRoute>(navController)
    comingSoon<TransferRoute>(navController)
    comingSoon<RecurringListRoute>(navController)
    comingSoon<ExchangeRatesRoute>(navController)
    comingSoon<NotificationSettingsRoute>(navController)
    comingSoon<OutboxRoute>(navController)
}

private inline fun <reified T : Any> NavGraphBuilder.comingSoon(navController: NavHostController) {
    composable<T> { ComingSoonScreen(onBack = { navController.popBackStack() }) }
}
