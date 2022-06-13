package com.siliconlabs.bledemo.thunderboard.utils

import android.bluetooth.BluetoothGattCharacteristic

class SensorChecker {

    val motionSensors = mutableMapOf(
            ThunderboardSensor.Acceleration to SensorState.WORKING,
            ThunderboardSensor.Orientation to SensorState.WORKING
    )
    val environmentSensors = mutableMapOf(
            ThunderboardSensor.Temperature to SensorState.WORKING,
            ThunderboardSensor.Humidity to SensorState.WORKING,
            ThunderboardSensor.AmbientLight to SensorState.WORKING,
            ThunderboardSensor.UvIndex to SensorState.WORKING,
            ThunderboardSensor.Pressure to SensorState.WORKING,
            ThunderboardSensor.SoundLevel to SensorState.WORKING,
            ThunderboardSensor.CO2 to SensorState.WORKING,
            ThunderboardSensor.TVOC to SensorState.WORKING,
            ThunderboardSensor.MagneticField to SensorState.WORKING,
            ThunderboardSensor.DoorState to SensorState.WORKING
    )

    fun checkIfMotionSensorBroken(sensor: ThunderboardSensor, x: Int, y: Int, z: Int) {
        if (x.toLong() == sensor.brokenValue &&
                y.toLong() == sensor.brokenValue &&
                z.toLong() == sensor.brokenValue) {
            motionSensors[sensor] = SensorState.BROKEN
        }
    }

    fun checkIfEnvSensorBroken(sensor: ThunderboardSensor, sentValue: Long) {
        if (sentValue == sensor.brokenValue) {
            environmentSensors[sensor] = SensorState.BROKEN
            if (sensor == ThunderboardSensor.MagneticField) {
                /* DoorState depends on MagneticField. */
                environmentSensors[ThunderboardSensor.DoorState] = SensorState.BROKEN
            }
        }
    }

    fun setupEnvSensorCharacteristic(sensor: ThunderboardSensor, characteristic: BluetoothGattCharacteristic?) {
        if (characteristic != null) {
            sensor.characteristic = characteristic
        } else {
            environmentSensors[sensor] = SensorState.MISSING
        }
    }


    enum class ThunderboardSensor(val brokenValue: Long?, var characteristic: BluetoothGattCharacteristic? = null) {
        Acceleration(0x7fff),
        Orientation(0x7fff),
        Temperature(0x7fff),
        Humidity(0xffff),
        AmbientLight(0xffffffff),
        UvIndex(0xff),
        Pressure(0xffffffff),
        SoundLevel(0x7fff),
        CO2(0xffff),
        TVOC(0xffff),
        MagneticField(0x7fffffff),
        DoorState(null) // it depends on MagneticField sensor
    }

    enum class SensorState {
        WORKING,
        MISSING,
        BROKEN
    }

}