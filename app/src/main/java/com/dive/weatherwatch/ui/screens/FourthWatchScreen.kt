package com.dive.weatherwatch.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.dive.weatherwatch.ui.components.DynamicBackgroundOverlay
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.dive.weatherwatch.R
import com.dive.weatherwatch.ui.viewmodels.ChatViewModel
import com.dive.weatherwatch.ui.theme.GalaxyWatchTypography
import com.dive.weatherwatch.ui.theme.Cafe24DongdongFont
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FourthWatchScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToWeather: () -> Unit = {},
    onNavigateToTide: () -> Unit = {},
    onNavigateToFishingPoint: () -> Unit = {}
) {
    val context = LocalContext.current
    val chatViewModel: ChatViewModel = viewModel { ChatViewModel(context) }
    val weatherViewModel: com.dive.weatherwatch.ui.viewmodels.WeatherViewModel = viewModel()
    val listState = rememberScalingLazyListState()
    
    // 위치 정보 가져오기
    val locationName by weatherViewModel.locationName.collectAsState()
    
    // 위치 정보 요청 시작
    LaunchedEffect(Unit) {
        if (locationName.isNullOrEmpty()) {
            // 위치 권한이 있다면 위치 정보 가져오기 시작
            // 이 부분은 SecondWatchScreen의 fetchCurrentLocation 함수를 사용할 수 있음
        }
    }
    
    // 현재 시간 상태
    var currentTime by remember { mutableStateOf(Date()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            kotlinx.coroutines.delay(1000) // 1초마다 업데이트
        }
    }
    
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeText = timeFormat.format(currentTime)
    
    var hasAudioPermission by remember { mutableStateOf(false) }
    val chatMessages by chatViewModel.messages.collectAsState()
    val isProcessing by chatViewModel.isLoading.collectAsState()
    val isSpeaking by chatViewModel.isSpeaking.collectAsState()
    var isListening by remember { mutableStateOf(false) }
    
    // 애니메이션 단계를 위한 상태
    var animationStep by remember { mutableStateOf(0) }
    // 0: 로고만 표시
    // 1: 인사말만 표시  
    // 2: 전체 UI 표시


    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.lastIndex)
        }
    }

    // 자동 전송을 위한 타이머 상태
    var autoSendTimer by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    val voiceRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        autoSendTimer?.cancel() // 타이머 취소
        
        Log.d("FourthWatchScreen", "음성인식 결과 코드: ${result.resultCode}")
        
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                Log.d("FourthWatchScreen", "인식된 결과 리스트: $results")
                
                if (!results.isNullOrEmpty()) {
                    val spokenText = results[0]
                    if (spokenText.isNotEmpty()) {
                        Log.d("FourthWatchScreen", "음성인식 성공: '$spokenText'")
                        chatViewModel.processUserMessage(
                            spokenText, 
                            locationName,
                            onNavigateToScreen = { screenType ->
                                when (screenType) {
                                    "weather" -> onNavigateToWeather()
                                    "tide" -> onNavigateToTide()
                                    "fishing_point" -> onNavigateToFishingPoint()
                                }
                            }
                        )
                    } else {
                        Log.w("FourthWatchScreen", "인식된 텍스트가 비어있음")
                    }
                } else {
                    Log.w("FourthWatchScreen", "음성인식 결과가 null 또는 비어있음")
                }
            }
            Activity.RESULT_CANCELED -> {
                Log.w("FourthWatchScreen", "음성인식이 취소됨")
            }
            else -> {
                Log.e("FourthWatchScreen", "음성인식 실패 - 결과 코드: ${result.resultCode}")
            }
        }
    }

    fun createVoiceRecognitionIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "말씀하세요...")
            
            // 더 관대한 타이밍 설정으로 변경
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L) // 2초로 증가
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L) // 1.5초로 증가
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L) // 0.5초로 증가
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5) // 결과 5개로 증가
            
            // 추가 설정
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            
            // WearOS 특화 설정
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("ko-KR"))
            putExtra(RecognizerIntent.EXTRA_SECURE, false)
        }
    }

    val voiceRecognitionIntent = remember { createVoiceRecognitionIntent() }


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            isListening = true
            voiceRecognitionLauncher.launch(voiceRecognitionIntent)
        }
    }

    LaunchedEffect(Unit) {
        hasAudioPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    // 순차 애니메이션 제어
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000) // 2초 대기 - 로고 표시
        animationStep = 1 // 인사말로 전환
        kotlinx.coroutines.delay(2000) // 2초 대기 - 인사말 표시
        animationStep = 2 // 전체 UI로 전환
    }

    fun startListening() {
        Log.d("FourthWatchScreen", "=== startListening 호출됨 ===")
        Log.d("FourthWatchScreen", "hasAudioPermission: $hasAudioPermission")
        
        if (!hasAudioPermission) {
            Log.d("FourthWatchScreen", "권한 없음 - 권한 요청 시작")
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        
        // 새로운 인텐트를 매번 생성해서 충돌 방지
        val freshIntent = createVoiceRecognitionIntent()
        
        // 음성 인식 가능 여부 체크
        val packageManager = context.packageManager
        val activities = packageManager.queryIntentActivities(freshIntent, 0)
        Log.d("FourthWatchScreen", "음성인식 처리 가능한 앱 수: ${activities.size}")
        
        if (activities.isNotEmpty()) {
            // 사용 가능한 음성인식 앱들 로그
            activities.forEach { resolveInfo ->
                Log.d("FourthWatchScreen", "음성인식 앱: ${resolveInfo.activityInfo.packageName}")
            }
        } else {
            Log.e("FourthWatchScreen", "음성인식을 처리할 수 있는 앱이 없습니다")
            // 기본적인 설정으로 재시도
            freshIntent.removeExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS)
            freshIntent.removeExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS)
            freshIntent.removeExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS)
        }
        
        Log.d("FourthWatchScreen", "권한 OK - 음성인식 인텐트 시작")
        Log.d("FourthWatchScreen", "Intent action: ${freshIntent.action}")
        Log.d("FourthWatchScreen", "Intent extras keys: ${freshIntent.extras?.keySet()}")
        
        try {
            isListening = true
            voiceRecognitionLauncher.launch(freshIntent)
            Log.d("FourthWatchScreen", "voiceRecognitionLauncher.launch() 성공")
        } catch (e: Exception) {
            Log.e("FourthWatchScreen", "음성인식 인텐트 시작 중 오류", e)
            isListening = false
            
            // 간단한 인텐트로 재시도
            try {
                Log.d("FourthWatchScreen", "간단한 인텐트로 재시도")
                val simpleIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
                }
                isListening = true
                voiceRecognitionLauncher.launch(simpleIntent)
            } catch (e2: Exception) {
                Log.e("FourthWatchScreen", "간단한 인텐트도 실패", e2)
                isListening = false
            }
        }
    }

    if (chatMessages.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .semantics {
                    contentDescription = "AI 채팅 화면. 음성으로 대화할 수 있습니다. 화면 가장자리를 터치하면 이전 화면으로 돌아갑니다."
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
            // Dynamic Background Overlay (시간 기반 배경)
            DynamicBackgroundOverlay(
                weatherData = null,
                alpha = 0.7f,
                forceTimeBasedBackground = true
            )
            
            when (animationStep) {
                0 -> {
                    // 1단계: 로고만 표시 - 올라오기 애니메이션
                    var showFirstLogo by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(animationStep) {
                        if (animationStep == 0) {
                            kotlinx.coroutines.delay(300) // 약간의 딜레이
                            showFirstLogo = true
                        }
                    }
                    
                    AnimatedVisibility(
                        visible = showFirstLogo,
                        enter = fadeIn(animationSpec = tween(1000, easing = FastOutSlowInEasing)),
                        exit = fadeOut(animationSpec = tween(500)),
                        modifier = Modifier.offset(y = (0).dp)  // 위로 30dp 이동
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.loading),
                            contentDescription = "어福톡톡 AI",
                            modifier = Modifier.size(140.dp)
                        )
                    }
                }
                1 -> {
                    // 2단계: 인사말만 표시 - 올라오기 애니메이션
                    var showText by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(animationStep) {
                        if (animationStep == 1) {
                            kotlinx.coroutines.delay(200) // 약간의 딜레이
                            showText = true
                        }
                    }
                    
                    AnimatedVisibility(
                        visible = showText,
                        enter = fadeIn(animationSpec = tween(1000, easing = FastOutSlowInEasing)),
                        exit = fadeOut(animationSpec = tween(500))
                    ) {
                        Text(
                            text = "안녕하세요,\n무엇을 도와드릴까요?",
                            style = TextStyle(
                                fontFamily = Cafe24DongdongFont,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = Color.White
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                2 -> {
                    // 3단계: 최종 UI - 올라오기 애니메이션
                    var showLogo by remember { mutableStateOf(false) }
                    var showQuestions by remember { mutableStateOf(false) }
                    var showMic by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(animationStep) {
                        if (animationStep == 2) {
                            showLogo = true
                            kotlinx.coroutines.delay(300)
                            showQuestions = true
                            kotlinx.coroutines.delay(300)
                            showMic = true
                        }
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 0.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top  // spacedBy 제거하고 Top으로 변경
                    ) {
                        // 상단에 로고 올라오기 - 강제로 간격 줄이기
                        AnimatedVisibility(
                            visible = showLogo,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(800, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(600)),
                            modifier = Modifier
                                .padding(top = 1.dp)  // 상단 여백 최소화
                                .offset(y = 5.dp)      // 추가로 아래로 이동
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.logo_white),
                                contentDescription = "어福톡톡 AI",
                                modifier = Modifier
                                    .size(100.dp)
                                    .offset(y = (-13).dp)  // 로고 자체를 위로 당기기
                            )
                        }
                        
                        // 간격을 강제로 줄이기 위한 음수 Spacer
                        Spacer(modifier = Modifier.height((-60).dp))
                        
                        // 예시 질문들 중앙에서 올라오기
                        AnimatedVisibility(
                            visible = showQuestions,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(800, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(600))
                        ) {
                            ExampleQuestions(
                                onQuestionClick = { question ->
                                    chatViewModel.processUserMessage(
                                        question, 
                                        locationName,
                                        onNavigateToScreen = { screenType ->
                                            when (screenType) {
                                                "weather" -> onNavigateToWeather()
                                                "tide" -> onNavigateToTide()
                                                "fishing_point" -> onNavigateToFishingPoint()
                                            }
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .offset(y = (-30).dp)  // 예시 질문도 위로 당기기
                            )
                        }
                        
                        // 말풍선 추가
                        AnimatedVisibility(
                            visible = showQuestions,
                            enter = fadeIn(animationSpec = tween(800, delayMillis = 600, easing = FastOutSlowInEasing)),
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .offset(y = (-17).dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(14.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                // 말풍선 본체
                                Box(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFF667eea).copy(alpha = 0.8f),
                                                    Color(0xFF764ba2).copy(alpha = 0.8f)
                                                )
                                            )
                                        )
                                        .shadow(4.dp, RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "마이크를 눌러 질문하세요",
                                        style = GalaxyWatchTypography.Caption.copy(
                                            fontSize = 6.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.99f)
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                
                                // 말풍선 꼬리 (작은 삼각형)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .offset(y = (-1).dp)
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .graphicsLayer {
                                            rotationZ = 45f
                                        }
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFF667eea).copy(alpha = 0.8f),
                                                    Color(0xFF764ba2).copy(alpha = 0.8f)
                                                )
                                            )
                                        )
                                )
                            }
                        }
                        
                        // 가변 여백으로 공간 채우기
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    
                    // 마이크 버튼을 Box 레벨에서 하단 고정
                    AnimatedVisibility(
                        visible = showMic,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(800, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(600)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .offset(y = (8).dp)
                    ) {
                        MicButton(
                            modifier = Modifier,
                            isListening = isListening,
                            isProcessing = isProcessing,
                            isSpeaking = isSpeaking,
                            onStartListening = { startListening() },
                            onStopSpeaking = { 
                                Log.d("FourthWatchScreen", "Stop speaking button clicked")
                                chatViewModel.stopSpeaking() 
                            },
                            gradient = Brush.linearGradient(listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045)))
                        )
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                }
        ) {
            // Dynamic Background Overlay (시간 기반 배경)
            DynamicBackgroundOverlay(
                weatherData = null,
                alpha = 0.7f,
                forceTimeBasedBackground = true
            )
            
            // 채팅 영역
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // 상단에 시간 표시 - ScalingLazyColumn 밖으로 이동
                Text(
                    text = timeText,
                    style = GalaxyWatchTypography.Caption.copy(
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(top = 0.dp, bottom = 50.dp)
                ) {
                    
                    items(chatMessages.size) { index ->
                        val message = chatMessages[index]
                        ChatBubble(
                            message = message.content, 
                            isUser = message.isUser,
                            userGradient = Brush.linearGradient(
                                colors = listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045)) // 인스타그램 그라데이션
                            ),
                            aiGradient = Brush.linearGradient(
                                colors = listOf(Color(0xFFB8B8B8), Color(0xFFB8B8B8)) // 밝은 회색 단색
                            )
                        )
                    }
                }
            }
            }
            
            // 마이크 버튼을 하단 중앙에 고정 배치 (위치 조정)
            MicButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .wrapContentSize(), // 마이크 버튼을 약간 위로 올림
                isListening = isListening,
                isProcessing = isProcessing,
                isSpeaking = isSpeaking,
                onStartListening = { startListening() },
                onStopSpeaking = { 
                    Log.d("FourthWatchScreen", "Stop speaking button clicked (bottom)")
                    chatViewModel.stopSpeaking() 
                },
                gradient = Brush.linearGradient(listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045)))
            )
        }
    }
}

@Composable
fun ChatBubble(
    message: String, 
    isUser: Boolean, 
    userGradient: Brush,
    aiGradient: Brush,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { if (isUser) it else -it },
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(300)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 6.dp),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            // AI 응답일 때 왼쪽에 물고기 아이콘
            if (!isUser) {
                Image(
                    painter = painterResource(id = R.drawable.talk2),
                    contentDescription = "AI 응답",
                    modifier = Modifier
                        .size(32.dp)
                        .padding(end = 4.dp, top = 2.dp)
                )
            }
            
            Box(
                modifier = Modifier
                    .widthIn(max = 130.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(
                            topStart = if (isUser) 16.dp else 4.dp,
                            topEnd = 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isUser) 16.dp else 4.dp,
                            topEnd = 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(if (isUser) userGradient else aiGradient)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message,
                    style = GalaxyWatchTypography.Caption.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        color = if (isUser) Color.White else Color.Black
                    )
                )
            }
            
            // 사용자 메시지일 때 오른쪽에 물고기 아이콘
            if (isUser) {
                Image(
                    painter = painterResource(id = R.drawable.talk1),
                    contentDescription = "사용자 메시지",
                    modifier = Modifier
                        .size(32.dp)
                        .padding(start = 4.dp, bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
fun MicButton(
    modifier: Modifier = Modifier,
    isListening: Boolean,
    isProcessing: Boolean,
    isSpeaking: Boolean = false,
    onStartListening: () -> Unit,
    onStopSpeaking: () -> Unit = {},
    gradient: Brush
) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, 
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            tween(800, easing = FastOutSlowInEasing), 
            RepeatMode.Reverse
        ),
        label = ""
    )
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, 
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = LinearEasing), 
            RepeatMode.Reverse
        ),
        label = ""
    )
    
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = LinearOutSlowInEasing),
            RepeatMode.Restart
        ),
        label = ""
    )
    
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = LinearOutSlowInEasing),
            RepeatMode.Restart
        ),
        label = ""
    )
    
    Box(
        modifier = modifier.size(50.dp),
        contentAlignment = Alignment.Center
    ) {
        // 외곽 애니메이션 링
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer(
                        scaleX = ringScale,
                        scaleY = ringScale,
                        alpha = ringAlpha
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF00BCD4).copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        
        // 메인 버튼
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(
                    elevation = if (isListening) 20.dp else 15.dp,
                    shape = CircleShape
                )
                .clip(CircleShape)
                .background(
                    if (isListening) {
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00E676),
                                Color(0xFF00BCD4)
                            )
                        )
                    } else {
                        gradient
                    }
                )
                .graphicsLayer(
                    scaleX = if (isListening) pulseScale else 1f,
                    scaleY = if (isListening) pulseScale else 1f,
                    alpha = glowAlpha
                ),
            contentAlignment = Alignment.Center
        ) {
            // 내부 글로우 효과
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isSpeaking -> {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "응답 중지",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    isProcessing -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    isListening -> {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "듣는 중",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    else -> {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "음성 인식 시작",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // 클릭 영역
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .run {
                        when {
                            isSpeaking -> clickable { 
                                Log.d("MicButton", "Speaking state - stop button clicked")
                                onStopSpeaking() 
                            }.semantics {
                                contentDescription = "AI 응답 중지 버튼"
                                role = Role.Button
                            }
                            !isListening && !isProcessing -> clickable { 
                                Log.d("MicButton", "Idle state - start button clicked")
                                onStartListening() 
                            }.semantics {
                                contentDescription = "음성 인식 시작 버튼. 터치하면 AI와 음성으로 대화할 수 있습니다."
                                role = Role.Button
                            }
                            else -> this.semantics {
                                contentDescription = when {
                                    isListening -> "음성을 듣는 중입니다."
                                    isProcessing -> "AI가 응답을 처리하는 중입니다."
                                    else -> "음성 인식 대기 중"
                                }
                            }
                        }
                    }
            )
        }
    }
}

@Composable
fun ExampleQuestions(
    onQuestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    data class QuestionCard(
        val keyword: String,
        val fullQuestion: String
    )
    
    val questionCards = listOf(
        QuestionCard("날씨", "오늘 날씨가 어떻게 돼?"),
        QuestionCard("포인트", "근처 낚시 포인트를 추천해줘."),
        QuestionCard("물때", "오늘 물때가 어때? 낚시하기 좋은 시간대를 알려줘."),
        QuestionCard("조황", "조황 정보가 어떄?")
    )
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 첫 번째 줄 (2개 카드)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            questionCards.take(2).forEach { card ->
                ExampleQuestionCard(
                    keyword = card.keyword,
                    onClick = { onQuestionClick(card.fullQuestion) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // 두 번째 줄 (2개 카드)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            questionCards.drop(2).forEach { card ->
                ExampleQuestionCard(
                    keyword = card.keyword,
                    onClick = { onQuestionClick(card.fullQuestion) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ExampleQuestionCard(
    keyword: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(28.dp)  // 작은 정사각형 카드
            .clip(RoundedCornerShape(8.dp))
            .background(
                Color.White.copy(alpha = 0.1f)
            )
            .clickable { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = keyword,
            style = GalaxyWatchTypography.Caption.copy(
                fontSize = 10.sp,  // 8sp로 크기 조정
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.95f)
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SpeechBubble(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 말풍선 본체
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.9f),
                            Color.White.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = text,
                style = GalaxyWatchTypography.Caption.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2D3748)
                ),
                textAlign = TextAlign.Center
            )
        }
        
        // 말풍선 꼬리 (작은 다이아몬드 모양)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 3.dp)
                .size(6.dp)
                .clip(CircleShape)
                .graphicsLayer {
                    rotationZ = 45f
                }
                .background(Color.White.copy(alpha = 0.8f))
        )
    }
}

