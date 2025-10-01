package com.dive.weatherwatch.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dive.weatherwatch.data.FishingPoint
import com.dive.weatherwatch.data.FishingPointSampleData
import com.dive.weatherwatch.data.toFishingPoint
import com.dive.weatherwatch.di.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FishingPointViewModel : ViewModel() {
    
    private val _fishingPoints = MutableStateFlow<List<FishingPoint>>(emptyList())
    val fishingPoints: StateFlow<List<FishingPoint>> = _fishingPoints.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _selectedPoint = MutableStateFlow<FishingPoint?>(null)
    val selectedPoint: StateFlow<FishingPoint?> = _selectedPoint.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val badaTimeService = NetworkModule.badaTimeService
    
    init {
        // 초기에는 샘플 데이터로 시작 (위치 정보가 없을 때)
        loadSampleData()
        android.util.Log.d("FishingPointViewModel", "초기화: 샘플 데이터 로드")
    }
    
    private fun loadSampleData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                android.util.Log.d("FishingPointViewModel", "샘플 데이터 로드")
                val sampleData = FishingPointSampleData.getSampleFishingPoints()
                android.util.Log.d("FishingPointViewModel", "샘플 데이터 첫 번째 아이템: ${sampleData.firstOrNull()?.name}")
                android.util.Log.d("FishingPointViewModel", "샘플 데이터 forecast: ${sampleData.firstOrNull()?.forecast}")
                android.util.Log.d("FishingPointViewModel", "샘플 데이터 notice: ${sampleData.firstOrNull()?.notice}")
                _fishingPoints.value = sampleData
            } catch (e: Exception) {
                android.util.Log.e("FishingPointViewModel", "샘플 데이터 로드 실패", e)
                _error.value = "데이터 로드 실패: ${e.message}"
                _fishingPoints.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadFishingPoints(lat: Double, lon: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            android.util.Log.d("FishingPointViewModel", "낚시 포인트 API 호출: lat=$lat, lon=$lon")
            
            try {
                val response = badaTimeService.getFishingPoints(lat, lon)
                
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    android.util.Log.d("FishingPointViewModel", "API 응답 성공: ${apiResponse?.fishingPoint?.size}개의 포인트")
                    
                    if (apiResponse?.fishingPoint != null) {
                        // API 응답 디버깅
                        android.util.Log.d("FishingPointViewModel", "API info 데이터: ${apiResponse.info}")
                        android.util.Log.d("FishingPointViewModel", "intro: '${apiResponse.info?.intro}'")
                        android.util.Log.d("FishingPointViewModel", "introBom: '${apiResponse.info?.introBom}'")
                        android.util.Log.d("FishingPointViewModel", "forecast: '${apiResponse.info?.forecast}'")
                        android.util.Log.d("FishingPointViewModel", "forecastBom: '${apiResponse.info?.forecastBom}'")
                        android.util.Log.d("FishingPointViewModel", "notice: '${apiResponse.info?.notice}'")
                        android.util.Log.d("FishingPointViewModel", "noticeBom: '${apiResponse.info?.noticeBom}'")
                        
                        val fishingPoints = apiResponse.fishingPoint.map { point ->
                            point.toFishingPoint(apiResponse.info)
                        }
                        
                        android.util.Log.d("FishingPointViewModel", "변환된 첫 번째 포인트: ${fishingPoints.firstOrNull()?.name}")
                        android.util.Log.d("FishingPointViewModel", "변환된 intro: '${fishingPoints.firstOrNull()?.intro}'")
                        android.util.Log.d("FishingPointViewModel", "변환된 forecast: '${fishingPoints.firstOrNull()?.forecast}'")
                        android.util.Log.d("FishingPointViewModel", "변환된 notice: '${fishingPoints.firstOrNull()?.notice}'")
                        
                        _fishingPoints.value = fishingPoints
                    } else {
                        android.util.Log.w("FishingPointViewModel", "API 응답이 비어있음, 샘플 데이터 사용")
                        val sampleData = FishingPointSampleData.getSampleFishingPoints()
                        _fishingPoints.value = sampleData
                    }
                } else {
                    android.util.Log.e("FishingPointViewModel", "API 호출 실패: ${response.code()} - ${response.message()}")
                    _error.value = "API 호출 실패: ${response.message()}"
                    
                    // API 실패 시 샘플 데이터로 폴백
                    android.util.Log.d("FishingPointViewModel", "API 실패로 샘플 데이터 사용")
                    val sampleData = FishingPointSampleData.getSampleFishingPoints()
                    _fishingPoints.value = sampleData
                }
            } catch (e: Exception) {
                android.util.Log.e("FishingPointViewModel", "API 호출 중 오류 발생", e)
                _error.value = "네트워크 오류: ${e.message}"
                
                // 예외 발생 시 샘플 데이터로 폴백
                android.util.Log.d("FishingPointViewModel", "예외 발생으로 샘플 데이터 사용")
                try {
                    val sampleData = FishingPointSampleData.getSampleFishingPoints()
                    _fishingPoints.value = sampleData
                } catch (sampleException: Exception) {
                    android.util.Log.e("FishingPointViewModel", "샘플 데이터 로드도 실패", sampleException)
                    _fishingPoints.value = emptyList()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun selectPoint(point: FishingPoint) {
        _selectedPoint.value = point
    }
    
    fun clearSelectedPoint() {
        _selectedPoint.value = null
    }
    
    fun getNearbyPoints(userLat: Double, userLon: Double, radiusKm: Double = 50.0): List<FishingPoint> {
        return _fishingPoints.value.filter { point ->
            val distance = calculateDistance(userLat, userLon, point.lat, point.lon)
            distance <= radiusKm
        }.sortedBy { point ->
            calculateDistance(userLat, userLon, point.lat, point.lon)
        }
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return earthRadius * c
    }
    
    fun refreshPoints(lat: Double? = null, lon: Double? = null) {
        if (lat != null && lon != null) {
            android.util.Log.d("FishingPointViewModel", "위치 기반 새로고침: lat=$lat, lon=$lon")
            loadFishingPoints(lat, lon)
        } else {
            android.util.Log.d("FishingPointViewModel", "샘플 데이터로 새로고침")
            loadSampleData()
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}