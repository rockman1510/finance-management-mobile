package com.financemanage.mobile.feature.loan.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.financemanage.mobile.core.navigation.LoanCreateRoute
import com.financemanage.mobile.core.navigation.LoanDetailRoute
import com.financemanage.mobile.core.navigation.LoansRoute

/** Loan destinations (F17). */
fun NavGraphBuilder.loanGraph(navController: NavController) {
    composable<LoansRoute> {
        LoanListScreen(
            onOpen = { navController.navigate(LoanDetailRoute(it)) },
            onAdd = { navController.navigate(LoanCreateRoute) },
            onBack = { navController.popBackStack() },
        )
    }
    composable<LoanCreateRoute> {
        LoanCreateScreen(
            onCreated = { id -> navController.navigate(LoanDetailRoute(id)) { popUpTo(LoansRoute) } },
            onBack = { navController.popBackStack() },
        )
    }
    composable<LoanDetailRoute> { entry ->
        LoanDetailScreen(entry.toRoute<LoanDetailRoute>().id, onBack = { navController.popBackStack() })
    }
}
