package com.siliconlabs.bledemo.features.scan.browser.services

import android.app.Service
import android.content.Intent
import android.os.*
import android.widget.Toast
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.utils.Constants
import java.util.*

class ShareLogServices : Service() {
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null

    // Handler that receives messages from the thread
    private inner class ServiceHandler(looper: Looper?) : Handler(looper!!) {
        override fun handleMessage(msg: Message) {
            val logsText = parseLogsToText()
            if (logsText == null) {
                stopSelf(msg.arg1)
                return
            }
            val sendIntent = Intent()
            sendIntent.action = Intent.ACTION_SEND
            sendIntent.putExtra(Intent.EXTRA_TEXT, logsText)
            sendIntent.type = "text/plain"
            val shareIntent = Intent.createChooser(sendIntent,
                resources.getString(R.string.app_name_simplicity_Connect) + " LOGS")
            shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(shareIntent)
            stopSelf(msg.arg1)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val thread = HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND)
        thread.start()

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.looper
        serviceHandler = ServiceHandler(serviceLooper)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Toast.makeText(this, resources.getString(R.string.Preparing_logs_please_wait), Toast.LENGTH_SHORT).show()

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        val msg = serviceHandler!!.obtainMessage()
        msg.arg1 = startId
        serviceHandler!!.sendMessage(msg)

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    private fun parseLogsToText(): String? {
        val sb = StringBuilder()
        return try {
            Constants.LOGS.forEach {
                sb.append(it.logTime).append(" ")
                sb.append(it.logInfo)
                sb.append("\n")
            }
            sb.toString()
        } catch (ex: ConcurrentModificationException) {
            Toast.makeText(this, resources.getString(R.string.Logs_cannot_be_generated), Toast.LENGTH_SHORT).show()
            null
        }
    }
}
