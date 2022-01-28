package com.siliconlabs.bledemo.thunderboard.model

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import com.siliconlabs.bledemo.thunderboard.sensor.SensorBlinky
import com.siliconlabs.bledemo.thunderboard.sensor.SensorEnvironment
import com.siliconlabs.bledemo.thunderboard.sensor.SensorMotion

class ThunderBoardDevice(device: BluetoothDevice) {
    var name: String = device.name
    var state = BluetoothProfile.STATE_DISCONNECTED
    var batteryLevel = 0
    var powerSource = PowerSource.UNKNOWN
    var modelNumber: String? = null
    var firmwareVersion: String? = null

    // configuration settings
    var isBatteryConfigured: Boolean? = null
    var isBatteryNotificationEnabled: Boolean? = null
    var isPowerSourceConfigured: Boolean? = null
    var isPowerSourceNotificationEnabled: Boolean? = null
    var isServicesDiscovered: Boolean? = null
    var isCalibrateNotificationEnabled: Boolean? = null
    var isAccelerationNotificationEnabled: Boolean? = null
    var isOrientationNotificationEnabled: Boolean? = null
    var isHallStateNotificationEnabled: Boolean? = null

    // Demo sensors
    var sensorMotion: SensorMotion? = null
        set(sensor) {
            field = sensor
            sensor?.clearCharacteristicsStatus()
        }
    var sensorEnvironment: SensorEnvironment? = null
    var sensorBlinky: SensorBlinky? = null


    val boardType: Type
        get() = when (modelNumber) {
            THUNDERBOARD_MODEL_SENSE -> Type.THUNDERBOARD_SENSE
            THUNDERBOARD_MODEL_BLUE_V1,
            THUNDERBOARD_MODEL_BLUE_V2 -> Type.THUNDERBOARD_BLUE
            THUNDERBOARD_MODEL_DEV_KIT_V1,
            THUNDERBOARD_MODEL_DEV_KIT_V2 -> Type.THUNDERBOARD_DEV_KIT
            else -> Type.UNKNOWN
        }

    enum class PowerSource(val value: Int) {
        USB(1),
        COIN_CELL(4),
        UNKNOWN(0);

        companion object {
            fun fromInt(code: Int) : PowerSource {
                for (source in values()) {
                    if (source.value == code ) return source
                }
                return UNKNOWN
            }
        }
    }

    enum class Type {
        THUNDERBOARD_SENSE,
        THUNDERBOARD_BLUE,
        THUNDERBOARD_DEV_KIT,
        UNKNOWN
    }

    companion object {
        const val THUNDERBOARD_MODEL_SENSE = "BRD4166A"
        const val THUNDERBOARD_MODEL_BLUE_V1 = "BRD4184A"
        const val THUNDERBOARD_MODEL_BLUE_V2 = "BRD4184B"
        const val THUNDERBOARD_MODEL_DEV_KIT_V1 = "BRD2601A"
        const val THUNDERBOARD_MODEL_DEV_KIT_V2 = "BRD2601B"
    }

}