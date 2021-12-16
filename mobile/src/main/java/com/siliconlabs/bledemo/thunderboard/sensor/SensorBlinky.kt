package com.siliconlabs.bledemo.thunderboard.sensor

import com.siliconlabs.bledemo.thunderboard.model.LedRGBState

class SensorBlinky : SensorBase() {
    override var sensorData = SensorData(0.toByte())

    fun setSwitch(b: Byte) {
        sensorData.sw0 = if (b.toInt() and IO_0_ON.toInt() != 0) 1 else 0
        sensorData.sw1 = if (b.toInt() and IO_1_ON.toInt() != 0) 1 else 0
        isSensorDataChanged = true
    }

    fun setLed(b: Byte) {
        sensorData.ledb = if (b.toInt() and IO_0_ON.toInt() != 0) 1 else 0
        sensorData.ledg = if (b.toInt() and IO_1_ON.toInt() != 0) 1 else 0
        isSensorDataChanged = true
    }

    var colorLed: LedRGBState?
        get() = sensorData.colorLed
        set(ledstate) {
            sensorData.colorLed = ledstate
            isSensorDataChanged = true
        }

    class SensorData : com.siliconlabs.bledemo.thunderboard.sensor.SensorData {
        var ledb = 0
        var ledg = 0
        var sw0 = 0
        var sw1 = 0
        var colorLed: LedRGBState? = null

        internal constructor() {}
        constructor(b: Byte) {
            ledb = if (b.toInt() and IO_0_ON.toInt() != 0) 1 else 0
            ledg = if (b.toInt() and IO_1_ON.toInt() != 0) 1 else 0
            sw0 = if (b.toInt() and IO_0_ON.toInt() != 0) 1 else 0
            sw1 = if (b.toInt() and IO_1_ON.toInt() != 0) 1 else 0
        }

        override fun toString(): String {
            return String.format("%d %d %d %d", ledb, ledg, sw0, sw1)
        }

        override fun clone(): com.siliconlabs.bledemo.thunderboard.sensor.SensorData {
            val d = SensorData()
            d.ledb = ledb
            d.ledg = ledg
            d.sw0 = sw0
            d.sw1 = sw1
            d.colorLed = colorLed
            return d
        }
    }

    companion object {
        private const val IO_0_ON: Byte = 0x01
        private const val IO_1_ON: Byte = 0x04
    }
}