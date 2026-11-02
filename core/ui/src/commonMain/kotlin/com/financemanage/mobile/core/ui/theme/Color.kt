package com.financemanage.mobile.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal val LightColors =
    lightColorScheme(
        primary = Color(0xFF006C51),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF7FF8CF),
        onPrimaryContainer = Color(0xFF002117),
        secondary = Color(0xFF4C6359),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCEE9DB),
        onSecondaryContainer = Color(0xFF092017),
        tertiary = Color(0xFF3F6375),
        onTertiary = Color(0xFFFFFFFF),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        background = Color(0xFFFBFDF9),
        onBackground = Color(0xFF191C1A),
        surface = Color(0xFFFBFDF9),
        onSurface = Color(0xFF191C1A),
        surfaceVariant = Color(0xFFDBE5DE),
        onSurfaceVariant = Color(0xFF404944),
        outline = Color(0xFF707974),
    )

internal val DarkColors =
    darkColorScheme(
        primary = Color(0xFF61DBB4),
        onPrimary = Color(0xFF003828),
        primaryContainer = Color(0xFF00513C),
        onPrimaryContainer = Color(0xFF7FF8CF),
        secondary = Color(0xFFB3CCBF),
        onSecondary = Color(0xFF1F352B),
        secondaryContainer = Color(0xFF354B41),
        onSecondaryContainer = Color(0xFFCEE9DB),
        tertiary = Color(0xFFA7CCE1),
        onTertiary = Color(0xFF0B3445),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        background = Color(0xFF191C1A),
        onBackground = Color(0xFFE1E3DF),
        surface = Color(0xFF191C1A),
        onSurface = Color(0xFFE1E3DF),
        surfaceVariant = Color(0xFF404944),
        onSurfaceVariant = Color(0xFFBFC9C2),
        outline = Color(0xFF8A938E),
    )

/** Colours with a financial meaning, not covered by Material roles. */
data class FinanceColors(
    val income: Color,
    val expense: Color,
    val warning: Color,
    val neutral: Color,
)

internal val LightFinanceColors =
    FinanceColors(
        income = Color(0xFF1B7F3B),
        expense = Color(0xFFC62828),
        warning = Color(0xFFB26A00),
        neutral = Color(0xFF707974),
    )

internal val DarkFinanceColors =
    FinanceColors(
        income = Color(0xFF6DD58C),
        expense = Color(0xFFFF8A80),
        warning = Color(0xFFFFC266),
        neutral = Color(0xFF8A938E),
    )
