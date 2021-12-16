package com.siliconlabs.bledemo.environment.presenters

import com.siliconlabs.bledemo.environment.model.HallState
import com.siliconlabs.bledemo.thunderboard.base.BaseViewListener
import com.siliconlabs.bledemo.thunderboard.model.ThunderBoardDevice

interface EnvironmentListener : BaseViewListener {
    fun setPowerSource(powerSource: ThunderBoardDevice.PowerSource)

    fun setTemperature(temperature: Float, temperatureType: Int)
    fun setHumidity(humidity: Int)
    fun setUvIndex(uvIndex: Int)
    fun setAmbientLight(ambientLight: Long)
    fun setSoundLevel(soundLevel: Float)
    fun setPressure(pressure: Float)
    fun setCO2Level(co2Level: Int)
    fun setTVOCLevel(tvocLevel: Int)
    fun setHallStrength(hallStrength: Float)
    fun setHallState(@HallState hallState: Int)

    fun setTemperatureEnabled(enabled: Boolean)
    fun setHumidityEnabled(enabled: Boolean)
    fun setUvIndexEnabled(enabled: Boolean)
    fun setAmbientLightEnabled(enabled: Boolean)
    fun setSoundLevelEnabled(enabled: Boolean)
    fun setPressureEnabled(enabled: Boolean)
    fun setCO2LevelEnabled(enabled: Boolean)
    fun setTVOCLevelEnabled(enabled: Boolean)
    fun setHallStrengthEnabled(enabled: Boolean)
    fun setHallStateEnabled(enabled: Boolean)

    fun initGrid()
}