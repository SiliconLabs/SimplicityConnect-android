package com.siliconlabs.bledemo.features.scan.browser.models.logs

import android.bluetooth.BluetoothGatt
import com.siliconlabs.bledemo.utils.Converters
import java.util.*
import kotlin.text.StringBuilder

class GattOperationWithDataLog(
        gatt: BluetoothGatt,
        type: Type,
        private val status: Int? = null,
        private val uuid: UUID,
        private val value: ByteArray?
) : GattOperationLog(gatt, type) {

    override fun generateLogInfo(): String {
        return StringBuilder().apply {
            append(parseType())
            status?.let { append(", status: ${parseStatus(it)}") }
            append("\nUUID: ${uuid.toString().lowercase(Locale.getDefault())}")
            append(", ${getGattDataInfo()}")
        }.toString()
    }

    private fun getGattDataInfo(): String {
        return if (null == value) "data: Empty data."
        else {
            val hexData = "0x".plus(Converters.bytesToHex(value).uppercase(Locale.getDefault())).plus(" (hex)")
            val asciiData = Converters.getAsciiValue(value).plus(" (ascii)")
            val decimalData = Converters.getDecimalValue(value).plus("(dec)")

            StringBuilder().apply {
                append("data: ")
                        .append(hexData).append(", ")
                        .append(asciiData).append(", ")
                        .append(decimalData).append(".")
            }.toString()
        }
    }

}
