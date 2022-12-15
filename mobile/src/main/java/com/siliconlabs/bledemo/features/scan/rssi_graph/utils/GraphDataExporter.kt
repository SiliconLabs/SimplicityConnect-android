package com.siliconlabs.bledemo.features.scan.rssi_graph.utils

import com.opencsv.CSVWriter
import com.siliconlabs.bledemo.home_screen.viewmodels.ScanFragmentViewModel
import com.siliconlabs.bledemo.features.scan.rssi_graph.model.GraphPoint
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

class GraphDataExporter {

    fun export(data: List<ScanFragmentViewModel.ExportDeviceState>, miliTimestamp: Long, nanoTimestamp: Long) : String {
        val contentWriter = StringWriter()
        CSVWriter(contentWriter).also {
            it.writeNext(printTableHeaders(miliTimestamp))
            data.forEach { device ->
                device.graphData.forEach { point ->
                    it.writeNext(arrayOf(
                            device.name,
                            device.address,
                            printFormattedTime(point, nanoTimestamp),
                            point.rssi.toString()
                    ))
                }
            }
            it.close()
        }
        return contentWriter.toString()
    }

    private fun printTableHeaders(miliTimestamp: Long) : Array<String> {
        val calendar = Calendar.getInstance().apply { timeInMillis = miliTimestamp }
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

    private fun printFormattedTime(point: GraphPoint, nanoTimestamp: Long) : String {
        return String.format(
                "%.3f",
                point.convertTimestampToSeconds(nanoTimestamp)
        )
    }
}