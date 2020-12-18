package com.siliconlabs.bledemo.Browser.Models.Logs

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile
import com.siliconlabs.bledemo.Browser.Models.LogType

class ConnectionStateChangeLog(gatt: BluetoothGatt, status: Int, newState: Int) : Log() {
    private fun parseNewState(newState: Int): String {
        return when (newState) {
            BluetoothProfile.STATE_DISCONNECTED -> "State Disconnected"
            BluetoothProfile.STATE_CONNECTING -> "State Connecting"
            BluetoothProfile.STATE_CONNECTED -> "Successful connect to device"
            BluetoothProfile.STATE_DISCONNECTING -> "State Disconnecting"
            else -> newState.toString()
        }
    }

    private fun parseStatus(status: Int): String {
        return if (status == BluetoothGatt.GATT_SUCCESS) {
            "Success"
        } else status.toString()
    }

    init {
        logTime = getTime()
        logInfo = (gatt.device.address + " (" + getDeviceName(gatt.device.name) + "): "
                + parseNewState(newState) + ". Status: " + parseStatus(status))
        logType = LogType.INFO
        deviceAddress = gatt.device.address
    }
}