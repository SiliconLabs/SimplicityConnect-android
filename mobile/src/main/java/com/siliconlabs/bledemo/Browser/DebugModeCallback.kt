package com.siliconlabs.bledemo.browser

import android.bluetooth.BluetoothDevice

interface DebugModeCallback {
    fun connectToDevice(device: BluetoothDevice)
    fun addToFavorite(deviceAddress: String)
    fun removeFromFavorite(deviceAddress: String)
    fun addToTemporaryFavorites(deviceAddress: String)
    fun updateCountOfConnectedDevices()
}
