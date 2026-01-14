package com.siliconlabs.bledemo.features.demo.energyharvesting.domain.data

import com.siliconlabs.bledemo.features.demo.energyharvesting.domain.model.DeviceStatus
import com.siliconlabs.bledemo.features.demo.energyharvesting.domain.model.SensorReading
import com.siliconlabs.bledemo.features.demo.energyharvesting.domain.repository.EnergyRepository
import kotlinx.coroutines.delay

class FakeEnergyRepository : EnergyRepository {
    override suspend fun getDeviceStatus(): DeviceStatus {
        delay(100)
        return DeviceStatus(dataHex = "0x0BA5CD12", voltageMv = 2981, voltageHex = "0x0BA5")
    }

    override suspend fun getVoltageHistory(): List<SensorReading> {
        delay(100)
        val values = listOf(
            3000,
            2976,
            2855,
            2793,
            2651,
            2528,
            2502,
            2475,
            2512,
            2630,
            2767,
            2930,
            3100,
            3233
        )
        return values.mapIndexed { index, i -> SensorReading(index, i) }
    }
}