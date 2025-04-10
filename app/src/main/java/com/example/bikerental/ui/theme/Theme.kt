package com.example.bikerental.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0066CC),          // Main brand color
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    
    secondary = Color(0xFF2E7D32),        // Accent color
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB8F397),
    onSecondaryContainer = Color(0xFF002201),
    
    tertiary = Color(0xFFFF6B00),         // Additional accent
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBCC),
    onTertiaryContainer = Color(0xFF331400),
    
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    
    outline = Color(0xFF72777F),
    outlineVariant = Color(0xFFC2C7CF),
    
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF42474E)
)

@Composable
fun BikerentalTheme(
    content: @Composable () -> Unit
) {
    // Always use light theme
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Make status bar transparent and use light icons for better visibility
            window.statusBarColor = Color.Transparent.toArgb()
            // Change this to 'true' for light status bar icons, 'false' for dark icons
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            
            // Enable edge-to-edge UI
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}