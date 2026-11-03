package com.financemanage.mobile.feature.notification.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financemanage.mobile.core.data.push.NotificationLink
import com.financemanage.mobile.core.model.time.toDisplayString
import com.financemanage.mobile.core.ui.components.EmptyState
import com.financemanage.mobile.core.ui.components.ErrorState
import com.financemanage.mobile.core.ui.components.FinanceScaffold
import com.financemanage.mobile.core.ui.components.ListSkeleton
import com.financemanage.mobile.core.ui.components.RefreshOnResume
import com.financemanage.mobile.core.ui.components.ResourceContent
import com.financemanage.mobile.core.ui.components.SectionHeader
import com.financemanage.mobile.core.ui.error.userMessage
import com.financemanage.mobile.core.ui.platform.rememberNotificationPermissionController
import com.financemanage.mobile.core.ui.theme.Spacing
import com.financemanage.mobile.feature.notification.data.NotificationDto
import com.financemanage.mobile.feature.notification.presentation.NotificationSettingsViewModel.Companion.DEFAULT_REMINDER_TIME
import com.financemanage.mobile.feature.notification.resources.Res
import com.financemanage.mobile.feature.notification.resources.daily_reminder
import com.financemanage.mobile.feature.notification.resources.daily_reminder_hint
import com.financemanage.mobile.feature.notification.resources.earlier
import com.financemanage.mobile.feature.notification.resources.group_bills
import com.financemanage.mobile.feature.notification.resources.group_budgets
import com.financemanage.mobile.feature.notification.resources.group_deposits
import com.financemanage.mobile.feature.notification.resources.group_goals
import com.financemanage.mobile.feature.notification.resources.group_loans
import com.financemanage.mobile.feature.notification.resources.mark_all_read
import com.financemanage.mobile.feature.notification.resources.no_notifications
import com.financemanage.mobile.feature.notification.resources.no_notifications_message
import com.financemanage.mobile.feature.notification.resources.notification_settings
import com.financemanage.mobile.feature.notification.resources.notifications
import com.financemanage.mobile.feature.notification.resources.open_system_settings
import com.financemanage.mobile.feature.notification.resources.permission_off
import com.financemanage.mobile.feature.notification.resources.quiet_from
import com.financemanage.mobile.feature.notification.resources.quiet_hours
import com.financemanage.mobile.feature.notification.resources.quiet_hours_hint
import com.financemanage.mobile.feature.notification.resources.quiet_to
import com.financemanage.mobile.feature.notification.resources.reminder_time
import com.financemanage.mobile.feature.notification.resources.turn_on
import com.financemanage.mobile.feature.notification.resources.type_BILL_DUE_SOON
import com.financemanage.mobile.feature.notification.resources.type_BILL_OVERDUE
import com.financemanage.mobile.feature.notification.resources.type_BILL_PAYMENT_FAILED
import com.financemanage.mobile.feature.notification.resources.type_BUDGET_EXCEEDED
import com.financemanage.mobile.feature.notification.resources.type_BUDGET_WARNING
import com.financemanage.mobile.feature.notification.resources.type_DEPOSIT_MATURED
import com.financemanage.mobile.feature.notification.resources.type_DEPOSIT_MATURING_SOON
import com.financemanage.mobile.feature.notification.resources.type_INSTALLMENT_DUE_SOON
import com.financemanage.mobile.feature.notification.resources.type_INSTALLMENT_OVERDUE
import com.financemanage.mobile.feature.notification.resources.type_LOAN_PAID_OFF
import com.financemanage.mobile.feature.notification.resources.type_SAVING_GOAL_MILESTONE
import com.financemanage.mobile.feature.notification.resources.unread
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import com.financemanage.mobile.core.ui.resources.Res as CoreRes
import com.financemanage.mobile.core.ui.resources.action_cancel as coreCancel
import com.financemanage.mobile.core.ui.resources.action_save as coreSave
import com.financemanage.mobile.core.ui.resources.today as coreToday
import com.financemanage.mobile.core.ui.resources.yesterday as coreYesterday

private fun iconFor(type: String): ImageVector =
    when {
        type.startsWith("DEPOSIT") -> Icons.Outlined.AccountBalance
        type.startsWith("INSTALLMENT") || type.startsWith("LOAN") -> Icons.Outlined.Handshake
        type.startsWith("BUDGET") -> Icons.Outlined.PieChart
        type.startsWith("BILL") -> Icons.AutoMirrored.Outlined.ReceiptLong
        type.startsWith("SAVING") -> Icons.Outlined.Savings
        type == NotificationSettingsViewModel.DAILY_REMINDER -> Icons.Outlined.EditNote
        else -> Icons.Outlined.Notifications
    }

/** Notification list (F20): grouped by day, unread in bold; tapping marks as read and opens the related screen. */
@Composable
fun NotificationsScreen(
    onOpen: (NotificationLink) -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    RefreshOnResume(viewModel::refresh)
    val actionError = state.actionError?.userMessage()
    LaunchedEffect(actionError) {
        if (actionError != null) {
            snackbar.showSnackbar(actionError)
            viewModel.clearError()
        }
    }
    FinanceScaffold(
        title = stringResource(Res.string.notifications),
        onBack = onBack,
        snackbarHostState = snackbar,
        actions = {
            if (state.unreadCount > 0) {
                IconButton(onClick = viewModel::markAllRead) {
                    Icon(Icons.Outlined.DoneAll, contentDescription = stringResource(Res.string.mark_all_read))
                }
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = stringResource(Res.string.notification_settings))
            }
        },
    ) { padding ->
        ResourceContent(
            resource = state.days,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding),
            isEmpty = { it.isEmpty() },
            empty = {
                EmptyState(
                    title = stringResource(Res.string.no_notifications),
                    message = stringResource(Res.string.no_notifications_message),
                    icon = Icons.Outlined.Notifications,
                )
            },
        ) { days ->
            LazyColumn {
                item { PermissionCard() }
                days.forEach { day ->
                    item(key = "day-${day.date}") {
                        val title =
                            when (day.date) {
                                null -> stringResource(Res.string.earlier)
                                state.today -> stringResource(CoreRes.string.coreToday)
                                state.today?.minus(DatePeriod(days = 1)) -> stringResource(CoreRes.string.coreYesterday)
                                else -> day.date.toDisplayString()
                            }
                        SectionHeader(title)
                    }
                    items(day.items, key = { it.id }) { notification ->
                        NotificationRow(notification) {
                            viewModel.open(notification)
                            onOpen(NotificationLink(notification.referenceType, notification.referenceId))
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: NotificationDto,
    onClick: () -> Unit,
) {
    val weight = if (notification.read) FontWeight.Normal else FontWeight.Bold
    val time =
        notification.createdAt
            ?.toLocalDateTime(TimeZone.currentSystemDefault())
            ?.time
            ?.let { formatTime(it) }
            .orEmpty()
    ListItem(
        headlineContent = { Text(notification.title, fontWeight = weight) },
        supportingContent = { Text(notification.body, maxLines = 3) },
        leadingContent = { Icon(iconFor(notification.type), contentDescription = null) },
        trailingContent = {
            Column {
                Text(time, style = MaterialTheme.typography.labelSmall)
                if (!notification.read) {
                    val unread = stringResource(Res.string.unread)
                    Badge(modifier = Modifier.padding(top = Spacing.xs)) { Text(unread) }
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

private fun formatTime(time: LocalTime): String =
    "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"

/** Shown when the system blocks notifications: asks for the permission or links to the system settings. */
@Composable
private fun PermissionCard() {
    val permission = rememberNotificationPermissionController()
    if (permission.enabled) return
    Card(
        modifier = Modifier.fillMaxWidth().padding(Spacing.md),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        ListItem(
            leadingContent = { Icon(Icons.Outlined.NotificationsOff, contentDescription = null) },
            headlineContent = { Text(stringResource(Res.string.permission_off)) },
            supportingContent = {
                Column {
                    TextButton(onClick = permission::request) { Text(stringResource(Res.string.turn_on)) }
                    TextButton(
                        onClick = permission::openSettings,
                    ) { Text(stringResource(Res.string.open_system_settings)) }
                }
            },
        )
    }
}

private val TypeGroups: List<Pair<StringResource, List<Pair<String, StringResource>>>> =
    listOf(
        Res.string.group_deposits to
            listOf(
                "DEPOSIT_MATURING_SOON" to Res.string.type_DEPOSIT_MATURING_SOON,
                "DEPOSIT_MATURED" to Res.string.type_DEPOSIT_MATURED,
            ),
        Res.string.group_loans to
            listOf(
                "INSTALLMENT_DUE_SOON" to Res.string.type_INSTALLMENT_DUE_SOON,
                "INSTALLMENT_OVERDUE" to Res.string.type_INSTALLMENT_OVERDUE,
                "LOAN_PAID_OFF" to Res.string.type_LOAN_PAID_OFF,
            ),
        Res.string.group_budgets to
            listOf(
                "BUDGET_WARNING" to Res.string.type_BUDGET_WARNING,
                "BUDGET_EXCEEDED" to Res.string.type_BUDGET_EXCEEDED,
            ),
        Res.string.group_bills to
            listOf(
                "BILL_DUE_SOON" to Res.string.type_BILL_DUE_SOON,
                "BILL_OVERDUE" to Res.string.type_BILL_OVERDUE,
                "BILL_PAYMENT_FAILED" to Res.string.type_BILL_PAYMENT_FAILED,
            ),
        Res.string.group_goals to listOf("SAVING_GOAL_MILESTONE" to Res.string.type_SAVING_GOAL_MILESTONE),
    )

/** Notification settings (Settings → Notifications): a switch per type, daily reminder, quiet hours. */
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val saveError = state.saveError?.userMessage()
    LaunchedEffect(saveError) {
        if (saveError != null) {
            snackbar.showSnackbar(saveError)
            viewModel.clearError()
        }
    }
    FinanceScaffold(
        title = stringResource(Res.string.notification_settings),
        onBack = onBack,
        snackbarHostState = snackbar,
    ) { padding ->
        val preferences = state.preferences
        val loadError = state.loadError
        when {
            preferences == null && loadError != null ->
                ErrorState(loadError.userMessage(), onRetry = viewModel::load, modifier = Modifier.padding(padding))
            preferences == null -> ListSkeleton(Modifier.padding(padding))
            else ->
                LazyColumn(Modifier.padding(padding)) {
                    item { PermissionCard() }
                    item {
                        SwitchRow(
                            title = stringResource(Res.string.daily_reminder),
                            subtitle = stringResource(Res.string.daily_reminder_hint),
                            checked = preferences.dailyReminderEnabled,
                            onChange = viewModel::setDailyReminder,
                        )
                        if (preferences.dailyReminderEnabled) {
                            TimeRow(
                                label = stringResource(Res.string.reminder_time),
                                time = preferences.dailyReminderTime ?: DEFAULT_REMINDER_TIME,
                                onChange = viewModel::setDailyReminderTime,
                            )
                        }
                        HorizontalDivider()
                        val quiet = preferences.quietStart != null && preferences.quietEnd != null
                        SwitchRow(
                            title = stringResource(Res.string.quiet_hours),
                            subtitle = stringResource(Res.string.quiet_hours_hint),
                            checked = quiet,
                            onChange = viewModel::setQuietHours,
                        )
                        if (quiet) {
                            TimeRow(
                                stringResource(Res.string.quiet_from),
                                preferences.quietStart,
                                viewModel::setQuietStart,
                            )
                            TimeRow(stringResource(Res.string.quiet_to), preferences.quietEnd, viewModel::setQuietEnd)
                        }
                    }
                    TypeGroups.forEach { (group, types) ->
                        item(key = group.key) {
                            SectionHeader(stringResource(group))
                            types.forEach { (type, label) ->
                                SwitchRow(
                                    title = stringResource(label),
                                    checked = type !in preferences.disabledTypes,
                                    onChange = { viewModel.setTypeEnabled(type, it) },
                                )
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        modifier = Modifier.clickable { onChange(!checked) },
    )
}

@Composable
private fun TimeRow(
    label: String,
    time: LocalTime?,
    onChange: (LocalTime) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            Text(
                time?.let { formatTime(it) }.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        modifier = Modifier.clickable { open = true },
    )
    if (open) {
        val pickerState =
            rememberTimePickerState(initialHour = time?.hour ?: 0, initialMinute = time?.minute ?: 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    onChange(LocalTime(pickerState.hour, pickerState.minute))
                    open = false
                }) { Text(stringResource(CoreRes.string.coreSave)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { open = false },
                ) { Text(stringResource(CoreRes.string.coreCancel)) }
            },
            text = { TimePicker(pickerState) },
        )
    }
}
