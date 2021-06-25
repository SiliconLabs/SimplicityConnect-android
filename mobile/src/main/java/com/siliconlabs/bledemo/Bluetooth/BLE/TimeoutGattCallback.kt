package com.siliconlabs.bledemo.Bluetooth.BLE

import android.bluetooth.BluetoothGattCallback

abstract class TimeoutGattCallback : BluetoothGattCallback() {
    open fun onTimeout() {}
}