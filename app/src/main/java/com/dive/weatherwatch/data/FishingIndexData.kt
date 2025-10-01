package com.dive.weatherwatch.data

import com.google.gson.annotations.SerializedName

data class FishingIndexResponse(
    @SerializedName("response") val response: FishingIndexResponseBody
)

data class FishingIndexResponseBody(
    @SerializedName("header") val header: FishingIndexHeader,
    @SerializedName("body") val body: FishingIndexBody?
)

data class FishingIndexHeader(
    @SerializedName("resultCode") val resultCode: String,
    @SerializedName("resultMsg") val resultMsg: String
)

data class FishingIndexBody(
    @SerializedName("items") val items: FishingIndexItems,
    @SerializedName("numOfRows") val numOfRows: Int,
    @SerializedName("pageNo") val pageNo: Int,
    @SerializedName("totalCount") val totalCount: Int
)

data class FishingIndexItems(
    @SerializedName("item") val item: List<FishingIndexData>
)

data class FishingIndexData(
    @SerializedName("seafsPstnNm") val locationName: String,     // 위치
    @SerializedName("lat") val latitude: Double,                // 위도
    @SerializedName("lot") val longitude: Double,               // 경도
    @SerializedName("predcYmd") val predictionDate: String,     // 날짜 (YYYYMMDD)
    @SerializedName("predcNoonSeCd") val predictionTime: String, // 시간 (오전/오후)
    @SerializedName("seafsTgfshNm") val targetFish: String,     // 대상어
    @SerializedName("tdlvHrScr") val fishingIndex: Int,         // 낚시 지수 (1-5점) - Int로 변경
    @SerializedName("lastScr") val fishingScore: Int,           // 낚시 점수 - Int로 변경
    @SerializedName("totalIndex") val totalIndex: String? = null, // 전체 지수
    @SerializedName("minWvhgt") val minWaveHeight: Int? = null,  // 최소 파고
    @SerializedName("maxWvhgt") val maxWaveHeight: Int? = null,  // 최대 파고
    @SerializedName("minWtem") val minWaterTemp: Int? = null,    // 최소 수온
    @SerializedName("maxWtem") val maxWaterTemp: Int? = null,    // 최대 수온
    @SerializedName("minArtmp") val minAirTemp: Int? = null,     // 최소 기온
    @SerializedName("maxArtmp") val maxAirTemp: Int? = null,     // 최대 기온
    @SerializedName("minCrsp") val minCloudiness: Int? = null,   // 최소 구름량
    @SerializedName("maxCrsp") val maxCloudiness: Int? = null,   // 최대 구름량
    @SerializedName("minWspd") val minWindSpeed: Int? = null,    // 최소 풍속
    @SerializedName("maxWspd") val maxWindSpeed: Int? = null     // 최대 풍속
)