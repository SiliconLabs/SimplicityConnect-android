package com.siliconlabs.bledemo.Browser

import android.bluetooth.BluetoothDevice

interface DebugModeCallback {
    fun connectToDevice(device: BluetoothDevice)
    fun addToFavorite(deviceAddress: String)
    fun removeFromFavorite(deviceAddress: String)
    fun addToTemporaryFavorites(deviceAddress: String)
    fun updateCountOfConnectedDevices()
}
