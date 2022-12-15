package com.siliconlabs.bledemo.features.demo.thunderboard_demos.base.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.base.models.ThunderBoardDevice

class StatusViewModel : ViewModel() {

    val thunderboardDevice = MutableLiveData<ThunderBoardDevice>()
    val state = MutableLiveData<Int>()

    fun setDeviceName(name: String) {
        thunderboardDevice.value?.apply {
            this.name = name
            thunderboardDevice.postValue(this)
        }
    }

    fun setModelNumber(modelNumber: String) {
        thunderboardDevice.value?.apply {
            this.modelNumber = modelNumber
            thunderboardDevice.postValue(this)
        }
    }

    fun setPowerSource(powerSource: ThunderBoardDevice.PowerSource) {
        thunderboardDevice.value?.apply {
            this.powerSource = powerSource
            thunderboardDevice.postValue(this)
        }
    }

    fun setBatteryLevel(batteryLevel: Int) {
        thunderboardDevice.value?.apply {
            this.batteryLevel = batteryLevel
            thunderboardDevice.postValue(this)
        }
    }

    fun setFirmwareVersion(firmwareVersion: String) {
        thunderboardDevice.value?.apply {
            this.firmwareVersion = firmwareVersion
            thunderboardDevice.postValue(this)
        }
    }
}