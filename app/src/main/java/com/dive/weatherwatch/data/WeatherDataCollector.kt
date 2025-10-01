package com.dive.weatherwatch.data

import android.util.Log
import com.dive.weatherwatch.di.NetworkModule
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 모든 API 데이터를 실시간으로 수집하고 관리하는 통합 서비스
 */
class WeatherDataCollector {
    
    private val badaTimeService = NetworkModule.badaTimeService
    private val contextService = WeatherContextService()
    
    private val _weatherContext = MutableStateFlow(WeatherDataContext())
    val weatherContext: StateFlow<WeatherDataContext> = _weatherContext.asStateFlow()
    
    private val _geminiContext = MutableStateFlow("")
    val geminiContext: StateFlow<String> = _geminiContext.asStateFlow()
    
    private val _fishingAnalysis = MutableStateFlow("")
    val fishingAnalysis: StateFlow<String> = _fishingAnalysis.asStateFlow()
    
    private var collectJob: Job? = null
    
    /**
     * 모든 API 데이터를 수집하고 Gemini 컨텍스트 생성
     */
    suspend fun collectAllWeatherData(
        latitude: Double,
        longitude: Double,
        locationName: String? = null
    ) {
        try {
            Log.d("WeatherDataCollector", "데이터 수집 시작: lat=$latitude, lon=$longitude")
            
            // withContext를 사용하여 모든 API를 병렬로 호출
            withContext(Dispatchers.IO) {
                val currentWeatherDeferred = async { getCurrentWeather(latitude, longitude) }
                val forecastWeatherDeferred = async { getForecastWeather(latitude, longitude) }
                val tideDataDeferred = async { getTideData(latitude, longitude) }
                val fishingPointsDeferred = async { getFishingPoints(latitude, longitude) }
                val waterTemperatureDeferred = async { getWaterTemperature(latitude, longitude) }
                
                // 결과 수집
                val currentWeather = currentWeatherDeferred.await()
                val forecastWeather = forecastWeatherDeferred.await()
                val tideData = tideDataDeferred.await()
                val fishingPoints = fishingPointsDeferred.await()
                val waterTemperature = waterTemperatureDeferred.await()
                
                // WeatherDataContext 업데이트
                val newContext = WeatherDataContext(
                    currentWeather = currentWeather,
                    forecastWeather = forecastWeather,
                    tideData = tideData,
                    fishingPoints = fishingPoints,
                    waterTemperature = waterTemperature,
                    locationName = locationName,
                    latitude = latitude,
                    longitude = longitude
                )
                
                _weatherContext.value = newContext
                
                // Gemini용 컨텍스트 생성
                val geminiContext = contextService.generateContextForGemini(newContext)
                _geminiContext.value = geminiContext
                
                // 낚시 분석 정보 생성
                val fishingAnalysis = contextService.generateFishingAnalysis(newContext)
                _fishingAnalysis.value = fishingAnalysis
                
                Log.d("WeatherDataCollector", "모든 데이터 수집 완료")
            }
            
        } catch (e: Exception) {
            Log.e("WeatherDataCollector", "데이터 수집 중 오류 발생", e)
        }
    }
    
    /**
     * 주기적으로 데이터를 업데이트 (10분마다)
     */
    fun startPeriodicUpdate(
        latitude: Double,
        longitude: Double,
        locationName: String? = null,
        intervalMinutes: Long = 10
    ) {
        collectJob?.cancel()
        collectJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    collectAllWeatherData(latitude, longitude, locationName)
                    delay(intervalMinutes * 60 * 1000) // 분을 밀리초로 변환
                } catch (e: Exception) {
                    Log.e("WeatherDataCollector", "주기적 업데이트 중 오류", e)
                    delay(30000) // 오류 시 30초 후 재시도
                }
            }
        }
    }
    
    /**
     * 주기적 업데이트 중지
     */
    fun stopPeriodicUpdate() {
        collectJob?.cancel()
        collectJob = null
    }
    
    // 개별 API 호출 함수들
    private suspend fun getCurrentWeather(lat: Double, lon: Double): BadaTimeCurrentResponse? {
        return try {
            val response = badaTimeService.getCurrentWeather(lat, lon)
            if (response.isSuccessful) {
                response.body()?.weather?.firstOrNull()
            } else {
                Log.e("WeatherDataCollector", "현재 날씨 API 실패: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("WeatherDataCollector", "현재 날씨 API 오류", e)
            null
        }
    }
    
    private suspend fun getForecastWeather(lat: Double, lon: Double): List<BadaTimeForecastResponse> {
        return try {
            val response = badaTimeService.getForecastWeather(lat, lon)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                Log.e("WeatherDataCollector", "예보 API 실패: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("WeatherDataCollector", "예보 API 오류", e)
            emptyList()
        }
    }
    
    private suspend fun getTideData(lat: Double, lon: Double): List<BadaTimeTideResponse> {
        return try {
            val response = badaTimeService.getTideData(lat, lon)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                Log.e("WeatherDataCollector", "조위 API 실패: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("WeatherDataCollector", "조위 API 오류", e)
            emptyList()
        }
    }
    
    private suspend fun getFishingPoints(lat: Double, lon: Double): List<FishingPoint> {
        return try {
            val response = badaTimeService.getFishingPoints(lat, lon)
            if (response.isSuccessful) {
                val apiResponse = response.body()
                apiResponse?.fishingPoint?.map { point ->
                    point.toFishingPoint(apiResponse.info)
                } ?: emptyList()
            } else {
                Log.e("WeatherDataCollector", "낚시포인트 API 실패: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("WeatherDataCollector", "낚시포인트 API 오류", e)
            emptyList()
        }
    }
    
    private suspend fun getWaterTemperature(lat: Double, lon: Double): String? {
        return try {
            val response = badaTimeService.getWaterTemperature(lat, lon)
            if (response.isSuccessful) {
                val temperatureData = response.body() ?: emptyList()
                temperatureData.findClosest()?.obsWt
            } else {
                Log.e("WeatherDataCollector", "수온 API 실패: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("WeatherDataCollector", "수온 API 오류", e)
            null
        }
    }
    
    /**
     * 현재 수집된 데이터의 상태 정보
     */
    fun getDataStatus(): String {
        val context = _weatherContext.value
        val status = StringBuilder()
        
        status.append("=== 데이터 수집 상태 ===\n")
        status.append("현재 날씨: ${if (context.currentWeather != null) "✅" else "❌"}\n")
        status.append("예보 데이터: ${if (context.forecastWeather.isNotEmpty()) "✅ (${context.forecastWeather.size}개)" else "❌"}\n")
        status.append("조위 정보: ${if (context.tideData.isNotEmpty()) "✅" else "❌"}\n")
        status.append("낚시 포인트: ${if (context.fishingPoints.isNotEmpty()) "✅ (${context.fishingPoints.size}개)" else "❌"}\n")
        status.append("수온 정보: ${if (context.waterTemperature != null) "✅" else "❌"}\n")
        status.append("위치 정보: ${if (context.locationName != null) "✅" else "❌"}\n")
        
        return status.toString()
    }
}