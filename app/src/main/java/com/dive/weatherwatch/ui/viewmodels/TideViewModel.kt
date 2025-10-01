package com.dive.weatherwatch.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dive.weatherwatch.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class TideViewModel : ViewModel() {

    private val _weeklyTideInfo = MutableStateFlow<List<DailyTideInfo>>(emptyList())
    val weeklyTideInfo: StateFlow<List<DailyTideInfo>> = _weeklyTideInfo

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun fetchTideData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                android.util.Log.d("TideViewModel", "Loading new tide data format...")
                
                // 새로운 샘플 데이터 로딩 (일주일치)
                val sampleData = TideSampleDataGenerator.generateSampleData()
                android.util.Log.d("TideViewModel", "Generated ${sampleData.size} days of sample data")
                
                // 모든 데이터를 파싱해서 일주일치 데이터 생성
                val weeklyData = sampleData.map { tideData ->
                    TideDataParser.parseTideData(tideData)
                }
                
                _weeklyTideInfo.value = weeklyData
                
                android.util.Log.d("TideViewModel", "Loaded weekly tide data: ${weeklyData.size} days")
                weeklyData.forEach { dailyInfo ->
                    android.util.Log.d("TideViewModel", "Day: ${dailyInfo.date}, Events: ${dailyInfo.tideEvents.size}, Phase: ${dailyInfo.waterPhase}")
                }

            } catch (e: Exception) {
                _errorMessage.value = "조위 데이터 로딩 실패: ${e.message}"
                android.util.Log.e("TideViewModel", "Error loading tide data", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

}