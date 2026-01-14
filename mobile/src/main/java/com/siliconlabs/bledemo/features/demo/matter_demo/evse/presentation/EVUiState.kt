package com.siliconlabs.bledemo.features.demo.matter_demo.evse.presentation

import com.siliconlabs.bledemo.features.demo.matter_demo.evse.domain.entity.ChargingMode

data class EVUiState(
    val percent: Int = 0,
    val connected: Boolean = false,
    val mode: ChargingMode = ChargingMode.Manual, // legacy semantic mode (approximation)
    val remainingSeconds: Long = 0,
    val showModePicker: Boolean = false,
    // Map: device numeric modeId -> label (preserves insertion order if LinkedHashMap provided)
    val modeLabels: Map<Int, String> = emptyMap(),
    val currentModeId: Int? = null,
    // Newly added dynamic vehicle identifier fetched from EnergyEvseCluster VehicleID attribute
    val vehicleId: String? = null,
    // Charging state code & user-friendly label from EnergyEvseCluster.readStateAttribute()
    val chargingStateCode: Int? = null,
    val chargingStateLabel: String? = null,
    // Offline tracking (new)
    val isOffline: Boolean = false,
    val lastError: String? = null
)
