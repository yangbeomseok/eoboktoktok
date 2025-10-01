package com.dive.weatherwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import com.dive.weatherwatch.ui.navigation.WatchNavigation
import com.dive.weatherwatch.ui.theme.GodoMFont
import androidx.compose.ui.unit.sp
import com.dive.weatherwatch.services.TideNotificationService
import com.dive.weatherwatch.services.FishingHotspotNotificationService

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 조위 알림 채널 생성
        TideNotificationService.createNotificationChannel(this)
        
        // 낚시 핫스팟 알림 채널 생성
        FishingHotspotNotificationService.createNotificationChannel(this)
        
        // 백 제스처 핸들링 (유니버셜 제스처 포함)
        onBackPressedDispatcher.addCallback(this) {
            android.util.Log.d("MainActivity", "Back gesture detected")
            // 기본 백 동작 수행
            if (!tryNavigateBack()) {
                finish()
            }
        }
        

        setContent {
            // 커스텀 Typography 정의 (모든 텍스트에 GodoM 폰트 적용)
            val customTypography = Typography(
                display1 = TextStyle(fontFamily = GodoMFont, fontSize = 34.sp),
                display2 = TextStyle(fontFamily = GodoMFont, fontSize = 30.sp),
                display3 = TextStyle(fontFamily = GodoMFont, fontSize = 26.sp),
                title1 = TextStyle(fontFamily = GodoMFont, fontSize = 22.sp),
                title2 = TextStyle(fontFamily = GodoMFont, fontSize = 18.sp),
                title3 = TextStyle(fontFamily = GodoMFont, fontSize = 16.sp),
                body1 = TextStyle(fontFamily = GodoMFont, fontSize = 14.sp),
                body2 = TextStyle(fontFamily = GodoMFont, fontSize = 12.sp),
                button = TextStyle(fontFamily = GodoMFont, fontSize = 14.sp),
                caption1 = TextStyle(fontFamily = GodoMFont, fontSize = 12.sp),
                caption2 = TextStyle(fontFamily = GodoMFont, fontSize = 11.sp),
                caption3 = TextStyle(fontFamily = GodoMFont, fontSize = 10.sp)
            )
            
            MaterialTheme(
                typography = customTypography
            ) {
                WatchNavigation()
            }
        }
    }
    
    
    
    private fun tryNavigateBack(): Boolean {
        // 여기에서 현재 화면의 네비게이션 상태를 확인하고
        // 적절한 백 네비게이션을 수행합니다
        return false // 기본적으로 시스템 백 동작 허용
    }
}