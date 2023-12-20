package com.siliconlabs.bledemo.features.demo.matter_demo.model

enum class ProvisionNetworkType {
    NONE,
    THREAD,
    WIFI,
    ;

    companion object {
        fun fromName(name: String?): ProvisionNetworkType? {
            return when (name) {
                THREAD.name -> THREAD
                WIFI.name -> WIFI
                else -> null
            }
        }
    }
}