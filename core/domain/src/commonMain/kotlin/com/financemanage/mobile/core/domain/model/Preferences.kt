package com.financemanage.mobile.core.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Local, non-secret preferences of the device. */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val hideBalances: Boolean = false,
    val onboardingCompleted: Boolean = false,
    /** Epoch ms until which the "turn on two-step verification" card stays hidden (F04). */
    val twoFactorCardHiddenUntil: Long? = null,
    /** Require fingerprint / Face ID (or the device passcode) when returning to the app (F04). */
    val appLockEnabled: Boolean = false,
    /** Lock again after this many minutes in the background: 1, 5 or 15. */
    val relockAfterMinutes: Int = DEFAULT_RELOCK_MINUTES,
    /** Recently used category ids, most recent first (F06 picker). */
    val recentCategoryIds: List<String> = emptyList(),
    /** Last wallet used in Quick add (F07). */
    val lastWalletId: String? = null,
    /** Notification permission was already requested (F20). */
    val notificationPermissionAsked: Boolean = false,
    /** Backend address set at runtime in a debug build, `null` = the build's default. */
    val apiBaseUrlOverride: String? = null,
) {
    companion object {
        const val DEFAULT_RELOCK_MINUTES = 5
        const val MAX_RECENT_CATEGORIES = 8
    }
}
