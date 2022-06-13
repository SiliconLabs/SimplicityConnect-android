package com.siliconlabs.bledemo.rssi_graph.viewmodels

import android.bluetooth.le.ScanResult
import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.siliconlabs.bledemo.rssi_graph.dialog_fragments.FilterDialogFragment
import com.siliconlabs.bledemo.rssi_graph.dialog_fragments.SortDialogFragment
import com.siliconlabs.bledemo.rssi_graph.model.GraphPoint
import com.siliconlabs.bledemo.rssi_graph.model.ScannedDevice

class RssiGraphViewModel : ViewModel() {

    val isScanning: MutableLiveData<Boolean> = MutableLiveData()
    val activeFilters = MutableLiveData<MutableMap<FilterDialogFragment.FilterType, String>>(mutableMapOf())
    val activeSortMode = MutableLiveData<SortDialogFragment.SortMode>(SortDialogFragment.SortMode.NONE)
    val highlightedDevice = MutableLiveData<ScannedDevice?>(null)

    var exportTimestamp: Long = 0L

    val savedDevices = mutableMapOf<String, ScannedDevice>()
    var filteredDevices = mutableListOf<ScannedDevice>()
    val newMatchingDevice = MutableLiveData<ScannedDevice>()
    val anyDevicesFound = MutableLiveData(false)

    fun handleScanResult(result: ScanResult) {
        if (result.rssi > 50) return //prevent from reading weird data, like 70k dBm rssi

        val deviceAddress = result.device.address
        val graphPoint = GraphPoint(result.rssi, result.timestampNanos)

        savedDevices.let { devices ->
            if (devices.keys.contains(deviceAddress)) {
                devices[deviceAddress]!!.graphData.let {
                    synchronized(it) { it.add(graphPoint) }
                }
            } else {
                val newDevice = ScannedDevice(
                        result.device.name ?: result.scanRecord?.deviceName ?: "N/A",
                        deviceAddress,
                        graphColors[devices.size % graphColors.size],
                        mutableListOf(graphPoint)
                )
                newDevice.let {
                    devices[deviceAddress] = it

                    if (iterateThroughFilters(listOf(it)).isNotEmpty()) {
                        newMatchingDevice.postValue(it)
                        synchronized(filteredDevices) { filteredDevices.add(it) }
                        anyDevicesFound.postValue(true)
                    }
                }
            }
        }
    }

    fun toggleScanningState() {
        isScanning.value = isScanning.value != true
    }

    fun setScanningState(setOn: Boolean) {
        isScanning.value = setOn
    }

    fun applyFilters(filters: MutableMap<FilterDialogFragment.FilterType, String>) {
        this.activeFilters.value = filters
    }

    fun setSortMode(sortMode: SortDialogFragment.SortMode) {
        this.activeSortMode.value = sortMode
    }

    fun filterScannedDevices() {
        synchronized(filteredDevices) {
            filteredDevices = iterateThroughFilters(savedDevices.values.toList()).toMutableList()
        }
        anyDevicesFound.postValue(filteredDevices.any())
    }

    fun handleOnLegendItemClick(clickedDevice: ScannedDevice?) {
        highlightedDevice.value =
                when (highlightedDevice.value?.address) {
                    null -> clickedDevice
                    clickedDevice?.address -> null
                    else -> clickedDevice
                }
    }

    fun reset() {
        savedDevices.clear()
        filteredDevices.clear()
        highlightedDevice.value = null
    }

    private fun iterateThroughFilters(devices: List<ScannedDevice>) : List<ScannedDevice> {
        return devices
                .filter { isNameOrAddressMatch(it,
                    activeFilters.value?.get(FilterDialogFragment.FilterType.NAME_OR_ADDRESS)) }
                .filter { isRssiMatch(it,
                    activeFilters.value?.get(FilterDialogFragment.FilterType.RSSI)?.toInt())
        }
    }

    private fun isNameOrAddressMatch(device: ScannedDevice, filterValue: String?) : Boolean {
        return filterValue?.let {
                device.address.contains(it, ignoreCase = true) ||
                device.name.contains(it, ignoreCase = true)
        } ?: true
    }

    private fun isRssiMatch(device: ScannedDevice, filterValue: Int?) : Boolean {
        return filterValue?.let {
            device.graphData.last().rssi >= it
        } ?: true
    }

    private val graphColors = listOf(
            Color.rgb(255, 0, 0),
            Color.rgb(255, 128, 0),
            Color.rgb(255, 255, 0),
            Color.rgb(128, 255, 0),
            Color.rgb(0, 255, 0),
            Color.rgb(0, 255, 128),
            Color.rgb(0, 255, 255),
            Color.rgb(0, 128, 255),
            Color.rgb(0, 0, 255),
            Color.rgb(128, 0, 255),
            Color.rgb(255, 0, 255),
            Color.rgb(255, 0, 128)
    )
}