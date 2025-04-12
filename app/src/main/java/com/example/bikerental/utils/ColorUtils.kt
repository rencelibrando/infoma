package com.example.bikerental.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.example.bikerental.R
import com.example.bikerental.ui.theme.Pink40 as ThemePink40
import com.example.bikerental.ui.theme.Pink80 as ThemePink80
import com.example.bikerental.ui.theme.Purple40 as ThemePurple40
import com.example.bikerental.ui.theme.Purple80 as ThemePurple80
import com.example.bikerental.ui.theme.PurpleGrey40 as ThemePurpleGrey40
import com.example.bikerental.ui.theme.PurpleGrey80 as ThemePurpleGrey80

/**
 * Utility object for consistent color usage across the app.
 * This centralizes color definitions to avoid redundancy.
 */
object ColorUtils {
    // Primary brand colors
    val DarkGreen = Color(0xFF0A5F38) // Dark green as primary brand color
    val LightGreen = Color(0xFF4CAF50) // Lighter variant for secondary elements
    
    // Complementary colors
    val Amber = Color(0xFFFFB300) // Warm amber that complements dark green
    val LightAmber = Color(0xFFFFD54F) // Lighter amber for hover states
    val SoftCoral = Color(0xFFF3A683) // Soft coral as an accent color
    
    // Text colors for better readability
    val LightText = Color(0xFFF5F5F5) // Off-white for text on dark backgrounds
    val DarkText = Color(0xFF212121) // Near-black for text on light backgrounds
    
    // Functional colors
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFFC107)
    val Error = Color(0xFFE53935)
    val Info = Color(0xFF2196F3)
    
    // App colors from resources
    @Composable
    fun purple200() = colorResource(id = R.color.purple_200)
    
    @Composable
    fun purple500() = colorResource(id = R.color.purple_500)
    
    @Composable
    fun purple700() = colorResource(id = R.color.purple_700)
    
    @Composable
    fun teal200() = colorResource(id = R.color.teal_200)
    
    @Composable
    fun teal700() = colorResource(id = R.color.teal_700)
    
    @Composable
    fun black() = colorResource(id = R.color.black)
    
    // Alias for black() to maintain compatibility with existing code
    @Composable
    fun blackcol() = black()
    
    @Composable
    fun white() = colorResource(id = R.color.white)
    
    // References to Material3 Compose colors from the theme package
    // Updated to use our dark green theme while maintaining compatibility
    val Purple80 = LightGreen   // Use LightGreen instead of purple
    val PurpleGrey80 = ThemePurpleGrey80
    val Pink80 = LightAmber     // Use LightAmber instead of pink
    val Purple40 = DarkGreen    // Use DarkGreen as the main brand color
    val PurpleGrey40 = ThemePurpleGrey40
    val Pink40 = Amber          // Use Amber instead of pink
} 