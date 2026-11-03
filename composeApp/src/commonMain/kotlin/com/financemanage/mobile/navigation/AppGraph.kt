package com.financemanage.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.financemanage.mobile.assets.AssetsScreen
import com.financemanage.mobile.core.navigation.AssetsRoute
import com.financemanage.mobile.core.navigation.BillDetailRoute
import com.financemanage.mobile.core.navigation.BudgetDetailRoute
import com.financemanage.mobile.core.navigation.GoalDetailRoute
import com.financemanage.mobile.core.navigation.HomeRoute
import com.financemanage.mobile.core.navigation.NotificationsRoute
import com.financemanage.mobile.core.navigation.PlanRoute
import com.financemanage.mobile.core.navigation.ReportsRoute
import com.financemanage.mobile.core.navigation.SettingsRoute
import com.financemanage.mobile.core.navigation.TransactionDetailRoute
import com.financemanage.mobile.core.navigation.TransactionsRoute
import com.financemanage.mobile.core.navigation.WalletsRoute
import com.financemanage.mobile.feature.bill.presentation.billGraph
import com.financemanage.mobile.feature.budget.presentation.budgetGraph
import com.financemanage.mobile.feature.category.presentation.categoryGraph
import com.financemanage.mobile.feature.currency.presentation.currencyGraph
import com.financemanage.mobile.feature.deposit.presentation.depositGraph
import com.financemanage.mobile.feature.home.presentation.HomeDestination
import com.financemanage.mobile.feature.home.presentation.HomeScreen
import com.financemanage.mobile.feature.investment.presentation.investmentGraph
import com.financemanage.mobile.feature.loan.presentation.loanGraph
import com.financemanage.mobile.feature.notification.presentation.notificationGraph
import com.financemanage.mobile.feature.report.presentation.reportGraph
import com.financemanage.mobile.feature.saving.presentation.savingGraph
import com.financemanage.mobile.feature.settings.presentation.settingsGraph
import com.financemanage.mobile.feature.transaction.presentation.transactionGraph
import com.financemanage.mobile.feature.wallet.presentation.walletGraph
import com.financemanage.mobile.plan.PlanScreen

/** Every destination of the signed-in app. Each feature contributes its own graph. */
fun NavGraphBuilder.appGraph(navController: NavHostController) {
    composable<HomeRoute> {
        HomeScreen(
            onNavigate = { destination ->
                navController.navigate(
                    when (destination) {
                        HomeDestination.Notifications -> NotificationsRoute
                        HomeDestination.Settings -> SettingsRoute
                        HomeDestination.Reports -> ReportsRoute
                        HomeDestination.Wallets -> WalletsRoute
                        HomeDestination.Transactions -> TransactionsRoute()
                        is HomeDestination.Plan -> PlanRoute(destination.tab)
                        is HomeDestination.Budget -> BudgetDetailRoute(destination.id)
                        is HomeDestination.Bill -> BillDetailRoute(destination.id)
                        is HomeDestination.Goal -> GoalDetailRoute(destination.id)
                        is HomeDestination.Transaction -> TransactionDetailRoute(destination.id)
                    },
                )
            },
        )
    }
    composable<PlanRoute> { entry ->
        PlanScreen(initialTab = entry.toRoute<PlanRoute>().tab, navController = navController)
    }
    composable<AssetsRoute> { AssetsScreen(onNavigate = { navController.navigate(it) }) }

    settingsGraph(navController)
    walletGraph(navController)
    categoryGraph(navController)
    transactionGraph(navController)
    currencyGraph(navController)
    budgetGraph(navController)
    billGraph(navController)
    savingGraph(navController)
    depositGraph(navController)
    loanGraph(navController)
    investmentGraph(navController)
    reportGraph(navController)
    notificationGraph(navController, onOpenLink = { navController.navigate(routeFor(it)) })
}
