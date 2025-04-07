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
    // These are used for direct color references (not in Composable context)
    val Purple80 = ThemePurple80
    val PurpleGrey80 = ThemePurpleGrey80
    val Pink80 = ThemePink80
    val Purple40 = ThemePurple40
    val PurpleGrey40 = ThemePurpleGrey40
    val Pink40 = ThemePink40
} 