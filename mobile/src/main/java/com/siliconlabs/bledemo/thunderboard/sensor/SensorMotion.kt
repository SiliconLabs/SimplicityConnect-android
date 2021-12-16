package com.siliconlabs.bledemo.thunderboard.sensor

class SensorMotion : SensorBase() {

    var characteristicsStatus = 0
        private set
    override val sensorData = SensorData()

    fun clearCharacteristicsStatus() {
        characteristicsStatus = 0
    }
    
    fun setAccelerationNotificationEnabled(enabled: Boolean) {
        characteristicsStatus = characteristicsStatus or 0x30
    }

    fun setOrientationNotificationEnabled(enabled: Boolean) {
        characteristicsStatus = characteristicsStatus or 0xc0
    }

    fun setOrientation(orientationX: Float, orientationY: Float, orientationZ: Float) {
        sensorData.ox = orientationX
        sensorData.oy = orientationY
        sensorData.oz = orientationZ
        isSensorDataChanged = true
    }

    fun setAcceleration(accelerationX: Float, accelerationY: Float, accelerationZ: Float) {
        sensorData.ax = accelerationX
        sensorData.ay = accelerationY
        sensorData.az = accelerationZ
        isSensorDataChanged = true
    }

    class SensorData : com.siliconlabs.bledemo.thunderboard.sensor.SensorData {
        override fun toString(): String {
            return String.format("%f %f %f %f %f %f", ax, ay, az, ox, oy, oz)
        }

        // Acceleration along X-axis in . Units in g with resolution of 0.001 g
        var ax = 0f

        // Acceleration along Y-axis in . Units in g with resolution of 0.001 g
        var ay = 0f

        // Acceleration along Z-axis in . Units in g with resolution of 0.001 g
        var az = 0f

        // Orientation alpha angle in deg (+180 to -180) with resolution of 0.01 deg
        var ox = 0f

        // Orientation beta angle in deg (+90 to -90) with resolution of 0.01 deg
        var oy = 0f

        // Orientation gamma angle in deg (+180 to -180) with resolution of 0.01 deg
        var oz = 0f

        override fun clone(): com.siliconlabs.bledemo.thunderboard.sensor.SensorData {
            val d = SensorData()
            d.ax = ax
            d.ay = ay
            d.az = az
            d.ox = ox
            d.oy = oy
            d.oz = oz
            return d
        }
    }
}