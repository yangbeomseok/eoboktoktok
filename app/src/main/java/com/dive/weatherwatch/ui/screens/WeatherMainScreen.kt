package com.dive.weatherwatch.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.ui.semantics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.PI
import kotlin.random.Random
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.pow
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.dive.weatherwatch.ui.navigation.WatchDestinations
import com.dive.weatherwatch.ui.theme.AppColors
import com.dive.weatherwatch.ui.theme.AppGradients
import com.dive.weatherwatch.ui.viewmodels.LocationViewModel
import com.dive.weatherwatch.ui.viewmodels.WeatherViewModel
import com.dive.weatherwatch.ui.viewmodels.BadaTimeViewModel
import com.dive.weatherwatch.data.TideData
import com.dive.weatherwatch.data.BadaTimeTideResponse
import com.dive.weatherwatch.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar
import java.util.Locale
import java.io.File
import com.dive.weatherwatch.ui.components.DynamicBackgroundOverlay

data class FeatureItem(
    val icon: ImageVector,
    val name: String,
    val description: String,
    val destination: String
)

// 실제 날씨별 배경화면 리소스 선택 함수
private fun getDynamicBackgroundResource(sky: String?, items: List<com.dive.weatherwatch.data.Item>): Int {
    val calendar = Calendar.getInstance()
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(Calendar.MINUTE)
    val isNight = currentHour < 6 || currentHour >= 19
    val isRainy = hasRain(items)
    
    return when {
        // 🌧️ 비오는 날
        isRainy && isNight -> R.mipmap.rainy_night
        isRainy && !isNight -> R.mipmap.rainy_morning_day
        
        // ☁️ 흐림 (SKY: 4)
        sky == "4" && isNight -> R.mipmap.cloudy_night
        sky == "4" && !isNight -> R.mipmap.cloudy_morning_day
        
        // ⛅ 구름많음 (SKY: 3) - 흐림과 동일한 배경 사용
        sky == "3" && isNight -> R.mipmap.cloudy_night
        sky == "3" && !isNight -> R.mipmap.cloudy_morning_day
        
        // ☀️ 맑음 (SKY: 1) 및 기본값
        sky == "1" && isNight -> R.mipmap.clear_night
        sky == "1" && !isNight -> R.mipmap.clear_morning_day
        
        // 기본값 (맑음)
        isNight -> R.mipmap.clear_night
        else -> R.mipmap.clear_morning_day
    }
}

private fun hasRain(items: List<com.dive.weatherwatch.data.Item>): Boolean {
    val pop = items.firstOrNull { it.category == "POP" }?.fcstValue?.toIntOrNull() ?: 0
    val pty = items.firstOrNull { it.category == "PTY" }?.fcstValue?.toIntOrNull() ?: 0
    return pop > 30 || pty > 0 // 강수확률 30% 이상이거나 강수형태가 있으면 비
}

// 날씨와 시간대에 따른 동적 배경 색상
private fun getDynamicBackgroundColor(sky: String?, items: List<com.dive.weatherwatch.data.Item>): List<Color> {
    val calendar = Calendar.getInstance()
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(Calendar.MINUTE)
    val isNight = currentHour < 6 || currentHour >= 19
    val isRainy = hasRain(items)
    
    return when {
        isRainy && isNight -> listOf(
            Color(0xFF1A1A2E), // 어두운 보라
            Color(0xFF16213E), // 어두운 파랑
            Color(0xFF0F3460)  // 짙은 파랑
        )
        isRainy && !isNight -> listOf(
            Color(0xFF4A5568), // 회색
            Color(0xFF2D3748), // 어두운 회색
            Color(0xFF1A202C)  // 진한 회색
        )
        sky == "4" && isNight -> listOf( // 흐림 + 밤
            Color(0xFF2D3436), // 어두운 회색
            Color(0xFF636E72), // 중간 회색
            Color(0xFF74B9FF).copy(alpha = 0.3f) // 은은한 파랑
        )
        sky == "4" && !isNight -> listOf( // 흐림 + 낮
            Color(0xFF74B9FF).copy(alpha = 0.4f), // 흐린 하늘색
            Color(0xFFDDD6FE).copy(alpha = 0.3f), // 연한 보라
            Color(0xFFF8F9FA).copy(alpha = 0.2f)  // 연한 회색
        )
        sky == "3" && isNight -> listOf( // 구름많음 + 밤
            Color(0xFF6C5CE7).copy(alpha = 0.3f), // 은은한 보라
            Color(0xFF74B9FF).copy(alpha = 0.2f), // 은은한 파랑
            Color(0xFF0984E3).copy(alpha = 0.1f)  // 진한 파랑
        )
        sky == "3" && !isNight -> listOf( // 구름많음 + 낮
            Color(0xFF74B9FF).copy(alpha = 0.5f), // 구름 하늘색
            Color(0xFFFFFFFF).copy(alpha = 0.3f), // 흰 구름
            Color(0xFF00CEC9).copy(alpha = 0.2f)  // 청록색
        )
        sky == "1" && isNight -> listOf( // 맑음 + 밤
            Color(0xFF2D3436).copy(alpha = 0.3f), // 어두운 밤하늘
            Color(0xFF6C5CE7).copy(alpha = 0.2f), // 은은한 보라
            Color(0xFFFFD700).copy(alpha = 0.1f)  // 별빛 금색
        )
        else -> listOf( // 맑음 + 낮 (기본값)
            Color(0xFF74B9FF).copy(alpha = 0.6f), // 맑은 하늘색
            Color(0xFFFFFFFF).copy(alpha = 0.4f), // 흰색
            Color(0xFF00CEC9).copy(alpha = 0.3f)  // 청록색
        )
    }
}

@Composable
fun WeatherMainScreen(
    onNavigateToHeartRate: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToTide: () -> Unit = {},
    onNavigateToFishingPoint: () -> Unit = {},
    onNavigateToCompass: () -> Unit = {},
    onNavigateToTrapLocation: () -> Unit = {},
    weatherViewModel: WeatherViewModel = viewModel(),
    locationViewModel: LocationViewModel = viewModel(),
    badaTimeViewModel: BadaTimeViewModel = viewModel()
) {
    val context = LocalContext.current
    val features = remember {
        listOf(
            FeatureItem(Icons.Default.WbSunny, "날씨", "현재 위치 날씨 정보", WatchDestinations.WEATHER),
            FeatureItem(Icons.Default.Waves, "조위 정보", "실시간 조위 및 물때 정보", WatchDestinations.TIDE),
            FeatureItem(Icons.Default.Place, "낚시 포인트", "주변 낚시 포인트 정보", WatchDestinations.FISHING_POINT),
            FeatureItem(Icons.Default.Anchor, "통발 추적", "투하한 통발 위치 추적", WatchDestinations.TRAP_LOCATION),
            FeatureItem(Icons.Default.Chat, "챗봇", "음성으로 AI와 대화", WatchDestinations.CHAT),
            FeatureItem(Icons.Default.Favorite, "심박수", "실시간 심박수 측정", WatchDestinations.HEART_RATE),
            FeatureItem(Icons.Default.Navigation, "나침반", "실시간 방향 및 각도", WatchDestinations.COMPASS),
        )
    }

    val featureNavigationMap = remember {
        mapOf(
            WatchDestinations.HEART_RATE to onNavigateToHeartRate,
            WatchDestinations.CHAT to onNavigateToChat,
            WatchDestinations.TIDE to onNavigateToTide,
            WatchDestinations.FISHING_POINT to onNavigateToFishingPoint,
            WatchDestinations.TRAP_LOCATION to onNavigateToTrapLocation,
            WatchDestinations.COMPASS to onNavigateToCompass,
        )
    }
    
    // 시간별 날씨는 이제 같은 스크롤 안에 포함됨

    val weatherData by weatherViewModel.weatherData.collectAsState()
    val locationName by weatherViewModel.locationName.collectAsState()
    val isLoading by weatherViewModel.isLoading.collectAsState()
    val errorMessage by weatherViewModel.errorMessage.collectAsState()
    val latitude by weatherViewModel.latitude.collectAsState()
    val longitude by weatherViewModel.longitude.collectAsState()
    
    // LocationViewModel 상태들
    val currentLocationName by locationViewModel.locationName.collectAsState()
    val currentLatitude by locationViewModel.latitude.collectAsState()
    val currentLongitude by locationViewModel.longitude.collectAsState()
    val isLocationLoading by locationViewModel.isLocationLoading.collectAsState()
    
    // LocationViewModel에서 WeatherViewModel로 위치 정보 동기화
    LaunchedEffect(currentLocationName, currentLatitude, currentLongitude) {
        val locationName = currentLocationName
        val latitude = currentLatitude
        val longitude = currentLongitude
        
        if (!locationName.isNullOrEmpty() && latitude != null && longitude != null) {
            android.util.Log.d("LocationSync", "Syncing location from LocationViewModel to WeatherViewModel")
            android.util.Log.d("LocationSync", "Location: $locationName, Lat: $latitude, Lon: $longitude")
            weatherViewModel.updateLocationName(locationName)
            
            // WeatherViewModel의 위치 정보를 즉시 업데이트
            weatherViewModel.updateLocation(latitude, longitude)
            
            // WeatherViewModel의 위치 정보도 업데이트
            val (baseDate, baseTime) = getValidBaseDateTime()
            weatherViewModel.fetchWeatherData(
                serviceKey = BuildConfig.DATA_GO_KR_API_KEY,
                baseDate = baseDate,
                baseTime = baseTime,
                lat = latitude,
                lon = longitude,
                locationName = locationName
            )
        }
    }
    
    // BadaTime API 상태들
    val currentWeather by badaTimeViewModel.currentWeather.collectAsState()
    val forecastWeather by badaTimeViewModel.forecastWeather.collectAsState()
    val badaTimeLoading by badaTimeViewModel.isLoading.collectAsState()
    val badaTimeError by badaTimeViewModel.error.collectAsState()
    val waterTemperature by badaTimeViewModel.waterTemperature.collectAsState()
    val tideData by badaTimeViewModel.tideData.collectAsState() // tide 데이터 추가

    var selectedFeatureIndex by remember { mutableStateOf<Int?>(null) }
    var isAppInitialized by remember { mutableStateOf(false) }

    val fusedLocationClient: FusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)) {
            // LocationViewModel을 사용하여 위치 가져오기
            locationViewModel.startLocationFetch(context, weatherViewModel)
        } else {
            fallbackToDefaultLocationInternal(weatherViewModel)
        }
    }

    // App initialization without loading screen
    LaunchedEffect(Unit) {
        isAppInitialized = true // Skip loading screen entirely

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            // LocationViewModel을 사용하여 위치 가져오기
            locationViewModel.startLocationFetch(context, weatherViewModel)
        }
    }
    
    // BadaTime API 호출 (위치 로딩 상태 고려)
    LaunchedEffect(latitude, longitude, isLocationLoading) {
        android.util.Log.d("WeatherMainScreen", "🔄 LaunchedEffect triggered - lat=$latitude, lon=$longitude, isLoading=$isLocationLoading")
        
        if (latitude != null && longitude != null) {
            android.util.Log.e("WeatherMainScreen", "🌊 === CALLING BADATIME APIs ===")
            android.util.Log.e("WeatherMainScreen", "🌊 Calling BadaTime API with lat=$latitude, lon=$longitude")
            badaTimeViewModel.loadCurrentWeather(latitude!!, longitude!!)
            badaTimeViewModel.loadForecastWeather(latitude!!, longitude!!)
            android.util.Log.e("WeatherMainScreen", "🌊 About to call loadTideData with lat=$latitude, lon=$longitude")
            badaTimeViewModel.loadTideData(latitude!!, longitude!!)
            android.util.Log.e("WeatherMainScreen", "🌊 Called loadTideData successfully")
            android.util.Log.d("WeatherMainScreen", "🌡️ Loading water temperature...")
            badaTimeViewModel.loadWaterTemperature(latitude!!, longitude!!)
        } else if (!isLocationLoading) {
            // 위치 로딩이 완료되었는데도 위치 정보가 없는 경우에만 에러 출력
            android.util.Log.e("WeatherMainScreen", "❌ Location fetch completed but location is null - lat=$latitude, lon=$longitude")
        } else {
            // 위치 로딩 중인 경우
            android.util.Log.d("WeatherMainScreen", "⏳ Location is loading, waiting for GPS...")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // 메인 날씨 화면 (시간별 날씨가 같은 스크롤 안에 포함됨)
        WeatherMainContent(
            locationName = locationName,
            isLoading = isLoading,
            errorMessage = errorMessage,
            latitude = latitude,
            longitude = longitude,
            features = features,
            selectedFeatureIndex = selectedFeatureIndex,
            onFeatureSelected = { index ->
                selectedFeatureIndex = index
            },
            onFeatureSelectionClear = { selectedFeatureIndex = null },
            onNavigateToHeartRate = onNavigateToHeartRate,
            onNavigateToChat = onNavigateToChat,
            onNavigateToTide = onNavigateToTide,
            onNavigateToFishingPoint = onNavigateToFishingPoint,
            onNavigateToTrapLocation = onNavigateToTrapLocation,
            onNavigateToCompass = onNavigateToCompass,
            onSwipeToHourly = { /* 더 이상 필요 없음 */ },
            // BadaTime 데이터만 사용
            currentWeather = currentWeather,
            forecastWeather = forecastWeather,
            badaTimeLoading = badaTimeLoading,
            badaTimeError = badaTimeError,
            waterTemperature = waterTemperature,
            tideData = tideData // tide 데이터 전달 추가
        )
    }
}

// Internal helper functions
internal fun fallbackToDefaultLocationInternal(weatherViewModel: WeatherViewModel) {
    android.util.Log.d("Location", "=== USING FALLBACK LOCATION ===")
    android.util.Log.d("Location", "GPS failed, using default Busan location")
    android.util.Log.d("Location", "Default coordinates: 35.1796, 129.0756 (Busan)")
    weatherViewModel.updateLocationName("부산시 (기본 위치)")
    val (baseDate, baseTime) = getValidBaseDateTime()
    weatherViewModel.fetchWeatherData(
        serviceKey = BuildConfig.DATA_GO_KR_API_KEY,
        baseDate = baseDate,
        baseTime = baseTime,
        lat = 35.1796,
        lon = 129.0756,
        locationName = "Busan-si"
    )
    android.util.Log.d("Location", "=== FALLBACK LOCATION REQUEST SENT ===")
}

internal fun getValidBaseDateTime(): Pair<String, String> {
    val calendar = Calendar.getInstance()
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(Calendar.MINUTE)
    val availableTimes = listOf(2, 5, 8, 11, 14, 17, 20, 23)
    var baseHour = -1
    for (time in availableTimes.reversed()) {
        if (currentHour > time || (currentHour == time && currentMinute >= 10)) {
            baseHour = time
            break
        }
    }
    if (baseHour == -1) {
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        baseHour = 23
    }
    val baseTime = String.format("%02d00", baseHour)
    val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val baseDate = dateFormat.format(calendar.time)
    android.util.Log.d("Weather", "Selected base_date: $baseDate, base_time: $baseTime")
    return Pair(baseDate, baseTime)
}

@Composable
internal fun ErrorStateInternal(errorMessage: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "⚠️",
            style = MaterialTheme.typography.display1.copy(fontSize = 32.sp, color = Color.Red)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.body1.copy(
                color = Color.Red.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun LoadingStateInternal(locationName: String?, latitude: Double?, longitude: Double?) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_animation")

    Box(modifier = Modifier.fillMaxSize()) {
        // 로딩 화면 배경
        DynamicBackgroundOverlay(
            weatherData = null,
            alpha = 0.7f,
            forceTimeBasedBackground = true
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
        if (locationName.isNullOrEmpty()) {
            val offsetY by infiniteTransition.animateFloat(
                initialValue = -30f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "gps_logo_drop"
            )
            Image(
                painter = painterResource(id = R.mipmap.gps_logo),
                contentDescription = "GPS",
                modifier = Modifier
                    .size(36.dp)
                    .offset(y = offsetY.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            AnimatedLoadingText(
                baseText = "GPS 신호를 수신하고 있습니다",
                style = MaterialTheme.typography.body1.copy(
                    color = Color.White.copy(alpha = 1f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp
                ),
                modifier = Modifier.offset(y = (0).dp)
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
            Image(
                painter = painterResource(id = R.mipmap.weather),
                contentDescription = "Weather",
                modifier = Modifier
                    .size(120.dp)
                    .alpha(rotation / 360f)
                    .offset(y = (-5).dp)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = (-10).dp)
            ) {
                Text(
                    text = "$locationName",
                    style = MaterialTheme.typography.body2.copy(
                        color = Color.White.copy(alpha = 1f),
                        fontSize = 10.sp
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (latitude != null) {
                    Text(
                        text = "위도: ${String.format("%.4f", latitude)}",
                        style = MaterialTheme.typography.body2.copy(
                            color = Color.White.copy(alpha = 1f),
                            fontSize = 9.sp
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(modifier = Modifier.height(1.dp))
                if (longitude != null) {
                    Text(
                        text = "경도: ${String.format("%.4f", longitude)}",
                        style = MaterialTheme.typography.body2.copy(
                            color = Color.White.copy(alpha = 1f),
                            fontSize = 9.sp
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                AnimatedLoadingText(
                    baseText = "날씨 정보를 불러오는 중입니다",
                    style = MaterialTheme.typography.body1.copy(
                        color = Color.White.copy(alpha = 1f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 8.sp
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }
        }
    }
}

@Composable
private fun VoiceFishingLoader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Voice Fishing Logo
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    brush = AppGradients.primaryCard,
                    shape = CircleShape
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.mipmap.voice_fishing_logo_1),
                contentDescription = "Voice Fishing",
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Voice Fishing",
            style = MaterialTheme.typography.title1.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Modern loading indicator
        LoadingIndicatorWithGPS()
    }
}

@Composable
private fun LoadingIndicatorWithGPS() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Pulsing GPS icon
        val gpsAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "gps_pulse"
        )

        Text(
            text = "📍",
            fontSize = 14.sp,
            modifier = Modifier.alpha(gpsAlpha)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Loading dots
        Row {
            repeat(3) { index ->
                val dotAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing, delayMillis = index * 200),
                        repeatMode = RepeatMode.Reverse
                    ), label = "dot_$index"
                )

                Text(
                    text = "●",
                    fontSize = 8.sp,
                    color = AppColors.PrimaryLight.copy(alpha = dotAlpha),
                    modifier = Modifier.padding(horizontal = 1.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "현재 위치를 찾는 중...",
        style = MaterialTheme.typography.body2.copy(
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Light
        )
    )
}

@Composable
private fun WeatherMainContent(
    locationName: String?,
    isLoading: Boolean,
    errorMessage: String?,
    latitude: Double?,
    longitude: Double?,
    features: List<FeatureItem>,
    selectedFeatureIndex: Int?,
    onFeatureSelected: (Int?) -> Unit,
    onFeatureSelectionClear: () -> Unit,
    onNavigateToHeartRate: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToTide: () -> Unit,
    onNavigateToFishingPoint: () -> Unit,
    onNavigateToCompass: () -> Unit = {},
    onNavigateToTrapLocation: () -> Unit = {},
    onSwipeToHourly: () -> Unit = {},
    // BadaTime 데이터만 사용
    currentWeather: com.dive.weatherwatch.data.BadaTimeCurrentResponse? = null,
    forecastWeather: List<com.dive.weatherwatch.data.BadaTimeForecastResponse> = emptyList(),
    badaTimeLoading: Boolean = false,
    badaTimeError: String? = null,
    waterTemperature: String? = null,
    tideData: List<BadaTimeTideResponse> = emptyList() // tide 데이터 파라미터 추가
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var totalDragOffset by remember { mutableStateOf(0f) }
    var hoveredItemIndex by remember { mutableStateOf<Int?>(null) }
    var isNavigating by remember { mutableStateOf(false) }

    // Haptic feedback effect
    LaunchedEffect(hoveredItemIndex) {
        if (hoveredItemIndex != null) {
            vibrate(context)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0A0A0A),
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF000000)
                    ),
                    radius = 300f
                )
            )
            .semantics {
                contentDescription = "메인 화면. 드래그해서 다른 기능으로 이동하거나 개별 요소를 선택하세요. 심박수, 채팅, 조위, 낚시포인트, 나침반 기능이 있습니다."
                role = Role.Button
            }
            .onSizeChanged { boxSize = it }
            .pointerInput(features.size) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // Check if drag started on a ring segment - if so, don't start drag
                        val ringCenter = Offset(size.width / 2f, size.height / 2f)
                        val ringRadius = size.width / 2 * 0.95f
                        val distanceFromCenter = sqrt((offset.x - ringCenter.x).pow(2) + (offset.y - ringCenter.y).pow(2))
                        
                        // Only start drag if not near the ring area
                        if (distanceFromCenter < ringRadius * 1.2f && distanceFromCenter > ringRadius * 0.7f) {
                            // Near ring area - don't process drag
                            return@detectDragGestures
                        }
                        
                        totalDragOffset = 0f
                        hoveredItemIndex = null
                        onFeatureSelectionClear()
                    },
                    onDragEnd = {
                        // Navigate to the final hovered index when drag ends
                        hoveredItemIndex?.let { index ->
                            if (index > 0) { // Skip weather (index 0) since we're already on weather screen
                                // Trigger navigation animation
                                isNavigating = true
                                onFeatureSelected(index) // Keep selection for animation
                            } else {
                                // Reset if weather is selected (no navigation needed)
                                hoveredItemIndex = null
                                onFeatureSelectionClear()
                                totalDragOffset = 0f
                            }
                        } ?: run {
                            // Reset if no selection
                            hoveredItemIndex = null
                            onFeatureSelectionClear()
                            totalDragOffset = 0f
                        }
                    },
                    onDragCancel = {
                        hoveredItemIndex = null
                        onFeatureSelectionClear()
                        totalDragOffset = 0f
                    }
                ) { change, dragAmount ->
                    // Simple and stable drag calculation - use primary direction only
                    val verticalDrag = dragAmount.y
                    val horizontalDrag = dragAmount.x

                    // Use whichever direction has more movement
                    val effectiveDrag = if (abs(verticalDrag) >= abs(horizontalDrag)) {
                        verticalDrag
                    } else {
                        horizontalDrag
                    }

                    totalDragOffset += effectiveDrag

                    // Much lower sensitivity to prevent accidental touches
                    val sensitivity = 35f // Low sensitivity to prevent over-sensitive responses
                    val itemIndex = (totalDragOffset / sensitivity).toInt()
                    val clampedIndex = itemIndex.coerceIn(0, features.size - 1)

                    // Update hovered index during drag for visual feedback only
                    if (clampedIndex != hoveredItemIndex) {
                        hoveredItemIndex = clampedIndex
                        onFeatureSelected(clampedIndex)
                        // Haptic feedback is handled by LaunchedEffect above
                    }
                    change.consume()
                }
            }
    ) {
        // Dynamic Background Overlay (BadaTime 데이터 사용)
        if (currentWeather != null) {
            DynamicBackgroundOverlay(
                currentWeather = currentWeather,
                forecastWeather = forecastWeather,
                alpha = 0.7f // 배경 투명도 증가 (더 선명하게)
            )
        }
        
        // Premium Particle System
        PremiumParticleSystem()
        
        // Glassmorphism Background Layer
        GlassmorphismBackground()
        
        // Simple ring without icons - MainHubScreen.kt style
        SimpleSelectionRing(
            features = features,
            selectedIndex = hoveredItemIndex,
            boxSize = boxSize,
            onFeatureClick = { index ->
                // 개별 기능 클릭 시 바로 네비게이션
                when (index) {
                    1 -> onNavigateToTide()
                    2 -> onNavigateToFishingPoint()
                    3 -> onNavigateToTrapLocation()
                    4 -> onNavigateToChat()
                    5 -> onNavigateToHeartRate()
                    6 -> onNavigateToCompass()
                    // 0은 날씨 (현재 화면)이므로 아무 작업 없음
                }
            }
        )

        // Central weather content
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (selectedFeatureIndex != null) Modifier.blur(radius = 4.dp)
                    else Modifier
                )
        ) {
            if (!errorMessage.isNullOrEmpty()) {
                ErrorStateInternal(errorMessage = errorMessage)
            } else if (!badaTimeError.isNullOrEmpty()) {
                ErrorStateInternal(errorMessage = "날씨 데이터 오류: $badaTimeError")
            } else if (locationName.isNullOrEmpty() || (isLoading && currentWeather == null) || badaTimeLoading) {
                LoadingStateInternal(locationName = locationName, latitude = latitude, longitude = longitude)
            } else {
                WeatherContent(
                    locationName = locationName!!, 
                    onSwipeToHourly = onSwipeToHourly,
                    // BadaTime 데이터만 사용
                    currentWeather = currentWeather,
                    forecastWeather = forecastWeather,
                    badaTimeLoading = badaTimeLoading,
                    badaTimeError = badaTimeError,
                    waterTemperature = waterTemperature,
                    tideData = tideData // tide 데이터 전달 추가
                )
            }
        }

        // Simple Feature Description at Bottom
        hoveredItemIndex?.let { index ->
            val feature = features.getOrNull(index)
            feature?.let {
                BlurOverlayFeaturePreview(
                    feature = it,
                    shouldNavigate = isNavigating,
                    onNavigate = {
                        coroutineScope.launch {
                            // Reset states first to hide overlay
                            hoveredItemIndex = null
                            onFeatureSelectionClear()
                            isNavigating = false
                            totalDragOffset = 0f
                            
                            // Then navigate immediately
                            when (index) {
                                1 -> onNavigateToTide()
                                2 -> onNavigateToFishingPoint()
                                3 -> onNavigateToTrapLocation()
                                4 -> onNavigateToChat()
                                5 -> onNavigateToHeartRate()
                                6 -> onNavigateToCompass()
                                        }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ModernFeatureTooltip(
    feature: FeatureItem,
    ringRadius: Float,
    center: Offset,
    featureIndex: Int,
    totalFeatures: Int
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(feature) {
        visible = false
        kotlinx.coroutines.delay(80)
        visible = true
    }
    
    // Calculate tooltip position based on feature position on ring
    val startAngle = -90f  // 12시 위치에서 시작
    val totalAngleRange = 90f  // 12시에서 6시까지 180도 범위
    val gapAngle = 3f  // 4f -> 2f로 간격 좁히기
    val numGaps = totalFeatures - 1
    val totalArcAngle = totalAngleRange - (gapAngle * numGaps)
    val segmentSweepAngle = totalArcAngle / totalFeatures
    val featureAngle = startAngle + (segmentSweepAngle + gapAngle) * featureIndex + (segmentSweepAngle / 2)
    
    // Convert angle to position
    val featurePosition = Offset(
        center.x + (ringRadius * cos(Math.toRadians(featureAngle.toDouble()))).toFloat(),
        center.y + (ringRadius * sin(Math.toRadians(featureAngle.toDouble()))).toFloat()
    )
    
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.3f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh
            )
        ) + fadeIn(
            animationSpec = tween(200, easing = FastOutSlowInEasing)
        ),
        exit = scaleOut(
            targetScale = 0.3f,
            animationSpec = tween(150, easing = FastOutLinearInEasing)
        ) + fadeOut(
            animationSpec = tween(100)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Modern Speech Bubble Tooltip
            SpeechBubbleTooltip(
                feature = feature,
                position = featurePosition,
                ringCenter = center,
                bubbleDirection = getBubbleDirection(featureAngle)
            )
        }
    }
}

@Composable
private fun SpeechBubbleTooltip(
    feature: FeatureItem,
    position: Offset,
    ringCenter: Offset,
    bubbleDirection: BubbleDirection
) {
    val density = LocalDensity.current
    
    // Bubble dimensions
    val bubbleWidth = 140.dp
    val bubbleHeight = 80.dp
    val tailSize = 8.dp
    
    // Calculate bubble position based on direction, keeping within screen bounds
    val bubbleOffset = with(density) {
        val screenWidth = 400.dp.toPx() // Approximate watch screen width
        val screenHeight = 400.dp.toPx() // Approximate watch screen height
        val margin = 16.dp.toPx()
        
        // Place bubble in center-top area, guaranteed within screen
        val bubbleX = (screenWidth - bubbleWidth.toPx()) / 2 // Perfect horizontal center
        val bubbleY = 40.dp.toPx() // Safe top margin
        
        Offset(bubbleX, bubbleY)
    }
    
    Box(
        modifier = Modifier
            .offset(
                x = with(density) { bubbleOffset.x.toDp() },
                y = with(density) { bubbleOffset.y.toDp() }
            )
            .size(bubbleWidth, bubbleHeight)
    ) {
        // Gradient background with glassmorphism
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(18.dp),
                    clip = false
                )
                .clip(RoundedCornerShape(18.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            getFeatureBackgroundColor(feature.name).copy(alpha = 0.8f),
                            Color.Black.copy(alpha = 0.75f)
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Feature icon (smaller) - 시간 기반 색상 적용
                Icon(
                    imageVector = feature.icon,
                    contentDescription = feature.name,
                    tint = getFeatureColor(feature.name),
                    modifier = Modifier.size(40.dp) // 크기를 크게 변경해서 확인
                )
                
                // Feature name
                Text(
                    text = feature.name,
                    style = MaterialTheme.typography.body1.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                // Feature description (shorter)
                Text(
                    text = getShortDescription(feature.name),
                    style = MaterialTheme.typography.body2.copy(
                        fontSize = 10.sp,
                        lineHeight = 12.sp
                    ),
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
        
        // Speech bubble tail pointing to ring center
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawSpeechBubbleTail(
                bubbleDirection = bubbleDirection,
                tailSize = tailSize.toPx(),
                bubbleSize = Size(bubbleWidth.toPx(), bubbleHeight.toPx()),
                ringCenter = ringCenter,
                bubblePosition = bubbleOffset,
                color = Color.Black.copy(alpha = 0.85f),
                density = density
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpeechBubbleTail(
    bubbleDirection: BubbleDirection,
    tailSize: Float,
    bubbleSize: Size,
    ringCenter: Offset,
    bubblePosition: Offset,
    color: Color,
    density: androidx.compose.ui.unit.Density
) {
    val path = androidx.compose.ui.graphics.Path()
    
    // Calculate direction from bubble to ring center
    val bubbleCenter = Offset(bubbleSize.width / 2, bubbleSize.height / 2)
    val absoluteBubbleCenter = Offset(
        bubblePosition.x + bubbleCenter.x,
        bubblePosition.y + bubbleCenter.y
    )
    
    // Vector from bubble center to ring center
    val direction = Offset(
        ringCenter.x - absoluteBubbleCenter.x,
        ringCenter.y - absoluteBubbleCenter.y
    )
    
    // Normalize direction
    val length = kotlin.math.sqrt(direction.x * direction.x + direction.y * direction.y)
    if (length > 0) {
        val normalizedDir = Offset(direction.x / length, direction.y / length)
        
        // Find the point on bubble edge closest to ring center
        val edgePoint = Offset(
            bubbleCenter.x + normalizedDir.x * (bubbleSize.width / 2 - tailSize),
            bubbleCenter.y + normalizedDir.y * (bubbleSize.height / 2 - tailSize)
        )
        
        // Create tail pointing toward ring center
        val perpendicular = Offset(-normalizedDir.y, normalizedDir.x)
        
        path.moveTo(
            edgePoint.x + perpendicular.x * tailSize / 2,
            edgePoint.y + perpendicular.y * tailSize / 2
        )
        path.lineTo(
            edgePoint.x + normalizedDir.x * tailSize * 1.5f,
            edgePoint.y + normalizedDir.y * tailSize * 1.5f
        )
        path.lineTo(
            edgePoint.x - perpendicular.x * tailSize / 2,
            edgePoint.y - perpendicular.y * tailSize / 2
        )
        path.close()
        
        drawPath(path, color)
    }
}

private enum class BubbleDirection {
    TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT
}

private fun getBubbleDirection(featureAngle: Float): BubbleDirection {
    return when {
        featureAngle < -60f -> BubbleDirection.BOTTOM_RIGHT
        featureAngle < -40f -> BubbleDirection.TOP_RIGHT
        featureAngle < -20f -> BubbleDirection.TOP_LEFT
        else -> BubbleDirection.BOTTOM_LEFT
    }
}

private fun getShortDescription(featureName: String): String {
    return when (featureName) {
        "날씨" -> "현재 위치\n날씨 정보"
        "심박수" -> "실시간\n심박 측정"
        "AI 채팅", "Voice with Gemini" -> "음성으로\n대화하기"
        "조위정보" -> "실시간\n조위 데이터"
        else -> "기능 설명"
    }
}


@Composable
private fun AnimatedTextContent(
    title: String,
    description: String,
    isNavigating: Boolean = false,
    onNavigationComplete: () -> Unit = {}
) {
    var alpha by remember { mutableStateOf(0f) }
    
    LaunchedEffect(title) {
        alpha = 0f
        kotlinx.coroutines.delay(50)
        alpha = 1f
    }
    
    LaunchedEffect(isNavigating) {
        if (isNavigating) {
            alpha = 0f
            kotlinx.coroutines.delay(160) // Wait for fade out animation
            onNavigationComplete()
        }
    }
    
    val animatedAlpha by animateFloatAsState(
        targetValue = alpha,
        animationSpec = tween(150, easing = LinearEasing),
        label = "text_fade"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.alpha(animatedAlpha)
    ) {
        // Feature title
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Feature description
        Text(
            text = description,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Light
        )
    }
}

@Composable
private fun BlurOverlayFeaturePreview(
    feature: FeatureItem,
    onNavigate: () -> Unit = {},
    shouldNavigate: Boolean = false
) {
    var isNavigating by remember { mutableStateOf(false) }
    
    LaunchedEffect(shouldNavigate) {
        if (shouldNavigate && !isNavigating) {
            isNavigating = true
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Full screen blur overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .blur(radius = 8.dp)
        )
        
        // Central feature display - Text only with animations
        AnimatedTextContent(
            title = feature.name,
            description = feature.description,
            isNavigating = isNavigating,
            onNavigationComplete = onNavigate
        )
    }
}

// Feature별 색상 테마 (시간 기반 적응)
private fun getFeatureColor(featureName: String): Color {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    
    // 밤(20-6시)과 낮(6-20시)에 따라 색상 조정
    val isNight = hour < 6 || hour >= 20
    
    // 디버깅을 위한 로그
    android.util.Log.d("FeatureColor", "Feature: $featureName, Hour: $hour, IsNight: $isNight")
    
    return when (featureName) {
        "날씨" -> if (isNight) Color(0xFF8E9AAF) else Color(0xFFFFD700) // 낮엔 금색으로 테스트
        "심박수" -> if (isNight) Color(0xFFDDA0DD) else Color(0xFFFF1493) // 낮엔 진한 분홍
        "챗봇" -> if (isNight) Color(0xFFA8D1A8) else Color(0xFF32CD32) // 낮엔 라임그린
        "조위 정보" -> if (isNight) Color(0xFFB8A8E8) else Color(0xFF8A2BE2) // 낮엔 블루바이올렛
        "낚시 포인트" -> if (isNight) Color(0xFFE6B86B) else Color(0xFFFF8C00) // 낮엔 주황색
        else -> if (isNight) Color(0xFF9BB5E8) else Color(0xFFFF4500) // 기본값은 오렌지레드
    }
}

// Feature별 배경 색상 (시간 기반 적응, 은은한 반투명)
private fun getFeatureBackgroundColor(featureName: String): Color {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    
    // 밤(20-6시)과 낮(6-20시)에 따라 색상 조정
    val isNight = hour < 6 || hour >= 20
    val alpha = if (isNight) 0.15f else 0.25f // 밤엔 더 연하게
    
    return when (featureName) {
        "날씨" -> if (isNight) Color(0xFF6C7B8A).copy(alpha = alpha) else Color(0xFF1E88E5).copy(alpha = alpha)
        "심박수" -> if (isNight) Color(0xFF9A5B9A).copy(alpha = alpha) else Color(0xFFE53935).copy(alpha = alpha)
        "챗봇" -> if (isNight) Color(0xFF6B8B6B).copy(alpha = alpha) else Color(0xFF43A047).copy(alpha = alpha)
        "조위 정보" -> if (isNight) Color(0xFF7A6B8A).copy(alpha = alpha) else Color(0xFF5E35B1).copy(alpha = alpha)
        "낚시 포인트" -> if (isNight) Color(0xFF8B7355).copy(alpha = alpha) else Color(0xFFFF7043).copy(alpha = alpha)
        else -> Color.Black.copy(alpha = alpha) // 기본값
    }
}

@Composable
private fun SimpleSelectionRing(
    features: List<FeatureItem>,
    selectedIndex: Int?,
    boxSize: IntSize,
    onFeatureClick: (Int) -> Unit = {}
) {
    val ringRadius = boxSize.width / 2 * 0.95f
    val center = Offset(boxSize.width / 2f, boxSize.height / 2f)

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Canvas for ring visual
        Canvas(modifier = Modifier.fillMaxSize()) {
            val startAngle = -45f  // 12시 위치에서 시작
            val totalAngleRange = 90f  // 12시에서 6시까지 180도 범위
            val gapAngle = 3f  // 4f -> 2f로 간격 좁히기
            val numGaps = features.size - 1
            val totalArcAngle = totalAngleRange - (gapAngle * numGaps)
            val segmentSweepAngle = totalArcAngle / features.size

            features.forEachIndexed { index, _ ->
                val segmentStartAngle = startAngle + (segmentSweepAngle + gapAngle) * index
                val isSelected = index == selectedIndex
                val isWeatherFeature = index == 0

                // Weather feature is always highlighted when it's the main screen, no icons
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)
                val isNight = hour < 6 || hour >= 19
                
                val color = when {
                    isWeatherFeature && selectedIndex == null -> Color.White
                    isSelected -> Color.White
                    else -> if (isNight) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.2f) // 밤엔 보라색, 낮엔 라임그린
                }

                val strokeWidth = when {
                    isWeatherFeature && selectedIndex == null -> 2.5.dp.toPx()  // 4dp -> 2.5dp로 얇게
                    isSelected -> 2.5.dp.toPx()  // 4dp -> 2.5dp로 얇게
                    else -> 1.5f.dp.toPx()  // 2.5dp -> 1.5dp로 얇게
                }

                // Glow effect for selected items
                if (isSelected || (isWeatherFeature && selectedIndex == null)) {
                    drawArc(
                        color = color.copy(alpha = 0.3f),
                        startAngle = segmentStartAngle,
                        sweepAngle = segmentSweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth + 1.dp.toPx()),  // 2dp -> 1dp로 글로우 효과도 얇게
                        size = Size(ringRadius * 2, ringRadius * 2),
                        topLeft = Offset(center.x - ringRadius, center.y - ringRadius)
                    )
                }

                drawArc(
                    color = color,
                    startAngle = segmentStartAngle,
                    sweepAngle = segmentSweepAngle,
                    useCenter = false,
                    style = Stroke(
                        width = strokeWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    ),
                    size = Size(ringRadius * 2, ringRadius * 2),
                    topLeft = Offset(center.x - ringRadius, center.y - ringRadius)
                )
            }
        }
        
        // Large clickable areas for each ring segment
        features.forEachIndexed { index, feature ->
            val startAngle = -45f
            val totalAngleRange = 90f
            val gapAngle = 3f
            val numGaps = features.size - 1
            val totalArcAngle = totalAngleRange - (gapAngle * numGaps)
            val segmentSweepAngle = totalArcAngle / features.size
            val segmentStartAngle = startAngle + (segmentSweepAngle + gapAngle) * index
            val segmentCenterAngle = segmentStartAngle + (segmentSweepAngle / 2)
            
            // Calculate position for clickable area (further out on the ring)
            val angleRad = Math.toRadians(segmentCenterAngle.toDouble())
            val buttonRadius = ringRadius * 1.1f // Move buttons slightly outside the ring
            val buttonX = (center.x + (buttonRadius * cos(angleRad))).toFloat()
            val buttonY = (center.y + (buttonRadius * sin(angleRad))).toFloat()
            
            Box(
                modifier = Modifier
                    .offset(
                        x = with(LocalDensity.current) { (buttonX - 40.dp.toPx()).toDp() },
                        y = with(LocalDensity.current) { (buttonY - 40.dp.toPx()).toDp() }
                    )
                    .size(80.dp) // 더 큰 클릭 영역
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        android.util.Log.d("RingClick", "Clicked feature: ${feature.name} (index: $index)")
                        onFeatureClick(index)
                    }
                    .semantics {
                        contentDescription = "${feature.name} 버튼. ${feature.description}"
                        role = Role.Button
                    }
                    // 클릭 가능한 영역
            ) {
                // 클릭 영역을 시각적으로 표시하기 위한 디버그 텍스트
                Text(
                    text = "${index}", // 인덱스 번호로 표시
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun WeatherContent(
    locationName: String,
    onSwipeToHourly: () -> Unit = {},
    // BadaTime 데이터만 사용
    currentWeather: com.dive.weatherwatch.data.BadaTimeCurrentResponse? = null,
    forecastWeather: List<com.dive.weatherwatch.data.BadaTimeForecastResponse> = emptyList(),
    badaTimeLoading: Boolean = false,
    badaTimeError: String? = null,
    waterTemperature: String? = null,
    tideData: List<BadaTimeTideResponse> = emptyList() // tide 데이터 파라미터 추가
) {
    // BadaTime 데이터가 있으면 우선 사용, 없으면 기존 데이터 사용
    android.util.Log.d("WeatherContent", "currentWeather=$currentWeather")
    android.util.Log.d("WeatherContent", "forecastWeather size=${forecastWeather.size}")
    android.util.Log.d("WeatherContent", "badaTimeLoading=$badaTimeLoading")
    android.util.Log.d("WeatherContent", "badaTimeError=$badaTimeError")
    
    if (currentWeather != null) {
        android.util.Log.d("WeatherContent", "currentWeather.temp value: '${currentWeather.temp}'")
        val temperature = currentWeather.temp?.takeIf { it.isNotBlank() } ?: "N/A"
        val sky = currentWeather.sky?.takeIf { it.isNotBlank() } ?: "맑음"
        val skyCode = (currentWeather.skyCode ?: currentWeather.skycode)?.takeIf { it.isNotBlank() } ?: "1"
        val windSpeed = currentWeather.windspd?.takeIf { it.isNotBlank() } ?: "0"
        val humidity = currentWeather.humidity?.takeIf { it.isNotBlank() } ?: "0"
        val rain = currentWeather.rain?.takeIf { it.isNotBlank() } ?: "0.0"
        val winddir = currentWeather.winddir?.takeIf { it.isNotBlank() } ?: "N"
        android.util.Log.d("WeatherContent", "processed temperature value: '$temperature', skyCode: '$skyCode'")

        androidx.wear.compose.material.ScalingLazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 0.dp, bottom = 40.dp),
            anchorType = androidx.wear.compose.material.ScalingLazyListAnchorType.ItemStart,
            autoCentering = null
        ) {
            // Date and time at top
            item {
                Text(
                    text = "${SimpleDateFormat("M월 d일 (E)", Locale.KOREAN).format(Date())} ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}",
                    style = MaterialTheme.typography.body2.copy(
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }

            // Location only
            item {
                Text(
                    text = "$locationName",
                    style = MaterialTheme.typography.body2.copy(
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Main weather box with temperature comparison
            item {
                android.util.Log.d("WeatherContent", "About to convert temperature '$temperature' to double")
                val currentTemp = try {
                    if (temperature == "N/A" || temperature.isBlank()) {
                        0
                    } else {
                        temperature.toDoubleOrNull()?.toInt() ?: 0
                    }
                } catch (e: Exception) {
                    android.util.Log.e("WeatherContent", "Failed to convert temperature '$temperature'", e)
                    0
                }
                android.util.Log.d("WeatherContent", "Converted temperature to currentTemp: $currentTemp")
                CompactMainWeatherBox(sky = skyCode, temperature = temperature, currentTemperature = currentTemp, forecastWeather = forecastWeather)
            }

            // Weather detail cards in 2x2 grid - BadaTime 데이터 사용
            item {
                // 중앙열 기준 레이아웃 - 화면 전체를 사용
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 첫 번째 줄: 풍속, 습도, 강수확률 (습도가 화면 중앙)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 풍속 (중앙에서 왼쪽으로)
                        WindSpeedCard(
                            windSpeed = windSpeed,
                            windDirection = winddir,
                            modifier = Modifier.width(60.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // 습도 (중앙 위치)
                        ModernWeatherCard(
                            icon = "💧", 
                            label = "습도", 
                            value = "${humidity}%",
                            modifier = Modifier.width(60.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // 강수확률 (중앙에서 오른쪽으로)
                        val rainProbability = if (forecastWeather.isNotEmpty()) {
                            "${forecastWeather.firstOrNull()?.rain ?: "0"}%"
                        } else {
                            "N/A"
                        }
                        RainCard(
                            rainProbability = rainProbability,
                            rainfall = rain,
                            modifier = Modifier.width(60.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    // 두 번째 줄: 파고, 수온, 미세먼지농도 (수온이 화면 중앙)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 파고 (중앙에서 왼쪽으로)
                        ModernWeatherCard(
                            icon = "🌊", 
                            label = "파고", 
                            value = "${currentWeather?.pago ?: "0.0"}m",
                            modifier = Modifier.width(60.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // 수온 (중앙 위치)
                        val waterTempValue = waterTemperature?.let { "${it}°C" } ?: "정보없음"
                        ModernWeatherCard(
                            icon = "🌡️", 
                            label = "수온", 
                            value = waterTempValue,
                            modifier = Modifier.width(60.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // 초미세먼지농도 (중앙에서 오른쪽으로)
                        val pm25Status = currentWeather?.pm25Status ?: "정보없음"
                        ModernWeatherCard(
                            icon = "🌫️", 
                            label = "초미세먼지", 
                            value = pm25Status,
                            modifier = Modifier.width(60.dp)
                        )
                    }
                }
            }
            
            // 시간별 날씨 섹션
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "시간별 날씨",
                        style = MaterialTheme.typography.title1.copy(
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // forecastWeather 데이터 상태 로그
                    android.util.Log.d("WeatherContent", "시간별 날씨에 전달하는 forecastWeather size: ${forecastWeather.size}")
                    if (forecastWeather.isNotEmpty()) {
                        android.util.Log.d("WeatherContent", "첫 번째 forecast 항목: ${forecastWeather.first()}")
                    }
                    InlineHourlyWeatherContent(forecastWeather = forecastWeather, currentWeather = currentWeather)
                }
            }
            
            // 주간 날씨 섹션
            item {
                WeeklyWeatherContent(forecastWeather = forecastWeather)
            }
            
            // 천체 정보 섹션 (주간 날씨 바로 아래)
            item {
                android.util.Log.d("WeatherContent", "🌊 tideData.size = ${tideData.size}")
                android.util.Log.d("WeatherContent", "🌊 tideData.firstOrNull() = ${tideData.firstOrNull()}")
                CelestialInfoContent(tideData = tideData.firstOrNull()) // 실제 tide 데이터 사용
            }
        }
    } else {
        // BadaTime 데이터가 없을 때 로딩 상태 또는 오류 메시지 표시
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "날씨 데이터를 불러오는 중...",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun NewMainWeatherBox(sky: String, temperature: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.4f)
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
            // Weather icon (시간대 고려)
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val isCurrentNight = currentHour < 6 || currentHour >= 19
            
            val weatherIcon = when (sky) {
                "1" -> if (isCurrentNight) "🌙" else "☀️"
                "3" -> "⛅"
                "4" -> "☁️"
                else -> if (isCurrentNight) "🌙" else "🌤️"
            }
            Text(
                text = weatherIcon,
                style = MaterialTheme.typography.display1.copy(fontSize = 32.sp),
                modifier = Modifier.padding(end = 12.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Temperature
                Text(
                    text = "${temperature}°",
                    style = MaterialTheme.typography.display2.copy(
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
                    style = MaterialTheme.typography.body2.copy(
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun MinMaxTemperature(
    currentTemp: Int,
    forecastWeather: List<com.dive.weatherwatch.data.BadaTimeForecastResponse> = emptyList()
) {
    // 실제 API 데이터에서 오늘의 최저/최고 온도 계산
    val (minTemp, maxTemp) = remember(forecastWeather) {
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
        
        val todayTemperatures = forecastWeather
            .filter { it.ymdt?.startsWith(today) == true }
            .mapNotNull { forecast ->
                val tempValue = forecast.tempBom ?: forecast.temp
                tempValue?.toDoubleOrNull()
            }
        
        android.util.Log.d("MinMaxTemp", "오늘($today) 온도 데이터: ${todayTemperatures.joinToString(",")}")
        
        if (todayTemperatures.isNotEmpty()) {
            val min = todayTemperatures.minOrNull() ?: currentTemp
            val max = todayTemperatures.maxOrNull() ?: currentTemp
            android.util.Log.d("MinMaxTemp", "계산된 최저/최고: $min°/$max°")
            Pair(min, max)
        } else {
            android.util.Log.d("MinMaxTemp", "오늘 데이터 없음, 현재 온도 사용: $currentTemp")
            // API 데이터가 없으면 현재 온도 기준으로 추정
            Pair(currentTemp - 3, currentTemp + 5)
        }
    }
    
    Text(
        text = "${minTemp}° / ${maxTemp}°",
        style = MaterialTheme.typography.body2.copy(
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 7.sp,
            fontWeight = FontWeight.Medium
        ),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun TemperatureComparisonInPanel(
    currentTemperature: Int,
    forecastWeather: List<com.dive.weatherwatch.data.BadaTimeForecastResponse> = emptyList()
) {
    val context = LocalContext.current
    
    // forecast 데이터에서 어제 온도 찾기
    val yesterdayTemp = remember(forecastWeather) {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -1) // 하루 전
        val yesterdayDateFormat = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
        val yesterdayDateStr = yesterdayDateFormat.format(calendar.time)
        
        android.util.Log.e("TempComparison", "=== 어제 온도 검색 시작 ===")
        android.util.Log.e("TempComparison", "현재 온도: $currentTemperature")
        android.util.Log.e("TempComparison", "찾는 어제 날짜: $yesterdayDateStr")
        android.util.Log.e("TempComparison", "총 forecast 데이터 개수: ${forecastWeather.size}")
        
        // 모든 forecast 데이터 출력
        forecastWeather.forEachIndexed { index, item ->
            android.util.Log.e("TempComparison", "[$index] ymdt=${item.ymdt}, temp=${item.temp}, tempBom=${item.tempBom}, sky=${item.sky}")
        }
        
        // forecast 데이터에서 어제 날짜의 온도 찾기
        val yesterdayForecasts = forecastWeather.filter { 
            it.ymdt?.startsWith(yesterdayDateStr) == true 
        }
        
        android.util.Log.e("TempComparison", "어제 날짜와 일치하는 forecast 개수: ${yesterdayForecasts.size}")
        yesterdayForecasts.forEachIndexed { index, item ->
            android.util.Log.e("TempComparison", "어제 데이터[$index]: ymdt=${item.ymdt}, temp=${item.temp}, tempBom=${item.tempBom}")
        }
        
        val yesterdayForecast = yesterdayForecasts.firstOrNull()
        // BOM이 포함된 tempBom 필드를 먼저 시도하고, 없으면 일반 temp 필드 사용
        val tempValue = yesterdayForecast?.tempBom ?: yesterdayForecast?.temp
        val temp = tempValue?.toDoubleOrNull() ?: currentTemperature.toDouble()
        
        android.util.Log.e("TempComparison", "최종 선택된 어제 온도: $temp (tempBom: ${yesterdayForecast?.tempBom}, temp: ${yesterdayForecast?.temp})")
        android.util.Log.e("TempComparison", "=== 어제 온도 검색 완료 ===")
        
        temp
    }
    
    // 현재 온도를 어제 온도로 저장 (내일 비교를 위해)
    LaunchedEffect(currentTemperature) {
        val sharedPref = context.getSharedPreferences("weather_temp_history", Context.MODE_PRIVATE)
        val currentDateKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val lastSavedDate = sharedPref.getString("last_saved_date", "")
        
        // 오늘 아직 저장하지 않았다면 저장
        if (lastSavedDate != currentDateKey) {
            with(sharedPref.edit()) {
                // 어제 데이터를 어제나 만료
                val existingTemp = sharedPref.getInt("today_temp", currentTemperature)
                putInt("yesterday_temp", existingTemp)
                
                // 오늘 온도 저장
                putInt("today_temp", currentTemperature)
                putString("last_saved_date", currentDateKey)
                apply()
            }
        }
    }
    
    val tempDifference = currentTemperature - yesterdayTemp
    
    // 디버깅용 로그
    android.util.Log.d("TempComparison", "현재: $currentTemperature, 어제: $yesterdayTemp, 차이: $tempDifference")
    
    // 테스트: 항상 표시 (차이가 0이어도 "어제와 같아요" 표시)
    if (true) {
        val annotatedString = if (tempDifference == 0.0) {
            buildAnnotatedString {
                append("어제와 ")
                withStyle(
                    style = SpanStyle(
                        color = Color(0xFF00B894),
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append("같아요")
                }
            }
        } else {
            val isHigher = tempDifference > 0
            val highlightColor = if (isHigher) Color(0xFFFF4757) else Color(0xFF3742FA)
            val statusText = if (isHigher) "높아요" else "낮아요"
            
            buildAnnotatedString {
                append("어제보다 ${kotlin.math.abs(tempDifference)}° ")
                withStyle(
                    style = SpanStyle(
                        color = highlightColor,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(statusText)
                }
            }
        }
        
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.body2.copy(
                color = Color.White,
                fontSize = 7.sp
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(top = 3 .dp)
        )
    }
}

@Composable
private fun CompactMainWeatherBox(
    sky: String, 
    temperature: String, 
    currentTemperature: Int = 0,
    forecastWeather: List<com.dive.weatherwatch.data.BadaTimeForecastResponse> = emptyList()
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.6f) // 패널 크기 확대 (0.5f -> 0.6f)
            .height(80.dp) // 패널 높이 명시적 설정
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    radius = 300f
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(1.dp)
    ) {
        // 모든 날씨 정보를 하나의 Column으로 그룹화
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 1.dp), // 전체 그룹을 위아래로 이동 조절
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 메인 날씨 정보 (아이콘 + 온도 + 상태)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Weather icon (시간대 고려)
                val calendar = Calendar.getInstance()
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentMinute = calendar.get(Calendar.MINUTE)
                val isCurrentNight = currentHour < 6 || currentHour >= 19
                
                val weatherIcon = when (sky) {
                    "1" -> if (isCurrentNight) "🌙" else "☀️"
                    "3" -> "⛅"
                    "4" -> "☁️"
                    else -> if (isCurrentNight) "🌙" else "🌤️"
                }
                Text(
                    text = weatherIcon,
                    style = MaterialTheme.typography.display1.copy(fontSize = 24.sp),
                    modifier = Modifier.padding(end = 8.dp)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Temperature
                    Text(
                        text = "${temperature}°",
                        style = MaterialTheme.typography.display2.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
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
                        style = MaterialTheme.typography.body2.copy(
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 8.sp
                        )
                    )
                }
            }
            
            // 최저/최고 온도
            Spacer(modifier = Modifier.height(4.dp))
            MinMaxTemperature(
                currentTemp = currentTemperature,
                forecastWeather = forecastWeather
            )
            
            // 온도 비교 텍스트
            if (currentTemperature > 0) {
                Spacer(modifier = Modifier.height(1.dp)) // 간격 좁히기
                TemperatureComparisonInPanel(
                    currentTemperature = currentTemperature,
                    forecastWeather = forecastWeather
                )
            }
        }
    }
}

@Composable
fun ModernWeatherCard(icon: String, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(8.dp)
,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.body1.copy(fontSize = 18.sp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.body2.copy(
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 7.sp
            ),
            textAlign = TextAlign.Center
        )
        Text(
            text = value,
            style = MaterialTheme.typography.body1.copy(
                color = Color.White,
                fontSize = 7.sp,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )
    }
}

// 풍속(풍향) 전용 카드 - 한 줄에 표시
@Composable
fun WindSpeedCard(windSpeed: String, windDirection: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(8.dp)
,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🌬️",
            style = MaterialTheme.typography.body1.copy(fontSize = 18.sp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "풍속",
            style = MaterialTheme.typography.body2.copy(
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 8.sp
            ),
            textAlign = TextAlign.Center
        )
        // 풍속과 풍향을 한 줄에 표시
        Text(
            text = "${windSpeed}m/s ${getWindDirectionArrow(windDirection)}", // 공백 제거
            style = MaterialTheme.typography.body1.copy(
                color = Color.White,
                fontSize = 7.sp,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center,
            maxLines = 1 // 강제로 한 줄만
        )
    }
}

// 강수확률(강수량) 전용 카드
@Composable
fun RainCard(rainProbability: String, rainfall: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(8.dp)
,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🌧️",
            style = MaterialTheme.typography.body1.copy(fontSize = 18.sp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "강수확률",
            style = MaterialTheme.typography.body2.copy(
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 8.sp
            ),
            textAlign = TextAlign.Center
        )
        Text(
            text = rainProbability,
            style = MaterialTheme.typography.body1.copy(
                color = Color.White,
                fontSize = 7.sp,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )
        // 강수량 표시
        if (rainfall != "0.0") {
            Text(
                text = "${rainfall}mm",
                style = MaterialTheme.typography.body2.copy(
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 7.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

// 풍향을 화살표로 변환하는 함수
private fun getWindDirectionArrow(direction: String): String {
    return when (direction.uppercase()) {
        "N" -> "↑"
        "NE" -> "↗"
        "E" -> "→"
        "SE" -> "↘"
        "S" -> "↓"
        "SW" -> "↙"
        "W" -> "←"
        "NW" -> "↖"
        else -> "○" // 무풍 또는 알 수 없는 방향
    }
}

// 시간별 날씨 데이터 모델
data class HourlyWeatherData(
    val time: String,
    val temperature: Double,
    val skyCondition: String,
    val precipitationAmount: String,
    val weatherIcon: String
)

// 첫 번째는 current 데이터, 나머지는 forecast 데이터에서 가져오는 새로운 함수
private fun processMixedCurrentAndForecastData(
    forecastData: List<com.dive.weatherwatch.data.BadaTimeForecastResponse>, 
    currentWeather: com.dive.weatherwatch.data.BadaTimeCurrentResponse?
): List<HourlyWeatherData> {
    android.util.Log.e("시간별날씨", "=== 시간별 날씨 데이터 생성 시작 ===")
    android.util.Log.e("시간별날씨", "예측 데이터: ${forecastData.size}개, 현재 날씨: ${currentWeather != null}")
    
    val result = mutableListOf<HourlyWeatherData>()
    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    
    // 첫 번째 항목: 메인 화면과 완전히 동일한 방식으로 current 데이터 처리
    if (currentWeather != null) {
        android.util.Log.e("MixedWeatherData", "=== 메인화면과 동일한 방식으로 current 데이터 처리 ===")
        android.util.Log.e("MixedWeatherData", "Raw currentWeather.temp: '${currentWeather.temp}'")
        android.util.Log.e("MixedWeatherData", "Raw currentWeather.sky: '${currentWeather.sky}'")
        android.util.Log.e("MixedWeatherData", "Raw currentWeather.skyCode: '${currentWeather.skyCode}'")
        android.util.Log.e("MixedWeatherData", "Raw currentWeather.skycode: '${currentWeather.skycode}'")
        
        // 메인 화면과 동일한 방식으로 데이터 처리 (line 1230-1232와 동일)
        val temperature = currentWeather.temp?.takeIf { it.isNotBlank() } ?: "N/A"
        val sky = currentWeather.sky?.takeIf { it.isNotBlank() } ?: "맑음"
        val skyCode = (currentWeather.skyCode ?: currentWeather.skycode)?.takeIf { it.isNotBlank() } ?: "1"
        val rain = currentWeather.rain?.takeIf { it.isNotBlank() } ?: "0.0"
        
        android.util.Log.e("MixedWeatherData", "메인화면 방식 처리 결과: temp='$temperature', sky='$sky', skyCode='$skyCode', rain='$rain'")
        
        // 온도를 Double로 변환 (소수점 그대로 유지)
        val tempDouble = try {
            if (temperature == "N/A" || temperature.isBlank()) {
                0.0
            } else {
                temperature.toDoubleOrNull() ?: 0.0
            }
        } catch (e: Exception) {
            android.util.Log.e("MixedWeatherData", "온도 변환 실패: '$temperature'", e)
            0.0
        }
        android.util.Log.e("MixedWeatherData", "온도 변환 과정: '$temperature' -> $tempDouble")
        val currentTime = String.format("%02d:00", currentHour)
        
        // 메인 화면과 동일한 아이콘 생성 로직 (line 1674-1679와 동일)
        val calendar = java.util.Calendar.getInstance()
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)
        val isCurrentNight = currentHour < 6 || currentHour >= 19
        
        val weatherIcon = when (skyCode) {
            "1" -> if (isCurrentNight) "🌙" else "☀️"
            "3" -> "⛅"
            "4" -> "☁️"
            else -> if (isCurrentNight) "🌙" else "🌤️"
        }
        
        android.util.Log.e("MixedWeatherData", "메인화면 동일 아이콘 로직: skyCode='$skyCode', isNight=$isCurrentNight -> '$weatherIcon'")
        
        val currentHourlyData = HourlyWeatherData(
            time = currentTime,
            temperature = tempDouble,
            skyCondition = sky,
            precipitationAmount = rain,
            weatherIcon = weatherIcon
        )
        
        result.add(currentHourlyData)
        android.util.Log.e("MixedWeatherData", "메인화면과 동일한 첫 번째 데이터 추가: ${currentTime}, ${tempDouble}°C, ${sky}, 아이콘: ${weatherIcon}")
    }
    
    // 나머지 3개 항목: 3시간 단위 예측 데이터에서 생성
    val availableHours = listOf(0, 3, 6, 9, 12, 15, 18, 21)
    
    // 현재 시간 이후의 다음 3개 시간대 찾기
    val targetHours = mutableListOf<Int>()
    var hourPointer = currentHour
    var count = 0
    
    while (count < 3) {
        hourPointer = (hourPointer + 1) % 24
        if (availableHours.contains(hourPointer)) {
            targetHours.add(hourPointer)
            count++
        }
    }
    
    android.util.Log.e("시간별날씨", "선택된 3시간 단위 시간대: $targetHours")
    
    for (targetHour in targetHours) {
        val targetDateStr = if (targetHour <= currentHour) {
            // 다음 날 (예: 현재 23시, 다음이 03시인 경우)
            val tomorrow = java.util.Calendar.getInstance()
            tomorrow.add(java.util.Calendar.DAY_OF_MONTH, 1)
            String.format("%04d%02d%02d", 
                tomorrow.get(java.util.Calendar.YEAR),
                tomorrow.get(java.util.Calendar.MONTH) + 1,
                tomorrow.get(java.util.Calendar.DAY_OF_MONTH)
            )
        } else {
            // 오늘
            val today = java.util.Calendar.getInstance()
            String.format("%04d%02d%02d", 
                today.get(java.util.Calendar.YEAR),
                today.get(java.util.Calendar.MONTH) + 1,
                today.get(java.util.Calendar.DAY_OF_MONTH)
            )
        }
        
        val targetTimeStr = String.format("%s%02d", targetDateStr, targetHour)
        android.util.Log.e("시간별날씨", "검색할 시간: $targetTimeStr ($targetHour)시")
        
        val matchingForecast = forecastData.find { forecast ->
            val ymdt = forecast.ymdt?.replace("-", "")?.replace(":", "") ?: ""
            val aplYmdt = forecast.aplYmdt?.replace("-", "")?.replace(":", "") ?: ""
            
            ymdt.startsWith(targetTimeStr) || aplYmdt.startsWith(targetTimeStr)
        }
        
        if (matchingForecast != null) {
            android.util.Log.e("MixedWeatherData", "=== forecast 데이터 상세 분석 (${targetHour}시) ===")
            android.util.Log.e("MixedWeatherData", "matchingForecast.temp: '${matchingForecast.temp}'")
            android.util.Log.e("MixedWeatherData", "matchingForecast.tempBom: '${matchingForecast.tempBom}'")
            android.util.Log.e("MixedWeatherData", "matchingForecast.sky: '${matchingForecast.sky}'")
            android.util.Log.e("MixedWeatherData", "matchingForecast.skyCode: '${matchingForecast.skyCode}'")
            android.util.Log.e("MixedWeatherData", "matchingForecast.skycode: '${matchingForecast.skycode}'")
            android.util.Log.e("MixedWeatherData", "matchingForecast.rainAmt: '${matchingForecast.rainAmt}'")
            android.util.Log.e("MixedWeatherData", "matchingForecast.rainAmtBom: '${matchingForecast.rainAmtBom}'")
            
            val tempValue = matchingForecast.tempBom?.takeIf { it.isNotBlank() } ?: 
                           matchingForecast.temp ?: "0"
            val temperature = tempValue.toDoubleOrNull() ?: 0.0
            
            val skyCondition = matchingForecast.sky ?: "맑음"
            val skyCodeValue = (matchingForecast.skycode ?: matchingForecast.skyCode) ?: "1"
            val rainAmount = matchingForecast.rainAmtBom?.takeIf { it.isNotBlank() } ?: 
                            matchingForecast.rainAmt ?: "0.0"
            
            android.util.Log.e("MixedWeatherData", "파싱된 forecast 값: temp=$temperature, sky='$skyCondition', skyCode='$skyCodeValue', rain='$rainAmount'")
            
            val displayTime = String.format("%02d:00", targetHour)
            val weatherIcon = getWeatherIcon(skyCondition, rainAmount, targetHour)
            
            android.util.Log.e("MixedWeatherData", "forecast 아이콘 생성: sky='$skyCondition', rain='$rainAmount', hour=$targetHour -> '$weatherIcon'")
            
            val forecastHourlyData = HourlyWeatherData(
                time = displayTime,
                temperature = temperature,
                skyCondition = skyCondition,
                precipitationAmount = rainAmount,
                weatherIcon = weatherIcon
            )
            
            result.add(forecastHourlyData)
            android.util.Log.e("MixedWeatherData", "forecast 데이터 추가: ${displayTime}, ${temperature}°C, ${skyCondition}, 아이콘: ${weatherIcon}")
        } else {
            android.util.Log.w("시간별날씨", "${targetHour}시 예측 데이터 없음 - 임시 데이터 생성")
            // 임시 데이터 생성 (forecast 데이터가 없어도 UI가 깨지지 않도록)
            val displayTime = String.format("%02d:00", targetHour)
            val dummyTemp = 25.0 + (Math.random() * 6 - 3) // 22~28도 사이 랜덤
            val dummyData = HourlyWeatherData(
                time = displayTime,
                temperature = dummyTemp,
                skyCondition = "맑음",
                precipitationAmount = "0.0",
                weatherIcon = if (targetHour < 6 || targetHour >= 20) "🌙" else "☀️"
            )
            result.add(dummyData)
            android.util.Log.w("시간별날씨", "임시 데이터 추가: ${displayTime}, ${dummyTemp}°C")
        }
    }
    
    android.util.Log.e("시간별날씨", "=== 시간별 날씨 데이터 생성 완료: ${result.size}개 ===")
    return result
}

// 기존 동기화 함수는 호환성을 위해 유지
private fun processBadaTimeForecastDataWithCurrentSync(
    forecastData: List<com.dive.weatherwatch.data.BadaTimeForecastResponse>, 
    currentWeather: com.dive.weatherwatch.data.BadaTimeCurrentResponse?
): List<HourlyWeatherData> {
    // 이 함수는 현재 사용되지 않음
    
    // 먼저 기존 로직으로 시간별 데이터를 생성
    val originalHourlyData = processBadaTimeForecastData(forecastData)
    
    // currentWeather 데이터가 있고, 첫 번째 시간이 현재 시간대와 가깝다면 동기화
    if (currentWeather != null && originalHourlyData.isNotEmpty()) {
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val firstDataHour = originalHourlyData.first().time.substringBefore(":").toIntOrNull() ?: 0
        
        
        val currentTemp = currentWeather.temp?.toDoubleOrNull() ?: originalHourlyData.first().temperature
        val currentSky = currentWeather.sky ?: "맑음"
        val currentSkyCode = (currentWeather.skyCode ?: currentWeather.skycode) ?: "1"
        val currentRain = currentWeather.rain ?: "0.0"
        
        android.util.Log.e("HourlyWeatherSync", "현재 날씨 데이터: 온도=${currentTemp}°C, 하늘=${currentSky}, skyCode=${currentSkyCode}, 강수량=${currentRain}")
        android.util.Log.e("HourlyWeatherSync", "기존 첫 번째 데이터: 온도=${originalHourlyData.first().temperature}°C, 하늘=${originalHourlyData.first().skyCondition}, 강수량=${originalHourlyData.first().precipitationAmount}")
        
        // BadaTime 데이터의 강수량을 적절한 형식으로 변환
        val precipitationForIcon = if (currentRain.toDoubleOrNull() ?: 0.0 > 0.0) currentRain else "0"
        val newWeatherIcon = getWeatherIcon(currentSkyCode, precipitationForIcon, firstDataHour)
        
        android.util.Log.e("HourlyWeatherSync", "아이콘 생성: skyCode=${currentSkyCode}, precipitation=${precipitationForIcon}, hour=${firstDataHour} -> ${newWeatherIcon}")
        
        val syncedFirstItem = originalHourlyData.first().copy(
            temperature = currentTemp,
            skyCondition = currentSky,
            precipitationAmount = currentRain,
            weatherIcon = newWeatherIcon
        )
        
        val result = listOf(syncedFirstItem) + originalHourlyData.drop(1)
        android.util.Log.e("HourlyWeatherSync", "동기화 완료: 첫 번째 항목을 현재 날씨로 교체")
        android.util.Log.e("HourlyWeatherSync", "동기화된 첫 번째 데이터: 온도=${syncedFirstItem.temperature}°C, 하늘=${syncedFirstItem.skyCondition}, 강수량=${syncedFirstItem.precipitationAmount}, 아이콘=${syncedFirstItem.weatherIcon}")
        return result
    }
    
    android.util.Log.e("HourlyWeatherSync", "currentWeather가 null이거나 hourlyData가 비어있어 동기화하지 않음")
    return originalHourlyData
}

// BadaTime 예측 데이터를 HourlyWeatherData로 변환 (현재 시간 기준 4개 시간대)
private fun processBadaTimeForecastData(forecastData: List<com.dive.weatherwatch.data.BadaTimeForecastResponse>): List<HourlyWeatherData> {
    // 이 함수는 현재 사용되지 않음
    
    // 현재 시간 구하기
    val calendar = java.util.Calendar.getInstance()
    val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    val currentDate = calendar.get(java.util.Calendar.DATE)
    val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1
    val currentYear = calendar.get(java.util.Calendar.YEAR)
    
    android.util.Log.d("HourlyWeather", "현재 시간: ${currentYear}년 ${currentMonth}월 ${currentDate}일 ${currentHour}시")
    
    // 3시간 단위로 정렬된 시간대 (00, 03, 06, 09, 12, 15, 18, 21)
    val availableHours = listOf(0, 3, 6, 9, 12, 15, 18, 21)
    
    // 현재 시간이 포함된 시간대 찾기 (현재 시간 이하의 가장 큰 시간대)
    val currentTimeSlot = availableHours.filter { it <= currentHour }.maxOrNull() 
        ?: availableHours.last() // 현재 시간이 00시 이전이면 21시 사용
    
    // 현재 시간대부터 4개의 시간대 생성 (3시간씩)
    val targetHours = mutableListOf<Int>()
    var hour = currentTimeSlot
    repeat(4) {
        targetHours.add(hour)
        hour = (hour + 3) % 24
    }
    
    android.util.Log.d("HourlyWeather", "현재 시간대: ${currentTimeSlot}시, 표시할 시간대: $targetHours")
    
    // 해당 시간대의 데이터 찾기 (날짜 고려)
    val result = mutableListOf<HourlyWeatherData>()
    val currentDateStr = String.format("%04d%02d%02d", currentYear, currentMonth, currentDate)
    val tomorrowCalendar = calendar.clone() as java.util.Calendar
    tomorrowCalendar.add(java.util.Calendar.DATE, 1)
    val tomorrowDateStr = String.format("%04d%02d%02d", 
        tomorrowCalendar.get(java.util.Calendar.YEAR),
        tomorrowCalendar.get(java.util.Calendar.MONTH) + 1,
        tomorrowCalendar.get(java.util.Calendar.DATE))
    
    android.util.Log.d("HourlyWeather", "오늘 날짜: $currentDateStr, 내일 날짜: $tomorrowDateStr")
    
    for ((index, targetHour) in targetHours.withIndex()) {
        // 자정이 지나면 다음 날 데이터를 찾아야 함
        val needTomorrowData = targetHour < currentTimeSlot && index > 0
        val targetDateStr = if (needTomorrowData) tomorrowDateStr else currentDateStr
        
        // 해당 시간대의 데이터 찾기
        val matchingForecast = forecastData.find { forecast ->
            val ymdt = forecast.ymdt ?: forecast.aplYmdt ?: ""
            if (ymdt.length >= 10) {
                val forecastDateStr = ymdt.substring(0, 8)
                val forecastHour = ymdt.substring(8, 10).toIntOrNull() ?: -1
                forecastDateStr == targetDateStr && forecastHour == targetHour
            } else false
        }
        
        android.util.Log.d("HourlyWeather", "목표 시간: ${targetHour}시, 목표 날짜: $targetDateStr, 찾은 데이터: ${matchingForecast != null}")
        
        if (matchingForecast != null) {
            val ymdt = matchingForecast.ymdt ?: matchingForecast.aplYmdt ?: ""
            val displayTime = String.format("%02d:00", targetHour)
            
            // 원본 데이터 상세 로그
            android.util.Log.d("HourlyWeather", "=== 원본 forecast 데이터 ===")
            android.util.Log.d("HourlyWeather", "ymdt: '${matchingForecast.ymdt}', aplYmdt: '${matchingForecast.aplYmdt}'")
            android.util.Log.d("HourlyWeather", "temp: '${matchingForecast.temp}', tempBom: '${matchingForecast.tempBom}', sky: '${matchingForecast.sky}', rain: '${matchingForecast.rain}'")
            android.util.Log.d("HourlyWeather", "rainAmt: '${matchingForecast.rainAmt}', rainAmtBom: '${matchingForecast.rainAmtBom}'")
            android.util.Log.d("HourlyWeather", "skycode: '${matchingForecast.skycode}', skyCode: '${matchingForecast.skyCode}'")
            
            // BOM 문자 버전을 우선적으로 확인 (forecast 데이터는 대부분 BOM 포함)
            val tempValue = matchingForecast.tempBom ?: matchingForecast.temp ?: "0"
            val rainAmount = matchingForecast.rainAmtBom ?: matchingForecast.rainAmt ?: "0.0"
            val skyCodeValue = matchingForecast.skycode ?: matchingForecast.skyCode ?: "1"
            val windDir = matchingForecast.winddirBom ?: matchingForecast.winddir ?: "N"
            val windSpd = matchingForecast.windspdBom ?: matchingForecast.windspd ?: "0"
            val humidityValue = matchingForecast.humidityBom ?: matchingForecast.humidity ?: "0"
            val temperature = tempValue.toDoubleOrNull() ?: 0.0
            
            android.util.Log.d("HourlyWeather", "=== 처리된 데이터 ===")
            android.util.Log.d("HourlyWeather", "시간: $displayTime, tempValue: '$tempValue' → 온도: ${temperature}°C")
            android.util.Log.d("HourlyWeather", "하늘: '${matchingForecast.sky}', 강수: '${matchingForecast.rain}'%")
            android.util.Log.d("HourlyWeather", "강수량: '$rainAmount', 스카이코드: '$skyCodeValue'")
            android.util.Log.d("HourlyWeather", "풍향: '$windDir', 풍속: '$windSpd', 습도: '$humidityValue'")
            
            result.add(HourlyWeatherData(
                time = displayTime,
                temperature = temperature,
                skyCondition = matchingForecast.sky ?: "맑음",
                precipitationAmount = rainAmount,
                weatherIcon = getWeatherIcon(skyCodeValue, matchingForecast.rain ?: "0", targetHour)
            ))
        } else {
            android.util.Log.w("HourlyWeather", "${targetHour}시 데이터를 찾을 수 없음")
        }
    }
    
    android.util.Log.d("HourlyWeather", "최종 처리된 데이터 개수: ${result.size}")
    return result
}

// BadaTime forecast 데이터를 주간 날씨 데이터로 변환
private fun processBadaTimeForecastToWeeklyData(forecastData: List<com.dive.weatherwatch.data.BadaTimeForecastResponse>): List<DailyWeatherData> {
    android.util.Log.d("WeeklyWeather", "=== 주간 날씨 데이터 처리 시작 ===")
    android.util.Log.d("WeeklyWeather", "forecast 데이터 개수: ${forecastData.size}")
    
    // 현재 날짜 구하기
    val currentCalendar = Calendar.getInstance()
    val currentDateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(currentCalendar.time)
    android.util.Log.d("WeeklyWeather", "현재 날짜: $currentDateStr")
    
    // 날짜별로 그룹화 (ymdt 필드의 앞 8자리로 날짜 구분)
    val groupedByDate = forecastData.groupBy { forecast ->
        val ymdt = forecast.ymdt ?: forecast.aplYmdt ?: ""
        if (ymdt.length >= 8) ymdt.substring(0, 8) else ""
    }.filter { it.key.isNotEmpty() && it.key >= currentDateStr } // 현재 날짜 이후만 포함
    
    // 날짜순으로 정렬하고 중복 제거
    val sortedGroupedByDate = groupedByDate.toSortedMap()
    
    // 각 날짜별로 유니크한 데이터만 확인하고 로깅
    android.util.Log.d("WeeklyWeather", "=== 중복 제거 전 날짜별 데이터 ===")
    sortedGroupedByDate.entries.forEach { (date, dataList) ->
        android.util.Log.d("WeeklyWeather", "날짜 $date: ${dataList.size}개 데이터")
    }
    
    android.util.Log.d("WeeklyWeather", "현재 날짜 이후 그룹 수: ${sortedGroupedByDate.size}")
    sortedGroupedByDate.keys.take(3).forEach { date ->
        android.util.Log.d("WeeklyWeather", "날짜: $date, 데이터 개수: ${sortedGroupedByDate[date]?.size}")
    }
    
    return sortedGroupedByDate.entries.take(7).mapIndexed { index, (dateStr, dayData) ->
        android.util.Log.d("WeeklyWeather", "=== 날짜 $dateStr 처리 중 (인덱스: $index) ===")
        
        // 날짜 파싱 및 실제 날짜 비교로 정확한 레이블 설정
        val dayName = try {
            val targetCalendar = Calendar.getInstance()
            val targetDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).parse(dateStr)
            targetCalendar.time = targetDate!!
            
            val diffDays = ((targetCalendar.timeInMillis - currentCalendar.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()
            android.util.Log.d("WeeklyWeather", "날짜 ${dateStr}의 차이: ${diffDays}일")
            
            when (diffDays) {
                0 -> {
                    android.util.Log.d("WeeklyWeather", "-> 오늘로 설정")
                    "오늘"
                }
                1 -> {
                    android.util.Log.d("WeeklyWeather", "-> 내일로 설정")
                    "내일"
                }
                else -> {
                    val dayOfWeek = listOf("일", "월", "화", "수", "목", "금", "토")
                    val dayName = dayOfWeek[targetCalendar.get(Calendar.DAY_OF_WEEK) - 1]
                    android.util.Log.d("WeeklyWeather", "-> ${dayName}로 설정")
                    dayName
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("WeeklyWeather", "날짜 파싱 오류: $dateStr", e)
            when (index) {
                0 -> "오늘"
                1 -> "내일"
                else -> {
                    val dayOfWeek = listOf("일", "월", "화", "수", "목", "금", "토")
                    dayOfWeek[(index + getCurrentDayOfWeek()) % 7]
                }
            }
        }
        
        // 하루 중 최저/최고 온도 (BOM 문자 고려)
        val temperatures = dayData.mapNotNull { forecast ->
            val tempValue = forecast.tempBom ?: forecast.temp
            android.util.Log.d("WeeklyWeather", "온도 데이터 - temp: '${forecast.temp}', tempBom: '${forecast.tempBom}', 사용값: '$tempValue'")
            tempValue?.toDoubleOrNull()
        }
        android.util.Log.d("WeeklyWeather", "추출된 온도들: $temperatures")
        
        val minTemp = temperatures.minOrNull() ?: 15.0
        val maxTemp = temperatures.maxOrNull() ?: 25.0
        
        // 하루 중 대표 날씨 (가장 많이 나타나는 하늘상태)
        val skyConditions = dayData.mapNotNull { it.sky }.filter { it.isNotBlank() }
        val representativeSky = skyConditions.groupBy { it }.maxByOrNull { it.value.size }?.key ?: "맑음"
        
        // 강수확률 (최대값)
        val rainProbs = dayData.mapNotNull { it.rain?.toIntOrNull() }
        val maxRainProb = rainProbs.maxOrNull() ?: 0
        android.util.Log.d("WeeklyWeather", "강수확률들: $rainProbs, 최대값: $maxRainProb%")
        
        android.util.Log.d("WeeklyWeather", "대표 하늘상태: '$representativeSky', 온도범위: ${minTemp}°C~${maxTemp}°C")
        
        // 날씨 아이콘 결정
        val dayIcon = when {
            maxRainProb > 60 -> "🌧️"
            maxRainProb > 30 -> "⛅"
            representativeSky.contains("맑음") -> "☀️"
            representativeSky.contains("구름") -> "☁️"
            else -> "☀️"
        }
        
        val nightIcon = when {
            maxRainProb > 60 -> "🌧️"
            maxRainProb > 30 -> "☁️"
            representativeSky.contains("맑음") -> "🌙"
            representativeSky.contains("구름") -> "☁️"
            else -> "🌙"
        }
        
        android.util.Log.d("WeeklyWeather", "$dayName: 낮아이콘=$dayIcon, 밤아이콘=$nightIcon")
        
        val result = DailyWeatherData(
            dayName = dayName,
            dayIcon = dayIcon,
            nightIcon = nightIcon,
            rainProbability = "${maxRainProb}%",
            minTemp = minTemp,
            maxTemp = maxTemp
        )
        
        android.util.Log.d("WeeklyWeather", "완성된 데이터: $result")
        result
    }.distinctBy { it.dayName }.also { weeklyData -> // dayName으로 중복 제거
        android.util.Log.d("WeeklyWeather", "=== 주간 날씨 데이터 처리 완료 ===")
        android.util.Log.d("WeeklyWeather", "중복 제거 후 총 ${weeklyData.size}일간 데이터 생성됨")
        weeklyData.forEach { data ->
            android.util.Log.d("WeeklyWeather", "최종 데이터: ${data.dayName}")
        }
    }
}

// 현재 요일을 숫자로 반환 (일요일=0, 월요일=1, ...)
private fun getCurrentDayOfWeek(): Int {
    val calendar = java.util.Calendar.getInstance()
    return calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1
}

// 주간 날씨 데이터 모델
data class DailyWeatherData(
    val dayName: String,     // 오늘, 내일, 수, 목...
    val dayIcon: String,     // 낮 날씨 아이콘
    val nightIcon: String,   // 밤 날씨 아이콘
    val rainProbability: String, // 강수확률
    val minTemp: Double,     // 최저 온도
    val maxTemp: Double      // 최고 온도
)

// 시간별 날씨 화면
@Composable
fun HourlyWeatherScreen(
    weatherData: com.dive.weatherwatch.data.WeatherResponse?,
    locationName: String,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        if (offset.y < size.height * 0.2f) {
                            onNavigateBack()
                        }
                    }
                )
            }
    ) {
        // Dynamic Background Overlay
        DynamicBackgroundOverlay(
            weatherData = weatherData?.response?.body?.items?.item,
            alpha = 0.7f
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 제목
            Text(
                text = "시간별 날씨",
                style = MaterialTheme.typography.title1.copy(
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
            
            // 시간별 날씨 콘텐츠 (별도 화면에서는 BadaTime 데이터 없이 표시)
            HourlyWeatherContent(
                locationName = locationName
            )
        }
    }
}

// 시간별 날씨 콘텐츠
@Composable
private fun HourlyWeatherContent(
    locationName: String,
    forecastWeather: List<com.dive.weatherwatch.data.BadaTimeForecastResponse> = emptyList(),
    currentWeather: com.dive.weatherwatch.data.BadaTimeCurrentResponse? = null
) {
    val hourlyData = if (forecastWeather.isNotEmpty()) {
        processBadaTimeForecastDataWithCurrentSync(forecastWeather, currentWeather)
    } else {
        emptyList()
    }
    
    if (hourlyData.isNotEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 온도 그래프
            HourlyTemperatureChart(hourlyData)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 시간별 정보 (시간, 아이콘, 강수량)
            HourlyWeatherDetails(hourlyData)
        }
    } else {
        // 데이터가 없을 때
        Text(
            text = "시간별 날씨 데이터를 불러오는 중...",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            modifier = Modifier.padding(32.dp)
        )
    }
}

// 시간별 온도 차트
@Composable
private fun HourlyTemperatureChart(hourlyData: List<HourlyWeatherData>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            if (hourlyData.isEmpty()) return@Canvas
            
            val temperatures = hourlyData.map { it.temperature }
            val minTemp = temperatures.minOrNull() ?: 0.0
            val maxTemp = temperatures.maxOrNull() ?: 0.0
            val tempRange = maxTemp - minTemp
            
            val width = size.width
            val height = size.height
            val padding = 40f
            
            val stepX = (width - padding * 2) / (hourlyData.size - 1).coerceAtLeast(1)
            
            // 온도 점들 계산
            val points = hourlyData.mapIndexed { index, data ->
                val x = padding + index * stepX
                val normalizedTemp = if (tempRange > 0) {
                    (data.temperature - minTemp).toFloat() / tempRange.toFloat()
                } else 0.5f
                val y = height - padding - normalizedTemp * (height - padding * 2)
                Offset(x, y)
            }
            
            // 선 그리기
            if (points.size > 1) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            
            // 온도 점 그리기
            points.forEach { point ->
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = point
                )
            }
        }
        
        // 온도 텍스트 오버레이 - 간단하게 Row로 배치
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            hourlyData.forEach { data ->
                Text(
                    text = "${if (data.temperature % 1.0 == 0.0) data.temperature.toInt() else String.format("%.1f", data.temperature)}°",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

// 시간별 상세 정보 (시간, 아이콘, 강수량)
@Composable
private fun HourlyWeatherDetails(hourlyData: List<HourlyWeatherData>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        hourlyData.forEach { data ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(60.dp)
            ) {
                // 날씨 아이콘
                Text(
                    text = data.weatherIcon,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                // 시간
                Text(
                    text = data.time,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                
                // 강수량
                Text(
                    text = "${data.precipitationAmount}mm",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}


// 날씨 조건에 따른 아이콘 선택 (시간대별 아이콘 지원)
private fun getWeatherIcon(sky: String, precipitation: String, hour: Int): String {
    android.util.Log.e("WeatherIcon", "아이콘 생성 요청: sky='$sky', precipitation='$precipitation', hour=$hour")
    
    // forecast 데이터의 rain 필드는 강수확률이므로, 실제 강수량이 아닌 경우 sky 기준으로 아이콘 선택
    // 강수량이 실제로 측정된 값(mm 단위)인 경우에만 비 아이콘 사용
    val precipitationValue = precipitation.toDoubleOrNull() ?: 0.0
    // 강수확률(%)이 아닌 실제 강수량(mm)인 경우에만 비 아이콘 표시 (5mm 이상)
    val hasActualRain = precipitationValue >= 5.0 && precipitation != "강수없음" && !precipitation.contains("%")
    
    android.util.Log.e("WeatherIcon", "강수량 분석: precipitationValue=$precipitationValue, hasActualRain=$hasActualRain")
    
    val result = when {
        hasActualRain -> {
            android.util.Log.e("WeatherIcon", "비 아이콘 선택 (실제 강수량 >= 5mm)")
            "🌧️"
        }
        // 숫자 코드로 판단
        sky == "1" -> {
            val icon = if (hour >= 20 || hour < 6) "🌙" else "☀️"
            android.util.Log.e("WeatherIcon", "맑음 아이콘 선택: $icon (sky=1)")
            icon
        }
        sky == "3" -> {
            android.util.Log.e("WeatherIcon", "구름많음 아이콘 선택 (sky=3)")
            "⛅"
        }
        sky == "4" -> {
            android.util.Log.e("WeatherIcon", "흐림 아이콘 선택 (sky=4)")
            "☁️"
        }
        // 한글 텍스트로 판단
        sky.contains("맑음") -> {
            val icon = if (hour >= 20 || hour < 6) "🌙" else "☀️"
            android.util.Log.e("WeatherIcon", "맑음 아이콘 선택: $icon (sky='맑음' 텍스트)")
            icon
        }
        sky.contains("구름많음") || sky.contains("구름") -> {
            android.util.Log.e("WeatherIcon", "구름많음 아이콘 선택 (sky='구름많음' 텍스트)")
            "⛅"
        }
        sky.contains("흐림") -> {
            android.util.Log.e("WeatherIcon", "흐림 아이콘 선택 (sky='흐림' 텍스트)")
            "☁️"
        }
        sky.contains("비") || sky.contains("비") -> {
            android.util.Log.e("WeatherIcon", "비 아이콘 선택 (sky='비' 텍스트)")
            "🌧️"
        }
        else -> {
            val icon = if (hour >= 20 || hour < 6) "🌙" else "☀️"
            android.util.Log.e("WeatherIcon", "기본 아이콘 선택: $icon (sky='$sky'는 알 수 없는 값)")
            icon
        }
    }
    
    android.util.Log.e("WeatherIcon", "최종 아이콘: '$result'")
    return result
}

// 주간 날씨 컴포넌트
@Composable
private fun WeeklyWeatherContent(
    forecastWeather: List<com.dive.weatherwatch.data.BadaTimeForecastResponse> = emptyList()
) {
    // BadaTime forecast 데이터만 사용
    val weeklyData = if (forecastWeather.isNotEmpty()) {
        processBadaTimeForecastToWeeklyData(forecastWeather)
    } else {
        emptyList()
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 주간 날씨 제목
        Text(
            text = "주간 날씨",
            style = MaterialTheme.typography.title1.copy(
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // 일주일 날씨 리스트
        weeklyData.forEach { dailyData ->
            DailyWeatherRow(dailyData)
        }
    }
}

// 현대적이고 멋진 천체 정보 화면
@Composable
private fun CelestialInfoContent(tideData: BadaTimeTideResponse? = null) {
    // 일출/일몰, 월출/월몰 시간 파싱 (실제 API 데이터 사용) - 디버깅 로그 추가
    android.util.Log.d("CelestialInfo", "🌅 tideData = $tideData")
    android.util.Log.d("CelestialInfo", "🌅 sunRiseSet = '${tideData?.sunRiseSet}'")
    android.util.Log.d("CelestialInfo", "🌙 moonRiseSet = '${tideData?.moonRiseSet}'")
    
    val (sunriseTime, sunsetTime) = parseSunMoonTime(tideData?.sunRiseSet ?: "06:00/18:00")
    val (moonriseTime, moonsetTime) = parseSunMoonTime(tideData?.moonRiseSet ?: "19:00/07:00")
    
    android.util.Log.d("CelestialInfo", "🌅 파싱된 sunrise=$sunriseTime, sunset=$sunsetTime")
    android.util.Log.d("CelestialInfo", "🌙 파싱된 moonrise=$moonriseTime, moonset=$moonsetTime")
    
    // 현재 시간을 기준으로 애니메이션 계산
    val currentTime = remember { 
        val calendar = Calendar.getInstance()
        calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 제목
        Text(
            text = "일출/일몰",
            style = MaterialTheme.typography.title1.copy(
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 0.dp) // 16dp → 8dp로 간격 축소
        )
        
        // 태양 궤도 차트
        SunMoonOrbitChart(
            sunriseTime = sunriseTime,
            sunsetTime = sunsetTime,
            currentTime = currentTime,
            celestialType = CelestialType.SUN
        )
        
        Text(
            text = "월출/월몰",
            style = MaterialTheme.typography.title1.copy(
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier
                .padding(bottom = 0.dp)
                .offset(y = (-120).dp)
        )
        
        // 달 궤도 차트
        SunMoonOrbitChart(
            sunriseTime = moonriseTime,
            sunsetTime = moonsetTime,
            currentTime = currentTime,
            celestialType = CelestialType.MOON,
            modifier = Modifier.offset(y = (-120).dp)
        )
    }
}

// 천체 타입 enum
enum class CelestialType(val emoji: String, val color: Color) {
    SUN("☀️", Color(0xFFFFD700)),
    MOON("🌙", Color(0xFFE6E6FA))
}

// 현대적인 반원형 궤도 차트
@Composable
private fun SunMoonOrbitChart(
    sunriseTime: Int, // 분 단위
    sunsetTime: Int,  // 분 단위
    currentTime: Int, // 분 단위
    celestialType: CelestialType,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = calculateCelestialProgress(sunriseTime, sunsetTime, currentTime),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "CelestialProgress"
    )
    
    Box(
        modifier = modifier
            .size(280.dp)  // 200dp → 280dp로 천체 크기 대폭 증가
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            
            // 왼쪽 상단: 올라가는 화살표 (↗) - 크기 조정
            val arrowSize = 10.dp.toPx() // 40dp → 30dp (헤드 더 얇게)
            val arrowLength = 50.dp.toPx() // 60dp → 50dp (길이 조금 줄임)
            val leftTopArrowCenter = Offset(center.x - 60.dp.toPx(), center.y - 120.dp.toPx()) // -100dp → -120dp (20dp 위로)
            
            // 올라가는 화살표 그리기 (더 길게)
            val upArrowPath = Path().apply {
                // 화살표 머리
                moveTo(leftTopArrowCenter.x - arrowSize/2, leftTopArrowCenter.y + arrowSize/3)
                lineTo(leftTopArrowCenter.x, leftTopArrowCenter.y - arrowSize/3)
                lineTo(leftTopArrowCenter.x + arrowSize/2, leftTopArrowCenter.y + arrowSize/3)
                // 긴 화살표 꼬리
                moveTo(leftTopArrowCenter.x, leftTopArrowCenter.y - arrowSize/3)
                lineTo(leftTopArrowCenter.x, leftTopArrowCenter.y + arrowLength)
            }
            drawPath(
                path = upArrowPath,
                color = Color.White,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            
            // 오른쪽 하단: 내려가는 화살표 (↘)
            val rightBottomArrowCenter = Offset(center.x + 60.dp.toPx(), center.y + 0.dp.toPx()) // -20dp → +0dp (20dp 아래로)
            
            // 내려가는 화살표 그리기 (더 길게)
            val downArrowPath = Path().apply {
                // 화살표 머리
                moveTo(rightBottomArrowCenter.x - arrowSize/2, rightBottomArrowCenter.y - arrowSize/3)
                lineTo(rightBottomArrowCenter.x, rightBottomArrowCenter.y + arrowSize/3)
                lineTo(rightBottomArrowCenter.x + arrowSize/2, rightBottomArrowCenter.y - arrowSize/3)
                // 긴 화살표 꼬리
                moveTo(rightBottomArrowCenter.x, rightBottomArrowCenter.y + arrowSize/3)
                lineTo(rightBottomArrowCenter.x, rightBottomArrowCenter.y - arrowLength)
            }
            drawPath(
                path = downArrowPath,
                color = Color.White,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        
        // 중앙 상단: 일출 시간 (태양 위쪽 박스 위치)
        Text(
            text = formatTime(sunriseTime),
            style = MaterialTheme.typography.body1.copy(
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.align(Alignment.Center).offset(y = -110.dp) // 태양 위쪽
        )
        Text(
            text = if (celestialType == CelestialType.SUN) "일출" else "월출",
            style = MaterialTheme.typography.body2.copy(
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp
            ),
            modifier = Modifier.align(Alignment.Center).offset(y = -92.dp) // 시간 아래
        )
        
        // 중앙 하단: 일몰 시간 (태양 아래쪽 박스 위치)
        Text(
            text = formatTime(sunsetTime),
            style = MaterialTheme.typography.body1.copy(
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.align(Alignment.Center).offset(y = -20.dp) // 40dp → -20dp (일출과 대칭적으로)
        )
        Text(
            text = if (celestialType == CelestialType.SUN) "일몰" else "월몰",
            style = MaterialTheme.typography.body2.copy(
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp
            ),
            modifier = Modifier.align(Alignment.Center).offset(y = -2.dp) // 60dp → 0dp (일출과 대칭적으로)
        )
        
        // 중앙에 천체 이모지 (위로 이동)
        Text(
            text = celestialType.emoji,
            fontSize = 40.sp, // 24sp → 40sp로 대폭 증가
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -60.dp) // -20dp → -60dp (40dp 더 위로)
        )
    }
}

// 천체 진행률 계산 (0.0 ~ 1.0)
private fun calculateCelestialProgress(sunrise: Int, sunset: Int, current: Int): Float {
    return when {
        current < sunrise -> 0f // 아직 뜨지 않음
        current > sunset -> 1f  // 이미 짐
        else -> {
            val totalDuration = sunset - sunrise
            val elapsed = current - sunrise
            (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
        }
    }
}

// 시간 파싱 함수 ("05:51/19:00" -> (351, 1140))
private fun parseSunMoonTime(timeString: String): Pair<Int, Int> {
    return try {
        val parts = timeString.split("/")
        val rise = parts[0].split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        val set = parts[1].split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        rise to set
    } catch (e: Exception) {
        360 to 1080 // 기본값: 06:00 ~ 18:00
    }
}

// 분을 시:분 형태로 변환
private fun formatTime(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return String.format("%02d:%02d", hours, mins)
}

// 일일 날씨 행 컴포넌트
@Composable
private fun DailyWeatherRow(dailyData: DailyWeatherData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 요일명
        Text(
            text = dailyData.dayName,
            color = Color.White,
            fontSize = 11.sp,
            modifier = Modifier.width(30.dp)
        )
        
        // 날씨 아이콘들 (낮/밤)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = dailyData.dayIcon,
                fontSize = 16.sp
            )
            Text(
                text = dailyData.nightIcon,
                fontSize = 16.sp
            )
        }
        
        // 강수확률
        Text(
            text = dailyData.rainProbability,
            color = Color(0xFF87CEEB), // 하늘색
            fontSize = 9.sp,
            modifier = Modifier.width(25.dp)
        )
        
        // 온도 범위
        Text(
            text = "${if (dailyData.minTemp % 1.0 == 0.0) dailyData.minTemp.toInt() else String.format("%.1f", dailyData.minTemp)}° ${if (dailyData.maxTemp % 1.0 == 0.0) dailyData.maxTemp.toInt() else String.format("%.1f", dailyData.maxTemp)}°",
            color = Color.White,
            fontSize = 10.sp
        )
    }
}


// 데이터가 없을 때 사용할 fallback 데이터
private fun getFallbackWeeklyData(): List<DailyWeatherData> {
    val calendar = Calendar.getInstance()
    val dayFormat = SimpleDateFormat("E", Locale.KOREAN)
    val fallbackData = mutableListOf<DailyWeatherData>()
    
    for (i in 0 until 7) {
        val dayName = when (i) {
            0 -> "오늘"
            1 -> "내일"
            else -> dayFormat.format(calendar.time)
        }
        
        // 기본적인 날씨 패턴으로 생성
        val baseTemp = 20 + (Math.random() * 10).toInt() - 5 // 15-25도 범위
        val rainProb = (Math.random() * 60).toInt() // 0-60% 범위
        
        fallbackData.add(
            DailyWeatherData(
                dayName = dayName,
                dayIcon = if (rainProb > 30) "⛅" else "☀️",
                nightIcon = if (rainProb > 40) "🌧️" else "🌙",
                rainProbability = "${rainProb}%",
                minTemp = (baseTemp - 5).toDouble(),
                maxTemp = (baseTemp + 5).toDouble()
            )
        )
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
    }
    
    return fallbackData
}

// 인라인 시간별 날씨 컴포넌트 (메인 스크롤 안에서 사용)
@Composable
private fun InlineHourlyWeatherContent(
    forecastWeather: List<com.dive.weatherwatch.data.BadaTimeForecastResponse> = emptyList(),
    currentWeather: com.dive.weatherwatch.data.BadaTimeCurrentResponse? = null
) {
    android.util.Log.e("InlineHourlyWeather", "=== InlineHourlyWeatherContent 시작 ===")
    android.util.Log.e("InlineHourlyWeather", "forecastWeather size: ${forecastWeather.size}")
    android.util.Log.e("InlineHourlyWeather", "currentWeather available: ${currentWeather != null}")
    if (currentWeather != null) {
        android.util.Log.e("InlineHourlyWeather", "currentWeather temp: ${currentWeather.temp}, sky: ${currentWeather.sky}, skyCode: ${currentWeather.skyCode}")
    }
    
    // BadaTime 예측 데이터만 사용
    val hourlyData = if (forecastWeather.isNotEmpty()) {
        android.util.Log.e("InlineHourlyWeather", "BadaTime forecast 데이터 사용 - 혼합 데이터 함수 호출")
        processMixedCurrentAndForecastData(forecastWeather, currentWeather)
    } else {
        android.util.Log.e("InlineHourlyWeather", "BadaTime forecast 데이터가 없음")
        emptyList()
    }
    
    android.util.Log.e("InlineHourlyWeather", "처리된 hourlyData size: ${hourlyData.size}")
    hourlyData.forEachIndexed { index, data ->
        android.util.Log.e("InlineHourlyWeather", "[$index] 시간: ${data.time}, 온도: ${data.temperature}°")
    }
    
    if (hourlyData.isNotEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 온도 그래프 (더 작은 크기)
            InlineHourlyTemperatureChart(hourlyData)
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // 시간별 정보 (시간, 아이콘, 강수량) - 더 컴팩트하게
            InlineHourlyWeatherDetails(hourlyData)
        }
    } else {
        // 데이터가 없을 때
        Text(
            text = "시간별 날씨 데이터 로딩 중...",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.padding(16.dp)
        )
    }
}

// 인라인용 온도 차트 (더 작은 버전)
@Composable
private fun InlineHourlyTemperatureChart(hourlyData: List<HourlyWeatherData>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp) // 차트 높이를 늘려서 시각적 차이 확대
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            if (hourlyData.isEmpty()) return@Canvas
            
            val temperatures = hourlyData.map { it.temperature }
            val minTemp = temperatures.minOrNull() ?: 0.0
            val maxTemp = temperatures.maxOrNull() ?: 0.0
            
            android.util.Log.e("TempChart", "=== 온도 차트 디버깅 ===")
            android.util.Log.e("TempChart", "온도들: $temperatures")
            android.util.Log.e("TempChart", "minTemp: $minTemp, maxTemp: $maxTemp")
            
            // 극단적인 범위 설정으로 테스트 (확실한 차이를 보기 위해)
            val actualRange = maxTemp - minTemp
            val tempRange = if (actualRange < 0.1) 20.0 else kotlin.math.max(actualRange, 15.0) 
            val adjustedMinTemp = if (actualRange < 0.1) minTemp - 10.0 else minTemp - (tempRange - actualRange) / 2.0
            
            android.util.Log.e("TempChart", "tempRange: $tempRange, adjustedMinTemp: $adjustedMinTemp")
            
            val width = size.width
            val height = size.height
            
            // Row 레이아웃과 동일한 방식으로 X 위치 계산 (dp를 px로 변환)
            val density = this.density
            val itemWidth = 40.dp.toPx() // Column width를 px로 변환
            val spacing = 8.dp.toPx() // spacedBy 값을 원래대로
            val horizontalPadding = 6.dp.toPx() // Row padding을 px로 변환
            
            val totalItemsWidth = itemWidth * hourlyData.size
            val totalSpacingWidth = spacing * (hourlyData.size - 1)
            val totalContentWidth = totalItemsWidth + totalSpacingWidth
            val startX = (width - totalContentWidth) / 2f // CenterHorizontally 효과
            
            // 온도 점들 계산
            val chartHeight = height - 40f // 상하 여백
            val points = hourlyData.mapIndexed { index, data ->
                val x = startX + (itemWidth / 2f) + index * (itemWidth + spacing) // 각 아이템의 중앙
                val normalizedTemp = (data.temperature - adjustedMinTemp).toFloat() / tempRange.toFloat()
                val y = height - 20f - normalizedTemp * chartHeight
                
                android.util.Log.e("TempChart", "[$index] 온도: ${data.temperature}°")
                android.util.Log.e("TempChart", "  -> 정규화: $normalizedTemp (${data.temperature} - $adjustedMinTemp) / $tempRange")
                android.util.Log.e("TempChart", "  -> Y좌표: $y (height=$height, chartHeight=$chartHeight)")
                
                Offset(x, y)
            }
            
            // 선 그리기
            if (points.size > 1) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round) // 기존 3.dp에서 2.dp로 축소
                )
            }
            
            // 온도 점 그리기
            points.forEach { point ->
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(), // 기존 4.dp에서 3.dp로 축소
                    center = point
                )
            }
        }
        
        // 온도 텍스트 오버레이 - 더 작은 폰트
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally)
        ) {
            hourlyData.forEach { data ->
                Text(
                    text = "${if (data.temperature % 1.0 == 0.0) data.temperature.toInt() else String.format("%.1f", data.temperature)}°",
                    color = Color.White,
                    fontSize = 10.sp, // 기존 12.sp에서 10.sp로 축소
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

// 인라인용 시간별 상세 정보 (더 컴팩트)
@Composable
private fun InlineHourlyWeatherDetails(hourlyData: List<HourlyWeatherData>) {
    android.util.Log.e("InlineHourlyDetails", "표시할 hourlyData 개수: ${hourlyData.size}")
    hourlyData.forEachIndexed { index, data ->
        android.util.Log.e("InlineHourlyDetails", "[$index] 시간: ${data.time}, 온도: ${data.temperature}°, 강수량: ${data.precipitationAmount}mm")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        hourlyData.forEach { data ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(40.dp) // 기존 50.dp에서 40.dp로 축소
            ) {
                // 날씨 아이콘
                Text(
                    text = data.weatherIcon,
                    fontSize = 18.sp, // 기존 24.sp에서 20.sp로 축소
                    modifier = Modifier.padding(vertical = 3.dp) // 기존 4.dp에서 3.dp로 축소
                )
                
                // 시간
                Text(
                    text = data.time,
                    color = Color.White,
                    fontSize = 10.sp, // 기존 12.sp에서 10.sp로 축소
                    fontWeight = FontWeight.Medium
                )
                
                // 강수량
                Text(
                    text = "${data.precipitationAmount}mm",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 8.sp, // 기존 10.sp에서 8.sp로 축소
                )
            }
        }
    }
}

// 스와이프 제스처가 추가된 메인 컨텐츠 래퍼
@Composable
private fun WeatherMainContentWithSwipe(
    weatherData: com.dive.weatherwatch.data.WeatherResponse?,
    locationName: String?,
    isLoading: Boolean,
    errorMessage: String?,
    latitude: Double?,
    longitude: Double?,
    features: List<FeatureItem>,
    selectedFeatureIndex: Int?,
    onFeatureSelected: (Int?) -> Unit,
    onFeatureSelectionClear: () -> Unit,
    onNavigateToHeartRate: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToTide: () -> Unit,
    onNavigateToFishingPoint: () -> Unit,
    onNavigateToCompass: () -> Unit = {},
    onNavigateToTrapLocation: () -> Unit = {},
    onSwipeToHourly: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { 
                        // 드래그 종료 시에만 처리 (탭 제스처와 구별)
                    }
                ) { change, dragAmount ->
                    // 아래쪽으로 스와이프 감지 (더 민감하게)
                    if (dragAmount.y > 30f && kotlin.math.abs(dragAmount.x) < kotlin.math.abs(dragAmount.y) * 2) {
                        onSwipeToHourly()
                        change.consume() // 제스처 소비해서 중복 호출 방지
                    }
                }
            }
    ) {
        // 기존 WeatherMainContent 사용
        WeatherMainContent(
            locationName = locationName,
            isLoading = isLoading,
            errorMessage = errorMessage,
            latitude = latitude,
            longitude = longitude,
            features = features,
            selectedFeatureIndex = selectedFeatureIndex,
            onFeatureSelected = onFeatureSelected,
            onFeatureSelectionClear = onFeatureSelectionClear,
            onNavigateToHeartRate = onNavigateToHeartRate,
            onNavigateToChat = onNavigateToChat,
            onNavigateToTide = onNavigateToTide,
            onNavigateToFishingPoint = onNavigateToFishingPoint,
            onNavigateToTrapLocation = onNavigateToTrapLocation,
            onNavigateToCompass = onNavigateToCompass,
            onSwipeToHourly = onSwipeToHourly,
            // BadaTime 데이터 기본값
            currentWeather = null,
            forecastWeather = emptyList(),
            badaTimeLoading = false,
            badaTimeError = null,
            waterTemperature = null
        )
    }
}

@Composable
private fun AnimatedLoadingText(
    baseText: String,
    style: androidx.compose.ui.text.TextStyle,
    textAlign: TextAlign = TextAlign.Start,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_dots")
    
    val animationValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dots_animation"
    )
    
    val activeDotsCount = animationValue.toInt().coerceIn(0, 3)
    
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = baseText,
            style = style,
            textAlign = textAlign
        )
        
        repeat(3) { index ->
            val alpha = if (index < activeDotsCount) 1f else 0.2f
            Text(
                text = ".",
                style = style.copy(color = style.color.copy(alpha = alpha)),
                textAlign = textAlign
            )
        }
    }
}

private fun vibrate(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(120)
    }
}

// 🌟 프리미엄 파티클 시스템 (단순화된 버전)
@Composable
private fun PremiumParticleSystem() {
    val particles = remember {
        (0..10).map {
            Particle(
                x = Random.nextFloat() * 400f,
                y = Random.nextFloat() * 400f,
                speed = 0.2f,
                size = 2f,
                alpha = 0.3f
            )
        }
    }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            drawCircle(
                color = Color.White.copy(alpha = particle.alpha),
                radius = particle.size,
                center = Offset(particle.x % size.width, particle.y % size.height)
            )
        }
    }
}

// 🎭 글라스모피즘 배경
@Composable 
private fun GlassmorphismBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 그라데이션 블롭들
            val blobs = listOf(
                Blob(Offset(size.width * 0.2f, size.height * 0.3f), 80f, Color(0xFF667EEA).copy(alpha = 0.1f)),
                Blob(Offset(size.width * 0.8f, size.height * 0.7f), 100f, Color(0xFF764BA2).copy(alpha = 0.08f)),
                Blob(Offset(size.width * 0.6f, size.height * 0.2f), 60f, Color(0xFFF093FB).copy(alpha = 0.12f))
            )
            
            blobs.forEach { blob ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blob.color, Color.Transparent),
                        radius = blob.radius
                    ),
                    radius = blob.radius,
                    center = blob.center
                )
            }
        }
    }
}


// 데이터 클래스들
private data class Particle(
    val x: Float,
    val y: Float,
    val speed: Float,
    val size: Float,
    val alpha: Float
)

private data class Blob(
    val center: Offset,
    val radius: Float,
    val color: Color
)