package com.dive.weatherwatch.ui.theme

// Import GodoMFont from Type.kt
import com.dive.weatherwatch.ui.theme.GodoMFont

import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Colors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Wear OS Galaxy Watch Design System
 * 이 디자인 시스템은 Galaxy Watch의 일관된 UI/UX를 위한 스타일 가이드입니다.
 */

// ========================================
// Color Palette
// ========================================
object WearColors {
    // Primary Colors
    val BackgroundGradientStart = Color(0xFF5BA3E0)  // 상단 밝은 하늘색
    val BackgroundGradientEnd = Color(0xFF2563A8)    // 하단 진한 파란색
    
    // Surface Colors
    val CardBackground = Color(0x33FFFFFF)            // 반투명 흰색 카드
    val CardBackgroundDark = Color(0x1A000000)        // 어두운 반투명 배경
    
    // Text Colors
    val TextPrimary = Color(0xFFFFFFFF)               // 주요 텍스트 (흰색)
    val TextSecondary = Color(0xB3FFFFFF)             // 보조 텍스트 (70% 흰색)
    val TextTertiary = Color(0x80FFFFFF)              // 3차 텍스트 (50% 흰색)
    
    // Accent Colors
    val AccentYellow = Color(0xFFFFD700)              // 강조 포인트 (노란색)
    val AccentBlue = Color(0xFF00B4D8)                // 차트 라인 색상
    
    // Chart Colors
    val ChartLine = Color(0xFF00E5FF)                 // 차트 메인 라인
    val ChartFill = Color(0x3300E5FF)                 // 차트 영역 채우기
    val ChartGrid = Color(0x33FFFFFF)                 // 차트 그리드 라인
    val ChartDot = Color(0xFFFFFFFF)                  // 차트 데이터 포인트
    
    // UI Element Colors
    val DividerColor = Color(0x1AFFFFFF)              // 구분선
    val IconTint = Color(0xFFFFFFFF)                  // 아이콘 색상
    val ButtonBackground = Color(0x33FFFFFF)          // 버튼 배경
}

// ========================================
// Typography
// ========================================
object WearTypography {
    // Title Styles
    val LargeTitle = TextStyle(
        fontFamily = GodoMFont,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        color = WearColors.TextPrimary,
        letterSpacing = 0.5.sp
    )
    
    val MediumTitle = TextStyle(
        fontFamily = GodoMFont,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        color = WearColors.TextPrimary,
        letterSpacing = 0.25.sp
    )
    
    val SmallTitle = TextStyle(
        fontFamily = GodoMFont,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = WearColors.TextSecondary,
        letterSpacing = 0.15.sp
    )
    
    // Body Styles
    val BodyLarge = TextStyle(
        fontFamily = GodoMFont,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        color = WearColors.TextPrimary,
        letterSpacing = 0.sp
    )
    
    val BodyMedium = TextStyle(
        fontFamily = GodoMFont,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = WearColors.TextSecondary,
        letterSpacing = 0.sp
    )
    
    val BodySmall = TextStyle(
        fontFamily = GodoMFont,
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        color = WearColors.TextTertiary,
        letterSpacing = 0.sp
    )
    
    // Display Styles (큰 숫자 표시용)
    val DisplayLarge = TextStyle(
        fontFamily = GodoMFont,
        fontSize = 48.sp,
        fontWeight = FontWeight.Light,
        color = WearColors.TextPrimary,
        letterSpacing = (-0.5).sp
    )
    
    val DisplayMedium = TextStyle(
        fontFamily = GodoMFont,
        fontSize = 36.sp,
        fontWeight = FontWeight.Light,
        color = WearColors.TextPrimary,
        letterSpacing = 0.sp
    )
    
    // Caption Styles
    val Caption = TextStyle(
        fontFamily = GodoMFont,
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        color = WearColors.TextTertiary,
        letterSpacing = 0.1.sp
    )
}

// ========================================
// Dimensions & Spacing
// ========================================
object WearDimensions {
    // Screen Dimensions
    val ScreenWidth = 396.dp    // Galaxy Watch 화면 너비
    val ScreenHeight = 484.dp   // Galaxy Watch 화면 높이
    
    // Padding & Margins
    val ScreenPaddingHorizontal = 16.dp
    val ScreenPaddingVertical = 20.dp
    val ContentPadding = 12.dp
    val ItemSpacing = 8.dp
    val SectionSpacing = 16.dp
    
    // Card Dimensions
    val CardCornerRadius = 20.dp
    val CardElevation = 4.dp
    val CardPadding = 16.dp
    val CardMinHeight = 80.dp
    
    // Chart Dimensions
    val ChartHeight = 120.dp
    val ChartLineWidth = 2.dp
    val ChartDotSize = 6.dp
    val ChartGridLineWidth = 0.5.dp
    
    // Button & Icon Sizes
    val IconSizeSmall = 16.dp
    val IconSizeMedium = 20.dp
    val IconSizeLarge = 24.dp
    val ButtonHeight = 40.dp
    val ButtonCornerRadius = 20.dp
    
    // Component Heights
    val TopBarHeight = 48.dp
    val BottomBarHeight = 56.dp
    val ListItemHeight = 48.dp
}

// ========================================
// Component Styles
// ========================================
object WearComponentStyles {
    
    // Status Bar Style
    data class StatusBarStyle(
        val height: Dp = 24.dp,
        val backgroundColor: Color = Color.Transparent,
        val timeTextStyle: TextStyle = WearTypography.BodyMedium.copy(
            color = WearColors.TextPrimary,
            fontWeight = FontWeight.Medium
        )
    )
    
    // Card Style
    data class CardStyle(
        val backgroundColor: Color = WearColors.CardBackground,
        val cornerRadius: Dp = WearDimensions.CardCornerRadius,
        val elevation: Dp = WearDimensions.CardElevation,
        val padding: Dp = WearDimensions.CardPadding,
        val borderWidth: Dp = 0.5.dp,
        val borderColor: Color = WearColors.DividerColor
    )
    
    // Chart Style
    data class ChartStyle(
        val lineColor: Color = WearColors.ChartLine,
        val fillColor: Color = WearColors.ChartFill,
        val gridColor: Color = WearColors.ChartGrid,
        val dotColor: Color = WearColors.ChartDot,
        val lineWidth: Dp = WearDimensions.ChartLineWidth,
        val dotSize: Dp = WearDimensions.ChartDotSize,
        val showGrid: Boolean = true,
        val showDots: Boolean = true,
        val animationEnabled: Boolean = true
    )
    
    // Metric Display Style
    data class MetricStyle(
        val valueTextStyle: TextStyle = WearTypography.DisplayLarge,
        val unitTextStyle: TextStyle = WearTypography.BodyLarge,
        val labelTextStyle: TextStyle = WearTypography.BodySmall,
        val iconSize: Dp = WearDimensions.IconSizeMedium,
        val iconTint: Color = WearColors.IconTint
    )
}

// ========================================
// Theme Configuration
// ========================================
@Composable
fun WearOSTheme(
    darkTheme: Boolean = true,  // Wear OS는 기본적으로 다크 테마
    content: @Composable () -> Unit
) {
    val colors = WearColors
    
    MaterialTheme(
        colors = Colors(
            primary = colors.AccentBlue,
            primaryVariant = colors.AccentBlue,
            secondary = colors.AccentYellow,
            background = colors.BackgroundGradientEnd,
            surface = colors.CardBackground,
            error = Color.Red,
            onPrimary = colors.TextPrimary,
            onSecondary = colors.TextPrimary,
            onBackground = colors.TextPrimary,
            onSurface = colors.TextPrimary,
            onError = colors.TextPrimary
        ),
        content = content
    )
}