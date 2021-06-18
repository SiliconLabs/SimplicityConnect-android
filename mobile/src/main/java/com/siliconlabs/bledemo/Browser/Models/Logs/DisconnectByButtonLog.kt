package com.siliconlabs.bledemo.browser.models.logs

import com.siliconlabs.bledemo.browser.models.LogType

class DisconnectByButtonLog(deviceAddress: String) : Log() {
    init {
        logTime = getTime()
        logInfo = "$deviceAddress Disconnected on UI"
        logType = LogType.INFO
        super.deviceAddress = deviceAddress
    }
}
