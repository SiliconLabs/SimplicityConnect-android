package com.siliconlabs.bledemo.bluetooth.ble.values

import android.bluetooth.BluetoothGattCharacteristic

/**
 * Gatt Characteristic value representing a temperature measurement.
 */
class TemperatureValue private constructor(value: BluetoothGattCharacteristic) {
    private var temperatureType: Int? = null
    private val timeStamp: IntArray?

    private val isFahrenheit: Boolean
    val temperature: Float

    class Factory : ValueFactory<TemperatureValue?> {
        override fun create(value: BluetoothGattCharacteristic): TemperatureValue {
            return TemperatureValue(value)
        }
    }

    init {
        val flags = value.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) and 0x00000007
        temperature = value.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 1)
        isFahrenheit = flags and 0x00000001 != 0
        when (flags) {
            2, 3 -> {
                timeStamp = getTimeStamp(value, 1 + 4)
                temperatureType = null
            }
            4, 5 -> {
                timeStamp = null
                temperatureType = value.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1 + 4)
            }
            6, 7 -> {
                timeStamp = getTimeStamp(value, 1 + 4)
                temperatureType = value.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1 + 4 + 7)
            }
            else -> {
                timeStamp = null
                temperatureType = null
            }
        }
    }

    private fun getTimeStamp(value: BluetoothGattCharacteristic, offset: Int): IntArray {
        val year: Int = value.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset)
        val month: Int = value.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 2)
        val day: Int = value.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 3)
        val hours: Int = value.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 4)
        val minutes: Int = value.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 5)
        val seconds: Int = value.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 6)
        return intArrayOf(year, month, day, hours, minutes, seconds)
    }

}