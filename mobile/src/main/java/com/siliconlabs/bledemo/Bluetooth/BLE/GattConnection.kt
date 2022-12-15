package com.siliconlabs.bledemo.bluetooth.ble

import android.bluetooth.BluetoothGatt

data class GattConnection(val address: String) {

    var gatt: BluetoothGatt? = null
    var hasRssiUpdates: Boolean = false

    constructor(gatt: BluetoothGatt, hasRssiUpdates: Boolean
    ) : this (gatt.device.address) {
        this.gatt = gatt
        this.hasRssiUpdates = hasRssiUpdates
    }

}

