package com.siliconlabs.bledemo.features.demo.awsiot.model

import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.AccelerometerGyroScopeResponse

data class SensorData(
    val accelerometer: AccelerometerGyroScopeResponse,
    val gyro: AccelerometerGyroScopeResponse
)
