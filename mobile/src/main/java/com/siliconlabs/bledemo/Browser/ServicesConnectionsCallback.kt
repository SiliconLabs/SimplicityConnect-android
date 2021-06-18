package com.siliconlabs.bledemo.browser

import android.bluetooth.BluetoothDevice
import com.siliconlabs.bledemo.bluetooth.ble.BluetoothDeviceInfo

interface ServicesConnectionsCallback {
    fun onDisconnectClicked(deviceInfo: BluetoothDeviceInfo?)
    fun onDeviceClicked(device: BluetoothDevice)
}
