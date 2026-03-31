# 🎣 eoboktoktok

**DIVE 2025 글로벌 해커톤 대상 수상작**

갤럭시 워치에서 동작하는 낚시 도우미 앱입니다. 실시간 해양 데이터와 Gemini AI를 활용하여 낚시에 필요한 정보와 조언을 제공합니다.

## 주요 기능

- **AI 낚시 어드바이저** - Gemini AI 기반 음성 대화형 조언
- **실시간 해양 정보** - 날씨, 조석, 수온, 파고
- **낚시 포인트 추천** - GPS 기반 주변 포인트 검색 및 길안내
- **통발 위치 추적** - 설치한 통발 위치 저장 및 관리
- **심박수 모니터링** - 안전한 낚시를 위한 실시간 심박 측정
- **나침반** - 자기 센서 기반 방위 표시

## 🛠 기술 스택

| 분류 | 기술 |
|------|------|
| 언어 | Kotlin |
| UI | Jetpack Compose (Wear OS) |
| AI | Google Gemini 2.5 Flash Lite |
| 음성 | Google Cloud TTS, Android Speech Recognition |
| 해양 데이터 | BadaTime API, data.go.kr |
| 네트워크 | Retrofit 2, OkHttp |
| 비동기 | Kotlin Coroutines |

## 설치 방법

### 요구사항
- Android Studio Hedgehog 이상
- Kotlin 1.8+
- Galaxy Watch 4 이상 (Wear OS 3.0+)
- API Level 30+

### 빌드

```bash
git clone https://github.com/yangbeomseok/eoboktoktok.git
cd eoboktoktok
```

루트 디렉토리에 `local.properties` 파일을 생성하고 API 키를 설정합니다.

```properties
GEMINI_API_KEY=your_key
GOOGLE_CLOUD_TTS_API_KEY=your_key
BADA_TIME_API_KEY=your_key
DATA_GO_KR_API_KEY=your_key
```

`build.gradle.kts`(Module: app)에 BuildConfig 필드를 추가합니다.

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

Android Studio에서 프로젝트를 열고 Galaxy Watch를 연결한 뒤 실행하면 됩니다.

## 📁 프로젝트 구조

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

## 🏆 수상

- DIVE 2025 글로벌 해커톤 대상 (1위)

## 연락처

- GitHub: [@yangbeomseok](https://github.com/yangbeomseok)
