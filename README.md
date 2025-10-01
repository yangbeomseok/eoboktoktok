# 🎣 Hackathon_DIVE_2025_Multimodal_LLM_Galaxy_Watch_Agent

> **🏆 Global Hackathon DIVE 2025 1st Prize Winner**
> **Multimodal LLM-based Fishing Agent: EoboktokTok**

A fishing assistant application for Galaxy Watch that provides optimal advice to anglers using real-time marine data and Gemini AI.

[![Android](https://img.shields.io/badge/Platform-Android_Wear_OS-green.svg)](https://developer.android.com/wear)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack_Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![API](https://img.shields.io/badge/Min_SDK-30-orange.svg)](https://developer.android.com/studio/releases/platforms)

## 📖 Project Overview

EoboktokTok is a fishing assistant application that won 1st place at the 2025 DIVE Hackathon. It combines Galaxy Watch-optimized UI/UX with multimodal AI technology to provide real-time marine information and advice to anglers.

### ✨ Key Features

- **Multimodal AI Assistant**: Gemini AI-powered voice conversational fishing advisor
- **Real-time Marine Data**: Comprehensive marine information including weather, tides, water temperature, and wave height
- **Smart Fishing Points**: GPS-based nearby fishing point recommendations and navigation
- **Trap Tracking System**: Save and manage deployed trap locations
- **Health Monitoring**: Real-time heart rate measurement for safe fishing activities
- **Voice Interaction**: Natural voice conversation using Google Cloud TTS
- **Universal Gesture Support**: Hands-free navigation through back gestures for convenient operation while fishing 🤲

## 🛠 Tech Stack

### Frontend
- **Kotlin** - Main development language
- **Jetpack Compose** - Modern UI framework
- **Wear OS Compose** - Galaxy Watch optimized UI
- **Navigation Compose** - Screen navigation

### AI & API Integration
- **Google Gemini AI** - Multimodal AI assistant
- **Google Cloud Text-to-Speech** - Voice synthesis
- **Android Speech Recognition** - Voice recognition
- **BadaTime API** - Real-time marine data

### Location & Sensors
- **Google Play Services Location** - GPS location services
- **Android Sensors** - Heart rate, compass sensors
- **Background Location** - Background location tracking

### Networking & Data
- **Retrofit 2** - RESTful API communication
- **OkHttp** - HTTP client
- **Gson** - JSON serialization/deserialization
- **Kotlin Coroutines** - Asynchronous processing

## 📱 Main Features

### 1. Main Hub (MainHubScreen)
- Intuitive navigation based on circular bezel
- Real-time clock and location information display
- Time-based theme switching (day/night mode)
- Enhanced user experience through haptic feedback
- Universal gesture support for hands-free operation

### 2. 🤖 AI Chatbot (FourthWatchScreen)
```kotlin
// Gemini AI-based conversational assistant
private val generativeModel = GenerativeModel(
    modelName = "gemini-2.5-flash-lite",
    apiKey = apiKey
)
```
- Natural language queries through voice recognition
- Customized advice based on real-time marine data
- Voice responses using Google Cloud TTS
- Screen navigation commands ("Show weather", "Find fishing points", etc.)
- Hands-free navigation through back gestures

### 3. Weather Information (SecondWatchScreen)
- Real-time weather data (temperature, humidity, wind speed, wave height)
- Hourly weather forecast
- Fishing suitability index calculation
- Sunrise/sunset, moonrise/moonset times

### 4. 🌊 Tide Information (TideScreen)
- Real-time tide status (low/high tide)
- Tide information and predictions
- Visual tide charts
- Optimal fishing time recommendations

### 5. Fishing Points (FishingPointScreen)
- GPS-based nearby fishing point search
- Detailed information for each point (depth, fish species, bait recommendations)
- Distance calculation and navigation features
- Fishing index and chlorophyll concentration information

### 6. Trap Tracking (TrapLocationScreen)
- GPS saving of deployed trap locations
- Management of multiple trap locations
- Distance and direction display to each trap
- Time recording and memo features

### 7. 🧭 Compass (CompassScreen)
- Real-time bearing display
- Direction guidance to target points
- Accurate direction measurement based on magnetic sensors

### 8. Heart Rate Monitoring (ThirdWatchScreen)
- Real-time heart rate measurement
- Exercise intensity analysis
- Safety alert features

## 🚀 Installation and Setup

### Requirements
- **Android Studio Hedgehog** or later
- **Kotlin 1.8+**
- **Galaxy Watch 4** or later (Wear OS 3.0+)
- **API Level 30** or higher

### Environment Setup

1. **Clone Repository**
```bash
git clone https://github.com/yangbeomseok/Hackathon_DIVE_2025_Multimodal_LLM_Galaxy_Watch_Agent.git
cd Hackathon_DIVE_2025_Multimodal_LLM_Galaxy_Watch_Agent
```

2. **API Key Configuration**

Create `local.properties` file in the root directory:
```properties
# Add these lines to local.properties (this file is gitignored)
GEMINI_API_KEY=your_actual_gemini_api_key_here
GOOGLE_CLOUD_TTS_API_KEY=your_actual_google_cloud_tts_api_key_here
BADA_TIME_API_KEY=your_actual_bada_time_api_key_here
```

Add to `build.gradle.kts` (Module: app):
```kotlin
android {
    buildTypes {
        debug {
            buildConfigField("String", "GEMINI_API_KEY", "\"${project.findProperty("GEMINI_API_KEY") ?: ""}\"")
            buildConfigField("String", "GOOGLE_CLOUD_TTS_API_KEY", "\"${project.findProperty("GOOGLE_CLOUD_TTS_API_KEY") ?: ""}\"")
            buildConfigField("String", "BADA_TIME_API_KEY", "\"${project.findProperty("BADA_TIME_API_KEY") ?: ""}\"")
        }
        release {
            buildConfigField("String", "GEMINI_API_KEY", "\"${project.findProperty("GEMINI_API_KEY") ?: ""}\"")
            buildConfigField("String", "GOOGLE_CLOUD_TTS_API_KEY", "\"${project.findProperty("GOOGLE_CLOUD_TTS_API_KEY") ?: ""}\"")
            buildConfigField("String", "BADA_TIME_API_KEY", "\"${project.findProperty("BADA_TIME_API_KEY") ?: ""}\"")
        }
    }
}
```

Use in code:
```kotlin
// In ChatViewModel.kt
private val apiKey = BuildConfig.GEMINI_API_KEY
private val cloudTtsApiKey = BuildConfig.GOOGLE_CLOUD_TTS_API_KEY

// In BadaTimeService.kt
@Query("key") apiKey: String = BuildConfig.BADA_TIME_API_KEY
```

3. **Permissions Required**
The app requests the following permissions:
- Location Information (GPS)
- Microphone (Voice Recognition)
- Body Sensors (Heart Rate)
- Internet Connection
- Background Location

### Build and Run

1. **Open project in Android Studio**
2. **Connect Galaxy Watch or run emulator**
3. **Click Run button to install app**

## 🏗 Project Structure

```
app/src/main/java/com/dive/weatherwatch/
├── MainActivity.kt                    # Main activity
├── data/                             # Data layer
│   ├── WeatherService.kt            # Weather API service
│   ├── TideService.kt               # Tide API service
│   ├── BadaTimeService.kt           # BadaTime API integration service
│   ├── WeatherDataCollector.kt      # Data collector
│   └── [Response Classes]           # API response models
├── ui/
│   ├── screens/                     # Screen components
│   │   ├── MainHubScreen.kt        # Main hub
│   │   ├── FourthWatchScreen.kt    # AI chatbot
│   │   ├── SecondWatchScreen.kt    # Weather information
│   │   ├── TideScreen.kt           # Tide information
│   │   ├── FishingPointScreen.kt   # Fishing points
│   │   ├── TrapLocationScreen.kt   # Trap tracking
│   │   ├── CompassScreen.kt        # Compass
│   │   └── ThirdWatchScreen.kt     # Heart rate monitor
│   ├── viewmodels/                  # ViewModels
│   │   ├── ChatViewModel.kt        # AI chatbot logic
│   │   ├── WeatherViewModel.kt     # Weather data management
│   │   ├── LocationViewModel.kt    # Location services
│   │   └── [Other ViewModels]      # Other ViewModels
│   ├── components/                  # Reusable components
│   │   ├── DynamicBackground.kt    # Dynamic background
│   │   └── RealTimeCompass.kt      # Real-time compass
│   ├── navigation/                  # Navigation
│   │   └── WatchNavigation.kt      # Screen navigation
│   └── theme/                       # Theme and styles
├── services/                        # Background services
│   ├── TideNotificationService.kt  # Tide notifications
│   └── FishingHotspotService.kt    # Fishing hotspot notifications
└── utils/                           # Utilities
    └── GpsConverter.kt             # GPS coordinate conversion
```

## 🔧 API Integration

### 1. Gemini AI
```kotlin
val response = generativeModel.generateContent(enhancedPrompt)
val aiResponse = response.text ?: "Unable to generate response."
```

### 2. Google Cloud TTS
```kotlin
val request = SynthesizeRequest(
    input = TextInput(text),
    voice = VoiceSelection(languageCode = "ko-KR", name = "ko-KR-Standard-C"),
    audioConfig = AudioConfig(audioEncoding = "OGG_OPUS")
)
```

### 3. BadaTime Marine Data API
```kotlin
@GET("current")
suspend fun getCurrentWeather(
    @Query("lat") lat: Double,
    @Query("lon") lon: Double,
    @Query("key") apiKey: String
): Response<BadaTimeCurrentApiResponse>
```

## 🎨 UI/UX Design

### Galaxy Watch Optimization
- Bezel navigation utilizing circular display
- Rotary gesture support
- Intuitive interaction through haptic feedback
- Automatic theme switching based on time
- Universal gesture (back gesture) support for hands-free operation

### Accessibility
- Large touch areas optimized for small screens
- Clear visual feedback
- Voice guidance provision
- High contrast color usage

## 🔄 Data Flow

1. **Location Acquisition**: Current location detection via GPS
2. **Data Collection**: Real-time data gathering from marine APIs
3. **AI Analysis**: Comprehensive data analysis by Gemini AI
4. **User Interaction**: Queries through voice/touch/gestures
5. **Customized Response**: Voice feedback via TTS

## ⚡ Performance Optimization

- Data caching to minimize unnecessary API calls
- Location update interval adjustment for battery optimization
- Proper lifecycle management of ViewModels and coroutines
- Network optimization using compressed audio formats

## 🧪 Testing

### Unit Testing
```bash
./gradlew test
```

### Integration Testing
```bash
./gradlew connectedAndroidTest
```

### Real Device Testing
- Tested on Galaxy Watch 4, 5, 6
- Verified in various marine environments

## 🤝 Contributing

1. Fork the repository
2. Create Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to Branch (`git push origin feature/AmazingFeature`)
5. Create Pull Request

## 📄 License

This project is distributed under the MIT License. See [LICENSE](LICENSE) file for details.

> **Note**: LICENSE file will be added soon.

## 🏆 Awards

- **DIVE 2025 Hackathon 1st Place** - Recognition for innovative user experience based on multimodal LLM

## 👥 Development Team

- DIVE 2025 participating team
- Developed based on expertise in multimodal AI and Wear OS

## 📞 Contact

If you have any questions or suggestions about the project, please contact:
- **GitHub**: [@yangbeomseok](https://github.com/yangbeomseok)
- **Issues**: [Create an Issue](https://github.com/yangbeomseok/Hackathon_DIVE_2025_Multimodal_LLM_Galaxy_Watch_Agent/issues)

## 🙏 Acknowledgments

- DIVE 2025 Hackathon organizers
- Google Gemini AI and Cloud TTS services
- BadaTime API providers
- Samsung Galaxy Watch platform

---

**"Where Ocean Meets Technology - Smart Fishing Life with EoboktokTok"**
