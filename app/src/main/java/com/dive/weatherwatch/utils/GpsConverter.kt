package com.dive.weatherwatch.utils

import kotlin.math.*

object GpsConverter {
    private const val DEGRAD = PI / 180.0
    private const val GRID = 5.0
    private const val RE = 6371.00877 / GRID
    private const val SLAT1 = 30.0 * DEGRAD
    private const val SLAT2 = 60.0 * DEGRAD
    private const val OLON = 126.0 * DEGRAD
    private const val OLAT = 38.0 * DEGRAD
    private const val XO = 43.0
    private const val YO = 136.0

    fun toXY(lat: Double, lon: Double): Pair<Int, Int> {
        android.util.Log.d("GpsConverter", "Converting GPS: lat=$lat, lon=$lon")
        
        val latRad = lat * DEGRAD
        val lonRad = lon * DEGRAD
        
        val sn = tan(PI * 0.25 + SLAT2 * 0.5) / tan(PI * 0.25 + SLAT1 * 0.5)
        val sn_val = ln(cos(SLAT1) / cos(SLAT2)) / ln(sn)
        val sf = tan(PI * 0.25 + SLAT1 * 0.5).pow(sn_val) * cos(SLAT1) / sn_val
        val ro = tan(PI * 0.25 + OLAT * 0.5).pow(sn_val) * RE * sf
        
        val ra = tan(PI * 0.25 + latRad * 0.5).pow(sn_val) * RE * sf

        var theta = lonRad - OLON
        if (theta > PI) theta -= 2.0 * PI
        if (theta < -PI) theta += 2.0 * PI
        theta *= sn_val

        val xTemp = ra * sin(theta) + XO + 0.5
        val yTemp = ro - ra * cos(theta) + YO + 0.5
        
        val x = floor(xTemp).toInt()
        val y = floor(yTemp).toInt()
        
        android.util.Log.d("GpsConverter", "Intermediate values: sn=$sn_val, sf=$sf, ro=$ro, ra=$ra, theta=$theta")
        android.util.Log.d("GpsConverter", "Before rounding: x=$xTemp, y=$yTemp")
        
        // 유효한 격자 범위 체크 (대한민국 영역)
        if (x < 1 || x > 149 || y < 1 || y > 253) {
            android.util.Log.w("GpsConverter", "Invalid grid coordinates: x=$x, y=$y (outside Korea bounds)")
        }

        android.util.Log.d("GpsConverter", "Converted to Grid: nx=$x, ny=$y")
        return Pair(x, y)
    }
}