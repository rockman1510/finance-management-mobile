package com.financemanage.mobile.feature.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financemanage.mobile.core.data.settings.ThemeMode
import com.financemanage.mobile.core.ui.components.ConfirmDialog
import com.financemanage.mobile.core.ui.components.FinanceScaffold
import com.financemanage.mobile.core.ui.components.SectionHeader
import com.financemanage.mobile.core.ui.theme.Spacing
import com.financemanage.mobile.feature.settings.resources.Res
import com.financemanage.mobile.feature.settings.resources.app_lock
import com.financemanage.mobile.feature.settings.resources.appearance
import com.financemanage.mobile.feature.settings.resources.categories
import com.financemanage.mobile.feature.settings.resources.delete_account
import com.financemanage.mobile.feature.settings.resources.devices
import com.financemanage.mobile.feature.settings.resources.email_verification
import com.financemanage.mobile.feature.settings.resources.exchange_rates
import com.financemanage.mobile.feature.settings.resources.hide_balances
import com.financemanage.mobile.feature.settings.resources.later
import com.financemanage.mobile.feature.settings.resources.not_verified
import com.financemanage.mobile.feature.settings.resources.notifications
import com.financemanage.mobile.feature.settings.resources.outbox
import com.financemanage.mobile.feature.settings.resources.password
import com.financemanage.mobile.feature.settings.resources.profile
import com.financemanage.mobile.feature.settings.resources.recurring
import com.financemanage.mobile.feature.settings.resources.section_account
import com.financemanage.mobile.feature.settings.resources.section_app
import com.financemanage.mobile.feature.settings.resources.section_data
import com.financemanage.mobile.feature.settings.resources.section_security
import com.financemanage.mobile.feature.settings.resources.settings_title
import com.financemanage.mobile.feature.settings.resources.sign_out
import com.financemanage.mobile.feature.settings.resources.sign_out_confirm
import com.financemanage.mobile.feature.settings.resources.theme_dark
import com.financemanage.mobile.feature.settings.resources.theme_light
import com.financemanage.mobile.feature.settings.resources.theme_system
import com.financemanage.mobile.feature.settings.resources.turn_on
import com.financemanage.mobile.feature.settings.resources.two_factor
import com.financemanage.mobile.feature.settings.resources.two_factor_card_message
import com.financemanage.mobile.feature.settings.resources.two_factor_card_title
import com.financemanage.mobile.feature.settings.resources.two_factor_off
import com.financemanage.mobile.feature.settings.resources.two_factor_on
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Where the settings screen can go; every destination is a shared route handled by the app. */
enum class SettingsDestination {
    PROFILE,
    EMAIL,
    PASSWORD,
    TWO_FACTOR,
    TWO_FACTOR_SETUP,
    SESSIONS,
    NOTIFICATIONS,
    EXCHANGE_RATES,
    CATEGORIES,
    RECURRING,
    OUTBOX,
    APP_LOCK,
    DELETE_ACCOUNT,
}

@Composable
fun SettingsScreen(
    onNavigate: (SettingsDestination) -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmSignOut by remember { mutableStateOf(false) }
    val profile = state.profile.data
    com.financemanage.mobile.core.ui.components
        .RefreshOnResume(viewModel::refresh)

    FinanceScaffold(title = stringResource(Res.string.settings_title), onBack = onBack) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            item {
                ListItem(
                    headlineContent = {
                        Text(
                            profile?.fullName.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    supportingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Text(profile?.email.orEmpty())
                            if (profile != null && !profile.emailVerified) {
                                Text(stringResource(Res.string.not_verified), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    leadingContent = { Icon(Icons.Outlined.AccountCircle, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigate(SettingsDestination.PROFILE) },
                )
            }
            if (state.showTwoFactorCard) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                    ) {
                        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Text(
                                stringResource(Res.string.two_factor_card_title),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                stringResource(Res.string.two_factor_card_message),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(
                                    onClick = viewModel::snoozeTwoFactorCard,
                                ) { Text(stringResource(Res.string.later)) }
                                TextButton(onClick = { onNavigate(SettingsDestination.TWO_FACTOR_SETUP) }) {
                                    Text(stringResource(Res.string.turn_on))
                                }
                            }
                        }
                    }
                }
            }
            item { SectionHeader(stringResource(Res.string.section_account)) }
            item { NavRow(Icons.Outlined.Person, Res.string.profile) { onNavigate(SettingsDestination.PROFILE) } }
            if (profile != null && !profile.emailVerified) {
                item {
                    NavRow(
                        Icons.Outlined.Email,
                        Res.string.email_verification,
                    ) { onNavigate(SettingsDestination.EMAIL) }
                }
            }
            item { NavRow(Icons.Outlined.Lock, Res.string.password) { onNavigate(SettingsDestination.PASSWORD) } }

            item { SectionHeader(stringResource(Res.string.section_security)) }
            item {
                NavRow(
                    Icons.Outlined.Security,
                    Res.string.two_factor,
                    trailing =
                        if (profile?.twoFactorEnabled ==
                            true
                        ) {
                            Res.string.two_factor_on
                        } else {
                            Res.string.two_factor_off
                        },
                ) { onNavigate(SettingsDestination.TWO_FACTOR) }
            }
            item { NavRow(Icons.Outlined.Devices, Res.string.devices) { onNavigate(SettingsDestination.SESSIONS) } }
            item {
                NavRow(
                    Icons.Outlined.Fingerprint,
                    Res.string.app_lock,
                ) { onNavigate(SettingsDestination.APP_LOCK) }
            }

            item { SectionHeader(stringResource(Res.string.section_app)) }
            item {
                NavRow(
                    Icons.Outlined.Notifications,
                    Res.string.notifications,
                ) { onNavigate(SettingsDestination.NOTIFICATIONS) }
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.hide_balances)) },
                    leadingContent = { Icon(Icons.Outlined.VisibilityOff, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = state.preferences.hideBalances,
                            onCheckedChange = viewModel::setHideBalances,
                        )
                    },
                )
            }
            item {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(Res.string.appearance)) },
                        leadingContent = { Icon(Icons.Outlined.Palette, contentDescription = null) },
                    )
                    ThemeChooser(state.preferences.themeMode, viewModel::setTheme)
                }
            }

            item { SectionHeader(stringResource(Res.string.section_data)) }
            item {
                NavRow(
                    Icons.Outlined.Category,
                    Res.string.categories,
                ) { onNavigate(SettingsDestination.CATEGORIES) }
            }
            item { NavRow(Icons.Outlined.Repeat, Res.string.recurring) { onNavigate(SettingsDestination.RECURRING) } }
            item {
                NavRow(
                    Icons.Outlined.CurrencyExchange,
                    Res.string.exchange_rates,
                ) { onNavigate(SettingsDestination.EXCHANGE_RATES) }
            }
            if (state.pendingWrites > 0) {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(Res.string.outbox)) },
                        leadingContent = { Icon(Icons.Outlined.CloudUpload, contentDescription = null) },
                        trailingContent = { Text(state.pendingWrites.toString()) },
                        modifier = Modifier.clickable { onNavigate(SettingsDestination.OUTBOX) },
                    )
                }
            }
            item { HorizontalDivider(Modifier.padding(vertical = Spacing.sm)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.sign_out)) },
                    leadingContent = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
                    modifier = Modifier.clickable(enabled = !state.signingOut) { confirmSignOut = true },
                )
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(Res.string.delete_account),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Outlined.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    modifier = Modifier.clickable { onNavigate(SettingsDestination.DELETE_ACCOUNT) },
                )
            }
        }
    }
    if (confirmSignOut) {
        ConfirmDialog(
            title = stringResource(Res.string.sign_out),
            message = stringResource(Res.string.sign_out_confirm),
            onConfirm = {
                confirmSignOut = false
                viewModel.signOut()
            },
            onDismiss = { confirmSignOut = false },
        )
    }
}

@Composable
private fun NavRow(
    icon: ImageVector,
    label: StringResource,
    trailing: StringResource? = null,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(label)) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trailing != null) Text(stringResource(trailing), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun ThemeChooser(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val options =
        listOf(
            ThemeMode.SYSTEM to Res.string.theme_system,
            ThemeMode.LIGHT to Res.string.theme_light,
            ThemeMode.DARK to Res.string.theme_dark,
        )
    Column(Modifier.padding(start = Spacing.xl)) {
        options.forEach { (mode, label) ->
            Row(
                Modifier.fillMaxWidth().selectable(selected = mode == selected, onClick = { onSelect(mode) }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = mode == selected, onClick = { onSelect(mode) })
                Text(stringResource(label))
            }
        }
    }
}
