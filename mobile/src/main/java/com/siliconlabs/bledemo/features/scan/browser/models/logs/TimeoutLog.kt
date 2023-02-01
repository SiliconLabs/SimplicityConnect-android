package com.siliconlabs.bledemo.features.scan.browser.models.logs

import android.bluetooth.BluetoothGatt

class TimeoutLog(gatt: BluetoothGatt) : Log(gatt) {

    override fun generateLogInfo(): String {
        return StringBuilder().apply {
            append("Connection timeout for device ${parseDeviceInfo()}")
        }.toString()
    }
}
