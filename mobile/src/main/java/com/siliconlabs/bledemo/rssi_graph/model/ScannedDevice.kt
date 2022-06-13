package com.siliconlabs.bledemo.rssi_graph.model

data class ScannedDevice(
        val name: String,
        val address: String,
        val dataColor: Int,
        val graphData: MutableList<GraphPoint> = mutableListOf()
)