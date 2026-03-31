# Hackathon_DIVE_2025_Multimodal_LLM_Galaxy_Watch_Agent

**글로벌 해커톤 DIVE 2025 대상 수상작**

갤럭시 워치용 낚시 도우미 앱 **어복톡톡**. 실시간 해양 데이터와 Gemini AI를 결합하여 낚시꾼에게 최적의 조언을 제공한다.

## 주요 기능

- **AI 낚시 어드바이저**: Gemini AI 기반 음성 대화형 조언
- **실시간 해양 정보**: 날씨, 조석, 수온, 파고
- **낚시 포인트 추천**: GPS 기반 주변 포인트 검색 및 길안내
- **통발 위치 추적**: 설치한 통발 위치 저장/관리
- **심박수 모니터링**: 안전한 낚시를 위한 실시간 심박 측정
- **나침반**: 자기 센서 기반 방위 표시

## 기술 스택

| 분류 | 기술 |
|------|------|
| 언어 | Kotlin |
| UI | Jetpack Compose (Wear OS) |
| AI | Google Gemini 2.5 Flash Lite |
| 음성 | Google Cloud TTS, Android Speech Recognition |
| 해양 데이터 | BadaTime API, data.go.kr |
| 네트워크 | Retrofit 2, OkHttp |
| 비동기 | Kotlin Coroutines |

## 설치

### 요구사항
- Android Studio Hedgehog 이상
- Kotlin 1.8+
- Galaxy Watch 4 이상 (Wear OS 3.0+)
- API Level 30+

### 빌드

```bash
git clone https://github.com/yangbeomseok/Hackathon_DIVE_2025_Multimodal_LLM_Galaxy_Watch_Agent.git
cd Hackathon_DIVE_2025_Multimodal_LLM_Galaxy_Watch_Agent
```

루트 디렉토리에 `local.properties` 파일을 만들고 API 키 설정:

```properties
GEMINI_API_KEY=your_key
GOOGLE_CLOUD_TTS_API_KEY=your_key
BADA_TIME_API_KEY=your_key
DATA_GO_KR_API_KEY=your_key
```

`build.gradle.kts`(Module: app)에 BuildConfig 필드 추가:

```kotlin
android {
    buildTypes {
        debug {
            buildConfigField("String", "GEMINI_API_KEY", "\"${project.findProperty("GEMINI_API_KEY") ?: ""}\"")
            buildConfigField("String", "GOOGLE_CLOUD_TTS_API_KEY", "\"${project.findProperty("GOOGLE_CLOUD_TTS_API_KEY") ?: ""}\"")
            buildConfigField("String", "BADA_TIME_API_KEY", "\"${project.findProperty("BADA_TIME_API_KEY") ?: ""}\"")
            buildConfigField("String", "DATA_GO_KR_API_KEY", "\"${project.findProperty("DATA_GO_KR_API_KEY") ?: ""}\"")
        }
    }
}
```

Android Studio에서 프로젝트 열고 Galaxy Watch 연결 후 실행.

## 프로젝트 구조

```
app/src/main/java/com/dive/weatherwatch/
├── MainActivity.kt
├── data/                          # API 서비스 및 데이터 모델
│   ├── WeatherService.kt
│   ├── TideService.kt
│   ├── BadaTimeService.kt
│   └── WeatherDataCollector.kt
├── ui/
│   ├── screens/                   # 화면
│   │   ├── MainHubScreen.kt      # 메인 허브
│   │   ├── FourthWatchScreen.kt  # AI 챗봇
│   │   ├── SecondWatchScreen.kt  # 날씨
│   │   ├── TideScreen.kt        # 조석
│   │   ├── FishingPointScreen.kt # 낚시 포인트
│   │   ├── TrapLocationScreen.kt # 통발 추적
│   │   ├── CompassScreen.kt     # 나침반
│   │   └── ThirdWatchScreen.kt  # 심박수
│   ├── viewmodels/               # 뷰모델
│   ├── components/               # 재사용 컴포넌트
│   ├── navigation/               # 화면 네비게이션
│   └── theme/                    # 테마
├── services/                     # 백그라운드 서비스
└── utils/                        # 유틸리티
```

## 수상

- DIVE 2025 글로벌 해커톤 대상 (1위)

## 연락처

- GitHub: [@yangbeomseok](https://github.com/yangbeomseok)
