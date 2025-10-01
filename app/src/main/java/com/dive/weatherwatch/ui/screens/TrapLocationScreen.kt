package com.dive.weatherwatch.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import com.dive.weatherwatch.data.TrapLocation
import com.dive.weatherwatch.data.ProximityLevel
import com.dive.weatherwatch.ui.components.DynamicBackgroundOverlay
import com.dive.weatherwatch.ui.theme.AppColors
import com.dive.weatherwatch.ui.viewmodels.LocationViewModel
import com.dive.weatherwatch.ui.viewmodels.TrapViewModel
import com.dive.weatherwatch.ui.viewmodels.WeatherViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun TrapLocationScreen(
    onBack: () -> Unit,
    locationViewModel: LocationViewModel = viewModel(),
    weatherViewModel: WeatherViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val trapViewModel = remember { TrapViewModel(context) }
    
    val traps by trapViewModel.traps.collectAsState()
    val currentLocation by trapViewModel.currentLocation.collectAsState()
    val isDeployingTrap by trapViewModel.isDeployingTrap.collectAsState()
    val selectedTrap by trapViewModel.selectedTrap.collectAsState()
    val navigationInfo by trapViewModel.navigationInfo.collectAsState()
    val isNavigating by trapViewModel.isNavigating.collectAsState()
    
    val userLatitude by locationViewModel.latitude.collectAsState()
    val userLongitude by locationViewModel.longitude.collectAsState()
    val isLocationLoading by locationViewModel.isLocationLoading.collectAsState()
    
    var currentView by remember { mutableStateOf(TrapView.LIST) }
    var showDeployDialog by remember { mutableStateOf(false) }
    
    // 위치 권한 요청
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            locationViewModel.startLocationFetch(context, weatherViewModel)
        }
    }
    
    // 위치 업데이트
    LaunchedEffect(userLatitude, userLongitude) {
        if (userLatitude != null && userLongitude != null) {
            Log.d("TrapLocationScreen", "위치 업데이트: 위도=${userLatitude}, 경도=${userLongitude}")
            trapViewModel.updateCurrentLocation(userLatitude!!, userLongitude!!)
        } else {
            Log.d("TrapLocationScreen", "위치 정보 null: 위도=${userLatitude}, 경도=${userLongitude}")
        }
    }
    
    // 위치 권한 체크 및 요청
    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            locationViewModel.startLocationFetch(context, weatherViewModel)
        }
    }
    
    // 네비게이션 모드일 때 연속적인 위치 추적 시작/중지
    LaunchedEffect(isNavigating) {
        if (isNavigating) {
            Log.d("TrapLocationScreen", "🧭 네비게이션 모드 진입 - 연속 위치 추적 시작")
            locationViewModel.startContinuousLocationTracking(context)
        } else {
            Log.d("TrapLocationScreen", "📍 네비게이션 모드 종료 - 연속 위치 추적 중지")
            locationViewModel.stopContinuousLocationTracking()
        }
    }
    
    // 근접 진동 효과
    LaunchedEffect(navigationInfo?.proximityLevel) {
        navigationInfo?.let { navInfo ->
            if (navInfo.proximityLevel != ProximityLevel.VERY_FAR) {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                val pattern = trapViewModel.getVibrationPattern(navInfo.proximityLevel)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        DynamicBackgroundOverlay(
            weatherData = null,
            alpha = 0.6f,
            forceTimeBasedBackground = true
        )

        if (isNavigating) {
            // Full screen navigation view - compass at the very top, no padding
            NavigationView(
                navigationInfo = navigationInfo,
                selectedTrap = selectedTrap,
                onStopNavigation = {
                    trapViewModel.stopNavigation()
                },
                currentLocation = if (userLatitude != null && userLongitude != null) {
                    Log.d("TrapLocationScreen", "NavigationView에 위치 전달: 위도=${userLatitude}, 경도=${userLongitude}")
                    Pair(userLatitude!!, userLongitude!!)
                } else {
                    Log.d("TrapLocationScreen", "NavigationView에 기본 위치 전달: ${currentLocation}")
                    currentLocation
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Show header only when not navigating
                TrapHeader(
                    onBack = onBack,
                    currentView = currentView,
                    onViewChange = { 
                        currentView = it
                        if (it != TrapView.NAVIGATE) {
                            trapViewModel.stopNavigation()
                        }
                    },
                    isNavigating = isNavigating
                )

                Spacer(modifier = Modifier.height(8.dp))

                TrapListView(
                    traps = traps,
                    isLocationLoading = isLocationLoading,
                    currentLocation = currentLocation,
                    onDeployTrap = { showDeployDialog = true },
                    onDeleteTrap = { trapViewModel.deleteTrap(it) },
                    onNavigateToTrap = { 
                        trapViewModel.startNavigation(it)
                    }
                )
            }
        }

        // Deploy trap dialog
        if (showDeployDialog) {
            DeployTrapDialog(
                currentLocation = currentLocation,
                isDeploying = isDeployingTrap,
                onDismiss = { showDeployDialog = false },
                onDeploy = { name, memo, baitType, depth ->
                    currentLocation?.let { (lat, lon) ->
                        trapViewModel.deployTrap(lat, lon, name, memo, baitType, depth)
                    }
                    showDeployDialog = false
                }
            )
        }
    }
}

enum class TrapView {
    LIST, NAVIGATE
}

@Composable
private fun TrapHeader(
    onBack: () -> Unit,
    currentView: TrapView,
    onViewChange: (TrapView) -> Unit,
    isNavigating: Boolean
) {
    Column {
        // Back button and title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Chip(
                onClick = onBack,
                label = {},
                modifier = Modifier.size(36.dp),
                colors = ChipDefaults.chipColors(
                    backgroundColor = Color.White.copy(alpha = 0.1f)
                ),
                icon = {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            Text(
                text = "통발 추적",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(36.dp))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
    }
}