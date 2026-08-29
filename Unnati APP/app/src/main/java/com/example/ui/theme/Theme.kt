package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = UxBrownPrimary,
    onPrimary = Color.White,
    primaryContainer = UxOrangeContainer,
    onPrimaryContainer = UxBrownDark,
    secondary = UxTextSecondary,
    onSecondary = Color.White,
    secondaryContainer = UxSurfaceContainer,
    onSecondaryContainer = UxTextSecondary,
    tertiary = UxSuccessGreenDark,
    onTertiary = Color.White,
    tertiaryContainer = UxSuccessContainer,
    onTertiaryContainer = Color(0xFF004118),
    background = UxSurfaceBright,
    onBackground = UxOnSurface,
    surface = UxSurfaceBright,
    onSurface = UxOnSurface,
    surfaceVariant = UxSurfaceContainer,
    onSurfaceVariant = UxTextSecondary,
    outline = UxOrangeBorder,
    outlineVariant = UxCardBorder,
    surfaceContainer = UxSurfaceContainer,
    surfaceContainerLow = UxSurfaceContainerLow,
    surfaceContainerLowest = UxSurfaceLowest,
    surfaceContainerHigh = UxSurfaceContainerHigh
)

private val DarkColorScheme = lightColorScheme(
    primary = UxOrange,
    onPrimary = Color.White,
    primaryContainer = UxOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = UxTextSecondary,
    onSecondary = Color.White,
    background = Color(0xFF121212),
    onBackground = Color(0xFFF1F1F1),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFF1F1F1),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFBBBBBB),
    outline = Color(0xFF444444)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Design specifies clean high-contrast light theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = UxSurfaceBright.toArgb()
            window.navigationBarColor = UxSurfaceLowest.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
