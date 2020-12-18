package com.siliconlabs.bledemo.Bluetooth.BLE

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import java.util.*

class Discovery(private val container: DeviceContainer<BluetoothDeviceInfo>, private val host: BluetoothDiscoveryHost) : BlueToothService.Listener {
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

    private var bluetoothBinding: BlueToothService.Binding? = null
    private var blueToothService: BlueToothService? = null

    private var executeDiscovery = false
    private var isBluetoothEnabled = false
    private var isScanning = false
    private var isDeviceStarted = false
    private var isDeviceReady: Boolean? = null

    fun connect(context: Context) {
        bluetoothBinding = object : BlueToothService.Binding(context) {
            override fun onBound(service: BlueToothService?) {
                blueToothService?.removeListener(this@Discovery)

                blueToothService = service

                blueToothService?.let {
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
        blueToothService = null
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

    fun startDiscovery(clearCachedDiscoveries: Boolean) {
        if (clearCachedDiscoveries) {
            container.flushContainer()
        }
        if (blueToothService != null) {
            executeDiscovery = false
            discoverDevices(false) // Don't clear devices container every time
        } else {
            executeDiscovery = true
        }
    }

    fun clearDevicesCache() {
        blueToothService?.clearCache()
    }

    fun stopDiscovery(clearCachedDiscoveries: Boolean) {
        if (blueToothService != null) {
            isScanning = false
            blueToothService?.removeListener(this)
            blueToothService?.stopDiscoveringDevices(true)
        }
        if (clearCachedDiscoveries) {
            container.flushContainer()
        }
    }

    private fun discoverDevices(clearCache: Boolean) {
        blueToothService?.addListener(this@Discovery)
        isDeviceStarted = blueToothService?.discoverDevicesOfInterest(clearCache)!!
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