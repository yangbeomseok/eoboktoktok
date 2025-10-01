package com.dive.weatherwatch.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Icon
import com.dive.weatherwatch.data.ProximityLevel
import com.dive.weatherwatch.ui.theme.AppColors
import kotlin.math.*

@Composable
fun RealTimeCompass(
    targetBearing: Double,
    distance: Double,
    proximityLevel: ProximityLevel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var deviceHeading by remember { mutableStateOf(0f) }
    var isCompassReady by remember { mutableStateOf(false) }

    // 센서 매니저 설정
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        var gravity: FloatArray? = null
        var geomagnetic: FloatArray? = null

        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        gravity = event.values.clone()
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        geomagnetic = event.values.clone()
                    }
                }

                if (gravity != null && geomagnetic != null) {
                    val rotationMatrix = FloatArray(9)
                    val orientationAngles = FloatArray(3)

                    if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        
                        // 방위각을 도(degree)로 변환
                        val azimuthInRadians = orientationAngles[0]
                        val azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()
                        
                        // 0-360도 범위로 정규화
                        deviceHeading = ((azimuthInDegrees + 360) % 360)
                        isCompassReady = true
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // 센서 리스너 등록
        magnetometer?.let { 
            sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_UI)
        }
        accelerometer?.let {
            sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }

    // 상대적 방위각 계산
    val relativeBearing = if (isCompassReady) {
        ((targetBearing - deviceHeading + 360) % 360).toFloat()
    } else {
        0f
    }

    // 애니메이션 효과
    val infiniteTransition = rememberInfiniteTransition(label = "compass_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "compass_alpha"
    )

    // 근접도에 따른 색상 변화
    val arrowColor = when (proximityLevel) {
        ProximityLevel.AT_TARGET -> Color.Green
        ProximityLevel.VERY_CLOSE -> Color(0xFFFFD700) // Gold
        ProximityLevel.CLOSE -> Color(0xFFFF8C00) // Orange
        ProximityLevel.FAR -> AppColors.AccentGreen
        ProximityLevel.VERY_FAR -> Color.White
    }

    Box(
        modifier = modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        // 나침반 배경과 화살표
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2 * 0.85f

            // 외곽 원
            drawCircle(
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // 나침반 방향 표시 (N, E, S, W)
            val directions = listOf("N", "E", "S", "W")
            repeat(4) { index ->
                val angle = index * 90f - deviceHeading
                rotate(angle, center) {
                    val directionRadius = radius * 0.85f
                    val textCenter = androidx.compose.ui.geometry.Offset(
                        center.x,
                        center.y - directionRadius
                    )
                    
                    drawCircle(
                        color = if (index == 0) androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.7f) 
                               else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                        radius = 8.dp.toPx(),
                        center = textCenter
                    )
                }
            }

            // 목표 방향 화살표 (실시간 회전)
            rotate(relativeBearing, center) {
                val arrowLength = radius * 0.7f
                val arrowEnd = androidx.compose.ui.geometry.Offset(
                    center.x,
                    center.y - arrowLength
                )

                // 화살표 몸통
                drawLine(
                    color = arrowColor.copy(alpha = pulseAlpha),
                    start = center,
                    end = arrowEnd,
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // 화살표 머리
                val arrowHeadLength = 20.dp.toPx()
                val arrowHeadAngle = 30f * PI / 180f
                
                val leftWing = androidx.compose.ui.geometry.Offset(
                    arrowEnd.x - arrowHeadLength * sin(arrowHeadAngle).toFloat(),
                    arrowEnd.y + arrowHeadLength * cos(arrowHeadAngle).toFloat()
                )
                val rightWing = androidx.compose.ui.geometry.Offset(
                    arrowEnd.x + arrowHeadLength * sin(arrowHeadAngle).toFloat(),
                    arrowEnd.y + arrowHeadLength * cos(arrowHeadAngle).toFloat()
                )

                drawLine(
                    color = arrowColor.copy(alpha = pulseAlpha),
                    start = arrowEnd,
                    end = leftWing,
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = arrowColor.copy(alpha = pulseAlpha),
                    start = arrowEnd,
                    end = rightWing,
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 중심 거리 표시 원
            val distanceRadius = when (proximityLevel) {
                ProximityLevel.AT_TARGET -> radius * 0.8f
                ProximityLevel.VERY_CLOSE -> radius * 0.6f
                ProximityLevel.CLOSE -> radius * 0.4f
                ProximityLevel.FAR -> radius * 0.25f
                ProximityLevel.VERY_FAR -> radius * 0.15f
            }

            drawCircle(
                color = arrowColor.copy(alpha = 0.3f),
                radius = distanceRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // 중앙 위치 아이콘
        Icon(
            imageVector = Icons.Default.MyLocation,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )

        // 상태 표시
        if (!isCompassReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                androidx.wear.compose.material.Text(
                    text = "나침반 보정 중...",
                    fontSize = 8.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}