package com.siliconlabs.bledemo.Browser.Models.Logs

import com.siliconlabs.bledemo.Browser.Models.LogType
import java.text.SimpleDateFormat
import java.util.*

open class Log {
    var logTime: String = ""
    var logInfo: String = ""
    var logType: LogType? = null
    var deviceAddress: String = ""

    companion object {
        fun getTime(): String {
            val calendar = Calendar.getInstance()
            val formatter = SimpleDateFormat("HH:mm:ss.SSS")
            return formatter.format(calendar.time)
        }

        fun getDeviceName(name: String?): String {
            return name ?: "N/A"
        }
    }
}