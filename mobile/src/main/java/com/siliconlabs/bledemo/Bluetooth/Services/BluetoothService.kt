package com.siliconlabs.bledemo.Bluetooth.Services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
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
import androidx.core.app.NotificationCompat
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.Bluetooth.ConnectedGatts
import com.siliconlabs.bledemo.Bluetooth.BLE.*
import androidx.annotation.RequiresApi
import com.siliconlabs.bledemo.Bluetooth.Parsing.ScanRecordParser
import com.siliconlabs.bledemo.Browser.Activities.BrowserActivity
import com.siliconlabs.bledemo.Browser.Models.Logs.*
import com.siliconlabs.bledemo.gatt_configurator.utils.BluetoothGattServicesCreator
import com.siliconlabs.bledemo.gatt_configurator.utils.GattConfiguratorStorage
import com.siliconlabs.bledemo.Browser.Models.Logs.ConnectionStateChangeLog
import com.siliconlabs.bledemo.Browser.Models.Logs.DisconnectByButtonLog
import com.siliconlabs.bledemo.Utils.Constants
import com.siliconlabs.bledemo.Utils.LocalService
import timber.log.Timber
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Service handling Bluetooth (regular and BLE) communcations.
 */
private val TAG = BluetoothService::class.java.simpleName

class BluetoothService : LocalService<BluetoothService>() {

    companion object {
        private const val CONNECTION_TIMEOUT = 15000
        private const val SCAN_DEVICE_TIMEOUT = 4000
        private const val RSSI_UPDATE_FREQUENCY = 2000
        private const val PREF_KEY_SAVED_DEVICES = "_pref_key_saved_devs_"

        private const val ACTION_GATT_SERVER_DEBUG_CONNECTION = "com.siliconlabs.bledemo.action.GATT_SERVER_DEBUG_CONNECTION"
        private const val ACTION_GATT_SERVER_REMOVE_NOTIFICATION = "com.siliconlabs.bledemo.action.GATT_SERVER_REMOVE_NOTIFICATION"
        private const val GATT_SERVER_REMOVE_NOTIFICATION_REQUEST_CODE = 666
        private const val GATT_SERVER_DEBUG_CONNECTION_REQUEST_CODE = 888
        private const val GATT_SERVER_OPEN_CONNECTION_REQUEST_CODE = 777
        private const val NOTIFICATION_ID = 999
        const val EXTRA_BLUETOOTH_DEVICE = "EXTRA_BLUETOOTH_DEVICE"
    }

    abstract class Binding(context: Context) : LocalService.Binding<BluetoothService>(context) {
        override fun getServiceClass(): Class<BluetoothService> {
            return BluetoothService::class.java
        }
    }

    enum class GattConnectType {
        THERMOMETER,
        LIGHT,
        RANGE_TEST,
        BLINKY,
        THROUGHPUT_TEST
    }

    class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)
                synchronized(currentState) {
                    currentState.set(state)
                    for (service in registeredServices) {
                        service.notifyBluetoothStateChange(state)
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                var restart = false
                for (service in registeredServices) {
                    if (service.notifyDiscoverFinished()) {
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
            val registeredServices: MutableList<BluetoothService> = ArrayList()
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

    private val mReceiver: BroadcastReceiver = BScanCallback(this)
    private val interestingDevices: MutableMap<String, BluetoothDeviceInfo?> = LinkedHashMap()
    private val discoveredDevices: MutableMap<String, BluetoothDeviceInfo> = LinkedHashMap()
    private val knownDevice = AtomicReference<BluetoothDevice?>()
    private val currentState = Receiver.currentState
    private val listeners = Listeners()

    private lateinit var savedInterestingDevices: SharedPreferences
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var handler: Handler

    private var prevBluetoothState = 0
    private var discoveryStarted = false
    private var isDestroyed = false
    private var useBLE = true

    var bluetoothGattServer: BluetoothGattServer? = null
        private set
    private var gattServerCallback: BluetoothGattServerCallback? = null
    private var bluetoothLEGatt: BluetoothLEGatt? = null
    private var bleScannerCallback: Any? = null
    var bluetoothAdapter: BluetoothAdapter? = null
    var connectedGatt: BluetoothGatt? = null
        private set

    private var gattServerServicesToAdd: LinkedList<BluetoothGattService>? = null

    private val scanTimeout = Runnable {
        stopScanning()
    }

    private val rssiUpdate = object : Runnable {
        override fun run() {
            connectedGatt?.let { gatt ->
                gatt.readRemoteRssi()
                handler.postDelayed(this, RSSI_UPDATE_FREQUENCY.toLong())
            }
        }
    }

    private val connectionTimeout = Runnable {
        connectedGatt?.let { gatt ->
            Log.d("timeout", "called")
            gatt.disconnect()
            gatt.close()
            extraGattCallback?.onTimeout()
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

        registerBluetoothReceiver()
        registerGattServerReceiver()
        initGattServer()
    }

    private fun registerBluetoothReceiver() {
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    private val bluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_ON -> initGattServer()
                    BluetoothAdapter.STATE_OFF -> clearAllGatts()
                }
            }
        }
    }

    private fun initGattServer() {
        if (bluetoothGattServer == null && bluetoothAdapter != null && bluetoothAdapter?.isEnabled!!) {
            bluetoothGattServer = bluetoothManager.openGattServer(this, bluetoothGattServerCallback)
            setGattServer()
        }
    }

    fun clearGattServer() {
        bluetoothGattServer?.clearServices()
        gattServerServicesToAdd?.clear()
    }

    fun setGattServer() {
        val storage = GattConfiguratorStorage(this)
        val gattServer = storage.loadActiveGattServer()
        clearGattServer()

        gattServer?.let { server ->
            gattServerServicesToAdd = BluetoothGattServicesCreator.getBluetoothGattServices(server)
            gattServerServicesToAdd?.let { services ->
                if(services.isNotEmpty()) bluetoothGattServer?.addService(services.pop())
            }
        }
    }

    private fun registerGattServerReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_GATT_SERVER_DEBUG_CONNECTION)
            addAction(ACTION_GATT_SERVER_REMOVE_NOTIFICATION)
        }
        registerReceiver(gattServerBroadcastReceiver, filter)
    }

    override fun onDestroy() {
        isDestroyed = true
        handler.removeCallbacks(scanTimeout)
        stopScanning()
        stopDiscovery()
        clearAllGatts()
        unregisterReceiver(mReceiver)
        unregisterReceiver(gattServerBroadcastReceiver)
        unregisterReceiver(bluetoothReceiver)
        Receiver.registeredServices.remove(this)
        bluetoothLEGatt?.cancel()
        bluetoothGattServer?.close()

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
        synchronized(currentState) {
            listeners.remove(listener)
        }
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
     * [BluetoothService.Listener].
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
            isNotificationEnabled = true
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

        listenerResult?.let {
            handler.post {
                listeners.onScanResultUpdated(listenerResult, listenerChanged)
            }
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
            devInfoForDiscovery.discover(object : BluetoothLEGatt(this@BluetoothService, devInfoForDiscovery.device) {

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
        ConnectedGatts.getByAddress(deviceAddress)?.let {
            ConnectedGatts.clearGatt(deviceAddress)
            if (connectedGatt?.device?.address == deviceAddress) {
                connectedGatt = null
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
        connectedGatt?.let {
            refreshGattDB(it)
        }
    }

    private fun refreshGattDB(gatt: BluetoothGatt) {
        refreshDeviceCache(gatt)
        Timer().schedule(object : TimerTask() {
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

    fun clearConnectedGatt() {
        Log.d(TAG, "clearGatt() called")

        handler.removeCallbacks(rssiUpdate)
        handler.removeCallbacks(connectionTimeout)

        connectedGatt?.apply {
            ConnectedGatts.clearGatt(device.address)
        }
        connectedGatt = null
    }

    fun clearAllGatts() {
        Log.d(TAG, "clearAllGatts() called")

        handler.removeCallbacks(rssiUpdate)
        handler.removeCallbacks(connectionTimeout)

        ConnectedGatts.clearAllGatts()
        connectedGatt = null
    }

    fun registerGattCallback(requestRssiUpdates: Boolean, callback: TimeoutGattCallback?) {
        handler.removeCallbacks(rssiUpdate)
        if (requestRssiUpdates) {
            handler.post(rssiUpdate)
        }
        extraGattCallback = callback
    }

    fun isGattConnected(): Boolean {
        return connectedGatt != null
                && ConnectedGatts.isGattWithAddressConnected(connectedGatt?.device?.address!!)
                && bluetoothManager.getConnectionState(connectedGatt?.device, BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED
    }

    fun isGattConnected(deviceAddress: String?): Boolean {
        return if (deviceAddress == null) {
            false
        } else {
            ConnectedGatts.isGattWithAddressConnected(deviceAddress) && bluetoothManager.getConnectedDevices(BluetoothProfile.GATT).any {
                it.address == deviceAddress
            }
        }
    }

    fun getConnectedGatt(deviceAddress: String?): BluetoothGatt? {
        deviceAddress?.let {
            connectedGatt = ConnectedGatts.getByAddress(it)
        }
        return connectedGatt
    }

    fun connectGatt(device: BluetoothDevice, requestRssiUpdates: Boolean, callback: TimeoutGattCallback? = null): Boolean {
        stopDiscovery()

        callback?.let {
            extraGattCallback = callback
        }

        connectedGatt = if (useBLE) {
            device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(this, false, gattCallback)
        }

        handler.postDelayed(connectionTimeout, CONNECTION_TIMEOUT.toLong())
        connectedGatt?.let {
            if (requestRssiUpdates) {
                handler.post(rssiUpdate)
            } else {
                handler.removeCallbacks(rssiUpdate)
            }
            return true
        }

        return false
    }

    private var extraGattCallback: TimeoutGattCallback? = null

    fun registerGattCallback(callback: TimeoutGattCallback) {
        extraGattCallback = callback
    }

    fun unregisterGattCallback() {
        extraGattCallback = null
    }

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            Timber.d("onConnectionStateChange(): gatt device = %s, status = %d, newState = %d",
                    gatt.device.address, status, newState)
            Constants.LOGS.add(ConnectionStateChangeLog(gatt, status, newState))
            handler.removeCallbacks(connectionTimeout)

            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                ConnectedGatts.addOrSwap(gatt.device.address, gatt)
            }

            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                disconnectGatt(gatt.device.address)
            }

            ConnectedGatts.removePendingConnection(gatt.device.address)

            extraGattCallback?.onConnectionStateChange(gatt, status, newState)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Timber.d("onServicesDiscovered(): gatt device = %s, status = %d",
                    gatt.device.address, status)
            extraGattCallback?.onServicesDiscovered(gatt, status)
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Timber.d("onCharacteristicWrite(): gatt device = %s, characteristic uuid = %s",
                    gatt.device.address, characteristic.uuid)
            extraGattCallback?.onCharacteristicWrite(gatt, characteristic, status)
        }

        override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            super.onDescriptorRead(gatt, descriptor, status)
            Timber.d("onDescriptorRead(): gatt device = %s, descriptor uuid = %s",
                    gatt.device.address, descriptor.uuid)
            extraGattCallback?.onDescriptorRead(gatt, descriptor, status)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            Timber.d("onDescriptorWrite(): gatt device = %s, descriptor uuid = %s",
                    gatt.device.address, descriptor.uuid)
            extraGattCallback?.onDescriptorWrite(gatt, descriptor, status)
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int) {
            super.onReliableWriteCompleted(gatt, status)
            Timber.d("onReliableWriteCompleted(): gatt device = %s, status = %d",
                    gatt.device.address, status)
            extraGattCallback?.onReliableWriteCompleted(gatt, status)
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            Timber.d("onReadRemoteRssi(): gatt device = %s, rssi = %d",
                    gatt.device.address, rssi)
            extraGattCallback?.onReadRemoteRssi(gatt, rssi, status)
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            Timber.d("onMtuChanged(): gatt device = %s, mtu = %d", gatt.device.address, mtu)
            extraGattCallback?.onMtuChanged(gatt, mtu, status)
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            Timber.d("onCharacteristicRead(): gatt device = %s, status = %d",
                    gatt.device.address, status) //todo characteristic value ?

            extraGattCallback?.onCharacteristicRead(gatt, characteristic, status)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            Timber.d("onCharacteristicChanged(): gatt device = %s, uuid = %s",
                gatt.device.address, characteristic.uuid)

            extraGattCallback?.onCharacteristicChanged(gatt, characteristic)
        }

        override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status)
            Timber.d("onPhyUpdate(): gatt device = %s, txPhy = %d, rxPhy = %d, status = %d",
                    gatt?.device?.address, txPhy, rxPhy, status)
            extraGattCallback?.onPhyUpdate(gatt, txPhy, rxPhy, status)
        }
    }

    fun unregisterGattServerCallback() {
        gattServerCallback = null
    }

    fun registerGattServerCallback(callback: BluetoothGattServerCallback) {
        this.gattServerCallback = callback
    }

    var isNotificationEnabled = true

    private val bluetoothGattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)

            val isSuccessfullyConnected = status == BluetoothGatt.GATT_SUCCESS
                    && newState == BluetoothProfile.STATE_CONNECTED

            if (gattServerCallback != null) {
                gattServerCallback?.onConnectionStateChange(device, status, newState)
            } else if (isSuccessfullyConnected && isNotificationEnabled) {
                showDebugConnectionNotification(device)
            }
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
            super.onServiceAdded(status, service)
            gattServerServicesToAdd?.let { services ->
                if (services.isNotEmpty()) bluetoothGattServer?.addService(services.pop())
            }
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            bluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic?.value)
        }

        override fun onDescriptorReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor?) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor)
            bluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor?.value)
        }

        override fun onCharacteristicWriteRequest(device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)
            characteristic?.value = value
            if (responseNeeded) {
                bluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }

        override fun onDescriptorWriteRequest(device: BluetoothDevice?, requestId: Int, descriptor: BluetoothGattDescriptor?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value)
            descriptor?.value = value
            if (responseNeeded) {
                bluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }

        override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
            super.onNotificationSent(device, status)
            gattServerCallback?.onNotificationSent(device, status)
        }
    }

    private fun getYesPendingIntent(device: BluetoothDevice): PendingIntent {
        val intent = Intent(ACTION_GATT_SERVER_DEBUG_CONNECTION)
        intent.putExtra(EXTRA_BLUETOOTH_DEVICE, device)
        return PendingIntent.getBroadcast(this, GATT_SERVER_DEBUG_CONNECTION_REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    private fun getNoPendingIntent(): PendingIntent {
        val intent = Intent(ACTION_GATT_SERVER_REMOVE_NOTIFICATION)
        return PendingIntent.getBroadcast(this, GATT_SERVER_REMOVE_NOTIFICATION_REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    private fun getYesAndOpenPendingIntent(device: BluetoothDevice): PendingIntent {
        val intent = Intent(this, BrowserActivity::class.java)
        intent.putExtra(EXTRA_BLUETOOTH_DEVICE, device)
        return TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(GATT_SERVER_OPEN_CONNECTION_REQUEST_CODE, PendingIntent.FLAG_UPDATE_CURRENT)
        } as PendingIntent
    }

    private fun showDebugConnectionNotification(device: BluetoothDevice) {
        val deviceName = device.name ?: getString(R.string.not_advertising_shortcut)
        val CHANNEL_ID = "DEBUG_CONNECTION_CHANNEL"
        createNotificationChannel(CHANNEL_ID)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.notification_title_device_has_connected, deviceName))
                .setContentText(getString(R.string.notification_note_debug_connection))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(0, getString(R.string.button_yes), getYesPendingIntent(device))
                .addAction(0, getString(R.string.notification_button_yes_and_open), getYesAndOpenPendingIntent(device))
                .addAction(0, getString(R.string.button_no), getNoPendingIntent())
                .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private val gattServerBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == ACTION_GATT_SERVER_DEBUG_CONNECTION) {
                val device = intent.getParcelableExtra<BluetoothDevice>(EXTRA_BLUETOOTH_DEVICE)
                device?.let {
                    connectGatt(device, false, null)
                }

                closeGattServerNotification()
            } else if (action == ACTION_GATT_SERVER_REMOVE_NOTIFICATION) {
                closeGattServerNotification()
            }
        }
    }

    fun closeGattServerNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
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
