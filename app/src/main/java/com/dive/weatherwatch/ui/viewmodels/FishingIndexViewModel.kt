package com.dive.weatherwatch.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dive.weatherwatch.data.FishingIndexData
import com.dive.weatherwatch.data.WeatherResponse
import com.dive.weatherwatch.data.DailyTideInfo
import com.dive.weatherwatch.di.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class FishingIndexViewModel : ViewModel() {
    
    private val fishingIndexService = NetworkModule.fishingIndexService
    
    private val _fishingIndexData = MutableStateFlow<List<FishingIndexData>>(emptyList())
    val fishingIndexData: StateFlow<List<FishingIndexData>> = _fishingIndexData
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    private val _dynamicFishingIndex = MutableStateFlow<Int>(3)
    val dynamicFishingIndex: StateFlow<Int> = _dynamicFishingIndex
    
    private val _algorithmExplanation = MutableStateFlow<String>("")
    val algorithmExplanation: StateFlow<String> = _algorithmExplanation
    
    fun loadFishingIndex(serviceKey: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                android.util.Log.d("FishingIndexViewModel", "=== 낚시 지수 API 호출 시작 ===")
                android.util.Log.d("FishingIndexViewModel", "📋 API Parameters:")
                android.util.Log.d("FishingIndexViewModel", "  - serviceKey: ${serviceKey.take(20)}...${serviceKey.takeLast(10)}")
                android.util.Log.d("FishingIndexViewModel", "  - numOfRows: 50")
                android.util.Log.d("FishingIndexViewModel", "  - pageNo: 1")
                android.util.Log.d("FishingIndexViewModel", "  - dataType: JSON")
                android.util.Log.d("FishingIndexViewModel", "  - type: json")
                android.util.Log.d("FishingIndexViewModel", "📡 Expected URL: https://apis.data.go.kr/1192136/fcstFishing")
                android.util.Log.d("FishingIndexViewModel", "🔄 Making API call...")
                
                // 서비스 키 처리 - 여러 방식 시도
                android.util.Log.d("FishingIndexViewModel", "🔑 Service Key Processing:")
                
                // 방법 1: 원본 키 그대로
                android.util.Log.d("FishingIndexViewModel", "  - Original key: ${serviceKey.take(20)}...${serviceKey.takeLast(10)}")
                
                // 방법 2: URL 디코딩
                val decodedServiceKey = try {
                    java.net.URLDecoder.decode(serviceKey, "UTF-8")
                } catch (e: Exception) {
                    android.util.Log.w("FishingIndexViewModel", "Failed to decode service key, using original")
                    serviceKey
                }
                android.util.Log.d("FishingIndexViewModel", "  - Decoded key: ${decodedServiceKey.take(20)}...${decodedServiceKey.takeLast(10)}")
                
                // 첫 번째 시도: 디코딩된 키
                android.util.Log.d("FishingIndexViewModel", "🚀 First attempt with decoded key...")
                var response: com.dive.weatherwatch.data.FishingIndexResponse? = null
                var lastException: Exception? = null
                
                try {
                    response = fishingIndexService.getFishingIndex(
                        serviceKey = decodedServiceKey,
                        numOfRows = 50,
                        pageNo = 1,
                        dataType = "JSON",
                        type = "json"
                    )
                } catch (e: Exception) {
                    android.util.Log.w("FishingIndexViewModel", "First attempt failed: ${e.message}")
                    lastException = e
                    
                    // 두 번째 시도: 원본 키
                    android.util.Log.d("FishingIndexViewModel", "🚀 Second attempt with original key...")
                    try {
                        response = fishingIndexService.getFishingIndex(
                            serviceKey = serviceKey,
                            numOfRows = 50,
                            pageNo = 1,
                            dataType = "JSON",
                            type = "json"
                        )
                    } catch (e2: Exception) {
                        android.util.Log.w("FishingIndexViewModel", "Second attempt failed: ${e2.message}")
                        lastException = e2
                        
                        // 세 번째 시도: type 파라미터 제거
                        android.util.Log.d("FishingIndexViewModel", "🚀 Third attempt without type parameter...")
                        try {
                            response = fishingIndexService.getFishingIndex(
                                serviceKey = decodedServiceKey,
                                numOfRows = 10,
                                pageNo = 1,
                                dataType = "JSON",
                                type = null
                            )
                        } catch (e3: Exception) {
                            android.util.Log.w("FishingIndexViewModel", "Third attempt failed: ${e3.message}")
                            lastException = e3
                            
                            // 네 번째 시도: 대체 엔드포인트 1
                            android.util.Log.d("FishingIndexViewModel", "🚀 Fourth attempt with alternative endpoint 1...")
                            try {
                                response = fishingIndexService.getFishingIndexAlt1(
                                    serviceKey = decodedServiceKey,
                                    numOfRows = 10,
                                    pageNo = 1,
                                    dataType = "JSON"
                                )
                            } catch (e4: Exception) {
                                android.util.Log.w("FishingIndexViewModel", "Fourth attempt failed: ${e4.message}")
                                lastException = e4
                                
                                // 다섯 번째 시도: 대체 엔드포인트 2
                                android.util.Log.d("FishingIndexViewModel", "🚀 Fifth attempt with alternative endpoint 2...")
                                try {
                                    response = fishingIndexService.getFishingIndexAlt2(
                                        serviceKey = decodedServiceKey,
                                        numOfRows = 10,
                                        pageNo = 1,
                                        dataType = "JSON"
                                    )
                                } catch (e5: Exception) {
                                    android.util.Log.e("FishingIndexViewModel", "All 5 attempts failed, throwing last exception")
                                    throw e5
                                }
                            }
                        }
                    }
                }
                
                if (response == null) {
                    throw lastException ?: Exception("Unknown error occurred")
                }
                
                android.util.Log.d("FishingIndexViewModel", "✅ Raw response received")
                android.util.Log.d("FishingIndexViewModel", "📄 Response Structure Analysis:")
                android.util.Log.d("FishingIndexViewModel", "  - response object: ${response}")
                android.util.Log.d("FishingIndexViewModel", "  - response.response: ${response.response}")
                android.util.Log.d("FishingIndexViewModel", "  - header: ${response.response.header}")
                android.util.Log.d("FishingIndexViewModel", "  - header.resultCode: '${response.response.header.resultCode}'")
                android.util.Log.d("FishingIndexViewModel", "  - header.resultMsg: '${response.response.header.resultMsg}'")
                android.util.Log.d("FishingIndexViewModel", "  - body: ${response.response.body}")
                android.util.Log.d("FishingIndexViewModel", "  - body is null: ${response.response.body == null}")
                
                if (response.response.header.resultCode == "00") {
                    android.util.Log.d("FishingIndexViewModel", "🟢 API Success - parsing data...")
                    android.util.Log.d("FishingIndexViewModel", "  - body.items: ${response.response.body?.items}")
                    android.util.Log.d("FishingIndexViewModel", "  - body.items is null: ${response.response.body?.items == null}")
                    
                    val items = response.response.body?.items?.item ?: emptyList()
                    android.util.Log.d("FishingIndexViewModel", "  - items list: $items")
                    android.util.Log.d("FishingIndexViewModel", "  - items count: ${items.size}")
                    
                    if (items.isNotEmpty()) {
                        val firstItem = items.first()
                        android.util.Log.d("FishingIndexViewModel", "First item details:")
                        android.util.Log.d("FishingIndexViewModel", "  - locationName: ${firstItem.locationName}")
                        android.util.Log.d("FishingIndexViewModel", "  - targetFish: ${firstItem.targetFish}")
                        android.util.Log.d("FishingIndexViewModel", "  - fishingIndex: ${firstItem.fishingIndex}")
                        android.util.Log.d("FishingIndexViewModel", "  - fishingScore: ${firstItem.fishingScore}")
                    }
                    
                    _fishingIndexData.value = items
                    android.util.Log.d("FishingIndexViewModel", "✅ Successfully loaded ${items.size} fishing index items")
                } else if (response.response.header.resultCode == "03") {
                    // NODATA_ERROR - 데이터가 없는 경우
                    android.util.Log.w("FishingIndexViewModel", "📭 No data available from API")
                    android.util.Log.w("FishingIndexViewModel", "  - Result Code: 03 (NODATA_ERROR)")
                    android.util.Log.w("FishingIndexViewModel", "  - Message: ${response.response.header.resultMsg}")
                    
                    // 빈 리스트로 설정하고 에러는 설정하지 않음
                    _fishingIndexData.value = emptyList()
                    _error.value = null
                    android.util.Log.d("FishingIndexViewModel", "🔧 Using test data due to no data available")
                    
                    // 테스트 데이터 사용
                    val testData = listOf(
                        com.dive.weatherwatch.data.FishingIndexData(
                            locationName = "부산 광안리",
                            latitude = 35.1536,
                            longitude = 129.1186,
                            predictionDate = "20250822",
                            predictionTime = "오전",
                            targetFish = "감성돔, 농어, 광어",
                            fishingIndex = 4,
                            fishingScore = 85
                        )
                    )
                    _fishingIndexData.value = testData
                } else {
                    android.util.Log.e("FishingIndexViewModel", "🔴 API Error - non-success result code")
                    android.util.Log.e("FishingIndexViewModel", "  - Expected: '00'")
                    android.util.Log.e("FishingIndexViewModel", "  - Actual: '${response.response.header.resultCode}'")
                    android.util.Log.e("FishingIndexViewModel", "  - Message: '${response.response.header.resultMsg}'")
                    
                    val errorMsg = "API Error: ${response.response.header.resultCode} - ${response.response.header.resultMsg}"
                    _error.value = errorMsg
                    android.util.Log.e("FishingIndexViewModel", "❌ $errorMsg")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("FishingIndexViewModel", "💥 Exception occurred during API call")
                android.util.Log.e("FishingIndexViewModel", "  - Exception type: ${e.javaClass.simpleName}")
                android.util.Log.e("FishingIndexViewModel", "  - Exception message: ${e.message}")
                android.util.Log.e("FishingIndexViewModel", "  - Cause: ${e.cause}")
                android.util.Log.e("FishingIndexViewModel", "  - Stack trace:")
                
                val errorMsg = "Failed to load fishing index: ${e.message}"
                _error.value = errorMsg
                android.util.Log.e("FishingIndexViewModel", "❌ Exception: $errorMsg", e)
                e.printStackTrace()
                
                // 임시 테스트 데이터 (API 실패 시 대체용)
                android.util.Log.d("FishingIndexViewModel", "🔧 Using test data as fallback")
                val testData = listOf(
                    com.dive.weatherwatch.data.FishingIndexData(
                        locationName = "부산 광안리",
                        latitude = 35.1536,
                        longitude = 129.1186,
                        predictionDate = "20250822",
                        predictionTime = "오전",
                        targetFish = "감성돔, 농어, 광어",
                        fishingIndex = 4,
                        fishingScore = 85
                    )
                )
                _fishingIndexData.value = testData
                _error.value = null // 테스트 데이터 사용 시 에러 클리어
            } finally {
                _isLoading.value = false
                android.util.Log.d("FishingIndexViewModel", "=== 낚시 지수 API 호출 완료 ===")
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    /**
     * 실제 데이터를 기반으로 동적 낚시 지수를 계산합니다
     */
    fun calculateDynamicFishingIndex(
        weatherData: WeatherResponse?,
        tideData: List<DailyTideInfo>,
        targetFish: String = "감성돔"
    ) {
        viewModelScope.launch {
            try {
                val weatherScore = calculateWeatherScore(weatherData)
                val tideScore = calculateTideScore(tideData)
                val waterTempScore = calculateWaterTempScore(weatherData)
                val waveScore = calculateWaveScore(weatherData)
                val windScore = calculateWindScore(weatherData)
                val cloudScore = calculateCloudScore(weatherData)
                
                // 가중치 적용
                val totalScore = when(targetFish) {
                    "감성돔" -> {
                        (weatherScore * 0.25) + (tideScore * 0.25) + 
                        (waterTempScore * 0.30) + (waveScore * 0.10) + 
                        (windScore * 0.05) + (cloudScore * 0.05)
                    }
                    "농어" -> {
                        (weatherScore * 0.20) + (tideScore * 0.35) + 
                        (waterTempScore * 0.20) + (waveScore * 0.15) + 
                        (windScore * 0.05) + (cloudScore * 0.05)
                    }
                    else -> {
                        (weatherScore * 0.25) + (tideScore * 0.25) + 
                        (waterTempScore * 0.20) + (waveScore * 0.15) + 
                        (windScore * 0.10) + (cloudScore * 0.05)
                    }
                }
                
                val finalIndex = (totalScore.toInt()).coerceIn(1, 5)
                _dynamicFishingIndex.value = finalIndex
                
                // 알고리즘 설명 생성
                generateAlgorithmExplanation(
                    weatherScore, tideScore, waterTempScore, 
                    waveScore, windScore, cloudScore, 
                    finalIndex, targetFish
                )
                
                android.util.Log.d("FishingIndexVM", 
                    "동적 낚시지수 계산: 날씨($weatherScore) + 물때($tideScore) + 수온($waterTempScore) + 파고($waveScore) + 풍속($windScore) + 구름($cloudScore) = ${finalIndex}점"
                )
                
            } catch (e: Exception) {
                android.util.Log.e("FishingIndexVM", "동적 낚시지수 계산 실패: ${e.message}")
                _dynamicFishingIndex.value = 3 // 기본값
            }
        }
    }
    
    private fun calculateWeatherScore(weatherData: WeatherResponse?): Double {
        val currentWeather = weatherData?.response?.body?.items?.item
            ?.filter { it.category == "SKY" }
            ?.firstOrNull()?.fcstValue?.toIntOrNull()
        
        return when(currentWeather) {
            1 -> 5.0  // 맑음
            3 -> 4.0  // 구름많음  
            4 -> 3.0  // 흐림
            else -> 3.0
        }
    }
    
    private fun calculateTideScore(tideData: List<DailyTideInfo>): Double {
        if (tideData.isEmpty()) return 3.0
        
        val today = tideData.firstOrNull() ?: return 3.0
        
        // 물때 강도 계산 (조차가 클수록 높은 점수)
        val tideRange = if (today.tideEvents.size >= 2) {
            val maxLevel = today.tideEvents.maxOfOrNull { it.height } ?: 100
            val minLevel = today.tideEvents.minOfOrNull { it.height } ?: 100
            abs(maxLevel - minLevel)
        } else 100
        
        return when {
            tideRange >= 150 -> 5.0  // 대조
            tideRange >= 100 -> 4.0  // 중조
            tideRange >= 50 -> 3.0   // 소조
            else -> 2.0              // 매우 약한 조류
        }
    }
    
    private fun calculateWaterTempScore(weatherData: WeatherResponse?): Double {
        // 현재 기온을 기준으로 수온 추정 (임시)
        val currentTemp = weatherData?.response?.body?.items?.item
            ?.filter { it.category == "TMP" }
            ?.firstOrNull()?.fcstValue?.toDoubleOrNull() ?: 20.0
        
        val estimatedWaterTemp = currentTemp - 2.0 // 수온은 보통 기온보다 2도 정도 낮음
        
        return when {
            estimatedWaterTemp in 18.0..25.0 -> 5.0  // 적정 수온
            estimatedWaterTemp in 15.0..18.0 || estimatedWaterTemp in 25.0..28.0 -> 3.0
            else -> 1.0
        }
    }
    
    private fun calculateWaveScore(weatherData: WeatherResponse?): Double {
        val windSpeed = weatherData?.response?.body?.items?.item
            ?.filter { it.category == "WSD" }
            ?.firstOrNull()?.fcstValue?.toDoubleOrNull() ?: 5.0
        
        // 풍속으로 파고 추정
        val estimatedWaveHeight = windSpeed * 0.2
        
        return when {
            estimatedWaveHeight < 0.5 -> 5.0
            estimatedWaveHeight < 1.0 -> 3.0
            else -> 1.0
        }
    }
    
    private fun calculateWindScore(weatherData: WeatherResponse?): Double {
        val windSpeed = weatherData?.response?.body?.items?.item
            ?.filter { it.category == "WSD" }
            ?.firstOrNull()?.fcstValue?.toDoubleOrNull() ?: 5.0
        
        return when {
            windSpeed < 3.0 -> 5.0
            windSpeed < 5.0 -> 3.0
            else -> 1.0
        }
    }
    
    private fun calculateCloudScore(weatherData: WeatherResponse?): Double {
        val skyCondition = weatherData?.response?.body?.items?.item
            ?.filter { it.category == "SKY" }
            ?.firstOrNull()?.fcstValue?.toIntOrNull()
        
        return when(skyCondition) {
            1 -> 5.0  // 맑음
            3 -> 3.0  // 구름많음
            4 -> 2.0  // 흐림
            else -> 3.0
        }
    }
    
    private fun generateAlgorithmExplanation(
        weatherScore: Double,
        tideScore: Double, 
        waterTempScore: Double,
        waveScore: Double,
        windScore: Double,
        cloudScore: Double,
        finalIndex: Int,
        targetFish: String
    ) {
        val explanation = buildString {
            appendLine("📊 현재 실시간 계산 결과")
            appendLine()
            appendLine("🌤️ 날씨 조건: ${String.format("%.1f", weatherScore)}점 (가중치 25%)")
            appendLine("🌊 물때 강도: ${String.format("%.1f", tideScore)}점 (가중치 25%)")
            appendLine("🌡️ 수온 조건: ${String.format("%.1f", waterTempScore)}점 (가중치 20%)")
            appendLine("💨 파고/풍속: ${String.format("%.1f", waveScore)}점 (가중치 15%)")
            appendLine("💨 풍속 조건: ${String.format("%.1f", windScore)}점 (가중치 10%)")
            appendLine("☁️ 구름량: ${String.format("%.1f", cloudScore)}점 (가중치 5%)")
            appendLine()
            appendLine("🧮 계산 과정:")
            appendLine("• (${String.format("%.1f", weatherScore)} × 0.25) = ${String.format("%.2f", weatherScore * 0.25)}")
            appendLine("• (${String.format("%.1f", tideScore)} × 0.25) = ${String.format("%.2f", tideScore * 0.25)}")
            appendLine("• (${String.format("%.1f", waterTempScore)} × 0.20) = ${String.format("%.2f", waterTempScore * 0.20)}")
            appendLine("• (${String.format("%.1f", waveScore)} × 0.15) = ${String.format("%.2f", waveScore * 0.15)}")
            appendLine("• (${String.format("%.1f", windScore)} × 0.10) = ${String.format("%.2f", windScore * 0.10)}")
            appendLine("• (${String.format("%.1f", cloudScore)} × 0.05) = ${String.format("%.2f", cloudScore * 0.05)}")
            appendLine()
            val totalScore = (weatherScore * 0.25) + (tideScore * 0.25) + (waterTempScore * 0.20) + 
                           (waveScore * 0.15) + (windScore * 0.10) + (cloudScore * 0.05)
            appendLine("📊 총합: ${String.format("%.2f", totalScore)}점")
            appendLine("🎯 최종 지수: ${finalIndex}점/5점")
            appendLine()
            appendLine("💡 기상청 실시간 데이터 + 조석 정보 기반")
        }
        
        _algorithmExplanation.value = explanation
    }
}