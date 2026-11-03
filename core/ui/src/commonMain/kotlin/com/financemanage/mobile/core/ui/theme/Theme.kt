package com.financemanage.mobile.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalFinanceColors = staticCompositionLocalOf { LightFinanceColors }

/** `true` when every amount in the app must be hidden ("eye" button, F12). */
val LocalHideBalances = compositionLocalOf { false }

/** Whether the device is online; actions that need the server (pay, settle, trade…) are disabled offline (F21). */
val LocalIsOnline = compositionLocalOf { true }

@Composable
fun FinanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    hideBalances: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalFinanceColors provides if (darkTheme) DarkFinanceColors else LightFinanceColors,
        LocalHideBalances provides hideBalances,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography(),
            content = content,
        )
    }
}

object FinanceTheme {
    val colors: FinanceColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFinanceColors.current
}
