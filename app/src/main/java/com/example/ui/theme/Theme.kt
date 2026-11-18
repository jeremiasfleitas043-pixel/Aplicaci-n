package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class CleanMasterExtendedColors(
    val isDark: Boolean,
    val cardBorder: Color,
    val emeraldAccent: Color,
    val blueAccent: Color,
    val cyanAccent: Color,
    val warning: Color,
    val danger: Color,
    val success: Color,
    val orbTrack: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    CleanMasterExtendedColors(
        isDark = true,
        cardBorder = DarkBorder,
        emeraldAccent = EmeraldBright,
        blueAccent = BlueBright,
        cyanAccent = CyanSecondary,
        warning = StatusWarning,
        danger = StatusDanger,
        success = StatusSuccess,
        orbTrack = DarkSurfaceHighest
    )
}

private val CleanMasterDarkScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF032117),
    primaryContainer = EmeraldMuted,
    onPrimaryContainer = EmeraldBright,
    secondary = ElectricBlue,
    onSecondary = Color(0xFF031D2E),
    secondaryContainer = BlueMuted,
    onSecondaryContainer = BlueBright,
    tertiary = CyanSecondary,
    onTertiary = Color(0xFF031E24),
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceElevated,
    surfaceContainerHigh = DarkSurfaceHighest,
    outline = DarkBorder,
    error = StatusDanger,
    onError = Color.White
)

private val CleanMasterLightScheme = lightColorScheme(
    primary = EmeraldLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = BlueLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFF0891B2),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurfaceElevated,
    surfaceContainerHigh = LightSurfaceHighest,
    outline = LightBorder,
    error = Color(0xFFE11D48),
    onError = Color.White
)

val CleanMasterShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CleanMasterTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CleanMasterDarkScheme else CleanMasterLightScheme
    val extendedColors = if (darkTheme) {
        CleanMasterExtendedColors(
            isDark = true,
            cardBorder = DarkBorder,
            emeraldAccent = EmeraldBright,
            blueAccent = BlueBright,
            cyanAccent = CyanSecondary,
            warning = StatusWarning,
            danger = StatusDanger,
            success = StatusSuccess,
            orbTrack = DarkSurfaceHighest
        )
    } else {
        CleanMasterExtendedColors(
            isDark = false,
            cardBorder = LightBorder,
            emeraldAccent = EmeraldLightPrimary,
            blueAccent = BlueLightSecondary,
            cyanAccent = Color(0xFF0891B2),
            warning = Color(0xFFD97706),
            danger = Color(0xFFE11D48),
            success = Color(0xFF059669),
            orbTrack = LightSurfaceHighest
        )
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CleanMasterTypography,
            shapes = CleanMasterShapes,
            content = content
        )
    }
}
