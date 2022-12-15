package com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.viewmodels

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.model.HallState
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.model.TemperatureScale
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.utils.PreferenceManager

class EnvironmentViewModel(context: Context) : ViewModel() {

    private var preferenceManager = PreferenceManager(context)
    var temperatureScale = TemperatureScale.CELSIUS
    var activeControls = 0

    var controlsRead = MutableLiveData(0)

    var temperature = MutableLiveData<Float>() //unit: C/F degree, resolution: 0.1 deg
    var humidity = MutableLiveData<Int>() //unit: %, resolution: 0.01%
    var uvIndex = MutableLiveData<Int>() //unitless
    var ambientLight = MutableLiveData<Long>() //unit: lx, resolution: 1 lx
    var soundLevel = MutableLiveData<Int>() //unit: dB, resolution: 1dB
    var pressure = MutableLiveData<Long>() //unit: mbar, resolution: 1 mbar
    var co2Level = MutableLiveData<Int>() //unit: ppm, resolution: 1 ppm
    var tvocLevel = MutableLiveData<Int>() //unit: ppb, resolution 1 ppb
    var hallStrength = MutableLiveData<Int>() //unit: uT (micro tesla). resolution: 1 uT
    var hallState = MutableLiveData<HallState>() //unitless


    fun incrementControlsRead() {
        controlsRead.postValue(controlsRead.value?.plus(1))
    }

    fun resetControlsRead() {
        controlsRead.postValue(0)
    }

    fun checkTemperatureScale() {
        temperatureScale = preferenceManager.retrievePreferences().scale
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EnvironmentViewModel(context) as T
        }
    }

}