package com.siliconlabs.bledemo.features.demo.channel_sounding.activities

import android.Manifest.permission
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.base.activities.BaseDemoActivity
import com.siliconlabs.bledemo.bluetooth.ble.TimeoutGattCallback
import com.siliconlabs.bledemo.databinding.ActivityChannelSoundMainLayoutBinding
import com.siliconlabs.bledemo.features.demo.channel_sounding.fragments.ChannelSoundingConfigureFragment
import com.siliconlabs.bledemo.features.demo.channel_sounding.utils.ChannelSoundingConfigureParameters
import com.siliconlabs.bledemo.features.demo.channel_sounding.utils.ChannelSoundingConstant
import com.siliconlabs.bledemo.features.demo.channel_sounding.viewmodels.ChannelSoundingBleConnectViewModel
import com.siliconlabs.bledemo.features.demo.channel_sounding.viewmodels.ChannelSoundingDistanceMeasurementViewModel
import com.siliconlabs.bledemo.features.demo.channel_sounding.viewmodels.SharedViewModel
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.CustomProgressDialog
import com.siliconlabs.bledemo.utils.AppUtil
import com.siliconlabs.bledemo.utils.CustomToastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.DecimalFormat
import kotlin.compareTo

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
class ChannelSoundingActivity : BaseDemoActivity(),
    ChannelSoundingConfigureFragment.CallBackHandler {
    private lateinit var binding: ActivityChannelSoundMainLayoutBinding
    private val processor = GattProcessor()
    private lateinit var viewModelBle: ChannelSoundingBleConnectViewModel
    private lateinit var viewModelDM: ChannelSoundingDistanceMeasurementViewModel
    private lateinit var viewModelShared: SharedViewModel

    private var freqInfo: String = HIGH_FREQUENCY
    private var durationInfo: Int = 0
    private val DISTANCE_DECIMAL_FMT = DecimalFormat("0.00")

    private var targetedDevice: BluetoothDevice? = null
    private var pairingProgressDialog: CustomProgressDialog? = null
    private val rangeLow = 0.0..5.0
    private val rangeMid = 6.0..10.00
    private var menu: Menu? = null
    private var isManualStop = false
    private var measurementStartTime = 0L
    private var lastMeasurementTime = 0L
    private var isDeviceBonded = false



    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChannelSoundMainLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AppUtil.setEdgeToEdge(window, this)
        setSupportActionBar(binding.toolbar)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.matter_back)
            actionBar.setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.channel_sounding_screen_name)
        }
        binding.channelSoundingConfig.setOnClickListener {
            binding.configureContainer.visibility = View.VISIBLE
            binding.channelSoundingConfig.isEnabled = false
            binding.channelSoundingActivityContainer.visibility = View.GONE
            hideConfigureIcon()
            val fragment = ChannelSoundingConfigureFragment.newInstance()
            showFragment(fragment, fragment::class.java.simpleName)
        }
        initViews()
    }

    fun hideConfigureIcon() {
        menu?.findItem(R.id.menu_start_stop)?.isVisible = false
        binding.channelSoundingConfig.visibility = View.GONE
    }

    fun showConfigureIcon() {
        menu?.findItem(R.id.menu_start_stop)?.isVisible = true
        binding.channelSoundingConfig.visibility = View.VISIBLE
    }

    private fun registerReceiver() {
        val filter = android.content.IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        registerReceiver(bluetoothReceiver, filter)
    }

    private fun showFragment(
        fragment: Fragment, tag: String? = null,
    ) {
        val fManager = supportFragmentManager
        val fTransaction = fManager.beginTransaction()

        fTransaction.replace(binding.configureContainer.id, fragment, tag)
            .addToBackStack(null)
            .commit()

    }




    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_channel_sounding, menu)
        this.menu = menu
        updateMenuTitle()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (this.supportFragmentManager.backStackEntryCount > 0) {
                    this.supportFragmentManager.popBackStack()
                    binding.configureContainer.visibility = android.view.View.GONE
                    binding.channelSoundingActivityContainer.visibility = android.view.View.VISIBLE
                    binding.channelSoundingConfig.isEnabled = true
                    return true
                } else {
                    this.finish()
                }
                true
            }

            R.id.menu_start_stop -> {
                handleStartStopMenuClick()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("NewApi")
    private fun handleStartStopMenuClick() {
        val currentState = viewModelDM.getSessionState().value
        // Prevent action if already in transition state
        if (currentState == ChannelSoundingConstant.RangeSessionState.STARTING ||
            currentState == ChannelSoundingConstant.RangeSessionState.STOPPING) {
            Timber.tag(TAG).w("CS Ignoring start/stop click - already transitioning")
            return
        }

        if (currentState == ChannelSoundingConstant.RangeSessionState.STARTED ||
            currentState == ChannelSoundingConstant.RangeSessionState.STARTING) {
            // User clicked stop
            isManualStop = true
            freqInfo = ChannelSoundingConfigureFragment.getFrequency(this) ?: HIGH_FREQUENCY
            durationInfo = ChannelSoundingConfigureFragment.getDuration(this)?.toInt() ?: 0
            viewModelDM.toggleStartStop(TECHNOLOGY, freqInfo, durationInfo)
        } else {
            // User clicked start - no delay, start immediately
            isManualStop = false
            freqInfo = ChannelSoundingConfigureFragment.getFrequency(this) ?: HIGH_FREQUENCY
            durationInfo = ChannelSoundingConfigureFragment.getDuration(this)?.toInt() ?: 0
            viewModelDM.toggleStartStop(TECHNOLOGY, freqInfo, durationInfo)
        }
    }

    @SuppressLint("NewApi")
    private fun updateMenuTitle() {
        val currentState = viewModelDM.getSessionState().value
        val menuItem = menu?.findItem(R.id.menu_start_stop)
        when (currentState) {
            ChannelSoundingConstant.RangeSessionState.STARTED,
            ChannelSoundingConstant.RangeSessionState.STARTING -> {
                setMenuItemTitleWithWhiteColor(menuItem, getString(R.string.channel_sounding_menu_stop))
            }
            ChannelSoundingConstant.RangeSessionState.STOPPED,
            ChannelSoundingConstant.RangeSessionState.STOPPING -> {
                setMenuItemTitleWithWhiteColor(menuItem, getString(R.string.channel_sounding_menu_start))
            }
            else -> {
                setMenuItemTitleWithWhiteColor(menuItem, getString(R.string.channel_sounding_menu_start))
            }
        }
    }

    private fun setMenuItemTitleWithWhiteColor(menuItem: MenuItem?, title: String) {
        menuItem?.let {
            val spannableString = android.text.SpannableString(title)
            spannableString.setSpan(
                android.text.style.ForegroundColorSpan(
                    ContextCompat.getColor(this, R.color.silabs_white)
                ),
                0,
                spannableString.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            it.title = spannableString
        }
    }


    @RequiresPermission(permission.BLUETOOTH_CONNECT)
    override fun onBluetoothServiceBound() {

        service?.registerGattCallback(processor)
        service?.getActiveConnections()?.let { connDev ->
            if (connDev.isNotEmpty()) {
                val dev = connDev[0].connection.gatt

                deviceInfo(gatt)
                gatt = dev
                lifecycleScope.launch {
                    gatt?.discoverServices()
                }
                service?.refreshGattServices(service?.connectedGatt)

            } else {
                Timber.tag(TAG).e("No active connections found.")
            }
        } ?: Timber.tag(TAG).e("Service is null or has no active connections.")

    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun initViews() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.BAKLAVA) {
            requestAllPermissions()
        }
        registerReceiver()
        ChannelSoundingConfigureParameters.resetInstance(
            this,
            false
        )
        // Reset frequency and duration to default values
        ChannelSoundingConfigureFragment.saveFrequency(this, HIGH_FREQUENCY)
        ChannelSoundingConfigureFragment.saveDuration(this, 0)
        binding.configureContainer.visibility = android.view.View.GONE
        binding.channelSoundingActivityContainer.visibility = android.view.View.VISIBLE

        viewModelShared = ViewModelProvider(this).get(SharedViewModel::class.java)
        viewModelBle =
            ViewModelProvider(
                this,
                ChannelSoundingBleConnectViewModel.Factory(application)
            )[ChannelSoundingBleConnectViewModel::class.java]

        viewModelBle.getGattState().observe(this) { gattSate ->
            Timber.tag(TAG).d("CS Gatt state changed to $gattSate")
        }
        viewModelDM =
            ViewModelProvider(
                this,
                ChannelSoundingDistanceMeasurementViewModel.Factory(this, viewModelBle)
            )[ChannelSoundingDistanceMeasurementViewModel::class.java]
        try {
            viewModelBle.getTargetDevice().observe(this) { device ->
                Timber.tag(TAG).d("CS Target device changed to ${device?.address}")
                viewModelDM.setTargetDevice(device)
                targetedDevice = device

                freqInfo =
                    ChannelSoundingConfigureFragment.getFrequency(this@ChannelSoundingActivity)
                        ?: HIGH_FREQUENCY
                durationInfo = ChannelSoundingConfigureFragment.getDuration(this)?.toInt() ?: 0
                viewModelDM.toggleStartStop(TECHNOLOGY, freqInfo, durationInfo)
                // Only auto-start if not already starting/started and device is not null
                if (device != null) {
                    val currentState = viewModelDM.getSessionState().value
                    if (currentState == ChannelSoundingConstant.RangeSessionState.STOPPED ||
                        currentState == ChannelSoundingConstant.RangeSessionState.STOPPING) {
                        freqInfo =
                            ChannelSoundingConfigureFragment.getFrequency(this@ChannelSoundingActivity)
                                ?: HIGH_FREQUENCY
                        durationInfo = ChannelSoundingConfigureFragment.getDuration(this)?.toInt() ?: 0
                        isManualStop = false // Starting automatically after device connection
                        viewModelDM.toggleStartStop(TECHNOLOGY, freqInfo, durationInfo)
                    } else {
                        Timber.tag(TAG).d("CS Skipping auto-start - session already in progress: $currentState")
                    }
                }
            }
        } catch (e: SecurityException) {
            Timber.tag(TAG).e("CS Security Exception: ${e.message}")
        }

        viewModelShared.message.observe(this) { msg ->
            Timber.tag(TAG).d("CS Shared ViewModel message: $msg")
            //CustomToastManager.show(this@ChannelSoundingActivity, msg)
            freqInfo = ChannelSoundingConfigureFragment.getFrequency(this@ChannelSoundingActivity)
                ?: HIGH_FREQUENCY
            durationInfo = ChannelSoundingConfigureFragment.getDuration(this)?.toInt() ?: 0
            showConfigureIcon()
            isManualStop = true // Stop triggered by config change
            viewModelDM.channelSoundingStop()
            binding.configureContainer.visibility = android.view.View.GONE
            binding.channelSoundingActivityContainer.visibility = android.view.View.VISIBLE
            binding.channelSoundingConfig.isEnabled = true
            lifecycleScope.launch {
                delay(2000)
                // Only restart if session is stopped (not if user manually stopped in the meantime)
                val currentState = viewModelDM.getSessionState().value
                if (currentState == ChannelSoundingConstant.RangeSessionState.STOPPED) {
                    isManualStop = false // Reset before starting again
                    viewModelDM.toggleStartStop(TECHNOLOGY, freqInfo, durationInfo)
                } else {
                    Timber.tag(TAG).d("CS Skipping auto-restart after config change - session state: $currentState")
                }
            }

        }

        // Observe session state changes to update menu and show toast
        viewModelDM.getSessionState().observe(this) { state ->
            Timber.tag(TAG).d("CS Session state changed to: $state")
            updateMenuTitle()

            if (state == ChannelSoundingConstant.RangeSessionState.STARTED) {
                measurementStartTime = System.currentTimeMillis()
                lastMeasurementTime = measurementStartTime
                // Start monitoring for measurement stall
                startMeasurementStallMonitor()
            } else if (state == ChannelSoundingConstant.RangeSessionState.STOPPED) {
                measurementStartTime = 0L
                lastMeasurementTime = 0L

                // Show toast when measurements stop automatically (not manual stop) and device is bonded
                if (!isManualStop && isDeviceBonded) {
                    CustomToastManager.show(
                        this@ChannelSoundingActivity,
                        getString(R.string.channel_sounding_stopped_collecting),
                        3000
                    )
                }
                isManualStop = false
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModelDM.getDistanceResult().collect { distance ->
                    // Track measurement timing
                    val currentTime = System.currentTimeMillis()
                    if (measurementStartTime == 0L) {
                        measurementStartTime = currentTime
                    }
                    lastMeasurementTime = currentTime

                    val confidence = distance.confidence

                    if(confidence > 0){
                        // Update ripple color based on distance
                        updateRippleColor(distance.distanceMeter)

                        binding.distanceResult.setTextColor(
                            ContextCompat.getColor(
                                this@ChannelSoundingActivity,
                                R.color.silabs_white
                            )
                        )
                        binding.distanceResult.text =
                            DISTANCE_DECIMAL_FMT.format(
                                distance.distanceMeter
                            ) + " m"
                        binding.confidenceScore.visibility = View.GONE

                        Timber.tag(TAG).d("CS Distance result changed to $distance")
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun requestAllPermissions() {
        val requiredPermissions =
            arrayOf(
                permission.ACCESS_COARSE_LOCATION,
                permission.ACCESS_FINE_LOCATION,
                permission.BLUETOOTH_ADVERTISE,
                permission.BLUETOOTH_CONNECT,
                permission.BLUETOOTH_SCAN,
                permission.UWB_RANGING,
                permission.RANGING
            )
        val permissionsToRequest: MutableList<String?> = ArrayList()

        for (permission in requiredPermissions) {
            if (ActivityCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(permission)
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                this@ChannelSoundingActivity,
                permissionsToRequest.toTypedArray<String?>(),
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            var allGranted = true
            for (r in grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    CustomToastManager.show(
                        this@ChannelSoundingActivity,
                        "All permissions are required"
                    )
                    allGranted = false
                    break
                }
            }
            if (!allGranted) {
                CustomToastManager.show(this@ChannelSoundingActivity, "Permissions not granted")
                finish()
            }
        }
    }

    override fun onBackHandler() {
        binding.configureContainer.visibility = android.view.View.GONE
        binding.channelSoundingActivityContainer.visibility = android.view.View.VISIBLE
        binding.channelSoundingConfig.isEnabled = true
    }

    @RequiresPermission(permission.BLUETOOTH_CONNECT)
    private fun deviceInfo(gatt: BluetoothGatt?) {
        val device: BluetoothDevice = gatt?.device!!
        Timber.tag(TAG).d("CS Device ${device.address} is connected")
        gatt.setPreferredPhy(
            BluetoothDevice.PHY_LE_1M_MASK,
            BluetoothDevice.PHY_LE_1M_MASK,
            BluetoothDevice.PHY_OPTION_NO_PREFERRED
        )
        gatt.requestMtu(GATT_MTU_SIZE)
        viewModelBle.connectedDevices.add(device)
        viewModelBle.connectedGatts.add(gatt)
        viewModelBle.setGattState(
            ChannelSoundingConstant.GattState.CONNECTED
        )
        viewModelBle.setTargetDevice(device)

    }


    @RequiresPermission(permission.BLUETOOTH_CONNECT)
    override fun onDestroy() {
        super.onDestroy()
        // Dismiss progress dialog if still showing
        if (pairingProgressDialog != null && pairingProgressDialog!!.isShowing) {
            pairingProgressDialog!!.dismiss()
            pairingProgressDialog = null
        }
        unregisterReceiver(bluetoothReceiver)
        if (targetedDevice != null) {
            removeBond(targetedDevice!!)
        }
        service?.unregisterGattCallback()
        gatt?.close()
    }

    private val bluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        override fun onReceive(context: Context, intent: Intent?) {
            Timber.tag("BroadcastReceiver").d("CS BluetoothReceiver action: ${intent?.action}")
            if (intent == null || intent.action == null) return
            val action = intent.action
            when (action) {

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {

                    val device: BluetoothDevice? = intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class.java
                    )
                    device?.let {
                        try {
                            Timber.tag(TAG)
                                .d("CS Bluetooth Device Name:${it.name} - Device Address: ${it.address}")
                        } catch (exe: SecurityException) {
                            Timber.tag(TAG).e("CS Security Exception: ${exe.message}")
                        }
                    }

                    val bondState = intent.getIntExtra(
                        BluetoothDevice.EXTRA_BOND_STATE,
                        BluetoothDevice.BOND_NONE
                    )
                    val prevBondState = intent.getIntExtra(
                        BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                        BluetoothDevice.BOND_NONE
                    )
                    when (bondState) {
                        BluetoothDevice.BOND_BONDING -> {
                            Timber.tag(TAG).d("CS Bonding in progress...")
                            runOnUiThread {
                                // Show progress dialog when pairing starts
                                if (pairingProgressDialog == null || !pairingProgressDialog!!.isShowing) {
                                    pairingProgressDialog =
                                        CustomProgressDialog(this@ChannelSoundingActivity)
                                    pairingProgressDialog!!.window?.setBackgroundDrawable(
                                        ColorDrawable(Color.TRANSPARENT)
                                    )
                                    pairingProgressDialog!!.setMessage(getString(R.string.channel_sounding_waiting_for_pairing))
                                    pairingProgressDialog!!.setCanceledOnTouchOutside(false)
                                    pairingProgressDialog!!.show()
                                }
                            }
                        }

                        BluetoothDevice.BOND_BONDED -> {
                            Timber.tag(TAG).d("CS Bonded successfully")
                            isDeviceBonded = true
                            runOnUiThread {
                                // Dismiss progress dialog when pairing is successful
                                if (pairingProgressDialog != null && pairingProgressDialog!!.isShowing) {
                                    pairingProgressDialog!!.dismiss()
                                    pairingProgressDialog = null
                                }
                            }

                            CustomToastManager.showSuccess(
                                this@ChannelSoundingActivity,
                                "CS Bonded successfully"
                            )

                            // Only auto-start if session is stopped
                            val currentState = viewModelDM.getSessionState().value
                            if (currentState == ChannelSoundingConstant.RangeSessionState.STOPPED ||
                                currentState == ChannelSoundingConstant.RangeSessionState.STOPPING) {
                                isManualStop = false // Starting automatically after bonding
                                freqInfo = ChannelSoundingConfigureFragment.getFrequency(this@ChannelSoundingActivity) ?: HIGH_FREQUENCY
                                durationInfo = ChannelSoundingConfigureFragment.getDuration(this@ChannelSoundingActivity)?.toInt() ?: 0
                                viewModelDM.toggleStartStop(TECHNOLOGY, freqInfo, durationInfo)
                                lifecycleScope.launch {
                                    withContext(Dispatchers.Main) {
                                        binding.rippleAnimation.startRippleAnimation()
                                    }
                                }
                            } else {
                                Timber.tag(TAG).d("CS Skipping auto-start after bonding - session already in progress: $currentState")
                            }
                        }

                        BluetoothDevice.BOND_NONE -> {
                            Timber.tag(TAG).d("CS Bonding failed or broken")
                            isDeviceBonded = false
                            runOnUiThread {
                                // Dismiss progress dialog if bonding fails
                                if (pairingProgressDialog != null && pairingProgressDialog!!.isShowing) {
                                    pairingProgressDialog!!.dismiss()
                                    pairingProgressDialog = null
                                }

                                CustomToastManager.showError(
                                    this@ChannelSoundingActivity,
                                    "CS Bonding failed or broken"
                                )
                            }
                            if (targetedDevice != null) {
                                removeBond(targetedDevice!!)
                            }
                        }
                    }
                    Timber.tag("BroadcastReceiver")
                        .d("CS Bond state changed for device ${device?.address} from $prevBondState to $bondState")
                }
            }

        }
    }

    private fun showReconnectionMessage() {
        showMessages(R.string.connection_failed_reconnecting)
    }

    private fun showMessages(strResId: Int) {
        runOnUiThread {
            CustomToastManager.show(this@ChannelSoundingActivity, getString(strResId), 5000)
        }
    }

    private fun disconnectMessage() {
        showMessages(R.string.connection_failed)
    }

    private inner class GattProcessor : TimeoutGattCallback() {
        @RequiresPermission(permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Timber.tag(TAG).d("onConnectionStateChange status:$status, newState:$newState")
            val device = gatt.device
            Timber.tag(TAG).d("CS Device ${device.address} changed state to $newState")
            if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                isDeviceBonded = false
                viewModelBle.setGattState(
                    ChannelSoundingConstant.GattState.DISCONNECTED
                )
                viewModelBle.connectedDevices.remove(device)
                viewModelBle.connectedGatts.remove(gatt)
                when (status) {
                    133 -> {
                        showReconnectionMessage()
                    }

                    else -> {
                        disconnectMessage()
                        removeBond(targetedDevice!!)
                        //close
                        finish()
                    }
                }

                gatt.close()
            }

        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Timber.tag(TAG).d("CS MTU changed to: $mtu")
            } else {
                Timber.tag(TAG).e("CS MTU change failed: $status")
            }
        }

        @RequiresPermission(permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (service: BluetoothGattService in gatt.services) {
                    if (service.uuid == ChannelSoundingConstant.OOB_SERVICE) {
                        Timber.tag(TAG).e("CS Listening for PSM characteristics change")
                        val characteristic: BluetoothGattCharacteristic? =
                            service.getCharacteristic(ChannelSoundingConstant.OOB_PSM_CHARACTERISTICS)
                        if (characteristic == null) {
                            Timber.tag(TAG).e("CS Failed to get PSM characteristic")
                            return
                        }
                        gatt.setCharacteristicNotification(characteristic, true)
                    }
                }
            } else {
                Timber.tag(TAG).e("CS Service discovery failed: $status")
            }
        }


    }

    private fun removeBond(device: BluetoothDevice): Boolean {
        return try {
            return device::class.java.getMethod(REMOVE_BONDING).invoke(device) as Boolean
        } catch (e: Exception) {
            Timber.tag(TAG).e("CS removeBond exception: ${e.message}")
            false
        }
    }

    /**
     * Gets the confidence score as a readable string based on the confidence value
     * @param confidence The confidence value (0 = LOW, 1 = MEDIUM, 2 = HIGH)
     * @return String representation of the confidence level
     */
    private fun getConfidenceScore(confidence: Int): String {
        return when (confidence) {
            0 -> CONFIDENCE_LOW
            1 -> CONFIDENCE_MEDIUM
            2 -> CONFIDENCE_HIGH
            else -> {
                Timber.tag(TAG).w("Unknown confidence value: $confidence")
                CONFIDENCE_LOW // Default to LOW for unknown values
            }
        }
    }

    private fun updateRippleColor(distance: Double) {
        val color = when (distance) {
            in rangeLow -> ContextCompat.getColor(this, R.color.cs_silabs_blue_high)
            in rangeMid -> ContextCompat.getColor(this, R.color.cs_silabs_blue_mid)
            else -> ContextCompat.getColor(this, R.color.cs_silabs_blue_low)
        }

        binding.rippleAnimation.setRippleColor(color)
    }

    private fun startMeasurementStallMonitor() {
        lifecycleScope.launch {
            while (viewModelDM.getSessionState().value == ChannelSoundingConstant.RangeSessionState.STARTED) {
                delay(5000) // Check every 5 seconds

                val currentState = viewModelDM.getSessionState().value
                if (currentState == ChannelSoundingConstant.RangeSessionState.STARTED) {
                    val currentTime = System.currentTimeMillis()
                    val timeSinceLastMeasurement = currentTime - lastMeasurementTime

                    Timber.tag(TAG).d("CS Stall monitor: time since last measurement = ${timeSinceLastMeasurement}ms")

                    // If no measurements received for 5 seconds, stop and show error
                    if (timeSinceLastMeasurement > 5000 && measurementStartTime > 0L) {
                        Timber.tag(TAG).e("CS Measurement stall detected - no data for 5+ seconds")
                        isManualStop = false
                        viewModelDM.channelSoundingStop()
                        CustomToastManager.show(
                            this@ChannelSoundingActivity,
                            "Failed collecting measurements. Click Start to try again.",
                            5000
                        )
                        break
                    }
                }
            }
        }
    }

    companion object {
        private val TAG = ChannelSoundingActivity::class.java.simpleName
        private const val GATT_MTU_SIZE = 512
        private const val TECHNOLOGY = "BLE_CS"
        private const val REQUEST_CODE_PERMISSIONS = 100
        const val MEDIUM_FREQUENCY = "MEDIUM"
        const val HIGH_FREQUENCY = "HIGH"
        const val LOW_FREQUENCY = "LOW"
        const val CONFIDENCE_HIGH = "HIGH"
        const val CONFIDENCE_MEDIUM = "MEDIUM"
        const val CONFIDENCE_LOW = "LOW"
        private const val REMOVE_BONDING = "removeBond"
    }


}