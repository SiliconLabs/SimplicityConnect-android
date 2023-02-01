package com.siliconlabs.bledemo.features.scan.browser.models.logs

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile

class ConnectionStateChangeLog(
        gatt: BluetoothGatt,
        private val status: Int,
        private val newState: Int
) : Log(gatt) {


    override fun generateLogInfo() : String {
        return StringBuilder().apply {
            append("Connection state changed for device ${parseDeviceInfo()}")
            append("\nChange status: ${parseStatus(status)}, new state: ${parseNewState()}")
        }.toString()
    }

    private fun parseNewState(): String {
        return when (newState) {
            BluetoothProfile.STATE_DISCONNECTED -> "Disconnected"
            BluetoothProfile.STATE_CONNECTING -> "Connecting"
            BluetoothProfile.STATE_CONNECTED -> "Connected"
            BluetoothProfile.STATE_DISCONNECTING -> "Disconnecting"
            else -> newState.toString()
        }
    }
}
