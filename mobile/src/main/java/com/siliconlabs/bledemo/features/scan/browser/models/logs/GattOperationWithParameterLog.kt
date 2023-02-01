package com.siliconlabs.bledemo.features.scan.browser.models.logs

import android.bluetooth.BluetoothGatt

class GattOperationWithParameterLog(
        gatt: BluetoothGatt,
        operationName: Type,
        private val status: Int? = null,
        private val parameterMessage: String? = null
) : GattOperationLog(gatt, operationName) {

    override fun generateLogInfo(): String {
        return StringBuilder().apply {
            append(parseType())
            status?.let { append(", status: ${parseStatus(it)}") }
            parameterMessage?.let { append("\n$it") }
        }.toString()
    }


}