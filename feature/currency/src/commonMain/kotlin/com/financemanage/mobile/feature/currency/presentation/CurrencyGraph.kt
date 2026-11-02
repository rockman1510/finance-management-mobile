package com.financemanage.mobile.feature.currency.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.financemanage.mobile.core.navigation.AddRateRoute
import com.financemanage.mobile.core.navigation.CurrencyConverterRoute
import com.financemanage.mobile.core.navigation.ExchangeRatesRoute

/** Exchange rate destinations (F11). */
fun NavGraphBuilder.currencyGraph(navController: NavController) {
    composable<ExchangeRatesRoute> {
        RatesScreen(
            onAdd = { navController.navigate(AddRateRoute()) },
            onConverter = { navController.navigate(CurrencyConverterRoute) },
            onBack = { navController.popBackStack() },
        )
    }
    composable<AddRateRoute> { entry ->
        val route = entry.toRoute<AddRateRoute>()
        AddRateScreen(route.base, route.quote, onDone = {
            navController.popBackStack()
        }, onBack = { navController.popBackStack() })
    }
    composable<CurrencyConverterRoute> { ConverterScreen(onBack = { navController.popBackStack() }) }
}
