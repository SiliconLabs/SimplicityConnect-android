package com.siliconlabs.bledemo.Browser.Services

import android.app.Service
import android.content.Intent
import android.os.*
import android.widget.Toast
import com.siliconlabs.bledemo.Browser.Fragments.LoggerFragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.Utils.Constants
import java.util.*

class ShareLogServices : Service() {
    private var isFiltering = false
    private var filteringPhrase: String? = ""
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
            val shareIntent = Intent.createChooser(sendIntent, resources.getString(R.string.app_name_EFR_Connect) + " LOGS")
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
        if (intent.extras != null) {
            isFiltering = intent.extras!!.getBoolean(LoggerFragment.IS_FILTERING_EXTRA)
            if (isFiltering) {
                filteringPhrase = intent.extras!!.getString(LoggerFragment.FILTERING_PHRASE_EXTRA)
                if (filteringPhrase != null) filteringPhrase = filteringPhrase!!.toLowerCase()
            } else {
                filteringPhrase = ""
            }
        }

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
        return if (isFiltering) {
            try {
                for (log in Constants.LOGS) {
                    if (log.logTime.toLowerCase().contains(filteringPhrase!!) || log.logInfo.toLowerCase().contains(filteringPhrase!!)) {
                        sb.append(log.logTime)
                        sb.append(log.logInfo)
                        sb.append("\n")
                    }
                }
                sb.toString()
            } catch (ex: ConcurrentModificationException) {
                Toast.makeText(this, resources.getString(R.string.Logs_cannot_be_generated), Toast.LENGTH_SHORT).show()
                null
            }
        } else {
            try {
                for (log in Constants.LOGS) {
                    sb.append(log.logTime)
                    sb.append(log.logInfo)
                    sb.append("\n")
                }
                sb.toString()
            } catch (ex: ConcurrentModificationException) {
                Toast.makeText(this, resources.getString(R.string.Logs_cannot_be_generated), Toast.LENGTH_SHORT).show()
                null
            }
        }
    }
}