package com.dive.weatherwatch.services

import android.content.Context
import com.dive.weatherwatch.data.FishingHotspot
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.*

class FishingHotspotService(private val context: Context) {
    private val gson = Gson()
    private var fishingSpots: List<FishingHotspot> = emptyList()
    
    suspend fun loadFishingSpots(): List<FishingHotspot> {
        return withContext(Dispatchers.IO) {
            try {
                // assets에서 JSON 파일 읽기
                val inputStream = context.assets.open("busan_fishing_hotspots_final.json")
                val json = inputStream.bufferedReader().use { it.readText() }
                val type = object : TypeToken<List<FishingHotspot>>() {}.type
                fishingSpots = gson.fromJson(json, type)
                fishingSpots
            } catch (e: Exception) {
                // assets에서 실패한 경우 루트 디렉토리에서 시도
                try {
                    val file = File(context.filesDir.parent + "/../../../busan_fishing_hotspots_final.json")
                    if (file.exists()) {
                        val json = file.readText()
                        val type = object : TypeToken<List<FishingHotspot>>() {}.type
                        fishingSpots = gson.fromJson(json, type)
                        fishingSpots
                    } else {
                        emptyList()
                    }
                } catch (e2: Exception) {
                    e2.printStackTrace()
                    emptyList()
                }
            }
        }
    }
    
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371 // 지구 반지름 (km)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c * 1000 // 미터로 변환
    }
    
    fun findNearbySpots(currentLat: Double, currentLon: Double, radiusMeters: Double = 500.0): List<Pair<FishingHotspot, Double>> {
        return fishingSpots.mapNotNull { spot ->
            val distance = calculateDistance(currentLat, currentLon, spot.latitude, spot.longitude)
            if (distance <= radiusMeters) {
                Pair(spot, distance)
            } else {
                null
            }
        }.sortedBy { it.second }
    }
}