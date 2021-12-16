package com.siliconlabs.bledemo.thunderboard.sensor

abstract class SensorBase {
    var isNotificationEnabled: Boolean? = null
    var isSensorDataChanged = false

    abstract val sensorData: SensorData
}