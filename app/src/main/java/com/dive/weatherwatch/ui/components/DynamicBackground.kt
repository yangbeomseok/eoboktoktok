package com.dive.weatherwatch.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dive.weatherwatch.R
import com.dive.weatherwatch.data.Item
import java.util.Calendar

@Composable
fun DynamicBackgroundOverlay(
    weatherData: List<Item>? = null,
    alpha: Float = 0.4f,
    forceTimeBasedBackground: Boolean = false
) {
    val backgroundResource = if (forceTimeBasedBackground || weatherData.isNullOrEmpty()) {
        // 날씨 데이터가 없거나 강제 시간 기반 모드일 때는 시간만으로 배경 결정
        getTimeBasedBackgroundResource()
    } else {
        // 날씨 데이터가 있을 때는 기존 로직 사용
        val sky = weatherData.firstOrNull { it.category == "SKY" }?.fcstValue ?: "1"
        getDynamicBackgroundResource(sky, weatherData)
    }
    
    if (backgroundResource != 0) {
        Box(modifier = Modifier.fillMaxSize()) {
            SafeBackgroundImage(
                resourceId = backgroundResource,
                alpha = alpha
            )
        }
    }
}

@Composable
fun DynamicBackgroundOverlay(
    currentWeather: com.dive.weatherwatch.data.BadaTimeCurrentResponse?,
    forecastWeather: List<com.dive.weatherwatch.data.BadaTimeForecastResponse> = emptyList(),
    alpha: Float = 0.4f
) {
    val backgroundResource = if (currentWeather == null) {
        // BadaTime 데이터가 없을 때는 시간만으로 배경 결정
        getTimeBasedBackgroundResource()
    } else {
        // BadaTime 데이터가 있을 때는 하늘 상태를 사용
        val skyCode = (currentWeather.skyCode ?: currentWeather.skycode) ?: "1"
        val rain = currentWeather.rain?.toDoubleOrNull() ?: 0.0
        getBadaTimeDynamicBackgroundResource(skyCode, rain)
    }
    
    if (backgroundResource != 0) {
        Box(modifier = Modifier.fillMaxSize()) {
            SafeBackgroundImage(
                resourceId = backgroundResource,
                alpha = alpha
            )
        }
    }
}

// 시간만으로 배경 결정하는 함수 추가
private fun getTimeBasedBackgroundResource(): Int {
    val calendar = Calendar.getInstance()
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(Calendar.MINUTE)
    val isNight = currentHour < 6 || (currentHour == 19 && currentMinute >= 30) || currentHour >= 20
    
    return if (isNight) {
        R.mipmap.clear_night
    } else {
        R.mipmap.clear_morning_day
    }
}

// 실제 날씨별 배경화면 리소스 선택 함수
private fun getDynamicBackgroundResource(sky: String?, items: List<Item>): Int {
    val calendar = Calendar.getInstance()
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(Calendar.MINUTE)
    val isNight = currentHour < 6 || (currentHour == 19 && currentMinute >= 30) || currentHour >= 20
    val isRainy = hasRain(items)
    
    return when {
        // 🌧️ 비오는 날
        isRainy && isNight -> R.mipmap.rainy_night
        isRainy && !isNight -> R.mipmap.rainy_morning_day
        
        // ☁️ 흐림 (SKY: 4)
        sky == "4" && isNight -> R.mipmap.cloudy_night
        sky == "4" && !isNight -> R.mipmap.cloudy_morning_day
        
        // ⛅ 구름많음 (SKY: 3) - 흐림과 동일한 배경 사용
        sky == "3" && isNight -> R.mipmap.cloudy_night
        sky == "3" && !isNight -> R.mipmap.cloudy_morning_day
        
        // ☀️ 맑음 (SKY: 1) 및 기본값
        sky == "1" && isNight -> R.mipmap.clear_night
        sky == "1" && !isNight -> R.mipmap.clear_morning_day
        
        // 기본값 (맑음)
        isNight -> R.mipmap.clear_night
        else -> R.mipmap.clear_morning_day
    }
}

private fun hasRain(items: List<Item>): Boolean {
    val pop = items.firstOrNull { it.category == "POP" }?.fcstValue?.toIntOrNull() ?: 0
    val pty = items.firstOrNull { it.category == "PTY" }?.fcstValue?.toIntOrNull() ?: 0
    return pop > 30 || pty > 0 // 강수확률 30% 이상이거나 강수형태가 있으면 비
}

// BadaTime 데이터를 위한 배경 리소스 선택 함수
private fun getBadaTimeDynamicBackgroundResource(skyCode: String, rain: Double): Int {
    val calendar = Calendar.getInstance()
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(Calendar.MINUTE)
    val isNight = currentHour < 6 || (currentHour == 19 && currentMinute >= 30) || currentHour >= 20
    val isRainy = rain > 0.0
    
    return when {
        // 🌧️ 비오는 날 (강수량이 0보다 클 때)
        isRainy && isNight -> R.mipmap.rainy_night
        isRainy && !isNight -> R.mipmap.rainy_morning_day
        
        // ☁️ 흐림 (SKY: 4)
        skyCode == "4" && isNight -> R.mipmap.cloudy_night
        skyCode == "4" && !isNight -> R.mipmap.cloudy_morning_day
        
        // ⛅ 구름많음 (SKY: 3) - 흐림과 동일한 배경 사용
        skyCode == "3" && isNight -> R.mipmap.cloudy_night
        skyCode == "3" && !isNight -> R.mipmap.cloudy_morning_day
        
        // ☀️ 맑음 (SKY: 1) 및 기본값
        skyCode == "1" && isNight -> R.mipmap.clear_night
        skyCode == "1" && !isNight -> R.mipmap.clear_morning_day
        
        // 기본값 (맑음)
        isNight -> R.mipmap.clear_night
        else -> R.mipmap.clear_morning_day
    }
}