package com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.blinky_thunderboard.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.blinky_thunderboard.model.LedRGBState


class BlinkyThunderboardViewModel : ViewModel() {

    val button0 = MutableLiveData(false)
    val button1 = MutableLiveData(false)
    val led0 = MutableLiveData(false)
    val led1 = MutableLiveData(false)
    val colorLed = MutableLiveData(LedRGBState(false, 0, 0, 0))

    val rgbLedMask = MutableLiveData<Int>()

    companion object {
        const val BUTTON_0_ON = 0x01
        const val BUTTON_1_ON = 0X04
        const val LED_0_ON = 0x01
        const val LED_1_ON = 0x04
    }
}