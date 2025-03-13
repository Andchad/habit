package com.andchad.habit.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.andchad.habit.R
import com.andchad.habit.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmService : Service() {

    @Inject
    lateinit var alarmUtils: AlarmUtils

    companion object {
        private const val TAG = "AlarmService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "habit_alarm_service"

        // Actions
        const val ACTION_START_SERVICE = "com.andchad.habit.ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.andchad.habit.ACTION_STOP_SERVICE"
        const val ACTION_TRIGGER_ALARM = "com.andchad.habit.ACTION_TRIGGER_ALARM"

        // Extras
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_HABIT_NAME = "habit_name"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AlarmService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                Log.d(TAG, "Starting alarm service")
                try {
                    // Create notification first
                    val notification = createNotification("Habit Alarm Service Active")

                    // Start as foreground service
                    startForeground(NOTIFICATION_ID, notification)
                    Log.d(TAG, "Started foreground service successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting foreground service: ${e.message}", e)
                }
                return START_STICKY
            }
            // Other handlers remain the same
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Habit Alarm Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ensures habit alarms remain active"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Hab-it!")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your notification icon
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}