package com.siliconlabs.bledemo.Browser.Models.Logs

import com.siliconlabs.bledemo.Browser.Models.LogType

class CommonLog(value: String, deviceAddress: String) : Log() {
    init {
        logTime = getTime()
        logInfo = value
        logType = LogType.INFO
        super.deviceAddress = deviceAddress
    }
}
