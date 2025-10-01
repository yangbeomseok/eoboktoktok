package com.dive.weatherwatch.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import com.dive.weatherwatch.ui.theme.AppGradients
import com.dive.weatherwatch.ui.viewmodels.WeatherViewModel
import com.dive.weatherwatch.ui.viewmodels.LocationViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.dive.weatherwatch.ui.components.DynamicBackgroundOverlay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SecondWatchScreen(
    weatherViewModel: WeatherViewModel = viewModel(),
    locationViewModel: LocationViewModel = viewModel()
) {
    val weatherData by weatherViewModel.weatherData.collectAsState()
    val locationName by weatherViewModel.locationName.collectAsState()
    val isLoading by weatherViewModel.isLoading.collectAsState()
    val errorMessage by weatherViewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    val fusedLocationClient: FusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)) {
            Log.d("Location", "Permission granted, fetching location.")
            fetchCurrentLocation(context, fusedLocationClient, weatherViewModel)
        } else {
            Log.d("Location", "Permission denied.")
            weatherViewModel.updateErrorMessage("위치 권한이 거부되었습니다.")
            fallbackToDefaultLocation(weatherViewModel)
        }
    }

    LaunchedEffect(Unit) {
        // Check if location is already available from LocationViewModel
        if (locationViewModel.isLocationAvailable()) {
            Log.d("Location", "Using cached location from LocationViewModel")
            val cachedLocationName = locationViewModel.locationName.value ?: "Unknown"
            val lat = locationViewModel.latitude.value
            val lon = locationViewModel.longitude.value
            
            weatherViewModel.updateLocationName(cachedLocationName)
            
            if (lat != null && lon != null) {
                val (baseDate, baseTime) = com.dive.weatherwatch.ui.screens.getValidBaseDateTime()
                weatherViewModel.fetchWeatherData(
                    serviceKey = BuildConfig.DATA_GO_KR_API_KEY,
                    baseDate = baseDate,
                    baseTime = baseTime,
                    lat = lat,
                    lon = lon,
                    locationName = cachedLocationName
                )
            }
            return@LaunchedEffect
        }
        
        Log.d("Location", "=== GPS DEBUG SESSION START ===")
        Log.d("Location", "Checking location permissions...")
        
        val fineLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        
        Log.d("Location", "Fine location permission: ${if (fineLocationPermission == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"}")
        Log.d("Location", "Coarse location permission: ${if (coarseLocationPermission == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"}")
        
        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED) {
            Log.d("Location", "Requesting location permissions...")
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            Log.d("Location", "Permissions already granted, fetching location...")
            fetchCurrentLocation(context, fusedLocationClient, weatherViewModel)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Dynamic Background Overlay (시간 기반 배경)
        DynamicBackgroundOverlay(
            weatherData = weatherData?.response?.body?.items?.item,
            alpha = 0.7f,
            forceTimeBasedBackground = weatherData?.response?.body?.items?.item.isNullOrEmpty()
        )
        
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                if (!errorMessage.isNullOrEmpty()) {
                    ErrorState(errorMessage = errorMessage!!)
                } else if (locationName.isNullOrEmpty() || (isLoading && weatherData == null)) {
                    LoadingState(locationName = locationName)
                }
            }

            if (!isLoading && errorMessage.isNullOrEmpty() && locationName != null) {
                // Location at top edge of bezel
                item {
                    Text(
                        text = locationName,
                        style = androidx.wear.compose.material.MaterialTheme.typography.body2.copy(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                // Date right below location
                item {
                    Text(
                        text = SimpleDateFormat("M월 d일 (E)", Locale.KOREAN).format(Date()),
                        style = androidx.wear.compose.material.MaterialTheme.typography.body2.copy(
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 9.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                weatherData?.response?.body?.items?.item?.let { items ->
                    if (items.isNotEmpty()) {
                        val temperature = items.firstOrNull { it.category == "TMP" }?.fcstValue ?: "N/A"
                        val sky = items.firstOrNull { it.category == "SKY" }?.fcstValue ?: "1"
                        val windSpeed = items.firstOrNull { it.category == "WSD" }?.fcstValue ?: "N/A"
                        val humidity = items.firstOrNull { it.category == "REH" }?.fcstValue ?: "N/A"
                        val pop = items.firstOrNull { it.category == "POP" }?.fcstValue ?: "N/A"
                        val uvi = items.firstOrNull { it.category == "UVT" }?.fcstValue ?: "N/A" // 자외선
                        
                        // Main weather box
                        item {
                            NewMainWeatherBox(sky = sky, temperature = temperature)
                        }
                        
                        // Weather detail cards in 2x2 grid
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    SecondScreenWeatherCard(icon = "🌬️", label = "풍속", value = "${windSpeed}m/s", modifier = Modifier.weight(1f))
                                    SecondScreenWeatherCard(icon = "🌧️", label = "강수확률", value = "${pop}%", modifier = Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    SecondScreenWeatherCard(icon = "💧", label = "습도", value = "${humidity}%", modifier = Modifier.weight(1f))
                                    SecondScreenWeatherCard(icon = "☀️", label = "자외선", value = if (uvi != "N/A") "${uvi}" else "보통", modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        item { EmptyDataState() }
                    }
                } ?: run {
                    item { EmptyDataState() }
                }
            }
        }
    }
}

@Composable
private fun ErrorState(errorMessage: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        Text(text = "⚠️", style = androidx.wear.compose.material.MaterialTheme.typography.display1.copy(fontSize = 32.sp, color = Color.Red))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            style = androidx.wear.compose.material.MaterialTheme.typography.body1.copy(color = Color.Red.copy(alpha = 0.8f), fontWeight = FontWeight.Medium, fontSize = 11.sp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoadingState(locationName: String?) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_animation")
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 로딩 화면 배경
        DynamicBackgroundOverlay(
            weatherData = null,
            alpha = 0.7f,
            forceTimeBasedBackground = true
        )
        
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        if (locationName.isNullOrEmpty()) {
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "location_alpha"
            )
            Text(
                text = "🛰️",
                style = androidx.wear.compose.material.MaterialTheme.typography.display1.copy(fontSize = 32.sp, color = Color.White),
                modifier = Modifier.alpha(alpha)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "GPS 신호를 수신하고 있습니다...",
                style = androidx.wear.compose.material.MaterialTheme.typography.body1.copy(color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium, fontSize = 11.sp)
            )
        } else {
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "weather_rotation"
            )
            Text(
                text = "🌤️",
                style = androidx.wear.compose.material.MaterialTheme.typography.display1.copy(fontSize = 32.sp, color = Color.White),
                modifier = Modifier.graphicsLayer { rotationZ = rotation }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "위치: $locationName",
                style = androidx.wear.compose.material.MaterialTheme.typography.body2.copy(color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "날씨 정보를 찾는 중...",
                style = androidx.wear.compose.material.MaterialTheme.typography.body1.copy(color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium, fontSize = 11.sp),
                textAlign = TextAlign.Center
            )
        }
        }
    }
}

@Composable
private fun EmptyDataState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        Text(text = "❌", style = androidx.wear.compose.material.MaterialTheme.typography.display1.copy(fontSize = 32.sp, color = Color.Red))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "날씨 정보가 없습니다.",
            style = androidx.wear.compose.material.MaterialTheme.typography.body1.copy(color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium, fontSize = 12.sp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NewMainWeatherBox(sky: String, temperature: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.DarkGray)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Weather icon
            val weatherIcon = when (sky) {
                "1" -> "☀️"
                "3" -> "⛅"
                "4" -> "☁️"
                else -> "🌤️"
            }
            Text(
                text = weatherIcon,
                style = androidx.wear.compose.material.MaterialTheme.typography.display1.copy(fontSize = 32.sp),
                modifier = Modifier.padding(end = 12.dp)
            )
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Temperature
                Text(
                    text = "${temperature}°",
                    style = androidx.wear.compose.material.MaterialTheme.typography.display2.copy(
                        color = Color.White, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 28.sp
                    )
                )
                
                // Weather description
                val weatherDesc = when (sky) {
                    "1" -> "맑음"
                    "3" -> "구름많음"
                    "4" -> "흐림"
                    else -> "보통"
                }
                Text(
                    text = weatherDesc,
                    style = androidx.wear.compose.material.MaterialTheme.typography.body2.copy(
                        color = Color.White.copy(alpha = 0.8f), 
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun SecondScreenWeatherCard(icon: String, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = icon,
            style = androidx.wear.compose.material.MaterialTheme.typography.body1.copy(
                fontSize = 14.sp
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = androidx.wear.compose.material.MaterialTheme.typography.body2.copy(
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 8.sp
            ),
            textAlign = TextAlign.Center
        )
        Text(
            text = value,
            style = androidx.wear.compose.material.MaterialTheme.typography.body1.copy(
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )
    }
}

fun fetchCurrentLocation(context: Context, fusedLocationClient: FusedLocationProviderClient, weatherViewModel: WeatherViewModel) {
    Log.d("Location", "=== FETCH CURRENT LOCATION START ===")
    Log.d("Location", "fetchCurrentLocation called")

    // Check if location services are enabled
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
    val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
    val isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    
    Log.d("Location", "GPS Provider enabled: $isGpsEnabled")
    Log.d("Location", "Network Provider enabled: $isNetworkEnabled")
    
    if (!isGpsEnabled && !isNetworkEnabled) {
        Log.e("Location", "No location providers enabled!")
        weatherViewModel.updateErrorMessage("위치 서비스가 비활성화되어 있습니다.")
        fallbackToDefaultLocation(weatherViewModel)
        return
    }

    var isLocationReceived = false
    val locationRequestTimeout = 20000L // Increased timeout for better debugging

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            if (isLocationReceived) return
            isLocationReceived = true
            fusedLocationClient.removeLocationUpdates(this)

            locationResult.lastLocation?.let { location ->
                val latitude = location.latitude
                val longitude = location.longitude
                Log.d("Location", "Fetched Latitude: $latitude, Longitude: $longitude")
                Log.d("Location", "Location accuracy: ${location.accuracy} meters")

                // Validate location coordinates
                if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                    Log.e("Location", "Invalid GPS coordinates: lat=$latitude, lon=$longitude")
                    weatherViewModel.updateErrorMessage("잘못된 GPS 좌표")
                    fallbackToDefaultLocation(weatherViewModel)
                    return
                }

                try {
                    val geocoder = Geocoder(context, Locale.KOREAN)
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        val address = addresses[0]
                        
                        // Detailed address parsing for Korean locations (including 구/동)
                        Log.d("Location", "Full address details:")
                        Log.d("Location", "  countryName: ${address.countryName}")
                        Log.d("Location", "  adminArea: ${address.adminArea}")
                        Log.d("Location", "  locality: ${address.locality}")
                        Log.d("Location", "  subLocality: ${address.subLocality}")
                        Log.d("Location", "  thoroughfare: ${address.thoroughfare}")
                        Log.d("Location", "  subThoroughfare: ${address.subThoroughfare}")
                        Log.d("Location", "  addressLine[0]: ${address.getAddressLine(0)}")
                        
                        val locationParts = mutableListOf<String>()
                        
                        // Add city (시/도)
                        address.adminArea?.let { area ->
                            val cleanArea = area.replace("특별시", "").replace("광역시", "").replace("시", "")
                            if (cleanArea.isNotEmpty() && cleanArea.length > 1) {
                                locationParts.add(cleanArea)
                            }
                        }
                        
                        // Add district (구/군)
                        address.locality?.let { locality ->
                            if (locality.isNotEmpty() && locality != address.adminArea) {
                                locationParts.add(locality)
                            }
                        }
                        
                        // Add sub-district (동/읍/면)
                        address.subLocality?.let { subLocality ->
                            if (subLocality.isNotEmpty() && !locationParts.contains(subLocality)) {
                                locationParts.add(subLocality)
                            }
                        }
                        
                        // If no detailed parts found, try parsing from full address line
                        if (locationParts.isEmpty()) {
                            address.getAddressLine(0)?.let { fullAddress ->
                                val addressTokens = fullAddress.split(" ").filter { 
                                    it.isNotEmpty() && it != "대한민국" && it != "South Korea" 
                                }
                                if (addressTokens.size >= 2) {
                                    locationParts.addAll(addressTokens.take(3)) // Take first 3 meaningful parts
                                }
                            }
                        }

                        // Format address as "시 구 상세위치" removing "대한민국" and cleaning up
                        val fullAddress = address.getAddressLine(0) ?: ""
                        val finalLocationName = fullAddress
                            .replace("대한민국 ", "")
                            .replace("특별시", "시")
                            .replace("광역시", "시")
                            .split(" ")
                            .filter { it.isNotEmpty() && it.length > 1 }
                            .let { parts ->
                                when {
                                    parts.size >= 3 -> "${parts[0]} ${parts[1]} ${parts.drop(2).joinToString(" ")}"
                                    parts.size == 2 -> "${parts[0]} ${parts[1]}"
                                    parts.size == 1 -> parts[0]
                                    else -> "현재 위치"
                                }
                            }

                        weatherViewModel.updateLocationName(finalLocationName)
                        Log.d("Location", "Final Location Name: $finalLocationName")
                        Log.d("Location", "Address components: ${address.getAddressLine(0)}")

                        val (baseDate, baseTime) = com.dive.weatherwatch.ui.screens.getValidBaseDateTime()
                        weatherViewModel.fetchWeatherData(
                            serviceKey = BuildConfig.DATA_GO_KR_API_KEY,
                            baseDate = baseDate,
                            baseTime = baseTime,
                            lat = latitude,
                            lon = longitude,
                            locationName = finalLocationName
                        )
                    } else {
                        Log.w("Location", "Geocoder returned empty results")
                        // Use detailed coordinates if geocoding fails - helpful for debugging GPS accuracy
                        val coordinateLocationName = "GPS 좌표\n${String.format("%.6f", latitude)}\n${String.format("%.6f", longitude)}"
                        weatherViewModel.updateLocationName(coordinateLocationName)
                        Log.d("Location", "Using coordinate fallback: $coordinateLocationName")
                        
                        val (baseDate, baseTime) = com.dive.weatherwatch.ui.screens.getValidBaseDateTime()
                        weatherViewModel.fetchWeatherData(
                            serviceKey = BuildConfig.DATA_GO_KR_API_KEY,
                            baseDate = baseDate,
                            baseTime = baseTime,
                            lat = latitude,
                            lon = longitude,
                            locationName = coordinateLocationName
                        )
                    }
                } catch (e: Exception) {
                    Log.e("Location", "Geocoder failed: ${e.message}", e)
                    // Continue with detailed coordinates if geocoder fails - shows exact GPS data
                    val coordinateLocationName = "Geocoder 실패\nGPS: ${String.format("%.6f", latitude)}, ${String.format("%.6f", longitude)}\n정확도: ${location.accuracy}m"
                    weatherViewModel.updateLocationName(coordinateLocationName)
                    Log.d("Location", "Using geocoder error fallback: $coordinateLocationName")
                    
                    val (baseDate, baseTime) = com.dive.weatherwatch.ui.screens.getValidBaseDateTime()
                    weatherViewModel.fetchWeatherData(
                        serviceKey = BuildConfig.DATA_GO_KR_API_KEY,
                        baseDate = baseDate,
                        baseTime = baseTime,
                        lat = latitude,
                        lon = longitude,
                        locationName = coordinateLocationName
                    )
                }
            } ?: run {
                Log.e("Location", "Last location is null.")
                weatherViewModel.updateErrorMessage("GPS 신호 수신 실패")
                fallbackToDefaultLocation(weatherViewModel)
            }
        }
    }

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        Log.d("Location", "Location permission confirmed, proceeding with location request")
        
        // First try to get last known location for faster response
        Log.d("Location", "Attempting to get last known location...")
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                Log.d("Location", "Last location request success callback triggered")
                if (location != null && !isLocationReceived) {
                    val locationAge = (System.currentTimeMillis() - location.time) / 1000
                    Log.d("Location", "Last known location found:")
                    Log.d("Location", "  Coordinates: ${location.latitude}, ${location.longitude}")
                    Log.d("Location", "  Age: $locationAge seconds")
                    Log.d("Location", "  Accuracy: ${location.accuracy} meters")
                    Log.d("Location", "  Provider: ${location.provider}")
                    
                    // Use last known location if it's recent (within 10 minutes for better debugging)
                    if (locationAge < 600) {
                        Log.d("Location", "Using last known location (recent enough)")
                        locationCallback.onLocationResult(LocationResult.create(listOf(location)))
                        return@addOnSuccessListener
                    } else {
                        Log.d("Location", "Last known location too old ($locationAge seconds), requesting fresh location")
                    }
                } else {
                    Log.d("Location", "Last known location is null or already received location")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Location", "Failed to get last known location: ${e.message}", e)
            }

        // Request fresh location updates with multiple priorities for better debugging
        Log.d("Location", "Setting up fresh location request...")
        
        // Try multiple location requests with different priorities
        val highAccuracyRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
            fastestInterval = 500
            numUpdates = 1
        }
        
        val balancedRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            interval = 2000
            fastestInterval = 1000
            numUpdates = 1
        }
        
        // Start with high accuracy, fallback to balanced if needed
        val locationRequest = if (isNetworkEnabled) balancedRequest else highAccuracyRequest
        
        Log.d("Location", "Location request configuration:")
        Log.d("Location", "  Priority: ${if (isNetworkEnabled) "BALANCED_POWER_ACCURACY" else "HIGH_ACCURACY"}")
        Log.d("Location", "  Interval: ${locationRequest.interval}ms")
        Log.d("Location", "  Fastest interval: ${locationRequest.fastestInterval}ms")
        Log.d("Location", "  Max updates: ${locationRequest.numUpdates}")
        Log.d("Location", "  Using network location: $isNetworkEnabled")
        
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            .addOnSuccessListener {
                Log.d("Location", "Location updates request successful")
            }
            .addOnFailureListener { e ->
                Log.e("Location", "Failed to request location updates: ${e.message}", e)
                weatherViewModel.updateErrorMessage("위치 요청 실패: ${e.message}")
                fallbackToDefaultLocation(weatherViewModel)
            }

        // Set timeout handler
        Log.d("Location", "Setting timeout handler for ${locationRequestTimeout}ms")
        val handler = android.os.Handler(Looper.getMainLooper())
        handler.postDelayed({
            if (!isLocationReceived) {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                Log.e("Location", "=== LOCATION REQUEST TIMEOUT ===")
                Log.e("Location", "Location request timed out after $locationRequestTimeout ms")
                Log.e("Location", "Possible reasons:")
                Log.e("Location", "1. GPS signal is weak (try going outdoors)")
                Log.e("Location", "2. Location services disabled")
                Log.e("Location", "3. App location permission issues")
                Log.e("Location", "4. Device GPS hardware issues")
                weatherViewModel.updateErrorMessage("GPS 신호 수신 시간 초과\n야외에서 다시 시도해 주세요")
                fallbackToDefaultLocation(weatherViewModel)
            }
        }, locationRequestTimeout)
    } else {
        Log.e("Location", "=== PERMISSION ERROR ===")
        Log.e("Location", "Location permission not granted when trying to fetch location")
        weatherViewModel.updateErrorMessage("위치 권한이 필요합니다")
        fallbackToDefaultLocation(weatherViewModel)
    }
}

private fun fallbackToDefaultLocation(weatherViewModel: WeatherViewModel) {
    Log.d("Location", "=== USING FALLBACK LOCATION ===")
    Log.d("Location", "GPS failed, using default Busan location")
    Log.d("Location", "Default coordinates: 35.1796, 129.0756 (Busan)")
    weatherViewModel.updateLocationName("부산시 (기본 위치)")
    val (baseDate, baseTime) = com.dive.weatherwatch.ui.screens.getValidBaseDateTime()
    weatherViewModel.fetchWeatherData(
        serviceKey = BuildConfig.DATA_GO_KR_API_KEY,
        baseDate = baseDate,
        baseTime = baseTime,
        lat = 35.1796,
        lon = 129.0756,
        locationName = "Busan-si"
    )
    Log.d("Location", "=== FALLBACK LOCATION REQUEST SENT ===")
}

