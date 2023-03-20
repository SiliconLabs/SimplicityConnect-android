package com.siliconlabs.bledemo.home_screen.viewmodels

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.graphics.Color
import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.base.viewmodels.ScannerViewModel
import com.siliconlabs.bledemo.bluetooth.beacon_utils.BleFormat
import com.siliconlabs.bledemo.bluetooth.ble.BluetoothDeviceInfo
import com.siliconlabs.bledemo.bluetooth.ble.ConnectedDeviceInfo
import com.siliconlabs.bledemo.bluetooth.ble.ScanResultCompat
import com.siliconlabs.bledemo.features.scan.browser.models.GraphInfo
import com.siliconlabs.bledemo.features.scan.browser.models.ScannedDeviceInfo
import com.siliconlabs.bledemo.features.scan.browser.view_states.GraphFragmentViewState
import com.siliconlabs.bledemo.features.scan.browser.view_states.ScannerFragmentViewState
import com.siliconlabs.bledemo.features.scan.rssi_graph.model.GraphPoint
import com.siliconlabs.bledemo.utils.FilterDeviceParams
import com.siliconlabs.bledemo.utils.SharedPrefUtils
import com.siliconlabs.bledemo.utils.StringUtils
import java.util.*
import kotlin.random.Random

class ScanFragmentViewModel(private val context: Context) : ScannerViewModel() {

    private val sharedPrefUtils = SharedPrefUtils(context)

    private val _activeFiltersDescription: MutableLiveData<String?> = MutableLiveData()
    val activeFiltersDescription: LiveData<String?> = _activeFiltersDescription
    private val _activeFilters: MutableLiveData<FilterDeviceParams?> = MutableLiveData()
    val activeFilters: LiveData<FilterDeviceParams?> = _activeFilters

    private val scannedDevices: MutableMap<String, ScannedDeviceInfo> = mutableMapOf()

    private val _filteredDevices: MutableLiveData<MutableList<ScannedDeviceInfo>> = MutableLiveData(mutableListOf())
    val filteredDevices: LiveData<MutableList<ScannedDeviceInfo>> = _filteredDevices

    private val _deviceToInsert: MutableLiveData<BluetoothInfoViewState> = MutableLiveData()
    val deviceToInsert: LiveData<BluetoothInfoViewState> = _deviceToInsert
    private val _labelToInsert: MutableLiveData<LabelViewState> = MutableLiveData()
    val labelToInsert: LiveData<LabelViewState> = _labelToInsert

    private val _highlightedLabel: MutableLiveData<LabelViewState?> = MutableLiveData(null)
    val highlightedLabel: LiveData<LabelViewState?> = _highlightedLabel

    private val _activeConnections: MutableLiveData<MutableList<ConnectedDeviceInfo>> = MutableLiveData(mutableListOf())
    val activeConnections: LiveData<MutableList<ConnectedDeviceInfo>> = _activeConnections
    private val _numberOfConnectedDevices: MutableLiveData<Int> = MutableLiveData(0)
    val numberOfConnectedDevices: LiveData<Int> = _numberOfConnectedDevices

    var nanoTimestamp: Long = 0L /* These two have a different starting epoch! */
    var miliTimestamp: Long = 0L

    var shouldResetChart = false

    fun getScannerFragmentViewState() : ScannerFragmentViewState {
        return ScannerFragmentViewState(
                getIsScanningOn(),
                getBluetoothInfoViewsState()
        )
    }

    fun getGraphFragmentViewState() : GraphFragmentViewState {
        return GraphFragmentViewState(
                getIsScanningOn(),
                getLabelViewsState(),
                getGraphDevicesState(),
                _highlightedLabel.value,
                nanoTimestamp
        )
    }

    fun getBluetoothInfoViewsState() : List<BluetoothInfoViewState> {
        return _filteredDevices.value?.map {
            BluetoothInfoViewState(it)
        } ?: emptyList()
    }

    fun getLabelViewsState() : List<LabelViewState> {
        return _filteredDevices.value?.map {
            LabelViewState(it)
        } ?: emptyList()
    }

    fun getGraphDevicesState() : List<GraphDeviceState> {
        return _filteredDevices.value?.map {
            GraphDeviceState(it)
        } ?: emptyList()
    }

    fun getExportDevicesState() : List<ExportDeviceState> {
        return _filteredDevices.value?.map {
            ExportDeviceState(it)
        } ?: emptyList()
    }

    fun getConnectionViewStates() : List<ConnectionViewState> {
        return _activeConnections.value?.map {
            ConnectionViewState(it.bluetoothInfo)
        } ?: emptyList()
    }

    fun updateConnectionStates() {
        val connectedAddresses = _activeConnections.value?.map { it.connection.address }
        _filteredDevices.value?.forEach {
            it.bluetoothInfo = it.bluetoothInfo.clone().apply {
                connectionState =
                        if (connectedAddresses?.contains(it.bluetoothInfo.address) == true) BluetoothDeviceInfo.ConnectionState.CONNECTED
                        else BluetoothDeviceInfo.ConnectionState.DISCONNECTED
            }
        }
    }

    fun updateActiveConnections(activeConnections: List<ConnectedDeviceInfo>?) {
        activeConnections?.let {
            _activeConnections.value = it.toMutableList()
            _numberOfConnectedDevices.value = _activeConnections.value?.size ?: 0
        }
    }

    fun findConnectionByAddress(address: String) : ConnectedDeviceInfo? {
        return _activeConnections.value?.find { it.connection.address == address }
    }

    private fun findPositionByAddress(address: String) : Int? {
        val index = _filteredDevices.value?.indexOfFirst { it.bluetoothInfo.device.address == address }
        return if (index != -1) index else null
    }

    override fun handleScanResult(result: ScanResultCompat) {
        val address = result.device?.address ?: return
        val dataColor = generateDataColor()

        scannedDevices.apply {
            synchronized(this) {
                getOrPut(address, { ScannedDeviceInfo(
                            result.device!!,
                            sharedPrefUtils.isFavorite(address),
                            dataColor
                    )}
                ).apply {
                    bluetoothInfo = updateScanInfo(bluetoothInfo, result)
                    addNewGraphData(graphInfo.data, result)
                }
            }

            if (doesDeviceMatchFilters(this[address]!!.bluetoothInfo)) {
                _filteredDevices.value?.apply {
                    synchronized(this) {
                        val index = indexOfFirst { it.bluetoothInfo.address == address }

                        ( getOrNull(index) ?: ScannedDeviceInfo(
                                    result.device!!,
                                    sharedPrefUtils.isFavorite(address),
                                    dataColor)
                        ).apply {
                            bluetoothInfo = updateScanInfo(bluetoothInfo, result)
                            addNewGraphData(graphInfo.data, result)
                        }.also {
                            if (index == -1) {
                                _deviceToInsert.value = BluetoothInfoViewState(it)
                                _labelToInsert.value = LabelViewState(it)
                                add(it)
                            }
                        }
                    }
                }
            }
            _isAnyDeviceDiscovered.value = _filteredDevices.value?.size ?: 0 > 0
        }
    }

    fun handleOnLegendItemClick(clickedLabel: LabelViewState?) {
        _highlightedLabel.value =
                when (highlightedLabel.value?.address) {
                    null -> clickedLabel
                    clickedLabel?.address -> null
                    else -> clickedLabel
                }
    }

    private fun addNewGraphData(graphData: MutableList<GraphPoint>, result: ScanResultCompat) {
        /* Prevent from reading weird data, like 70k dBm rssi.
           Positive rssi values are not supposed to happen and they stretch the graph. */
        if (result.rssi > 5) return

        graphData.add(GraphPoint(result.rssi, result.timestampNanos))
    }

    fun setTimestamps() {
        miliTimestamp = System.currentTimeMillis()
        nanoTimestamp = SystemClock.elapsedRealtimeNanos()
    }

    fun sortDevices()  {
        _filteredDevices.value?.apply {
            sortByDescending { it.bluetoothInfo.rssi}
        }
    }

    fun updateFiltering(activeFilters: FilterDeviceParams?) {
        _activeFilters.value = activeFilters
        _activeFiltersDescription.value = activeFilters?.buildDescription(context)
        _filteredDevices.value = _filteredDevices.value?.apply {
            clear()
            scannedDevices.values.forEach {
                if (doesDeviceMatchFilters(it.bluetoothInfo)) {
                    add(ScannedDeviceInfo(it.bluetoothInfo, it.graphInfo))
                }
            }
        }
    }

    fun toggleViewExpansion(position: Int) {
        _filteredDevices.value = _filteredDevices.value?.apply {
            set(position, this[position].copy(isBluetoothInfoExpanded = !this[position].isBluetoothInfoExpanded))
        }
    }

    fun toggleIsFavorite(address: String) {
        findPositionByAddress(address)?.let {
            _filteredDevices.postValue(_filteredDevices.value?.apply {
                val newInfo = this[it].bluetoothInfo.apply { this.isFavorite = !this.isFavorite }
                set(it, this[it].copy(bluetoothInfo = newInfo))
            })
        }
    }

    fun refreshConnectedDeviceInfo(device: BluetoothDevice, connectionState: Int) {
        _filteredDevices.value = _filteredDevices.value?.apply {
            findPositionByAddress(device.address)?.let {
                val newInfo = this[it].bluetoothInfo.apply {
                    this.device = device // bond state can change here
                    this.connectionState = when (connectionState) {
                        BluetoothGatt.STATE_CONNECTED -> BluetoothDeviceInfo.ConnectionState.CONNECTED
                        else -> BluetoothDeviceInfo.ConnectionState.DISCONNECTED
                    }
                }
                set(it, this[it].copy(bluetoothInfo = newInfo))
            }
        }
    }

    fun setDeviceConnectionState(position: Int, connectionState: BluetoothDeviceInfo.ConnectionState) {
        _filteredDevices.postValue(_filteredDevices.value?.apply {
            val newInfo = this[position].bluetoothInfo.apply { this.connectionState = connectionState }
            set(position, this[position].copy(bluetoothInfo = newInfo))
        })
    }

    fun setDeviceConnectionState(address: String, connectionState: BluetoothDeviceInfo.ConnectionState) {
        findPositionByAddress(address)?.let {
            setDeviceConnectionState(it, connectionState)
        }
    }

    fun reset() {
        scannedDevices.clear()
        _filteredDevices.value = _filteredDevices.value?.apply { clear() }
        _isAnyDeviceDiscovered.value = _filteredDevices.value?.size ?: 0 > 0
        _highlightedLabel.value = null
    }


    @SuppressLint("MissingPermission")
    private fun doesDeviceMatchFilters(device: BluetoothDeviceInfo) : Boolean {
        _activeFilters.value?.let {
            it.name?.let { filterName ->
                if (filterName.isNotBlank()) {
                    if (!doesNameMatch(filterName, device.name) &&
                            !doesAddressMatch(filterName, device.address) &&
                            !doesRawDataMatch(filterName, device)) return false
                }
            }
            if (it.isRssiFlag && device.rssi < it.rssiValue) return false
            if (it.bleFormats.isNotEmpty() && !it.bleFormats.contains(device.bleFormat)) return false
            if (it.isOnlyFavourite && !(device.isFavorite)) return false
            if (it.isOnlyBonded && device.device.bondState != BluetoothDevice.BOND_BONDED) return false
            if (it.isOnlyConnectable && !device.isConnectable) return false
        }

        return true
    }

    private fun doesNameMatch(filterName: String, deviceName: String) : Boolean {
        return getLowerCase(deviceName).contains(getLowerCase(filterName))
    }

    private fun doesAddressMatch(filterName: String, deviceAddress: String) : Boolean {
        return getLowerCase(deviceAddress).contains(getLowerCase(filterName)) ||
                StringUtils.getStringWithoutColons(getLowerCase(deviceAddress)).contains(getLowerCase(filterName))
    }

    private fun doesRawDataMatch(filterName: String, device: BluetoothDeviceInfo) : Boolean {
        return device.rawData?.let {
            getLowerCase(it).contains(filterName)
        } ?: false
    }

    private fun getLowerCase(text: String) = text.toLowerCase(Locale.getDefault())

    private fun generateDataColor() : Int {
        return Color.rgb(
                Random.nextInt(256),
                Random.nextInt(256),
                Random.nextInt(256)
        )
    }


    data class BluetoothInfoViewState(
            val deviceInfo: BluetoothDeviceInfo,
            var isInfoExpanded: Boolean
    ) {
        constructor(info: ScannedDeviceInfo) : this(info.bluetoothInfo.clone(), info.isBluetoothInfoExpanded)
    }

    data class LabelViewState(
            val name: String,
            val address: String,
            val color: Int
    ) {
        constructor(info: ScannedDeviceInfo) : this(
                info.bluetoothInfo.name,
                info.bluetoothInfo.address,
                info.graphInfo.dataColor
        )
    }

    data class GraphDeviceState(
            val address: String,
            val graphInfo: GraphInfo
    ) {
        constructor(info: ScannedDeviceInfo) : this(
                info.bluetoothInfo.address,
                info.graphInfo
        )
    }

    data class ExportDeviceState(
            val name: String,
            val address: String,
            val graphData: List<GraphPoint>
    ) {
        constructor(info: ScannedDeviceInfo) : this(
                info.bluetoothInfo.name,
                info.bluetoothInfo.address,
                info.graphInfo.data
        )
    }

    data class ConnectionViewState(
            val device: BluetoothDevice,
            val manufacturer: BluetoothDeviceInfo.DeviceManufacturer,
            val rssi: Int,
            val intervalNanos: Long,
            val beaconType: BleFormat
    ) {
        constructor(info: BluetoothDeviceInfo) : this(
                info.device,
                info.manufacturer,
                info.rssi,
                info.intervalNanos,
                info.bleFormat ?: BleFormat.UNSPECIFIED
        )
    }


    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScanFragmentViewModel(context) as T
        }
    }

}