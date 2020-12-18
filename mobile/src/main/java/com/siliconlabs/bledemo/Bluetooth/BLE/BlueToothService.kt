package com.siliconlabs.bledemo.Bluetooth.BLE

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import com.siliconlabs.bledemo.Bluetooth.Parsing.ScanRecordParser
import com.siliconlabs.bledemo.Browser.Models.Logs.*
import com.siliconlabs.bledemo.Utils.Constants
import com.siliconlabs.bledemo.Utils.LocalService
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Service handling Bluetooth (regular and BLE) communcations.
 */
class BlueToothService : LocalService<BlueToothService>() {

    companion object {
        // If connection is not successfully established or error message received after CONNECTION_TIMEOUT milliseconds.
        private const val CONNECTION_TIMEOUT = 15000

        // If a scan of one device's services takes more than SCAN_DEVICE_TIMEOUT milliseconds, cancel it.
        private const val SCAN_DEVICE_TIMEOUT = 4000
        private const val PREF_KEY_SAVED_DEVICES = "_pref_key_saved_devs_"
        private val TAG = BlueToothService::class.java.simpleName
        private const val RSSI_UPDATE_FREQ: Long = 2000
    }

    abstract class Binding(context: Context) : LocalService.Binding<BlueToothService>(context) {

        override fun getServiceClass(): Class<BlueToothService> {
            return BlueToothService::class.java
        }
    }

    enum class GattConnectType {
        THERMOMETER, LIGHT, RANGE_TEST
    }

    class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)
                synchronized(currentState) {
                    currentState.set(state)
                    for (blueToothService in registeredServices) {
                        blueToothService.notifyBluetoothStateChange(state)
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                var restart = false
                for (blueToothService in registeredServices) {
                    if (blueToothService.notifyDiscoverFinished()) {
                        restart = true
                    }
                }
                if (restart) {
                    registeredServices[0].bluetoothAdapter?.startDiscovery()
                }
            }
        }

        companion object {
            val currentState = AtomicInteger(0)
            val registeredServices: MutableList<BlueToothService> = ArrayList()
        }
    }

    interface Listener {
        /**
         * If this returns a non-empty list, scanned devices will be matched by the
         * return filters. A device will be scanned and recognized if it advertisement matches
         * one of more filters returned by this method.
         *
         *
         * An empty list (or null) will match every bluetooth device, i.e. no filtering.
         *
         * @return The filters by which the BLE scanning will be filtered.
         */
        fun getScanFilters(): List<ScanFilterCompat>?

        /**
         * This method should get the user's permission (at least the first time around) to
         * enable the bluetooth automatically when necessary.
         *
         * @return True only if the user allows the code to automatically enable the bluetooth adapter.
         */
        fun askForEnablingBluetoothAdapter(): Boolean

        /**
         * Called when the Bluetooth-adapter state changes.
         *
         * @param bluetoothAdapterState State of the adapter.
         */
        fun onStateChanged(bluetoothAdapterState: Int)

        /**
         * Called when a discovery of bluetooth devices has started.
         */
        fun onScanStarted()

        /**
         * Called when a new bluetooth device has ben discovered or when an already discovered bluetooth
         * device's information has been updated.
         *
         * @param devices           List of all bluetooth devices currently discovered by the scan since [.onScanStarted].
         * @param changedDeviceInfo Indicates which device in 'devices' is new or updated (can be ignored).
         */
        fun onScanResultUpdated(devices: List<BluetoothDeviceInfo>?, changedDeviceInfo: BluetoothDeviceInfo?)

        /**
         * Called when the current discovery process has ended.
         * Note that this method may be called more than once after a call to [.onScanStarted].
         */
        fun onScanEnded()

        /**
         * Called when a interesting device is ready to be used for communication (it is connected and ready).
         * It is possible that after this method is called with a non-null device parameter, it can be called
         * with a null device parameter value later when the device gets disconnected or some other
         * error occurs.
         *
         * @param device Device that is the currently selected device or null if something went wrong.
         */
        fun onDeviceReady(device: BluetoothDevice?, isInteresting: Boolean)

        /**
         * Called when a device is connected and one of its characteristics has changed.
         *
         * @param characteristic The characteristic.
         * @param value          The new value of the characteristic.
         */
        fun onCharacteristicChanged(characteristic: GattCharacteristic?, value: Any?)
    }

    private lateinit var savedInterestingDevices: SharedPreferences
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var handler: Handler

    private var discoveryStarted = false
    private var isDestroyed = false
    private var useBLE = true

    private var prevBluetoothState = 0

    var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLEGatt: BluetoothLEGatt? = null

    private val discoveredDevices: MutableMap<String, BluetoothDeviceInfo> = LinkedHashMap()
    private val interestingDevices: MutableMap<String, BluetoothDeviceInfo?> = LinkedHashMap()

    private val mReceiver: BroadcastReceiver = BScanCallback(this)


    private val knownDevice = AtomicReference<BluetoothDevice?>()
    private val currentState = Receiver.currentState
    private val listeners = Listeners()
    var connectedGatt: BluetoothGatt? = null
        private set
    private var extraCallback: TimeoutGattCallback? = null
    var gattMap: MutableMap<String?, BluetoothGatt?> = HashMap()

    private var bleScannerCallback: Any? = null


    private val scanTimeout = Runnable { stopScanning() }
    private val rssiUpdate: Runnable = object : Runnable {
        override fun run() {
            connectedGatt?.let { gatt ->
                gatt.readRemoteRssi()
                handler.postDelayed(this, RSSI_UPDATE_FREQ)
            }
        }
    }
    private val connectionTimeout: Runnable = Runnable {
        connectedGatt?.let { gatt ->
            Log.d("timeout", "called")
            gatt.disconnect()
            gatt.close()
            extraCallback?.onTimeout()
        }
    }

    /**
     * Preference whose [.PREF_KEY_SAVED_DEVICES] key will have a set addresses of known devices.
     * Note that this list only grows... since we don't expect a phone/device to come into contact with many
     * interesting devices, this is fine. In the future we may want to back this by a LRU list or something similar to purge
     * device-addresses that have been used a long time ago.
     */

    override fun onCreate() {
        super.onCreate()

        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            stopSelf()
            return
        }

        if (useBLE) {
            useBLE = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        }

        savedInterestingDevices = PreferenceManager.getDefaultSharedPreferences(this)
        handler = Handler()

        knownDevice.set(null)
        discoveredDevices.clear()
        interestingDevices.clear()

        synchronized(currentState) { currentState.set(bluetoothAdapter?.state!!) }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(mReceiver, filter)
        Receiver.registeredServices.add(this)
    }

    override fun onDestroy() {
        isDestroyed = true
        handler.removeCallbacks(scanTimeout)
        stopScanning()
        stopDiscovery()
        clearGatt()
        unregisterReceiver(mReceiver)
        Receiver.registeredServices.remove(this)
        bluetoothLEGatt?.cancel()

        super.onDestroy()
    }

    fun addListener(listener: Listener) {
        synchronized(currentState) {
            if (!listeners.contains(listener)) {
                notifyInitialStateForListener(listener)
                listeners.add(listener)
            }
        }
    }

    private fun notifyInitialStateForListener(listener: Listener) {
        handler.post {
            val state = Receiver.currentState.get()
            if (state == BluetoothAdapter.STATE_ON) {
                listener.onStateChanged(state)
                if (discoveryStarted) {
                    listener.onScanStarted()
                }
            } else {
                listener.onStateChanged(state)
            }
        }
    }

    fun removeListener(listener: Listener?) {
        synchronized(currentState) { listeners.remove(listener) }
    }

    /**
     * Reads the value of a characteristic of the currently connected device.
     *
     * @param characteristic Characteristic whose value will be read.
     */
    fun read(characteristic: GattCharacteristic) {
        if (bluetoothLEGatt == null) {
            listeners.onCharacteristicChanged(characteristic, null)
            return
        }
        bluetoothLEGatt?.read(characteristic.number)
    }

    /**
     * This method discovers which devices of interest are available.
     *
     *
     * If this method returns true (discovery started), the caller should wait for the onScanXXX methods of
     * [BlueToothService.Listener].
     *
     * @param clearCache True if the cache/list of the currently discovered devices should be cleared.
     * @return True if discovery started.
     */
    fun discoverDevicesOfInterest(clearCache: Boolean): Boolean {
        if (!bluetoothAdapter?.isEnabled!!) {
            return false
        }
        discoveryStarted = true
        listeners.onScanStarted()
        if (clearCache) {
            synchronized(discoveredDevices) {
                discoveredDevices.clear()
                interestingDevices.clear()
            }
        }
        startDiscovery()
        return true
    }

    fun clearCache() {
        synchronized(discoveredDevices) {
            discoveredDevices.clear()
            interestingDevices.clear()
        }
    }

    fun stopDiscoveringDevices(clearCache: Boolean) {
        if (discoveryStarted) {
            if (clearCache) {
                synchronized(discoveredDevices) {
                    discoveredDevices.clear()
                    interestingDevices.clear()
                }
            }
            stopDiscovery()
            if (!scanDiscoveredDevices()) {
                onScanningCanceled()
            }
        }
    }

    /**
     * If the call [.startOrDiscoverDeviceOfInterest] returned true, a device is currently connected or about to be connected.
     * This method will disconnected from the currently connected device or about any ongoing attempt to connect.
     */
    private fun stopConnectedDevice() {
        bluetoothLEGatt?.cancel()
        bluetoothLEGatt = null
    }

    private fun startDiscovery() {
        if (bluetoothAdapter == null) {
            bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager.adapter
        }
        handler.removeCallbacks(scanTimeout)
        if (useBLE) {
            val scannerCallback: ScanCallback = BLEScanCallbackLollipop(this)
            bleScannerCallback = scannerCallback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val settings = ScanSettings.Builder()
                        .setLegacy(false)
                        .setReportDelay(0)
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
                bluetoothAdapter?.bluetoothLeScanner?.startScan(listeners.scanFilterL as List<ScanFilter?>?, settings, scannerCallback)
            } else {
                val settings = ScanSettings.Builder()
                        .setReportDelay(0)
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
                bluetoothAdapter?.bluetoothLeScanner?.startScan(listeners.scanFilterL as List<ScanFilter?>?, settings, scannerCallback)
            }
        } else {
            if (!bluetoothAdapter?.startDiscovery()!!) {
                onDiscoveryCanceled()
            }
        }
    }

    fun onDiscoveryCanceled() {
        if (discoveryStarted) {
            discoveryStarted = false
            listeners.onScanEnded()
        }
    }

    private fun stopDiscovery() {
        if (bluetoothAdapter == null) {
            return
        }
        if (useBLE) {
            if (bluetoothAdapter?.bluetoothLeScanner == null) {
                return
            }
            if (bleScannerCallback != null) {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(bleScannerCallback as ScanCallback?)
            }
        } else if (bluetoothAdapter?.isDiscovering!!) {
            bluetoothAdapter?.cancelDiscovery()
        }
    }

    private fun stopScanning() {
        var leGattsToClose: MutableCollection<BluetoothLEGatt?>
        synchronized(discoveredDevices) {
            leGattsToClose = ArrayList(discoveredDevices.size)
            for (devInfo in discoveredDevices.values) {
                leGattsToClose.add(devInfo.gattHandle as BluetoothLEGatt?)
            }
        }
        BluetoothLEGatt.cancelAll(leGattsToClose)
    }

    private fun setKnownDevice(device: BluetoothDevice?) {
        knownDevice.set(device)
    }

    fun notifyBluetoothStateChange(newState: Int) {
        if (newState == BluetoothAdapter.STATE_TURNING_OFF) {
            stopScanning()
            stopConnectedDevice()
        }
        handler.post {
            if (prevBluetoothState != newState) {
                if (newState == BluetoothAdapter.STATE_OFF) {
                    if (discoveryStarted) {
                        discoveryStarted = false
                        listeners.onScanEnded()
                    }
                    synchronized(discoveredDevices) {
                        discoveredDevices.clear()
                        interestingDevices.clear()
                    }
                    setKnownDevice(null)
                    listeners.onDeviceReady(null, false)
                    listeners.onStateChanged(newState)
                } else if (newState == BluetoothAdapter.STATE_ON) {
                    listeners.onStateChanged(newState)
                } else {
                    listeners.onStateChanged(newState)
                }
                prevBluetoothState = newState
            }
        }
    }

    fun notifyDiscoverFinished(): Boolean {
        if (useBLE) {
            return false
        }
        val continueScanning = !isDestroyed && knownDevice.get() == null
        if (!continueScanning) {
            if (discoveryStarted) {
                discoveryStarted = false
                listeners.onScanEnded()
            }
        }
        return continueScanning
    }

    fun addDiscoveredDevice(result: ScanResultCompat): Boolean {
        Log.d(TAG, "addDiscoveredDevice: $result")
        if (knownDevice.get() != null) {
            return true
        }
        var listenerResult: ArrayList<BluetoothDeviceInfo>?
        var listenerChanged: BluetoothDeviceInfo?
        val device = result.device
        var devInfo: BluetoothDeviceInfo?
        synchronized(discoveredDevices) {
            val address = device?.address!!
            devInfo = discoveredDevices[address]
            if (devInfo == null) {
                devInfo = BluetoothDeviceInfo()
                devInfo!!.device = device
                discoveredDevices[address] = devInfo!!
            } else {
                devInfo!!.device = device
            }
            devInfo!!.scanInfo = result
            if (!devInfo!!.isConnectable) {
                devInfo!!.isConnectable = result.isConnectable
            }
            devInfo!!.count++
            if (devInfo!!.timestampLast == 0L) {
                devInfo!!.timestampLast = result.timestampNanos
            } else {
                devInfo!!.setIntervalIfLower(result.timestampNanos - devInfo!!.timestampLast)
                devInfo!!.timestampLast = result.timestampNanos
            }
            devInfo!!.rawData = ScanRecordParser.getRawAdvertisingDate(result.scanRecord?.bytes)
            devInfo!!.isNotOfInterest = false
            devInfo!!.isOfInterest = true
            if (!interestingDevices.containsKey(address)) {
                interestingDevices[address] = devInfo
            }
            if (!listeners.isEmpty()) {
                listenerResult = ArrayList(discoveredDevices.size)
                listenerChanged = devInfo!!.clone()
                for (di in discoveredDevices.values) {
                    listenerResult?.add(di.clone())
                }
            } else {
                listenerResult = null
                listenerChanged = null
            }
        }
        if (listenerResult != null) {
            handler.post { listeners.onScanResultUpdated(listenerResult, listenerChanged) }
        }
        return false
    }

    private fun scanDiscoveredDevices(): Boolean {
        Log.d("scanDiscoveredDevices", "called")
        handler.removeCallbacks(scanTimeout)
        handler.postDelayed(scanTimeout, SCAN_DEVICE_TIMEOUT.toLong())
        if (knownDevice.get() != null) {
            return false
        }
        var devInfo: BluetoothDeviceInfo? = null
        synchronized(discoveredDevices) {
            val devices: Collection<BluetoothDeviceInfo> = discoveredDevices.values
            for (di in devices) {
                if (di.isUnDiscovered()) {
                    devInfo = di
                    break
                }
            }
            if (devInfo == null) {
                Log.d("scanDiscoveredDevices", "called: Nothing left!")
                return false
            }
            val devInfoForDiscovery: BluetoothDeviceInfo = devInfo!!
            devInfoForDiscovery.discover(object : BluetoothLEGatt(this@BlueToothService, devInfoForDiscovery.device) {

                override fun setGattServices(services: List<BluetoothGattService>?) {
                    super.setGattServices(services)
                    close()
                    updateDiscoveredDevice(devInfoForDiscovery, interestingServices, true)
                }
            })
        }
        Log.d("scanDiscoveredDevices", " called: Next up is " + devInfo?.device?.address)
        return true
    }

    private fun onScanningCanceled() {
        handler.removeCallbacks(scanTimeout)
        if (discoveryStarted) {
            discoveryStarted = false
            listeners.onScanEnded()
        }
    }

    fun updateDiscoveredDevice(devInfo: BluetoothDeviceInfo, services: List<BluetoothGattService?>?, keepScanning: Boolean) {
        var listenerResult: ArrayList<BluetoothDeviceInfo>?
        var listenerChanged: BluetoothDeviceInfo?
        synchronized(discoveredDevices) {
            devInfo.gattHandle = null
            if (services == null) {
                devInfo.serviceDiscoveryFailed = true
            } else {
                devInfo.serviceDiscoveryFailed = false
                devInfo.isOfInterest = services.isNotEmpty()
                devInfo.isNotOfInterest = services.isEmpty()
            }
            devInfo.areServicesBeingDiscovered = false
            if (devInfo.isOfInterest) {
                interestingDevices[devInfo.device.address] = devInfo
                val devices = savedInterestingDevices.getStringSet(PREF_KEY_SAVED_DEVICES, null)
                val knownDevices: MutableSet<String> = devices?.let { HashSet(it) } ?: HashSet()
                knownDevices.add(devInfo.device.address)
                savedInterestingDevices.edit().putStringSet(PREF_KEY_SAVED_DEVICES, knownDevices).apply()
            }
            if (!listeners.isEmpty()) {
                listenerResult = ArrayList(discoveredDevices.size)
                listenerChanged = devInfo.clone()
                for (di in discoveredDevices.values) {
                    listenerResult?.add(di.clone())
                }
            } else {
                listenerResult = null
                listenerChanged = null
            }
        }
        handler.post {
            var resumeScanning = !isDestroyed && keepScanning
            if (resumeScanning) {
                resumeScanning = scanDiscoveredDevices()
            }
            if (listenerResult != null) {
                listeners.onScanResultUpdated(listenerResult, listenerChanged)
            }
            if (!resumeScanning) {
                onScanningCanceled()
            }
        }
    }

    fun disconnectGatt(deviceAddress: String): Boolean {
        var disconnect = false
        if (connectedGatt != null && connectedGatt?.device?.address == deviceAddress) {
            disconnect = true
        } else {
            connectedGatt = gattMap[deviceAddress]
            if (connectedGatt != null) {
                disconnect = true
            }
        }
        if (disconnect) {
            clearGatt(connectedGatt)
            connectedGatt = null
            if (gattMap.containsKey(deviceAddress)) {
                clearGatt(gattMap[deviceAddress])
                gattMap.remove(deviceAddress)
            }
            handler.removeCallbacks(rssiUpdate)
            handler.removeCallbacks(connectionTimeout)
            Constants.LOGS.add(DisconnectByButtonLog(deviceAddress))
            return true
        }
        return false
    }

    fun discoverGattServices() {
        connectedGatt?.discoverServices()
    }

    fun refreshGattServices() {
        connectedGatt?.let { refreshGattDB(it) }
    }

    private fun refreshGattDB(gatt: BluetoothGatt) {
        refreshDeviceCache(gatt)
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                gatt.discoverServices()
            }
        }, 500)
    }

    private fun refreshDeviceCache(gatt: BluetoothGatt?): Boolean {
        try {
            Log.d("refreshDevice", "Called")
            val localMethod: Method = gatt?.javaClass?.getMethod("refresh")!!
            val bool: Boolean = (localMethod.invoke(gatt, *arrayOfNulls(0)) as Boolean)
            Log.d("refreshDevice", "bool: $bool")
            return bool
        } catch (localException: Exception) {
            Log.e("refreshDevice", "An exception occured while refreshing device")
        }
        return false
    }

    private fun clearGatt(bluetoothGatt: BluetoothGatt?) {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
    }

    fun clearGatt() {
        handler.removeCallbacks(rssiUpdate)
        handler.removeCallbacks(connectionTimeout)

        Log.d("clearGatt", "called")
        connectedGatt?.disconnect()
        connectedGatt?.close()
        connectedGatt = null

    }

    fun clearAllGatts() {
        handler.removeCallbacks(rssiUpdate)
        handler.removeCallbacks(connectionTimeout)

        Log.d("clearAllGatts", "called")

        for ((_, gatt) in gattMap) {
            gatt?.disconnect()
            gatt?.close()
        }

        connectedGatt = null
    }

    fun registerGattCallback(requestRssiUpdates: Boolean, callback: TimeoutGattCallback?) {
        if (requestRssiUpdates) {
            handler.post(rssiUpdate)
        } else {
            handler.removeCallbacks(rssiUpdate)
        }
        extraCallback = callback
    }

    val isGattConnected: Boolean
        get() = connectedGatt != null && bluetoothManager.getConnectionState(connectedGatt?.device, BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED

    fun isGattConnected(deviceAddress: String?): Boolean {
        val list = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
        for (bd in list) {
            if (deviceAddress != null && bd.address.contains(deviceAddress)) {
                return true
            }
        }
        return false
    }

    fun getConnectedGatt(deviceAddress: String?): BluetoothGatt? {
        if (gattMap[deviceAddress] != null) {
            connectedGatt = gattMap[deviceAddress]
        }
        return connectedGatt
    }

    fun connectGatt(device: BluetoothDevice, requestRssiUpdates: Boolean, callback: TimeoutGattCallback?): Boolean {
        stopDiscovery()

        val devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
        if (callback != null) {
            extraCallback = callback
        }
        if (devices.contains(device)) {
            if (connectedGatt != null) {
                if (connectedGatt?.device == device) {
                    if (requestRssiUpdates) {
                        handler.post(rssiUpdate)
                    } else {
                        handler.removeCallbacks(rssiUpdate)
                    }
                }
            }
            return true
        }
        val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                Log.d(TAG, "onConnectionStateChange: ")
                Constants.LOGS.add(ConnectionStateChangeLog(gatt, status, newState))
                super.onConnectionStateChange(gatt, status, newState)
                gattMap[device.address] = gatt
                handler.removeCallbacks(connectionTimeout)

                if (extraCallback != null && extraCallback.toString().contains("RangeTestActivity")) {
                    extraCallback?.onConnectionStateChange(gatt, status, newState)
                } else {
                    if (callback != null && newState == BluetoothGatt.STATE_DISCONNECTED) {
                        callback.onConnectionStateChange(gatt, status, newState)
                    }
                    if (extraCallback != null && !(extraCallback.toString().contains("Browser") && newState == BluetoothGatt.STATE_DISCONNECTED)) {
                        extraCallback?.onConnectionStateChange(gatt, status, newState)
                    }

                    if (extraCallback != null && extraCallback.toString().contains("DeviceServicesActivity") && newState == BluetoothGatt.STATE_DISCONNECTED) {
                        extraCallback?.onConnectionStateChange(gatt, status, newState)
                    }
                }



                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    try {
                        gatt.close()
                    } catch (e: Exception) {
                        Log.d(TAG, "close ignoring: $e")
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                Log.d(TAG, "onServicesDiscovered: ")
                Constants.LOGS.add(ServicesDiscoveredLog(gatt, status))
                super.onServicesDiscovered(gatt, status)

                extraCallback?.onServicesDiscovered(gatt, status)
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                Log.d(TAG, "onCharacteristicWrite: $characteristic")
                Constants.LOGS.add(GattOperationWithDataLog("onCharacteristicWrite", gatt, status, characteristic))
                super.onCharacteristicWrite(gatt, characteristic, status)

                extraCallback?.onCharacteristicWrite(gatt, characteristic, status)
            }

            override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                Log.d(TAG, "onDescriptorRead: ")
                Constants.LOGS.add(CommonLog("onDescriptorRead, " + "device: " + gatt.device.address + ", status: " + status, gatt.device.address))
                super.onDescriptorRead(gatt, descriptor, status)

                extraCallback?.onDescriptorRead(gatt, descriptor, status)
            }

            override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                Log.d(TAG, "onDescriptorWrite: ")
                Constants.LOGS.add(CommonLog("onDescriptorWrite, " + "device: " + gatt.device.address + ", status: " + status, gatt.device.address))
                super.onDescriptorWrite(gatt, descriptor, status)

                extraCallback?.onDescriptorWrite(gatt, descriptor, status)
            }

            override fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int) {
                Log.d(TAG, "onReliableWriteCompleted: ")
                Constants.LOGS.add(CommonLog("onReliableWriteCompleted, " + "device: " + gatt.device.address + ", status: " + status, gatt.device.address))
                super.onReliableWriteCompleted(gatt, status)

                extraCallback?.onReliableWriteCompleted(gatt, status)
            }

            override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
                Log.d(TAG, "onReadRemoteRssi: ")
                Constants.LOGS.add(CommonLog("onReadRemoteRssi, " + "device: " + gatt.device.address + ", status: " + status + ", rssi: " + rssi, gatt.device.address))
                super.onReadRemoteRssi(gatt, rssi, status)

                extraCallback?.onReadRemoteRssi(gatt, rssi, status)
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                Log.d(TAG, "onMtuChanged: ")
                Constants.LOGS.add(CommonLog("onMtuChanged, " + "device: " + gatt.device.address + ", status: " + status + ", mtu: " + mtu, gatt.device.address))
                super.onMtuChanged(gatt, mtu, status)

                extraCallback?.onMtuChanged(gatt, mtu, status)
            }

            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                super.onCharacteristicRead(gatt, characteristic, status)
                Log.d(TAG, "onCharacteristicRead: " + gatt.device.address + status) //todo charact value ?
                Constants.LOGS.add(GattOperationWithDataLog("onCharacteristicRead", gatt, status, characteristic))

                extraCallback?.onCharacteristicRead(gatt, characteristic, status)
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                Log.d(TAG, "onCharacteristicChanged: " + gatt.device.address + characteristic.uuid.toString() + characteristic.value) //todo charact value ?
                Constants.LOGS.add(GattOperationWithDataLog("onCharacteristicChanged", gatt, null, characteristic))
                super.onCharacteristicChanged(gatt, characteristic)

                extraCallback?.onCharacteristicChanged(gatt, characteristic)
            }
        }
        connectedGatt = if (useBLE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(this, false, gattCallback)
        }
        handler.postDelayed(connectionTimeout, CONNECTION_TIMEOUT.toLong())
        if (connectedGatt != null) {
            if (requestRssiUpdates) {
                handler.post(rssiUpdate)
            }
            return true
        }
        return false
    }

    private class Listeners : ArrayList<Listener>(), Listener {
        val scanFilterL: List<*>?
            get() {
                val scanFiltersCompat = getScanFilters()
                val scanFilters: MutableList<ScanFilter>? = if (scanFiltersCompat != null) ArrayList(scanFiltersCompat.size) else null
                return if (scanFiltersCompat != null) {
                    for (scanFilterCompat in scanFiltersCompat) {
                        scanFilters?.add(scanFilterCompat.createScanFilter())
                    }
                    if (scanFilters?.isEmpty()!!) null else scanFilters
                } else {
                    null
                }
            }

        override fun getScanFilters(): List<ScanFilterCompat>? {
            val result: MutableList<ScanFilterCompat> = ArrayList()
            for (listener in this) {
                val scanFilters = listener.getScanFilters()
                if (scanFilters != null) {
                    for (scanFilter in scanFilters) {
                        if (!result.contains(scanFilter)) {
                            result.add(scanFilter)
                        }
                    }
                }
            }
            return if (result.isEmpty()) null else result
        }

        override fun askForEnablingBluetoothAdapter(): Boolean {
            for (listener in this) {
                if (listener.askForEnablingBluetoothAdapter()) {
                    return true
                }
            }
            return false
        }

        override fun onStateChanged(bluetoothAdapterState: Int) {
            for (listener in this) {
                listener.onStateChanged(bluetoothAdapterState)
            }
        }

        override fun onScanStarted() {
            for (listener in this) {
                listener.onScanStarted()
            }
        }

        override fun onScanResultUpdated(devices: List<BluetoothDeviceInfo>?, changedDeviceInfo: BluetoothDeviceInfo?) {
            for (listener in this) {
                listener.onScanResultUpdated(devices, changedDeviceInfo)
            }
        }

        override fun onScanEnded() {
            for (listener in this) {
                listener.onScanEnded()
            }
        }

        override fun onDeviceReady(device: BluetoothDevice?, isInteresting: Boolean) {
            for (listener in this) {
                listener.onDeviceReady(device, isInteresting)
            }
        }

        override fun onCharacteristicChanged(characteristic: GattCharacteristic?, value: Any?) {
            for (listener in this) {
                listener.onCharacteristicChanged(characteristic, value)
            }
        }
    }
}