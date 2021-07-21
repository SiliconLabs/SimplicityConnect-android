package com.siliconlabs.bledemo.Utils

import android.view.MenuItem
import com.siliconlabs.bledemo.Browser.Models.Logs.Log
import java.util.*

object Constants {
    const val NA = "N/A"
    const val BOTTOM_NAVI_DEVELOP = "Develop"
    const val BOTTOM_NAVI_DEMO = "Demo"

    var LOGS: MutableList<Log> = LinkedList()
    var ota_button: MenuItem? = null
    fun clearLogs() {
        LOGS.clear()
    }
}
