package com.siliconlabs.bledemo.Bluetooth.BLE.Values

import android.bluetooth.BluetoothGattCharacteristic

interface ValueFactory<T> {
    fun create(value: BluetoothGattCharacteristic): T
}