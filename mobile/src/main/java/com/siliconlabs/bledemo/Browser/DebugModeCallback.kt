package com.siliconlabs.bledemo.Browser

import com.siliconlabs.bledemo.Bluetooth.BLE.BluetoothDeviceInfo

interface DebugModeCallback {
    fun connectToDevice(device: BluetoothDeviceInfo?)
    fun addToFavorite(deviceAddress: String)
    fun removeFromFavorite(deviceAddress: String)
    fun addToTemporaryFavorites(deviceAddress: String)
    fun updateCountOfConnectedDevices()
}