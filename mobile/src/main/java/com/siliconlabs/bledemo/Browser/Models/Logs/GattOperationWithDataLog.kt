package com.siliconlabs.bledemo.browser.models.logs

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.siliconlabs.bledemo.browser.models.LogType
import com.siliconlabs.bledemo.utils.Converters
import java.lang.StringBuilder

class GattOperationWithDataLog(operationName: String, gatt: BluetoothGatt, status: Int? = null, characteristic: BluetoothGattCharacteristic) : Log() {

    init {
        logTime = getTime()
        logInfo = getInfo(operationName, gatt, status, characteristic)
        logType = LogType.INFO
        deviceAddress = gatt.device.address
    }

    private fun getInfo(operationName: String, gatt: BluetoothGatt, status: Int?, characteristic: BluetoothGattCharacteristic): String {
        val values = characteristic.value
        val sb = StringBuilder()

        sb.append(operationName).append(", ")
                .append("device: ").append(gatt.device.address).append(", ")

        if (status != null) sb.append("status: ")
                .append(status).append(", ")

        sb.append(getGattDataInfo(values))

        return sb.toString()
    }

    private fun getGattDataInfo(values: ByteArray?): String {
        return if (values != null && values.isNotEmpty()) {
            val hexData = "0x".plus(Converters.bytesToHex(values).toUpperCase()).plus(" (hex)")
            val asciiData = Converters.getAsciiValue(values).plus(" (ascii)")
            val decimalData = Converters.getDecimalValue(values).plus("(dec)")

            StringBuilder().apply {
                append("data: ")
                        .append(hexData).append(", ")
                        .append(asciiData).append(", ")
                        .append(decimalData).append(".")
            }.toString()
        } else {
            "data: Empty data."
        }
    }

}
