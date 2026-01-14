package com.siliconlabs.bledemo.features.demo.energyharvesting.domain.model

data class DeviceStatus(
    val name: String = "EH Sensor",
    val dataHex: String,
    val voltageMv: Int,
    val voltageHex: String,
    val payload: String = "- - - -",
    val deviceAddress: String? = null,
    val deviceIdentifier: String? = null, // service UUID or generated stable pseudo UUID
    val rssi: Int? = null, // dynamic RSSI (dBm)
    val advIntervalMs: Double? = null // dynamic advertisement interval (ms)
)
