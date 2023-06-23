package com.siliconlabs.bledemo.features.demo.esl_demo.model

import android.bluetooth.BluetoothAdapter

data class QrCodeData(
    val fullCommand: String,
    val command: String,
    val address: String,
    val addressType: String? = null, /* Present only some in some versions of QR codes */
    val passcode: Int? = null /* To be added in the future (as of 23.03.2023) on the embedded side */
) {

    companion object {
        fun decode(qrCodeText: String) : QrCodeData {
            val data = qrCodeText.split(" ")

            if (data.size == 1) return QrCodeData(qrCodeText, data[0], "unknown")
            if (data.size == 2) return QrCodeData(qrCodeText, data[0], data[1])
            if (data.size == 3) return QrCodeData(qrCodeText, data[0], data[1], data[2])

            return QrCodeData(qrCodeText, data[0], data[1], data[2], data[3].toInt())
        }
    }

    fun isValid() : Boolean {
        if (command != EslCommand.CONNECT.message) return false
        if (!BluetoothAdapter.checkBluetoothAddress(address.uppercase())) return false

        return true
    }
}