package com.dive.weatherwatch.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.dive.weatherwatch.ui.components.DynamicBackgroundOverlay
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Text
import com.dive.weatherwatch.R
import com.dive.weatherwatch.ui.theme.AppColors
import com.dive.weatherwatch.ui.theme.AppGradients
import com.dive.weatherwatch.ui.theme.GalaxyWatchTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun ThirdWatchScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var heartRate by remember { mutableFloatStateOf(0f) }  // 초기값을 0으로 설정
    val heartRateHistory = remember { mutableStateListOf<Float>() }
    var lastUpdateTime by remember { mutableLongStateOf(0L) }
    var showEmergencyAlert by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(3) }
    var isLoading by remember { mutableStateOf(true) }  // 로딩 상태 추가
    var stableReadingsCount by remember { mutableIntStateOf(0) }  // 안정적인 측정 횟수
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) Log.d("HeartRate", "Body sensors permission granted")
        else Log.d("HeartRate", "Body sensors permission denied")
    }

    val sensorEventListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_HEART_RATE && it.values.isNotEmpty()) {
                        val newHeartRate = it.values[0]
                        
                        // 센서 값이 유효한 범위인지 확인 (20-220 BPM으로 확장)
                        if (newHeartRate in 20f..220f) {
                            // 안정적인 측정을 위해 최소 3회 연속 정상값 필요
                            stableReadingsCount++
                            
                            if (stableReadingsCount >= 3) {
                                isLoading = false  // 로딩 완료
                                heartRate = newHeartRate
                                
                                if (heartRateHistory.size > 30) {
                                    heartRateHistory.removeAt(0)
                                }
                                heartRateHistory.add(newHeartRate)
                                lastUpdateTime = System.currentTimeMillis()
                                
                                Log.d("HeartRate", "안정화된 심박수: $newHeartRate BPM")

                                // 긴급상황 감지 (40 미만)
                                if (newHeartRate < 40) {
                                    showEmergencyAlert = true
                                    Log.w("HeartRate", "Emergency: Heart rate below 40 BPM!")
                                }
                            } else {
                                Log.d("HeartRate", "센서 안정화 중... ($stableReadingsCount/3): $newHeartRate BPM")
                            }
                        } else {
                            // 비정상 값이면 카운터 리셋
                            stableReadingsCount = 0
                            Log.w("HeartRate", "비정상 심박수 값 감지: $newHeartRate BPM - 무시됨")
                        }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                Log.d("HeartRate", "Sensor accuracy changed: $accuracy")
            }
        }
    }

    // 실제 심박수 센서 사용
    // 센서가 없을 때 더미 데이터 시뮬레이션
    LaunchedEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        
        if (heartRateSensor == null) {
            Log.d("HeartRate", "센서 없음 - 더미 데이터 시뮬레이션 시작")
            
            // 로딩 시뮬레이션 (2초 대기)
            delay(2000)
            isLoading = false
            
            // 초기 데이터 설정
            heartRateHistory.clear()
            heartRateHistory.addAll(listOf(70f, 72f, 75f, 78f, 82f, 79f, 76f, 74f, 73f, 75f))
            heartRate = 75f
            
            // 실시간 더미 데이터 생성 (긴급상황 테스트 포함)
            while (true) {
                delay(1000) // 1초마다 업데이트 (더 현실적으로)
                val baseRate = 75f
                val variation = kotlin.random.Random.nextFloat() * 10f - 5f // -5 ~ +5 변동 (더 안정적)
                // 테스트를 위해 낮은 확률로 40 미만 값 생성
                val newRate = if (kotlin.random.Random.nextFloat() < 0.05f) { // 5% 확률
                    kotlin.random.Random.nextFloat() * 10f + 25f // 25-35 BPM
                } else {
                    (baseRate + variation).coerceIn(65f, 85f)
                }
                
                heartRate = newRate
                if (heartRateHistory.size >= 20) {
                    heartRateHistory.removeAt(0)
                }
                heartRateHistory.add(newRate)
                lastUpdateTime = System.currentTimeMillis()
                Log.d("HeartRate", "더미 심박수 업데이트: $newRate")
            }
        }
    }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        if (heartRateSensor != null) {
            Log.d("HeartRate", "심박수 센서 발견!")
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED) {
                sensorManager.registerListener(sensorEventListener, heartRateSensor, SensorManager.SENSOR_DELAY_FASTEST)
                Log.d("HeartRate", "심박수 모니터링 시작")
            } else {
                Log.w("HeartRate", "BODY_SENSORS 권한 필요")
                permissionLauncher.launch(Manifest.permission.BODY_SENSORS)
            }
        }

        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
            Log.d("HeartRate", "심박수 모니터링 중지")
        }
    }

    val heartScale = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            val duration = if (heartRate > 0) {
                (60000 / heartRate).toInt().coerceIn(300, 1500)
            } else {
                800 // 기본 애니메이션 속도
            }
            heartScale.animateTo(1.3f, tween(duration / 2))
            heartScale.animateTo(1f, tween(duration / 2))
        }
    }

    fun makeEmergencyCall() {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:119") }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("HeartRate", "Failed to make emergency call: ${e.message}")
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:119") }
                context.startActivity(dialIntent)
            } catch (e2: Exception) {
                Log.e("HeartRate", "Failed to open dialer: ${e2.message}")
            }
        }
    }

    LaunchedEffect(showEmergencyAlert) {
        if (showEmergencyAlert) {
            countdown = 3
            repeat(3) { index ->
                delay(1000)
                countdown = 3 - index - 1
            }
            makeEmergencyCall()
            Log.d("HeartRate", "Emergency call initiated.")
            showEmergencyAlert = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .semantics {
                contentDescription = "심박수 화면. 현재 심박수: ${if (heartRate > 0) "${heartRate.toInt()}BPM" else "측정 중"}. 화면 가장자리를 터치하면 이전 화면으로 돌아갑니다."
                role = Role.Button
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        // 화면 가장자리 터치 시 뒤로 가기
                        if (offset.x < size.width * 0.2f || offset.x > size.width * 0.8f ||
                            offset.y < size.height * 0.2f || offset.y > size.height * 0.8f) {
                            onNavigateBack()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 하트 배경 이미지 (기존 상태로 복구)
        Image(
            painter = painterResource(id = R.mipmap.heart_background),
            contentDescription = "Heart Background",
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.25f), // 25% 불투명도로 배경이 보이면서도 UI가 가려지지 않게
            contentScale = ContentScale.Crop // 화면을 꽉 채우도록
        )
        if (showEmergencyAlert) {
            EmergencyUI(heartRate = heartRate, countdown = countdown)
        } else if (isLoading) {
            LoadingHeartRateUI()
        } else {
            ModernMonitoringUI(
                heartRate = heartRate, 
                heartScale = heartScale.value, 
                history = heartRateHistory.toList(), // 리스트 복사로 전달
                heartCardGradient = AppGradients.heartRateCard
            )
        }
    }
}

@Composable
fun LoadingHeartRateUI() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // 심박수 모니터링 제목
        Text(
            text = "심박수 모니터링",
            style = androidx.wear.compose.material.MaterialTheme.typography.body1.copy(
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(
                    color = Color.Black,
                    offset = Offset(2f, 2f),
                    blurRadius = 4f
                )
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 로딩 애니메이션 (심박수 처럼 보이는 점들)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                val delay = index * 200
                val animatedAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = EaseInOut, delayMillis = delay),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot$index"
                )
                
                Canvas(
                    modifier = Modifier.size(12.dp)
                ) {
                    drawCircle(
                        color = Color(0xFFE57373).copy(alpha = animatedAlpha),
                        radius = size.width / 2
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 로딩 메시지
        Text(
            text = "심박수를 불러오는 중...",
            style = androidx.wear.compose.material.MaterialTheme.typography.body2.copy(
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "손목에 워치를 밀착시켜 주세요",
            style = androidx.wear.compose.material.MaterialTheme.typography.body2.copy(
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun ModernMonitoringUI(
    heartRate: Float, 
    heartScale: Float, 
    history: List<Float>,
    heartCardGradient: Brush
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),  // 간격을 0dp로 더 줄임
        modifier = Modifier.padding(top = 0.dp)  // 상단 간격 더 줄이기
    ) {
        // 심박수 모니터링 텍스트를 상단으로 이동
        Text(
            text = "심박수 모니터링",
            style = androidx.wear.compose.material.MaterialTheme.typography.body1.copy(
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(
                    color = Color.Black,
                    offset = Offset(2f, 2f),
                    blurRadius = 4f
                )
            ),
            modifier = Modifier
                .padding(top = 0.dp, bottom = 0.dp)
                .offset(y = (0).dp)  // 텍스트를 상단으로 크게 올림
        )
        // 심박수 수치 표시를 중간으로 위치
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = (10).dp)  // 중간 위치로 조정
        ) {
            if (heartRate > 0) {
                // 심박수 숫자 애니메이션과 펄스 효과
                val animatedHeartRate = remember { Animatable(heartRate) }
                val pulseScale = remember { Animatable(1f) }
                
                LaunchedEffect(heartRate) {
                    // 숫자 변화 애니메이션
                    animatedHeartRate.animateTo(
                        heartRate,
                        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                    )
                    
                    // 펄스 효과 (심박수가 변할 때마다)
                    pulseScale.animateTo(1.1f, tween(150))
                    pulseScale.animateTo(1f, tween(200))
                }
                
                Text(
                    text = "${animatedHeartRate.value.toInt()}",
                    style = androidx.wear.compose.material.MaterialTheme.typography.display1.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(3f, 3f),
                            blurRadius = 6f
                        )
                    ),
                    modifier = Modifier
                        .offset(y = (0).dp)
                        .graphicsLayer(
                            scaleX = pulseScale.value,
                            scaleY = pulseScale.value
                        )
                )
                Text(
                    text = "bpm",
                    style = androidx.wear.compose.material.MaterialTheme.typography.body2.copy(
                        fontSize = 14.sp,  // 더 크게 만들기
                        color = Color.White.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.offset(y = (0).dp)  // bpm을 숫자 가까이
                )
            } else {
                Text(
                    text = "--",
                    style = androidx.wear.compose.material.MaterialTheme.typography.display1.copy(
                        fontSize = 32.sp,  // 더 크게 만들기
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier.offset(y = (-25).dp)  // "--"도 위로 25dp 이동
                )
            }
        }

        if (history.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(60.dp)  // 적당한 높이로 조정
                    .offset(y = (20).dp)  // 그래프를 위로 40dp 이동
                    .shadow(8.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(4.dp)  // 패딩 줄임
            ) {
                ModernHeartRateGraph(
                    data = history.toList(),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (heartRate > 0) {
            val (status, statusColor) = when {
                heartRate < 40 -> "위험" to Color(0xFFFF6B6B)
                heartRate < 60 -> "낮음" to Color(0xFFFFAB40)
                heartRate > 100 -> "높음" to Color(0xFFFF6B6B)
                else -> "정상" to Color(0xFF4CAF50)
            }
            
            // 상태 변화 애니메이션 (간단한 버전)
            val animatedScale = remember { Animatable(1f) }
            
            LaunchedEffect(status) {
                // 상태가 변경될 때 스케일 애니메이션
                animatedScale.animateTo(1.2f, tween(200))
                animatedScale.animateTo(1f, tween(300))
            }
            
            Box(
                modifier = Modifier
                    .offset(y = (20).dp)  // 상태 박스를 적당한 위치로
                    .graphicsLayer(
                        scaleX = animatedScale.value,
                        scaleY = animatedScale.value
                    )
                    .shadow(4.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(statusColor.copy(alpha = 0.2f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = status,
                    style = androidx.wear.compose.material.MaterialTheme.typography.body1.copy(
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
fun EmergencyUI(heartRate: Float, countdown: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(text = "🚨", style = GalaxyWatchTypography.LargeTitle.copy(fontSize = 48.sp))
        Text(text = "응급상황!", style = GalaxyWatchTypography.LargeTitle.copy(fontSize = 24.sp, color = Color.Red, fontWeight = FontWeight.Bold))
        Text(text = "${heartRate.toInt()} BPM", style = GalaxyWatchTypography.LargeTitle.copy(fontSize = 32.sp, color = Color.Red))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (countdown > 0) "${countdown}초 후 119 자동 연결" else "119 연결 중...",
            style = GalaxyWatchTypography.Body.copy(
                color = if (countdown > 0) AppColors.TextSecondary else Color.Red,
                fontWeight = if (countdown > 0) FontWeight.Normal else FontWeight.Bold
            )
        )
    }
}

@Composable
fun ModernMiniHeartRateGraph(modifier: Modifier = Modifier, data: List<Float>) {
    Canvas(modifier = modifier) {
        Log.d("HeartRateGraph", "Drawing simple graph with ${data.size} data points")
        if (data.size < 2) return@Canvas

        val minHr = 40f
        val maxHr = 180f
        val xStep = size.width / (data.size - 1).toFloat()

        // 간단한 직선 연결
        data.forEachIndexed { index, hr ->
            if (index > 0) {
                val prevX = (index - 1) * xStep
                val prevY = size.height - ((data[index - 1].coerceIn(minHr, maxHr) - minHr) / (maxHr - minHr)) * size.height
                val x = index * xStep
                val y = size.height - ((hr.coerceIn(minHr, maxHr) - minHr) / (maxHr - minHr)) * size.height
                
                // 매우 두꺼운 빨간 선
                drawLine(
                    color = Color.Red,
                    start = Offset(prevX, prevY),
                    end = Offset(x, y),
                    strokeWidth = 6f
                )
                
                // 얇은 흰색 선 (위에 덮어서)
                drawLine(
                    color = Color.White,
                    start = Offset(prevX, prevY),
                    end = Offset(x, y),
                    strokeWidth = 2f
                )
            }
        }

        // 모든 점에 원 그리기
        data.forEachIndexed { index, hr ->
            val x = index * xStep
            val y = size.height - ((hr.coerceIn(minHr, maxHr) - minHr) / (maxHr - minHr)) * size.height
            
            drawCircle(
                color = Color.Red,
                radius = 4f,
                center = Offset(x, y)
            )
            drawCircle(
                color = Color.White,
                radius = 2f,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun ModernHeartRateGraph(modifier: Modifier = Modifier, data: List<Float>) {
    var counter by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            counter++
        }
    }

    Canvas(modifier = modifier) {
        // 무조건 그래프를 그리도록 임시 수정
        Log.d("HeartRateGraph", "Canvas drawing with data size: ${data.size}")
        
        if (data.isEmpty()) {
            return@Canvas
        }

        // 투명한 배경 (배경 이미지가 보이도록)
        drawRect(
            color = Color.Transparent,
            size = size
        )

        // 그리드 라인들 (세로)
        val gridLines = 10
        for (i in 1 until gridLines) {
            val x = (size.width / gridLines) * i
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
        }

        // 그리드 라인들 (가로)
        for (i in 1 until 5) {
            val y = (size.height / 5) * i
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        // 심박수 데이터 범위를 안전하게 설정
        val dataMin = data.minOrNull() ?: 70f
        val dataMax = data.maxOrNull() ?: 80f
        val minHr = (dataMin - 20f).coerceAtLeast(40f)
        val maxHr = (dataMax + 20f).coerceAtMost(200f)
        
        // min이 max보다 큰 경우 방지
        val safeMinHr = minOf(minHr, maxHr - 10f)
        val safeMaxHr = maxOf(maxHr, safeMinHr + 10f)
        val hrRange = safeMaxHr - safeMinHr
        
        Log.d("HeartRateGraph", "Data range: min=$safeMinHr, max=$safeMaxHr, range=$hrRange")
        Log.d("HeartRateGraph", "Data values: $data")

        val xStep = size.width / (data.size - 1).toFloat()

        // 심박수 라인 그리기 (항상 모든 데이터 표시)
        val path = Path()
        val points = mutableListOf<Offset>()
        val visibleData = data // 항상 모든 데이터 표시
        
        visibleData.forEachIndexed { index, hr ->
            val x = index * xStep
            val clampedHr = hr.coerceIn(safeMinHr, safeMaxHr)
            val normalizedY = (clampedHr - safeMinHr) / hrRange
            val y = size.height - (normalizedY * size.height * 0.8f) - size.height * 0.1f
            
            val point = Offset(x, y)
            points.add(point)
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        // 깔끔한 심박수 라인
        drawPath(
            path = path,
            color = Color(0xFFE57373), // 부드러운 빨간색
            style = Stroke(width = 3f)
        )

        // 작고 깔끔한 데이터 포인트들
        points.forEachIndexed { index, point ->
            drawCircle(
                color = Color(0xFFE57373),
                radius = 4f,
                center = point
            )
            drawCircle(
                color = Color.White,
                radius = 2f,
                center = point
            )
        }

        // 움직이는 스캔 라인 (실시간 효과) - 더 투명하게 해서 그래프가 잘 보이도록
        val scanX = (counter % 150) / 150f * size.width
        drawLine(
            color = Color.Cyan.copy(alpha = 0.4f), // 투명도 줄임
            start = Offset(scanX, 0f),
            end = Offset(scanX, size.height),
            strokeWidth = 1.5f // 더 얇게
        )

        // 현재 심박수 값 깔끔하게 표시
        if (points.isNotEmpty()) {
            val currentPoint = points.last()
            
            drawCircle(
                color = Color(0xFFFF5722), // 주황빛 빨간색
                radius = 6f,
                center = currentPoint
            )
            drawCircle(
                color = Color.White,
                radius = 3f,
                center = currentPoint
            )
        }
    }
}

// ECG 패턴 생성 함수
fun generateECGPattern(
    width: Float, 
    baselineY: Float, 
    amplitude: Float, 
    timeOffset: Float,
    beatInterval: Float = 1000f
): List<Offset> {
    val points = mutableListOf<Offset>()
    val stepSize = 2f
    
    for (x in 0..width.toInt() step stepSize.toInt()) {
        val adjustedX = (x + timeOffset) % width
        val phase = (adjustedX / width) * 2 * Math.PI * (1000f / beatInterval)
        
        // ECG 파형 수식 (P파, QRS파, T파 시뮬레이션)
        val ecgValue = when {
            // QRS 복합체 (큰 스파이크)
            phase % (2 * Math.PI) < 0.1 -> {
                val qrsPhase = (phase % (2 * Math.PI)) / 0.1
                when {
                    qrsPhase < 0.2 -> -0.2 * sin(qrsPhase * Math.PI / 0.2)
                    qrsPhase < 0.4 -> 1.0 * sin((qrsPhase - 0.2) * Math.PI / 0.2)
                    qrsPhase < 0.6 -> -0.8 * sin((qrsPhase - 0.4) * Math.PI / 0.2)
                    else -> 0.0
                }
            }
            // T파 (작은 언덕)
            phase % (2 * Math.PI) in 0.4..0.8 -> {
                0.3 * sin(((phase % (2 * Math.PI)) - 0.4) * Math.PI / 0.4)
            }
            // P파 (아주 작은 언덕)
            phase % (2 * Math.PI) in 1.8..(2 * Math.PI) -> {
                0.1 * sin(((phase % (2 * Math.PI)) - 1.8) * Math.PI / 0.2)
            }
            else -> 0.0
        }
        
        val y = baselineY - (ecgValue * amplitude).toFloat()
        points.add(Offset(adjustedX, y))
    }
    
    return points
}
