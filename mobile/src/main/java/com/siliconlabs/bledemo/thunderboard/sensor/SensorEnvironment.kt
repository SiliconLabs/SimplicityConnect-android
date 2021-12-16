package com.siliconlabs.bledemo.thunderboard.sensor

import com.siliconlabs.bledemo.environment.model.HallState

class SensorEnvironment(var temperatureType: Int) : SensorBase() {

    val MAX_AMBIENT_LIGHT = 99999
    var readStatus = 0
        private set
    override val sensorData = SensorData()

    fun setHallStateNotificationEnabled() {
        readStatus = readStatus or 0xC0000
    }

    fun setTemperature(temperature: Int) {
        val t = temperature / 100f
        sensorData.temperature = t
        // clear the other reads
        readStatus = 0x03
        isSensorDataChanged = true
    }

    fun setHumidity(humidity: Int) {
        sensorData.humidity = humidity / 100
        readStatus = readStatus or 0x0c
        isSensorDataChanged = true
    }

    fun setUvIndex(uvIndex: Int) {
        sensorData.uvIndex = uvIndex
        readStatus = readStatus or 0x30
        isSensorDataChanged = true
    }

    fun setAmbientLight(ambientLight: Long) {
        var light = ambientLight
        light /= 100
        sensorData.ambientLight =
                if (light > MAX_AMBIENT_LIGHT) MAX_AMBIENT_LIGHT.toLong()
                else light
        readStatus = readStatus or 0xc0
        isSensorDataChanged = true
    }

    fun setSoundLevel(soundLevel: Int) {
        // units of 0.01dB
        val soundLevelF = soundLevel.toFloat() / 100.0f
        sensorData.sound = soundLevelF
        readStatus = readStatus or 0x0300
        isSensorDataChanged = true
    }

    fun setPressure(pressure: Long) {
        // pressure is in units of 0.1Pa, convert to millibars
        val pressuref = pressure.toFloat() / 1000.0f
        sensorData.pressure = pressuref
        readStatus = readStatus or 0x0c00
        isSensorDataChanged = true
    }

    fun setCO2Level(co2Level: Int) {
        // units of ppm
        sensorData.cO2Level = co2Level
        readStatus = readStatus or 0x3000
        isSensorDataChanged = true
    }

    fun setTVOCLevel(tvocLevel: Int) {
        // units of ppb
        sensorData.tVOCLevel = tvocLevel
        readStatus = readStatus or 0xc000
        isSensorDataChanged = true
    }

    fun setHallStrength(hallStrength: Float) {
        // units of uT (micro tesla)
        sensorData.hallStrength = hallStrength
        readStatus = readStatus or 0x30000
        isSensorDataChanged = true
    }

    fun setHallState(@HallState hallState: Int) {
        // unitless
        sensorData.hallState = hallState
        readStatus = readStatus or 0xC0000
        isSensorDataChanged = true
    }

    class SensorData : com.siliconlabs.bledemo.thunderboard.sensor.SensorData {

        override fun toString(): String {
            return String.format(
                    "temperature: %.2f, humidity: %d, uvIndex: %d, ambientLight: %d, hallStrength: %.1f, hallState: %d",
                    temperature,
                    humidity,
                    uvIndex,
                    ambientLight,
                    hallStrength,
                    hallState)
        }

        var temperature: Float = 0f //unit: C/F degree, resolution: 0.1 deg
        var humidity: Int = 0 //unit: %, resolution: 0.01%
        var uvIndex: Int = 0 //unitless
        var ambientLight: Long = Long.MIN_VALUE // unit: lx, resolution: 1 lx
        var sound: Float = 0f // unit: dB, resolution: 1dB
        var pressure: Float = 0f //unit: mbar, resolution: 1 mbar
        var cO2Level: Int = 0 //unit: ppm, resolution: 1 ppm
        var tVOCLevel: Int = 0 //unit: ppb, resolution 1 ppb
        var hallStrength: Float = 0f //unit: uT (micro tesla). resolution: 1 uT
        @HallState var hallState: Int = 0 // Unitless


        override fun clone(): com.siliconlabs.bledemo.thunderboard.sensor.SensorData {
            val d = SensorData()
            d.temperature = temperature
            d.humidity = humidity
            d.uvIndex = uvIndex
            d.ambientLight = ambientLight
            d.sound = sound
            d.pressure = pressure
            d.cO2Level = cO2Level
            d.tVOCLevel = tVOCLevel
            d.hallStrength = hallStrength
            d.hallState = hallState
            return d
        }
    }
}