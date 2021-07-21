package com.siliconlabs.bledemo.Browser

import android.bluetooth.BluetoothDevice
import com.siliconlabs.bledemo.Bluetooth.BLE.BluetoothDeviceInfo

interface ServicesConnectionsCallback {
    fun onDisconnectClicked(deviceInfo: BluetoothDeviceInfo?)
    fun onDeviceClicked(device: BluetoothDevice)
}
