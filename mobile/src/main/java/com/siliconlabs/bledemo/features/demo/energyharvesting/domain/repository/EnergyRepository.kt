package com.siliconlabs.bledemo.features.demo.energyharvesting.domain.repository

import com.siliconlabs.bledemo.features.demo.energyharvesting.domain.model.DeviceStatus
import com.siliconlabs.bledemo.features.demo.energyharvesting.domain.model.SensorReading

interface EnergyRepository {
    suspend fun getDeviceStatus(): DeviceStatus
    suspend fun getVoltageHistory(): List<SensorReading>
}
