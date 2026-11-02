package com.financemanage.mobile.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.feature.auth.domain.AuthRepository
import com.financemanage.mobile.feature.auth.domain.AuthRules
import com.financemanage.mobile.feature.auth.domain.LoginResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/** Local validation problems, translated by the screens. */
enum class FieldProblem { REQUIRED, EMAIL_INVALID, PASSWORD_LENGTH }

// ---------------------------------------------------------------- Sign in

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val emailProblem: FieldProblem? = null,
    val passwordProblem: FieldProblem? = null,
    val loading: Boolean = false,
    val error: AppError? = null,
    val googleFailed: Boolean = false,
)

sealed interface SignInEvent {
    data class TwoFactorRequired(
        val challengeToken: String,
        val expiresInSeconds: Long,
    ) : SignInEvent
}

class SignInViewModel(
    private val repository: AuthRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = mutableState.asStateFlow()
    private val eventChannel = Channel<SignInEvent>(Channel.BUFFERED)
    val events: Flow<SignInEvent> = eventChannel.receiveAsFlow()

    fun onEmailChange(value: String) = mutableState.update { it.copy(email = value, emailProblem = null, error = null) }

    fun onPasswordChange(value: String) =
        mutableState.update {
            it.copy(password = value, passwordProblem = null, error = null)
        }

    fun signIn() {
        val current = mutableState.value
        val emailProblem = if (!AuthRules.isEmailValid(current.email)) FieldProblem.EMAIL_INVALID else null
        val passwordProblem = if (current.password.isEmpty()) FieldProblem.REQUIRED else null
        if (emailProblem != null || passwordProblem != null) {
            mutableState.update { it.copy(emailProblem = emailProblem, passwordProblem = passwordProblem) }
            return
        }
        run { repository.login(current.email, current.password) }
    }

    /** [idToken] is `null` when the user cancelled or Google returned nothing. */
    fun signInWithGoogle(idToken: String?) {
        if (idToken == null) {
            mutableState.update { it.copy(googleFailed = true) }
            return
        }
        run { repository.loginWithGoogle(idToken) }
    }

    private fun run(call: suspend () -> AppResult<LoginResult>) {
        if (mutableState.value.loading) return
        mutableState.update { it.copy(loading = true, error = null, googleFailed = false) }
        viewModelScope.launch {
            when (val result = call()) {
                is AppResult.Failure -> mutableState.update { it.copy(loading = false, error = result.error) }
                is AppResult.Success -> {
                    mutableState.update { it.copy(loading = false) }
                    val value = result.value
                    if (value is LoginResult.TwoFactorRequired) {
                        eventChannel.send(SignInEvent.TwoFactorRequired(value.challengeToken, value.expiresInSeconds))
                    }
                    // SignedIn: the session state changes and the app leaves the auth flow.
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Sign up

data class SignUpUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val nameProblem: FieldProblem? = null,
    val emailProblem: FieldProblem? = null,
    val passwordProblem: FieldProblem? = null,
    val loading: Boolean = false,
    val error: AppError? = null,
)

class SignUpViewModel(
    private val repository: AuthRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = mutableState.asStateFlow()

    fun onNameChange(value: String) =
        mutableState.update { it.copy(fullName = value, nameProblem = null, error = null) }

    fun onEmailChange(value: String) = mutableState.update { it.copy(email = value, emailProblem = null, error = null) }

    fun onPasswordChange(value: String) =
        mutableState.update {
            it.copy(password = value, passwordProblem = null, error = null)
        }

    fun signUp() {
        val current = mutableState.value
        val nameProblem = if (current.fullName.isBlank()) FieldProblem.REQUIRED else null
        val emailProblem = if (!AuthRules.isEmailValid(current.email)) FieldProblem.EMAIL_INVALID else null
        val passwordProblem = if (!AuthRules.isPasswordValid(current.password)) FieldProblem.PASSWORD_LENGTH else null
        if (nameProblem != null || emailProblem != null || passwordProblem != null) {
            mutableState.update {
                it.copy(nameProblem = nameProblem, emailProblem = emailProblem, passwordProblem = passwordProblem)
            }
            return
        }
        if (current.loading) return
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = repository.register(current.fullName, current.email, current.password)
            mutableState.update { it.copy(loading = false, error = (result as? AppResult.Failure)?.error) }
        }
    }
}

// ---------------------------------------------------------------- Two-factor

data class TwoFactorUiState(
    val code: String = "",
    val useRecoveryCode: Boolean = false,
    val loading: Boolean = false,
    val error: AppError? = null,
    val expired: Boolean = false,
)

class TwoFactorViewModel(
    private val repository: AuthRepository,
    private val challengeToken: String,
    expiresInSeconds: Long,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TwoFactorUiState())
    val state: StateFlow<TwoFactorUiState> = mutableState.asStateFlow()
    private var verifyJob: Job? = null

    init {
        viewModelScope.launch {
            delay(expiresInSeconds.seconds)
            mutableState.update { it.copy(expired = true) }
        }
    }

    fun onCodeChange(value: String) = mutableState.update { it.copy(code = value, error = null) }

    fun toggleRecoveryCode() =
        mutableState.update {
            it.copy(useRecoveryCode = !it.useRecoveryCode, code = "", error = null)
        }

    fun verify() {
        val current = mutableState.value
        val busy = current.loading || verifyJob?.isActive == true
        if (current.code.isBlank() || current.expired || busy) return
        mutableState.update { it.copy(loading = true, error = null) }
        verifyJob =
            viewModelScope.launch {
                val result = repository.verifyTwoFactor(challengeToken, current.code)
                val error = (result as? AppResult.Failure)?.error
                // A 401 here means a wrong code; after 5 wrong codes the challenge expires (API §4.1).
                mutableState.update {
                    it.copy(
                        loading = false,
                        error = error,
                        code =
                            if (error !=
                                null
                            ) {
                                ""
                            } else {
                                it.code
                            },
                    )
                }
            }
    }
}

// ---------------------------------------------------------------- Forgot password

data class ForgotPasswordUiState(
    val email: String = "",
    val codeSent: Boolean = false,
    val code: String = "",
    val newPassword: String = "",
    val emailProblem: FieldProblem? = null,
    val passwordProblem: FieldProblem? = null,
    val loading: Boolean = false,
    val error: AppError? = null,
    val done: Boolean = false,
)

class ForgotPasswordViewModel(
    private val repository: AuthRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = mutableState.asStateFlow()

    fun onEmailChange(value: String) = mutableState.update { it.copy(email = value, emailProblem = null, error = null) }

    fun onCodeChange(value: String) = mutableState.update { it.copy(code = value, error = null) }

    fun onNewPasswordChange(value: String) =
        mutableState.update {
            it.copy(newPassword = value, passwordProblem = null, error = null)
        }

    fun sendCode() {
        val current = mutableState.value
        if (!AuthRules.isEmailValid(current.email)) {
            mutableState.update { it.copy(emailProblem = FieldProblem.EMAIL_INVALID) }
            return
        }
        launchCall({ repository.forgotPassword(current.email) }) { it.copy(codeSent = true) }
    }

    fun reset() {
        val current = mutableState.value
        if (!AuthRules.isPasswordValid(current.newPassword)) {
            mutableState.update { it.copy(passwordProblem = FieldProblem.PASSWORD_LENGTH) }
            return
        }
        if (current.code.length != AuthRules.CODE_LENGTH) return
        launchCall(
            { repository.resetPassword(current.email, current.code, current.newPassword) },
        ) { it.copy(done = true) }
    }

    private fun launchCall(
        call: suspend () -> AppResult<Unit>,
        onSuccess: (ForgotPasswordUiState) -> ForgotPasswordUiState,
    ) {
        if (mutableState.value.loading) return
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = call()) {
                is AppResult.Success -> mutableState.update { onSuccess(it.copy(loading = false)) }
                is AppResult.Failure -> mutableState.update { it.copy(loading = false, error = result.error) }
            }
        }
    }
}
