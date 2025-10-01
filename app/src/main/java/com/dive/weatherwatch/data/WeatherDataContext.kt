package com.dive.weatherwatch.data

import android.util.Log

/**
 * 모든 API 데이터를 통합하여 Gemini가 이해할 수 있는 컨텍스트로 변환하는 데이터 클래스
 */
data class WeatherDataContext(
    val currentWeather: BadaTimeCurrentResponse? = null,
    val forecastWeather: List<BadaTimeForecastResponse> = emptyList(),
    val tideData: List<BadaTimeTideResponse> = emptyList(),
    val fishingPoints: List<FishingPoint> = emptyList(),
    val waterTemperature: String? = null,
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

/**
 * 모든 API 데이터를 Gemini가 이해할 수 있는 자연어 컨텍스트로 변환하는 서비스
 */
class WeatherContextService {
    
    fun generateContextForGemini(data: WeatherDataContext): String {
        val context = StringBuilder()
        
        // 위치 정보
        data.locationName?.let { location ->
            context.append("===현재 위치===\n")
            context.append("지역: $location\n")
            data.latitude?.let { lat -> context.append("위도: $lat\n") }
            data.longitude?.let { lon -> context.append("경도: $lon\n") }
            context.append("\n")
        }
        
        // 현재 날씨 정보
        data.currentWeather?.let { weather ->
            context.append("===현재 날씨 상황===\n")
            weather.temp?.let { context.append("기온: ${it}°C\n") }
            weather.sky?.let { context.append("하늘 상태: $it\n") }
            weather.windspd?.let { context.append("풍속: ${it}m/s\n") }
            weather.winddir?.let { context.append("풍향: $it\n") }
            weather.humidity?.let { context.append("습도: ${it}%\n") }
            weather.rain?.let { context.append("강수확률: ${it}%\n") }
            weather.pago?.let { context.append("파고: ${it}m\n") }
            weather.pm25Status?.let { context.append("초미세먼지: $it\n") }
            context.append("\n")
        }
        
        // 수온 정보
        data.waterTemperature?.let { temp ->
            context.append("===수온 정보===\n")
            context.append("현재 수온: ${temp}°C\n")
            context.append("\n")
        }
        
        // 예보 정보 (향후 24시간)
        if (data.forecastWeather.isNotEmpty()) {
            context.append("===날씨 예보===\n")
            data.forecastWeather.take(8).forEach { forecast -> // 24시간 (3시간 간격)
                forecast.ymdt?.let { time ->
                    context.append("시간: $time, ")
                }
                forecast.temp?.let { temp ->
                    context.append("기온: ${temp}°C, ")
                }
                forecast.sky?.let { sky ->
                    context.append("날씨: $sky, ")
                }
                forecast.rain?.let { rain ->
                    context.append("강수확률: ${rain}%")
                }
                context.append("\n")
            }
            context.append("\n")
        }
        
        // 조위 정보
        if (data.tideData.isNotEmpty()) {
            context.append("===조위 정보===\n")
            data.tideData.firstOrNull()?.let { tide ->
                tide.tideType?.let { context.append("물때: $it\n") }
                tide.sunRiseSet?.let { context.append("일출/일몰: $it\n") }
                tide.moonRiseSet?.let { context.append("월출/월몰: $it\n") }
                
                // 조위 시간 정보
                tide.tideTime1?.let { context.append("조위1: $it\n") }
                tide.tideTime2?.let { context.append("조위2: $it\n") }
                tide.tideTime3?.let { context.append("조위3: $it\n") }
                tide.tideTime4?.let { context.append("조위4: $it\n") }
            }
            context.append("\n")
        }
        
        // 낚시 포인트 정보
        if (data.fishingPoints.isNotEmpty()) {
            context.append("===주변 낚시 포인트===\n")
            data.fishingPoints.take(3).forEach { point -> // 가장 가까운 3곳
                context.append("포인트명: ${point.pointNm}\n")  // point.name 대신 point.pointNm 사용
                context.append("위치: ${point.addr}\n")
                context.append("수심: ${point.dpwt}\n")
                context.append("주요 어종: ${point.target}\n")
                context.append("미끼: ${point.material}\n")
                
                // 계절별 어종 정보
                if (point.fishSp.isNotEmpty()) context.append("봄 어종: ${point.fishSp}\n")
                if (point.fishSu.isNotEmpty()) context.append("여름 어종: ${point.fishSu}\n")
                if (point.fishFa.isNotEmpty()) context.append("가을 어종: ${point.fishFa}\n")
                if (point.fishWi.isNotEmpty()) context.append("겨울 어종: ${point.fishWi}\n")
                
                // 계절별 수온 정보
                if (point.wtempSp.isNotEmpty()) context.append("봄 수온: ${point.wtempSp}\n")
                if (point.wtempSu.isNotEmpty()) context.append("여름 수온: ${point.wtempSu}\n")
                if (point.wtempFa.isNotEmpty()) context.append("가을 수온: ${point.wtempFa}\n")
                if (point.wtempWi.isNotEmpty()) context.append("겨울 수온: ${point.wtempWi}\n")
                
                if (point.forecast.isNotEmpty()) context.append("해상 예보: ${point.forecast}\n")
                if (point.notice.isNotEmpty()) context.append("주의사항: ${point.notice}\n")
                
                context.append("\n")
            }
        }
        
        val finalContext = context.toString()
        Log.d("WeatherContextService", "Generated context for Gemini:\n$finalContext")
        
        return finalContext
    }
    
    /**
     * 낚시에 특화된 분석 정보를 추가로 생성
     */
    fun generateFishingAnalysis(data: WeatherDataContext): String {
        val analysis = StringBuilder()
        
        analysis.append("===낚시 조건 분석===\n")
        
        // 날씨 조건 분석
        data.currentWeather?.let { weather ->
            val windSpeed = weather.windspd?.toFloatOrNull() ?: 0f
            val waveHeight = weather.pago?.toFloatOrNull() ?: 0f
            val rainChance = weather.rain?.toIntOrNull() ?: 0
            
            when {
                windSpeed <= 3f && waveHeight <= 1f && rainChance <= 30 -> {
                    analysis.append("날씨 조건: 🟢 좋음 (낚시 적합)\n")
                }
                windSpeed <= 6f && waveHeight <= 2f && rainChance <= 60 -> {
                    analysis.append("날씨 조건: 🟡 보통 (주의해서 낚시 가능)\n")
                }
                else -> {
                    analysis.append("날씨 조건: 🔴 나쁨 (낚시 비추천)\n")
                }
            }
        }
        
        // 수온 분석
        data.waterTemperature?.let { temp ->
            val waterTemp = temp.replace("°C", "").toFloatOrNull() ?: 15f
            when {
                waterTemp >= 18f && waterTemp <= 25f -> {
                    analysis.append("수온 조건: 🟢 최적 (활발한 어종 활동 예상)\n")
                }
                waterTemp >= 10f && waterTemp < 18f || waterTemp > 25f -> {
                    analysis.append("수온 조건: 🟡 보통 (어종 활동 보통)\n")
                }
                else -> {
                    analysis.append("수온 조건: 🔴 저조 (어종 활동 저조)\n")
                }
            }
        }
        
        // 조위 분석
        data.tideData.firstOrNull()?.let { tide ->
            tide.tideType?.let { tideType ->
                when {
                    tideType.contains("조금") -> {
                        analysis.append("조위 조건: 🟡 조금 (물때 평범)\n")
                    }
                    tideType.contains("대조") || tideType.contains("사리") -> {
                        analysis.append("조위 조건: 🟢 대조/사리 (낚시 좋은 물때)\n")
                    }
                    else -> {
                        analysis.append("조위 조건: 🟡 보통 물때\n")
                    }
                }
            }
        }
        
        return analysis.toString()
    }
}