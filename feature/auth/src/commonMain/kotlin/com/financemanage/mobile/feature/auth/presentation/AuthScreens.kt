package com.financemanage.mobile.feature.auth.presentation

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.ErrorCodes
import com.financemanage.mobile.core.ui.components.FinanceScaffold
import com.financemanage.mobile.core.ui.components.FormColumn
import com.financemanage.mobile.core.ui.components.FormTextField
import com.financemanage.mobile.core.ui.components.LoadingButton
import com.financemanage.mobile.core.ui.components.OtpField
import com.financemanage.mobile.core.ui.components.PasswordField
import com.financemanage.mobile.core.ui.error.fieldError
import com.financemanage.mobile.core.ui.error.userMessage
import com.financemanage.mobile.core.ui.resources.action_cancel
import com.financemanage.mobile.core.ui.resources.action_save
import com.financemanage.mobile.core.ui.resources.field_required
import com.financemanage.mobile.core.ui.theme.Spacing
import com.financemanage.mobile.feature.auth.domain.model.AuthRules
import com.financemanage.mobile.feature.auth.resources.Res
import com.financemanage.mobile.feature.auth.resources.challenge_expired
import com.financemanage.mobile.feature.auth.resources.code_sent
import com.financemanage.mobile.feature.auth.resources.debug_server_address
import com.financemanage.mobile.feature.auth.resources.debug_server_current
import com.financemanage.mobile.feature.auth.resources.debug_server_default
import com.financemanage.mobile.feature.auth.resources.debug_server_hint
import com.financemanage.mobile.feature.auth.resources.debug_server_invalid
import com.financemanage.mobile.feature.auth.resources.debug_server_open
import com.financemanage.mobile.feature.auth.resources.debug_server_title
import com.financemanage.mobile.feature.auth.resources.debug_server_use_default
import com.financemanage.mobile.feature.auth.resources.email
import com.financemanage.mobile.feature.auth.resources.error_email_invalid
import com.financemanage.mobile.feature.auth.resources.error_email_taken
import com.financemanage.mobile.feature.auth.resources.error_google_disabled
import com.financemanage.mobile.feature.auth.resources.error_google_failed
import com.financemanage.mobile.feature.auth.resources.error_invalid_code
import com.financemanage.mobile.feature.auth.resources.error_password_length
import com.financemanage.mobile.feature.auth.resources.forgot_message
import com.financemanage.mobile.feature.auth.resources.forgot_password
import com.financemanage.mobile.feature.auth.resources.forgot_title
import com.financemanage.mobile.feature.auth.resources.full_name
import com.financemanage.mobile.feature.auth.resources.have_account
import com.financemanage.mobile.feature.auth.resources.new_password
import com.financemanage.mobile.feature.auth.resources.no_account
import com.financemanage.mobile.feature.auth.resources.or
import com.financemanage.mobile.feature.auth.resources.password
import com.financemanage.mobile.feature.auth.resources.password_hint
import com.financemanage.mobile.feature.auth.resources.recovery_code
import com.financemanage.mobile.feature.auth.resources.reset_code
import com.financemanage.mobile.feature.auth.resources.reset_password
import com.financemanage.mobile.feature.auth.resources.send_code
import com.financemanage.mobile.feature.auth.resources.sign_in
import com.financemanage.mobile.feature.auth.resources.sign_in_google
import com.financemanage.mobile.feature.auth.resources.sign_up
import com.financemanage.mobile.feature.auth.resources.two_factor_message
import com.financemanage.mobile.feature.auth.resources.two_factor_title
import com.financemanage.mobile.feature.auth.resources.two_factor_use_code
import com.financemanage.mobile.feature.auth.resources.two_factor_use_recovery
import com.financemanage.mobile.feature.auth.resources.verify
import com.financemanage.mobile.feature.auth.resources.welcome_subtitle
import com.financemanage.mobile.feature.auth.resources.welcome_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.financemanage.mobile.core.ui.resources.Res as CoreRes

/** Feature-specific error codes → messages. */
private val AuthErrorMessages: Map<String, StringResource> =
    mapOf(
        ErrorCodes.EMAIL_TAKEN to Res.string.error_email_taken,
        ErrorCodes.INVALID_CODE to Res.string.error_invalid_code,
        ErrorCodes.GOOGLE_SIGN_IN_DISABLED to Res.string.error_google_disabled,
    )

@Composable
private fun AppError.authMessage(): String = userMessage(AuthErrorMessages)

@Composable
private fun FieldProblem?.text(): String? =
    when (this) {
        null -> null
        FieldProblem.REQUIRED -> stringResource(CoreRes.string.field_required)
        FieldProblem.EMAIL_INVALID -> stringResource(Res.string.error_email_invalid)
        FieldProblem.PASSWORD_LENGTH -> stringResource(Res.string.error_password_length)
    }

@Composable
private fun ErrorText(text: String?) {
    if (text != null) {
        Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun WelcomeScreen(
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    serverSettings: ServerSettingsViewModel = koinViewModel(),
) {
    Box(Modifier.fillMaxSize()) {
        WelcomeContent(onSignIn = onSignIn, onSignUp = onSignUp)
        DebugServerSettings(serverSettings, Modifier.align(Alignment.BottomStart))
    }
}

@Composable
private fun WelcomeContent(
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.AccountBalanceWallet,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(Res.string.welcome_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(Res.string.welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.lg))
        Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.sign_in)) }
        OutlinedButton(
            onClick = onSignUp,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(Res.string.sign_up)) }
    }
}

/**
 * Debug builds only: an invisible target in the bottom-left corner; a long press opens a dialog to change the backend
 * address (e.g. the LAN IP of the Mac running the backend, for a real device). Nothing is shown in release builds.
 */
@Composable
private fun DebugServerSettings(
    viewModel: ServerSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val server = state.server
    if (!state.available || server == null) return
    val openLabel = stringResource(Res.string.debug_server_open)
    Box(
        modifier =
            modifier
                .size(DEBUG_TARGET_SIZE)
                .semantics { contentDescription = openLabel }
                .combinedClickable(
                    interactionSource = null,
                    indication = null,
                    onLongClickLabel = openLabel,
                    onLongClick = viewModel::open,
                    onClick = {},
                ),
        contentAlignment = Alignment.BottomStart,
    ) {
        // Only a hint while another backend is in use, so the developer knows where requests go.
        if (!server.isDefault) {
            Text(
                stringResource(Res.string.debug_server_current, server.baseUrl),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(Spacing.xs),
            )
        }
    }
    if (state.editing) {
        AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text(stringResource(Res.string.debug_server_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    FormTextField(
                        value = state.input,
                        onValueChange = viewModel::onInput,
                        label = stringResource(Res.string.debug_server_address),
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                        error = if (state.invalid) stringResource(Res.string.debug_server_invalid) else null,
                        supportingText = stringResource(Res.string.debug_server_hint),
                    )
                    Text(
                        stringResource(Res.string.debug_server_default, server.defaultBaseUrl),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::save) { Text(stringResource(CoreRes.string.action_save)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = viewModel::resetToDefault) {
                        Text(stringResource(Res.string.debug_server_use_default))
                    }
                    TextButton(onClick = viewModel::dismiss) { Text(stringResource(CoreRes.string.action_cancel)) }
                }
            },
        )
    }
}

private val DEBUG_TARGET_SIZE = 64.dp

@Composable
internal fun SignInScreen(
    googleWebClientId: String?,
    onTwoFactorRequired: (challengeToken: String, expiresInSeconds: Long) -> Unit,
    onForgotPassword: () -> Unit,
    onSignUp: () -> Unit,
    onBack: () -> Unit,
    viewModel: SignInViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val googleRequest = rememberGoogleIdTokenRequest(googleWebClientId)
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SignInEvent.TwoFactorRequired -> onTwoFactorRequired(event.challengeToken, event.expiresInSeconds)
            }
        }
    }
    FinanceScaffold(title = stringResource(Res.string.sign_in), onBack = onBack) { padding ->
        FormColumn(padding) {
            FormTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = stringResource(Res.string.email),
                keyboardType = KeyboardType.Email,
                error = state.emailProblem.text() ?: state.error.fieldError("email"),
            )
            PasswordField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = stringResource(Res.string.password),
                error = state.passwordProblem.text(),
            )
            ErrorText(state.error?.authMessage())
            LoadingButton(stringResource(Res.string.sign_in), onClick = viewModel::signIn, loading = state.loading)
            TextButton(onClick = onForgotPassword) { Text(stringResource(Res.string.forgot_password)) }
            if (googleRequest != null) {
                Text(
                    stringResource(Res.string.or),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { scope.launch { viewModel.signInWithGoogle(googleRequest()) } },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(Res.string.sign_in_google)) }
                if (state.googleFailed) ErrorText(stringResource(Res.string.error_google_failed))
            }
            TextButton(
                onClick = onSignUp,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(Res.string.no_account)) }
        }
    }
}

@Composable
internal fun SignUpScreen(
    onSignIn: () -> Unit,
    onBack: () -> Unit,
    viewModel: SignUpViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FinanceScaffold(title = stringResource(Res.string.sign_up), onBack = onBack) { padding ->
        FormColumn(padding) {
            FormTextField(
                value = state.fullName,
                onValueChange = viewModel::onNameChange,
                label = stringResource(Res.string.full_name),
                error = state.nameProblem.text() ?: state.error.fieldError("fullName"),
                maxLength = 120,
            )
            val emailTaken = (state.error as? AppError.Api)?.code == ErrorCodes.EMAIL_TAKEN
            FormTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = stringResource(Res.string.email),
                keyboardType = KeyboardType.Email,
                error =
                    state.emailProblem.text()
                        ?: (if (emailTaken) stringResource(Res.string.error_email_taken) else null)
                        ?: state.error.fieldError("email"),
            )
            PasswordField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = stringResource(Res.string.password),
                supportingText = stringResource(Res.string.password_hint),
                error = state.passwordProblem.text() ?: state.error.fieldError("password"),
            )
            if (!emailTaken) ErrorText(state.error?.authMessage())
            LoadingButton(stringResource(Res.string.sign_up), onClick = viewModel::signUp, loading = state.loading)
            TextButton(
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(Res.string.have_account)) }
        }
    }
}

@Composable
internal fun TwoFactorScreen(
    challengeToken: String,
    expiresInSeconds: Long,
    onExpired: () -> Unit,
    onBack: () -> Unit,
    viewModel: TwoFactorViewModel = koinViewModel { parametersOf(challengeToken, expiresInSeconds) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FinanceScaffold(title = stringResource(Res.string.two_factor_title), onBack = onBack) { padding ->
        FormColumn(padding) {
            if (state.expired) {
                ErrorText(stringResource(Res.string.challenge_expired))
                Button(
                    onClick = onExpired,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(Res.string.sign_in)) }
                return@FormColumn
            }
            Text(stringResource(Res.string.two_factor_message), style = MaterialTheme.typography.bodyLarge)
            if (state.useRecoveryCode) {
                FormTextField(
                    value = state.code,
                    onValueChange = viewModel::onCodeChange,
                    label = stringResource(Res.string.recovery_code),
                    imeAction = ImeAction.Done,
                )
            } else {
                OtpField(
                    value = state.code,
                    onValueChange = viewModel::onCodeChange,
                    isError = state.error != null,
                    onComplete = { viewModel.verify() },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            ErrorText(
                state.error?.let {
                    if ((it as? AppError.Api)?.isUnauthorized ==
                        true
                    ) {
                        stringResource(Res.string.error_invalid_code)
                    } else {
                        it.authMessage()
                    }
                },
            )
            LoadingButton(stringResource(Res.string.verify), onClick = viewModel::verify, loading = state.loading)
            TextButton(onClick = viewModel::toggleRecoveryCode, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(
                        if (state.useRecoveryCode) {
                            Res.string.two_factor_use_code
                        } else {
                            Res.string.two_factor_use_recovery
                        },
                    ),
                )
            }
        }
    }
}

@Composable
internal fun ForgotPasswordScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    FinanceScaffold(title = stringResource(Res.string.forgot_title), onBack = onBack) { padding ->
        FormColumn(padding) {
            Text(stringResource(Res.string.forgot_message), style = MaterialTheme.typography.bodyLarge)
            FormTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = stringResource(Res.string.email),
                keyboardType = KeyboardType.Email,
                enabled = !state.codeSent,
                error = state.emailProblem.text(),
            )
            if (!state.codeSent) {
                ErrorText(state.error?.authMessage())
                LoadingButton(
                    stringResource(Res.string.send_code),
                    onClick = viewModel::sendCode,
                    loading = state.loading,
                )
            } else {
                Text(stringResource(Res.string.code_sent), color = MaterialTheme.colorScheme.primary)
                FormTextField(
                    value = state.code,
                    onValueChange = { viewModel.onCodeChange(it.filter(Char::isDigit).take(AuthRules.CODE_LENGTH)) },
                    label = stringResource(Res.string.reset_code),
                    keyboardType = KeyboardType.NumberPassword,
                )
                PasswordField(
                    value = state.newPassword,
                    onValueChange = viewModel::onNewPasswordChange,
                    label = stringResource(Res.string.new_password),
                    supportingText = stringResource(Res.string.password_hint),
                    error = state.passwordProblem.text() ?: state.error.fieldError("newPassword"),
                )
                ErrorText(state.error?.authMessage())
                LoadingButton(
                    stringResource(Res.string.reset_password),
                    onClick = viewModel::reset,
                    loading = state.loading,
                )
            }
        }
    }
}
