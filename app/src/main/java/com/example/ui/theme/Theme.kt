package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimaryDark,
    onPrimary = Color(0xFF00354E),
    primaryContainer = CyanContainerDark,
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = VioletSecondaryDark,
    onSecondary = Color(0xFF381E72),
    secondaryContainer = VioletContainerDark,
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = RoseTertiaryDark,
    onTertiary = Color(0xFF4C0015),
    background = SlateBackgroundDark,
    onBackground = Color(0xFFE2E8F0),
    surface = SlateSurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = SlateSurfaceVariantDark,
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val LightColorScheme = lightColorScheme(
    primary = CyanPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = CyanContainerLight,
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = VioletSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = VioletContainerLight,
    onSecondaryContainer = Color(0xFF5B21B6),
    tertiary = RoseTertiaryLight,
    onTertiary = Color.White,
    background = SlateBackgroundLight,
    onBackground = Color(0xFF0F172A),
    surface = SlateSurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = SlateSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun ArenaPilotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve high-contrast Arena branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
