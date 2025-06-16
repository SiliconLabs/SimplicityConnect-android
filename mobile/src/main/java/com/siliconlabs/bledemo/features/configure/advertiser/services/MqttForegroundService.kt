package com.siliconlabs.bledemo.features.configure.advertiser.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.siliconlabs.bledemo.R

class MqttForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Foreground service started
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopForeground(true)
        stopSelf()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "mqtt_foreground_channel"
        val channelName = "MQTT Foreground Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("MQTT Service")
            .setContentText("AWS IoT MQTT service is running in the foreground.")
            .setSmallIcon(R.drawable.ic_aws_iot_icon) // Replace with your icon
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}