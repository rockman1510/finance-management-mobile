package com.financemanage.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.financemanage.mobile.core.data.push.NotificationLink
import com.financemanage.mobile.core.data.push.PushBridge
import com.financemanage.mobile.core.data.settings.AppSettingsRepository
import com.financemanage.mobile.core.navigation.BillDetailRoute
import com.financemanage.mobile.core.navigation.BudgetDetailRoute
import com.financemanage.mobile.core.navigation.DepositDetailRoute
import com.financemanage.mobile.core.navigation.GoalDetailRoute
import com.financemanage.mobile.core.navigation.LoanDetailRoute
import com.financemanage.mobile.core.navigation.NotificationsRoute
import com.financemanage.mobile.core.navigation.QuickAddRoute
import com.financemanage.mobile.core.ui.platform.rememberNotificationPermissionController
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject

/** Screen a notification points to (`referenceType` + `referenceId`, API §14). */
fun routeFor(link: NotificationLink): Any {
    val id = link.referenceId
    return when {
        link.referenceType == null -> QuickAddRoute()
        id == null -> NotificationsRoute
        else ->
            when (link.referenceType) {
                "BILL" -> BillDetailRoute(id)
                "BUDGET" -> BudgetDetailRoute(id)
                "SAVING_GOAL" -> GoalDetailRoute(id)
                "DEPOSIT" -> DepositDetailRoute(id)
                "LOAN" -> LoanDetailRoute(id)
                else -> NotificationsRoute
            }
    }
}

/**
 * Signed-in push glue (F20): opens the screen of a tapped notification / deep link, and asks for the notification
 * permission once, when a feature requests it (after the first budget or bill).
 */
@Composable
fun PushEffects(navController: NavController) {
    val opened by PushBridge.opened.collectAsStateWithLifecycle()
    LaunchedEffect(opened) {
        val link = opened ?: return@LaunchedEffect
        PushBridge.consumeOpened(link)
        navController.navigate(routeFor(link))
    }

    val settings = koinInject<AppSettingsRepository>()
    val permission = rememberNotificationPermissionController()
    LaunchedEffect(Unit) {
        PushBridge.permissionRequests.collect {
            val asked = settings.preferences.first().notificationPermissionAsked
            if (!asked && !permission.enabled) {
                settings.setNotificationPermissionAsked()
                permission.request()
            }
        }
    }
}
