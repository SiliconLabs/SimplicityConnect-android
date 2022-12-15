package com.siliconlabs.bledemo.features.scan.browser.models.logs


class DisconnectByButtonLog(deviceAddress: String) : Log() {
    init {
        logTime = getTime()
        logInfo = "$deviceAddress Disconnected on UI"
        super.deviceAddress = deviceAddress
    }
}
