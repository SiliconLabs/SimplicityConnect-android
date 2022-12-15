package com.siliconlabs.bledemo.features.scan.browser.models.logs

import android.bluetooth.BluetoothDevice

class TimeoutLog : Log {
    constructor(device: BluetoothDevice) {
        logTime = getTime()
        logInfo = (device.address + " (" + getDeviceName(device.name) + "): "
                + "Connection timeout")
        deviceAddress = device.address
    }

    constructor() {
        logTime = getTime()
        logInfo = "Connection timeout"
    }
}
