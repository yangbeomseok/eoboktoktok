package com.dive.weatherwatch.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.dive.weatherwatch.MainActivity
import com.dive.weatherwatch.R
import com.dive.weatherwatch.data.TideEvent
import com.dive.weatherwatch.data.TideType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class TideNotificationService {
    companion object {
        const val CHANNEL_ID = "tide_notifications"
        const val NOTIFICATION_ID = 1001
        private const val WORK_TAG = "tide_notification_work"
        
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "조위 알림"
                val descriptionText = "만조/간조 30분 전 알림"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    enableVibration(true)
                    setShowBadge(true)
                }
                
                val notificationManager: NotificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
        
        fun scheduleTideNotifications(context: Context, tideEvents: List<TideEvent>) {
            // 기존 알림 작업 취소
            WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
            
            val currentTime = System.currentTimeMillis()
            val thirtyMinutesInMillis = 30 * 60 * 1000L
            
            tideEvents.forEach { event ->
                val notificationTime = event.timestamp - thirtyMinutesInMillis
                
                // 알림 시간이 현재 시간보다 미래인 경우에만 스케줄
                if (notificationTime > currentTime) {
                    val delay = notificationTime - currentTime
                    
                    val inputData = workDataOf(
                        "tideType" to event.type.name,
                        "tideHeight" to event.height.toString(),
                        "tideTime" to event.timestamp.toString()
                    )
                    
                    val notificationWork = OneTimeWorkRequestBuilder<TideNotificationWorker>()
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(inputData)
                        .addTag(WORK_TAG)
                        .build()
                    
                    WorkManager.getInstance(context).enqueue(notificationWork)
                    
                    android.util.Log.d("TideNotification", 
                        "스케줄됨: ${event.type} ${event.height}cm - ${
                            SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(event.timestamp))
                        } (${delay/1000/60}분 후 알림)")
                }
            }
        }
        
        fun showTideNotification(
            context: Context, 
            tideType: TideType, 
            height: Float, 
            tideTime: Long
        ) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val tideTimeString = timeFormat.format(Date(tideTime))
            
            val title = when (tideType) {
                TideType.HIGH_TIDE -> "🌊 만조 30분 전"
                TideType.LOW_TIDE -> "🏖️ 간조 30분 전"
            }
            
            val message = "${tideTimeString}에 ${tideType.displayName} ${height}cm 예정"
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("navigate_to", "tide")
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.logo_white)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .build()
            
            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
                android.util.Log.d("TideNotification", "알림 표시: $title - $message")
            } catch (e: SecurityException) {
                android.util.Log.e("TideNotification", "알림 권한 없음: ${e.message}")
            }
        }
    }
}

class TideNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.Main) {
        try {
            val tideTypeString = inputData.getString("tideType") ?: return@withContext Result.failure()
            val tideHeight = inputData.getString("tideHeight")?.toFloatOrNull() ?: return@withContext Result.failure()
            val tideTime = inputData.getString("tideTime")?.toLongOrNull() ?: return@withContext Result.failure()
            
            val tideType = when (tideTypeString) {
                "HIGH_TIDE" -> TideType.HIGH_TIDE
                "LOW_TIDE" -> TideType.LOW_TIDE
                else -> return@withContext Result.failure()
            }
            
            TideNotificationService.showTideNotification(
                applicationContext,
                tideType,
                tideHeight,
                tideTime
            )
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("TideNotificationWorker", "알림 작업 실패: ${e.message}")
            Result.failure()
        }
    }
}