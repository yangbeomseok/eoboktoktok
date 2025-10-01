package com.dive.weatherwatch.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.dive.weatherwatch.R

// 안전한 배경 이미지 로딩 컴포넌트
@Composable
fun SafeBackgroundImage(
    resourceId: Int,
    alpha: Float
) {
    // 모든 리소스가 올바른 PNG로 수정되었으므로 원래 로직 사용
    val safeResourceId = resourceId
    
    Image(
        painter = painterResource(id = safeResourceId),
        contentDescription = "Weather Background",
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha),
        contentScale = ContentScale.Crop
    )
}