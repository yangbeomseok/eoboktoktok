package com.dive.weatherwatch.data

import java.text.SimpleDateFormat
import java.util.*

// 새로운 조위 데이터 모델 (prompt.txt 형식 기반)
data class TideData(
    val pThisDate: String,      // 날짜 (ex. 2025-7-21-월-6-27)
    val pName: String,          // 지역명
    val pArea: String,          // 지역ID
    val pMul: String,           // 물때 (ex. 4물)
    val pSun: String,           // 일출/일몰 (제외)
    val pMoon: String,          // 월출/월몰 (제외)
    val jowi1: String,          // 간만조시각1
    val jowi2: String,          // 간만조시각2
    val jowi3: String,          // 간만조시각3
    val jowi4: String           // 간만조시각4
)

// 간만조 정보를 파싱한 데이터
data class TideEvent(
    val time: String,           // 시각 (ex. "03:10")
    val height: Int,            // 물높이 (cm)
    val type: TideType,         // 만조/간조
    val difference: Int,        // 차이값 (ex. -13, +46)
    val timestamp: Long         // Unix timestamp
)

enum class TideType(val displayName: String) {
    HIGH_TIDE("만조"),  // 만조 ▲
    LOW_TIDE("간조")    // 간조 ▼
}

// 하루의 전체 조위 정보
data class DailyTideInfo(
    val date: String,
    val locationName: String,
    val tideEvents: List<TideEvent>,
    val waterPhase: String      // 물때 (ex. "4물")
)

// 조위 데이터 파서
object TideDataParser {
    
    fun parseJowiString(jowiString: String, date: String): TideEvent? {
        try {
            // "03:10 (17) ▼ -13" 형식 파싱
            val regex = """(\d{2}:\d{2})\s*\((\d+)\)\s*([▲▼])\s*([-+]?\d+)""".toRegex()
            val matchResult = regex.find(jowiString) ?: return null
            
            val time = matchResult.groupValues[1]
            val height = matchResult.groupValues[2].toInt()
            val symbol = matchResult.groupValues[3]
            val difference = matchResult.groupValues[4].toInt()
            
            val type = when (symbol) {
                "▲" -> TideType.HIGH_TIDE
                "▼" -> TideType.LOW_TIDE
                else -> return null
            }
            
            // 날짜와 시간을 조합하여 timestamp 생성
            val dateTimeString = "${date.split("-")[0]}-${date.split("-")[1].padStart(2, '0')}-${date.split("-")[2].padStart(2, '0')} $time:00"
            val timestamp = try {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateTimeString)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
            
            return TideEvent(
                time = time,
                height = height,
                type = type,
                difference = difference,
                timestamp = timestamp
            )
            
        } catch (e: Exception) {
            android.util.Log.e("TideDataParser", "Failed to parse jowi string: $jowiString", e)
            return null
        }
    }
    
    fun parseTideData(tideData: TideData): DailyTideInfo {
        val events = listOfNotNull(
            parseJowiString(tideData.jowi1, tideData.pThisDate),
            parseJowiString(tideData.jowi2, tideData.pThisDate),
            parseJowiString(tideData.jowi3, tideData.pThisDate),
            parseJowiString(tideData.jowi4, tideData.pThisDate)
        ).sortedBy { it.timestamp }
        
        return DailyTideInfo(
            date = tideData.pThisDate,
            locationName = tideData.pName,
            tideEvents = events,
            waterPhase = tideData.pMul
        )
    }
}

// 샘플 데이터 생성기
object TideSampleDataGenerator {
    
    fun generateSampleData(): List<TideData> {
        val sampleData = mutableListOf<TideData>()
        val calendar = Calendar.getInstance()
        
        // 오늘과 내일 데이터 (prompt.txt에서 제공된 샘플)
        sampleData.add(
            TideData(
                pThisDate = "2025-8-13-화-7-19",
                pName = "부산",
                pArea = "1",
                pMul = "4물",
                pSun = "05:51/19:00",
                pMoon = "07:32/19:59",
                jowi1 = "03:10 (17) ▼ -13",
                jowi2 = "09:36 (127) ▲ +46",
                jowi3 = "15:26 (21) ▼ -1",
                jowi4 = "21:45 (136) ▲ +54"
            )
        )
        
        sampleData.add(
            TideData(
                pThisDate = "2025-8-14-수-7-20",
                pName = "부산",
                pArea = "1",
                pMul = "5물",
                pSun = "05:52/19:00",
                pMoon = "08:15/20:45",
                jowi1 = "03:45 (15) ▼ -18",
                jowi2 = "10:12 (132) ▲ +51",
                jowi3 = "16:02 (19) ▼ -6",
                jowi4 = "22:21 (141) ▲ +59"
            )
        )
        
        // 추가 5일 샘플 데이터 생성
        val waterPhases = listOf("6물", "7물", "8물", "9물", "10물")
        val daysOfWeek = listOf("목", "금", "토", "일", "월")
        
        for (i in 0 until 5) {
            val date = calendar.apply { add(Calendar.DAY_OF_MONTH, 1) }.time
            val dateFormat = SimpleDateFormat("yyyy-M-d", Locale.getDefault())
            val dayOfWeek = daysOfWeek[i]
            val lunarMonth = 7
            val lunarDay = 21 + i
            
            // 조위 시간과 높이를 현실적으로 생성
            val baseTime1 = (3 + i * 0.5).toInt()
            val baseTime2 = (9 + i * 0.7).toInt()
            val baseTime3 = (15 + i * 0.6).toInt()
            val baseTime4 = (21 + i * 0.4).toInt()
            
            val height1 = (15 + i * 2)
            val height2 = (125 + i * 3)
            val height3 = (18 + i * 1)
            val height4 = (138 + i * 2)
            
            sampleData.add(
                TideData(
                    pThisDate = "${dateFormat.format(date)}-$dayOfWeek-$lunarMonth-$lunarDay",
                    pName = "부산",
                    pArea = "1",
                    pMul = waterPhases[i],
                    pSun = "05:${52 + i}/19:0${i % 10}",
                    pMoon = "0${8 + i}:${20 + i * 5}/2${0 + i}:${30 + i * 3}",
                    jowi1 = "${String.format("%02d", baseTime1)}:${String.format("%02d", 10 + i * 8)} ($height1) ▼ ${-15 - i}",
                    jowi2 = "${String.format("%02d", baseTime2)}:${String.format("%02d", 30 + i * 6)} ($height2) ▲ +${45 + i * 3}",
                    jowi3 = "${String.format("%02d", baseTime3)}:${String.format("%02d", 45 + i * 4)} ($height3) ▼ ${-5 - i}",
                    jowi4 = "${String.format("%02d", baseTime4)}:${String.format("%02d", 15 + i * 7)} ($height4) ▲ +${52 + i * 2}"
                )
            )
        }
        
        return sampleData
    }
}