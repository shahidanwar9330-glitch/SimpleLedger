package com.ledger.simpleledger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

data class LedgerColors(
    val liya: androidx.compose.ui.graphics.Color,
    val diya: androidx.compose.ui.graphics.Color,
    val muted: androidx.compose.ui.graphics.Color,
    val cardOutline: androidx.compose.ui.graphics.Color
)

val LocalLedgerColors = staticCompositionLocalOf {
    LedgerColors(LiyaGreen, DiyaRed, LightOnSurfaceMuted, LightOutline)
}

private val LightScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = BrandPrimary,
    onSecondary = LightOnPrimary,
    secondaryContainer = LightPrimaryContainer,
    onSecondaryContainer = LightOnPrimaryContainer,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = LightOnBackground,
    onSurface = LightOnBackground,
    outline = LightOutline
)

private val DarkScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = BrandPrimaryDark,
    onSecondary = DarkOnPrimary,
    secondaryContainer = DarkPrimaryContainer,
    onSecondaryContainer = DarkOnPrimaryContainer,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkOnBackground,
    onSurface = DarkOnBackground,
    outline = DarkOutline
)

@Composable
fun SimpleLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme
    val ledgerColors = if (darkTheme) {
        LedgerColors(LiyaGreenDark, DiyaRedDark, DarkOnSurfaceMuted, DarkOutline)
    } else {
        LedgerColors(LiyaGreen, DiyaRed, LightOnSurfaceMuted, LightOutline)
    }

    CompositionLocalProvider(LocalLedgerColors provides ledgerColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
