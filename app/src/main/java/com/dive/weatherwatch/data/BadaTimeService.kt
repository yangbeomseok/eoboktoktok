package com.dive.weatherwatch.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface BadaTimeService {
    @GET("current")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("key") apiKey: String = "YOUR_BADA_TIME_API_KEY_HERE"
    ): Response<BadaTimeCurrentApiResponse>
    
    @GET("forecast")
    suspend fun getForecastWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("key") apiKey: String = "YOUR_BADA_TIME_API_KEY_HERE"
    ): Response<List<BadaTimeWeatherData>>
    
    @GET("tide")
    suspend fun getTideData(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("key") apiKey: String = "YOUR_BADA_TIME_API_KEY_HERE"
    ): Response<List<BadaTimeTideResponse>>
    
    @GET("point")
    suspend fun getFishingPoints(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("key") apiKey: String = "YOUR_BADA_TIME_API_KEY_HERE"
    ): Response<BadaTimeFishingPointResponse>
    
    @GET("temp")
    suspend fun getWaterTemperature(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("key") apiKey: String = "YOUR_BADA_TIME_API_KEY_HERE"
    ): Response<List<WaterTemperatureData>>
}