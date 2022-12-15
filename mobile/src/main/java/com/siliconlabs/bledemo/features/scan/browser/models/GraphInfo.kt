package com.siliconlabs.bledemo.features.scan.browser.models

import com.siliconlabs.bledemo.features.scan.rssi_graph.model.GraphPoint

data class GraphInfo(
        val data: MutableList<GraphPoint> = mutableListOf(),
        val dataColor: Int
)