package com.siliconlabs.bledemo.bluetooth.ble

class ConnectedDeviceInfo(val connection: GattConnection) {
    var bluetoothInfo = BluetoothDeviceInfo(connection.gatt!!.device)
}