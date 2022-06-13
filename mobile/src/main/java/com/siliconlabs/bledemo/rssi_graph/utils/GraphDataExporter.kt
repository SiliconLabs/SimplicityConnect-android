package com.siliconlabs.bledemo.rssi_graph.utils

import com.opencsv.CSVWriter
import com.siliconlabs.bledemo.rssi_graph.model.GraphPoint
import com.siliconlabs.bledemo.rssi_graph.model.ScannedDevice
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

class GraphDataExporter {

    fun export(data: List<ScannedDevice>, exportTimestamp: Long, startTimestamp: Long) : String {
        val contentWriter = StringWriter()
        CSVWriter(contentWriter).also {
            it.writeNext(printTableHeaders(exportTimestamp))
            data.forEach { device ->
                device.graphData.forEach { point ->
                    it.writeNext(arrayOf(
                            device.name,
                            device.address,
                            printFormattedTime(point, startTimestamp),
                            point.rssi.toString()
                    ))
                }
            }
            it.close()
        }
        return contentWriter.toString()
    }

    private fun printTableHeaders(exportTimestamp: Long) : Array<String> {
        val calendar = Calendar.getInstance().apply { timeInMillis = exportTimestamp }
        val simpleDateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).apply {
            timeZone = calendar.timeZone
        }
        val time = simpleDateFormat.format(calendar.time)

        return arrayOf(
                "peripheral name",
                "peripheral address",
                "time since the scan start at: $time",
                "RSSI value [dB]"
        )
    }

    private fun printFormattedTime(point: GraphPoint, startTimestamp: Long) : String {
        return String.format(
                "%.3f",
                point.convertTimestampToSeconds(startTimestamp)
        )
    }
}