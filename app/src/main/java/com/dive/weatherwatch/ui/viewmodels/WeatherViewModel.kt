package com.dive.weatherwatch.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dive.weatherwatch.data.WeatherResponse
import com.dive.weatherwatch.di.NetworkModule
import com.dive.weatherwatch.utils.GpsConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WeatherViewModel : ViewModel() {

    private val _weatherData = MutableStateFlow<WeatherResponse?>(null)
    val weatherData: StateFlow<WeatherResponse?> = _weatherData

    private val _locationName = MutableStateFlow("")
    val locationName: StateFlow<String> = _locationName
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    private val _latitude = MutableStateFlow<Double?>(null)
    val latitude: StateFlow<Double?> = _latitude
    
    private val _longitude = MutableStateFlow<Double?>(null)
    val longitude: StateFlow<Double?> = _longitude

    fun updateLocationName(name: String) {
        _locationName.value = name
    }
    
    fun updateLocation(lat: Double, lon: Double) {
        _latitude.value = lat
        _longitude.value = lon
    }

    fun updateErrorMessage(message: String?) {
        _errorMessage.value = message
    }

    fun fetchWeatherData(
        serviceKey: String,
        baseDate: String,
        baseTime: String,
        lat: Double,
        lon: Double,
        locationName: String
    ) {
        android.util.Log.d("WeatherViewModel", "fetchWeatherData called for location: $locationName")
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                android.util.Log.d("WeatherViewModel", "Converting GPS coordinates: lat=$lat, lon=$lon")
                val (nx, ny) = GpsConverter.toXY(lat, lon)
                _locationName.value = locationName
                _latitude.value = lat
                _longitude.value = lon
                android.util.Log.d("WeatherViewModel", "Converted to Grid coordinates - NX: $nx, NY: $ny")

                android.util.Log.d("WeatherViewModel", "API Call Parameters:")
                android.util.Log.d("WeatherViewModel", "  serviceKey: ${serviceKey.take(50)}...")
                android.util.Log.d("WeatherViewModel", "  numOfRows: 100")
                android.util.Log.d("WeatherViewModel", "  pageNo: 1")
                android.util.Log.d("WeatherViewModel", "  dataType: JSON")
                android.util.Log.d("WeatherViewModel", "  baseDate: $baseDate")
                android.util.Log.d("WeatherViewModel", "  baseTime: $baseTime")
                android.util.Log.d("WeatherViewModel", "  nx: $nx")
                android.util.Log.d("WeatherViewModel", "  ny: $ny")
                
                // 브라우저 테스트용 URL 생성
                val testUrl = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst" +
                        "?serviceKey=$serviceKey" +
                        "&numOfRows=100" +
                        "&pageNo=1" +
                        "&dataType=JSON" +
                        "&base_date=$baseDate" +
                        "&base_time=$baseTime" +
                        "&nx=$nx" +
                        "&ny=$ny"
                android.util.Log.d("WeatherViewModel", "Test URL: $testUrl")

                val response = NetworkModule.weatherService.getWeather(
                    serviceKey = serviceKey,
                    numOfRows = 100,
                    pageNo = 1,
                    dataType = "JSON",
                    baseDate = baseDate,
                    baseTime = baseTime,
                    nx = nx,
                    ny = ny
                )
                
                android.util.Log.d("WeatherViewModel", "API response received - Result Code: ${response.response.header.resultCode}")
                android.util.Log.d("WeatherViewModel", "API response message: ${response.response.header.resultMsg}")
                
                if (response.response.header.resultCode == "00") {
                    // Body와 Items가 null이 아닌지 확인
                    val body = response.response.body
                    if (body != null && body.items != null && body.items.item.isNotEmpty()) {
                        android.util.Log.d("WeatherViewModel", "Number of items: ${body.items.item.size}")
                        _weatherData.value = response
                        android.util.Log.d("WeatherViewModel", "Weather data successfully loaded")
                    } else {
                        _errorMessage.value = "날씨 데이터가 비어있습니다"
                        android.util.Log.e("WeatherViewModel", "API response body or items is null/empty")
                    }
                } else {
                    _errorMessage.value = "API 오류: ${response.response.header.resultMsg}"
                    android.util.Log.e("WeatherViewModel", "API returned error: ${response.response.header.resultMsg}")
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "날씨 데이터 로딩 실패: ${e.message}"
                android.util.Log.e("WeatherViewModel", "Error fetching weather data", e)
                android.util.Log.e("WeatherViewModel", "Exception details: ${e.javaClass.simpleName} - ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}