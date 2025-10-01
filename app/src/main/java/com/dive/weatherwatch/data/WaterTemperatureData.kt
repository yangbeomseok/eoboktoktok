package com.dive.weatherwatch.data

import com.google.gson.annotations.SerializedName

data class WaterTemperatureData(
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lon") val lon: Double?,
    @SerializedName("obs_name") val obsName: String?,
    @SerializedName("obs_wt") val obsWt: String?, // 수온 데이터
    @SerializedName("obs_time") val obsTime: String?,
    @SerializedName("obs_dt") val obsDt: String? // 거리 정보 (예: "3 km")
)

// 가장 가까운 수온 관측소를 찾기 위한 유틸리티 함수
fun List<WaterTemperatureData>.findClosest(): WaterTemperatureData? {
    return this.minByOrNull { data ->
        // obs_dt에서 숫자만 추출하여 거리로 변환 (예: "3 km" -> 3)
        data.obsDt?.replace("[^0-9.]".toRegex(), "")?.toDoubleOrNull() ?: Double.MAX_VALUE
    }
}