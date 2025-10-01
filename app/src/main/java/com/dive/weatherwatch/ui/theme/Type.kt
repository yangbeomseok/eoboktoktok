package com.dive.weatherwatch.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 기본 시스템 폰트 사용
val GodoMFont = FontFamily.Default

// 기존 폰트 (호환성용)
val Cafe24DongdongFont = FontFamily.Default

object GalaxyWatchTypography {
    val LargeTitle = TextStyle(
        fontSize = 48.sp,
        fontWeight = FontWeight.Light,
        fontFamily = GodoMFont,
        color = AppColors.TextPrimary
    )

    val Title = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = GodoMFont,
        color = AppColors.TextPrimary
    )

    val Headline = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = GodoMFont,
        color = AppColors.TextPrimary
    )

    val Body = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = GodoMFont,
        color = AppColors.TextSecondary
    )

    val Caption = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = GodoMFont,
        color = AppColors.TextSecondary // Replaced TextTertiary with TextSecondary
    )

    val Metric = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = GodoMFont,
        color = AppColors.TextPrimary
    )

    val MetricUnit = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = GodoMFont,
        color = AppColors.TextSecondary
    )
}
