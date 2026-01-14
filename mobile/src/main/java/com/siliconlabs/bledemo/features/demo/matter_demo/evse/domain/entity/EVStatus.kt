package com.siliconlabs.bledemo.features.demo.matter_demo.evse.domain.entity

data class EVStatus(
    val batteryPercent: Int,
    val isConnected: Boolean,
    val mode: ChargingMode,
    val remainingSeconds: Long // for clock/timer demo
)

