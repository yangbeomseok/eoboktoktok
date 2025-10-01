package com.dive.weatherwatch.ui.viewmodels

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dive.weatherwatch.ui.screens.fetchCurrentLocation
import com.dive.weatherwatch.services.FishingHotspotService
import com.dive.weatherwatch.services.FishingHotspotNotificationService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationViewModel : ViewModel() {
    
    private val _locationName = MutableStateFlow<String?>(null)
    val locationName: StateFlow<String?> = _locationName
    
    private val _isLocationLoading = MutableStateFlow(false)
    val isLocationLoading: StateFlow<Boolean> = _isLocationLoading
    
    private val _latitude = MutableStateFlow<Double?>(null)
    val latitude: StateFlow<Double?> = _latitude
    
    private val _longitude = MutableStateFlow<Double?>(null)
    val longitude: StateFlow<Double?> = _longitude
    
    private var fishingHotspotService: FishingHotspotService? = null
    private var lastNotifiedSpots = mutableSetOf<String>()
    private var isHotspotsLoaded = false
    
    // 연속적인 위치 추적을 위한 변수들
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: com.google.android.gms.location.LocationCallback? = null
    private var isContinuousTracking = false
    private var lastUpdateTime: Long? = null
    private var trackingStartTime: Long? = null
    private var updateCount = 0

    fun updateLocation(name: String, lat: Double?, lon: Double?) {
        _locationName.value = name
        _latitude.value = lat
        _longitude.value = lon
        _isLocationLoading.value = false
        
        // 위치가 업데이트될 때마다 낚시 핫스팟 체크
        if (lat != null && lon != null) {
            checkNearbyFishingHotspots(lat, lon)
        }
    }

    fun startLocationFetch(context: Context, weatherViewModel: com.dive.weatherwatch.ui.viewmodels.WeatherViewModel) {
        if (_locationName.value != null) {
            android.util.Log.d("LocationViewModel", "✅ Location already available: ${_locationName.value}")
            return // Already have location
        }
        
        // 처음 실행 시 낚시 핫스팟 서비스 초기화
        if (fishingHotspotService == null) {
            fishingHotspotService = FishingHotspotService(context)
            initializeFishingHotspots(context)
        }
        
        android.util.Log.d("LocationViewModel", "🚀 Starting location fetch process...")
        _isLocationLoading.value = true
        val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        
        viewModelScope.launch {
            try {
                android.util.Log.d("LocationViewModel", "Starting GPS location fetch")
                
                // 위치 권한 확인
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && 
                    ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    android.util.Log.e("LocationViewModel", "Location permission not granted")
                    updateLocation("위치 권한 없음", null, null)
                    return@launch
                }
                
                // 실제 GPS 위치 가져오기 (lastLocation 우선, 없으면 실시간 요청)
                var location: android.location.Location? = null
                
                try {
                    location = fusedLocationClient.lastLocation.await()
                    android.util.Log.d("LocationViewModel", "LastLocation result: $location")
                } catch (e: Exception) {
                    android.util.Log.w("LocationViewModel", "Failed to get last location", e)
                }
                
                if (location == null) {
                    android.util.Log.d("LocationViewModel", "LastLocation is null, requesting current location")
                    
                    // 실시간 위치 요청을 위한 suspend 함수 사용
                    try {
                        location = getCurrentLocationSuspend(fusedLocationClient, context)
                        android.util.Log.d("LocationViewModel", "getCurrentLocationSuspend result: $location")
                    } catch (e: Exception) {
                        android.util.Log.e("LocationViewModel", "Failed to get current location", e)
                    }
                }
                
                // 여전히 위치를 받지 못한 경우 기본 위치 사용 (서울 중심)
                if (location == null) {
                    android.util.Log.w("LocationViewModel", "Using fallback location (Seoul City Hall)")
                    location = android.location.Location("").apply {
                        latitude = 37.5665
                        longitude = 126.9780
                    }
                }
                
                if (location != null) {
                    android.util.Log.d("LocationViewModel", "GPS Location found: ${location.latitude}, ${location.longitude}")
                    
                    // Geocoder로 주소 변환 (타임아웃 2초로 제한)
                    val geocoder = Geocoder(context, Locale.KOREAN)
                    val addressResult = withContext(Dispatchers.IO) {
                        try {
                            kotlinx.coroutines.withTimeoutOrNull(2000) { // 2초 타임아웃
                                geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("LocationViewModel", "Geocoding failed quickly", e)
                            null
                        }
                    }
                    
                    if (!addressResult.isNullOrEmpty()) {
                        try {
                            val addresses = addressResult
                            if (!addresses.isNullOrEmpty()) {
                                val address = addresses[0]
                                
                                // SecondWatchScreen과 동일한 방식으로 상세 주소 파싱
                                val fullAddress = address.getAddressLine(0) ?: ""
                                val locationName = fullAddress
                                    .replace("대한민국 ", "")
                                    .replace("특별시", "시")
                                    .replace("광역시", "시")
                                    .split(" ")
                                    .filter { it.isNotEmpty() && it.length > 1 }
                                    .let { parts ->
                                        when {
                                            parts.size >= 3 -> "${parts[0]} ${parts[1]} ${parts.drop(2).joinToString(" ")}"
                                            parts.size == 2 -> "${parts[0]} ${parts[1]}"
                                            parts.size == 1 -> parts[0]
                                            else -> "현재 위치"
                                        }
                                    }
                                
                                android.util.Log.d("LocationViewModel", "Full address: $fullAddress")
                                android.util.Log.d("LocationViewModel", "Parsed location name: $locationName")
                                updateLocation(locationName, location.latitude, location.longitude)
                            } else {
                                android.util.Log.d("LocationViewModel", "No address found, using coordinates")
                                val fallbackName = if (location.latitude == 37.5665 && location.longitude == 126.9780) {
                                    "서울시 중구 (기본위치)"
                                } else {
                                    "현재 위치"
                                }
                                updateLocation(fallbackName, location.latitude, location.longitude)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("LocationViewModel", "Address parsing failed", e)
                            val fallbackName = if (location.latitude == 37.5665 && location.longitude == 126.9780) {
                                "서울시 중구 (기본위치)"
                            } else {
                                "현재 위치"
                            }
                            updateLocation(fallbackName, location.latitude, location.longitude)
                        }
                    } else {
                        // Geocoding이 타임아웃되거나 실패한 경우
                        android.util.Log.w("LocationViewModel", "Geocoding timed out or failed, using coordinates only")
                        val fallbackName = if (location.latitude == 37.5665 && location.longitude == 126.9780) {
                            "서울시 중구 (기본위치)"
                        } else {
                            "현재 위치"
                        }
                        updateLocation(fallbackName, location.latitude, location.longitude)
                    }
                } else {
                    android.util.Log.e("LocationViewModel", "No GPS location available")
                    updateLocation("위치 확인 불가", null, null)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("LocationViewModel", "Failed to get location", e)
                updateLocation("위치 오류", null, null)
            }
        }
    }

    fun isLocationAvailable(): Boolean {
        return _locationName.value != null && _latitude.value != null && _longitude.value != null
    }
    
    // 실시간 위치 요청을 위한 suspend 함수
    private suspend fun getCurrentLocationSuspend(fusedLocationClient: FusedLocationProviderClient, context: Context): android.location.Location? {
        return withContext(Dispatchers.Main) {
            try {
                val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                    com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    10000 // 10초 간격
                )
                .setMinUpdateIntervalMillis(5000) // 최소 5초 간격
                .setMaxUpdates(1) // 한 번만 요청
                .setWaitForAccurateLocation(false) // 정확성보다는 빠른 응답 우선
                .build()
                
                // CompletableDeferred를 사용하여 비동기 콜백을 suspend 함수로 변환
                val locationDeferred = kotlinx.coroutines.CompletableDeferred<android.location.Location?>()
                
                val locationCallback = object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                        val receivedLocation = locationResult.lastLocation
                        android.util.Log.d("LocationViewModel", "Location received in callback: $receivedLocation")
                        try {
                            fusedLocationClient.removeLocationUpdates(this)
                        } catch (e: Exception) {
                            android.util.Log.w("LocationViewModel", "Failed to remove location updates", e)
                        }
                        locationDeferred.complete(receivedLocation)
                    }
                    
                    override fun onLocationAvailability(availability: com.google.android.gms.location.LocationAvailability) {
                        android.util.Log.d("LocationViewModel", "Location availability: ${availability.isLocationAvailable}")
                        if (!availability.isLocationAvailable) {
                            android.util.Log.e("LocationViewModel", "Location not available")
                            try {
                                fusedLocationClient.removeLocationUpdates(this)
                            } catch (e: Exception) {
                                android.util.Log.w("LocationViewModel", "Failed to remove location updates", e)
                            }
                            locationDeferred.complete(null)
                        }
                    }
                }
                
                // 위치 권한 재확인
                if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED) {
                    
                    android.util.Log.d("LocationViewModel", "Starting location updates...")
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, android.os.Looper.getMainLooper())
                    
                    // 30초 타임아웃 설정 (더 긴 시간)
                    val result = kotlinx.coroutines.withTimeoutOrNull(30000) {
                        locationDeferred.await()
                    }
                    
                    if (result == null) {
                        android.util.Log.w("LocationViewModel", "Location request timed out")
                        try {
                            fusedLocationClient.removeLocationUpdates(locationCallback)
                        } catch (e: Exception) {
                            android.util.Log.w("LocationViewModel", "Failed to remove location updates on timeout", e)
                        }
                    }
                    
                    result
                } else {
                    android.util.Log.e("LocationViewModel", "Location permission not granted for real-time request")
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("LocationViewModel", "Exception in getCurrentLocationSuspend", e)
                null
            }
        }
    }
    
    // 연속적인 실시간 위치 추적 시작
    fun startContinuousLocationTracking(context: Context) {
        if (isContinuousTracking) {
            android.util.Log.d("LocationViewModel", "연속 위치 추적이 이미 실행 중입니다")
            return
        }
        
        android.util.Log.d("LocationViewModel", "🔄 연속 위치 추적 시작")
        
        // 시스템 GPS 상태 확인
        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        
        android.util.Log.d("LocationViewModel", "📱 시스템 위치 제공자 상태:")
        android.util.Log.d("LocationViewModel", "  - GPS 제공자: ${if(isGpsEnabled) "활성화" else "비활성화"}")
        android.util.Log.d("LocationViewModel", "  - 네트워크 제공자: ${if(isNetworkEnabled) "활성화" else "비활성화"}")
        
        if (!isGpsEnabled && !isNetworkEnabled) {
            android.util.Log.w("LocationViewModel", "⚠️ 모든 위치 제공자가 비활성화되어 있습니다!")
            android.util.Log.w("LocationViewModel", "해결 방법: 설정 > 위치 서비스에서 GPS를 활성화하세요")
            
            // 위치 서비스가 꺼져있을 때 상태 업데이트
            _locationName.value = "위치 서비스 비활성화"
            _latitude.value = null
            _longitude.value = null
            _isLocationLoading.value = false
            isContinuousTracking = false
            return
        }
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        isContinuousTracking = true
        trackingStartTime = System.currentTimeMillis()
        updateCount = 0
        
        // 필터링 변수 제거 - 모든 위치 수용
        
        // 위치 권한 확인
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && 
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            android.util.Log.e("LocationViewModel", "위치 권한이 없어서 연속 추적을 시작할 수 없습니다")
            isContinuousTracking = false
            return
        }
        
        // 초기 빠른 수렴 + 극한 정밀도 GPS 설정
        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, // 최고 정밀도
            100 // 0.1초 간격으로 극한 빠르게 (초기 수렴 가속)
        )
        .setMinUpdateIntervalMillis(50) // 최소 50ms로 극한 설정 (초기 빠른 수렴)
        .setWaitForAccurateLocation(false) // 초기엔 기다리지 않고 빠르게 시작
        .setMinUpdateDistanceMeters(0.1f) // 10cm 이상 움직일 때만 (극미세 감지)
        .build()
        
        locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val currentTime = System.currentTimeMillis()
                    val trackingElapsed = currentTime - (trackingStartTime ?: currentTime)
                    updateCount++
                    
                    android.util.Log.d("LocationViewModel", "📍 실시간 위치 업데이트 #${updateCount}: ${location.latitude}, ${location.longitude} (정확도: ${location.accuracy}m, 경과: ${trackingElapsed/1000}s)")
                    
                    // 단계적 정확도 기준: 처음엔 관대하게, 점점 엄격하게
                    val accuracyThreshold = when {
                        trackingElapsed < 3000 || updateCount < 10 -> 25.0f // 처음 3초 또는 10회: 25m 허용
                        trackingElapsed < 8000 || updateCount < 20 -> 15.0f // 3-8초 또는 10-20회: 15m 허용
                        trackingElapsed < 15000 || updateCount < 40 -> 8.0f  // 8-15초 또는 20-40회: 8m 허용
                        else -> 5.0f // 15초 후: 5m 이하만 허용 (최고 정밀도)
                    }
                    
                    if (location.accuracy <= accuracyThreshold || (currentTime - (lastUpdateTime ?: 0)) > 10000) {
                        _latitude.value = location.latitude
                        _longitude.value = location.longitude
                        _locationName.value = "실시간 추적 중 (정확도: ${String.format("%.1f", location.accuracy)}m)"
                        lastUpdateTime = currentTime
                        
                        // 낚시 핫스팟 체크
                        checkNearbyFishingHotspots(location.latitude, location.longitude)
                        android.util.Log.d("LocationViewModel", "✅ 위치 업데이트 적용됨 - 정확도: ${location.accuracy}m (기준: ${accuracyThreshold}m)")
                    } else {
                        android.util.Log.d("LocationViewModel", "⚠️ 정확도 ${location.accuracy}m - ${accuracyThreshold}m 이하 대기 중... (${trackingElapsed/1000}초 경과)")
                    }
                }
            }
            
            override fun onLocationAvailability(availability: com.google.android.gms.location.LocationAvailability) {
                android.util.Log.d("LocationViewModel", "📡 위치 가용성 변경: ${availability.isLocationAvailable}")
                
                if (!availability.isLocationAvailable) {
                    android.util.Log.w("LocationViewModel", "❌ GPS 신호 불가 - 가능한 원인:")
                    android.util.Log.w("LocationViewModel", "  - GPS가 꺼져있음")
                    android.util.Log.w("LocationViewModel", "  - 실내에서 GPS 신호 약함")
                    android.util.Log.w("LocationViewModel", "  - 위치 권한 문제")
                    android.util.Log.w("LocationViewModel", "  - 시스템 GPS 서비스 문제")
                } else {
                    android.util.Log.d("LocationViewModel", "✅ GPS 신호 수신 가능")
                }
            }
        }
        
        try {
            fusedLocationClient?.requestLocationUpdates(
                locationRequest, 
                locationCallback!!, 
                android.os.Looper.getMainLooper()
            )
            android.util.Log.d("LocationViewModel", "✅ 연속 위치 추적이 성공적으로 시작되었습니다")
        } catch (e: Exception) {
            android.util.Log.e("LocationViewModel", "연속 위치 추적 시작 실패", e)
            isContinuousTracking = false
        }
    }
    
    // 연속 위치 추적 중지
    fun stopContinuousLocationTracking() {
        if (!isContinuousTracking) return
        
        android.util.Log.d("LocationViewModel", "🛑 연속 위치 추적 중지")
        
        locationCallback?.let { callback ->
            fusedLocationClient?.removeLocationUpdates(callback)
        }
        
        isContinuousTracking = false
        locationCallback = null
        fusedLocationClient = null
    }
    
    override fun onCleared() {
        super.onCleared()
        stopContinuousLocationTracking()
    }
    
    private fun initializeFishingHotspots(context: Context) {
        viewModelScope.launch {
            try {
                fishingHotspotService?.loadFishingSpots()
                isHotspotsLoaded = true
                android.util.Log.d("LocationViewModel", "Fishing hotspots loaded successfully")
            } catch (e: Exception) {
                android.util.Log.e("LocationViewModel", "Failed to load fishing hotspots", e)
            }
        }
    }
    
    private fun checkNearbyFishingHotspots(lat: Double, lon: Double) {
        if (!isHotspotsLoaded || fishingHotspotService == null) return
        
        viewModelScope.launch {
            try {
                val nearbySpots = fishingHotspotService!!.findNearbySpots(lat, lon, 500.0) // 500m 반경
                
                for ((spot, distance) in nearbySpots) {
                    val spotKey = "${spot.latitude}_${spot.longitude}"
                    
                    // 이미 알림을 보낸 스팟은 스킵
                    if (!lastNotifiedSpots.contains(spotKey)) {
                        // 알림 전송
                        val context = fishingHotspotService!!::class.java.getDeclaredField("context")
                            .apply { isAccessible = true }
                            .get(fishingHotspotService) as Context
                        
                        FishingHotspotNotificationService.showFishingSpotNotification(
                            context, spot, distance
                        )
                        
                        // 알림을 보낸 스팟으로 기록
                        lastNotifiedSpots.add(spotKey)
                        
                        android.util.Log.d("LocationViewModel", "Fishing hotspot notification sent for spot at ${spot.latitude}, ${spot.longitude}")
                    }
                }
                
                // 500m 이상 떨어진 스팟들은 알림 기록에서 제거 (재알림 가능하도록)
                val allNearbySpotKeys = nearbySpots.map { "${it.first.latitude}_${it.first.longitude}" }.toSet()
                lastNotifiedSpots.removeAll { spotKey ->
                    !allNearbySpotKeys.contains(spotKey)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("LocationViewModel", "Error checking fishing hotspots", e)
            }
        }
    }
}