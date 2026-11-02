package com.financemanage.mobile.plan

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
import com.financemanage.mobile.core.ui.resources.Res
import com.financemanage.mobile.core.ui.resources.plan_bills
import com.financemanage.mobile.core.ui.resources.plan_budgets
import com.financemanage.mobile.core.ui.resources.plan_savings
import com.financemanage.mobile.navigation.ComingSoon
import org.jetbrains.compose.resources.stringResource

private val PlanTabs = listOf(Res.string.plan_budgets, Res.string.plan_bills, Res.string.plan_savings)

/** Plan tab: Budgets (F13), Bills (F14), Savings (F15). */
@Composable
fun PlanScreen() {
    var selected by rememberSaveable { mutableIntStateOf(0) }
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
        ComingSoon(Modifier.weight(1f))
    }
}
