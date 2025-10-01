package com.dive.weatherwatch.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dive.weatherwatch.ui.screens.WeatherMainScreen
import com.dive.weatherwatch.ui.screens.SecondWatchScreen
import com.dive.weatherwatch.ui.screens.ThirdWatchScreen
import com.dive.weatherwatch.ui.screens.FourthWatchScreen
import com.dive.weatherwatch.ui.screens.TideScreen
import com.dive.weatherwatch.ui.screens.FishingPointScreen
import com.dive.weatherwatch.ui.screens.CompassScreen
import com.dive.weatherwatch.ui.screens.TrapLocationScreen
import com.dive.weatherwatch.ui.viewmodels.LocationViewModel
import com.dive.weatherwatch.ui.viewmodels.WeatherViewModel
import com.dive.weatherwatch.ui.viewmodels.FishingPointViewModel

object WatchDestinations {
    const val MAIN_HUB = "main_hub"
    const val WEATHER = "weather"
    const val HEART_RATE = "heart_rate"
    const val CHAT = "chat"
    const val TIDE = "tide"
    const val FISHING_POINT = "fishing_point"
    const val TRAP_LOCATION = "trap_location"
    const val COMPASS = "compass"
}

@Composable
fun WatchNavigation() {
    val navController = rememberNavController()
    val locationViewModel: LocationViewModel = viewModel()
    val context = LocalContext.current
    
    // LocationViewModel 위치 fetch 시작
    LaunchedEffect(Unit) {
        locationViewModel.startLocationFetch(context, WeatherViewModel())
    }
    NavHost(
        navController = navController, 
        startDestination = WatchDestinations.MAIN_HUB
    ) {
        composable(
            WatchDestinations.MAIN_HUB,
            enterTransition = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
            }
        ) {
            WeatherMainScreen(
                onNavigateToHeartRate = { navController.navigate(WatchDestinations.HEART_RATE) },
                onNavigateToChat = { navController.navigate(WatchDestinations.CHAT) },
                onNavigateToTide = { navController.navigate(WatchDestinations.TIDE) },
                onNavigateToFishingPoint = { navController.navigate(WatchDestinations.FISHING_POINT) },
                onNavigateToCompass = { navController.navigate(WatchDestinations.COMPASS) },
                onNavigateToTrapLocation = { navController.navigate(WatchDestinations.TRAP_LOCATION) }
            )
        }
        composable(
            WatchDestinations.WEATHER,
            enterTransition = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
            }
        ) {
            SecondWatchScreen()
        }
        composable(
            WatchDestinations.HEART_RATE,
            enterTransition = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
            }
        ) {
            ThirdWatchScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            WatchDestinations.CHAT,
            enterTransition = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
            }
        ) {
            FourthWatchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWeather = { navController.navigate(WatchDestinations.MAIN_HUB) },
                onNavigateToTide = { navController.navigate(WatchDestinations.TIDE) },
                onNavigateToFishingPoint = { navController.navigate(WatchDestinations.FISHING_POINT) }
            )
        }
        composable(
            WatchDestinations.TIDE,
            enterTransition = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
            }
        ) {
            TideScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            WatchDestinations.FISHING_POINT,
            enterTransition = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
            }
        ) {
            val latitude by locationViewModel.latitude.collectAsState()
            val longitude by locationViewModel.longitude.collectAsState()
            val locationName by locationViewModel.locationName.collectAsState()
            val fishingPointViewModel: FishingPointViewModel = viewModel()
            val fishingPoints by fishingPointViewModel.fishingPoints.collectAsState()
            
            // 디버깅 로그
            android.util.Log.e("WatchNavigation", "🏠 FishingPoint - locationName from LocationViewModel: '$locationName'")
            
            // 위치 정보가 있으면 API 호출
            LaunchedEffect(latitude, longitude) {
                if (latitude != null && longitude != null) {
                    android.util.Log.d("WatchNavigation", "낚시 포인트 API 호출 트리거: lat=$latitude, lon=$longitude")
                    fishingPointViewModel.loadFishingPoints(latitude!!, longitude!!)
                }
            }
            
            FishingPointScreen(
                fishingPoints = fishingPoints,
                userLat = latitude,
                userLon = longitude,
                locationName = locationName,
                onBackClick = { navController.popBackStack() },
                onNavigateToCompass = { targetLat, targetLon, targetName ->
                    // 목표 지점 정보를 로그로 출력 (현재는 파라미터 전달이 복잡하므로)
                    android.util.Log.d("Navigation", "나침반 네비게이션 - 목표: $targetName ($targetLat, $targetLon)")
                    navController.navigate(WatchDestinations.COMPASS)
                }
            )
        }
        composable(
            WatchDestinations.COMPASS,
            enterTransition = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
            }
        ) {
            CompassScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            WatchDestinations.TRAP_LOCATION,
            enterTransition = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
            }
        ) {
            TrapLocationScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}