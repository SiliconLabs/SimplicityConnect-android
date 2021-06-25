package com.siliconlabs.bledemo.bluetooth.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import java.util.*

class Discovery(private val container: DeviceContainer<BluetoothDeviceInfo>, private val host: BluetoothDiscoveryHost) : BluetoothService.Listener {
    interface BluetoothDiscoveryHost {
        fun isReady(): Boolean
        fun reDiscover()
        fun onAdapterDisabled()
        fun onAdapterEnabled()
    }

    interface DeviceContainer<T : BluetoothDeviceInfo> {
        fun flushContainer()
        fun updateWithDevices(devices: List<T>)
    }

    private val SERVICES: MutableList<GattService> = ArrayList()
    private val NAMES: MutableList<String> = ArrayList()

    private var bluetoothBinding: BluetoothService.Binding? = null
    private var bluetoothService: BluetoothService? = null

    private var executeDiscovery = false
    private var isBluetoothEnabled = false
    private var isScanning = false
    private var isDeviceStarted = false
    private var isDeviceReady: Boolean? = null

    fun connect(context: Context) {
        bluetoothBinding = object : BluetoothService.Binding(context) {
            override fun onBound(service: BluetoothService?) {
                bluetoothService?.removeListener(this@Discovery)

                bluetoothService = service

                bluetoothService?.let {
                    if (executeDiscovery) {
                        executeDiscovery = false
                        discoverDevices(true)
                    }
                }
            }
        }
        bluetoothBinding?.bind()
    }

    fun disconnect() {
        stopDiscovery(true)
        bluetoothBinding?.unbind()
        bluetoothBinding = null
        bluetoothService = null
    }

    fun clearFilters() {
        SERVICES.clear()
        NAMES.clear()
    }

    fun addFilter(vararg services: GattService) {
        if (services != null) {
            SERVICES.addAll(arrayOf(*services))
        }
    }

    fun addFilter(vararg names: String) {
        NAMES.addAll(arrayOf(*names))
    }

    fun startDiscovery(clearCachedDiscoveries: Boolean) {
        if (clearCachedDiscoveries) {
            container.flushContainer()
        }
        if (bluetoothService != null) {
            executeDiscovery = false
            discoverDevices(false) // Don't clear devices container every time
        } else {
            executeDiscovery = true
        }
    }

    fun clearDevicesCache() {
        bluetoothService?.clearCache()
    }

    fun stopDiscovery(clearCachedDiscoveries: Boolean) {
        if (bluetoothService != null) {
            isScanning = false
            bluetoothService?.removeListener(this)
            bluetoothService?.stopDiscoveringDevices(true)
        }
        if (clearCachedDiscoveries) {
            container.flushContainer()
        }
    }

    private fun discoverDevices(clearCache: Boolean) {
        bluetoothService?.addListener(this@Discovery)
        isDeviceStarted = bluetoothService?.discoverDevicesOfInterest(clearCache)!!
    }

    override fun getScanFilters(): List<ScanFilterCompat> {
        val filters: MutableList<ScanFilterCompat> = ArrayList()
        for (service in SERVICES) {
            val filter = ScanFilterCompat()
            filter.serviceUuid = ParcelUuid(service.number)
            filter.serviceUuidMask = ParcelUuid(GattService.UUID_MASK)
            filters.add(filter)
        }
        for (name in NAMES) {
            val filter = ScanFilterCompat()
            filter.deviceName = name
            filters.add(filter)
        }
        return filters
    }

    override fun askForEnablingBluetoothAdapter(): Boolean {
        return true
    }

    override fun onStateChanged(bluetoothAdapterState: Int) {
        if (bluetoothAdapterState == BluetoothAdapter.STATE_OFF) {
            isDeviceStarted = false
            isScanning = isDeviceStarted
            isDeviceReady = null

            if (isBluetoothEnabled) {
                isBluetoothEnabled = false
                host.onAdapterDisabled()
                // Adapter was off, but became turned on:
                // Allow restarting the adapter (askForEnablingBluetoothAdapter will be called at some point)
            }
        } else if (bluetoothAdapterState == BluetoothAdapter.STATE_ON) {
            if (!isBluetoothEnabled) {
                isBluetoothEnabled = true
                // The adapter was off and now turned on again. Re-start discovery to recover.
                host.onAdapterEnabled()
            }
        }
    }

    override fun onScanStarted() {
        isScanning = true
    }

    override fun onScanResultUpdated(devices: List<BluetoothDeviceInfo>?, changedDeviceInfo: BluetoothDeviceInfo?) {
        container.updateWithDevices(devices!!)
    }

    override fun onScanEnded() {
        isScanning = false
        if (host.isReady()) {
            host.reDiscover()
        }
    }

    override fun onDeviceReady(device: BluetoothDevice?, isInteresting: Boolean) {
        val deviceIsConnected = (device != null) && isInteresting
        if (isDeviceReady != null && isDeviceReady == deviceIsConnected) {
            return
        }
        isDeviceReady = deviceIsConnected
    }

    override fun onCharacteristicChanged(characteristicID: GattCharacteristic?, value: Any?) {
        Log.d("onCharacteristicChanged", "$characteristicID=$value")
    }

}