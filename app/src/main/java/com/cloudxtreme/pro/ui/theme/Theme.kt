package com.cloudxtreme.pro.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = VoidDark,
    primaryContainer = HyperBlue,
    onPrimaryContainer = TextPrimary,
    secondary = DeepViolet,
    onSecondary = TextPrimary,
    secondaryContainer = LavenderGlow,
    background = VoidDark,
    onBackground = TextPrimary,
    surface = VoidSurface,
    onSurface = TextPrimary,
    surfaceVariant = VoidSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = GlassCardBorder
)

@Composable
fun CloudXtremeTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = VoidDark.toArgb()
            window.navigationBarColor = VoidDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
