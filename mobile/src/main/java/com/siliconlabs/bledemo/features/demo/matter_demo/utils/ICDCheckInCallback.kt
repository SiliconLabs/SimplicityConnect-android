package com.siliconlabs.bledemo.features.demo.matter_demo.utils

import chip.devicecontroller.ICDClientInfo

interface ICDCheckInCallback {
    fun notifyCheckInMessage(info: ICDClientInfo)
}