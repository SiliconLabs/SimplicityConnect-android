package com.siliconlabs.bledemo.features.scan.browser.models.logs

class CommonLog(value: String, deviceAddress: String) : Log() {
    init {
        logTime = getTime()
        logInfo = value
        super.deviceAddress = deviceAddress
    }
}
