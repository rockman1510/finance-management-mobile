package com.financemanage.mobile.plan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.financemanage.mobile.core.navigation.BillDetailRoute
import com.financemanage.mobile.core.navigation.BillEditRoute
import com.financemanage.mobile.core.navigation.BudgetDetailRoute
import com.financemanage.mobile.core.navigation.BudgetEditRoute
import com.financemanage.mobile.core.navigation.GoalDetailRoute
import com.financemanage.mobile.core.navigation.GoalEditRoute
import com.financemanage.mobile.core.ui.resources.Res
import com.financemanage.mobile.core.ui.resources.plan_bills
import com.financemanage.mobile.core.ui.resources.plan_budgets
import com.financemanage.mobile.core.ui.resources.plan_savings
import com.financemanage.mobile.feature.bill.presentation.BillsTab
import com.financemanage.mobile.feature.budget.presentation.BudgetsTab
import com.financemanage.mobile.feature.saving.presentation.SavingsTab
import org.jetbrains.compose.resources.stringResource

private val PlanTabs = listOf(Res.string.plan_budgets, Res.string.plan_bills, Res.string.plan_savings)

/** Plan tab: Budgets (F13), Bills (F14), Savings (F15) – each tab comes from its feature. */
@Composable
fun PlanScreen(
    initialTab: Int,
    navController: NavController,
) {
    var selected by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, PlanTabs.lastIndex)) }
    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selected) {
            PlanTabs.forEachIndexed { index, label ->
                Tab(
                    selected = selected == index,
                    onClick = { selected = index },
                    text = { Text(stringResource(label)) },
                )
            }
        }
        Box(Modifier.weight(1f)) {
            when (selected) {
                0 ->
                    BudgetsTab(onOpen = {
                        navController.navigate(BudgetDetailRoute(it))
                    }, onAdd = { navController.navigate(BudgetEditRoute()) })
                1 ->
                    BillsTab(onOpen = {
                        navController.navigate(BillDetailRoute(it))
                    }, onAdd = { navController.navigate(BillEditRoute()) })
                else ->
                    SavingsTab(onOpen = {
                        navController.navigate(GoalDetailRoute(it))
                    }, onAdd = { navController.navigate(GoalEditRoute()) })
            }
        }
    }
}
