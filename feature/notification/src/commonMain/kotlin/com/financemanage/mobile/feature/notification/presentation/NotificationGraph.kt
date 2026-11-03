package com.financemanage.mobile.feature.notification.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.financemanage.mobile.core.data.push.NotificationLink
import com.financemanage.mobile.core.navigation.NotificationSettingsRoute
import com.financemanage.mobile.core.navigation.NotificationsRoute

/** Notification destinations (F20). [onOpenLink] navigates to the screen a notification points to. */
fun NavGraphBuilder.notificationGraph(
    navController: NavController,
    onOpenLink: (NotificationLink) -> Unit,
) {
    composable<NotificationsRoute> {
        NotificationsScreen(
            onOpen = onOpenLink,
            onOpenSettings = { navController.navigate(NotificationSettingsRoute) },
            onBack = { navController.popBackStack() },
        )
    }
    composable<NotificationSettingsRoute> { NotificationSettingsScreen(onBack = { navController.popBackStack() }) }
}
