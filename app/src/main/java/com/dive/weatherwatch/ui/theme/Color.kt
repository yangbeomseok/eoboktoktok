package com.dive.weatherwatch.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppColors {
    // Main Theme Color
    val Primary = Color(0xFF0BA1BA) // R 11, G 161, B 186
    val PrimaryDark = Color(0xFF087E9B)
    val PrimaryLight = Color(0xFF10C8E0)

    // Background
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF1A1A1A)

    // Text Colors
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB0B0B0)
    
    // Accent Colors for specific UI elements
    val AccentGreen = Color(0xFF38ef7d)
    val AccentRed = Color(0xFFFF6B6B)
}

object AppGradients {
    // Main background gradient for all screens
    val mainBackground = Brush.verticalGradient(
        colors = listOf(
            AppColors.PrimaryDark,
            AppColors.Background
        )
    )

    // Gradient for primary cards and buttons
    val primaryCard = Brush.horizontalGradient(
        colors = listOf(
            AppColors.Primary,
            AppColors.PrimaryLight
        )
    )
    
    // Gradient for secondary cards (e.g., weather details)
    val secondaryCard = Brush.horizontalGradient(
        colors = listOf(
            AppColors.Primary.copy(alpha = 0.4f),
            AppColors.PrimaryLight.copy(alpha = 0.2f)
        )
    )

    // Gradient for the seconds indicator on the first screen
    val secondsIndicator = Brush.sweepGradient(
        colors = listOf(AppColors.AccentGreen, AppColors.Primary, AppColors.AccentGreen)
    )

    // Gradient for the heart rate card
    val heartRateCard = Brush.radialGradient(
        colors = listOf(
            AppColors.AccentRed,
            AppColors.AccentRed.copy(alpha = 0.8f)
        )
    )

    // Gradients for chat bubbles
    val userMessage = Brush.horizontalGradient(
        colors = listOf(
            AppColors.Primary,
            AppColors.PrimaryLight
        )
    )
    val aiMessage = Brush.horizontalGradient(
        colors = listOf(
            AppColors.Surface,
            Color(0xFF3A3A3A)
        )
    )
    
    // Gradient for the microphone button
    val micButton = Brush.radialGradient(
        colors = listOf(
            AppColors.Primary,
            AppColors.PrimaryDark
        )
    )
}
