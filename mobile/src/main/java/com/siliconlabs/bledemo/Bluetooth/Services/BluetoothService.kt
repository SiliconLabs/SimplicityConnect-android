package com.siliconlabs.bledemo.bluetooth.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.location.LocationManagerCompat
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.bluetooth.ble.*
import com.siliconlabs.bledemo.features.configure.advertiser.activities.PendingServerConnectionActivity
import com.siliconlabs.bledemo.features.configure.advertiser.services.AdvertiserService
import com.siliconlabs.bledemo.features.configure.gatt_configurator.utils.BluetoothGattServicesCreator
import com.siliconlabs.bledemo.features.configure.gatt_configurator.utils.GattConfiguratorStorage
import com.siliconlabs.bledemo.features.scan.browser.models.logs.*
import com.siliconlabs.bledemo.home_screen.activities.MainActivity
import com.siliconlabs.bledemo.utils.LocalService
import com.siliconlabs.bledemo.utils.Notifications
import com.siliconlabs.bledemo.utils.UuidConsts
import timber.log.Timber
import java.lang.reflect.Method
import java.util.*

/**
 * Service handling Bluetooth (regular and BLE) communications.
 */

@SuppressWarnings("LogNotTimber")
class BluetoothService : LocalService<BluetoothService>() {

    companion object {
        private const val RECONNECTION_RETRIES = 3
        private const val RECONNECTION_DELAY = 1000L //connection drops after ~ 4s when reconnecting without delay
        private const val CONNECTION_TIMEOUT = 20000L
        private const val RSSI_UPDATE_FREQUENCY = 2000L

        private const val ACTION_GATT_SERVER_DEBUG_CONNECTION = "com.siliconlabs.bledemo.action.GATT_SERVER_DEBUG_CONNECTION"
        private const val ACTION_GATT_SERVER_REMOVE_NOTIFICATION = "com.siliconlabs.bledemo.action.GATT_SERVER_REMOVE_NOTIFICATION"
        private const val GATT_SERVER_REMOVE_NOTIFICATION_REQUEST_CODE = 666
        private const val GATT_SERVER_DEBUG_CONNECTION_REQUEST_CODE = 888
        private const val GATT_SERVER_OPEN_CONNECTION_REQUEST_CODE = 777
        private const val NOTIFICATION_ID = 999
        private const val CHANNEL_ID = "DEBUG_CONNECTION_CHANNEL"
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
        BLINKY_THUNDERBOARD,
        THROUGHPUT_TEST,
        WIFI_COMMISSIONING,
        MOTION,
        ENVIRONMENT,
        IOP_TEST
    }

    interface ScanListener {

        /**
         * Called when a new scan result has been obtained. There can be two different sources for
         * this: BleScanCallback or BluetoothScanCallback. Result is filled with informations
         * contained in those callbacks.
         *
         * @param scanResult    Data obtained through bluetooth scanner.
         */
        fun handleScanResult(scanResult: ScanResultCompat)

        /**
         * Called when scanning has ended due to system errors.
         */
        fun onDiscoveryFailed()

    }

    private val scanReceiver: BroadcastReceiver = BluetoothScanCallback(this)
    private val scanListeners = ScanListeners()

    var servicesStateListener: ServicesStateListener? = null

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var handler: Handler

    private var useBLE = true

    var bluetoothGattServer: BluetoothGattServer? = null
        private set
    private var gattServerCallback: BluetoothGattServerCallback? = null
    private var bleScannerCallback: BleScanCallback? = null
    var bluetoothAdapter: BluetoothAdapter? = null
    var connectedGatt: BluetoothGatt? = null
        private set

    private val pendingConnections: MutableMap<String, GattConnection> = mutableMapOf()
    private val activeConnections: MutableMap<String, ConnectedDeviceInfo> = mutableMapOf()
    private var retryAttempts = 0

    private val connectionLogs: MutableList<Log> = mutableListOf()

    private var gattServerServicesToAdd: LinkedList<BluetoothGattService>? = null
    private val devicesToNotify = mutableMapOf<UUID, MutableSet<BluetoothDevice>>()
    private val devicesToIndicate = mutableMapOf<UUID, MutableSet<BluetoothDevice>>()

    var isNotificationEnabled = true

    private val rssiUpdateRunnable = object : Runnable {
        override fun run() {
            synchronized(activeConnections) {
                activeConnections.values.forEach {
                    if (it.connection.hasRssiUpdates) {
                        it.connection.gatt?.readRemoteRssi()
                    }
                }
            }
            handler.postDelayed(this, RSSI_UPDATE_FREQUENCY)
        }
    }

    private val connectionTimeoutRunnable = Runnable {
        connectedGatt?.let { gatt ->
            addConnectionLog(TimeoutLog(gatt.device))
            gatt.disconnect()
            reconnectionRunnable?.let { handler.removeCallbacks(it) }
            reconnectionRunnable = null
            extraGattCallback?.onTimeout()
            retryAttempts = 0
        }
    }

    private var reconnectionRunnable: ReconnectionRunnable? = null

    inner class ReconnectionRunnable(val connection: GattConnection) : Runnable {
        override fun run() {
            connection.gatt?.let {
                connectGatt(it.device, connection.hasRssiUpdates, null, true)
            }
            reconnectionRunnable = null
        }
    }

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

        handler = Handler(Looper.getMainLooper())

        registerReceiver(scanReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        registerReceiver(locationReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
        registerGattServerReceiver()
        initGattServer()
    }

    interface ServicesStateListener {
        fun onBluetoothStateChanged(isOn: Boolean)
        fun onLocationStateChanged(isOn: Boolean)
    }

    private val bluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_ON -> {
                        initGattServer()
                        servicesStateListener?.onBluetoothStateChanged(true)
                    }
                    BluetoothAdapter.STATE_OFF -> {
                        disconnectAllGatts()
                        servicesStateListener?.onBluetoothStateChanged(false)
                    }
                }
            }
        }
    }

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
            if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                val state = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                servicesStateListener?.onLocationStateChanged(state)
            }
        }
    }

    fun isBluetoothOn() = BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isEnabled

    fun isLocationOn() : Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(locationManager)
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
        stopDiscovery()
        disconnectAllGatts()
        AdvertiserService.stopService(applicationContext)
        unregisterReceiver(scanReceiver)
        unregisterReceiver(gattServerBroadcastReceiver)
        unregisterReceiver(bluetoothReceiver)
        unregisterReceiver(locationReceiver)
        bluetoothGattServer?.close()

        super.onDestroy()
    }

    fun addListener(scanListener: ScanListener) {
        synchronized(scanListeners) {
            if (!scanListeners.contains(scanListener)) {
                scanListeners.add(scanListener)
            }
        }
    }

    fun removeListener(scanListener: ScanListener?) {
        synchronized(scanListeners) {
            scanListeners.remove(scanListener)
        }
    }

    fun startDiscovery(filters: List<ScanFilter>) {
        bluetoothAdapter?.let {
            if (useBLE) {
                bleScannerCallback = BleScanCallback(this)
                val settings = ScanSettings.Builder()
                        .setLegacy(false)
                        .setReportDelay(0)
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
                it.bluetoothLeScanner?.startScan(
                        filters, settings, bleScannerCallback
                ) ?: onDiscoveryFailed(ScanError.LeScannerUnavailable)
            } else {
                if (!it.startDiscovery()) onDiscoveryFailed(ScanError.BluetoothAdapterUnavailable)
            }
        } ?: onDiscoveryFailed(ScanError.BluetoothAdapterUnavailable)
    }

    fun onDiscoveryFailed(scanError: ScanError, errorCode: Int? = null) {
        val message = when (scanError) {
            ScanError.ScannerError -> getString(R.string.scan_failed_error_code, errorCode)
            ScanError.LeScannerUnavailable -> getString(R.string.scan_failed_le_scanner_unavailable)
            ScanError.BluetoothAdapterUnavailable -> getString(R.string.scan_failed_bluetooth_adapter_unavailable)
        }
        scanListeners.onDiscoveryFailed()
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    fun stopDiscovery() {
        bluetoothAdapter?.let {
            if (useBLE) {
                it.bluetoothLeScanner?.stopScan(bleScannerCallback as ScanCallback?)
            } else {
                if (it.isDiscovering) it.cancelDiscovery()
                else { /* added to satisfy lambda */ }
            }
        }
    }

    fun handleScanCallback(result: ScanResultCompat) {
        scanListeners.handleScanResult(result)
    }

    enum class ScanError {
        ScannerError,
        LeScannerUnavailable,
        BluetoothAdapterUnavailable,
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
            Timber.d("refreshDevice: Called")
            val localMethod: Method = gatt?.javaClass?.getMethod("refresh")!!
            val bool: Boolean = (localMethod.invoke(gatt, *arrayOfNulls(0)) as Boolean)
            Timber.d("refreshDevice: bool: $bool")
            return bool
        } catch (localException: Exception) {
            Timber.e("refreshDevice: An exception occurred while refreshing device")
        }
        return false
    }

    fun clearConnectedGatt() {
        connectedGatt?.let { disconnectGatt(it.device.address) }
    }

    fun registerGattCallback(requestRssiUpdates: Boolean, callback: TimeoutGattCallback?) {
        handler.removeCallbacks(rssiUpdateRunnable)
        if (requestRssiUpdates) {
            handler.post(rssiUpdateRunnable)
        }
        extraGattCallback = callback
    }

    fun isGattConnected(): Boolean {
        return connectedGatt != null
                && activeConnections.containsKey(connectedGatt?.device?.address!!)
                && bluetoothManager.getConnectionState(connectedGatt?.device, BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED
    }

    fun isGattConnected(deviceAddress: String?): Boolean {
        return deviceAddress?.let {
            activeConnections.containsKey(deviceAddress)
                && bluetoothManager.getConnectedDevices(BluetoothProfile.GATT).any { it.address == deviceAddress }
        } ?: false
    }

    fun getConnectedGatt(deviceAddress: String?): BluetoothGatt? {
        deviceAddress?.let {
            connectedGatt = getActiveConnection(it)?.connection?.gatt
        }
        return connectedGatt
    }

    fun getConnectedGatts() : List<BluetoothGatt> {
        return activeConnections.values.map { it.connection.gatt!! }
    }

    fun connectGatt(
            device: BluetoothDevice,
            requestRssiUpdates: Boolean,
            extraCallback: TimeoutGattCallback? = null,
            isConnectionRetry: Boolean = false
    ) {
        stopDiscovery()
        extraCallback?.let { extraGattCallback = it }

        if (!isConnectionRetry) handler.postDelayed(connectionTimeoutRunnable, CONNECTION_TIMEOUT)

        /* Invokes onConnectionStateChange() callback */
        connectedGatt =
                if (useBLE) device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                else device.connectGatt(this, false, gattCallback)
         connectedGatt?.let { addPendingConnection(GattConnection(it, requestRssiUpdates)) }
    }

    fun disconnectGatt(deviceAddress: String) {
        getActiveConnection(deviceAddress)?.let {
            /* Invokes onConnectionStateChange() callback */
            it.connection.gatt?.disconnect()
        }
    }

    fun disconnectAllGatts() {
        activeConnections.values.forEach { disconnectGatt(it.connection.address) }
    }

    private fun clearGattConnection(address: String) {
        getActiveConnection(address)?.let {
            addConnectionLog(DisconnectByButtonLog(address))
            it.connection.gatt?.close()
            removeActiveConnection(it.connection.gatt?.device?.address)
            if (connectedGatt?.device?.address == address) {
                connectedGatt = null
            }
        }
        if (activeConnections.isEmpty()) handler.removeCallbacks(rssiUpdateRunnable)
    }

    private fun addPendingConnection(connection: GattConnection) {
        pendingConnections.apply {
            synchronized(this) { put(connection.address, connection) }
        }
    }

    private fun removePendingConnection(address: String) {
        pendingConnections.apply {
            synchronized(this) { remove(address) }
        }
    }

    fun isAnyConnectionPending() = pendingConnections.isNotEmpty()

    fun updateConnectionInfo(info: BluetoothDeviceInfo) {
        activeConnections[info.address]?.bluetoothInfo = info
    }

    fun updateConnectionRssi(gatt: BluetoothGatt, rssi: Int) {
        activeConnections.values.apply {
            synchronized(this) {
                val connectedDevice = find { it.connection.gatt!!.device.address == gatt.device.address }
                connectedDevice?.bluetoothInfo?.rssi = rssi
            }
        }
    }

    private fun addActiveConnection(connection: GattConnection) {
        activeConnections.apply {
            synchronized(this) { put(connection.address, ConnectedDeviceInfo(connection)) }
        }
    }

    private fun removeActiveConnection(address: String?) {
        address?.let {
            activeConnections.apply {
                synchronized(this) { remove(it) }
            }
        }
    }

    private fun handleReconnection(gatt: BluetoothGatt) {
        pendingConnections[gatt.device.address]?.let {
            gatt.close()
            removePendingConnection(gatt.device.address)
            retryAttempts++

            if (retryAttempts < RECONNECTION_RETRIES) {
                reconnectionRunnable = ReconnectionRunnable(it)
                handler.postDelayed(reconnectionRunnable!!, RECONNECTION_DELAY)
            }
        }
    }

    fun getActiveConnection(address: String) = activeConnections[address]
    fun getActiveConnections() = activeConnections.values.toList()
    fun getNumberOfConnections() = activeConnections.size
    fun isAnyDeviceConnected() = activeConnections.isNotEmpty()

    fun addConnectionLog(log: Log) {
        synchronized(connectionLogs) {
            connectionLogs.add(log)
        }
    }

    fun getLogsForDevice(address: String) : List<Log> {
        return synchronized(connectionLogs) {
            connectionLogs.filter { it.deviceAddress == address }
        }
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
            Timber.d("onConnectionStateChange(): gatt device = ${gatt.device.address}, status = $status, newState = $newState")
            addConnectionLog(ConnectionStateChangeLog(gatt, status, newState))

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        if (activeConnections.isEmpty()) handler.post(rssiUpdateRunnable)

                        handler.removeCallbacks(connectionTimeoutRunnable)
                        pendingConnections[gatt.device.address]?.let { addActiveConnection(it) }
                        removePendingConnection(gatt.device.address)
                        retryAttempts = 0
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED ->
                    when (status) {
                        133 -> handleReconnection(gatt)
                        else -> clearGattConnection(gatt.device.address)
                    }
            }

            if (retryAttempts < RECONNECTION_RETRIES) {
                extraGattCallback?.onConnectionStateChange(gatt, status, newState)
            } else {
                extraGattCallback?.onMaxRetriesExceeded(gatt)
                retryAttempts = 0
                handler.removeCallbacks(connectionTimeoutRunnable)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Timber.d("onServicesDiscovered(): gatt device = ${gatt.device.address}, status = $status")
            addConnectionLog(ServicesDiscoveredLog(gatt, status))
            extraGattCallback?.onServicesDiscovered(gatt, status)
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            Timber.d("onCharacteristicRead(): gatt device = ${gatt.device.address}, uuid = ${
                characteristic.uuid}, value = ${characteristic.value?.contentToString()}")
            addConnectionLog(GattOperationWithDataLog("onCharacteristicRead", gatt, status, characteristic))
            extraGattCallback?.onCharacteristicRead(gatt, characteristic, status)
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Timber.d("onCharacteristicWrite(): gatt device = ${gatt.device.address}, uuid = ${
                characteristic.uuid}, status = $status, value = ${characteristic.value?.contentToString()}")
            addConnectionLog(GattOperationWithDataLog("onCharacteristicWrite", gatt, status, characteristic))
            extraGattCallback?.onCharacteristicWrite(gatt, characteristic, status)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            Timber.d("onCharacteristicChanged(): gatt device = ${gatt.device.address}, uuid = ${
                characteristic.uuid}, value = ${characteristic.value?.contentToString()}")
            addConnectionLog(GattOperationWithDataLog("onCharacteristicChanged", gatt, null, characteristic))
            extraGattCallback?.onCharacteristicChanged(gatt, characteristic)
        }

        override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            super.onDescriptorRead(gatt, descriptor, status)
            Timber.d("onDescriptorRead(): gatt device = ${gatt.device.address}, uuid = ${
                descriptor.uuid}, descriptor's characteristic = ${
                descriptor.characteristic.uuid}, value = ${descriptor.value?.contentToString()}")
            addConnectionLog(CommonLog("onDescriptorRead, device: ${gatt.device.address}, status: $status", gatt.device.address))
            extraGattCallback?.onDescriptorRead(gatt, descriptor, status)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            Timber.d("onDescriptorWrite(): gatt device = ${gatt.device.address}, uuid = ${
                descriptor.uuid}, descriptor's characteristic = ${
                descriptor.characteristic.uuid}, value = ${descriptor.value?.contentToString()}")
            addConnectionLog(CommonLog("onDescriptorWrite, device: ${gatt.device.address}, status: $status", gatt.device.address))
            extraGattCallback?.onDescriptorWrite(gatt, descriptor, status)
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int) {
            super.onReliableWriteCompleted(gatt, status)
            Timber.d("onReliableWriteCompleted(): gatt device = ${gatt.device.address}, status = $status")
            addConnectionLog(CommonLog("onReliableWriteCompleted, device: ${gatt.device.address}, status: $status", gatt.device.address))
            extraGattCallback?.onReliableWriteCompleted(gatt, status)
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            Timber.d("onReadRemoteRssi(): gatt device = ${gatt.device.address}, rssi = $rssi, status = $status")
            addConnectionLog(CommonLog("onReadRemoteRssi, device: ${gatt.device.address}, status: $status, rssi: $rssi", gatt.device.address))

            if (status == BluetoothGatt.GATT_SUCCESS) {
                updateConnectionRssi(gatt, rssi)
                extraGattCallback?.onReadRemoteRssi(gatt, rssi, status)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            Timber.d("onMtuChanged(): gatt device =${gatt.device.address}, mtu = $mtu")
            addConnectionLog(CommonLog("onMtuChanged, device: ${gatt.device.address}, status: $status, mtu: $mtu", gatt.device.address))
            extraGattCallback?.onMtuChanged(gatt, mtu, status)
        }

        @SuppressLint("BinaryOperationInTimber")
        override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status)
            Timber.d("onPhyUpdate(): gatt device = ${gatt?.device?.address}, txPhy = $txPhy, " +
                    "rxPhy = $rxPhy, status = $status")
            extraGattCallback?.onPhyUpdate(gatt, txPhy, rxPhy, status)
        }
    }

    fun unregisterGattServerCallback() {
        gattServerCallback = null
    }

    fun registerGattServerCallback(callback: BluetoothGattServerCallback) {
        this.gattServerCallback = callback
    }

    private val bluetoothGattServerCallback = object : BluetoothGattServerCallback() {
        @SuppressLint("BinaryOperationInTimber")
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            Timber.d("onServerConnectionStateChange(): device = ${device.address}," +
                    "status = $status, newState = $newState")

            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (gattServerCallback != null) {
                        gattServerCallback?.onConnectionStateChange(device, status, newState)
                    } else if (isNotificationEnabled) showDebugConnectionNotification(device)
                }
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
            Timber.i("onCharacteristicWriteRequest")
            characteristic?.value = value
            if (responseNeeded) {
                bluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }

        override fun onDescriptorWriteRequest(device: BluetoothDevice?, requestId: Int, descriptor: BluetoothGattDescriptor?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            super.onDescriptorWriteRequest(
                device,
                requestId,
                descriptor,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )
            Timber.i("onDescriptorWriteRequest")
            descriptor?.value = value
            if (
                descriptor != null && device != null && value != null
                && descriptor.uuid == UuidConsts.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR
            ) {
                val characteristicUuid = descriptor.characteristic.uuid

                when (value.asList()) {
                    Notifications.DISABLED.descriptorValue.asList() -> {
                        devicesToNotify[characteristicUuid]?.remove(device)
                        devicesToIndicate[characteristicUuid]?.remove(device)
                    }
                    Notifications.NOTIFY.descriptorValue.asList() -> {
                        devicesToNotify
                                .getOrPut(characteristicUuid) { mutableSetOf() }
                                .add(device)
                        devicesToIndicate[characteristicUuid]?.remove(device)
                    }
                    Notifications.INDICATE.descriptorValue.asList() -> {
                        devicesToIndicate
                                .getOrPut(characteristicUuid) { mutableSetOf() }
                                .add(device)
                        devicesToNotify[characteristicUuid]?.remove(device)
                    }
                }
            }
            if (responseNeeded) {
                bluetoothGattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    value
                )
            }
        }

        override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
            super.onNotificationSent(device, status)
            gattServerCallback?.onNotificationSent(device, status)
        }
    }

    fun getClientsToNotify(characteristicUuid: UUID): Collection<BluetoothDevice> {
        return devicesToNotify[characteristicUuid] ?: emptySet()
    }

    fun getClientsToIndicate(characteristicUuid: UUID) : Collection<BluetoothDevice> {
        return devicesToIndicate[characteristicUuid] ?: emptySet()
    }

    private fun getYesPendingIntent(device: BluetoothDevice): PendingIntent {
        val intent = Intent(ACTION_GATT_SERVER_DEBUG_CONNECTION)
        intent.putExtra(EXTRA_BLUETOOTH_DEVICE, device)
        return PendingIntent.getBroadcast(
            this,
            GATT_SERVER_DEBUG_CONNECTION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    private fun getNoPendingIntent(): PendingIntent {
        val intent = Intent(ACTION_GATT_SERVER_REMOVE_NOTIFICATION)
        return PendingIntent.getBroadcast(
            this,
            GATT_SERVER_REMOVE_NOTIFICATION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    private fun getYesAndOpenPendingIntent(device: BluetoothDevice): PendingIntent {
        val intent = Intent(this, PendingServerConnectionActivity::class.java).apply {
            putExtra(EXTRA_BLUETOOTH_DEVICE, device)
            flags = Intent.FLAG_ACTIVITY_NO_HISTORY
        }
        val backIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        return PendingIntent.getActivities(
            this,
            GATT_SERVER_OPEN_CONNECTION_REQUEST_CODE,
            arrayOf(backIntent, intent),
            PendingIntent.FLAG_ONE_SHOT
        )
    }

    private fun showDebugConnectionNotification(device: BluetoothDevice) {
        val deviceName = device.name ?: getString(R.string.not_advertising_shortcut)
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.efr_redesign_launcher)
                .setContentTitle(getString(R.string.notification_title_device_has_connected, deviceName))
                .setContentText(getString(R.string.notification_note_debug_connection))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(LongArray(0))
                .addAction(0, getString(R.string.button_yes), getYesPendingIntent(device))
                .addAction(0, getString(R.string.notification_button_yes_and_open), getYesAndOpenPendingIntent(device))
                .addAction(0, getString(R.string.button_no), getNoPendingIntent())
                .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.notification_channel_name)
        val descriptionText = getString(R.string.notification_channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }

        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private val gattServerBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_GATT_SERVER_DEBUG_CONNECTION -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(EXTRA_BLUETOOTH_DEVICE)
                    device?.let {
                        connectGatt(device, false, null)
                    }
                    closeGattServerNotification()
                }
                ACTION_GATT_SERVER_REMOVE_NOTIFICATION -> closeGattServerNotification()
            }
        }
    }

    fun closeGattServerNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private class ScanListeners : ArrayList<ScanListener>(), ScanListener {

        override fun handleScanResult(scanResult: ScanResultCompat) {
            forEach { it.handleScanResult(scanResult) }
        }

        override fun onDiscoveryFailed() {
            forEach { it.onDiscoveryFailed() }
        }
    }

}
