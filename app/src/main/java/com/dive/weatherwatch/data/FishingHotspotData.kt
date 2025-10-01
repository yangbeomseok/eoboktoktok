package com.dive.weatherwatch.data

import com.google.gson.annotations.SerializedName

data class FishingHotspot(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("median_concentration")
    val medianConcentration: Double,
    @SerializedName("grade")
    val grade: String
)