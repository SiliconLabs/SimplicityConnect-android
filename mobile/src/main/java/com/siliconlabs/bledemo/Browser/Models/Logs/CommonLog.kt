package com.siliconlabs.bledemo.browser.models.logs

import com.siliconlabs.bledemo.browser.models.LogType

class CommonLog(value: String, deviceAddress: String) : Log() {
    init {
        logTime = getTime()
        logInfo = value
        logType = LogType.INFO
        super.deviceAddress = deviceAddress
    }
}
