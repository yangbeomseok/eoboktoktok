package com.dive.weatherwatch.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dive.weatherwatch.data.BadaTimeCurrentResponse
import com.dive.weatherwatch.data.BadaTimeForecastResponse
import com.dive.weatherwatch.data.BadaTimeTideResponse
import com.dive.weatherwatch.data.WaterTemperatureData
import com.dive.weatherwatch.data.findClosest
import com.dive.weatherwatch.di.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BadaTimeViewModel : ViewModel() {
    
    private val _currentWeather = MutableStateFlow<BadaTimeCurrentResponse?>(null)
    val currentWeather: StateFlow<BadaTimeCurrentResponse?> = _currentWeather.asStateFlow()
    
    private val _forecastWeather = MutableStateFlow<List<BadaTimeForecastResponse>>(emptyList())
    val forecastWeather: StateFlow<List<BadaTimeForecastResponse>> = _forecastWeather.asStateFlow()
    
    private val _tideData = MutableStateFlow<List<BadaTimeTideResponse>>(emptyList())
    val tideData: StateFlow<List<BadaTimeTideResponse>> = _tideData.asStateFlow()
    
    private val _waterTemperature = MutableStateFlow<String?>(null)
    val waterTemperature: StateFlow<String?> = _waterTemperature.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true) // 초기 로딩 상태로 시작
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val badaTimeService = NetworkModule.badaTimeService
    
    fun loadCurrentWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            android.util.Log.d("BadaTimeViewModel", "Loading current weather for lat=$lat, lon=$lon")
            
            // 최대 3번 재시도
            var lastException: Exception? = null
            for (attempt in 0..2) {
                try {
                    android.util.Log.d("BadaTimeViewModel", "Current weather attempt ${attempt + 1}/3")
                    
                    val response = badaTimeService.getCurrentWeather(lat, lon)
                    android.util.Log.d("BadaTimeViewModel", "Current weather response: ${response.code()}")
                    
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        android.util.Log.d("BadaTimeViewModel", "Current weather API response: $apiResponse")
                        
                        // Extract current weather (first item in weather array)
                        val weatherData = apiResponse?.weather?.firstOrNull()
                        android.util.Log.d("BadaTimeViewModel", "Current weather data: $weatherData")
                        _currentWeather.value = weatherData
                        _isLoading.value = false
                        return@launch // 성공하면 함수 종료
                    } else {
                        val errorMsg = "현재 날씨 정보를 불러올 수 없습니다: ${response.code()}"
                        android.util.Log.e("BadaTimeViewModel", errorMsg)
                        if (attempt == 2) _error.value = errorMsg // 마지막 시도에서만 에러 설정
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    lastException = e
                    android.util.Log.w("BadaTimeViewModel", "Timeout on attempt ${attempt + 1}/3", e)
                    if (attempt < 2) {
                        kotlinx.coroutines.delay(2000) // 2초 대기 후 재시도
                    }
                } catch (e: java.net.UnknownHostException) {
                    val errorMsg = "서버에 연결할 수 없습니다. 인터넷 연결을 확인해주세요."
                    android.util.Log.e("BadaTimeViewModel", errorMsg, e)
                    _error.value = errorMsg
                    break // 네트워크 연결 문제는 재시도 안함
                } catch (e: Exception) {
                    lastException = e
                    android.util.Log.w("BadaTimeViewModel", "Error on attempt ${attempt + 1}/3", e)
                    if (attempt < 2) {
                        kotlinx.coroutines.delay(1000) // 1초 대기 후 재시도
                    }
                }
            }
            
            // 모든 재시도가 실패한 경우
            if (_currentWeather.value == null && _error.value == null) {
                val errorMsg = "연결에 실패했습니다: ${lastException?.message ?: "알 수 없는 오류"}"
                android.util.Log.e("BadaTimeViewModel", errorMsg, lastException)
                _error.value = errorMsg
            }
            
            _isLoading.value = false
        }
    }
    
    fun loadForecastWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            android.util.Log.d("BadaTimeViewModel", "Loading forecast weather for lat=$lat, lon=$lon")
            
            // 최대 3번 재시도
            var lastException: Exception? = null
            for (attempt in 0..2) {
                try {
                    android.util.Log.d("BadaTimeViewModel", "Forecast weather attempt ${attempt + 1}/3")
                    
                    val response = badaTimeService.getForecastWeather(lat, lon)
                    android.util.Log.d("BadaTimeViewModel", "Forecast weather response: ${response.code()}")
                    
                    if (response.isSuccessful) {
                        val forecastData = response.body() ?: emptyList()
                        android.util.Log.d("BadaTimeViewModel", "=== Forecast 데이터 상세 분석 ===")
                        android.util.Log.d("BadaTimeViewModel", "Forecast weather data size: ${forecastData.size}")
                        
                        // 처음 5개 항목의 상세 정보 로그
                        forecastData.take(5).forEachIndexed { index, item ->
                            android.util.Log.d("BadaTimeViewModel", "[$index] ymdt=${item.ymdt}, aplYmdt=${item.aplYmdt}, temp=${item.temp}, sky=${item.sky}, rain=${item.rain}")
                        }
                        
                        _forecastWeather.value = forecastData
                        return@launch // 성공하면 함수 종료
                    } else {
                        val errorMsg = "예보 정보를 불러올 수 없습니다: ${response.code()}"
                        android.util.Log.e("BadaTimeViewModel", errorMsg)
                        if (attempt == 2) _error.value = errorMsg // 마지막 시도에서만 에러 설정
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    lastException = e
                    android.util.Log.w("BadaTimeViewModel", "Forecast timeout on attempt ${attempt + 1}/3", e)
                    if (attempt < 2) {
                        kotlinx.coroutines.delay(2000) // 2초 대기 후 재시도
                    }
                } catch (e: java.net.UnknownHostException) {
                    val errorMsg = "서버에 연결할 수 없습니다. 인터넷 연결을 확인해주세요."
                    android.util.Log.e("BadaTimeViewModel", errorMsg, e)
                    if (_error.value == null) _error.value = errorMsg // 현재 날씨가 성공했다면 에러 덮어쓰지 않음
                    break // 네트워크 연결 문제는 재시도 안함
                } catch (e: Exception) {
                    lastException = e
                    android.util.Log.w("BadaTimeViewModel", "Forecast error on attempt ${attempt + 1}/3", e)
                    if (attempt < 2) {
                        kotlinx.coroutines.delay(1000) // 1초 대기 후 재시도
                    }
                }
            }
            
            // 모든 재시도가 실패한 경우 (하지만 현재 날씨가 성공했다면 에러 메시지는 덮어쓰지 않음)
            if (_forecastWeather.value.isEmpty() && _error.value == null) {
                val errorMsg = "예보 연결에 실패했습니다: ${lastException?.message ?: "알 수 없는 오류"}"
                android.util.Log.e("BadaTimeViewModel", errorMsg, lastException)
                _error.value = errorMsg
            }
        }
    }
    
    fun loadTideData(lat: Double, lon: Double) {
        viewModelScope.launch {
            android.util.Log.e("BadaTimeViewModel", "🚨🚨🚨 === STARTING TIDE DATA LOAD ===")
            android.util.Log.e("BadaTimeViewModel", "🌊 Loading tide data for lat=$lat, lon=$lon")
            
            // 상태 초기화 로그
            _isLoading.value = true
            _error.value = null
            android.util.Log.e("BadaTimeViewModel", "🌊 Set loading=true, error=null")
            
            // 최대 3번 재시도
            var lastException: Exception? = null
            for (attempt in 0..2) {
                try {
                    android.util.Log.e("BadaTimeViewModel", "🌊 Tide data attempt ${attempt + 1}/3")
                    android.util.Log.e("BadaTimeViewModel", "🌊 Calling URL: https://www.badatime.com/DIVE/tide?lat=$lat&lon=$lon&key=$BADA_TIME_API_KEY")
                    
                    val response = badaTimeService.getTideData(lat, lon)
                    android.util.Log.e("BadaTimeViewModel", "🌊 Tide data response: ${response.code()}")
                    android.util.Log.e("BadaTimeViewModel", "🌊 Response headers: ${response.headers()}")
                    android.util.Log.e("BadaTimeViewModel", "🌊 Response success: ${response.isSuccessful}")
                    android.util.Log.e("BadaTimeViewModel", "🌊 Response message: ${response.message()}")
                    
                    if (response.isSuccessful) {
                        val tideData = response.body() ?: emptyList()
                        android.util.Log.e("BadaTimeViewModel", "🌊 === TIDE 데이터 상세 분석 ===")
                        android.util.Log.e("BadaTimeViewModel", "🌊 Tide data size: ${tideData.size}")
                        android.util.Log.e("BadaTimeViewModel", "🌊 Raw response body: ${response.body()}")
                        android.util.Log.e("BadaTimeViewModel", "🌊 Response body type: ${response.body()?.javaClass?.simpleName}")
                        
                        // 응답이 빈 리스트인 경우 추가 디버깅
                        if (tideData.isEmpty()) {
                            android.util.Log.e("BadaTimeViewModel", "⚠️⚠️ TIDE DATA IS EMPTY! This might be a parsing issue.")
                            android.util.Log.e("BadaTimeViewModel", "⚠️⚠️ Response headers: ${response.headers()}")
                            android.util.Log.e("BadaTimeViewModel", "⚠️⚠️ Response raw string from body: ${response.body().toString()}")
                        }
                        
                        // 처음 3개 항목의 상세 정보 로그
                        tideData.take(3).forEachIndexed { index, item ->
                            android.util.Log.e("BadaTimeViewModel", "🔍🔍 [$index] date=${item.thisDate}, area=${item.selectedArea?.replace("<br>", "")}")
                            android.util.Log.e("BadaTimeViewModel", "🔍🔍 [$index] *** pMul (tideType) = '${item.tideType}' ***")
                            android.util.Log.e("BadaTimeViewModel", "🔍🔍 [$index] sun=${item.sunRiseSet}, moon=${item.moonRiseSet}")
                            android.util.Log.e("BadaTimeViewModel", "🔍🔍 [$index] times: ${item.tideTime1} | ${item.tideTime2} | ${item.tideTime3} | ${item.tideTime4}")
                        }
                        
                        _tideData.value = tideData
                        _isLoading.value = false
                        android.util.Log.e("BadaTimeViewModel", "🎉🎉🎉 Successfully loaded ${tideData.size} tide data items")
                        return@launch // 성공하면 함수 종료
                    } else {
                        val errorMsg = "물때 정보를 불러올 수 없습니다: ${response.code()}"
                        android.util.Log.e("BadaTimeViewModel", errorMsg)
                        if (attempt == 2) _error.value = errorMsg // 마지막 시도에서만 에러 설정
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    lastException = e
                    android.util.Log.w("BadaTimeViewModel", "Tide timeout on attempt ${attempt + 1}/3", e)
                    if (attempt < 2) {
                        kotlinx.coroutines.delay(2000) // 2초 대기 후 재시도
                    }
                } catch (e: java.net.UnknownHostException) {
                    val errorMsg = "서버에 연결할 수 없습니다. 인터넷 연결을 확인해주세요."
                    android.util.Log.e("BadaTimeViewModel", errorMsg, e)
                    if (_error.value == null) _error.value = errorMsg
                    break // 네트워크 연결 문제는 재시도 안함
                } catch (e: Exception) {
                    lastException = e
                    android.util.Log.w("BadaTimeViewModel", "Tide error on attempt ${attempt + 1}/3", e)
                    if (attempt < 2) {
                        kotlinx.coroutines.delay(1000) // 1초 대기 후 재시도
                    }
                }
            }
            
            // 모든 재시도가 실패한 경우
            if (_tideData.value.isEmpty() && _error.value == null) {
                val errorMsg = "물때 연결에 실패했습니다: ${lastException?.message ?: "알 수 없는 오류"}"
                android.util.Log.e("BadaTimeViewModel", errorMsg, lastException)
                _error.value = errorMsg
            }
            _isLoading.value = false
            android.util.Log.e("BadaTimeViewModel", "💥💥💥 FINAL: tideData.size=${_tideData.value.size}, error=${_error.value}, loading=${_isLoading.value}")
        }
    }

    fun loadWaterTemperature(lat: Double, lon: Double) {
        viewModelScope.launch {
            android.util.Log.d("BadaTimeViewModel", "Loading water temperature for lat=$lat, lon=$lon")
            
            try {
                val response = badaTimeService.getWaterTemperature(lat, lon)
                android.util.Log.d("BadaTimeViewModel", "Water temperature response: ${response.code()}")
                
                if (response.isSuccessful) {
                    val temperatureData = response.body() ?: emptyList()
                    android.util.Log.d("BadaTimeViewModel", "Water temperature data size: ${temperatureData.size}")
                    
                    // 가장 가까운 관측소 찾기
                    val closestData = temperatureData.findClosest()
                    android.util.Log.d("BadaTimeViewModel", "Closest water temperature data: $closestData")
                    
                    val waterTemp = closestData?.obsWt
                    android.util.Log.d("BadaTimeViewModel", "Water temperature: $waterTemp")
                    
                    _waterTemperature.value = waterTemp
                } else {
                    android.util.Log.e("BadaTimeViewModel", "Failed to load water temperature: ${response.code()}")
                    _waterTemperature.value = null
                }
            } catch (e: Exception) {
                android.util.Log.e("BadaTimeViewModel", "Error loading water temperature", e)
                _waterTemperature.value = null
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}