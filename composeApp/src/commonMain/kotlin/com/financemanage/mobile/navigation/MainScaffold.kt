package com.financemanage.mobile.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.financemanage.mobile.add.AddSheet
import com.financemanage.mobile.add.QuickAddPlaceholderScreen
import com.financemanage.mobile.assets.AssetsScreen
import com.financemanage.mobile.core.ui.components.OfflineBanner
import com.financemanage.mobile.core.ui.resources.Res
import com.financemanage.mobile.core.ui.resources.tab_add
import com.financemanage.mobile.home.HomeScreen
import com.financemanage.mobile.plan.PlanScreen
import com.financemanage.mobile.transactions.TransactionsScreen
import org.jetbrains.compose.resources.stringResource

/** Signed-in shell: offline banner, bottom bar with 5 items and the NavHost. */
@Composable
fun MainScaffold(
    isOnline: Boolean,
    navController: NavHostController = rememberNavController(),
) {
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = TopLevelDestination.entries.any { currentDestination.isOn(it) } || currentDestination == null

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                FinanceBottomBar(
                    currentDestination = currentDestination,
                    onNavigate = { navController.navigateToTopLevel(it) },
                    onAdd = { showAddSheet = true },
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!isOnline) OfflineBanner()
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                modifier = Modifier.weight(1f),
            ) {
                composable<HomeRoute> { HomeScreen() }
                composable<TransactionsRoute> { TransactionsScreen() }
                composable<PlanRoute> { PlanScreen() }
                composable<AssetsRoute> { AssetsScreen() }
                composable<QuickAddRoute> { entry ->
                    QuickAddPlaceholderScreen(
                        kind = entry.toRoute<QuickAddRoute>().kind,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddSheet(
            onDismiss = { showAddSheet = false },
            onSelect = { kind ->
                showAddSheet = false
                navController.navigate(QuickAddRoute(kind))
            },
        )
    }
}

@Composable
private fun FinanceBottomBar(
    currentDestination: NavDestination?,
    onNavigate: (TopLevelDestination) -> Unit,
    onAdd: () -> Unit,
) {
    NavigationBar {
        TopLevelDestination.entries.forEachIndexed { index, destination ->
            if (index == TopLevelDestination.PLAN.ordinal) {
                NavigationBarItem(
                    selected = false,
                    onClick = onAdd,
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.AddCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    label = { Text(stringResource(Res.string.tab_add)) },
                )
            }
            val selected = currentDestination.isOn(destination)
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(destination.label)) },
            )
        }
    }
}

private fun NavDestination?.isOn(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true

private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
