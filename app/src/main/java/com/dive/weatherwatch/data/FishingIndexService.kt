package com.dive.weatherwatch.data

import retrofit2.http.GET
import retrofit2.http.Query

interface FishingIndexService {
    @GET("fcstFishing")  // 기본 엔드포인트
    suspend fun getFishingIndex(
        @Query("serviceKey") serviceKey: String,
        @Query("numOfRows") numOfRows: Int = 10,
        @Query("pageNo") pageNo: Int = 1,
        @Query("dataType") dataType: String = "JSON",
        @Query("type") type: String? = null
    ): FishingIndexResponse
    
    // 대체 엔드포인트들
    @GET("fcstFishing/getFishingIndex")
    suspend fun getFishingIndexAlt1(
        @Query("serviceKey") serviceKey: String,
        @Query("numOfRows") numOfRows: Int = 10,
        @Query("pageNo") pageNo: Int = 1,
        @Query("dataType") dataType: String = "JSON"
    ): FishingIndexResponse
    
    @GET("getFishingIndex")
    suspend fun getFishingIndexAlt2(
        @Query("serviceKey") serviceKey: String,
        @Query("numOfRows") numOfRows: Int = 10,
        @Query("pageNo") pageNo: Int = 1,
        @Query("dataType") dataType: String = "JSON"
    ): FishingIndexResponse
}