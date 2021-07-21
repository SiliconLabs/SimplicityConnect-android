package com.siliconlabs.bledemo.Browser.Models.Logs

import com.siliconlabs.bledemo.Browser.Models.LogType

class DisconnectByButtonLog(deviceAddress: String) : Log() {
    init {
        logTime = getTime()
        logInfo = "$deviceAddress Disconnected on UI"
        logType = LogType.INFO
        super.deviceAddress = deviceAddress
    }
}
