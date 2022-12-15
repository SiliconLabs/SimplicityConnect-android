package com.siliconlabs.bledemo.features.demo.wifi_commissioning.models

/**
 * Created by harika on 20-04-2016.
 */
class AccessPoint(
        val name: String,
        val securityMode: SecurityMode,
        var status: Boolean = false
) {
    var password: String? = null
    var macAddress: String? = null
    var ipAddress: String? = null
}