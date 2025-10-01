package com.dive.weatherwatch.ui.viewmodels

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dive.weatherwatch.data.TrapLocation
import com.dive.weatherwatch.data.TrapNavigationInfo
import com.dive.weatherwatch.data.ProximityLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.math.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TrapViewModel(private val context: Context) : ViewModel() {
    private val _traps = MutableStateFlow<List<TrapLocation>>(emptyList())
    val traps: StateFlow<List<TrapLocation>> = _traps.asStateFlow()

    private val _currentLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val currentLocation: StateFlow<Pair<Double, Double>?> = _currentLocation.asStateFlow()

    private val _isDeployingTrap = MutableStateFlow(false)
    val isDeployingTrap: StateFlow<Boolean> = _isDeployingTrap.asStateFlow()

    private val _selectedTrap = MutableStateFlow<TrapLocation?>(null)
    val selectedTrap: StateFlow<TrapLocation?> = _selectedTrap.asStateFlow()

    private val _navigationInfo = MutableStateFlow<TrapNavigationInfo?>(null)
    val navigationInfo: StateFlow<TrapNavigationInfo?> = _navigationInfo.asStateFlow()

    private val _isNavigating = MutableStateFlow(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating.asStateFlow()

    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("trap_locations", Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        loadTraps()
    }

    private fun loadTraps() {
        viewModelScope.launch {
            try {
                val trapsJson = sharedPreferences.getString("traps", "[]")
                val type = object : TypeToken<List<TrapLocation>>() {}.type
                val loadedTraps: List<TrapLocation> = gson.fromJson(trapsJson, type) ?: emptyList()
                _traps.value = loadedTraps
                android.util.Log.i("TrapViewModel", "통발 데이터 로드 완료: ${loadedTraps.size}개")
            } catch (e: Exception) {
                android.util.Log.e("TrapViewModel", "Failed to load traps", e)
                _traps.value = emptyList()
            }
        }
    }

    fun updateCurrentLocation(latitude: Double, longitude: Double) {
        _currentLocation.value = Pair(latitude, longitude)
        
        // 네비게이션 중이면 정보 업데이트
        if (_isNavigating.value && _selectedTrap.value != null) {
            updateNavigationInfo(_selectedTrap.value!!, latitude, longitude)
        }
    }

    fun deployTrap(
        latitude: Double,
        longitude: Double,
        name: String? = null,
        memo: String = "",
        baitType: String = "미설정",
        estimatedDepth: String = "알 수 없음"
    ) {
        viewModelScope.launch {
            _isDeployingTrap.value = true
            try {
                val trap = TrapLocation(
                    name = name ?: "통발 #${(_traps.value.size + 1).toString().padStart(2, '0')}",
                    latitude = latitude,
                    longitude = longitude,
                    deployTime = LocalDateTime.now().toString(),
                    memo = memo,
                    baitType = baitType,
                    estimatedDepth = estimatedDepth
                )

                val updatedTraps = _traps.value + trap
                _traps.value = updatedTraps

                // SharedPreferences에 저장
                saveTrapToDatabase(trap)

                android.util.Log.i("TrapViewModel", "🎣 통발 투하 완료: ${trap.name} at (${trap.latitude}, ${trap.longitude})")
                
            } catch (e: Exception) {
                android.util.Log.e("TrapViewModel", "Failed to deploy trap", e)
            } finally {
                _isDeployingTrap.value = false
            }
        }
    }

    fun deleteTrap(trap: TrapLocation) {
        viewModelScope.launch {
            try {
                _traps.value = _traps.value.filter { it.id != trap.id }
                
                // 선택된 통발이 삭제되면 네비게이션 중지
                if (_selectedTrap.value?.id == trap.id) {
                    stopNavigation()
                }

                // SharedPreferences에서 삭제
                deleteTrapFromDatabase(trap)

                android.util.Log.i("TrapViewModel", "🗑️ 통발 삭제: ${trap.name}")
                
            } catch (e: Exception) {
                android.util.Log.e("TrapViewModel", "Failed to delete trap", e)
            }
        }
    }

    fun startNavigation(trap: TrapLocation) {
        _selectedTrap.value = trap
        _isNavigating.value = true
        
        _currentLocation.value?.let { (lat, lon) ->
            updateNavigationInfo(trap, lat, lon)
        }
        
        android.util.Log.i("TrapViewModel", "🧭 네비게이션 시작: ${trap.name}")
    }

    fun stopNavigation() {
        _selectedTrap.value = null
        _isNavigating.value = false
        _navigationInfo.value = null
        
        android.util.Log.i("TrapViewModel", "⏹️ 네비게이션 중지")
    }

    private fun updateNavigationInfo(trap: TrapLocation, currentLat: Double, currentLon: Double) {
        val distance = calculateDistance(currentLat, currentLon, trap.latitude, trap.longitude)
        val bearing = calculateBearing(currentLat, currentLon, trap.latitude, trap.longitude)
        val proximityLevel = getProximityLevel(distance)

        _navigationInfo.value = TrapNavigationInfo(
            trap = trap,
            currentLatitude = currentLat,
            currentLongitude = currentLon,
            distanceMeters = distance,
            bearingDegrees = bearing,
            proximityLevel = proximityLevel
        )
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3 // 지구 반지름 (미터)
        val φ1 = lat1 * PI / 180
        val φ2 = lat2 * PI / 180
        val Δφ = (lat2 - lat1) * PI / 180
        val Δλ = (lon2 - lon1) * PI / 180

        val a = sin(Δφ / 2) * sin(Δφ / 2) +
                cos(φ1) * cos(φ2) *
                sin(Δλ / 2) * sin(Δλ / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return r * c
    }

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val φ1 = lat1 * PI / 180
        val φ2 = lat2 * PI / 180
        val Δλ = (lon2 - lon1) * PI / 180

        val y = sin(Δλ) * cos(φ2)
        val x = cos(φ1) * sin(φ2) - sin(φ1) * cos(φ2) * cos(Δλ)

        val bearing = atan2(y, x) * 180 / PI
        return (bearing + 360) % 360 // 0-360도로 정규화
    }

    private fun getProximityLevel(distanceMeters: Double): ProximityLevel {
        return when {
            distanceMeters < 10 -> ProximityLevel.AT_TARGET
            distanceMeters < 50 -> ProximityLevel.VERY_CLOSE
            distanceMeters < 100 -> ProximityLevel.CLOSE
            distanceMeters < 500 -> ProximityLevel.FAR
            else -> ProximityLevel.VERY_FAR
        }
    }

    fun getProximityMessage(level: ProximityLevel): String {
        return when (level) {
            ProximityLevel.AT_TARGET -> "🎯 도착! 통발이 바로 여기에 있습니다!"
            ProximityLevel.VERY_CLOSE -> "🔥 매우 가까움! 거의 다 왔어요!"
            ProximityLevel.CLOSE -> "⚡ 가까워지고 있어요!"
            ProximityLevel.FAR -> "🚶 조금 더 가세요"
            ProximityLevel.VERY_FAR -> "🗺️ 아직 멀어요"
        }
    }

    fun getVibrationPattern(level: ProximityLevel): LongArray {
        return when (level) {
            ProximityLevel.AT_TARGET -> longArrayOf(0, 1000, 100, 1000) // 긴 진동
            ProximityLevel.VERY_CLOSE -> longArrayOf(0, 200, 100, 200, 100, 200) // 빠른 연속
            ProximityLevel.CLOSE -> longArrayOf(0, 300, 200, 300) // 중간 진동
            ProximityLevel.FAR -> longArrayOf(0, 500, 400, 500) // 긴 간격
            ProximityLevel.VERY_FAR -> longArrayOf(0, 800) // 단일 진동
        }
    }

    private fun saveTrapToDatabase(trap: TrapLocation) {
        try {
            val currentTraps = _traps.value
            val trapsJson = gson.toJson(currentTraps)
            sharedPreferences.edit()
                .putString("traps", trapsJson)
                .apply()
            android.util.Log.i("TrapViewModel", "통발 데이터 저장 완료")
        } catch (e: Exception) {
            android.util.Log.e("TrapViewModel", "통발 데이터 저장 실패", e)
        }
    }

    private fun deleteTrapFromDatabase(trap: TrapLocation) {
        try {
            val currentTraps = _traps.value
            val trapsJson = gson.toJson(currentTraps)
            sharedPreferences.edit()
                .putString("traps", trapsJson)
                .apply()
            android.util.Log.i("TrapViewModel", "통발 데이터 삭제 완료")
        } catch (e: Exception) {
            android.util.Log.e("TrapViewModel", "통발 데이터 삭제 실패", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopNavigation()
    }
}