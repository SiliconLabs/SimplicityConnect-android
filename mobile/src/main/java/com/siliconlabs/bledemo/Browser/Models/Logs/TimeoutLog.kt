package com.siliconlabs.bledemo.Browser.Models.Logs

import android.bluetooth.BluetoothDevice
import com.siliconlabs.bledemo.Browser.Models.LogType

class TimeoutLog : Log {
    constructor(device: BluetoothDevice) {
        logTime = getTime()
        logInfo = (device.address + " (" + getDeviceName(device.name) + "): "
                + "Connection timeout")
        logType = LogType.INFO
        deviceAddress = device.address
    }

    constructor() {
        logTime = getTime()
        logInfo = "Connection timeout"
        logType = LogType.INFO
    }
}