package com.siliconlabs.bledemo.features.configure.advertiser.services

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.siliconlabs.bledemo.features.configure.advertiser.fragments.AdvertiserFragment
import com.siliconlabs.bledemo.features.configure.advertiser.utils.AdvertiserStorage
import com.siliconlabs.bledemo.R

class AdvertiserService : Service() {
    companion object {
        const val CHANNEL_ID = "ForegroundAdvertiserService"

        fun startService(context: Context) {
            val serviceIntent = Intent(context, AdvertiserService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }

        fun stopService(context: Context) {
            AdvertiserStorage(context).clearRunningAdvertisers()
            val serviceIntent = Intent(context, AdvertiserService::class.java)
            context.stopService(serviceIntent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(receiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = prepareNotification()
        startForeground(1, notification)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(receiver)
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(CHANNEL_ID, "Advertiser Service Channel", NotificationManager.IMPORTANCE_DEFAULT)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }

    private fun prepareNotification(): Notification {
        createNotificationChannel()
        val notificationIntent = Intent(this, AdvertiserFragment::class.java)
        val pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("EFR Connect")
                .setContentText("Advertiser is running...")
                .setSmallIcon(R.mipmap.efr_redesign_launcher)
                .setContentIntent(pendingIntent)
                .setShowWhen(false)
                .build()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)

                when (state) {
                    BluetoothAdapter.STATE_OFF -> { context?.let {
                        AdvertiserStorage(it).clearRunningAdvertisers()
                        stopSelf()
                    } }
                }
            }
        }
    }
}