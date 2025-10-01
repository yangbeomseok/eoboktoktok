package com.dive.weatherwatch.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dive.weatherwatch.R
import com.dive.weatherwatch.data.FishingHotspot

class FishingHotspotNotificationService {
    companion object {
        private const val CHANNEL_ID = "fishing_hotspot_channel"
        private const val CHANNEL_NAME = "낚시 핫스팟 알림"
        private const val NOTIFICATION_ID = 2001
        
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "낚시 핫스팟 근처 접근 시 알림"
                    enableVibration(true)
                }
                
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
        
        fun showFishingSpotNotification(context: Context, spot: FishingHotspot, distance: Double) {
            val title = "🛰️ 위성 신호 포착!"
            val message = getConcentrationMessage(spot.medianConcentration, spot.grade)
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .build()
            
            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
        
        private fun getConcentrationMessage(concentration: Double, grade: String): String {
            return when {
                concentration >= 4.0 -> "주변 해역에서 과거 평균 농도 ${String.format("%.2f", concentration)}의 최상급 엽록소 수치가 관측된 '황금 어장'이 있습니다. 대어를 낚을 절호의 기회입니다!"
                concentration >= 3.0 -> "주변 해역에서 과거 평균 농도 ${String.format("%.2f", concentration)}의 우수한 엽록소 수치가 관측된 '유망 포인트'가 있습니다. 활발한 먹이 활동이 기대됩니다."
                else -> "주변 해역에서 과거 평균 농도 ${String.format("%.2f", concentration)}의 안정적인 엽록소 수치가 관측된 곳이 있습니다. 꾸준한 조과를 노려볼 만한 포인트입니다."
            }
        }
    }
}