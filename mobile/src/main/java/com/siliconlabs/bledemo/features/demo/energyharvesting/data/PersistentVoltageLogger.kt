package com.siliconlabs.bledemo.features.demo.energyharvesting.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

class PersistentVoltageLogger(private val context: Context) {
    private val file: File = File(context.filesDir, "eh_readings.csv")

    init {
        if (!file.exists()) {
            file.writeText("id,timestampWall,elapsedSinceStartMs,voltageMv,deviceAddress,deviceIdentifier\n")
        }
    }

    fun clear(){
        val f = File(context.filesDir, "eh_readings.csv")
        if (f.exists()){
            f.writeText("")
        }
    }

    private var nextId: Long = run {
        // Count existing lines minus header
        val lines = file.readLines()
        (lines.size - 1).toLong()
    }

    suspend fun record(elapsedMs: Long, voltageMv: Int, deviceAddress: String?, deviceIdentifier: String?) {
        val row = buildString {
            append(nextId++)
            append(',')
            append(System.currentTimeMillis())
            append(',')
            append(elapsedMs)
            append(',')
            append(voltageMv)
            append(',')
            append(deviceAddress ?: "")
            append(',')
            append(deviceIdentifier ?: "")
            append('\n')
        }
        withContext(Dispatchers.IO) {
            FileWriter(file, true).use { it.write(row) }
        }
    }

    suspend fun loadAll(): List<Pair<Long, Int>> = withContext(Dispatchers.IO) {
        file.readLines().drop(1).mapNotNull { line ->
            val parts = line.split(',')
            if (parts.size >= 4) {
                val elapsed = parts[2].toLongOrNull() ?: return@mapNotNull null
                val voltage = parts[3].toIntOrNull() ?: return@mapNotNull null
                elapsed to voltage
            } else null
        }.sortedBy { it.first }
    }
}
