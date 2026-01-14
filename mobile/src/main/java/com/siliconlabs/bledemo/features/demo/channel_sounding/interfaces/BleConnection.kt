package com.siliconlabs.bledemo.features.demo.channel_sounding.interfaces

interface BleConnection {
    fun waitForPsm(): Int {
        throw UnsupportedOperationException("waitForPsm not implemented")
    }

    fun notifyPsm(psm: Int) {
        throw UnsupportedOperationException("notifyPsm not implemented")
    }
}