package com.siliconlabs.bledemo.features.demo.devkitsensor917.utils

class DevKitSensorChecker {
    val devKitSensors = mutableMapOf(
        DevkitSensor917BoardSensor.Temperature to DevkitSensorState.WORKING,
        DevkitSensor917BoardSensor.Humidity to DevkitSensorState.WORKING,
        DevkitSensor917BoardSensor.AmbientLight to DevkitSensorState.WORKING,
        DevkitSensor917BoardSensor.Motion to DevkitSensorState.WORKING,
        DevkitSensor917BoardSensor.LED to DevkitSensorState.WORKING,
//        DevkitSensor917BoardSensor.Microphone to DevkitSensorState.WORKING,

    )

    enum class DevkitSensor917BoardSensor(val broken: Long?) {
        Temperature(0x7fff),
        Humidity(0xffff),
        AmbientLight(0xffffffff),
        Motion(0xffff),
        LED(0xffffff),
//        Microphone(0xffff)
    }

    enum class DevkitSensorState {
        WORKING,
        MISSING,
        BROKEN
    }
}