package com.financemanage.mobile.feature.settings.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financemanage.mobile.core.data.settings.UserPreferences
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.CommonCurrencies
import com.financemanage.mobile.core.model.ErrorCodes
import com.financemanage.mobile.core.ui.components.ConfirmDialog
import com.financemanage.mobile.core.ui.components.FinanceScaffold
import com.financemanage.mobile.core.ui.components.FormColumn
import com.financemanage.mobile.core.ui.components.FormTextField
import com.financemanage.mobile.core.ui.components.LoadingButton
import com.financemanage.mobile.core.ui.components.OptionPicker
import com.financemanage.mobile.core.ui.components.OtpField
import com.financemanage.mobile.core.ui.components.PasswordField
import com.financemanage.mobile.core.ui.components.ResourceContent
import com.financemanage.mobile.core.ui.components.SwitchRow
import com.financemanage.mobile.core.ui.components.currencyLabel
import com.financemanage.mobile.core.ui.error.fieldError
import com.financemanage.mobile.core.ui.error.userMessage
import com.financemanage.mobile.core.ui.platform.rememberBiometricAuthenticator
import com.financemanage.mobile.core.ui.resources.action_save
import com.financemanage.mobile.core.ui.resources.field_required
import com.financemanage.mobile.core.ui.theme.Spacing
import com.financemanage.mobile.feature.settings.resources.Res
import com.financemanage.mobile.feature.settings.resources.app_lock
import com.financemanage.mobile.feature.settings.resources.app_lock_message
import com.financemanage.mobile.feature.settings.resources.app_lock_unavailable
import com.financemanage.mobile.feature.settings.resources.change_password
import com.financemanage.mobile.feature.settings.resources.code
import com.financemanage.mobile.feature.settings.resources.confirm_email
import com.financemanage.mobile.feature.settings.resources.copied
import com.financemanage.mobile.feature.settings.resources.copy
import com.financemanage.mobile.feature.settings.resources.current_password
import com.financemanage.mobile.feature.settings.resources.default_currency
import com.financemanage.mobile.feature.settings.resources.delete_account
import com.financemanage.mobile.feature.settings.resources.delete_account_warning
import com.financemanage.mobile.feature.settings.resources.delete_forever
import com.financemanage.mobile.feature.settings.resources.devices
import com.financemanage.mobile.feature.settings.resources.email_code_sent
import com.financemanage.mobile.feature.settings.resources.email_resend_in
import com.financemanage.mobile.feature.settings.resources.email_send_code
import com.financemanage.mobile.feature.settings.resources.email_verification
import com.financemanage.mobile.feature.settings.resources.email_verified
import com.financemanage.mobile.feature.settings.resources.error_confirmation_mismatch
import com.financemanage.mobile.feature.settings.resources.error_invalid_code
import com.financemanage.mobile.feature.settings.resources.error_password_length
import com.financemanage.mobile.feature.settings.resources.error_wrong_password
import com.financemanage.mobile.feature.settings.resources.full_name
import com.financemanage.mobile.feature.settings.resources.language
import com.financemanage.mobile.feature.settings.resources.language_en
import com.financemanage.mobile.feature.settings.resources.language_vi
import com.financemanage.mobile.feature.settings.resources.last_active
import com.financemanage.mobile.feature.settings.resources.minutes
import com.financemanage.mobile.feature.settings.resources.month_start_day
import com.financemanage.mobile.feature.settings.resources.new_password
import com.financemanage.mobile.feature.settings.resources.password_changed
import com.financemanage.mobile.feature.settings.resources.password_hint
import com.financemanage.mobile.feature.settings.resources.profile
import com.financemanage.mobile.feature.settings.resources.recovery_codes_message
import com.financemanage.mobile.feature.settings.resources.recovery_codes_title
import com.financemanage.mobile.feature.settings.resources.recovery_saved
import com.financemanage.mobile.feature.settings.resources.relock_after
import com.financemanage.mobile.feature.settings.resources.revoke
import com.financemanage.mobile.feature.settings.resources.revoke_others
import com.financemanage.mobile.feature.settings.resources.set_password
import com.financemanage.mobile.feature.settings.resources.this_device
import com.financemanage.mobile.feature.settings.resources.time_zone
import com.financemanage.mobile.feature.settings.resources.turn_on
import com.financemanage.mobile.feature.settings.resources.two_factor
import com.financemanage.mobile.feature.settings.resources.two_factor_card_message
import com.financemanage.mobile.feature.settings.resources.two_factor_disable
import com.financemanage.mobile.feature.settings.resources.two_factor_disable_message
import com.financemanage.mobile.feature.settings.resources.two_factor_enabled
import com.financemanage.mobile.feature.settings.resources.two_factor_secret
import com.financemanage.mobile.feature.settings.resources.two_factor_step_code
import com.financemanage.mobile.feature.settings.resources.two_factor_step_scan
import com.financemanage.mobile.feature.settings.resources.unknown_device
import com.financemanage.mobile.feature.settings.resources.verify
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.financemanage.mobile.core.ui.resources.Res as CoreRes

private val SettingsErrors: Map<String, StringResource> =
    mapOf(
        ErrorCodes.WRONG_PASSWORD to Res.string.error_wrong_password,
        ErrorCodes.INVALID_CODE to Res.string.error_invalid_code,
        "CONFIRMATION_MISMATCH" to Res.string.error_confirmation_mismatch,
    )

@Composable
private fun ErrorLine(error: AppError?) {
    error?.let { Text(it.userMessage(SettingsErrors), color = MaterialTheme.colorScheme.error) }
}

@Composable
private fun SettingsProblem?.text(): String? =
    when (this) {
        null -> null
        SettingsProblem.REQUIRED -> stringResource(CoreRes.string.field_required)
        SettingsProblem.PASSWORD_LENGTH -> stringResource(Res.string.error_password_length)
        SettingsProblem.MONTH_DAY -> "1–28"
    }

private val CommonTimeZones =
    listOf(
        "Asia/Ho_Chi_Minh",
        "Asia/Bangkok",
        "Asia/Singapore",
        "Asia/Tokyo",
        "Asia/Seoul",
        "Europe/London",
        "Europe/Paris",
        "America/New_York",
        "America/Los_Angeles",
        "Australia/Sydney",
        "UTC",
    )

@Composable
fun ProfileEditScreen(
    onBack: () -> Unit,
    viewModel: ProfileEditViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    FinanceScaffold(title = stringResource(Res.string.profile), onBack = onBack) { padding ->
        FormColumn(padding) {
            FormTextField(
                value = state.fullName,
                onValueChange = viewModel::onName,
                label = stringResource(Res.string.full_name),
                error =
                    (if (state.problem == SettingsProblem.REQUIRED) state.problem.text() else null)
                        ?: state.error.fieldError("fullName"),
                maxLength = 120,
            )
            OptionPicker(
                label = stringResource(Res.string.default_currency),
                options = (CommonCurrencies + state.currency).distinct(),
                selected = state.currency,
                optionLabel = { currencyLabel(it) },
                onSelect = viewModel::onCurrency,
            )
            OptionPicker(
                label = stringResource(Res.string.language),
                options = listOf("vi", "en"),
                selected = state.locale,
                optionLabel = { stringResource(if (it == "en") Res.string.language_en else Res.string.language_vi) },
                onSelect = viewModel::onLocale,
            )
            OptionPicker(
                label = stringResource(Res.string.time_zone),
                options = (CommonTimeZones + state.timeZone).distinct(),
                selected = state.timeZone,
                optionLabel = { it },
                onSelect = viewModel::onTimeZone,
            )
            FormTextField(
                value = state.monthStartDay,
                onValueChange = viewModel::onMonthStartDay,
                label = stringResource(Res.string.month_start_day),
                keyboardType = KeyboardType.Number,
                error =
                    if (state.problem ==
                        SettingsProblem.MONTH_DAY
                    ) {
                        state.problem.text()
                    } else {
                        state.error.fieldError("monthStartDay")
                    },
                supportingText = "1–28",
            )
            ErrorLine(state.error)
            LoadingButton(
                stringResource(CoreRes.string.action_save),
                onClick = viewModel::save,
                loading = state.saving,
                enabled = state.loaded,
            )
        }
    }
}

@Composable
fun EmailVerificationScreen(
    onBack: () -> Unit,
    viewModel: EmailVerificationViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FinanceScaffold(title = stringResource(Res.string.email_verification), onBack = onBack) { padding ->
        FormColumn(padding) {
            if (state.verified) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(stringResource(Res.string.email_verified))
                }
                return@FormColumn
            }
            Text(state.email, style = MaterialTheme.typography.titleMedium)
            if (state.codeSent) {
                Text(stringResource(Res.string.email_code_sent, state.email))
                OtpField(
                    value = state.code,
                    onValueChange = viewModel::onCode,
                    isError = state.error != null,
                    onComplete = { viewModel.verify() },
                )
                LoadingButton(stringResource(Res.string.verify), onClick = viewModel::verify, loading = state.loading)
            }
            ErrorLine(state.error)
            OutlinedButton(
                onClick = viewModel::sendCode,
                enabled = state.resendIn == 0 && !state.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (state.resendIn > 0) {
                        stringResource(Res.string.email_resend_in, state.resendIn)
                    } else {
                        stringResource(Res.string.email_send_code)
                    },
                )
            }
        }
    }
}

@Composable
fun PasswordScreen(
    onBack: () -> Unit,
    viewModel: PasswordViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val title = stringResource(if (state.hasPassword) Res.string.change_password else Res.string.set_password)
    FinanceScaffold(title = title, onBack = onBack) { padding ->
        FormColumn(padding) {
            if (state.done) {
                Text(stringResource(Res.string.password_changed), color = MaterialTheme.colorScheme.primary)
                return@FormColumn
            }
            if (state.hasPassword) {
                PasswordField(
                    value = state.current,
                    onValueChange = viewModel::onCurrent,
                    label = stringResource(Res.string.current_password),
                    error = if (state.problem == SettingsProblem.REQUIRED) state.problem.text() else null,
                )
            }
            PasswordField(
                value = state.new,
                onValueChange = viewModel::onNew,
                label = stringResource(Res.string.new_password),
                supportingText = stringResource(Res.string.password_hint),
                error =
                    if (state.problem ==
                        SettingsProblem.PASSWORD_LENGTH
                    ) {
                        state.problem.text()
                    } else {
                        state.error.fieldError("newPassword")
                    },
            )
            ErrorLine(state.error)
            LoadingButton(
                stringResource(CoreRes.string.action_save),
                onClick = viewModel::save,
                loading = state.loading,
            )
        }
    }
}

/**
 * 2FA settings: enable flow when off, disable form when on.
 * @param startSetupImmediately open directly on the QR step (from the suggestion card or onboarding).
 * @param onFinished called when the user leaves (back, or after confirming the recovery codes when
 *   [startSetupImmediately]).
 */
@Composable
fun TwoFactorScreen(
    onFinished: () -> Unit,
    startSetupImmediately: Boolean = false,
    viewModel: TwoFactorViewModel =
        koinViewModel(key = "2fa-$startSetupImmediately") { parametersOf(startSetupImmediately) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    var copiedNotice by remember { mutableStateOf(false) }
    FinanceScaffold(title = stringResource(Res.string.two_factor), onBack = onFinished) { padding ->
        FormColumn(padding) {
            when (state.stage) {
                TwoFactorStage.LOADING -> Unit
                TwoFactorStage.OFF_INTRO -> {
                    Text(stringResource(Res.string.two_factor_card_message))
                    ErrorLine(state.error)
                    LoadingButton(
                        stringResource(Res.string.turn_on),
                        onClick = viewModel::startSetup,
                        loading = state.loading,
                    )
                }
                TwoFactorStage.SETUP -> {
                    val setup = state.setup ?: return@FormColumn
                    Text(stringResource(Res.string.two_factor_step_scan))
                    Image(
                        painter = rememberQrCodePainter(setup.otpauthUri),
                        contentDescription = null,
                        modifier = Modifier.size(200.dp).align(Alignment.CenterHorizontally),
                    )
                    Text(stringResource(Res.string.two_factor_secret), style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(setup.secret, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(setup.secret))
                            copiedNotice = true
                        }) { Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(Res.string.copy)) }
                    }
                    if (copiedNotice) {
                        Text(
                            stringResource(Res.string.copied),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(stringResource(Res.string.two_factor_step_code))
                    OtpField(
                        value = state.code,
                        onValueChange = viewModel::onCode,
                        isError = state.error != null,
                        onComplete = { viewModel.enable() },
                    )
                    ErrorLine(state.error)
                    LoadingButton(
                        stringResource(Res.string.turn_on),
                        onClick = viewModel::enable,
                        loading = state.loading,
                    )
                }
                TwoFactorStage.RECOVERY_CODES -> {
                    Text(stringResource(Res.string.recovery_codes_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(Res.string.recovery_codes_message))
                    Column(Modifier.fillMaxWidth().padding(vertical = Spacing.sm)) {
                        state.recoveryCodes.forEach { Text(it, fontFamily = FontFamily.Monospace) }
                    }
                    OutlinedButton(onClick = {
                        clipboard.setText(AnnotatedString(state.recoveryCodes.joinToString("\n")))
                        copiedNotice = true
                    }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.copy)) }
                    if (copiedNotice) {
                        Text(
                            stringResource(Res.string.copied),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = state.savedConfirmed, onCheckedChange = viewModel::onSavedConfirmed)
                        Text(stringResource(Res.string.recovery_saved))
                    }
                    LoadingButton(
                        text = stringResource(CoreRes.string.action_save),
                        onClick = {
                            viewModel.finishRecoveryCodes()
                            if (startSetupImmediately) onFinished()
                        },
                        enabled = state.savedConfirmed,
                    )
                }
                TwoFactorStage.ON -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            stringResource(Res.string.two_factor_enabled),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Text(stringResource(Res.string.two_factor_disable_message))
                    FormTextField(
                        value = state.code,
                        onValueChange = viewModel::onCode,
                        label = stringResource(Res.string.code),
                    )
                    ErrorLine(state.error)
                    LoadingButton(
                        stringResource(Res.string.two_factor_disable),
                        onClick = viewModel::disable,
                        loading = state.loading,
                    )
                }
            }
        }
    }
}

@Composable
fun SessionsScreen(
    onBack: () -> Unit,
    viewModel: SessionsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    com.financemanage.mobile.core.ui.components
        .RefreshOnResume(viewModel::refresh)
    FinanceScaffold(title = stringResource(Res.string.devices), onBack = onBack) { padding ->
        Column(Modifier.padding(padding)) {
            ErrorLine(state.actionError)
            ResourceContent(
                resource = state.sessions,
                onRefresh = viewModel::refresh,
                modifier = Modifier.weight(1f),
            ) { sessions ->
                LazyColumn {
                    items(sessions, key = { it.id }) { session ->
                        val lastActive = session.lastActiveAt?.toLocalDateTime(TimeZone.currentSystemDefault())
                        ListItem(
                            headlineContent = { Text(session.deviceName ?: stringResource(Res.string.unknown_device)) },
                            supportingContent = {
                                Text(
                                    if (session.current) {
                                        stringResource(Res.string.this_device)
                                    } else {
                                        stringResource(
                                            Res.string.last_active,
                                            lastActive
                                                ?.let { "${it.date.dayOfMonth}/${it.date.monthNumber}/${it.date.year}" }
                                                .orEmpty(),
                                        )
                                    },
                                )
                            },
                            leadingContent = { Icon(Icons.Outlined.PhoneAndroid, contentDescription = null) },
                            trailingContent = {
                                if (!session.current) {
                                    TextButton(
                                        onClick = { viewModel.revoke(session.id) },
                                    ) { Text(stringResource(Res.string.revoke)) }
                                }
                            },
                        )
                    }
                    if (sessions.count { !it.current } > 1) {
                        item {
                            OutlinedButton(
                                onClick = viewModel::revokeOthers,
                                modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                            ) {
                                Text(stringResource(Res.string.revoke_others))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** App lock settings; the lock itself is enforced by the app shell. */
@Composable
fun AppLockScreen(
    preferences: UserPreferences,
    onChange: (enabled: Boolean, minutes: Int) -> Unit,
    onBack: () -> Unit,
) {
    val authenticator = rememberBiometricAuthenticator()
    val scope = rememberCoroutineScope()
    val reason = stringResource(Res.string.app_lock)
    FinanceScaffold(title = stringResource(Res.string.app_lock), onBack = onBack) { padding ->
        FormColumn(padding) {
            if (!authenticator.isAvailable) {
                Text(stringResource(Res.string.app_lock_unavailable), color = MaterialTheme.colorScheme.error)
            }
            SwitchRow(
                label = stringResource(Res.string.app_lock),
                supportingText = stringResource(Res.string.app_lock_message),
                checked = preferences.appLockEnabled,
                onCheckedChange = { enable ->
                    // Confirm with the system prompt before turning the lock on, so users cannot lock themselves out.
                    if (!enable) {
                        onChange(false, preferences.relockAfterMinutes)
                    } else if (authenticator.isAvailable) {
                        scope.launch {
                            if (authenticator.authenticate(
                                    reason,
                                )
                            ) {
                                onChange(true, preferences.relockAfterMinutes)
                            }
                        }
                    }
                },
            )
            if (preferences.appLockEnabled) {
                OptionPicker(
                    label = stringResource(Res.string.relock_after),
                    options = listOf(1, 5, 15),
                    selected = preferences.relockAfterMinutes,
                    optionLabel = { stringResource(Res.string.minutes, it) },
                    onSelect = { onChange(true, it) },
                )
            }
        }
    }
}

@Composable
fun DeleteAccountScreen(
    onBack: () -> Unit,
    viewModel: DeleteAccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf(false) }
    val canDelete =
        state.confirmEmail.trim().equals(state.email, ignoreCase = true) &&
            (!state.hasPassword || state.password.isNotEmpty())
    FinanceScaffold(title = stringResource(Res.string.delete_account), onBack = onBack) { padding ->
        FormColumn(padding) {
            Text(stringResource(Res.string.delete_account_warning), color = MaterialTheme.colorScheme.error)
            FormTextField(
                value = state.confirmEmail,
                onValueChange = viewModel::onConfirmEmail,
                label = stringResource(Res.string.confirm_email),
                keyboardType = KeyboardType.Email,
                supportingText = state.email,
            )
            if (state.hasPassword) {
                PasswordField(
                    value = state.password,
                    onValueChange = viewModel::onPassword,
                    label = stringResource(Res.string.current_password),
                )
            }
            ErrorLine(state.error)
            LoadingButton(stringResource(Res.string.delete_forever), onClick = {
                confirm = true
            }, loading = state.loading, enabled = canDelete)
        }
    }
    if (confirm) {
        ConfirmDialog(
            title = stringResource(Res.string.delete_account),
            message = stringResource(Res.string.delete_account_warning),
            confirmLabel = stringResource(Res.string.delete_forever),
            onConfirm = {
                confirm = false
                viewModel.delete()
            },
            onDismiss = { confirm = false },
        )
    }
}
