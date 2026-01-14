package com.siliconlabs.bledemo.features.demo.connected_lighting.activities

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import androidx.lifecycle.lifecycleScope
import com.siliconlabs.bledemo.bluetooth.ble.GattCharacteristic
import com.siliconlabs.bledemo.bluetooth.ble.GattService
import com.siliconlabs.bledemo.bluetooth.ble.TimeoutGattCallback
import com.siliconlabs.bledemo.features.demo.connected_lighting.presenters.ConnectedLightingPresenter
import com.siliconlabs.bledemo.features.demo.connected_lighting.presenters.ConnectedLightingPresenter.BluetoothController
import com.siliconlabs.bledemo.features.demo.connected_lighting.models.TriggerSource
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.base.activities.BaseDemoActivity
import com.siliconlabs.bledemo.utils.AppUtil
import com.siliconlabs.bledemo.features.demo.wifi_commissioning.activities.WifiCommissioningActivity
import com.siliconlabs.bledemo.home_screen.activities.MainActivity
import com.siliconlabs.bledemo.utils.BLEUtils
import com.siliconlabs.bledemo.utils.Notifications
import com.siliconlabs.bledemo.utils.UuidUtils
import kotlinx.coroutines.launch
import timber.log.Timber

@SuppressLint("MissingPermission")
class ConnectedLightingActivity : BaseDemoActivity(), BluetoothController {
    private var presenter: ConnectedLightingPresenter? = null
    private var gattService: GattService? = null
    lateinit var mToolbar: Toolbar
    private var initSourceAddress = false
    private var serviceHasBeenSet = false
    private var updateDelayed = false
    private var isInitializationComplete = false

    // Progress bar views
    private lateinit var progressBar: ProgressBar
    private lateinit var progressOverlay: View

    // --- Light characteristic initialization retry bookkeeping (keeps old behavior: 1 retry only for TheDMP) ---
    private var lightInitAttempts = 0
    private var maxLightInitAttempts = LIGHT_INIT_MAX_ATTEMPTS_OTHERS

    // Service rediscovery state (to handle transient 0 services after cache refresh)
    private var serviceDiscoveryAttempts = 0
    private val maxServiceDiscoveryAttempts = 5
    private val serviceRediscoveryDelayMs = 1000L
    private var rediscoveryInFlight = false

    private val gattCallback: TimeoutGattCallback = object : TimeoutGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Timber.tag(TAG).i("GATT disconnected (status=$status). Showing disconnect dialog and routing to MainActivity.")
                disconnectWithModal()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
            Timber.tag(TAG).d("Services discovered (status=$status). Total services: ${gatt.services?.size ?: 0}")
            val serviceCount = gatt.services?.size ?: 0
            if (status == BluetoothGatt.GATT_SUCCESS && serviceCount == 0) {
                // Transient empty set: retry a few times before failing
                scheduleServiceRediscovery(gatt, reason = "Empty service list")
                return
            }
            gattService = getGattService().also {
                Timber.tag(TAG).d("Selected lighting service: ${it?.name ?: "NONE"}")
            }
            if (gattService == null) {
                scheduleServiceRediscovery(gatt, reason = "Lighting service not found")
                return
            }
            // Configure max attempts depending on protocol (TheDMP allows 1 retry -> total 2 attempts for light char)
            maxLightInitAttempts = if (gattService == GattService.TheDMP) LIGHT_INIT_MAX_ATTEMPTS_DMP else LIGHT_INIT_MAX_ATTEMPTS_OTHERS

            val characteristic = getLightCharacteristic()
            if (characteristic != null) {
                attemptLightCharacteristicRead(gatt, characteristic)
            } else {
                scheduleServiceRediscovery(gatt, reason = "Light characteristic missing in service ${gattService?.name}")
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            println("ConnectedLightingActivity onCharacteristicRead: " + UuidUtils.getUuidText(characteristic.uuid) + " status: " + status)
            if (GattCharacteristic.Light.uuid == characteristic.uuid) {
                handleLightCharacteristicReadResult(gatt, characteristic, status)
                return // Light path handles chaining & progress
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicUpdate(characteristic)
            } else {
                // Enhanced error handling - log the failure but don't immediately disconnect for non-critical characteristics
                Timber.tag(TAG).w("Characteristic read failed: ${UuidUtils.getUuidText(characteristic.uuid)} status: $status")
                if (status == BluetoothGatt.GATT_READ_NOT_PERMITTED) {
                    Timber.tag(TAG).i("Read not permitted for ${UuidUtils.getUuidText(characteristic.uuid)}, continuing gracefully")
                }
                hideInitializationProgress() // maintain previous behavior of hiding on failure
            }

            // Enhanced chaining logic with better error tolerance
            if (GattCharacteristic.TriggerSource.uuid == characteristic.uuid) {
                if (!initSourceAddress && status == BluetoothGatt.GATT_SUCCESS) {
                    // Only proceed to SourceAddress if TriggerSource read was successful
                    initSourceAddress = true
                    val sourceAddressChar = characteristic.service.getCharacteristic(GattCharacteristic.SourceAddress.uuid)
                    val success = gatt.readCharacteristic(sourceAddressChar)
                    if (!success) {
                        Timber.tag(TAG).w("Failed to initiate SourceAddress read, setting up notifications directly")
                        setupNotificationsForLight(gatt)
                    }
                } else if (status != BluetoothGatt.GATT_SUCCESS) {
                    // TriggerSource read failed - skip SourceAddress and go directly to notifications
                    Timber.tag(TAG).w("TriggerSource read failed (status=$status), skipping SourceAddress and setting up notifications")
                    setupNotificationsForLight(gatt)
                }
            } else if (GattCharacteristic.SourceAddress.uuid == characteristic.uuid) {
                // Always set up notifications regardless of SourceAddress read success/failure
                setupNotificationsForLight(gatt)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            if (GattCharacteristic.Light.uuid == descriptor.characteristic.uuid) {
                val success: Boolean = BLEUtils.setNotificationForCharacteristic(gatt,
                    gattService,
                    GattCharacteristic.TriggerSource,
                    Notifications.INDICATE)
                if (!success) {
                    disconnectWithModal()
                }
            } else if (GattCharacteristic.TriggerSource.uuid == descriptor.characteristic.uuid) {
                val success: Boolean = BLEUtils.setNotificationForCharacteristic(gatt,
                    gattService,
                    GattCharacteristic.SourceAddress,
                    Notifications.INDICATE)
                if (!success) {
                    disconnectWithModal()
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (characteristic === getLightCharacteristic()) {
                Log.d("onCharacteristicWrite", "" + status)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    runOnUiThread {
                        val value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                        val isLightOn = value != 0
                        presenter?.onLightUpdated(isLightOn)
                    }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            updateDelayed = true
            handleCharacteristicUpdate(characteristic)
        }
    }

    // Extracted from inner callback for reuse
    private fun handleCharacteristicUpdate(characteristic: BluetoothGattCharacteristic) {
        val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid) ?: return
        when (gattCharacteristic) {
            GattCharacteristic.Light -> runOnUiThread {
                val value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                val isLightOn = value != 0
                presenter?.onLightUpdated(isLightOn)
            }
            GattCharacteristic.TriggerSource -> runOnUiThread {
                val value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                println("ConnectedLightingScreen ConnectedLightingActivity onCharacteristicUpdate: TriggerSource: $value")
                if (presenter != null) {
                    val triggerSource = TriggerSource.forValue(value)
                    presenter?.onSourceUpdated(triggerSource)
                    if (triggerSource != TriggerSource.BLUETOOTH && updateDelayed) {
                        updateDelayed = false
                        presenter?.lightValueDelayed
                    }
                }
            }
            GattCharacteristic.SourceAddress -> runOnUiThread {
                var sourceAddress = ""
                var i = 0
                while (i < SOURCE_ADDRESS_LENGTH) {
                    val value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, i)
                    sourceAddress += String.format(if (i < SOURCE_ADDRESS_LENGTH - 1) "%02x:" else "%02x", value)
                    i++
                }

                presenter?.onSourceAddressUpdated(sourceAddress)
            }
            else -> {}
        }
    }

    // --- Light initialization helpers ---
    private fun attemptLightCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        lightInitAttempts++
        val success = gatt.readCharacteristic(characteristic)
        if (!success) {
            Timber.tag(TAG).e("Failed to read Light characteristic (attempt $lightInitAttempts/$maxLightInitAttempts)")
            if (shouldRetryLightInitialization()) {
                scheduleLightRetry(gatt, characteristic)
            } else {
                failInitialization("Light characteristic read failed; disconnecting.")
            }
        }
    }

    private fun handleLightCharacteristicReadResult(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            handleCharacteristicUpdate(characteristic)
            hideInitializationProgress() // first success ends progress
            // Chain to TriggerSource characteristic as before
            val success = gatt.readCharacteristic(characteristic.service
                .getCharacteristic(GattCharacteristic.TriggerSource.uuid))
            if (!success) {
                disconnectWithModal()
            }
        } else {
            Timber.tag(TAG).w("Light characteristic read failed with status=$status (attempt $lightInitAttempts/$maxLightInitAttempts)")
            if (shouldRetryLightInitialization()) {
                scheduleLightRetry(gatt, characteristic)
            } else {
                hideInitializationProgress()
                if (gattService == GattService.TheDMP) {
                    // keep parity with previous implementation: after final failure continue (no immediate disconnect) for DMP
                    Timber.tag(TAG).e("Final Light read failure for TheDMP; continuing without disconnect.")
                } else {
                    failInitialization("Light characteristic read failed; disconnecting.")
                }
            }
        }
    }

    private fun shouldRetryLightInitialization(): Boolean =
        lightInitAttempts < maxLightInitAttempts

    private fun scheduleLightRetry(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        Timber.tag(TAG).i("Scheduling Light characteristic retry (attempt ${lightInitAttempts + 1}/$maxLightInitAttempts)")
        lifecycleScope.launch {
            // A short delay gives firmware time to expose char when waking (kept small to avoid UX change)
            kotlinx.coroutines.delay(500)
            if (!isFinishing) {
                attemptLightCharacteristicRead(gatt, characteristic)
            }
        }
    }

    private fun failInitialization(reason: String) {
        Timber.tag(TAG).e(reason)
        hideInitializationProgress()
        disconnectWithModal()
    }

    private fun scheduleServiceRediscovery(gatt: BluetoothGatt, reason: String) {
        if (isFinishing) return
        if (serviceDiscoveryAttempts >= maxServiceDiscoveryAttempts) {
            Timber.tag(TAG).e("Service rediscovery failed after $serviceDiscoveryAttempts attempts. Reason: $reason")
            failInitialization("Light characteristic not found after retries; disconnecting.")
            return
        }
        if (rediscoveryInFlight) {
            Timber.tag(TAG).d("Rediscovery already in flight; skipping (reason: $reason)")
            return
        }
        serviceDiscoveryAttempts++
        rediscoveryInFlight = true
        Timber.tag(TAG).i("Scheduling service rediscovery attempt #$serviceDiscoveryAttempts in ${serviceRediscoveryDelayMs}ms (reason: $reason)")
        lifecycleScope.launch {
            kotlinx.coroutines.delay(serviceRediscoveryDelayMs)
            rediscoveryInFlight = false
            if (!isFinishing) {
                try {
                    gatt.discoverServices()
                } catch (ex: Exception) {
                    Timber.tag(TAG).e(ex, "discoverServices() threw on retry #$serviceDiscoveryAttempts")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_light)
        mToolbar = findViewById(R.id.toolbar)
        prepareToolBar()

        // Initialize progress bar views
        progressBar = findViewById(R.id.progress_bar_initialization)
        progressOverlay = findViewById(R.id.view_progress_overlay)

        // Show progress bar immediately when activity is created
        showInitializationProgress()
    }

    private fun showInitializationProgress() {
        runOnUiThread {
            if (!isFinishing) {
                progressBar.visibility = View.VISIBLE
                progressOverlay.visibility = View.VISIBLE
                isInitializationComplete = false
            }
        }
    }

    private fun hideInitializationProgress() {
        runOnUiThread {
            if (!isFinishing && !isInitializationComplete) {
                progressBar.visibility = View.GONE
                progressOverlay.visibility = View.GONE
                isInitializationComplete = true
            }
        }
    }

    private fun prepareToolBar() {
        AppUtil.setEdgeToEdge(window, this)
        setSupportActionBar(mToolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.matter_back)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.title = this.getString(R.string.title_Connected_Lighting)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter?.cancelPeriodicReads()
        hideInitializationProgress()
    }

    override fun onResume() {
        super.onResume()
        if (serviceHasBeenSet && service == null || service != null && !service?.isGattConnected(connectionAddress)!!) {
            disconnectWithModal()
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.getItemId()) {
            android.R.id.home -> {
                onBackPressed()
                closeSmartLock()
                this.finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    private fun closeSmartLock() {
        val resultIntent = Intent()
        resultIntent.putExtra(WifiCommissioningActivity.CLOSE, true)
        setResult(RESULT_OK, resultIntent)
    }
    override fun onBluetoothServiceBound() {
        serviceHasBeenSet = true
        service?.registerGattCallback(true, gattCallback)
        service?.getActiveConnections()?.let { connDev ->
            if (connDev.isNotEmpty()) {
                val dev = connDev[0].connection.gatt
                if (gatt == null) {
                    gatt = dev
                    Timber.tag(TAG).d("Acquired existing GATT from service. Starting service discovery.")
                    // Defer refreshGattServices to avoid transient empty list; perform plain discover first
                    lifecycleScope.launch { gatt?.discoverServices() }
                    if (connectionAddress == null) connectionAddress = gatt?.device?.address
                    // Only refresh later if we fail several times
                } else {
                    Timber.tag(TAG).d("Reusing existing GATT. Ensuring services ready.")
                    ensureServicesReady(forceRediscover = gatt?.services.isNullOrEmpty())
                }
            } else {
                Timber.tag(TAG).e("No active connections found.")
                hideInitializationProgress()
            }
        } ?: run {
            Timber.tag(TAG).e("Service is null or has no active connections.")
            hideInitializationProgress()
        }
    }

    private fun ensureServicesReady(forceRediscover: Boolean = false) {
        val currentGatt = gatt ?: return
        if (forceRediscover || currentGatt.services.isNullOrEmpty()) {
            Timber.tag(TAG).d("Forcing service discovery (force=$forceRediscover, servicesSize=${currentGatt.services?.size ?: 0}).")
            lifecycleScope.launch { currentGatt.discoverServices() }
            return
        }
        if (gattService == null) {
            gattService = getGattService()
            Timber.tag(TAG).d("Resolved lighting GATT service without rediscovery: ${gattService?.name}")
        }
        if (gattService != null) {
            hideInitializationProgress()
        }
    }

    private fun disconnectWithModal() {
        runOnUiThread {
            if (!isFinishing && presenter != null) {
                presenter?.showDeviceDisconnectedDialog()
            } else if (!isFinishing && presenter == null) {
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            }
        }
    }

    override fun setLightValue(lightOn: Boolean): Boolean {
        val characteristic = getLightCharacteristic() ?: return false
        characteristic.setValue(if (lightOn) 1 else 0, GattCharacteristic.Light.format, 0)
        return gatt?.writeCharacteristic(characteristic)!!
    }

    override fun setPresenter(presenter: ConnectedLightingPresenter?) {
        this.presenter = presenter
    }

    override fun getLightValue(): Boolean {
        val characteristic = getLightCharacteristic()
        return characteristic != null && gatt?.readCharacteristic(characteristic)!!
    }

    private fun getLightCharacteristic(): BluetoothGattCharacteristic? {
        if (service == null) return null
        if (!service?.isGattConnected(connectionAddress)!!) return null
        gattService = getGattService()
        if (gattService != null) {
            presenter?.let { it.gattService = gattService }
            return gatt?.getService(gattService?.number)?.getCharacteristic(GattCharacteristic.Light.uuid)
        }
        return null
    }

    override fun leaveDemo() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()

    }

    private fun getGattService(): GattService? {
        return when {
            gatt?.getService(GattService.ProprietaryLightService.number) != null -> GattService.ProprietaryLightService
            gatt?.getService(GattService.ZigbeeLightService.number) != null -> GattService.ZigbeeLightService
            gatt?.getService(GattService.ConnectLightService.number) != null -> GattService.ConnectLightService
            gatt?.getService(GattService.ThreadLightService.number) != null -> GattService.ThreadLightService
            gatt?.getService(GattService.TheDMP.number) != null -> GattService.TheDMP
            gatt?.getService(GattService.TheAmazonSideWalk.number) != null -> GattService.TheAmazonSideWalk
            else -> null
        }
    }

    // New helper method to set up Light notifications with better error handling
    private fun setupNotificationsForLight(gatt: BluetoothGatt) {
        val success: Boolean = BLEUtils.setNotificationForCharacteristic(gatt,
            gattService,
            GattCharacteristic.Light,
            Notifications.INDICATE)
        if (!success) {
            Timber.tag(TAG).e("Failed to set Light notifications - this is critical, disconnecting")
            disconnectWithModal()
        } else {
            Timber.tag(TAG).d("Light notifications set up successfully")
        }
    }

    companion object {
        private const val SOURCE_ADDRESS_LENGTH = 8
        const val EXTRA_FROM_WIFI_COMMISSIONING = "extra_from_wifi_commissioning"
        private const val TAG = "ConnectedLightingActivity"
        private const val LIGHT_INIT_MAX_ATTEMPTS_DMP = 2 // initial + 1 retry
        private const val LIGHT_INIT_MAX_ATTEMPTS_OTHERS = 1 // only initial attempt
    }
}
