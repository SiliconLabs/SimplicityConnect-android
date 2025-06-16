package com.siliconlabs.bledemo.features.demo.wifi_provisioning.model

data class ScanResult(
    val ssid: String,
    val security_type: String,
    val network_type: String,
    val bssid: String,
    val channel: String,
    val rssi: String
)
