package com.siliconlabs.bledemo.features.scan.browser.models.logs

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import java.text.SimpleDateFormat
import java.util.*

abstract class Log(protected val gatt: BluetoothGatt) {

    val deviceAddress: String = gatt.device.address
    val logTime = getTime()
    val logInfo by lazy { generateLogInfo() }

    abstract fun generateLogInfo() : String

    @SuppressLint("MissingPermission")
    protected fun parseDeviceInfo() : String {
        return "\"${gatt.device.name}\" (${gatt.device.address})"
    }

    protected fun parseStatus(status: Int): String {
        return if (status == BluetoothGatt.GATT_SUCCESS) "success"
        else status.toString()
    }

    private fun getTime(): String {
        val calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return formatter.format(calendar.time)
    }

    fun getDeviceName(name: String?): String {
        return name ?: "N/A"
    }
}
