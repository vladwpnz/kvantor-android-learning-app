package com.bambiloff.kvantor.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bambiloff.kvantor.KvantorSystemBars

private val DarkColorScheme = darkColorScheme(
    primary = KvantorViolet,
    onPrimary = Color.White,
    primaryContainer = KvantorSurfaceHigh,
    onPrimaryContainer = Color.White,
    secondary = KvantorCyan,
    onSecondary = KvantorInk,
    secondaryContainer = Color(0xFF12344B),
    onSecondaryContainer = Color.White,
    tertiary = KvantorGold,
    onTertiary = KvantorInk,
    background = KvantorDeepPurple,
    onBackground = KvantorText,
    surface = KvantorSurface,
    onSurface = KvantorText,
    surfaceVariant = KvantorSurfaceHigh,
    onSurfaceVariant = KvantorMuted,
    outline = KvantorVioletSoft.copy(alpha = .45f),
    error = KvantorError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = KvantorPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE3FF),
    onPrimaryContainer = KvantorInk,
    secondary = Color(0xFF007A92),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7F8FF),
    onSecondaryContainer = KvantorInk,
    tertiary = Color(0xFF8A6200),
    onTertiary = Color.White,
    background = KvantorLightBg,
    onBackground = KvantorInk,
    surface = KvantorLightSurface,
    onSurface = KvantorInk,
    surfaceVariant = Color(0xFFF0E8FF),
    onSurfaceVariant = Color(0xFF5C4B76),
    outline = Color(0xFFB5A5D3),
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val KvantorShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp)
)

@Composable
fun KvantorTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    KvantorSystemBars()

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = KvantorShapes,
        content = content
    )
}
