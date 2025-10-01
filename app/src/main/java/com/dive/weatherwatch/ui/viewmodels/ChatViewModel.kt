package com.dive.weatherwatch.ui.viewmodels

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent
import android.os.Bundle
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.dive.weatherwatch.data.WeatherDataCollector
import com.dive.weatherwatch.data.WeatherDataContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import android.media.MediaPlayer
import android.util.Base64
import java.io.File
import java.io.FileOutputStream

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class LocationInfo(
    val latitude: Double,
    val longitude: Double
)

// Google Cloud TTS API 응답 데이터 클래스들
data class VoicesResponse(
    val voices: List<Voice>
)

data class Voice(
    val languageCodes: List<String>,
    val name: String,
    val ssmlGender: String,
    val naturalSampleRateHertz: Int
)

// Google Cloud TTS 음성 합성 요청/응답 데이터 클래스들
data class SynthesizeRequest(
    val input: TextInput,
    val voice: VoiceSelection,
    val audioConfig: AudioConfig
)

data class TextInput(
    val text: String
)

data class VoiceSelection(
    val languageCode: String,
    val name: String
)

data class AudioConfig(
    val audioEncoding: String
)

data class SynthesizeResponse(
    val audioContent: String
)

class ChatViewModel(private val context: Context) : ViewModel() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var mediaPlayer: MediaPlayer? = null
    private val weatherDataCollector = WeatherDataCollector()
    
    // SpeechRecognizer 관련 (Intent 기반 음성인식과 충돌 방지를 위해 비활성화)
    // private var speechRecognizer: SpeechRecognizer? = null
    // private val _isListening = MutableStateFlow(false)
    // val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    
    init {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        // initializeSpeechRecognizer() // Intent 기반 음성인식과 충돌 방지를 위해 비활성화
        // Google Cloud TTS 음성 목록 조회 (지연 실행)
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000) // 1초 지연 후 실행
            fetchCloudTTSVoices()
        }
    }
    // 🔑 실제 Gemini API 키를 여기에 입력하세요.
    private val apiKey = "YOUR_GEMINI_API_KEY_HERE"

    // 🔑 Google Cloud TTS API 키
    private val cloudTtsApiKey = "YOUR_GOOGLE_CLOUD_TTS_API_KEY_HERE"

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash-lite",
        apiKey = apiKey
    )

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // TTS 상태 관리
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    
    // API 데이터 상태 노출
    val weatherContext = weatherDataCollector.weatherContext
    val geminiContext = weatherDataCollector.geminiContext
    val fishingAnalysis = weatherDataCollector.fishingAnalysis
    
    // 첫 응답 여부 추적
    private var isFirstResponse = true
    
    // HTTP 클라이언트 (지연 초기화) - 균형잡힌 최적화
    private val httpClient by lazy { 
        OkHttpClient.Builder()
            .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS) // 연결 타임아웃 3초
            .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)    // 읽기 타임아웃 8초 (TTS용)
            .writeTimeout(3, java.util.concurrent.TimeUnit.SECONDS)   // 쓰기 타임아웃 3초
            .retryOnConnectionFailure(true) // 재시도 활성화 (안정성 위해)
            .build()
    }
    private val gson by lazy { Gson() }
    
    
    /**
     * Google Cloud TTS 사용 가능한 음성 목록 조회
     */
    private fun fetchCloudTTSVoices() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = "https://texttospeech.googleapis.com/v1/voices?key=$cloudTtsApiKey"
                val request = Request.Builder()
                    .url(url)
                    .build()
                
                val response = httpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val voicesResponse = gson.fromJson(responseBody, VoicesResponse::class.java)
                    
                    Log.d("ChatViewModel", "=== Google Cloud TTS 사용 가능한 음성들 ===")
                    
                    // 한국어 음성들만 필터링
                    val koreanVoices = voicesResponse.voices.filter { voice ->
                        voice.languageCodes.any { it.startsWith("ko") }
                    }
                    
                    Log.d("ChatViewModel", "한국어 음성 개수: ${koreanVoices.size}")
                    koreanVoices.forEach { voice ->
                        Log.d("ChatViewModel", "음성: ${voice.name}, 언어: ${voice.languageCodes.joinToString()}, 성별: ${voice.ssmlGender}, 샘플레이트: ${voice.naturalSampleRateHertz}Hz")
                    }
                    
                    // 전체 음성 목록도 출력 (다른 언어 참고용)
                    Log.d("ChatViewModel", "=== 전체 음성 목록 (처음 10개) ===")
                    voicesResponse.voices.take(10).forEach { voice ->
                        Log.d("ChatViewModel", "음성: ${voice.name}, 언어: ${voice.languageCodes.joinToString()}, 성별: ${voice.ssmlGender}")
                    }
                    
                } else {
                    Log.e("ChatViewModel", "Google Cloud TTS 음성 목록 조회 실패: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Google Cloud TTS 음성 목록 조회 중 오류", e)
            }
        }
    }
    
    fun speakText(text: String) {
        speakWithCloudTTS(text)
    }
    
    private fun speakWithCloudTTS(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("ChatViewModel", "Google Cloud TTS 시작: $text")
                
                val request = SynthesizeRequest(
                    input = TextInput(text),
                    voice = VoiceSelection(
                        languageCode = "ko-KR",
                        name = "ko-KR-Standard-C" // 빠른 남성 음성
                    ),
                    audioConfig = AudioConfig(audioEncoding = "OGG_OPUS") // 더 압축된 포맷으로 변경
                )
                
                val url = "https://texttospeech.googleapis.com/v1/text:synthesize?key=$cloudTtsApiKey"
                val requestBody = RequestBody.create(
                    "application/json".toMediaType(),
                    gson.toJson(request)
                )
                
                val httpRequest = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()
                
                val response = httpClient.newCall(httpRequest).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val synthesizeResponse = gson.fromJson(responseBody, SynthesizeResponse::class.java)
                    
                    // Base64 디코딩하여 오디오 데이터 얻기
                    val audioData = Base64.decode(synthesizeResponse.audioContent, Base64.DEFAULT)
                    
                    // MediaPlayer로 재생
                    playAudio(audioData)
                    
                    Log.d("ChatViewModel", "Google Cloud TTS 성공")
                } else {
                    Log.e("ChatViewModel", "Google Cloud TTS 실패: ${response.code}")
                    _isSpeaking.value = false
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Google Cloud TTS 오류", e)
                _isSpeaking.value = false
            }
        }
    }
    
    
    private suspend fun playAudio(audioData: ByteArray) {
        try {
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                // 기존 MediaPlayer 정리
                mediaPlayer?.release()
                
                // 임시 파일에 오디오 데이터 저장 (OGG 확장자로 변경)
                val tempFile = File(context.cacheDir, "cloud_tts_audio_${System.currentTimeMillis()}.ogg")
                FileOutputStream(tempFile).use { fos ->
                    fos.write(audioData)
                }
                
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(tempFile.absolutePath)
                    setOnPreparedListener {
                        _isSpeaking.value = true
                        start()
                        Log.d("ChatViewModel", "Google Cloud TTS 재생 시작")
                    }
                    setOnCompletionListener {
                        _isSpeaking.value = false
                        tempFile.delete() // 임시 파일 삭제
                        Log.d("ChatViewModel", "Google Cloud TTS 재생 완료")
                    }
                    setOnErrorListener { _, what, extra ->
                        _isSpeaking.value = false
                        tempFile.delete() // 오류 시에도 임시 파일 삭제
                        Log.e("ChatViewModel", "MediaPlayer 오류: what=$what, extra=$extra")
                        true
                    }
                    // 더 빠른 prepare 방식 시도
                    try {
                        prepare() // 동기식으로 더 빠르게
                        _isSpeaking.value = true
                        start()
                        Log.d("ChatViewModel", "Google Cloud TTS 즉시 재생 시작")
                    } catch (e: Exception) {
                        Log.w("ChatViewModel", "동기식 prepare 실패, 비동기로 전환: ${e.message}")
                        prepareAsync() // 실패 시 비동기로 전환
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "오디오 재생 오류", e)
            _isSpeaking.value = false
        }
    }
    
    fun stopSpeaking() {
        Log.d("ChatViewModel", "TTS 중지 요청됨")
        
        // Google Cloud TTS MediaPlayer 중지
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.stop()
                Log.d("ChatViewModel", "MediaPlayer stop() 호출됨")
            }
        }
        
        _isSpeaking.value = false
        Log.d("ChatViewModel", "TTS 상태를 false로 변경")
    }
    
    // SpeechRecognizer 초기화 (Intent 기반 음성인식과 충돌 방지를 위해 비활성화)
    /*
    private fun initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("ChatViewModel", "음성인식 준비 완료")
                _isListening.value = true
            }
            
            override fun onBeginningOfSpeech() {
                Log.d("ChatViewModel", "음성 입력 시작")
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // 음성 볼륨 변화 (필요시 사용)
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // 음성 버퍼 (필요시 사용)
            }
            
            override fun onEndOfSpeech() {
                Log.d("ChatViewModel", "음성 입력 종료")
                _isListening.value = false
            }
            
            override fun onError(error: Int) {
                Log.e("ChatViewModel", "음성인식 오류: $error")
                _isListening.value = false
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "오디오 오류"
                    SpeechRecognizer.ERROR_CLIENT -> "클라이언트 오류"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "권한 부족"
                    SpeechRecognizer.ERROR_NETWORK -> "네트워크 오류"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 타임아웃"
                    SpeechRecognizer.ERROR_NO_MATCH -> "인식 결과 없음"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "인식기 사용 중"
                    SpeechRecognizer.ERROR_SERVER -> "서버 오류"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "음성 입력 타임아웃"
                    else -> "알 수 없는 오류"
                }
                Log.e("ChatViewModel", "음성인식 오류 상세: $errorMessage")
            }
            
            override fun onResults(results: Bundle?) {
                Log.d("ChatViewModel", "음성인식 결과 수신")
                _isListening.value = false
                
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0]
                    Log.d("ChatViewModel", "인식된 텍스트: $recognizedText")
                    // 자동으로 메시지 처리
                    processUserMessage(recognizedText)
                }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                // 부분 결과 (필요시 사용)
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    Log.d("ChatViewModel", "부분 인식 결과: ${matches[0]}")
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                // 기타 이벤트 (필요시 사용)
            }
        })
    }
    */
    
    // 음성인식 시작 (Intent 기반 음성인식과 충돌 방지를 위해 비활성화)
    /*
    fun startListening() {
        Log.d("ChatViewModel", "startListening 호출됨")
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e("ChatViewModel", "음성 녹음 권한이 없습니다")
            return
        }
        
        // SpeechRecognizer 사용 가능 여부 확인
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("ChatViewModel", "SpeechRecognizer를 사용할 수 없습니다")
            return
        }
        
        if (speechRecognizer == null) {
            Log.e("ChatViewModel", "SpeechRecognizer가 초기화되지 않았습니다")
            initializeSpeechRecognizer()
        }
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            // 자동 완료 설정
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        Log.d("ChatViewModel", "음성인식 시작 시도")
        speechRecognizer?.startListening(intent)
    }
    
    // 음성인식 중지 (Intent 기반 음성인식과 충돌 방지를 위해 비활성화)
    fun stopListening() {
        // Log.d("ChatViewModel", "음성인식 중지")
        // speechRecognizer?.stopListening()
        // _isListening.value = false
    }
    */
    
    
    
    
    
    
    
    

    private suspend fun getCurrentLocation(): LocationInfo? {
        return try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && 
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return null
            }
            
            val location = fusedLocationClient.lastLocation.await()
            location?.let {
                LocationInfo(it.latitude, it.longitude)
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error getting location", e)
            null
        }
    }
    
    fun processUserMessage(userInput: String, locationName: String? = null, onNavigateToScreen: ((String) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val userMessage = ChatMessage(content = userInput, isUser = true)
            _messages.value = _messages.value + userMessage
            _isLoading.value = true

            // 화면 이동 키워드 및 조건 체크
            val shouldNavigate = checkNavigationTrigger(userInput)
            if (shouldNavigate != null && onNavigateToScreen != null) {
                // "잠시만 기다려 주세요..." 메시지 표시
                val loadingMessage = ChatMessage(content = "잠시만 기다려 주세요...", isUser = false)
                _messages.value = _messages.value + loadingMessage
                _isLoading.value = false
                
                // 1초 대기 후 화면 이동 (TTS 없이)
                kotlinx.coroutines.delay(1000L)
                
                // 화면 이동 실행 (메인 스레드에서)
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onNavigateToScreen(shouldNavigate)
                }
                return@launch
            }

            try {
                // 위치 정보 가져오기
                val location = getCurrentLocation()
                
                // 위치가 있으면 최신 API 데이터 수집
                if (location != null) {
                    Log.d("ChatViewModel", "위치 확인됨, API 데이터 수집 시작")
                    weatherDataCollector.collectAllWeatherData(
                        location.latitude, 
                        location.longitude, 
                        locationName
                    )
                }
                
                // 수집된 데이터로 Gemini 컨텍스트 생성
                val currentGeminiContext = geminiContext.value
                val currentFishingAnalysis = fishingAnalysis.value
                val dataStatus = weatherDataCollector.getDataStatus()
                
                Log.d("ChatViewModel", "Gemini 컨텍스트 길이: ${currentGeminiContext.length}")
                Log.d("ChatViewModel", dataStatus)
                
                // 전문적인 어시스턴트 프롬프트 생성
                val enhancedPrompt = buildAdvancedPrompt(
                    userInput, 
                    location, 
                    locationName, 
                    currentGeminiContext, 
                    currentFishingAnalysis
                )
                
                val response = generativeModel.generateContent(enhancedPrompt)
                var aiResponse = response.text ?: "죄송합니다. 응답을 생성할 수 없습니다."
                
                // 첫 응답에 인사말 추가
                if (isFirstResponse) {
                    aiResponse = "안녕하세요, '어복이'예요! $aiResponse"
                    isFirstResponse = false
                }
                
                val aiMessage = ChatMessage(content = aiResponse, isUser = false)
                _messages.value = _messages.value + aiMessage

                _isLoading.value = false
                // AI 응답을 TTS로 재생
                speakText(aiResponse)

            } catch (e: Exception) {
                val errorMessage = "오류가 발생했습니다: ${e.message}"
                val aiMessage = ChatMessage(content = errorMessage, isUser = false)
                _messages.value = _messages.value + aiMessage
                _isLoading.value = false
                speakText(errorMessage)
                Log.e("ChatViewModel", "Error generating response", e)
            }
        }
    }
    
    private fun checkNavigationTrigger(userInput: String): String? {
        val lowerInput = userInput.lowercase()
        
        // "이동해줘" 또는 "띄워줘" 키워드 확인
        val hasNavigationTrigger = lowerInput.contains("이동해줘") || lowerInput.contains("띄워줘") || lowerInput.contains("보여줘") || lowerInput.contains("찾아줘")
                || lowerInput.contains("찾자") || lowerInput.contains("어디갔노")

        if (!hasNavigationTrigger) {
            return null // 네비게이션 키워드가 없으면 일반 채팅으로 처리
        }
        
        // WeatherMainScreen 키워드
        val weatherKeywords = listOf(
            "날씨", "바람 세기", "습도", "강수확률", "강수량", "파고", "수온",
            "미세먼지", "초미세먼지", "풍속", "풍향", "바람 방향",
            "일출", "일몰", "월출", "월몰"
        )
        
        // TideScreen 키워드
        val tideKeywords = listOf("물때", "몇 물", "간조", "만조", "물때 정보")
        
        // FishingPointScreen 키워드
        val fishingPointKeywords = listOf(
            "낚시 지수", "낚시 점수", "대상어", "낚시 포인트", "지역 정보",
            "포인트", "클로로필", "엽록소"
        )
        
        // TrapLocationScreen 키워드
        val trapLocationKeywords = listOf("통발", "어구")
        
        // ThirdWatchScreen 키워드
        val thirdWatchKeywords = listOf("심박수", "심장")
        
        // CompassScreen 키워드
        val compassKeywords = listOf("나침반", "방위", "위치", "방향")
        
        // 각 화면별 키워드 매칭
        return when {
            weatherKeywords.any { lowerInput.contains(it) } -> "weather"
            tideKeywords.any { lowerInput.contains(it) } -> "tide"
            fishingPointKeywords.any { lowerInput.contains(it) } -> "fishing_point"
            trapLocationKeywords.any { lowerInput.contains(it) } -> "trap_location"
            thirdWatchKeywords.any { lowerInput.contains(it) } -> "third_watch"
            compassKeywords.any { lowerInput.contains(it) } -> "compass"
            else -> null // 매칭되는 키워드가 없으면 일반 채팅
        }
    }
    
    private fun buildAdvancedPrompt(
        userInput: String,
        location: LocationInfo?,
        locationName: String?,
        geminiContext: String,
        fishingAnalysis: String
    ): String {
        val prompt = StringBuilder()
        
        // AI 어시스턴트 역할 설정
        prompt.append("""
            당신은 '어복'이라는 이름의 최고의 바다낚시 전문가이자 해양 기상 분석가입니다.
            실시간 API 데이터를 바탕으로 정확하고 전문적인 조언을 제공합니다.
            
            ==답변 규칙==
            - 150자 이내로 간결하되 핵심 정보 포함
            - 현재 상황에 맞는 실용적 조언 제공
            - 안전 관련 정보는 반드시 언급
            - 구체적인 수치와 근거 제시
            - 인사말이나 자기소개 없이 바로 본론으로 시작
            - 이모티콘을 절대 사용하지 마세요 (음성으로 읽을 때 문제가 됩니다)
            - 반드시 순우리말과 한글로만 답변하세요 (영어, 중국어, 일본어 단어 사용 금지)
            - 예: "condition" → "상태", "point" → "지점", "temperature" → "온도"
            - 정중한 존댓말로 답변하세요 (~입니다, ~세요, ~습니다 등)
            - 예: "바람이 강함" → "바람이 강합니다", "조심해" → "조심하세요"
            - 온도는 반드시 "25도", "18도" 형태로 표현하세요 (°C, ℃ 기호 사용 금지)
            - 예: "수온 23°C" → "수온 23도", "기온이 15℃" → "기온이 15도"
            - 대신 명확한 문장과 줄바꿈으로 구조화하여 답변
            - 중요한 정보별로 줄을 나누어 명확하게 구분
            - 낚시 포인트 질문 시 반드시 실시간 데이터에서 제공되는 정확한 포인트명을 사용 (예: '한국해양대학교 선착장')
            - 절대 '영도', '부산' 같은 광범위한 지역명만 말하지 말고, 구체적인 시설명이나 세부 지명까지 포함하여 답변
            
        """.trimIndent())
        
        // 실시간 데이터 컨텍스트 추가
        if (geminiContext.isNotEmpty()) {
            prompt.append("\n==실시간 데이터==\n")
            prompt.append(geminiContext)
            prompt.append("\n")
        }
        
        // 낚시 조건 분석 추가
        if (fishingAnalysis.isNotEmpty()) {
            prompt.append("\n==현재 낚시 조건 분석==\n")
            prompt.append(fishingAnalysis)
            prompt.append("\n")
        }
        
        // 위치 정보 추가
        if (location != null && locationName != null) {
            prompt.append("\n==사용자 현재 위치==\n")
            prompt.append("지역: $locationName\n")
            prompt.append("좌표: ${location.latitude}, ${location.longitude}\n")
            prompt.append("위 실시간 데이터는 모두 이 위치 기준입니다.\n")
        }
        
        // 질문 유형별 특화 지침
        val questionType = analyzeQuestionType(userInput)
        when (questionType) {
            "weather" -> {
                prompt.append("\n==날씨 관련 답변 가이드==\n")
                prompt.append("- 풍속, 파고 등 낚시 안전에 직결되는 정보 우선 제공\n")
                prompt.append("- 현재 상황이 낚시에 적합한지 명확히 판단\n")
                prompt.append("- 날씨 정보는 줄바꿈으로 구분하여 제공\n")
            }
            "fishing" -> {
                prompt.append("\n==낚시 관련 답변 가이드==\n")
                prompt.append("- 현재 수온과 계절을 고려한 어종 추천\n")
                prompt.append("- 조위 상태에 따른 최적 낚시 시간대 제안\n")
                prompt.append("- 구체적인 포인트와 미끼 정보 제공\n")
                prompt.append("- 각 정보를 줄바꿈으로 구분하여 명확하게 제시\n")
            }
            "fishing_point" -> {
                prompt.append("\n==낚시 포인트 추천 답변 가이드==\n")
                prompt.append("- 사용자 위치에서 가장 가까운 1곳의 낚시 포인트만 추천\n")
                prompt.append("- 반드시 다음 형식으로 시작: '현재 위치에서 가장 가까운 낚시 포인트는 [===주변 낚시 포인트=== 섹션의 첫 번째 포인트명 그대로]입니다.'\n")
                prompt.append("- 포인트명은 실시간 데이터의 '===주변 낚시 포인트===' 섹션에 나온 첫 번째 '포인트명: XXX' 값을 정확히 그대로 사용\n")
                prompt.append("- 예시: 실시간 데이터에 '포인트명: 한국해양대학교 선착장'이라고 되어 있으면 반드시 '한국해양대학교 선착장'을 사용\n")
                prompt.append("- 절대로 '영도', '부산', '기장' 같은 광범위한 행정구역명만 사용하지 말 것\n")
                prompt.append("- 절대로 임의로 축약하거나 변경하지 말고, 실시간 데이터의 포인트명을 정확히 그대로 복사하여 사용\n")
                prompt.append("- 이후 자연스럽게 이어지는 문장으로 답변:\n")
                prompt.append("  '수심은 [구체적 수치]이며, [지형 특징]합니다.'\n")
                prompt.append("  '[어종명]이 주로 잡히며, [미끼 정보]를 사용하시면 좋습니다.'\n")
                prompt.append("  '현재 수온은 [온도]도이고, [낚시 조건]합니다.'\n")
                prompt.append("  '[안전 주의사항]하시기 바랍니다.'\n")
                prompt.append("- 모든 문장은 자연스럽게 연결되어 하나의 완성된 답변이 되도록 구성\n")
            }
            "bait" -> {
                prompt.append("\n==미끼 추천 답변 가이드==\n")
                prompt.append("- 현재 수온과 계절을 고려한 최적 미끼 추천\n")
                prompt.append("- 실시간 데이터의 어종 정보를 바탕으로 미끼 선택\n")
                prompt.append("- 자연 미끼와 인조 미끼 구분하여 제안\n")
                prompt.append("- 현재 조류와 날씨에 적합한 미끼 크기와 색상 안내\n")
                prompt.append("- 예: '현재 수온 20도에서는 [미끼명]이 효과적입니다. [어종명] 대상으로 [구체적 사용법]을 권합니다.'\n")
            }
            "fishing_condition" -> {
                prompt.append("\n==조황 정보 답변 가이드==\n")
                prompt.append("- 실시간 데이터를 종합한 현재 낚시 조건 분석\n")
                prompt.append("- 수온, 조류, 바람, 파고를 모두 고려한 종합 판단\n")
                prompt.append("- 시간대별 조황 변화 예측 제공\n")
                prompt.append("- 어종별 입질 활성도와 예상 포인트 안내\n")
                prompt.append("- 예: '현재 조황은 [좋음/보통/나쁨]입니다. [구체적 근거]로 인해 [시간대]에 [어종명] 위주로 입질이 예상됩니다.'\n")
            }
            "tide" -> {
                prompt.append("\n==조위 관련 답변 가이드==\n")
                prompt.append("- 현재 물때와 향후 조위 변화 설명\n")
                prompt.append("- 어종별 최적 조위 시간대 안내\n")
            }
            "safety" -> {
                prompt.append("\n==안전 관련 답변 가이드==\n")
                prompt.append("- 위험 요소 우선 언급\n")
                prompt.append("- 구체적인 안전 수치 제시\n")
            }
        }
        
        prompt.append("\n==사용자 질문==\n")
        prompt.append(userInput)
        prompt.append("\n\n위의 실시간 데이터를 바탕으로 정확하고 전문적인 답변을 제공해주세요.")
        
        return prompt.toString()
    }
    
    private fun analyzeQuestionType(input: String): String {
        val lowerInput = input.lowercase()
        return when {
            lowerInput.contains("포인트") || lowerInput.contains("장소") || lowerInput.contains("어디") || 
            lowerInput.contains("추천") && (lowerInput.contains("낚시") || lowerInput.contains("포인트")) -> "fishing_point"
            lowerInput.contains("날씨") || lowerInput.contains("바람") || lowerInput.contains("파도") -> "weather"
            lowerInput.contains("미끼") || lowerInput.contains("밑밥") || lowerInput.contains("먹이") -> "bait"
            lowerInput.contains("조황") || lowerInput.contains("입질") || lowerInput.contains("잡히") -> "fishing_condition"
            lowerInput.contains("조위") || lowerInput.contains("물때") || lowerInput.contains("썰물") || lowerInput.contains("밀물") -> "tide"
            lowerInput.contains("낚시") || lowerInput.contains("어종") -> "fishing"
            lowerInput.contains("안전") || lowerInput.contains("위험") || lowerInput.contains("주의") -> "safety"
            else -> "general"
        }
    }
    
    /**
     * 주기적으로 데이터를 업데이트 시작
     */
    fun startDataCollection(latitude: Double, longitude: Double, locationName: String?) {
        weatherDataCollector.startPeriodicUpdate(latitude, longitude, locationName, 10)
        Log.d("ChatViewModel", "주기적 데이터 수집 시작")
    }
    
    /**
     * 데이터 수집 중지
     */
    fun stopDataCollection() {
        weatherDataCollector.stopPeriodicUpdate()
        Log.d("ChatViewModel", "데이터 수집 중지")
    }
    
    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        // speechRecognizer?.destroy() // Intent 기반 음성인식과 충돌 방지를 위해 비활성화
        stopDataCollection()
    }
}