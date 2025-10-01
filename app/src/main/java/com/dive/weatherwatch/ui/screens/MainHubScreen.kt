package com.dive.weatherwatch.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import com.dive.weatherwatch.ui.navigation.WatchDestinations
import com.dive.weatherwatch.ui.theme.AppColors
import com.dive.weatherwatch.ui.theme.AppGradients
import com.dive.weatherwatch.ui.viewmodels.LocationViewModel
import com.dive.weatherwatch.ui.viewmodels.WeatherViewModel
import com.dive.weatherwatch.ui.components.DynamicBackgroundOverlay
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.Calendar

// Hub Item별 색상 테마 (시간 기반 적응)
private fun getHubItemColor(itemName: String): Color {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    
    // 밤(20-6시)과 낮(6-20시)에 따라 색상 조정
    val isNight = hour < 6 || hour >= 20
    
    // 디버깅을 위한 로그
    android.util.Log.d("HubItemColor", "HubItem: $itemName, Hour: $hour, IsNight: $isNight")
    
    return when (itemName) {
        "날씨" -> if (isNight) Color(0xFF8E9AAF) else Color(0xFFFFD700) // 낮엔 금색으로 테스트
        "심박수" -> if (isNight) Color(0xFFDDA0DD) else Color(0xFFFF1493) // 낮엔 진한 분홍
        "챗봇" -> if (isNight) Color(0xFFA8D1A8) else Color(0xFF32CD32) // 낮엔 라임그린
        "조위 정보" -> if (isNight) Color(0xFFB8A8E8) else Color(0xFF8A2BE2) // 낮엔 블루바이올렛
        "낚시 포인트" -> if (isNight) Color(0xFFE6B86B) else Color(0xFFFF8C00) // 낮엔 주황색
        "통발 추적" -> if (isNight) Color(0xFFCD853F) else Color(0xFFD2691E) // 낮엔 초콜릿색
        "나침반" -> if (isNight) Color(0xFF87CEEB) else Color(0xFF1E90FF) // 낮엔 파란색
        else -> if (isNight) Color(0xFF9BB5E8) else Color(0xFFFF4500) // 기본값은 오렌지레드
    }
}

data class HubItem(
    val icon: ImageVector,
    val name: String,
    val description: String,
    val destination: String
)

@Composable
fun MainHubScreen(
    onNavigateToWeather: () -> Unit,
    onNavigateToTide: () -> Unit,
    onNavigateToFishingPoint: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToHeartRate: () -> Unit,
    onNavigateToCompass: () -> Unit,
    onNavigateToTrapLocation: () -> Unit,
    locationViewModel: LocationViewModel = viewModel(),
    weatherViewModel: WeatherViewModel = viewModel()
) {
    val context = LocalContext.current
    val hubItems = remember {
        listOf(
            HubItem(Icons.Default.WbSunny, "날씨", "현재 위치의 날씨를 확인합니다.", WatchDestinations.WEATHER),
            HubItem(Icons.Default.Waves, "조위 정보", "실시간 조위 상황을 확인합니다.", WatchDestinations.TIDE),
            HubItem(Icons.Default.Place, "낚시 포인트", "주변 낚시 포인트를 찾습니다.", WatchDestinations.FISHING_POINT),
            HubItem(Icons.Default.Anchor, "통발 추적", "투하한 통발 위치를 추적합니다.", WatchDestinations.TRAP_LOCATION),
            HubItem(Icons.Default.Chat, "챗봇", "음성으로 AI와 대화합니다.", WatchDestinations.CHAT),
            HubItem(Icons.Default.Favorite, "심박수", "실시간 심박수를 측정합니다.", WatchDestinations.HEART_RATE),
            HubItem(Icons.Default.Navigation, "나침반", "실시간 방향과 각도를 확인합니다.", WatchDestinations.COMPASS)
        )
    }
    val navigationMap = remember {
        mapOf(
            WatchDestinations.WEATHER to onNavigateToWeather,
            WatchDestinations.TIDE to onNavigateToTide,
            WatchDestinations.FISHING_POINT to onNavigateToFishingPoint,
            WatchDestinations.TRAP_LOCATION to onNavigateToTrapLocation,
            WatchDestinations.CHAT to onNavigateToChat,
            WatchDestinations.HEART_RATE to onNavigateToHeartRate,
            WatchDestinations.COMPASS to onNavigateToCompass
        )
    }

    var hoveredItemIndex by remember { mutableStateOf<Int?>(null) }
    var selectedItemIndex by remember { mutableStateOf<Int?>(null) }
    var isNavigating by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val itemPositions = remember { mutableStateMapOf<Int, Offset>() }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var totalDragOffset by remember { mutableStateOf(0f) }
    
    val locationName by locationViewModel.locationName.collectAsState()
    val isLocationLoading by locationViewModel.isLocationLoading.collectAsState()

    // Start location fetch when screen loads
    LaunchedEffect(Unit) {
        locationViewModel.startLocationFetch(context, weatherViewModel)
    }

    // Haptic feedback effect
    LaunchedEffect(hoveredItemIndex) {
        if (hoveredItemIndex != null) {
            vibrate(context)
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { boxSize = it }
            .pointerInput(hubItems.size) {
                detectDragGestures(
                    onDragStart = { offset ->
                        totalDragOffset = 0f
                        selectedItemIndex = null
                        hoveredItemIndex = null
                    },
                    onDragEnd = {
                        selectedItemIndex?.let { index ->
                            // Trigger navigation animation
                            isNavigating = true
                        } ?: run {
                            // Reset states if no item selected
                            selectedItemIndex = null
                            hoveredItemIndex = null
                            totalDragOffset = 0f
                        }
                    },
                    onDragCancel = {
                        // Reset states on cancel
                        selectedItemIndex = null
                        hoveredItemIndex = null
                        totalDragOffset = 0f
                    }
                ) { change, dragAmount ->
                    // Use both vertical and horizontal drag for better touch recognition
                    val verticalDrag = dragAmount.y
                    val horizontalDrag = dragAmount.x
                    
                    // Prioritize vertical drag but allow slight horizontal movement
                    val effectiveDrag = if (kotlin.math.abs(verticalDrag) > kotlin.math.abs(horizontalDrag)) {
                        verticalDrag
                    } else {
                        // Allow horizontal drag with reduced sensitivity
                        horizontalDrag * 0.7f
                    }
                    
                    totalDragOffset += effectiveDrag
                    
                    // Original sensitivity restored
                    val sensitivity = 30f // Back to original value
                    val itemIndex = (totalDragOffset / sensitivity).toInt()
                    val clampedIndex = itemIndex.coerceIn(0, hubItems.size - 1)
                    
                    if (clampedIndex != hoveredItemIndex) {
                        hoveredItemIndex = clampedIndex
                        selectedItemIndex = clampedIndex
                    }
                    
                    change.consume()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Dynamic Background Overlay (시간 기반 배경 적용)
        DynamicBackgroundOverlay(
            weatherData = null,
            alpha = 0.7f,
            forceTimeBasedBackground = true // 시간만으로 배경 결정
        )
        
        val ringRadius = boxSize.width / 2 * 0.95f // Move closer to edge
        val center = Offset(boxSize.width / 2f, boxSize.height / 2f)

        // Modern thin bezel rings with clear segment separation
        Canvas(modifier = Modifier.fillMaxSize()) {
            val startAngle = -60f
            val totalAngleRange = 120f
            val gapAngle = 8f // Increased gap for better visual separation
            val numGaps = hubItems.size - 1
            val totalArcAngle = totalAngleRange - (gapAngle * numGaps)
            val segmentSweepAngle = totalArcAngle / hubItems.size

            hubItems.forEachIndexed { index, _ ->
                val segmentStartAngle = startAngle + (segmentSweepAngle + gapAngle) * index
                val isHovered = index == hoveredItemIndex
                
                // Modern thin design with subtle glow effect
                val baseColor = if (isHovered) Color.White else Color.Gray.copy(alpha = 0.4f)
                val strokeWidth = if (isHovered) 4.dp.toPx() else 2.5f.dp.toPx()
                
                // Outer glow for hovered state
                if (isHovered) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.3f),
                        startAngle = segmentStartAngle,
                        sweepAngle = segmentSweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth + 2.dp.toPx()),
                        size = Size(ringRadius * 2, ringRadius * 2),
                        topLeft = Offset(center.x - ringRadius, center.y - ringRadius)
                    )
                }
                
                // Main ring segment
                drawArc(
                    color = baseColor,
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

        // Calculate icon positions
        LaunchedEffect(boxSize) {
            if (boxSize != IntSize.Zero) {
                val startAngle = -60.0
                val totalAngleRange = 120.0
                val gapAngle = 8.0 // Match the increased gap
                val numGaps = hubItems.size - 1
                val totalArcAngle = totalAngleRange - (gapAngle * numGaps)
                val segmentSweepAngle = totalArcAngle / hubItems.size

                hubItems.forEachIndexed { index, _ ->
                    val segmentStartAngle = startAngle + (segmentSweepAngle + gapAngle) * index
                    val angle = segmentStartAngle + (segmentSweepAngle / 2)
                    val x = center.x + (ringRadius * cos(Math.toRadians(angle))).toFloat()
                    val y = center.y + (ringRadius * sin(Math.toRadians(angle))).toFloat()
                    itemPositions[index] = Offset(x, y)
                }
            }
        }

        // Blur Overlay and Feature Preview
        AnimatedVisibility(
            visible = hoveredItemIndex != null,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            val item = hoveredItemIndex?.let { hubItems[it] }
            if (item != null) {
                BlurOverlayFeaturePreview(
                    item = item,
                    shouldNavigate = isNavigating,
                    onNavigate = {
                        selectedItemIndex?.let { index ->
                            coroutineScope.launch {
                                // Reset states first to hide overlay
                                selectedItemIndex = null
                                hoveredItemIndex = null
                                isNavigating = false
                                totalDragOffset = 0f
                                
                                // Then navigate immediately
                                navigationMap[hubItems[index].destination]?.invoke()
                            }
                        }
                    }
                )
            }
        }
        
        AnimatedVisibility(visible = hoveredItemIndex == null, enter = fadeIn(), exit = fadeOut()) {
            FuturisticClockHub(
                locationName = locationName,
                isLocationLoading = isLocationLoading
            )
        }

        // Bezel Icons
        val density = LocalDensity.current
        hubItems.forEachIndexed { index, item ->
            itemPositions[index]?.let {
                val scale = animateFloatAsState(targetValue = if (index == hoveredItemIndex) 1.3f else 1f, label = "icon_scale").value
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.name,
                    tint = getHubItemColor(item.name),
                    modifier = Modifier
                        .offset(x = with(density) { it.x.toDp() } - 12.dp, y = with(density) { it.y.toDp() } - 12.dp)
                        .scale(scale)
                        .size(24.dp)
                        .then(
                            if (hoveredItemIndex != null && index != hoveredItemIndex) {
                                Modifier.alpha(0.3f).blur(radius = 2.dp)
                            } else Modifier
                        )
                )
            }
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

@Composable
private fun FuturisticClockHub(
    locationName: String?,
    isLocationLoading: Boolean
) {
    var currentTime by remember { mutableStateOf(Date()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            kotlinx.coroutines.delay(1000)
        }
    }
    
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("M월 d일 (E)", Locale.KOREAN)
    val secondFormat = SimpleDateFormat("ss", Locale.getDefault())
    
    val timeText = timeFormat.format(currentTime)
    val dateText = dateFormat.format(currentTime)
    val seconds = secondFormat.format(currentTime).toInt()
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(100.dp)
    ) {
        // Outer pulsing ring
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "pulse_alpha"
        )
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            
            // Outer glow ring
            drawCircle(
                color = AppColors.PrimaryLight.copy(alpha = pulseAlpha),
                radius = size.width / 2 * 0.9f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            
            // Inner progress ring (seconds)
            val sweepAngle = (seconds / 60f) * 360f
            drawArc(
                brush = AppGradients.secondsIndicator,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                size = androidx.compose.ui.geometry.Size(size.width * 0.7f, size.height * 0.7f),
                topLeft = Offset(center.x - size.width * 0.35f, center.y - size.height * 0.35f)
            )
        }
        
        // Central content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Modern digital time
            Text(
                text = timeText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            
            // Minimalist date
            Text(
                text = dateText,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Light
            )
            
            // Location status indicator - Using correct state variables
            LocationStatusIndicator(
                locationName = locationName,
                isLoading = isLocationLoading,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun BlurOverlayFeaturePreview(
    item: HubItem,
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
            title = item.name,
            description = item.description,
            isNavigating = isNavigating,
            onNavigationComplete = onNavigate
        )
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
        
        Spacer(modifier = Modifier.height(6.dp))
        
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
private fun LocationStatusIndicator(
    locationName: String?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            // Blinking GPS icon
            val infiniteTransition = rememberInfiniteTransition(label = "gps_blink")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "gps_alpha"
            )
            
            Text(
                text = "📍",
                fontSize = 10.sp,
                modifier = Modifier.alpha(alpha)
            )
            Text(
                text = "\n위치를 찾는 중",
                fontSize = 8.sp,
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center
            )
        } else if (locationName != null) {
            // Show location
            Text(
                text = "📍",
                fontSize = 8.sp,
                color = AppColors.AccentGreen
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = locationName,
                fontSize = 7.sp,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Light,
                maxLines = 1
            )
        }
    }
}
