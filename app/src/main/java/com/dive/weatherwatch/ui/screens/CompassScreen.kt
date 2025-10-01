package com.dive.weatherwatch.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dive.weatherwatch.R
import com.dive.weatherwatch.ui.theme.GalaxyWatchTypography
import com.dive.weatherwatch.ui.components.DynamicBackgroundOverlay
import com.dive.weatherwatch.ui.viewmodels.LocationViewModel
import kotlinx.coroutines.delay
import kotlin.math.*

// 임시 함수들 (FishingPointScreen과 중복 방지를 위해 이름 변경)
private fun compassCalculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371 // 지구 반지름 (km)
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}

private fun compassCalculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLon = Math.toRadians(lon2 - lon1)
    val lat1Rad = Math.toRadians(lat1)
    val lat2Rad = Math.toRadians(lat2)
    
    val y = sin(dLon) * cos(lat2Rad)
    val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)
    
    val bearing = atan2(y, x) * 180.0 / PI
    return (bearing + 360.0) % 360.0
}


@Composable
fun CompassScreen(
    onNavigateBack: () -> Unit = {},
    targetLat: Double? = null,
    targetLon: Double? = null,
    targetName: String? = null
) {
    val context = LocalContext.current
    val locationViewModel: LocationViewModel = viewModel()
    
    var azimuth by remember { mutableStateOf<Float?>(null) } // 초기값 null로 설정
    var isLoading by remember { mutableStateOf(true) }
    var hasCompassSensor by remember { mutableStateOf(false) }
    var hasReceivedFirstData by remember { mutableStateOf(false) } // 첫 데이터 수신 여부
    
    // 현재 위치 정보
    val currentLat by locationViewModel.latitude.collectAsState()
    val currentLon by locationViewModel.longitude.collectAsState()
    
    // 목표 지점 (고농도 엽록소 포인트 - 샘플 데이터)
    val finalTargetLat = targetLat ?: 35.1595 // 부산 해운대 근해
    val finalTargetLon = targetLon ?: 129.1615
    
    // 목표까지의 거리와 방위각 계산
    val targetDistance by remember {
        derivedStateOf {
            if (currentLat != null && currentLon != null) {
                compassCalculateDistance(currentLat!!, currentLon!!, finalTargetLat, finalTargetLon)
            } else null
        }
    }
    
    val targetBearing by remember {
        derivedStateOf {
            if (currentLat != null && currentLon != null) {
                compassCalculateBearing(currentLat!!, currentLon!!, finalTargetLat, finalTargetLon)
            } else null
        }
    }

    // 방향 계산 함수 (수정된 로직: 0도=북, 90도=동, 180도=남, 270도=서)
    fun getDirectionText(degrees: Float): String {
        val normalizedDegrees = ((degrees % 360f + 360f) % 360f)
        return when {
            normalizedDegrees >= 337.5f || normalizedDegrees < 22.5f -> "북"
            normalizedDegrees >= 22.5f && normalizedDegrees < 67.5f -> "동북"
            normalizedDegrees >= 67.5f && normalizedDegrees < 112.5f -> "동"
            normalizedDegrees >= 112.5f && normalizedDegrees < 157.5f -> "동남"
            normalizedDegrees >= 157.5f && normalizedDegrees < 202.5f -> "남"
            normalizedDegrees >= 202.5f && normalizedDegrees < 247.5f -> "서남"
            normalizedDegrees >= 247.5f && normalizedDegrees < 292.5f -> "서"
            normalizedDegrees >= 292.5f && normalizedDegrees < 337.5f -> "서북"
            else -> "북"
        }
    }

    // 스무딩을 위한 변수들
    var smoothedAzimuth by remember { mutableStateOf<Float?>(null) }
    var lastUpdateTime by remember { mutableLongStateOf(0L) }

    // 센서 이벤트 리스너 (prompt.txt 가이드에 따라 구현)
    val sensorEventListener = remember {
        object : SensorEventListener {
            private val accelerometerReading = FloatArray(3)
            private val magnetometerReading = FloatArray(3)
            private val rotationMatrix = FloatArray(9)
            private val orientationAngles = FloatArray(3)
            
            // 로우 패스 필터 상수 (0.85f로 빠른 반응과 부드러움의 균형)
            private val ALPHA = 0.85f

            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    when (it.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> {
                            System.arraycopy(it.values, 0, accelerometerReading, 0, accelerometerReading.size)
                        }
                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            System.arraycopy(it.values, 0, magnetometerReading, 0, magnetometerReading.size)
                        }
                    }

                    // 두 센서 데이터가 모두 준비되었을 때만 계산 (센서 퓨전)
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastUpdateTime > 20) { // 50Hz 업데이트 (더 빠른 반응)
                        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)) {
                            SensorManager.getOrientation(rotationMatrix, orientationAngles)
                            
                            // 방위각을 라디안에서 디그리로 변환
                            val azimuthInRadians = orientationAngles[0]
                            var azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()
                            
                            // 0-360도 범위로 정규화
                            azimuthInDegrees = (azimuthInDegrees + 360) % 360
                            
                            // 첫 데이터 수신 시 초기화
                            if (smoothedAzimuth == null) {
                                smoothedAzimuth = azimuthInDegrees
                                azimuth = azimuthInDegrees
                                hasReceivedFirstData = true
                                isLoading = false
                                Log.d("Compass", "첫 번째 방위각 데이터 수신: ${azimuthInDegrees.toInt()}°")
                            } else {
                                // 로우 패스 필터 적용 (스무딩)
                                val currentSmoothed = smoothedAzimuth!!
                                val angleDiff = azimuthInDegrees - currentSmoothed
                                val normalizedDiff = when {
                                    angleDiff > 180 -> angleDiff - 360
                                    angleDiff < -180 -> angleDiff + 360
                                    else -> angleDiff
                                }
                                
                                smoothedAzimuth = (currentSmoothed + normalizedDiff * (1 - ALPHA) + 360) % 360
                                azimuth = smoothedAzimuth
                            }
                            
                            lastUpdateTime = currentTime
                            
                            smoothedAzimuth?.let { 
                                Log.d("Compass", "Raw: ${azimuthInDegrees.toInt()}°, Smoothed: ${it.toInt()}° (${getDirectionText(it)})")
                            }
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                Log.d("Compass", "센서 정확도 변경: $accuracy")
            }
        }
    }

    // 센서 관리
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (accelerometer != null && magnetometer != null) {
            Log.d("Compass", "나침반 센서 발견!")
            hasCompassSensor = true
            sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
            sensorManager.registerListener(sensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_FASTEST)
        } else {
            Log.w("Compass", "나침반 센서 없음 - 시뮬레이션 모드")
            hasCompassSensor = false
        }

        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
            Log.d("Compass", "나침반 센서 해제")
        }
    }

    // 시뮬레이션 모드 (센서가 없을 때)
    LaunchedEffect(hasCompassSensor) {
        if (!hasCompassSensor) {
            delay(2000) // 로딩 시뮬레이션
            isLoading = false
            
            // 첫 데이터 시뮬레이션 (랜덤한 시작 각도)
            val startDirection = kotlin.random.Random.nextFloat() * 360f
            var time = 0f
            
            // 첫 번째 데이터로 초기화
            delay(1000) // 1초 후 첫 데이터 제공
            azimuth = startDirection
            hasReceivedFirstData = true
            isLoading = false
            Log.d("CompassSimulation", "첫 번째 시뮬레이션 데이터: ${startDirection.toInt()}°")
            
            // 이후 자연스러운 움직임
            while (true) {
                delay(20) // 50Hz 업데이트 (매우 빠른 반응)
                time += 0.02f
                
                // 자연스러운 나침반 흔들림 시뮬레이션
                val naturalShake = sin(time * 3f) * 2f + cos(time * 1.7f) * 1.5f
                val slowDrift = sin(time * 0.1f) * 30f // 천천한 방향 변화 (범위 확장)
                
                azimuth = (startDirection + slowDrift + naturalShake + 360f) % 360f
                
                // 로그로 확인
                azimuth?.let {
                    Log.d("CompassSimulation", "방위각: ${it.toInt()}° (${getDirectionText(it)})")
                }
            }
        }
    }

    // 바늘 투명도 애니메이션 (fade-in 효과)
    val needleAlpha by animateFloatAsState(
        targetValue = if (hasReceivedFirstData) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800, // 부드러운 fade-in
            easing = FastOutSlowInEasing
        ),
        label = "needle_alpha"
    )

    // 매우 빠른 회전 애니메이션 (즉시 반응)
    val animatedAzimuth by animateFloatAsState(
        targetValue = azimuth ?: 0f,
        animationSpec = tween(
            durationMillis = 50, // 매우 빠른 반응 (50ms)
            easing = LinearEasing // 선형 보간으로 자연스러운 움직임
        ),
        label = "azimuth_animation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .semantics {
                contentDescription = "나침반 화면. 현재 방향: ${azimuth?.let { "${getDirectionText(it)} ${it.toInt()}도" } ?: "측정 중"}. 터치하면 이전 화면으로 돌아갑니다."
                role = Role.Button
            }
            .clickable { onNavigateBack() }
    ) {
        // DynamicBackground 적용
        DynamicBackgroundOverlay()

        if (isLoading) {
            LoadingCompassUI()
        } else {
            WatchCompassLayout(
                azimuth = animatedAzimuth,
                direction = getDirectionText(animatedAzimuth),
                hasReceivedFirstData = hasReceivedFirstData,
                needleAlpha = needleAlpha,
                onBackClick = onNavigateBack,
                targetLat = finalTargetLat,
                targetLon = finalTargetLon,
                targetName = targetName,
                targetDistance = targetDistance,
                targetBearing = targetBearing,
                currentBearing = animatedAzimuth.toDouble()
            )
        }
    }
}

@Composable
fun LoadingCompassUI() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "나침반",
            style = GalaxyWatchTypography.LargeTitle.copy(
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 로딩 나침반 아이콘
        Canvas(
            modifier = Modifier
                .size(80.dp)
                .rotate(rotationAngle)
        ) {
            val center = size.center
            val radius = size.minDimension / 2

            // 외부 원
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // 나침반 바늘
            drawLine(
                color = Color.Red,
                start = center,
                end = Offset(center.x, center.y - radius * 0.7f),
                strokeWidth = 4.dp.toPx()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "나침반을 보정하는 중...",
            style = GalaxyWatchTypography.Body.copy(
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        )
    }
}

@Composable
fun WatchCompassLayout(
    azimuth: Float,
    direction: String,
    hasReceivedFirstData: Boolean,
    needleAlpha: Float,
    onBackClick: () -> Unit,
    targetLat: Double? = null,
    targetLon: Double? = null,
    targetName: String? = null,
    targetDistance: Double? = null,
    targetBearing: Double? = null,
    currentBearing: Double = 0.0
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 상단: 현재 나침반 방향과 목표 정보 표시
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .fillMaxWidth()
                .background(
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // 현재 방향
            Text(
                text = if (hasReceivedFirstData) "${azimuth.toInt()}° $direction" else "측정 중...",
                color = Color(0xFF00D4FF),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            // 목표까지의 거리와 방향 (실시간 업데이트 - 항상 표시)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 목표 거리 (실제 거리가 있으면 사용, 없으면 샘플 데이터)
                val displayDistance = targetDistance ?: 2.5 // 샘플 거리
                Text(
                    text = "목표: ${String.format("%.1f", displayDistance)}km",
                    color = Color(0xFF4CAF50),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // 목표 방향 (실제 방향이 있으면 사용, 없으면 현재 방위각 기준 상대적 샘플)
                val displayBearing = targetBearing ?: (azimuth + 90.0) % 360.0 // 현재 방향에서 90도 회전된 방향
                val targetDirection = when ((displayBearing / 45.0).toInt()) {
                    0, 8 -> "북"
                    1 -> "북동"
                    2 -> "동"
                    3 -> "남동"
                    4 -> "남"
                    5 -> "남서"
                    6 -> "서"
                    7 -> "북서"
                    else -> "북"
                }
                Text(
                    text = "${displayBearing.toInt()}° $targetDirection",
                    color = Color(0xFFFFD700),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // 메인: 현대적 나침반과 목표 화살표
        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(top = 60.dp), // 위쪽 텍스트와 충분한 간격 확보
            contentAlignment = Alignment.Center
        ) {
            // 기본 나침반
            ModernCompass(
                azimuth = azimuth,
                needleAlpha = needleAlpha,
                modifier = Modifier.fillMaxSize()
            )
            
            // 목표 방향 화살표 (별도 레이어)
            if (targetBearing != null) {

                TargetArrow(
                    azimuth = azimuth,
                    targetBearing = targetBearing,
                    needleAlpha = needleAlpha,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // 하단: 고농도 엽록소 포인트 정보 (파라미터가 있거나 샘플 데이터 표시)
        val showTargetInfo = targetLat != null && targetLon != null
        val displayLat = targetLat ?: 35.1595 // 샘플 데이터 (부산 해운대 근해)
        val displayLon = targetLon ?: 129.1615 // 샘플 데이터 (부산 해운대 근해)
        
        if (showTargetInfo || true) { // 항상 표시 (테스트용)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
                    .background(
                        color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp)
            ) {
                Text(
                    text = "🌊 목표: 고농도 엽록소",
                    color = Color(0xFF4CAF50),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${String.format("%.4f", displayLat)}, ${String.format("%.4f", displayLon)}",
                    color = Color.White,
                    fontSize = 8.sp
                )
            }
        }
    }
}

@Composable
fun WatchStyleCompass(
    azimuth: Float,
    direction: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 검은 원형 배경과 N,S,E,W 텍스트
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = size.center
            val radius = size.minDimension / 2 - 10.dp.toPx()
            
            // 검은 배경 원 (스크린샷과 동일)
            drawCircle(
                color = Color.Black,
                radius = radius,
                center = center
            )
            
            // 중앙에 노란 점
            drawCircle(
                color = Color(0xFFFFD700), // 노란색
                radius = 8.dp.toPx(),
                center = center
            )
            
            // N, S, E, W 텍스트 배치
            drawIntoCanvas { canvas ->
                val textPaint = Paint().asFrameworkPaint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 16.sp.toPx()
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                
                // N (상단)
                canvas.nativeCanvas.drawText("N", center.x, center.y - radius * 0.7f, textPaint)
                // S (하단)  
                canvas.nativeCanvas.drawText("S", center.x, center.y + radius * 0.8f, textPaint)
                // E (우측)
                canvas.nativeCanvas.drawText("E", center.x - radius * 0.7f, center.y + 5.dp.toPx(), textPaint)
                // W (좌측)
                canvas.nativeCanvas.drawText("W", center.x + radius * 0.7f, center.y + 5.dp.toPx(), textPaint)
            }
        }
        
        // 중앙 파란 방향 표시 상자 (스크린샷과 동일)
        Box(
            modifier = Modifier
                .size(70.dp, 35.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E88E5)) // 파란색
                .offset(y = (-10).dp), // 살짝 위로 이동
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = direction,
                style = GalaxyWatchTypography.LargeTitle.copy(
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
fun AppleWatchCompass(
    azimuth: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = size.center
        val radius = size.minDimension / 2 - 4.dp.toPx()
        
        // 외곽 흰색 테두리 (두껍게)
        drawCircle(
            color = Color.White,
            radius = radius,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )
        
        // 검은 배경 원
        drawCircle(
            color = Color.Black,
            radius = radius - 4.dp.toPx(),
            center = center
        )
        
        // 북쪽 방향 표시 (빨간 바늘 + 화살표) - prompt.txt의 rotate 적용
        rotate(-azimuth, center) { // UI 회전 방향과 센서 방향이 반대이므로 -를 붙임
            val needleLength = radius * 0.7f // 더 긴 바늘
            val arrowTip = Offset(center.x, center.y - needleLength)
            
            // 빨간 북쪽 바늘 (더 굵게)
            drawLine(
                color = Color.Red,
                start = center,
                end = arrowTip,
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            // 화살표 머리 부분 (더 뾰족하고 큰 삼각형)
            val arrowSize = 16.dp.toPx()
            val arrowWidth = 10.dp.toPx()
            val arrowPath = Path().apply {
                moveTo(arrowTip.x, arrowTip.y) // 화살표 끝점 (뾰족한 부분)
                lineTo(arrowTip.x - arrowWidth/2, arrowTip.y + arrowSize) // 왼쪽
                lineTo(arrowTip.x, arrowTip.y + arrowSize * 0.6f) // 중간 (더 뾰족하게)
                lineTo(arrowTip.x + arrowWidth/2, arrowTip.y + arrowSize) // 오른쪽
                close()
            }
            
            drawPath(
                path = arrowPath,
                color = Color.Red
            )
            
            // 흰색 남쪽 바늘 (더 굵게)
            drawLine(
                color = Color.White,
                start = center,
                end = Offset(center.x, center.y + radius * 0.5f),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        
        
        // 중앙 점 (더 크게)
        drawCircle(
            color = Color.White,
            radius = 8.dp.toPx(),
            center = center
        )
        drawCircle(
            color = Color.Black,
            radius = 4.dp.toPx(),
            center = center
        )
    }
}

@Composable
fun ModernCompass(
    azimuth: Float,
    needleAlpha: Float = 1f,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = size.center
        val radius = size.minDimension / 2 - 12.dp.toPx()
        
        
        // 메인 배경 원 (어두운 그라데이션)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF0A0A0A),
                    Color(0xFF1A1A1A),
                    Color.Black
                ),
                radius = radius
            ),
            radius = radius,
            center = center
        )
        
        // 내부 섬세한 테두리 (네온 효과)
        drawCircle(
            color = Color(0xFF00D4FF).copy(alpha = 0.3f),
            radius = radius - 2.dp.toPx(),
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        
        // 방향별 마크선 (현대적 스타일)
        for (i in 0..7) {
            val angle = i * 45f
            val angleRad = Math.toRadians(angle.toDouble())
            val isCardinal = i % 2 == 0 // N, E, S, W는 더 굵게
            
            val startRadius = if (isCardinal) radius - 25.dp.toPx() else radius - 15.dp.toPx()
            val endRadius = radius - 5.dp.toPx()
            val strokeWidth = if (isCardinal) 2.dp.toPx() else 1.dp.toPx()
            val color = if (isCardinal) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.4f)
            
            drawLine(
                color = color,
                start = Offset(
                    center.x + cos(angleRad - Math.PI/2).toFloat() * startRadius,
                    center.y + sin(angleRad - Math.PI/2).toFloat() * startRadius
                ),
                end = Offset(
                    center.x + cos(angleRad - Math.PI/2).toFloat() * endRadius,
                    center.y + sin(angleRad - Math.PI/2).toFloat() * endRadius
                ),
                strokeWidth = strokeWidth
            )
        }
        
        // 방향 표시 (N, E, S, W) - 현대적 폰트
        drawIntoCanvas { canvas ->
            val textPaint = Paint().asFrameworkPaint().apply {
                color = android.graphics.Color.WHITE
                textSize = 16.sp.toPx()
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = android.graphics.Paint.Align.CENTER
            }
            
            val textRadius = radius - 35.dp.toPx()
            // N (상단) - 네온 효과
            canvas.nativeCanvas.drawText("N", center.x, center.y - textRadius + 6.dp.toPx(), textPaint)
            // S (하단)
            canvas.nativeCanvas.drawText("S", center.x, center.y + textRadius + 6.dp.toPx(), textPaint)
            // E (우측)
            canvas.nativeCanvas.drawText("E", center.x + textRadius, center.y + 6.dp.toPx(), textPaint)
            // W (좌측)
            canvas.nativeCanvas.drawText("W", center.x - textRadius, center.y + 6.dp.toPx(), textPaint)
        }
        
        // 현대적 나침반 바늘 (투명도 적용)
        if (needleAlpha > 0f) {
            rotate(-azimuth, center) {
                val needleLength = radius * 0.75f
                val arrowTip = Offset(center.x, center.y - needleLength)
                
                // 메인 바늘 (그라데이션 효과 + 투명도)
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF0040).copy(alpha = needleAlpha),
                            Color(0xFFFF4080).copy(alpha = needleAlpha),
                            Color(0xFFFF0040).copy(alpha = needleAlpha)
                        ),
                        start = center,
                        end = arrowTip
                    ),
                    start = center,
                    end = arrowTip,
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                
                // 현대적 화살촉 (더 날카롭고 세련된 + 투명도)
                val arrowSize = 18.dp.toPx()
                val arrowWidth = 12.dp.toPx()
                val arrowPath = Path().apply {
                    moveTo(arrowTip.x, arrowTip.y) // 뾰족한 끝
                    lineTo(arrowTip.x - arrowWidth/2, arrowTip.y + arrowSize) // 왼쪽
                    lineTo(arrowTip.x, arrowTip.y + arrowSize * 0.4f) // 중간 홈
                    lineTo(arrowTip.x + arrowWidth/2, arrowTip.y + arrowSize) // 오른쪽
                    close()
                }
                
                drawPath(
                    path = arrowPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF0040).copy(alpha = needleAlpha),
                            Color(0xFFFF6090).copy(alpha = needleAlpha)
                        )
                    )
                )
                
                // 화살촉 테두리 (네온 효과 + 투명도)
                drawPath(
                    path = arrowPath,
                    color = Color(0xFF00D4FF).copy(alpha = 0.6f * needleAlpha),
                    style = Stroke(width = 1.dp.toPx())
                )
                
                // 남쪽 바늘 (더 세련되게 + 투명도)
                drawLine(
                    color = Color.White.copy(alpha = 0.7f * needleAlpha),
                    start = center,
                    end = Offset(center.x, center.y + radius * 0.6f),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        
        // 중앙 허브 (현대적 디자인)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF333333),
                    Color.Black,
                    Color(0xFF1A1A1A)
                )
            ),
            radius = 12.dp.toPx(),
            center = center
        )
        
        drawCircle(
            color = Color(0xFF00D4FF).copy(alpha = 0.8f),
            radius = 8.dp.toPx(),
            center = center
        )
        
        drawCircle(
            color = Color.Black,
            radius = 4.dp.toPx(),
            center = center
        )
    }
}

@Composable
fun TargetArrow(
    azimuth: Float,
    targetBearing: Double,
    needleAlpha: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = size.center
        val radius = size.minDimension / 2 - 12.dp.toPx()
        
        if (needleAlpha > 0f) {
            val targetAngle = (targetBearing - azimuth).toFloat()
            val arrowAlpha = needleAlpha * 0.8f
            
            rotate(targetAngle, center) {
                val targetArrowLength = radius * 0.6f
                val targetArrowTip = Offset(center.x, center.y - targetArrowLength)
                
                // 초록색 목표 방향 화살표
                drawLine(
                    color = Color(0xFF4CAF50).copy(alpha = arrowAlpha),
                    start = center,
                    end = targetArrowTip,
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                
                // 목표 화살표 머리 (삼각형)
                val targetArrowSize = 12.dp.toPx()
                val targetArrowWidth = 8.dp.toPx()
                val targetArrowPath = Path().apply {
                    moveTo(targetArrowTip.x, targetArrowTip.y)
                    lineTo(targetArrowTip.x - targetArrowWidth/2, targetArrowTip.y + targetArrowSize)
                    lineTo(targetArrowTip.x, targetArrowTip.y + targetArrowSize * 0.6f)
                    lineTo(targetArrowTip.x + targetArrowWidth/2, targetArrowTip.y + targetArrowSize)
                    close()
                }
                
                drawPath(
                    path = targetArrowPath,
                    color = Color(0xFF4CAF50).copy(alpha = arrowAlpha)
                )
                
                // 목표 화살표 끝에 작은 원점 (고농도 스팟 표시)
                drawCircle(
                    color = Color(0xFF4CAF50).copy(alpha = needleAlpha),
                    radius = 4.dp.toPx(),
                    center = targetArrowTip
                )
            }
        }
    }
}
