package com.siliconlabs.bledemo.features.scan.browser.models.logs

import android.bluetooth.BluetoothGatt

class ServicesDiscoveredLog(gatt: BluetoothGatt, status: Int) : Log() {
    companion object {
        private fun parseStatus(status: Int): String {
            return if (status == BluetoothGatt.GATT_SUCCESS) {
                "Successfully discovered services"
            } else "Unsuccessfully discovered services with status: $status"
        }
    }

    init {
        logTime = getTime()
        logInfo = (gatt.device.address + " (" + getDeviceName(gatt.device.name) + "): "
                + parseStatus(status))
        deviceAddress = gatt.device.address
    }
}
