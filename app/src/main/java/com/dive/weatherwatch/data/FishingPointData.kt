package com.dive.weatherwatch.data

import com.google.gson.annotations.SerializedName

// BadaTime API 응답용 낚시 포인트 데이터 모델
data class BadaTimeFishingPoint(
    @SerializedName("name") val name: String?,
    @SerializedName("point_nm") val pointNm: String?,
    @SerializedName("dpwt") val dpwt: String?,
    @SerializedName("material") val material: String?,
    @SerializedName("tide_time") val tideTime: String?,
    @SerializedName("target") val target: String?,
    @SerializedName("lat") val lat: String?,
    @SerializedName("lon") val lon: String?,
    @SerializedName("photo") val photo: String?,
    @SerializedName("addr") val addr: String?,
    @SerializedName("seaside") val seaside: String?,
    @SerializedName("point_dt") val pointDt: String?
)

// API 전체 응답 구조
data class BadaTimeFishingPointResponse(
    @SerializedName("fishing_point") val fishingPoint: List<BadaTimeFishingPoint>?,
    @SerializedName("info") val info: BadaTimeFishingAreaInfo?
)

// 지역 정보
data class BadaTimeFishingAreaInfo(
    @SerializedName("intro") val intro: String?,
    @SerializedName("\uFEFFintro") val introBom: String?, // BOM 문자 처리
    @SerializedName("forecast") val forecast: String?,
    @SerializedName("\uFEFFforecast") val forecastBom: String?, // BOM 문자 처리
    @SerializedName("ebbf") val ebbf: String?,
    @SerializedName("\uFEFFebbf") val ebbfBom: String?, // BOM 문자 처리
    @SerializedName("notice") val notice: String?,
    @SerializedName("\uFEFFnotice") val noticeBom: String?, // BOM 문자 처리
    @SerializedName("wtemp_sp") val wtempSp: String?,
    @SerializedName("\uFEFFwtemp_sp") val wtempSpBom: String?, // BOM 문자 처리
    @SerializedName("fish_sp") val fishSp: String?,
    @SerializedName("\uFEFFfish_sp") val fishSpBom: String?, // BOM 문자 처리
    @SerializedName("wtemp_su") val wtempSu: String?,
    @SerializedName("\uFEFFwtemp_su") val wtempSuBom: String?, // BOM 문자 처리
    @SerializedName("fish_su") val fishSu: String?,
    @SerializedName("\uFEFF\uFEFFfish_su") val fishSuBom: String?, // 이중 BOM 처리
    @SerializedName("wtemp_fa") val wtempFa: String?,
    @SerializedName("\uFEFFwtemp_fa") val wtempFaBom: String?, // BOM 문자 처리
    @SerializedName("fish_fa") val fishFa: String?,
    @SerializedName("\uFEFF\uFEFFfish_fa") val fishFaBom: String?, // 이중 BOM 처리
    @SerializedName("wtemp_wi") val wtempWi: String?,
    @SerializedName("\uFEFFwtemp_wi") val wtempWiBom: String?, // BOM 문자 처리
    @SerializedName("\uFEFF\uFEFF\uFEFFfish_wi") val fishWiBom: String? // 삼중 BOM 처리
)

// 기존 UI에서 사용하는 FishingPoint 데이터 클래스 (호환성 유지)
data class FishingPoint(
    val name: String,
    val pointNm: String,
    val dpwt: String,
    val material: String,
    val tideTime: String,
    val target: String,
    val lat: Double,
    val lon: Double,
    val photo: String,
    val addr: String,
    val seaside: String,
    val intro: String,
    val forecast: String,
    val ebbf: String,
    val notice: String,
    val wtempSp: String,
    val fishSp: String,
    val wtempSu: String,
    val fishSu: String,
    val wtempFa: String,
    val fishFa: String,
    val wtempWi: String,
    val fishWi: String,
    val pointDt: String? = null
)

// BadaTimeFishingPoint를 FishingPoint로 변환하는 확장 함수
fun BadaTimeFishingPoint.toFishingPoint(areaInfo: BadaTimeFishingAreaInfo?): FishingPoint {
    return FishingPoint(
        name = this.name ?: "",
        pointNm = this.pointNm ?: "",
        dpwt = this.dpwt ?: "",
        material = this.material ?: "",
        tideTime = this.tideTime ?: "",
        target = this.target ?: "",
        lat = this.lat?.toDoubleOrNull() ?: 0.0,
        lon = this.lon?.toDoubleOrNull() ?: 0.0,
        photo = this.photo ?: "",
        addr = this.addr ?: "",
        seaside = this.seaside ?: "",
        intro = areaInfo?.introBom ?: areaInfo?.intro ?: "",
        forecast = areaInfo?.forecastBom ?: areaInfo?.forecast ?: "",
        ebbf = areaInfo?.ebbfBom ?: areaInfo?.ebbf ?: "",
        notice = areaInfo?.noticeBom ?: areaInfo?.notice ?: "",
        wtempSp = areaInfo?.wtempSp ?: areaInfo?.wtempSpBom ?: "",
        fishSp = areaInfo?.fishSp ?: areaInfo?.fishSpBom ?: "",
        wtempSu = areaInfo?.wtempSu ?: areaInfo?.wtempSuBom ?: "",
        fishSu = areaInfo?.fishSu ?: areaInfo?.fishSuBom ?: "",
        wtempFa = areaInfo?.wtempFa ?: areaInfo?.wtempFaBom ?: "",
        fishFa = areaInfo?.fishFa ?: areaInfo?.fishFaBom ?: "",
        wtempWi = areaInfo?.wtempWi ?: areaInfo?.wtempWiBom ?: "",
        fishWi = areaInfo?.fishWiBom ?: "",
        pointDt = this.pointDt
    )
}

data class SeasonalInfo(
    val season: String,
    val temperature: String,
    val fish: String
)

data class FishingPointResponse(
    val points: List<FishingPoint>
)

// BOM 문자 제거를 위한 확장 함수
fun String?.removeBOM(): String? {
    return this?.replace("\uFEFF", "")?.trim()?.takeIf { it.isNotBlank() }
}