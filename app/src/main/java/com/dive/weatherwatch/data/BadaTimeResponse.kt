package com.dive.weatherwatch.data

import com.google.gson.annotations.SerializedName

data class BadaTimeCurrentApiResponse(
    @SerializedName("weather") val weather: List<BadaTimeWeatherData>?,
    @SerializedName("info") val info: BadaTimeLocationInfo?
)

data class BadaTimeWeatherData(
    @SerializedName("aplYmdt") val aplYmdt: String?,
    @SerializedName("ymdt") val ymdt: String?, // forecast uses ymdt instead of aplYmdt
    @SerializedName("sky") val sky: String?,
    @SerializedName("sky_code") val skyCode: String?,
    @SerializedName("skycode") val skycode: String?, // forecast uses skycode instead of sky_code
    @SerializedName("rain") val rain: String?,
    @SerializedName("temp") val temp: String?,
    @SerializedName("winddir") val winddir: String?,
    @SerializedName("windspd") val windspd: String?,
    @SerializedName("pago") val pago: String?,
    @SerializedName("humidity") val humidity: String?,
    @SerializedName("pm25") val pm25: String?,
    @SerializedName("pm10") val pm10: String?,
    @SerializedName("pm25_s") val pm25Status: String?,
    @SerializedName("pm10_s") val pm10Status: String?,
    // Forecast specific fields with Unicode BOM characters (실제 API에서 사용되는 것들)
    @SerializedName("rainAmt") val rainAmt: String?,
    @SerializedName("﻿rainAmt") val rainAmtBom: String?, // BOM이 포함된 rainAmt
    @SerializedName("﻿temp") val tempBom: String?, // BOM이 포함된 temp (가장 중요!)
    @SerializedName("﻿winddir") val winddirBom: String?, // BOM이 포함된 winddir  
    @SerializedName("﻿﻿windspd") val windspdBom: String?, // 2개 BOM이 포함된 windspd
    @SerializedName("﻿﻿humidity") val humidityBom: String?, // 2개 BOM이 포함된 humidity
    @SerializedName("wavePrd") val wavePrd: String?,
    @SerializedName("﻿﻿﻿wavePrd") val wavePrdBom: String?, // 3개 BOM이 포함된 wavePrd
    @SerializedName("waveHt") val waveHt: String?,
    @SerializedName("﻿﻿﻿﻿waveHt") val waveHtBom: String?, // 4개 BOM이 포함된 waveHt
    @SerializedName("waveDir") val waveDir: String?
)

data class BadaTimeLocationInfo(
    @SerializedName("city") val city: String?,
    @SerializedName("cityCode") val cityCode: String?
)

// Backwards compatibility - current weather is the first item in the weather array
typealias BadaTimeCurrentResponse = BadaTimeWeatherData
typealias BadaTimeForecastResponse = BadaTimeWeatherData

// Tide data model
data class BadaTimeTideResponse(
    @SerializedName("pThisDate") val thisDate: String?, // "2025-8-14-목-6-21"
    @SerializedName("pSelArea") val selectedArea: String?, // "남해 해양박물관<br>"
    @SerializedName("pMul") val tideType: String?, // "13 물", "조금", "1 물" 등
    @SerializedName("pSun") val sunRiseSet: String?, // "05:43/19:14" (일출/일몰)
    @SerializedName("pMoon") val moonRiseSet: String?, // "21:55/10:47" (월출/월몰)
    @SerializedName("pTime1") val tideTime1: String?, // "05:07 (20) ▼-105"
    @SerializedName("pTime2") val tideTime2: String?, // "11:34 (118) ▲+98"
    @SerializedName("pTime3") val tideTime3: String?, // "17:21 (29) ▼-89"
    @SerializedName("pTime4") val tideTime4: String? // "23:35 (115) ▲+86" or ""
)