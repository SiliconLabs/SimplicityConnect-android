package com.siliconlabs.bledemo.features.iop_test.models

data class ConnectionParameters(
        val mtu: Int,
        val pdu: Int,
        val interval: Double,
        val slaveLatency: Int,
        val supervisionTimeout: Int,
)
