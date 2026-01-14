package com.siliconlabs.bledemo.features.demo.energyharvesting.presentation

import com.siliconlabs.bledemo.features.demo.energyharvesting.data.PersistentVoltageLogger
import com.siliconlabs.bledemo.features.demo.energyharvesting.domain.usecase.GetDeviceStatusUseCase

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconlabs.bledemo.features.demo.energyharvesting.domain.model.DeviceStatus
import com.siliconlabs.bledemo.features.demo.energyharvesting.domain.model.SensorReading
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EnergyViewModel(
    private val getDeviceStatus: GetDeviceStatusUseCase,
    private val voltageLogger: PersistentVoltageLogger
) : ViewModel() {

    private val _ui = MutableStateFlow(EnergyUiState(loading = true))
    val ui: StateFlow<EnergyUiState> = _ui.asStateFlow()
    private var startTimeMs: Long? = null

    init {
        refreshStatus()
        viewModelScope.launch(Dispatchers.IO) {
            val all = voltageLogger.loadAll()
            val readings = all.map { (elapsed, v) -> SensorReading(minuteIndex = elapsed.toInt(), voltageMv = v) }
            withContext(Dispatchers.Main) {
                _ui.update { it.copy(history = readings, loading = false) }
            }
        }
    }

    private fun refreshStatus() {
        viewModelScope.launch {
            try {
                val status = getDeviceStatus()
                if (startTimeMs == null) startTimeMs = SystemClock.elapsedRealtime()
                _ui.update { it.copy(loading = false, status = status) }
            } catch (_: Throwable) {
                _ui.update { it.copy(loading = false) }
            }
        }
    }

    fun onNewVoltage(voltageMv: Int) {
        val base = startTimeMs ?: run {
            startTimeMs = SystemClock.elapsedRealtime()
            startTimeMs!!
        }
        val elapsed = (SystemClock.elapsedRealtime() - base).coerceAtLeast(0)
        val timestamp = timeFormat.format(Date())
        _ui.update { current ->
            val lastTime = current.history.lastOrNull()?.minuteIndex ?: -1
            val adjustedElapsed = if (elapsed <= lastTime) lastTime + 1 else elapsed
            val newSample = SensorReading(minuteIndex = adjustedElapsed.toInt(), voltageMv = voltageMv)
            val newHistory = current.history + newSample
            val updatedStatus = (current.status ?: DeviceStatus(
                dataHex = "--",
                voltageMv = voltageMv,
                voltageHex = "0x" + voltageMv.toString(16).uppercase()
            )).copy(
                voltageMv = voltageMv,
                voltageHex = "0x" + voltageMv.toString(16).uppercase()
            )
            current.copy(
                history = newHistory,
                status = updatedStatus,
                loading = false,
                lastTimestamp = timestamp
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            val addr = ui.value.status?.deviceAddress
            val ident = ui.value.status?.deviceIdentifier
            voltageLogger.record(elapsedMs = elapsed, voltageMv = voltageMv, deviceAddress = addr, deviceIdentifier = ident)
        }
    }

    fun onDeviceAddress(address: String) {
        _ui.update { current ->
            val updatedStatus = (current.status ?: DeviceStatus(
                dataHex = "--",
                voltageMv = current.history.lastOrNull()?.voltageMv ?: 0,
                voltageHex = "0x" + (current.history.lastOrNull()?.voltageMv ?: 0).toString(16).uppercase()
            )).copy(deviceAddress = address)
            current.copy(status = updatedStatus, loading = false)
        }
    }

    fun onDeviceIdentifier(identifier: String) {
        _ui.update { current ->
            val status = (current.status ?: DeviceStatus(
                dataHex = "--",
                voltageMv = current.history.lastOrNull()?.voltageMv ?: 0,
                voltageHex = "0x" + (current.history.lastOrNull()?.voltageMv ?: 0).toString(16).uppercase()
            )).copy(deviceIdentifier = identifier)
            current.copy(status = status, loading = false)
        }
    }

    fun onRssiUpdate(rssi: Int) {
        _ui.update { current ->
            val status = (current.status ?: DeviceStatus(
                dataHex = "--",
                voltageMv = current.history.lastOrNull()?.voltageMv ?: 0,
                voltageHex = "0x" + (current.history.lastOrNull()?.voltageMv ?: 0).toString(16).uppercase()
            )).copy(rssi = rssi)
            current.copy(status = status, loading = false)
        }
    }

    fun onAdvInterval(intervalMs: Double) {
        _ui.update { current ->
            val status = (current.status ?: DeviceStatus(
                dataHex = "--",
                voltageMv = current.history.lastOrNull()?.voltageMv ?: 0,
                voltageHex = "0x" + (current.history.lastOrNull()?.voltageMv ?: 0).toString(16).uppercase()
            )).copy(advIntervalMs = intervalMs)
            current.copy(status = status, loading = false)
        }
    }

    companion object {
        private val timeFormat = SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault())
    }
}
