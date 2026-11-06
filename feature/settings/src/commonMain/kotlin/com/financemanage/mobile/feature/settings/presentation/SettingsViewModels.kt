package com.financemanage.mobile.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.domain.model.ThemeMode
import com.financemanage.mobile.core.domain.model.UpdateProfileRequest
import com.financemanage.mobile.core.domain.model.UserPreferences
import com.financemanage.mobile.core.domain.usecase.GetCachedProfileUseCase
import com.financemanage.mobile.core.domain.usecase.GetProfileUseCase
import com.financemanage.mobile.core.domain.usecase.HideTwoFactorCardUseCase
import com.financemanage.mobile.core.domain.usecase.ObservePendingWritesUseCase
import com.financemanage.mobile.core.domain.usecase.ObservePreferencesUseCase
import com.financemanage.mobile.core.domain.usecase.ObserveProfileUseCase
import com.financemanage.mobile.core.domain.usecase.RefreshProfileUseCase
import com.financemanage.mobile.core.domain.usecase.SetAppLockUseCase
import com.financemanage.mobile.core.domain.usecase.SetHideBalancesUseCase
import com.financemanage.mobile.core.domain.usecase.SetThemeModeUseCase
import com.financemanage.mobile.core.domain.usecase.SignOutUseCase
import com.financemanage.mobile.core.domain.usecase.UpdateProfileUseCase
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.domain.Profile
import com.financemanage.mobile.core.ui.state.Resource
import com.financemanage.mobile.core.ui.state.ResourceHolder
import com.financemanage.mobile.core.ui.state.memoryResource
import com.financemanage.mobile.feature.settings.domain.model.DeviceSession
import com.financemanage.mobile.feature.settings.domain.model.TwoFactorSetup
import com.financemanage.mobile.feature.settings.domain.usecase.ChangePasswordUseCase
import com.financemanage.mobile.feature.settings.domain.usecase.DeleteAccountUseCase
import com.financemanage.mobile.feature.settings.domain.usecase.DisableTwoFactorUseCase
import com.financemanage.mobile.feature.settings.domain.usecase.EnableTwoFactorUseCase
import com.financemanage.mobile.feature.settings.domain.usecase.GetDeviceSessionsUseCase
import com.financemanage.mobile.feature.settings.domain.usecase.RevokeDeviceSessionUseCase
import com.financemanage.mobile.feature.settings.domain.usecase.RevokeOtherDeviceSessionsUseCase
import com.financemanage.mobile.feature.settings.domain.usecase.SendEmailVerificationCodeUseCase
import com.financemanage.mobile.feature.settings.domain.usecase.SetupTwoFactorUseCase
import com.financemanage.mobile.feature.settings.domain.usecase.VerifyEmailUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days

/** Local validation problem shared by the settings forms. */
enum class SettingsProblem { REQUIRED, PASSWORD_LENGTH, MONTH_DAY }

private const val PASSWORD_MIN = 8
private const val PASSWORD_MAX = 72
private const val RESEND_SECONDS = 60
private val TWO_FACTOR_CARD_SNOOZE = 30.days

// ---------------------------------------------------------------- Settings home

data class SettingsUiState(
    val profile: Resource<Profile> = Resource(loading = true),
    val preferences: UserPreferences = UserPreferences(),
    val pendingWrites: Int = 0,
    val signingOut: Boolean = false,
) {
    /** "Make your account safer" card: only while 2FA is off and not snoozed (F04). */
    val showTwoFactorCard: Boolean
        get() {
            val profile = profile.data ?: return false
            val hiddenUntil = preferences.twoFactorCardHiddenUntil ?: 0
            return !profile.twoFactorEnabled && Clock.System.now().toEpochMilliseconds() >= hiddenUntil
        }
}

class SettingsViewModel(
    observeProfile: ObserveProfileUseCase,
    refreshProfile: RefreshProfileUseCase,
    observePreferences: ObservePreferencesUseCase,
    observePendingWrites: ObservePendingWritesUseCase,
    private val hideTwoFactorCard: HideTwoFactorCardUseCase,
    private val setHideBalancesUseCase: SetHideBalancesUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val setAppLockUseCase: SetAppLockUseCase,
    private val signOutUseCase: SignOutUseCase,
) : ViewModel() {
    private val profileHolder = ResourceHolder(viewModelScope, observeProfile()) { refreshProfile() }
    private val signingOut = MutableStateFlow(false)

    val state: StateFlow<SettingsUiState> =
        combine(
            profileHolder.state,
            observePreferences(),
            observePendingWrites(),
            signingOut,
        ) { profile, prefs, outbox, out ->
            SettingsUiState(profile, prefs, outbox.size, out)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), SettingsUiState())

    fun refresh() = profileHolder.refresh()

    fun snoozeTwoFactorCard() {
        viewModelScope.launch {
            hideTwoFactorCard((Clock.System.now() + TWO_FACTOR_CARD_SNOOZE).toEpochMilliseconds())
        }
    }

    fun setHideBalances(hide: Boolean) {
        viewModelScope.launch { setHideBalancesUseCase(hide) }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { setThemeMode(mode) }
    }

    fun setAppLock(
        enabled: Boolean,
        minutes: Int,
    ) {
        viewModelScope.launch { setAppLockUseCase(enabled, minutes) }
    }

    fun signOut() {
        if (signingOut.value) return
        signingOut.value = true
        viewModelScope.launch { signOutUseCase() }
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}

// ---------------------------------------------------------------- App lock

/** App lock settings (F04): fingerprint / Face ID when returning to the app, and the re-lock delay. */
class AppLockViewModel(
    observePreferences: ObservePreferencesUseCase,
    private val setAppLockUseCase: SetAppLockUseCase,
) : ViewModel() {
    val preferences: StateFlow<UserPreferences> =
        observePreferences().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), UserPreferences())

    fun setAppLock(
        enabled: Boolean,
        minutes: Int,
    ) {
        viewModelScope.launch { setAppLockUseCase(enabled, minutes) }
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}

// ---------------------------------------------------------------- Profile edit

data class ProfileEditUiState(
    val loaded: Boolean = false,
    val fullName: String = "",
    val currency: CurrencyCode = CurrencyCode.DEFAULT,
    val locale: String = "vi",
    val timeZone: String = "Asia/Ho_Chi_Minh",
    val monthStartDay: String = "1",
    val problem: SettingsProblem? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: AppError? = null,
)

class ProfileEditViewModel(
    getProfile: GetProfileUseCase,
    private val updateProfile: UpdateProfileUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileEditUiState())
    val state: StateFlow<ProfileEditUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = getProfile()
            if (profile != null) {
                mutableState.update {
                    it.copy(
                        loaded = true,
                        fullName = profile.fullName,
                        currency = profile.defaultCurrency,
                        locale = profile.locale,
                        timeZone = profile.timeZone,
                        monthStartDay = profile.monthStartDay.toString(),
                    )
                }
            } else {
                mutableState.update { it.copy(loaded = true) }
            }
        }
    }

    fun onName(value: String) = mutableState.update { it.copy(fullName = value, problem = null) }

    fun onCurrency(value: CurrencyCode) = mutableState.update { it.copy(currency = value) }

    fun onLocale(value: String) = mutableState.update { it.copy(locale = value) }

    fun onTimeZone(value: String) = mutableState.update { it.copy(timeZone = value) }

    fun onMonthStartDay(value: String) =
        mutableState.update {
            it.copy(monthStartDay = value.filter(Char::isDigit).take(2), problem = null)
        }

    fun save() {
        val current = mutableState.value
        val day = current.monthStartDay.toIntOrNull()
        val problem =
            when {
                current.fullName.isBlank() -> SettingsProblem.REQUIRED
                day == null || day !in 1..MAX_MONTH_START_DAY -> SettingsProblem.MONTH_DAY
                else -> null
            }
        if (problem != null) {
            mutableState.update { it.copy(problem = problem) }
            return
        }
        mutableState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val result =
                updateProfile(
                    UpdateProfileRequest(
                        fullName = current.fullName.trim(),
                        defaultCurrency = current.currency.value,
                        locale = current.locale,
                        timeZone = current.timeZone.trim(),
                        monthStartDay = day,
                    ),
                )
            mutableState.update {
                it.copy(
                    saving = false,
                    saved = result is AppResult.Success,
                    error = (result as? AppResult.Failure)?.error,
                )
            }
        }
    }

    companion object {
        const val MAX_MONTH_START_DAY = 28
    }
}

// ---------------------------------------------------------------- E-mail verification

data class EmailVerificationUiState(
    val email: String = "",
    val verified: Boolean = false,
    val codeSent: Boolean = false,
    val resendIn: Int = 0,
    val code: String = "",
    val loading: Boolean = false,
    val error: AppError? = null,
)

class EmailVerificationViewModel(
    private val sendEmailVerificationCode: SendEmailVerificationCodeUseCase,
    private val verifyEmail: VerifyEmailUseCase,
    observeProfile: ObserveProfileUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(EmailVerificationUiState())
    val state: StateFlow<EmailVerificationUiState> = mutableState.asStateFlow()
    private var countdown: Job? = null

    init {
        viewModelScope.launch {
            observeProfile().map { it?.value }.first { it != null }?.let { profile ->
                mutableState.update { it.copy(email = profile.email, verified = profile.emailVerified) }
            }
        }
    }

    fun onCode(value: String) = mutableState.update { it.copy(code = value, error = null) }

    fun sendCode() {
        if (mutableState.value.resendIn > 0 || mutableState.value.loading) return
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = sendEmailVerificationCode()) {
                is AppResult.Success -> {
                    mutableState.update { it.copy(loading = false, codeSent = true) }
                    startCountdown()
                }
                is AppResult.Failure -> mutableState.update { it.copy(loading = false, error = result.error) }
            }
        }
    }

    fun verify() {
        val code = mutableState.value.code
        if (code.length != CODE_LENGTH || mutableState.value.loading) return
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = verifyEmail(code)) {
                is AppResult.Success -> mutableState.update { it.copy(loading = false, verified = true) }
                is AppResult.Failure ->
                    mutableState.update {
                        it.copy(
                            loading = false,
                            error = result.error,
                            code = "",
                        )
                    }
            }
        }
    }

    private fun startCountdown() {
        countdown?.cancel()
        countdown =
            viewModelScope.launch {
                for (seconds in RESEND_SECONDS downTo 0) {
                    mutableState.update { it.copy(resendIn = seconds) }
                    if (seconds > 0) delay(ONE_SECOND_MS)
                }
            }
    }

    private companion object {
        const val CODE_LENGTH = 6
        const val ONE_SECOND_MS = 1_000L
    }
}

// ---------------------------------------------------------------- Password

data class PasswordUiState(
    val hasPassword: Boolean = true,
    val current: String = "",
    val new: String = "",
    val problem: SettingsProblem? = null,
    val loading: Boolean = false,
    val done: Boolean = false,
    val error: AppError? = null,
)

class PasswordViewModel(
    private val changePassword: ChangePasswordUseCase,
    getCachedProfile: GetCachedProfileUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PasswordUiState())
    val state: StateFlow<PasswordUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val hasPassword = getCachedProfile()?.hasPassword ?: true
            mutableState.update { it.copy(hasPassword = hasPassword) }
        }
    }

    fun onCurrent(value: String) = mutableState.update { it.copy(current = value, error = null) }

    fun onNew(value: String) = mutableState.update { it.copy(new = value, problem = null, error = null) }

    fun save() {
        val current = mutableState.value
        if (current.new.length !in PASSWORD_MIN..PASSWORD_MAX) {
            mutableState.update { it.copy(problem = SettingsProblem.PASSWORD_LENGTH) }
            return
        }
        if (current.hasPassword && current.current.isEmpty()) {
            mutableState.update { it.copy(problem = SettingsProblem.REQUIRED) }
            return
        }
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = changePassword(current.current.takeIf { current.hasPassword }, current.new)
            mutableState.update {
                it.copy(
                    loading = false,
                    done = result is AppResult.Success,
                    error = (result as? AppResult.Failure)?.error,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Two-factor

enum class TwoFactorStage { LOADING, OFF_INTRO, SETUP, RECOVERY_CODES, ON }

data class TwoFactorUiState(
    val stage: TwoFactorStage = TwoFactorStage.LOADING,
    val setup: TwoFactorSetup? = null,
    val code: String = "",
    val recoveryCodes: List<String> = emptyList(),
    val savedConfirmed: Boolean = false,
    val loading: Boolean = false,
    val error: AppError? = null,
)

/**
 * 2FA (F04): optional and off by default. Enabling = setup (QR + key) → code → 10 recovery codes with a required
 * "I saved them". Cancelling half-way leaves 2FA off (the backend only enables it after a valid code).
 */
class TwoFactorViewModel(
    private val setupTwoFactor: SetupTwoFactorUseCase,
    private val enableTwoFactor: EnableTwoFactorUseCase,
    private val disableTwoFactor: DisableTwoFactorUseCase,
    getProfile: GetProfileUseCase,
    startSetupImmediately: Boolean,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TwoFactorUiState())
    val state: StateFlow<TwoFactorUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val enabled = getProfile()?.twoFactorEnabled ?: false
            mutableState.update { it.copy(stage = if (enabled) TwoFactorStage.ON else TwoFactorStage.OFF_INTRO) }
            if (!enabled && startSetupImmediately) startSetup()
        }
    }

    fun onCode(value: String) = mutableState.update { it.copy(code = value, error = null) }

    fun startSetup() =
        call({
            setupTwoFactor()
        }) { state, setup -> state.copy(stage = TwoFactorStage.SETUP, setup = setup, code = "") }

    fun enable() {
        if (mutableState.value.code.length != CODE_LENGTH) return
        call({ enableTwoFactor(mutableState.value.code) }) { state, codes ->
            state.copy(stage = TwoFactorStage.RECOVERY_CODES, recoveryCodes = codes, code = "")
        }
    }

    fun onSavedConfirmed(value: Boolean) = mutableState.update { it.copy(savedConfirmed = value) }

    fun finishRecoveryCodes() = mutableState.update { it.copy(stage = TwoFactorStage.ON, recoveryCodes = emptyList()) }

    fun disable() {
        if (mutableState.value.code.isBlank()) return
        call({
            disableTwoFactor(mutableState.value.code.trim())
        }) { state, _ -> state.copy(stage = TwoFactorStage.OFF_INTRO, code = "") }
    }

    private fun <T> call(
        block: suspend () -> AppResult<T>,
        onSuccess: (TwoFactorUiState, T) -> TwoFactorUiState,
    ) {
        if (mutableState.value.loading) return
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = block()) {
                is AppResult.Success -> mutableState.update { onSuccess(it.copy(loading = false), result.value) }
                is AppResult.Failure ->
                    mutableState.update {
                        it.copy(
                            loading = false,
                            error = result.error,
                            code = "",
                        )
                    }
            }
        }
    }

    private companion object {
        const val CODE_LENGTH = 6
    }
}

// ---------------------------------------------------------------- Sessions

data class SessionsUiState(
    val sessions: Resource<List<DeviceSession>> = Resource(loading = true),
    val actionError: AppError? = null,
)

class SessionsViewModel(
    getDeviceSessions: GetDeviceSessionsUseCase,
    private val revokeDeviceSession: RevokeDeviceSessionUseCase,
    private val revokeOtherDeviceSessions: RevokeOtherDeviceSessionsUseCase,
) : ViewModel() {
    private val holder = viewModelScope.memoryResource { getDeviceSessions() }
    private val actionError = MutableStateFlow<AppError?>(null)

    val state: StateFlow<SessionsUiState> =
        combine(holder.state, actionError) { sessions, error -> SessionsUiState(sessions, error) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, SessionsUiState())

    fun refresh() = holder.refresh()

    fun revoke(id: String) = act { revokeDeviceSession(id) }

    fun revokeOthers() = act { revokeOtherDeviceSessions() }

    private fun act(block: suspend () -> AppResult<Unit>) {
        viewModelScope.launch {
            val result = block()
            actionError.value = (result as? AppResult.Failure)?.error
            holder.refresh()
        }
    }
}

// ---------------------------------------------------------------- Delete account

data class DeleteAccountUiState(
    val email: String = "",
    val hasPassword: Boolean = true,
    val confirmEmail: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: AppError? = null,
)

class DeleteAccountViewModel(
    private val deleteAccount: DeleteAccountUseCase,
    getCachedProfile: GetCachedProfileUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DeleteAccountUiState())
    val state: StateFlow<DeleteAccountUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            getCachedProfile()?.let { profile ->
                mutableState.update { it.copy(email = profile.email, hasPassword = profile.hasPassword) }
            }
        }
    }

    fun onConfirmEmail(value: String) = mutableState.update { it.copy(confirmEmail = value, error = null) }

    fun onPassword(value: String) = mutableState.update { it.copy(password = value, error = null) }

    val canDelete: Boolean
        get() {
            val current = mutableState.value
            return current.confirmEmail.trim().equals(current.email, ignoreCase = true) &&
                (!current.hasPassword || current.password.isNotEmpty())
        }

    fun delete() {
        val current = mutableState.value
        if (current.loading || !canDelete) return
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            // On success every local trace is wiped and the app goes back to Welcome (F04).
            when (val result = deleteAccount(current.password, current.confirmEmail)) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> mutableState.update { it.copy(loading = false, error = result.error) }
            }
        }
    }
}
