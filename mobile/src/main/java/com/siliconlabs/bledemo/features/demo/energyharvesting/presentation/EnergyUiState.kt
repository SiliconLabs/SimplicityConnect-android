package com.siliconlabs.bledemo.features.demo.energyharvesting.presentation

import com.siliconlabs.bledemo.features.demo.energyharvesting.domain.model.DeviceStatus
import com.siliconlabs.bledemo.features.demo.energyharvesting.domain.model.SensorReading

data class EnergyUiState(
    val loading: Boolean = true,
    val status: DeviceStatus? = null,
    val history: List<SensorReading> = emptyList(),
    val isFavorite: Boolean = false,
    val lastTimestamp: String? = null // formatted time of last voltage update (HH:mm:ss:SSS)
)
