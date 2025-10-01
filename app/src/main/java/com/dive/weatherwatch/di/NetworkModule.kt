package com.dive.weatherwatch.di

import com.dive.weatherwatch.data.WeatherService
import com.dive.weatherwatch.data.BadaTimeService
import com.dive.weatherwatch.data.FishingIndexService
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val WEATHER_BASE_URL = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/"
    private const val BADATIME_BASE_URL = "https://www.badatime.com/DIVE/"
    private const val FISHING_INDEX_BASE_URL = "https://apis.data.go.kr/1192136/"
    private const val FISHING_INDEX_ALT_BASE_URL = "https://apis.data.go.kr/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val customInterceptor = okhttp3.Interceptor { chain ->
        val request = chain.request()
        android.util.Log.d("NetworkModule", "Request URL: ${request.url}")
        
        val response = chain.proceed(request)
        val responseBody = response.body?.string() ?: ""
        android.util.Log.d("NetworkModule", "Raw Response: $responseBody")
        android.util.Log.d("NetworkModule", "Response Code: ${response.code}")
        android.util.Log.d("NetworkModule", "Response Headers: ${response.headers}")
        
        // JSON 형식이 아닌 응답 체크
        if (!responseBody.trim().startsWith("{") && !responseBody.trim().startsWith("[")) {
            android.util.Log.w("NetworkModule", "Response is not valid JSON format. First 200 chars: ${responseBody.take(200)}")
        }
        
        // 응답 본문을 다시 생성해서 반환
        val newResponse = response.newBuilder()
            .body(responseBody.toResponseBody(response.body?.contentType()))
            .build()
        
        newResponse
    }

    // 기존 기상청 API용 클라이언트
    private val client = OkHttpClient.Builder()
        .addInterceptor(customInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    // BadaTime API용 커스텀 인터셉터
    private val badaTimeInterceptor = okhttp3.Interceptor { chain ->
        val request = chain.request()
        android.util.Log.d("BadaTimeAPI", "Request URL: ${request.url}")
        android.util.Log.d("BadaTimeAPI", "Request Headers: ${request.headers}")
        
        val response = chain.proceed(request)
        val responseBody = response.body?.string() ?: ""
        android.util.Log.d("BadaTimeAPI", "Response Code: ${response.code}")
        android.util.Log.d("BadaTimeAPI", "Response Body: $responseBody")
        
        // 응답 본문을 다시 생성해서 반환
        val newResponse = response.newBuilder()
            .body(responseBody.toResponseBody(response.body?.contentType()))
            .build()
        
        newResponse
    }

    // FishingIndex API용 커스텀 인터셉터 (상세 로깅)
    private val fishingIndexInterceptor = okhttp3.Interceptor { chain ->
        val request = chain.request()
        android.util.Log.d("FishingIndexAPI", "========================================")
        android.util.Log.d("FishingIndexAPI", "🎣 FISHING INDEX API CALL START")
        android.util.Log.d("FishingIndexAPI", "Method: ${request.method}")
        android.util.Log.d("FishingIndexAPI", "Full URL: ${request.url}")
        android.util.Log.d("FishingIndexAPI", "Host: ${request.url.host}")
        android.util.Log.d("FishingIndexAPI", "Path: ${request.url.encodedPath}")
        android.util.Log.d("FishingIndexAPI", "Query: ${request.url.encodedQuery}")
        android.util.Log.d("FishingIndexAPI", "Request Headers:")
        request.headers.forEach { header ->
            android.util.Log.d("FishingIndexAPI", "  ${header.first}: ${header.second}")
        }
        android.util.Log.d("FishingIndexAPI", "----------------------------------------")
        
        val startTime = System.currentTimeMillis()
        val response = chain.proceed(request)
        val endTime = System.currentTimeMillis()
        
        val responseBody = response.body?.string() ?: ""
        android.util.Log.d("FishingIndexAPI", "Response Time: ${endTime - startTime}ms")
        android.util.Log.d("FishingIndexAPI", "Response Code: ${response.code}")
        android.util.Log.d("FishingIndexAPI", "Response Message: ${response.message}")
        android.util.Log.d("FishingIndexAPI", "Response Headers:")
        response.headers.forEach { header ->
            android.util.Log.d("FishingIndexAPI", "  ${header.first}: ${header.second}")
        }
        android.util.Log.d("FishingIndexAPI", "Response Body Length: ${responseBody.length}")
        
        // HTTP 500 오류 시 전체 응답 본문 로그
        if (response.code == 500) {
            android.util.Log.e("FishingIndexAPI", "🚨 HTTP 500 ERROR - Full Response Body:")
            android.util.Log.e("FishingIndexAPI", responseBody)
        } else {
            android.util.Log.d("FishingIndexAPI", "Response Body (first 1000 chars):")
            android.util.Log.d("FishingIndexAPI", responseBody.take(1000))
            if (responseBody.length > 1000) {
                android.util.Log.d("FishingIndexAPI", "... (truncated)")
            }
        }
        android.util.Log.d("FishingIndexAPI", "🎣 FISHING INDEX API CALL END")
        android.util.Log.d("FishingIndexAPI", "========================================")
        
        // 응답 본문을 다시 생성해서 반환
        val newResponse = response.newBuilder()
            .body(responseBody.toResponseBody(response.body?.contentType()))
            .build()
        
        newResponse
    }
    
    // BadaTime API용 클라이언트 (더 긴 타임아웃)
    private val badaTimeClient = OkHttpClient.Builder()
        .addInterceptor(badaTimeInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val weatherRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(WEATHER_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val badaTimeRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BADATIME_BASE_URL)
        .client(badaTimeClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // FishingIndex API용 클라이언트 (상세 로깅 포함)
    private val fishingIndexClient = OkHttpClient.Builder()
        .addInterceptor(fishingIndexInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val fishingIndexRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(FISHING_INDEX_BASE_URL)
        .client(fishingIndexClient) // 전용 클라이언트 사용
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val fishingIndexAltRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(FISHING_INDEX_ALT_BASE_URL)
        .client(fishingIndexClient) // 전용 클라이언트 사용
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val weatherService: WeatherService = weatherRetrofit.create(WeatherService::class.java)
    val badaTimeService: BadaTimeService = badaTimeRetrofit.create(BadaTimeService::class.java)
    val fishingIndexService: FishingIndexService = fishingIndexRetrofit.create(FishingIndexService::class.java)
    val fishingIndexAltService: FishingIndexService = fishingIndexAltRetrofit.create(FishingIndexService::class.java)
}