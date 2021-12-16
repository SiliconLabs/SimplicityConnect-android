package com.siliconlabs.bledemo.blinky_thunderboard.presenters

import com.siliconlabs.bledemo.thunderboard.base.BaseViewListener
import com.siliconlabs.bledemo.thunderboard.model.LedRGBState
import com.siliconlabs.bledemo.thunderboard.model.ThunderBoardDevice

interface BlinkyThunderboardListener : BaseViewListener {
    fun setButton0State(state: Int)
    fun setButton1State(state: Int)
    fun setLed0State(state: Int)
    fun setLed1State(state: Int)
    fun setColorLEDsValue(colorLEDsValue: LedRGBState)
    fun setPowerSource(powerSource: ThunderBoardDevice.PowerSource)
}