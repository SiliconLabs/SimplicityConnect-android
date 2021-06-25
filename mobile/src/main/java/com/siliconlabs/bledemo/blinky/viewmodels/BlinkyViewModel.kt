package com.siliconlabs.bledemo.blinky.viewmodels

import android.bluetooth.BluetoothGattCharacteristic
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.siliconlabs.bledemo.blinky.models.LightState

class BlinkyViewModel : ViewModel() {
    private val _lightState: MutableLiveData<LightState> = MutableLiveData(LightState.OFF)
    val lightState: LiveData<LightState> = _lightState
    private val _isButtonPressed: MutableLiveData<Boolean> = MutableLiveData(false)
    val isButtonPressed: LiveData<Boolean> = _isButtonPressed

    fun handleButtonStateChanges(characteristic: BluetoothGattCharacteristic) {
        val result = characteristic.value[0].toInt()
        _isButtonPressed.postValue(result == 1)
    }

    fun changeLightState() {
        when (_lightState.value) {
            LightState.ON -> _lightState.value = LightState.TOGGLING_OFF
            LightState.OFF -> _lightState.value = LightState.TOGGLING_ON
            else -> {
            }
        }
    }

    fun handleLightStateChanges(characteristic: BluetoothGattCharacteristic) {
        val result = characteristic.value[0].toInt()
        when (result) {
            0 -> _lightState.postValue(LightState.OFF)
            1 -> _lightState.postValue(LightState.ON)
        }
    }
}