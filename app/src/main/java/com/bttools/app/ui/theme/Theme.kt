package com.bttools.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BluePrimary = Color(0xFF1976D2)
private val BluePrimaryDark = Color(0xFF0D47A1)
private val BlueSecondary = Color(0xFF42A5F5)
private val BlueTertiary = Color(0xFF1565C0)

private val DarkColorSchemeBlue = darkColorScheme(
    primary = BlueSecondary,
    secondary = BluePrimary,
    tertiary = BlueTertiary,
    background = Color(0xFF0A1929),
    surface = Color(0xFF132F4C),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE3F2FD),
    onSurface = Color(0xFFE3F2FD)
)

private val LightColorSchemeBlue = lightColorScheme(
    primary = BluePrimary,
    secondary = BlueSecondary,
    tertiary = BlueTertiary,
    background = Color(0xFFF5F9FC),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0D47A1),
    onSurface = Color(0xFF0D47A1)
)

private fun createCustomColorScheme(primaryColor: Color, isDark: Boolean) = if (isDark) {
    darkColorScheme(
        primary = primaryColor,
        secondary = primaryColor.copy(alpha = 0.7f),
        tertiary = primaryColor.copy(alpha = 0.5f),
        background = Color(0xFF0A1929),
        surface = Color(0xFF132F4C),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFFE3F2FD),
        onSurface = Color(0xFFE3F2FD)
    )
} else {
    lightColorScheme(
        primary = primaryColor,
        secondary = primaryColor.copy(alpha = 0.7f),
        tertiary = primaryColor.copy(alpha = 0.5f),
        background = Color(0xFFF5F9FC),
        surface = Color(0xFFFFFFFF),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = primaryColor,
        onSurface = primaryColor
    )
}

@Composable
fun BTToolsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    customPrimaryColor: Long? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        customPrimaryColor != null -> {
            createCustomColorScheme(Color(customPrimaryColor.toInt()), darkTheme)
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorSchemeBlue
        else -> LightColorSchemeBlue
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
