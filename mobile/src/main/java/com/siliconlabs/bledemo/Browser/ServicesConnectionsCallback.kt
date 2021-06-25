package com.siliconlabs.bledemo.Browser

import com.siliconlabs.bledemo.Bluetooth.BLE.BluetoothDeviceInfo

interface ServicesConnectionsCallback {
    fun onDisconnectClicked(deviceInfo: BluetoothDeviceInfo?)
    fun onDeviceClicked(device: BluetoothDeviceInfo?)
}