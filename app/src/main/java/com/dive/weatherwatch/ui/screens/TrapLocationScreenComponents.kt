package com.dive.weatherwatch.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.dive.weatherwatch.data.TrapLocation
import com.dive.weatherwatch.data.ProximityLevel
import com.dive.weatherwatch.data.TrapNavigationInfo
import com.dive.weatherwatch.ui.theme.AppColors
import kotlin.math.*

@Composable
fun TrapListView(
    traps: List<TrapLocation>,
    isLocationLoading: Boolean,
    currentLocation: Pair<Double, Double>?,
    onDeployTrap: () -> Unit,
    onDeleteTrap: (TrapLocation) -> Unit,
    onNavigateToTrap: (TrapLocation) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Button row 1: Deploy trap
        Chip(
            onClick = onDeployTrap,
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            colors = ChipDefaults.chipColors(
                backgroundColor = AppColors.AccentGreen.copy(alpha = 0.8f)
            ),
            label = {
                Text(
                    text = if (isLocationLoading) "위치 확인 중..." else "통발 투하",
                    fontSize = 8.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            icon = {
                if (isLocationLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            },
            enabled = !isLocationLoading && currentLocation != null
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Traps list or empty state
        if (traps.isEmpty()) {
            EmptyTrapView(onDeployTrap)
        } else {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                traps.forEach { trap ->
                    TrapItem(
                        trap = trap,
                        currentLocation = currentLocation,
                        onDelete = { onDeleteTrap(trap) },
                        onNavigate = { onNavigateToTrap(trap) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTrapView(onDeployTrap: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "⚓",
            fontSize = 32.sp
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "등록된 통발이 없습니다",
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = "통발을 투하해보세요!",
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TrapItem(
    trap: TrapLocation,
    currentLocation: Pair<Double, Double>?,
    onDelete: () -> Unit,
    onNavigate: () -> Unit
) {
    val distance = currentLocation?.let { (lat: Double, lon: Double) ->
        calculateDistanceUtil(lat, lon, trap.latitude, trap.longitude)
    }
    
    Card(
        onClick = onNavigate,
        modifier = Modifier.fillMaxWidth(),
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = if (trap.isActive)
                AppColors.PrimaryLight.copy(alpha = 0.3f)
            else
                Color.Gray.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trap.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    distance?.let {
                        Text(
                            text = "${formatDistance(it)} 떨어짐",
                            fontSize = 9.sp,
                            color = AppColors.AccentGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    if (trap.baitType != "미설정") {
                        Text(
                            text = "🎣 ${trap.baitType}",
                            fontSize = 8.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    
                    Text(
                        text = formatDeployTime(trap.deployTime),
                        fontSize = 8.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Chip(
                        onClick = onNavigate,
                        modifier = Modifier.size(22.dp),
                        colors = ChipDefaults.chipColors(
                            backgroundColor = AppColors.AccentGreen.copy(alpha = 0.7f)
                        ),
                        label = {},
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = "찾기",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    )
                    
                    Chip(
                        onClick = onDelete,
                        modifier = Modifier.size(22.dp),
                        colors = ChipDefaults.chipColors(
                            backgroundColor = Color.Red.copy(alpha = 0.7f)
                        ),
                        label = {},
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "삭제",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NavigationView(
    navigationInfo: TrapNavigationInfo?,
    selectedTrap: TrapLocation?,
    onStopNavigation: () -> Unit,
    currentDeviceHeading: Double = 0.0,
    currentLocation: Pair<Double, Double>? = null
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (navigationInfo != null && selectedTrap != null) {
            // 실시간 거리 계산
            val realTimeDistance = if (currentLocation != null) {
                val distance = calculateDistanceUtil(
                    currentLocation.first, currentLocation.second,
                    selectedTrap.latitude, selectedTrap.longitude
                )
                Log.d("TrapNavigation", "현재 위치: ${currentLocation.first}, ${currentLocation.second}")
                Log.d("TrapNavigation", "통발 위치: ${selectedTrap.latitude}, ${selectedTrap.longitude}")
                Log.d("TrapNavigation", "계산된 거리: ${distance}m")
                distance
            } else {
                Log.d("TrapNavigation", "현재 위치 null - 기본 거리 사용: ${navigationInfo.distanceMeters}m")
                navigationInfo.distanceMeters
            }
            
            // Full screen compass navigation
            NavigationCompass(
                targetBearing = navigationInfo.bearingDegrees,
                distance = realTimeDistance, // 실시간 계산된 거리 사용
                proximityLevel = navigationInfo.proximityLevel,
                currentDeviceHeading = currentDeviceHeading,
                trapName = selectedTrap.name,
                modifier = Modifier.fillMaxSize()
            )
            
            // Back/Stop button at top left corner
            Chip(
                onClick = onStopNavigation,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(32.dp),
                colors = ChipDefaults.chipColors(
                    backgroundColor = Color.Black.copy(alpha = 0.7f)
                ),
                label = {},
                icon = {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
            
        } else {
            // No navigation active
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.GpsOff,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "통발을 선택해주세요",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun NavigationCompass(
    targetBearing: Double,
    distance: Double,
    proximityLevel: ProximityLevel,
    currentDeviceHeading: Double,
    trapName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // 센서 기반 움직이는 나침반 구현
    var deviceAzimuth by remember { mutableStateOf<Float?>(null) }
    var isCompassReady by remember { mutableStateOf(false) }
    
    Log.d("NavigationCompass", "움직이는 나침반을 사용합니다")
    
    // 거리 추적을 위한 상태들
    var previousDistance by remember { mutableStateOf<Double?>(null) }
    var distanceChangeMessage by remember { mutableStateOf<String?>(null) }
    var lastDistanceUpdate by remember { mutableLongStateOf(0L) }
    
    // 햅틱 피드백을 위한 상태들
    var lastHapticTime by remember { mutableLongStateOf(0L) }
    var isInProximityZone by remember { mutableStateOf(false) }
    
    // 센서 이벤트 리스너 - 움직이는 나침반
    val sensorEventListener = remember {
        object : SensorEventListener {
            private val accelerometerReading = FloatArray(3)
            private val magnetometerReading = FloatArray(3)
            private val rotationMatrix = FloatArray(9)
            private val orientationAngles = FloatArray(3)
            
            private var lastUpdateTime = 0L
            private val UPDATE_INTERVAL = 200L // 200ms 간격으로 제한
            
            override fun onSensorChanged(event: SensorEvent?) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdateTime < UPDATE_INTERVAL) {
                    return // 너무 자주 업데이트하지 않음
                }
                
                event?.let {
                    when (it.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> {
                            System.arraycopy(it.values, 0, accelerometerReading, 0, accelerometerReading.size)
                        }
                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            System.arraycopy(it.values, 0, magnetometerReading, 0, magnetometerReading.size)
                        }
                    }
                    
                    if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)) {
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        val azimuthInRadians = orientationAngles[0]
                        var azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()
                        azimuthInDegrees = (azimuthInDegrees + 360) % 360
                        
                        deviceAzimuth = azimuthInDegrees
                        isCompassReady = true
                        lastUpdateTime = currentTime
                    }
                }
            }
            
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }
    
    // 센서 관리
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        
        if (accelerometer != null && magnetometer != null) {
            Log.d("NavigationCompass", "센서 등록: 가속도계, 자기장 센서 (안정적 설정)")
            sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(sensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_GAME)
        } else {
            Log.w("NavigationCompass", "센서 등록 실패: 가속도계 또는 자기장 센서가 없습니다")
        }
        
        onDispose {
            Log.d("NavigationCompass", "센서 등록 해제")
            try {
                sensorManager.unregisterListener(sensorEventListener)
            } catch (e: Exception) {
                Log.e("NavigationCompass", "센서 등록 해제 중 오류 발생", e)
            }
        }
    }
    
    val compassRotation by animateFloatAsState(
        targetValue = -(deviceAzimuth ?: 0f),
        animationSpec = tween(durationMillis = 50, easing = LinearEasing),
        label = "compass_rotation"
    )
    
    // 실시간 센서 기반 방향 계산
    val bearingDiff = if (deviceAzimuth != null) {
        abs(targetBearing - deviceAzimuth!!.toDouble())
    } else {
        abs(targetBearing - currentDeviceHeading)
    }
    val isOnTarget = bearingDiff < 15.0 || bearingDiff > 345.0
    
    // 거리 변화 감지 및 상태 메시지 업데이트
    LaunchedEffect(distance) {
        val currentTime = System.currentTimeMillis()
        
        Log.d("NavigationCompass", "거리 업데이트: ${distance}m (이전: ${previousDistance}m)")
        
        if (previousDistance != null && currentTime - lastDistanceUpdate > 2000) { // 2초마다 체크
            val distanceChange = distance - previousDistance!!
            val changeThreshold = 1.0 // 1m 이상 변화 시 메시지 표시
            
            Log.d("NavigationCompass", "거리 변화: ${distanceChange}m (임계값: ${changeThreshold}m)")
            
            // 거리가 너무 작을 때는 변화 메시지를 표시하지 않음
            if (distance > 2.0 && previousDistance!! > 2.0) {
                when {
                    distanceChange > changeThreshold -> {
                        distanceChangeMessage = "${distanceChange.toInt()}m 멀어지는 중"
                        Log.d("NavigationCompass", "메시지 표시: ${distanceChangeMessage}")
                    }
                    distanceChange < -changeThreshold -> {
                        distanceChangeMessage = "${kotlin.math.abs(distanceChange).toInt()}m 가까워지는 중"
                        Log.d("NavigationCompass", "메시지 표시: ${distanceChangeMessage}")
                    }
                    else -> {
                        distanceChangeMessage = null
                        Log.d("NavigationCompass", "메시지 없음 - 변화 미미")
                    }
                }
                
                // 3초 후 메시지 제거
                kotlinx.coroutines.delay(3000)
                distanceChangeMessage = null
            }
            
            lastDistanceUpdate = currentTime
        }
        
        previousDistance = distance
    }
    
    // 햅틱 피드백 시스템 (PROMPT.TXT 요구사항에 따라)
    LaunchedEffect(distance) {
        val currentTime = System.currentTimeMillis()
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        when {
            // 목표 도착 (5m 이내) - 강한 연속 진동
            distance <= 5.0 -> {
                if (currentTime - lastHapticTime > 1000) { // 1초마다
                    val vibrationEffect = VibrationEffect.createWaveform(
                        longArrayOf(0, 200, 100, 200), // 패턴: 대기, 진동, 멈춤, 진동
                        -1 // 반복하지 않음
                    )
                    vibrator.vibrate(vibrationEffect)
                    lastHapticTime = currentTime
                }
                isInProximityZone = true
            }
            
            // 매우 근접 (10m 이내) - 빠른 진동
            distance <= 10.0 -> {
                if (currentTime - lastHapticTime > 2000) { // 2초마다
                    val vibrationEffect = VibrationEffect.createOneShot(150, 255) // 고정된 amplitude 값 사용
                    vibrator.vibrate(vibrationEffect)
                    lastHapticTime = currentTime
                }
                isInProximityZone = true
            }
            
            // 근접 (30m 이내) - 약한 진동
            distance <= 30.0 -> {
                if (currentTime - lastHapticTime > 5000) { // 5초마다
                    val vibrationEffect = VibrationEffect.createOneShot(100, 128) // 고정된 amplitude 값 사용
                    vibrator.vibrate(vibrationEffect)
                    lastHapticTime = currentTime
                }
                isInProximityZone = true
            }
            
            else -> {
                isInProximityZone = false
            }
        }
    }
    
    // 올바른 방향으로 향할 때 성공 햅틱 (PROMPT.TXT: '뒹!' 피드백)
    LaunchedEffect(isOnTarget) {
        if (isOnTarget) {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            // '뒹!' 효과 - 짧고 강한 진동
            val successVibration = VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(successVibration)
        }
    }
    
    // 거리에 따른 초록색 alpha 값 조정 (가까워지면 진한 초록색)
    val greenAlpha = when {
        distance <= 10.0 -> 1.0f  // 10m 이내에서 진한 초록색
        distance <= 30.0 -> 0.8f  // 30m 이내에서 조금 진한 초록색
        else -> 0.5f              // 그 외에는 연한 초록색
    }
    
    val arrowColor by animateColorAsState(
        targetValue = Color.Green.copy(alpha = greenAlpha), // 항상 초록색, alpha만 변경
        animationSpec = tween(durationMillis = 300),
        label = "arrow_color"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Background compass rose that rotates with device
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val center = size.center
            val radius = minOf(size.width, size.height) / 2f * 0.8f
            
            rotate(degrees = compassRotation, pivot = center) {
                // Draw compass background circle
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
                
                // Draw cardinal directions (N, E, S, W)
                for (i in 0 until 4) {
                    val angle = i * 90f - 90f // Start from North (top), -90 to make North point up
                    val radians = Math.toRadians(angle.toDouble())
                    val startRadius = radius * 0.85f
                    val endRadius = radius * 0.95f
                    
                    val startX = center.x + cos(radians).toFloat() * startRadius
                    val startY = center.y + sin(radians).toFloat() * startRadius
                    val endX = center.x + cos(radians).toFloat() * endRadius
                    val endY = center.y + sin(radians).toFloat() * endRadius
                    
                    drawLine(
                        color = if (i == 0) Color.Red else Color.White, // North (first) is red
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (i == 0) 3.dp.toPx() else 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                
                // Draw minor tick marks
                for (i in 0 until 36) {
                    if (i % 9 != 0) { // Skip cardinal directions
                        val angle = i * 10f - 90f // Adjust for North pointing up
                        val radians = Math.toRadians(angle.toDouble())
                        val startRadius = radius * 0.9f
                        val endRadius = radius * 0.95f
                        
                        val startX = center.x + cos(radians).toFloat() * startRadius
                        val startY = center.y + sin(radians).toFloat() * startRadius
                        val endX = center.x + cos(radians).toFloat() * endRadius
                        val endY = center.y + sin(radians).toFloat() * endRadius
                        
                        drawLine(
                            color = Color.White.copy(alpha = 0.8f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
                
                // Draw user direction needle (moves with compass background)
                // This needle rotates with the compass to show user's current direction
                val needleLength = radius * 0.6f
                val needleEndX = center.x
                val needleEndY = center.y - needleLength // Points straight up
                
                // Draw needle shaft
                drawLine(
                    color = Color.White, // 파란색 → 흰색
                    start = center,
                    end = Offset(needleEndX, needleEndY),
                    strokeWidth = 4.dp.toPx(), // 두께 줄임
                    cap = StrokeCap.Round
                )
                
                // Draw needle head (triangle pointing up)
                val needleHeadLength = 15.dp.toPx()
                val needleHeadAngle = Math.PI / 4 // 45 degrees
                
                val needleLeftWingX = needleEndX - sin(needleHeadAngle).toFloat() * needleHeadLength
                val needleLeftWingY = needleEndY + cos(needleHeadAngle).toFloat() * needleHeadLength
                val needleRightWingX = needleEndX + sin(needleHeadAngle).toFloat() * needleHeadLength
                val needleRightWingY = needleEndY + cos(needleHeadAngle).toFloat() * needleHeadLength
                
                val needlePath = Path().apply {
                    moveTo(needleEndX, needleEndY)
                    lineTo(needleLeftWingX, needleLeftWingY)
                    lineTo(needleRightWingX, needleRightWingY)
                    close()
                }
                
                drawPath(
                    path = needlePath,
                    color = Color.White, // 파란색 → 흰색
                    style = Fill
                )
            }
            
            // Draw target arrow OUTSIDE rotate block (fixed position, always points to target)
            // This arrow stays fixed relative to real-world direction and draws on top
            val adjustedTargetBearing = targetBearing - 90.0
            val targetAngleRad = Math.toRadians(adjustedTargetBearing)
            val arrowLength = radius * 0.7f
            val arrowTipX = center.x + cos(targetAngleRad).toFloat() * arrowLength
            val arrowTipY = center.y + sin(targetAngleRad).toFloat() * arrowLength
            
            // Draw arrow shaft
            drawLine(
                color = arrowColor,
                start = center,
                end = Offset(arrowTipX, arrowTipY),
                strokeWidth = 6.dp.toPx(), // 두께 줄임
                cap = StrokeCap.Round
            )
            
            // Draw arrow head
            val arrowHeadLength = 30.dp.toPx()
            val arrowAngle = Math.PI / 5 // 36 degrees for wider arrow head
            
            val leftWingX = arrowTipX - cos(targetAngleRad - arrowAngle).toFloat() * arrowHeadLength
            val leftWingY = arrowTipY - sin(targetAngleRad - arrowAngle).toFloat() * arrowHeadLength
            val rightWingX = arrowTipX - cos(targetAngleRad + arrowAngle).toFloat() * arrowHeadLength
            val rightWingY = arrowTipY - sin(targetAngleRad + arrowAngle).toFloat() * arrowHeadLength
            
            val arrowPath = Path().apply {
                moveTo(arrowTipX, arrowTipY)
                lineTo(leftWingX, leftWingY)
                lineTo(rightWingX, rightWingY)
                close()
            }
            
            drawPath(
                path = arrowPath,
                color = arrowColor,
                style = Fill
            )
            
            // Draw center dot
            drawCircle(
                color = Color.White,
                radius = 12.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.Black,
                radius = 6.dp.toPx(),
                center = center
            )
        }
        
        // Distance text at top
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Text(
                text = "${formatDistance(distance)} 남음",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red, // 빨간색으로 변경
                textAlign = TextAlign.Center
            )
            
            // 거리 변화 상태 메시지
            AnimatedVisibility(
                visible = distanceChangeMessage != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                distanceChangeMessage?.let { message ->
                    Text(
                        text = message,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (message.contains("가까워지는")) Color.Green else Color.Yellow,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        
        // Trap name at bottom
        Text(
            text = trapName,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Blue, // 파란색으로 변경
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DeployTrapDialog(
    currentLocation: Pair<Double, Double>?,
    isDeploying: Boolean,
    onDismiss: () -> Unit,
    onDeploy: (String, String, String, String) -> Unit
) {
    var showLocationDialog by remember { mutableStateOf(true) }
    
    if (showLocationDialog && currentLocation != null) {
        val (lat, lon) = currentLocation
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                backgroundPainter = CardDefaults.cardBackgroundPainter(
                    startBackgroundColor = AppColors.PrimaryLight.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎣 통발 투하 위치",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "위도: ${String.format("%.6f", lat)}",
                        fontSize = 9.sp,
                        color = AppColors.AccentGreen,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = "경도: ${String.format("%.6f", lon)}",
                        fontSize = 9.sp,
                        color = AppColors.AccentGreen,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Chip(
                        onClick = {
                            onDeploy(
                                "통발 #${System.currentTimeMillis().toString().takeLast(3)}",
                                "",
                                "새우",
                                "보통 (5-15m)"
                            )
                            showLocationDialog = false
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = AppColors.AccentGreen.copy(alpha = 0.8f)
                        ),
                        label = {
                            Text(
                                text = "투하 확인",
                                fontSize = 9.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }
        }
    }
}

// Utility functions
private fun calculateDistanceUtil(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371e3 // Earth radius in meters
    val φ1 = lat1 * PI / 180
    val φ2 = lat2 * PI / 180
    val Δφ = (lat2 - lat1) * PI / 180
    val Δλ = (lon2 - lon1) * PI / 180

    val a = sin(Δφ / 2) * sin(Δφ / 2) +
            cos(φ1) * cos(φ2) *
            sin(Δλ / 2) * sin(Δλ / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return r * c
}

private fun formatDistance(meters: Double): String {
    return when {
        meters < 1.0 -> "< 1m"  // 1m 미만일 때 "< 1m"으로 표시
        meters < 1000 -> "${meters.toInt()}m"
        else -> "${(meters / 1000).format(1)}km"
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)

private fun formatDeployTime(deployTime: String): String {
    return try {
        val time = java.time.LocalDateTime.parse(deployTime)
        val now = java.time.LocalDateTime.now()
        val duration = java.time.Duration.between(time, now)
        
        when {
            duration.toMinutes() < 60 -> "${duration.toMinutes()}분 전"
            duration.toHours() < 24 -> "${duration.toHours()}시간 전"
            else -> "${duration.toDays()}일 전"
        }
    } catch (e: Exception) {
        "알 수 없음"
    }
}