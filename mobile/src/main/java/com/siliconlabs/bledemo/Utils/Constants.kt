package com.siliconlabs.bledemo.utils

import android.view.MenuItem
import com.siliconlabs.bledemo.browser.models.logs.Log
import java.util.*

object Constants {
    /* Custom services names */
    const val OTA_SERVICE = "OTA Service"
    const val BLINKY_EXAMPLE = "Blinky Example"
    const val THROUGHPUT_TEST_SERVICE = "Throughput Test Service"
    const val THROUGHPUT_INFORMATION_SERVICE = "Throughput Information Service"

    /* Custom characteristics names */
    const val OTA_CONTROL = "OTA Control Attribute"
    const val OTA_DATA = "OTA Data"
    const val FW_VERSION = "Fw version"
    const val OTA_VERSION = "OTA Version"
    const val BOOTLOADER_VERSION = "Bootloader Version"
    const val APPLICATION_VERSION = "Application Version"

    const val LED_CONTROL = "LED Control"
    const val REPORT_BUTTON = "Report Button"

    const val THROUGHPUT_INDICATIONS = "Indications"
    const val THROUGHPUT_NOTIFICATIONS = "Notifications"
    const val THROUGHPUT_TRANSMISSION_ON = "Transmission ON"
    const val THROUGHPUT_RESULT = "Throughput Result"

    const val THROUGHPUT_PHY_STATUS = "Connection PHY"
    const val THROUGHPUT_CONNECTION_INTERVAL = "Connection Interval"
    const val THROUGHPUT_SLAVE_LATENCY = "Slave Latency"
    const val THROUGHPUT_SUPERVISION_TIMEOUT = "Supervision Timeout"
    const val THROUGHPUT_PDU_SIZE = "PDU Size"
    const val THROUGHPUT_MTU_SIZE = "MTU Size"

    const val NA = "N/A"
    const val BOTTOM_NAVI_DEVELOP = "Develop"
    const val BOTTOM_NAVI_DEMO = "Demo"

    var LOGS: MutableList<Log> = LinkedList()
    var ota_button: MenuItem? = null
    fun clearLogs() {
        LOGS.clear()
    }
}
