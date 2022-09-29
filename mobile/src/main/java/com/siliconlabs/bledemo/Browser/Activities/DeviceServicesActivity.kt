/*
 * Bluegiga’s Bluetooth Smart Android SW for Bluegiga BLE modules
 * Contact: support@bluegiga.com.
 *
 * This is free software distributed under the terms of the MIT license reproduced below.
 *
 * Copyright (c) 2013, Bluegiga Technologies
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files ("Software")
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF
 * ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A  PARTICULAR PURPOSE.
 */
package com.siliconlabs.bledemo.Browser.Activities

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.provider.OpenableColumns
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.appindexing.Action
import com.google.android.gms.appindexing.AppIndex
import com.google.android.gms.appindexing.Thing
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.siliconlabs.bledemo.Base.BaseActivity
import com.siliconlabs.bledemo.Bluetooth.BLE.BluetoothDeviceInfo
import com.siliconlabs.bledemo.Bluetooth.BLE.ErrorCodes
import com.siliconlabs.bledemo.Bluetooth.BLE.TimeoutGattCallback
import com.siliconlabs.bledemo.Bluetooth.ConnectedGatts
import com.siliconlabs.bledemo.Bluetooth.Services.BluetoothService
import com.siliconlabs.bledemo.Browser.Adapters.ConnectionsAdapter
import com.siliconlabs.bledemo.Browser.Adapters.LogAdapter
import com.siliconlabs.bledemo.Browser.Dialogs.ErrorDialog
import com.siliconlabs.bledemo.Browser.Dialogs.ErrorDialog.OtaErrorCallback
import com.siliconlabs.bledemo.Browser.Dialogs.UnbondDeviceDialog
import com.siliconlabs.bledemo.Browser.Fragment.*
import com.siliconlabs.bledemo.Browser.MessageQueue
import com.siliconlabs.bledemo.Browser.Models.Logs.CommonLog
import com.siliconlabs.bledemo.Browser.Models.Logs.GattOperationWithDataLog
import com.siliconlabs.bledemo.Browser.Models.Logs.ServicesDiscoveredLog
import com.siliconlabs.bledemo.Browser.Models.Logs.TimeoutLog
import com.siliconlabs.bledemo.Browser.Models.OtaFileType
import com.siliconlabs.bledemo.Browser.Models.ToolbarName
import com.siliconlabs.bledemo.Browser.ServicesConnectionsCallback
import com.siliconlabs.bledemo.Browser.ToolbarCallback
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.utils.*
import kotlinx.android.synthetic.main.actionbar.*
import kotlinx.android.synthetic.main.activity_device_services.*
import kotlinx.android.synthetic.main.toolbar_device_services.*
import timber.log.Timber
import java.io.*
import java.lang.reflect.Method
import java.util.*

@SuppressWarnings("LogNotTimber")
class DeviceServicesActivity : BaseActivity(), ServicesConnectionsCallback {

    private lateinit var handler: Handler
    private lateinit var connectionsAdapter: ConnectionsAdapter

    private lateinit var connectionsFragment: ConnectionsFragment
    private lateinit var loggerFragment: LoggerFragment

    private var reliable = true
    private var boolFullOTA = false
    private var boolOTAbegin = false
    private var connected = false
    private var boolOTAdata = false
    var UICreated = false
    private var discoverTimeout = true
    private var ota_mode = false
    private var boolrequest_mtu = false
    private var ota_process = false
    private var boolrefresh_services = false
    private var disconnect_gatt = false
    private var disconnectionTimeout = false
    private var homekit = false
    private var doubleStepUpload = false
    private var otaMode = false
    private var btToolbarOpened = false
    private var serviceHasBeenSet = false

    private var dialogLicense: Dialog? = null
    private var newPriority: Dialog? = null
    private var newMTU: Dialog? = null
    private var errorDialog: ErrorDialog? = null

    private var MTU = 247
    private var mtuDivisible = 0
    private var otatime: Long = 0
    private var pack = 0
    private var otafile: ByteArray? = null
    private val delayToConnect: Long = 0
    private var onScanCallback = 0

    // OTA progress
    private var otaProgress: Dialog? = null
    private var progressBar: ProgressBar? = null
    private var chrono: Chronometer? = null
    private var dataRate: TextView? = null
    private var datasize: TextView? = null
    private var filename: TextView? = null
    private var steps: TextView? = null
    private var uploadimage: ProgressBar? = null
    private var OTAStart: Button? = null

    // OTA setup
    private var otaSetup: Dialog? = null
    private var reliabilityRB: RadioButton? = null
    private var speedRB: RadioButton? = null
    private var partialOTA: Button? = null
    private var fullOTA: Button? = null
    private var OTA_OK: Button? = null
    private var requestMTU: SeekBar? = null
    private var delaySeekBar: SeekBar? = null
    private var delayNoResponse = 1
    private var sizename: TextView? = null
    private var mtuname: TextView? = null
    private var reliableWrite: CheckBox? = null
    private var delayText: TextView? = null
    private var appFileButton: Button? = null
    private var appLoaderFileButton: Button? = null
    private var currentOtaFileType: OtaFileType? = null
    private var priority = 2
    private var requestMTUValue = 0

    // OTA file paths
    private var appPath = ""
    private var stackPath = ""

    private var loadingdialog: Dialog? = null
    private var loadingLog: TextView? = null
    private var loadingHeader: TextView? = null
    private var loadingimage: ProgressBar? = null

    private val DFU_OTA_UPLOAD = Runnable { dfuMode("OTAUPLOAD") }
    private val WRITE_OTA_CONTROL_ZERO = Runnable { writeOtaControl(0x00.toByte()) }

    private var bluetoothBinding: BluetoothService.Binding? = null
    var bluetoothService: BluetoothService? = null
        private set

    private var kit_descriptor: BluetoothGattDescriptor? = null
    private var btToolbarOpenedName: ToolbarName? = null
    private var deviceAddress: String? = null
    private var bluetoothDevice: BluetoothDevice? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var reScanCallback: ScanCallback? = null
    var bluetoothGatt: BluetoothGatt? = null

    private var retryAttempts = 0

    private val remoteServicesFragment = RemoteServicesFragment()
    private val localServicesFragment = LocalServicesFragment()
    private var activeFragment: Fragment = remoteServicesFragment

    private var bondMenuItem: MenuItem? = null
    private val bondStateChangeListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            with(intent) {
                if (action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                    val state = getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                    val device = getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    when (state) {
                        BluetoothDevice.BOND_NONE -> {
                            runOnUiThread {
                                if (bluetoothGatt?.device == device) {
                                    tv_bond_state.text = getString(R.string.not_bonded)
                                    bondMenuItem?.title = getString(R.string.create_bond)
                                }
                            }
                            Timber.d("Bond state changed: NONE")
                        }
                        BluetoothDevice.BOND_BONDING -> {
                            runOnUiThread {
                                if (bluetoothGatt?.device == device) {
                                    tv_bond_state.text = getString(R.string.bonding)
                                }
                            }
                            Timber.d("Bond state changed: BONDING")
                        }
                        BluetoothDevice.BOND_BONDED -> {
                            runOnUiThread {
                                if (bluetoothGatt?.device == device) {
                                    tv_bond_state.text = getString(R.string.bonded)
                                    bondMenuItem?.title = getString(R.string.delete_bond)
                                }
                            }
                            Timber.d("Bond state changed: BONDED")
                        }
                    }
                }
            }
        }
    }

    private val bluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_OFF -> finish()
                }
            }
        }
    }

    private val gattCallback: TimeoutGattCallback = object : TimeoutGattCallback() {
        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            Constants.LOGS.add(CommonLog("onReadRemoteRssi, " + "device: " + gatt.device.address + ", status: " + status + ", rssi: " + rssi, gatt.device.address))

            if (!otaMode) {
                super.onReadRemoteRssi(gatt, rssi, status)
                runOnUiThread {
                    Timber.d("onReadRemoteRssi: RSSI = %d", rssi)
                    tv_rssi.text = resources.getString(R.string.n_dBm, rssi)
                }
            }
        }

        override fun onTimeout() {
            Constants.LOGS.add(TimeoutLog())
            super.onTimeout()
            Timber.d("onTimeout")
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Constants.LOGS.add(CommonLog("onMtuChanged, " + "device: " + gatt.device.address + ", status: " + status + ", mtu: " + mtu, gatt.device.address))

            Timber.d("onMtuChanged: MTU = %d, status = %d", mtu, status)
            if (status == 0) { //NO ERRORS
                MTU = mtu
                bluetoothGatt?.requestConnectionPriority(priority)
                if (boolrequest_mtu) { //Request MTU From btn_rounded_red menu
                    mtuOnButtonMenu()
                } else if (ota_process && !boolrequest_mtu) {
                    if (ota_mode && newMTU?.isShowing!!) { //Reopen OTA Setup
                        reopenOTASetup()
                    }
                    if (ota_mode) { //Reset OTA Progress
                        resetOTAProgress()
                    }
                }
            } else { //ERROR HANDLING
                Timber.d("onMtuChanged: error = %d", status)
                handler.post { runOnUiThread { showMessage("ERROR REQUESTING MTU: $status") } }
                handler.postDelayed({ disconnectGatt(bluetoothGatt) }, 2000)
            }
        }

        //CALLBACK ON CONNECTION STATUS CHANGES
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_DISCONNECTED && status != 0 && !otaMode) {
                val deviceName = if (TextUtils.isEmpty(gatt.device.name)) getString(R.string.not_advertising_shortcut) else gatt.device.name
                MessageQueue.add(ErrorCodes.getDeviceDisconnectedMessage(deviceName, status), Toast.LENGTH_LONG)
            }

            updateCountOfConnectedDevices()
            if (bluetoothGatt != null) {
                if (bluetoothGatt?.device?.address != gatt.device.address) {
                    return
                }
            }
            super.onConnectionStateChange(gatt, status, newState)
            Timber.d("onConnectionStateChange: status = %d, newState = %d", status, newState)
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    connected = true
                    Timber.d("onConnectionStateChange: CONNECTED")
                    runOnUiThread {
                        if (!loadingdialog?.isShowing!!) {
                            showMessage("DEVICE CONNECTED")
                        }
                    }
                    if (ota_process) { //After OTA process started
                        Timber.d("Address = %s", gatt.device.address)
                        Timber.d("Name = %s", gatt.device.name)
                        if (gatt.services.isEmpty()) {
                            handler.postDelayed({
                                bluetoothGatt = null //It's going to be equal gatt in Discover Services Callback...
                                Timber.d("onConnected; Start Services Discovery: %s", gatt.discoverServices())
                            }, 250)
                            discoverTimeout = true
                            val timeout = Runnable {
                                handler.postDelayed({
                                    if (discoverTimeout) {
                                        disconnectGatt(gatt)
                                        runOnUiThread { showMessage("DISCOVER SERVICES TIMEOUT") }
                                    }
                                }, 25000)
                            }
                            Thread(timeout).start()
                        }
                    }
                }
                BluetoothGatt.STATE_DISCONNECTED -> {
                    if (status == 133 && otaMode && retryAttempts < RECONNECTION_RETRIES) {
                        retryAttempts++
                        Timber.d("onConnectionStateChange: [DeviceServices]: Reconnect due to 0x85 (133) error")
                        reconnect(1000)
                        return
                    }
                    connected = false
                    discoverTimeout = false
                    disconnectionTimeout = false
                    if ((status != 0) && otaMode && (errorDialog == null)) {
                        runOnUiThread {
                            errorDialog = ErrorDialog(status, object : OtaErrorCallback {
                                override fun onDismiss() {
                                    exit(bluetoothGatt)
                                }
                            })
                            errorDialog?.show(supportFragmentManager, "ota_error_dialog")
                        }
                    } else {
                        if (disconnect_gatt) {
                            exit(gatt)
                        }
                        if (ota_process || boolOTAbegin || boolFullOTA) {
                            runOnUiThread {
                                if (loadingdialog?.isShowing!!) {
                                    loadingLog?.text = getString(R.string.ota_rebooting_text)
                                    handler.postDelayed(
                                        { runOnUiThread { loadingLog?.text = getString(R.string.ota_loading_text) } }, 1500)
                                }
                            }
                        }
                        if (otaSetup != null) if (otaSetup?.isShowing!!) {
                            exit(gatt)
                        }
                        if (gatt.services.isEmpty()) {
                            exit(gatt)
                        }
                        if (!boolFullOTA && !boolOTAbegin && !ota_process) {
                            exit(gatt)
                        }
                    }
                }
                BluetoothGatt.STATE_CONNECTING -> Timber.d("onConnectionStateChange: Connecting...")
            }
        }

        //CALLBACK ON CHARACTERISTIC READ
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            remoteServicesFragment.updateCurrentCharacteristicView(characteristic.uuid)
            Constants.LOGS.add(GattOperationWithDataLog("onCharacteristicRead", gatt, status, characteristic))
            Timber.i("onCharacteristicRead: value = %s, status = %s",
                    Converters.bytesToHexWhitespaceDelimited(characteristic.value), status)

            val otaControlCharacteristic = bluetoothGatt
                ?.getService(UuidConsts.OTA_SERVICE)
                ?.getCharacteristic(UuidConsts.OTA_CONTROL)
            if (characteristic === otaControlCharacteristic) {
                val value = characteristic.value
                if (value[2] == 0x05.toByte()) {
                    Timber.d("homekit_descriptor: Insecure Connection")
                    runOnUiThread { showMessage("Error: Not a Homekit Secure Connection") }
                } else if (value[2] == 0x04.toByte()) {
                    Timber.d("homekit_descriptor: Wrong Address")
                } else if (value[2] == 0x00.toByte()) {
                    Timber.d("homekit_descriptor: Entering in DFU_Mode...")
                    if (ota_mode && ota_process) {
                        Timber.d("OTAUPLOAD: Sent")
                        runOnUiThread(checkbeginrunnable)
                        handler.removeCallbacks(DFU_OTA_UPLOAD)
                        handler.postDelayed(DFU_OTA_UPLOAD, 500)
                    } else if (!ota_mode && ota_process) {
                        runOnUiThread {
                            loadingLog?.text = getString(R.string.ota_resetting_text)
                            showLoading()
                            animaloading()
                            Constants.ota_button?.isVisible = true
                        }
                        handler.postDelayed({ reconnect(4000) }, 200)
                    }
                }
            }
        }

        //CALLBACK ON CHARACTERISTIC WRITE (PROPERTY: WHITE)
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            remoteServicesFragment.updateCurrentCharacteristicView(characteristic.uuid, status)
            Constants.LOGS.add(GattOperationWithDataLog("onCharacteristicWrite", gatt, status, characteristic))

            if (characteristic.value.size < 10) {
                Timber.d("OnCharacteristicWrite: Char = %s, Value = %s, Status = %s",
                        characteristic.uuid.toString(), Converters.bytesToHexWhitespaceDelimited(characteristic.value), status)
            }
            if (status != 0) { // Error Handling
                Timber.d("onCharWrite: status = %s", Integer.toHexString(status))
                if (errorDialog == null) {
                    runOnUiThread {
                        errorDialog = ErrorDialog(status, object : OtaErrorCallback {
                            override fun onDismiss() {
                                exit(bluetoothGatt)
                            }
                        })
                        errorDialog?.show(supportFragmentManager, "ota_error_dialog")
                    }
                }
            } else {
                if ((characteristic.uuid == UuidConsts.OTA_CONTROL)) { //OTA Control Callback Handling
                    if (characteristic.value.size == 1) {
                        if (characteristic.value[0] == 0x00.toByte()) {
                            Timber.d("Callback: Control %s, status = %d",
                                    Converters.bytesToHexWhitespaceDelimited(characteristic.value), status)
                            if (ota_mode && ota_process) {
                                Timber.d("OTAUPLOAD: Sent")
                                runOnUiThread(checkbeginrunnable)
                                handler.removeCallbacks(DFU_OTA_UPLOAD)
                                handler.postDelayed(DFU_OTA_UPLOAD, 500)
                            } else if (!ota_mode && ota_process) {
                                runOnUiThread {
                                    loadingLog?.text = getString(R.string.ota_resetting_text)
                                    showLoading()
                                    animaloading()
                                    Constants.ota_button?.isVisible = true
                                }
                                handler.post { reconnect(4000) }
                            }
                        }
                        if (characteristic.value[0] == 0x03.toByte()) {
                            if (ota_process) {
                                Timber.d("Callback: Control %s, status = %d",
                                        Converters.bytesToHexWhitespaceDelimited(characteristic.value), status)
                                runOnUiThread {
                                    OTAStart?.setBackgroundColor(
                                        ContextCompat.getColor(
                                            this@DeviceServicesActivity,
                                            R.color.silabs_red
                                        )
                                    )
                                    OTAStart?.isClickable = true
                                }
                                boolOTAbegin = false
                                if (boolFullOTA) {
                                    stackPath = ""
                                    runOnUiThread {
                                        otaProgress?.dismiss()
                                        loadingLog?.text = getString(R.string.ota_loading_text)
                                        showLoading()
                                        animaloading()
                                    }
                                    handler.postDelayed({ reconnect(4000) }, 500)
                                }
                            }
                        }
                    } else {
                        Timber.i("OTA_Control: received: %s",
                                Converters.bytesToHexWhitespaceDelimited(characteristic.value)
                        )
                        if (characteristic.value[0] == 0x00.toByte() && characteristic.value[1] == 0x02.toByte()) {
                            Timber.i("HomeKit: reading OTA_Control...")
                            bluetoothGatt?.readCharacteristic(characteristic)
                        }
                    }
                }
                if ((characteristic.uuid == UuidConsts.OTA_DATA)) {   //OTA Data Callback Handling
                    if (reliable) {
                        if (otaProgress?.isShowing!!) {
                            pack += mtuDivisible
                            if (pack <= otafile?.size!! - 1) {
                                otaWriteDataReliable()
                            } else if (pack > otafile?.size!! - 1) {
                                handler.post {
                                    runOnUiThread {
                                        chrono?.stop()
                                        uploadimage?.clearAnimation()
                                        uploadimage?.visibility = View.INVISIBLE
                                    }
                                }
                                boolOTAdata = false
                                retryAttempts = 0
                                dfuMode("OTAEND")
                            }
                        }
                    }
                }
            }
            bluetoothGatt?.readCharacteristic(characteristic)
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int) {
            Constants.LOGS.add(CommonLog("onReliableWriteCompleted, " + "device: " + gatt.device.address + ", status: " + status, gatt.device.address))
        }

        //CALLBACK ON DESCRIPTOR WRITE
        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            Constants.LOGS.add(CommonLog("onDescriptorWrite, " + "device: " + gatt.device.address + ", status: " + status, gatt.device.address))

            remoteServicesFragment.updateDescriptorView(descriptor)
        }

        //CALLBACK ON DESCRIPTOR READ
        override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            Constants.LOGS.add(CommonLog("onDescriptorRead, " + "device: " + gatt.device.address + ", status: " + status, gatt.device.address))

            if (descriptor.uuid == UuidConsts.HOMEKIT_DESCRIPTOR) {
                val value = ByteArray(2)
                value[0] = 0xF2.toByte()
                value[1] = 0xFF.toByte()
                if (descriptor.value[0] == value[0] && descriptor.value[1] == value[1]) {
                    Timber.i("onDescriptorRead:Value = %s", Converters.bytesToHexWhitespaceDelimited(descriptor.value))
                    homeKitOTAControl(descriptor.value)
                }
            } else {
                remoteServicesFragment.updateDescriptorView(descriptor)
            }
        }

        //CALLBACK ON CHARACTERISTIC CHANGED VALUE (READ - CHARACTERISTIC NOTIFICATION)
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            Constants.LOGS.add(GattOperationWithDataLog("onCharacteristicChanged", gatt, null, characteristic))
            remoteServicesFragment.updateCharacteristicView(characteristic)
        }

        //CALLBACK ON SERVICES DISCOVERED
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Constants.LOGS.add(ServicesDiscoveredLog(gatt, status))

            bluetoothGatt = gatt
            discoverTimeout = false
            /**ERROR IN SERVICE DISCOVERY */
            if (status != 0) {
                Timber.d("Error status = %s", Integer.toHexString(status))
                if (errorDialog == null) {
                    runOnUiThread {
                        errorDialog = ErrorDialog(status, object : OtaErrorCallback {
                            override fun onDismiss() {
                                exit(bluetoothGatt)
                            }
                        })
                        errorDialog?.show(supportFragmentManager, "ota_error_dialog")
                    }
                }
            } else {
                /**ON SERVICE DISCOVERY WITHOUT ERROR */
                getServicesInfo(gatt) //SHOW SERVICES IN LOG

                //REFRESH SERVICES UI <- REFRESH SERVICES MENU BUTTON
                if (boolrefresh_services) {
                    boolrefresh_services = false
                    handler.postDelayed({
                        runOnUiThread {
                            onGattFetched()
                            hideCharacteristicLoadingAnimation()
                        }
                    }, GATT_FETCH_ON_SERVICE_DISCOVERED_DELAY.toLong())
                } else {
                    //DEFINE IF DEVICE SUPPORT OTA & MODE (NORMAL/DFU)
                    val otaServiceCheck = gatt.getService(UuidConsts.OTA_SERVICE) != null
                    if (otaServiceCheck) {
                        val otaDataCheck = gatt.getService(UuidConsts.OTA_SERVICE).getCharacteristic(
                            UuidConsts.OTA_DATA
                        ) != null
                        if (otaDataCheck) {
                            val homekitCheck = gatt.getService(UuidConsts.HOMEKIT_SERVICE) != null
                            if (!homekitCheck) {
                                ota_mode = true
                                val otaDataProperty = gatt.getService(UuidConsts.OTA_SERVICE).getCharacteristic(
                                    UuidConsts.OTA_DATA
                                ).properties
                                if ((otaDataProperty == 12) || (otaDataProperty == 8) || (otaDataProperty == 10)) {
                                    //reliable = true;
                                } else if (ota_mode && otaDataProperty == 4) {
                                    //reliable = false;
                                }
                            }
                        } else {
                            if (boolOTAbegin) onceAgain()
                        }
                    }

                    //REQUEST MTU
                    if (UICreated && loadingdialog?.isShowing!!) {
                        bluetoothGatt?.requestMtu(MTU)
                    }

                    //LAUNCH SERVICES UI
                    if (!boolFullOTA) {
                        handler.postDelayed({
                            runOnUiThread {
                                onGattFetched()
                                hideCharacteristicLoadingAnimation()
                            }
                        }, GATT_FETCH_ON_SERVICE_DISCOVERED_DELAY.toLong())
                    }

                    //IF DFU_MODE, LAUNCH OTA SETUP AUTOMATICALLY
                    if (ota_mode && boolOTAbegin) {
                        handler.postDelayed({
                            runOnUiThread {
                                loadingimage?.visibility = View.GONE
                                loadingdialog?.dismiss()
                                showOtaProgress()
                            }
                        }, (2.5 * UI_CREATION_DELAY).toLong())
                    }
                }
            }
        }
    }

    private var client: GoogleApiClient? = null

    /**
     * ACTIVITY STATES MACHINE
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_services)

        setupBottomNavigation()

        registerReceivers()
        handler = Handler()

        setSupportActionBar(toolbar)
        findViewById<View>(R.id.iv_go_back).setOnClickListener { onBackPressed() }

        if (!resources.getBoolean(R.bool.isTablet)) {
            tv_rssi.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            val size = (resources.displayMetrics.density * 16).toInt()
            val params = RelativeLayout.LayoutParams(size, size).apply {
                addRule(RelativeLayout.START_OF, tv_rssi.id)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
            iv_rssi.layoutParams = params
        }

        reScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val btDevice = result.device
                onScanCallback++
                loadingLog?.text = resources.getText(R.string.Waiting_to_connect)
                reconnectGatt(btDevice)
                onScanCallback = 0
            }
        }

        getDeviceAddress(savedInstanceState)?.let { address ->
            initDevice(address)
        }

        client = GoogleApiClient.Builder(this).addApi(AppIndex.API).build()
        fragmentsInit()
        setToolbarItemsNotClicked()

        ll_connections.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                if (btToolbarOpened && btToolbarOpenedName == ToolbarName.CONNECTIONS) {
                    closeToolbar()
                    btToolbarOpened = !btToolbarOpened
                    return
                }
                if (!btToolbarOpened) {
                    bluetooth_browser_background.setBackgroundColor(Color.parseColor("#99000000"))
                    bluetooth_browser_background.visibility = View.VISIBLE
                    ViewCompat.setTranslationZ(bluetooth_browser_background, 4f)
                    animateToolbarOpen()
                    btToolbarOpened = !btToolbarOpened
                }
                setToolbarItemsNotClicked()
                setToolbarItemClicked(iv_connections, tv_connections)
                btToolbarOpenedName = ToolbarName.CONNECTIONS
                setToolbarFragment(connectionsFragment)
            }
        })

        ll_log.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                if (btToolbarOpened && btToolbarOpenedName == ToolbarName.LOGS) {
                    loggerFragment.stopLogUpdater()
                    closeToolbar()
                    btToolbarOpened = !btToolbarOpened
                    return
                }
                if (!btToolbarOpened) {
                    bluetooth_browser_background.setBackgroundColor(Color.parseColor("#99000000"))
                    bluetooth_browser_background.visibility = View.VISIBLE
                    ViewCompat.setTranslationZ(bluetooth_browser_background, 4f)
                    animateToolbarOpen()
                    btToolbarOpened = !btToolbarOpened
                }
                setToolbarItemsNotClicked()
                setToolbarItemClicked(iv_log, tv_log)
                btToolbarOpenedName = ToolbarName.LOGS
                setToolbarFragment(loggerFragment)
                loggerFragment.scrollToEnd()
                loggerFragment.runLogUpdater()
            }
        })

        closeToolbar()
    }

    private fun setupBottomNavigation() {
        supportFragmentManager.beginTransaction()
            .add(R.id.services_fragment_container, localServicesFragment)
            .hide(localServicesFragment)
            .add(R.id.services_fragment_container, remoteServicesFragment)
            .commit()

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.services_bottom_nav)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.services_nav_remote -> remoteServicesFragment.let { newFragment ->
                    supportFragmentManager.beginTransaction()
                        .hide(activeFragment)
                        .show(newFragment)
                        .commit()
                    activeFragment = newFragment
                    true
                }
                R.id.services_nav_local -> localServicesFragment.let { newFragment ->
                    supportFragmentManager.beginTransaction()
                        .hide(activeFragment)
                        .show(newFragment)
                        .commit()
                    activeFragment = newFragment
                    true
                }
                else -> false
            }
        }
    }

    private fun registerReceivers() {
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        registerReceiver(
            bondStateChangeListener,
            IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        )
    }

    private fun getDeviceAddress(savedInstanceState: Bundle?): String? {
        val deviceAddress: String? = if (savedInstanceState == null) {
            intent.extras?.getString("DEVICE_SELECTED_ADDRESS")
        } else {
            savedInstanceState.getString("DEVICE_SELECTED_ADDRESS")
        }
        this.deviceAddress = deviceAddress
        return deviceAddress
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("DEVICE_SELECTED_ADDRESS", deviceAddress)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()

        bluetoothService?.apply {
            registerGattCallback(gattCallback)
            if (!isGattConnected()) {
                showMessage(R.string.toast_debug_connection_failed)
                finish()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        bluetoothService?.unregisterGattCallback()
    }

    override fun onDestroy() {
        super.onDestroy()

        otaProgress?.dismiss()
        loadingdialog?.dismiss()

        unregisterReceivers()
        bluetoothBinding?.unbind()
    }

    private fun unregisterReceivers() {
        unregisterReceiver(bluetoothReceiver)
        unregisterReceiver(bondStateChangeListener)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_device_services, menu)
        Constants.ota_button = menu.findItem(R.id.OTA_button)
        bondMenuItem = menu.findItem(R.id.bond_manage)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_license -> showAboutDialog()
            R.id.bond_manage -> {
                if (item.title == getString(R.string.create_bond)) {
                    bluetoothGatt?.device?.createBond()
                } else {
                    bluetoothGatt?.device?.let { unbondDevice(it) }
                }
            }
            R.id.OTA_button -> if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_EXTERNAL_STORAGE_REQUEST_PERMISSION)
            } else if (UICreated) {
                otaMode = true
                otaOnClick()
            }
            R.id.refresh_services -> {
                boolrefresh_services = true
                refreshServices()
            }
            R.id.request_mtu -> if (UICreated) {
                boolrequest_mtu = true
                showRequestMTU()
            }
            R.id.request_priority -> if (bluetoothGatt != null && newPriority != null) {
                showRequestPriority()
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun unbondDevice(device: BluetoothDevice) {
        if (!removeBond(device)) {
            if (SharedPrefUtils(this@DeviceServicesActivity).shouldDisplayUnbondDeviceDialog()) {
                val dialog = UnbondDeviceDialog(object : UnbondDeviceDialog.Callback {
                    override fun onOkClicked() {
                        try {
                            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                        } catch (e: ActivityNotFoundException) {
                        }
                    }
                })
                dialog.show(supportFragmentManager, "dialog_unbond_device")
            } else {
                try {
                    startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
                } catch (e: ActivityNotFoundException) {
                }
            }
        }
    }

    private fun removeBond(device: BluetoothDevice): Boolean {
        return try {
            return device::class.java.getMethod("removeBond").invoke(device) as Boolean
        } catch (e: Exception) {
            false
        }
    }

    fun onceAgain() {
        writeOtaControl(0x00.toByte())
    }

    /**
     * START OTA BUTTON (UI, Bools)
     */
    private fun otaOnClick() {
        if (ota_mode) {
            ota_process = true
            boolOTAbegin = false
        } else {
            ota_process = true
            boolOTAbegin = true
        }
        runOnUiThread {
            loadingimage?.visibility = View.GONE
            loadingdialog?.dismiss()
            showOtaSetup()
            Constants.ota_button?.isVisible = true
        }
    }

    /**
     * ACTION WHEN MTU MENU BUTTON IS PRESSED
     */
    fun mtuOnButtonMenu() {
        boolrequest_mtu = false
        if (newMTU?.isShowing!!) {
            newMTU?.dismiss()
        }
        runOnUiThread { showMessage(resources.getString(R.string.MTU_colon_n, MTU)) }
    }

    /**
     * CLOSES THE MTU DIALOG AND SHOW OTA SETUP DIALOG
     */
    fun reopenOTASetup() {
        runOnUiThread {
            newMTU?.dismiss()
            otaSetup?.show()
            requestMTU?.progress = MTU
        }
    }

    /**
     * SETS ALL THE INFO IN THE OTA PROGRESS DIALOG TO "" OR 0
     */
    fun resetOTAProgress() {
        boolFullOTA = false
        runOnUiThread {
            datasize?.text = ""
            filename?.text = ""
            loadingimage?.visibility = View.GONE
            loadingdialog?.dismiss()
            progressBar?.progress = 0
            datasize?.text = resources.getString(R.string.zero_percent)
            dataRate?.text = ""
            OTAStart?.isClickable = false
            OTAStart?.setBackgroundColor(ContextCompat.getColor(this@DeviceServicesActivity, R.color.silabs_button_inactive))
            showOtaProgress()
        }
    }

    /**
     * USED TO CLEAN CACHE AND REDISCOVER SERVICES
     */
    private fun refreshServices() {
        if (bluetoothGatt != null && bluetoothGatt?.device != null) {
            refreshDeviceCache()
            bluetoothGatt?.discoverServices()
        } else if (bluetoothService != null && bluetoothService?.connectedGatt != null) {
            refreshDeviceCache()
            bluetoothService?.connectedGatt?.discoverServices()
        }
    }

    /**
     * INITIALIZES ABOUT DIALOG
     */
    private fun initAboutDialog() {
        dialogLicense = Dialog(this)
        dialogLicense?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogLicense?.setContentView(R.layout.dialog_about_silicon_labs_blue_gecko)
        val webView = dialogLicense?.findViewById<WebView>(R.id.menu_item_license)
        val closeButton = dialogLicense?.findViewById<Button>(R.id.close_about_btn)
        webView?.loadUrl(ABOUT_DIALOG_HTML_ASSET_FILE_PATH)
        closeButton?.setOnClickListener { dialogLicense?.dismiss() }
    }

    /**
     * INITIALIZES OTA PROGRESS DIALOG
     */
    private fun initOtaProgress() {
        otaProgress = Dialog(this)
        otaProgress?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        otaProgress?.setContentView(R.layout.dialog_ota_progress)

        val address = otaProgress?.findViewById<TextView>(R.id.device_address)
        address?.text = bluetoothGatt?.device?.address

        progressBar = otaProgress?.findViewById(R.id.otaprogress)
        dataRate = otaProgress?.findViewById(R.id.datarate)
        datasize = otaProgress?.findViewById(R.id.datasize)
        filename = otaProgress?.findViewById(R.id.filename)
        steps = otaProgress?.findViewById(R.id.otasteps)
        chrono = otaProgress?.findViewById(R.id.chrono)
        OTAStart = otaProgress?.findViewById(R.id.otabutton)
        sizename = otaProgress?.findViewById(R.id.sizename)
        mtuname = otaProgress?.findViewById(R.id.mtuname)
        uploadimage = otaProgress?.findViewById(R.id.connecting_spinner)

        OTAStart?.setOnClickListener {
            otaProgress?.dismiss()
            dfuMode("DISCONNECTION")
        }
    }

    /**
     * INITIALIZES OTA SETUP DIALOG
     */
    private fun initOtaSetup() {
        otaSetup = Dialog(this)
        otaSetup?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        otaSetup?.setContentView(R.layout.dialog_ota_config)
        otaSetup?.setCancelable(false)

        partialOTA = otaSetup?.findViewById(R.id.radio_ota)
        val address = otaSetup?.findViewById<TextView>(R.id.device_address)
        address?.text = bluetoothGatt?.device?.address

        fullOTA = otaSetup?.findViewById(R.id.radio_ota_full)
        val stacklayout = otaSetup?.findViewById<LinearLayout>(R.id.stacklayout)

        OTA_OK = otaSetup?.findViewById(R.id.ota_proceed)
        val OTA_CANCEL = otaSetup?.findViewById<Button>(R.id.ota_cancel)

        reliableWrite = otaSetup?.findViewById(R.id.check_reliable)
        delaySeekBar = otaSetup?.findViewById(R.id.delay_seekBar)
        delayText = otaSetup?.findViewById(R.id.delay_text)
        delayText?.visibility = View.INVISIBLE
        delaySeekBar?.visibility = View.GONE
        requestMTU = otaSetup?.findViewById(R.id.mtu_seekBar)
        reliabilityRB = otaSetup?.findViewById(R.id.reliability_radio_button)
        speedRB = otaSetup?.findViewById(R.id.speed_radio_button)

        val mtuValue = otaSetup?.findViewById<EditText>(R.id.mtu_value)
        mtuValue?.setOnEditorActionListener { _, _, _ ->
            if (mtuValue.text != null) {
                var test = mtuValue.text.toString().toInt()
                if (test < 23) test = 23 else if (test > 250) test = 250
                requestMTU?.progress = test - 23
                MTU = test
            }
            false
        }

        appLoaderFileButton = otaSetup?.findViewById(R.id.select_apploader_file_btn)
        appFileButton = otaSetup?.findViewById(R.id.select_app_file_btn)
        appLoaderFileButton?.setOnClickListener {
            val i = Intent()
            i.type = "*/*"
            i.action = Intent.ACTION_GET_CONTENT
            currentOtaFileType = OtaFileType.APPLOADER
            startActivityForResult(Intent.createChooser(i, "Choose directory"), FILE_CHOOSER_REQUEST_CODE)
        }

        appFileButton?.setOnClickListener {
            val i = Intent()
            i.type = "*/*"
            i.action = Intent.ACTION_GET_CONTENT
            currentOtaFileType = OtaFileType.APPLICATION
            startActivityForResult(Intent.createChooser(i, "Choose directory"), FILE_CHOOSER_REQUEST_CODE)
        }

        requestMTU?.max = 250 - 23
        requestMTU?.progress = 250 - 23
        requestMTU?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mtuValue?.setText("" + (progress + 23))
                MTU = progress + 23
            }
        })

        val requestPriority = otaSetup?.findViewById<SeekBar>(R.id.connection_seekBar)
        requestPriority?.max = 2
        requestPriority?.progress = 2
        priority = 1
        requestPriority?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                Log.d("onProgressChanged", "" + progress)
                when (progress) {
                    1 -> priority = 0 //BALANCE
                    2 -> priority = 1 //HIGH
                    0 -> priority = 2 //LOW
                } //LOW
            }
        })

        delaySeekBar?.max = 100
        delaySeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                delayText?.text = "$progress ms"
                delayNoResponse = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        OTA_CANCEL?.setOnClickListener {
            otaMode = false
            otaSetup?.dismiss()
            boolOTAbegin = false
            ota_process = false
        }

        OTA_OK?.setOnClickListener {
            OTA_OK?.isClickable = false
            runOnUiThread {
                otaSetup?.dismiss()
                if (ota_mode) {
                    bluetoothGatt?.requestMtu(mtuValue?.text.toString().toInt())
                } else dfuMode("OTABEGIN")
            }
        }

        fullOTA?.setOnClickListener {
            stacklayout?.visibility = View.VISIBLE
            partialOTA?.setBackgroundColor(ContextCompat.getColor(this@DeviceServicesActivity, R.color.silabs_red))
            fullOTA?.setBackgroundColor(ContextCompat.getColor(this@DeviceServicesActivity, R.color.silabs_red_selected))
            doubleStepUpload = true
            if (areFullOTAFilesCorrect()) {
                OTA_OK?.isClickable = true
                OTA_OK?.setBackgroundColor(ContextCompat.getColor(this@DeviceServicesActivity, R.color.silabs_red))
            } else {
                OTA_OK?.isClickable = false
                OTA_OK?.setBackgroundColor(ContextCompat.getColor(this@DeviceServicesActivity, R.color.silabs_button_inactive))
            }
        }

        partialOTA?.setOnClickListener {
            stacklayout?.visibility = View.GONE
            partialOTA?.setBackgroundColor(ContextCompat.getColor(this@DeviceServicesActivity, R.color.silabs_red_selected))
            fullOTA?.setBackgroundColor(ContextCompat.getColor(this@DeviceServicesActivity, R.color.silabs_red))
            doubleStepUpload = false
            if (arePartialOTAFilesCorrect()) {
                OTA_OK?.isClickable = true
                OTA_OK?.setBackgroundColor(ContextCompat.getColor(this@DeviceServicesActivity, R.color.silabs_red))
            } else {
                OTA_OK?.isClickable = false
                OTA_OK?.setBackgroundColor(ContextCompat.getColor(this@DeviceServicesActivity, R.color.silabs_button_inactive))
            }
        }

        reliableWrite?.setOnClickListener {
            if (!reliableWrite?.isChecked!!) {
                reliable = false
                delayText?.visibility = View.VISIBLE
                delaySeekBar?.visibility = View.VISIBLE
            } else {
                delayText?.visibility = View.INVISIBLE
                delaySeekBar?.visibility = View.GONE
                reliable = true
            }
        }

        reliabilityRB?.setOnClickListener { reliable = true }
        speedRB?.setOnClickListener { reliable = false }
    }

    /**
     * INITIALIZES MTU DIALOG
     */
    private fun initNewMTU() {
        newMTU = Dialog(this)
        newMTU?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        newMTU?.setContentView(R.layout.dialog_new_mtu)
        val mtu_value = newMTU?.findViewById<TextView>(R.id.request_mtu_value)
        val requestMTU = newMTU?.findViewById<SeekBar>(R.id.request_mtu_seekBar)
        requestMTU?.max = 250 - 23
        requestMTU?.progress = 250 - 23
        requestMTUValue = requestMTU?.progress!! + 23
        requestMTU.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mtu_value?.text = "" + (progress + 23)
                requestMTUValue = progress + 23
            }
        })

        val requestBtn = newMTU?.findViewById<Button>(R.id.request)
        val cancelrequest = newMTU?.findViewById<Button>(R.id.cancel_request)
        requestBtn?.setOnClickListener {
            bluetoothGatt?.requestMtu(requestMTUValue)
            newMTU?.dismiss()
            if (!boolrequest_mtu) otaSetup?.show()
        }
        cancelrequest?.setOnClickListener {
            newMTU?.dismiss()
            if (!boolrequest_mtu) otaSetup?.show()
        }
    }

    /**
     * INITIALIZES CONNECTION INTERVAL DIALOG
     */
    private fun initNewPriority() {
        newPriority = Dialog(this)
        newPriority?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        newPriority?.setContentView(R.layout.dialog_new_priority)
        val request = newPriority!!.findViewById<Button>(R.id.request)
        val cancelrequest = newPriority!!.findViewById<Button>(R.id.cancel_request)
        val lowPriority = newPriority!!.findViewById<CheckBox>(R.id.low_priority)
        val balancedPriority = newPriority!!.findViewById<CheckBox>(R.id.balanced_priority)
        val highPriority = newPriority!!.findViewById<CheckBox>(R.id.high_priority)

        lowPriority.setOnClickListener {
            if (lowPriority.isChecked) {
                if (highPriority.isChecked) highPriority.isChecked = false
                if (balancedPriority.isChecked) balancedPriority.isChecked = false
            }
        }

        balancedPriority?.setOnClickListener {
            if (balancedPriority.isChecked) {
                if (lowPriority.isChecked) lowPriority.isChecked = false
                if (highPriority.isChecked) highPriority.isChecked = false
            }
        }

        highPriority.setOnClickListener {
            if (highPriority.isChecked) {
                if (lowPriority.isChecked) lowPriority.isChecked = false
                if (balancedPriority.isChecked) balancedPriority.isChecked = false
            }
        }

        request.setOnClickListener {
            if (highPriority.isChecked || balancedPriority.isChecked || lowPriority.isChecked) {
                when {
                    highPriority.isChecked -> {
                        bluetoothGatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                        showMessage(resources.getString(R.string.CONNECTION_PRIORITY_HIGH))
                    }
                    balancedPriority.isChecked -> {
                        bluetoothGatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED)
                        showMessage(resources.getString(R.string.CONNECTION_PRIORITY_BALANCED))
                    }
                    lowPriority.isChecked -> {
                        bluetoothGatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER)
                        showMessage(resources.getString(R.string.CONNECTION_PRIORITY_LOW))
                    }
                }
            }
            newPriority?.dismiss()
        }
        cancelrequest.setOnClickListener { newPriority?.dismiss() }
    }

    /**
     * INITIALIZES LOADING DIALOG
     */
    private fun initLoading() {
        loadingdialog = Dialog(this)
        loadingdialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        loadingdialog?.setContentView(R.layout.dialog_loading)

        loadingimage = loadingdialog?.findViewById(R.id.connecting_spinner)
        loadingLog = loadingdialog?.findViewById(R.id.loadingLog)
        loadingHeader = loadingdialog?.findViewById(R.id.loading_header)
    }

    /**
     * SHOWS OTA PROGRESS DIALOG IN UI
     */
    private fun showOtaProgress() {
        otaProgress?.show()
        OTAStart?.isClickable = false
        otaProgress?.setCanceledOnTouchOutside(false)
        dfuMode("OTABEGIN") //OTAProgress
    }

    /**
     * SHOWS OTA SETUP DIALOG IN UI
     */
    private fun showOtaSetup() {
        if (otaSetup != null && !otaSetup?.isShowing!!) {
            otaSetup?.show()
            otaSetup?.setCanceledOnTouchOutside(false)

            if (areFullOTAFilesCorrect() && doubleStepUpload) {
                OTA_OK?.isClickable = true
                OTA_OK?.setBackgroundColor(ContextCompat.getColor(this@DeviceServicesActivity, R.color.silabs_red))
            } else if (arePartialOTAFilesCorrect() && !doubleStepUpload) {
                OTA_OK?.isClickable = true
                OTA_OK?.setBackgroundColor(ContextCompat.getColor(this@DeviceServicesActivity, R.color.silabs_red))
            } else {
                OTA_OK?.isClickable = false
                OTA_OK?.setBackgroundColor(ContextCompat.getColor(this@DeviceServicesActivity, R.color.silabs_button_inactive))
            }

            if (reliable) {
                reliableWrite?.isChecked = true
            } else {
                delaySeekBar?.visibility = View.VISIBLE
                delayText?.visibility = View.VISIBLE
            }
        }
    }

    /**
     * SHOWS OTA SETUP DIALOG IN UI
     */
    private fun showLoading() {
        loadingdialog?.let { dialog ->
            dialog.show()
            dialog.setCanceledOnTouchOutside(false)
            animaloading()
        }
    }

    /**
     * SHOWS OTA REQUEST MTU DIALOG IN UI
     */
    private fun showRequestMTU() {
        newMTU?.show()
        newMTU?.setCanceledOnTouchOutside(false)
    }

    /**
     * SHOWS OTA CONNECTION INTERVAL DIALOG IN UI
     */
    private fun showRequestPriority() {
        newPriority?.show()
        newPriority?.setCanceledOnTouchOutside(false)
    }

    /**
     * SHOWS OTA ABOUT DIALOG IN UI
     */
    private fun showAboutDialog() {
        dialogLicense?.show()
    }

    /**
     * INITILIAZES ALL NECESSARY DIALOGS AND VIEW IN UI - ONCREATE
     */
    private fun onGattFetched() {
        connectionsAdapter.selectedDevice = bluetoothGatt?.device?.address
        connectionsAdapter.notifyDataSetChanged()

        var deviceName = bluetoothGatt?.device?.name
        deviceName = if (TextUtils.isEmpty(deviceName)) getString(R.string.not_advertising_shortcut) else deviceName

        displayBondState(bluetoothGatt?.device)

        supportActionBar?.title = deviceName
        remoteServicesFragment.init()
        localServicesFragment.init()
        initAboutDialog()

        if (!boolOTAbegin) {
            initOtaSetup()
            initOtaProgress()
            initLoading()
            initNewMTU()
            initNewPriority()
        }

        if (btToolbarOpened) {
            closeToolbar()
            btToolbarOpened = !btToolbarOpened
        }
    }

    /**
     * READ ALL THE SERVICES, PRINT IT ON LOG AND RECOGNIZES HOMEKIT ACCESSORIES
     */
    fun getServicesInfo(gatt: BluetoothGatt) {
        val gattServices = gatt.services
        Log.i("onServicesDiscovered", "Services count: " + gattServices.size)
        for (gattService: BluetoothGattService in gattServices) {
            val serviceUUID = gattService.uuid.toString()
            Log.i("onServicesDiscovered", "Service UUID " + serviceUUID + " - Char count: " + gattService.characteristics.size)
            val gattCharacteristics = gattService.characteristics
            for (gattCharacteristic: BluetoothGattCharacteristic in gattCharacteristics) {
                val characteristicUUID = gattCharacteristic.uuid.toString()
                Log.i("onServicesDiscovered", "Characteristic UUID " + characteristicUUID + " - Properties: " + gattCharacteristic.properties)
                if ((gattCharacteristic.uuid.toString() == UuidConsts.OTA_CONTROL.toString())) {
                    if (gattCharacteristics.contains(bluetoothGatt?.getService(UuidConsts.OTA_SERVICE)?.getCharacteristic(
                            UuidConsts.OTA_DATA
                        ))) {
                        if (!gattServices.contains(bluetoothGatt?.getService(UuidConsts.HOMEKIT_SERVICE))) {
                            Log.i("onServicesDiscovered", "Device in DFU Mode")
                        } else {
                            Log.i("onServicesDiscovered", "OTA_Control found")
                            val gattDescriptors = gattCharacteristic.descriptors
                            for (gattDescriptor: BluetoothGattDescriptor in gattDescriptors) {
                                val descriptor = gattDescriptor.uuid.toString()
                                if ((gattDescriptor.uuid.toString() == UuidConsts.HOMEKIT_DESCRIPTOR.toString())) {
                                    kit_descriptor = gattDescriptor
                                    Log.i("descriptor", "UUID: $descriptor")
                                    //bluetoothGatt.readDescriptor(gattDescriptor);
                                    val stable = byteArrayOf(0x00.toByte(), 0x00.toByte())
                                    homeKitOTAControl(stable)
                                    homekit = true
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * WRITES OTA CONTROL FOR HOMEKIT DEVICES
     */
    fun homeKitOTAControl(instanceID: ByteArray) {

        //WRITE CHARACTERISTIC FOR HOMEKIT
        val value = byteArrayOf(0x00, 0x02, 0xee.toByte(), instanceID[0], instanceID[1], 0x03, 0x00, 0x01, 0x01, 0x01)
        writeGenericCharacteristic(UuidConsts.OTA_SERVICE, UuidConsts.OTA_CONTROL, value)
        Log.d("characteristic", "writting: " + Converters.bytesToHexWhitespaceDelimited(value))
    }

    /**
     * WRITES BYTE TO OTA CONTROL CHARACTERISTIC
     */
    private fun writeOtaControl(ctrl: Byte): Boolean {
        Log.d("writeOtaControl", "Called")
        if (bluetoothGatt?.getService(UuidConsts.OTA_SERVICE) != null) {
            val charac = bluetoothGatt?.getService(UuidConsts.OTA_SERVICE)?.getCharacteristic(
                UuidConsts.OTA_CONTROL
            )
            if (charac != null) {
                Log.d("Instance ID", "" + charac.instanceId)
                charac.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                Log.d("charac_properties", "" + charac.properties)
                val control = ByteArray(1)
                control[0] = ctrl
                charac.value = control
                bluetoothGatt?.writeCharacteristic(charac)
                return true
            } else {
                Log.d("characteristic", "null")
            }
        } else {
            Log.d("service", "null")
        }
        return false
    }

    /**
     * WRITES BYTE ARRAY TO A GENERIC CHARACTERISTIC
     */
    private fun writeGenericCharacteristic(service: UUID?, characteristic: UUID?, value: ByteArray?): Boolean {
        if (bluetoothGatt != null) {
            val bluetoothGattCharacteristic = bluetoothGatt?.getService(service)?.getCharacteristic(characteristic)
            Log.d("characteristic", "exists")
            if (bluetoothGattCharacteristic != null) {
                bluetoothGattCharacteristic.value = value
                bluetoothGattCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                bluetoothGatt?.writeCharacteristic(bluetoothGattCharacteristic)
                Log.d("characteristic", "written")
            } else {
                Log.d("characteristic", "null")
                return false
            }
        } else {
            Log.d("bluetoothGatt", "null")
            return false
        }
        return true
    }

    @Synchronized
    fun whiteOtaData(datathread: ByteArray?) {
        try {
            boolOTAdata = true
            val value = ByteArray(MTU - 3)
            val start = System.nanoTime()
            var j = 0
            for (i in datathread?.indices!!) {
                value[j] = datathread[i]
                j++
                if (j >= MTU - 3 || i >= (datathread.size - 1)) {
                    var wait = System.nanoTime()
                    val charac = bluetoothGatt?.getService(UuidConsts.OTA_SERVICE)?.getCharacteristic(
                        UuidConsts.OTA_DATA
                    )
                    charac?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    val progress = ((i + 1).toFloat() / datathread.size) * 100
                    val bitrate = (((i + 1) * (8.0)).toFloat() / (((wait - start) / 1000000.0).toFloat()))
                    if (j < MTU - 3) {
                        val end = ByteArray(j)
                        System.arraycopy(value, 0, end, 0, j)
                        Log.d("Progress", "sent " + (i + 1) + " / " + datathread.size + " - " + String.format("%.1f", progress) + " % - " + String.format("%.2fkbit/s", bitrate) + " - " + Converters.bytesToHexWhitespaceDelimited(end))
                        runOnUiThread {
                            datasize?.text = progress.toInt().toString() + " %"
                            progressBar?.progress = progress.toInt()
                        }
                        charac?.value = end
                    } else {
                        j = 0
                        Log.d("Progress", "sent " + (i + 1) + " / " + datathread.size + " - " + String.format("%.1f", progress) + " % - " + String.format("%.2fkbit/s", bitrate) + " - " + Converters.bytesToHexWhitespaceDelimited(value))
                        runOnUiThread {
                            datasize?.text = progress.toInt().toString() + " %"
                            progressBar?.progress = progress.toInt()
                        }
                        charac?.value = value
                    }
                    if (bluetoothGatt?.writeCharacteristic(charac)!!) {
                        runOnUiThread {
                            val datarate = String.format(Locale.US, "%.2fkbit/s", bitrate)
                            dataRate?.text = datarate
                        }
                        while ((System.nanoTime() - wait) / 1000000.0 < delayNoResponse);
                    } else {
                        do {
                            while ((System.nanoTime() - wait) / 1000000.0 < delayNoResponse);
                            wait = System.nanoTime()
                            runOnUiThread {
                                val datarate = String.format(Locale.US, "%.2fkbit/s", bitrate)
                                dataRate?.text = datarate
                            }
                        } while (!bluetoothGatt?.writeCharacteristic(charac)!!)
                    }
                }
            }
            val end = System.currentTimeMillis()
            val time = (end - start) / 1000L.toFloat()
            Log.d("OTA Time - ", "" + time + "s")
            boolOTAdata = false
            runOnUiThread {
                chrono?.stop()
                uploadimage?.clearAnimation()
                uploadimage?.visibility = View.INVISIBLE
            }
            dfuMode("OTAEND")
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    /**
     * WRITES EBL/GBL FILES TO OTA_DATA CHARACTERISTIC
     */
    @Synchronized
    fun otaWriteDataReliable() {
        boolOTAdata = true
        if (pack == 0) {
            /**SET MTU_divisible by 4 */
            var minus = 0
            do {
                mtuDivisible = MTU - 3 - minus
                minus++
            } while (mtuDivisible % 4 != 0)
            runOnUiThread { mtuname?.text = "$mtuDivisible bytes" }
        }
        val writearray: ByteArray
        val pgss: Float
        if (pack + mtuDivisible > otafile?.size!! - 1) {
            /**SET last by 4 */
            var plus = 0
            var last = otafile?.size!! - pack
            do {
                last += plus
                plus++
            } while (last % 4 != 0)
            writearray = ByteArray(last)
            for ((j, i) in (pack until pack + last).withIndex()) {
                if (otafile?.size!! - 1 < i) {
                    writearray[j] = 0xFF.toByte()
                } else writearray[j] = otafile!![i]
            }
            pgss = ((pack + last).toFloat() / (otafile?.size!! - 1)) * 100
            Log.d("characte", "last: " + pack + " / " + (pack + last) + " : " + Converters.bytesToHexWhitespaceDelimited(writearray))
        } else {
            var j = 0
            writearray = ByteArray(mtuDivisible)
            for (i in pack until pack + mtuDivisible) {
                writearray[j] = otafile!![i]
                j++
            }
            pgss = ((pack + mtuDivisible).toFloat() / (otafile?.size!! - 1)) * 100
            Log.d("characte", "pack: " + pack + " / " + (pack + mtuDivisible) + " : " + Converters.bytesToHexWhitespaceDelimited(writearray))
        }
        val charac = bluetoothGatt?.getService(UuidConsts.OTA_SERVICE)?.getCharacteristic(UuidConsts.OTA_DATA)
        charac?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        charac?.value = writearray
        bluetoothGatt?.writeCharacteristic(charac)
        val waiting_time = (System.currentTimeMillis() - otatime)
        val bitrate = 8 * pack.toFloat() / waiting_time
        if (pack > 0) {
            handler.post {
                runOnUiThread {
                    progressBar?.progress = pgss.toInt()
                    val datarate = String.format(Locale.US, "%.2fkbit/s", bitrate)
                    dataRate?.text = datarate
                    datasize?.text = pgss.toInt().toString() + " %"
                }
            }
        } else {
            otatime = System.currentTimeMillis()
        }
    }

    /**
     * (RUNNABLE) CHECKS OTA BEGIN BOX AND STARTS
     */
    private val checkbeginrunnable: Runnable = Runnable {
        chrono?.base = SystemClock.elapsedRealtime()
        chrono?.start()
    }

    /**
     * CREATES BAR PROGRESS ANIMATION IN LOADING AND OTA PROGRESS DIALOG
     */
    private fun animaloading() {
        if ((uploadimage != null) && (loadingimage != null) && (otaProgress != null)) {
            uploadimage?.visibility = View.GONE
            loadingimage?.visibility = View.GONE
            if (loadingdialog?.isShowing!!) {
                loadingimage?.visibility = View.VISIBLE
            }
            if (otaProgress?.isShowing!!) {
                uploadimage?.visibility = View.VISIBLE
            }
        }
    }

    /**
     * OTA STATE MACHINE
     */
    @Synchronized
    fun dfuMode(step: String?) {
        when (step) {
            "INIT" -> dfuMode("OTABEGIN")
            "OTABEGIN" -> if (ota_mode) {
                //START OTA PROCESS -> gattCallback -> OnCharacteristicWrite
                Log.d("OTA_BEGIN", "true")
                handler.postDelayed(WRITE_OTA_CONTROL_ZERO, 200)
            } else {
                if (homekit) {
                    bluetoothGatt?.readDescriptor(kit_descriptor)
                } else {
                    Log.d("DFU_MODE", "true")
                    handler.postDelayed(WRITE_OTA_CONTROL_ZERO, 200)
                }
            }
            "OTAUPLOAD" -> {
                Log.d("OTAUPLOAD", "Called")
                /**Check Services */
                val mBluetoothGattService = bluetoothGatt?.getService(UuidConsts.OTA_SERVICE)
                if (mBluetoothGattService != null) {
                    val charac = bluetoothGatt?.getService(UuidConsts.OTA_SERVICE)!!.getCharacteristic(
                        UuidConsts.OTA_DATA
                    )
                    if (charac != null) {
                        charac.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                        Log.d("Instance ID", "" + charac.instanceId)
                        /**Check Files */
                        var ebl: ByteArray? = null
                        try {
                            Log.d("stackPath", "" + stackPath)
                            Log.d("appPath", "" + appPath)
                            val file: File
                            if (stackPath != "" && doubleStepUpload) {
                                file = File(stackPath)
                                boolFullOTA = true
                            } else {
                                file = File(appPath)
                                boolFullOTA = false
                            }
                            val fileInputStream = FileInputStream(file)
                            val size = fileInputStream.available()
                            Log.d("size", "" + size)
                            val temp = ByteArray(size)
                            fileInputStream.read(temp)
                            fileInputStream.close()
                            ebl = temp
                        } catch (e: Exception) {
                            Log.e("InputStream", "Couldn't open file$e")
                        }
                        val datathread = ebl
                        otafile = ebl
                        /**Check if it is partial of full OTA */
                        val fn: String
                        if (stackPath != "" && doubleStepUpload) {
                            val last = stackPath.lastIndexOf(File.separator)
                            fn = getString(R.string.ota_filename_s, stackPath.substring(last).removePrefix("/"))
                            Log.d("CurrentlyUpdating", "apploader")
                        } else {
                            val last = appPath.lastIndexOf(File.separator)
                            fn = getString(R.string.ota_filename_s, appPath.substring(last).removePrefix("/"))
                            Log.d("CurrentlyUpdating", "appliaction")
                        }
                        pack = 0
                        /**Prepare information about current upload step */
                        val stepInfo: String
                        if (doubleStepUpload) {
                            if (stackPath != "") {
                                stepInfo = "1 OF 2"
                            } else {
                                stepInfo = "2 OF 2"
                            }
                        } else {
                            stepInfo = "1 OF 1"
                        }
                        /**Set info into UI OTA Progress */
                        runOnUiThread {
                            filename?.text = fn
                            steps?.text = stepInfo
                            sizename?.text = datathread?.size.toString() + " bytes"
                            mtuname?.text = MTU.toString()
                            uploadimage?.visibility = View.VISIBLE
                            animaloading()
                        }
                        /**Start OTA_data Upload in another thread */
                        val otaUpload = Thread(Runnable {
                            if (reliable) {
                                otaWriteDataReliable()
                            } else whiteOtaData(datathread)
                        })
                        otaUpload.start()
                    }
                }
            }
            "OTAEND" -> {
                Log.d("OTAEND", "Called")
                handler.postDelayed({ writeOtaControl(0x03.toByte()) }, 500)
            }
            "DISCONNECTION" -> {
                ota_process = false
                boolFullOTA = false
                boolOTAbegin = false
                disconnectGatt(bluetoothGatt)
            }
            else -> {
            }
        }
    }

    //Not used - Reconnect with device after scanner
    private fun reconnectGatt(btDevice: BluetoothDevice) {
        bluetoothDevice = btDevice
        stopScan()
        val reconnectTimer = Timer()
        reconnectTimer.schedule(object : TimerTask() {
            override fun run() {
                bluetoothService?.connectGatt(bluetoothDevice!!, false, gattCallback)
                bluetoothGatt = bluetoothService?.connectedGatt
            }
        }, delayToConnect)
    }

    private fun stopScan() {
        bluetoothLeScanner?.stopScan(reScanCallback)
        Log.d("stopScan", "Called")
    }

    /**
     * CALLS A METHOD TO CLEAN DEVICE SERVICES
     */
    private fun refreshDeviceCache(): Boolean {
        try {
            Log.d("refreshDevice", "Called")
            val localMethod: Method = bluetoothGatt?.javaClass?.getMethod("refresh")!!
            val bool: Boolean = (localMethod.invoke(bluetoothGatt, *arrayOfNulls(0)) as Boolean)
            Log.d("refreshDevice", "bool: $bool")
            return bool
        } catch (localException: Exception) {
            Log.e("refreshDevice", "An exception occured while refreshing device")
        }
        return false
    }

    /**
     * DISCONNECT GATT GENTLY AND CLEAN GLOBAL VARIABLES
     */
    fun disconnectGatt(gatt: BluetoothGatt?) {
        val disconnectTimer = Timer()
        boolFullOTA = false
        boolOTAbegin = false
        ota_process = false
        disconnect_gatt = true
        UICreated = false

        if (gatt != null && gatt.device != null) {
            if (loadingdialog == null) {
                initLoading()
            }

            val btGatt: BluetoothGatt = gatt
            disconnectTimer.schedule(object : TimerTask() {
                override fun run() {
                    /**Getting bluetoothDevice to FetchUUID */
                    if (btGatt.device != null) bluetoothDevice = btGatt.device
                    /**Disconnect gatt */
                    btGatt.disconnect()
                    bluetoothService?.clearConnectedGatt()
                    Log.d("disconnectGatt", "gatt disconnect")
                    runOnUiThread {
                        showLoading()
                        loadingLog?.text = "Disconnecting..."
                        loadingHeader?.text = "GATT Connection"
                    }
                }
            }, 200)
            disconnectTimer.schedule(object : TimerTask() {
                override fun run() {
                    bluetoothDevice?.fetchUuidsWithSdp()
                }
            }, 300)
            disconnectionTimeout = true
            val timeout = Runnable {
                handler.postDelayed({
                    if (disconnectionTimeout) {
                        finish()
                        runOnUiThread { showMessage("DISCONNECTION PROBLEM") }
                    }
                }, 5000)
            }
            Thread(timeout).start()
        } else {
            finish()
        }
    }

    /**
     * CLEANS USER INTERFACE AND FINISH ACTIVITY
     */
    fun exit(gatt: BluetoothGatt?) {
        gatt?.close()
        bluetoothService?.connectedGatt?.close()
        bluetoothService?.clearCache()
        disconnect_gatt = false

        handler.postDelayed({
            bluetoothGatt = null
            bluetoothService = null
            if (loadingdialog != null && loadingdialog?.isShowing!!) loadingdialog?.dismiss()
            if (otaProgress != null && otaProgress?.isShowing!!) otaProgress?.dismiss()
            if (otaSetup != null && otaSetup?.isShowing!!) otaSetup?.dismiss()
            finish()
        }, 1000)
    }

    /**
     * DISCONNECTS AND CONNECTS WITH THE SELECTED DELAY
     */
    fun reconnect(delaytoconnect: Long) {
        val reconnectTimer = Timer()
        bluetoothDevice = bluetoothGatt?.device
        if (bluetoothService?.isGattConnected()!!) {
            bluetoothService?.clearConnectedGatt()
            bluetoothService?.clearCache()
        }
        bluetoothGatt?.disconnect()
        reconnectTimer.schedule(object : TimerTask() {
            override fun run() {
                bluetoothGatt?.close()
            }
        }, 400)
        reconnectTimer.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (loadingdialog?.isShowing!!) {
                        loadingLog?.text = "Attempting connection..."
                    }
                }
                bluetoothDevice?.let { device ->
                    bluetoothService?.connectGatt(device, false, gattCallback)
                }
            }
        }, delaytoconnect)
    }

    /**
     * ANIMATIONS CONTROLLERS
     */
    fun showCharacteristicLoadingAnimation() {
        runOnUiThread {
            loading_container.setOnClickListener { /* this onclicklistener prevents services and characteristics from user interaction before ui is loaded*/ }
            val loadingGradientAnimation = AnimationUtils.loadAnimation(this@DeviceServicesActivity, R.anim.connection_translate_right)
            loading_container.visibility = View.VISIBLE
            loading_anim_gradient_right_container.startAnimation(loadingGradientAnimation)
            val loadingBarFlyIn = AnimationUtils.loadAnimation(this@DeviceServicesActivity, R.anim.scanning_bar_fly_in)
            loading_bar_container.startAnimation(loadingBarFlyIn)
        }
    }

    fun hideCharacteristicLoadingAnimation() {
        runOnUiThread {
            loading_anim_gradient_right_container.clearAnimation()
            val loadingBarFlyIn = AnimationUtils.loadAnimation(this@DeviceServicesActivity, R.anim.scanning_bar_fly_out)
            loading_bar_container.startAnimation(loadingBarFlyIn)
            loadingBarFlyIn.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}
                override fun onAnimationEnd(animation: Animation) {
                    loading_container.visibility = View.GONE
                }

                override fun onAnimationRepeat(animation: Animation) {}
            })
        }
    }


    private val indexApiAction: Action
        get() {
            val `object` = Thing.Builder()
                    .setName("DeviceServices Page")
                    .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                    .build()
            return Action.Builder(Action.TYPE_VIEW)
                    .setObject(`object`)
                    .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                    .build()
        }

    public override fun onStart() {
        super.onStart()
        client?.connect()
        AppIndex.AppIndexApi.start(client, indexApiAction)
    }

    public override fun onStop() {
        super.onStop()
        AppIndex.AppIndexApi.end(client, indexApiAction)
        client?.disconnect()
    }

    private fun animateToolbarOpen() {
        val animator = ValueAnimator.ofInt(0, percentHeightToPx(TOOLBAR_OPEN_PERCENTAGE)).setDuration(350)
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            frame_layout.layoutParams.height = value
            frame_layout.requestLayout()
        }
        frame_layout.visibility = View.VISIBLE
        ViewCompat.setTranslationZ(framelayout_container, 5f)
        val set = AnimatorSet()
        set.play(animator)
        set.interpolator = AccelerateDecelerateInterpolator()
        set.start()
    }

    private fun closeToolbar() {
        animateToolbarClose()
        setToolbarItemsNotClicked()
        bluetooth_browser_background.visibility = View.GONE
    }

    private fun setToolbarItemClicked(imageView: ImageView?, textView: TextView?) {
        textView?.setTextColor(ContextCompat.getColor(this, R.color.silabs_blue))
        DrawableCompat.setTint(imageView?.drawable!!, ContextCompat.getColor(this, R.color.silabs_blue))
    }

    private fun setToolbarItemsNotClicked() {
        tv_connections.setTextColor(ContextCompat.getColor(this, R.color.silabs_primary_text))
        DrawableCompat.setTint(iv_connections.drawable, ContextCompat.getColor(this, R.color.silabs_primary_text))
        tv_log.setTextColor(ContextCompat.getColor(this, R.color.silabs_primary_text))
        DrawableCompat.setTint(iv_log.drawable, ContextCompat.getColor(this, R.color.silabs_primary_text))
    }

    private fun animateToolbarClose() {
        val animator = ValueAnimator.ofInt(percentHeightToPx(TOOLBAR_CLOSE_PERCENTAGE), 0).setDuration(350)
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            frame_layout.layoutParams.height = value
            frame_layout.requestLayout()
        }
        val set = AnimatorSet()
        set.play(animator)
        set.interpolator = AccelerateDecelerateInterpolator()
        set.start()
    }

    private fun percentHeightToPx(percent: Int): Int {
        if (percent < 0 || percent > 100) throw IllegalArgumentException()
        val height = servicesWrapper.height
        return ((percent.toFloat() / 100.0) * height).toInt()
    }

    private fun setToolbarFragment(fragment: Fragment?) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, (fragment)!!)
        fragmentTransaction.commit()
    }

    private fun fragmentsInit() {
        loggerFragment = LoggerFragment().setCallback(object : ToolbarCallback {
            override fun close() {
                closeToolbar()
                btToolbarOpened = !btToolbarOpened
            }

            override fun submit(filterDeviceParams: FilterDeviceParams?, close: Boolean) {}
        })
        loggerFragment.adapter = LogAdapter(Constants.LOGS, applicationContext)
        connectionsFragment = ConnectionsFragment().setCallback(object : ToolbarCallback {
            override fun close() {
                closeToolbar()
                btToolbarOpened = !btToolbarOpened
            }

            override fun submit(filterDeviceParams: FilterDeviceParams?, close: Boolean) {}
        })
        connectionsAdapter = ConnectionsAdapter(ConnectedGatts.list, applicationContext)
        connectionsFragment.adapter = connectionsAdapter
        connectionsFragment.adapter?.setServicesConnectionsCallback(this)
        tv_connections.text = getString(R.string.n_Connections, ConnectedGatts.size())
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showMessage(resources.getString(R.string.permissions_granted_successfully))
            } else {
                showMessage(R.string.permissions_not_granted)
            }
        }
    }

    override fun onDisconnectClicked(deviceInfo: BluetoothDeviceInfo?) {
        val currentDeviceAddress = bluetoothGatt?.device?.address
        bluetoothService?.disconnectGatt(deviceInfo?.address!!)

        updateCountOfConnectedDevices()
        connectionsFragment.adapter?.notifyDataSetChanged()
        if ((currentDeviceAddress == deviceInfo?.address)) {
            if (ConnectedGatts.isEmpty()) finish() else {
                val device = ConnectedGatts.list[0].device
                changeDevice(device.address)
            }
        }
    }

    override fun onDeviceClicked(device: BluetoothDevice) {
        boolOTAbegin = false
        changeDevice(device.address)
    }

    private fun initDevice(deviceAddress: String) {
        bluetoothBinding = object : BluetoothService.Binding(this) {
            override fun onBound(service: BluetoothService?) {
                serviceHasBeenSet = true
                this@DeviceServicesActivity.bluetoothService = service
                service?.registerGattCallback(gattCallback)
                if (!service?.isGattConnected(deviceAddress)!!) {
                    showMessage(R.string.toast_debug_connection_failed)
                    disconnectGatt(bluetoothGatt)
                } else {
                    bluetoothGatt = service.getConnectedGatt(deviceAddress)
                    if (bluetoothGatt == null) {
                        showMessage(R.string.toast_debug_connection_failed)
                        finish()
                    } else {
                        service.registerGattCallback(true, gattCallback)

                        if (bluetoothGatt?.services != null && bluetoothGatt?.services?.isNotEmpty()!!) {
                            onGattFetched()
                        } else {
                            showCharacteristicLoadingAnimation()
                            bluetoothGatt?.discoverServices()
                        }
                    }
                }
            }
        }
        handler.postDelayed({ runOnUiThread { bluetoothBinding?.bind() } }, UI_CREATION_DELAY.toLong())
    }

    private fun displayBondState(device: BluetoothDevice?) {
        val isBonded = device?.bondState == BluetoothDevice.BOND_BONDED

        bondMenuItem?.isVisible = true
        if (isBonded) {
            bondMenuItem?.title = getString(R.string.delete_bond)
            tv_bond_state.text = getString(R.string.bonded)
        } else {
            bondMenuItem?.title = getString(R.string.create_bond)
            tv_bond_state.text = getString(R.string.not_bonded)
        }
    }

    private fun changeDevice(address: String) {
        remoteServicesFragment.clear()
        loggerFragment.adapter?.logByDeviceAddress(address)
        initDevice(address)
    }

    fun updateCountOfConnectedDevices() {
        runOnUiThread {
            tv_connections.text = resources.getString(R.string.n_Connections, ConnectedGatts.size())
            connectionsFragment.adapter?.notifyDataSetChanged()
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                FILE_CHOOSER_REQUEST_CODE -> {
                    val type = currentOtaFileType
                    val uri = data?.data
                    val filename: String?

                    filename = try {
                        getFileName(uri)
                    } catch (e: Exception) {
                        ""
                    }

                    if (!hasOtaFileCorrectExtension(filename)) {
                        showMessage(resources.getString(R.string.incorrect_file))
                        return
                    }

                    if ((type == OtaFileType.APPLICATION)) {
                        prepareOtaFile(uri, OtaFileType.APPLICATION, filename)
                    } else {
                        prepareOtaFile(uri, OtaFileType.APPLOADER, filename)
                    }
                }
            }
        }

        if (areFullOTAFilesCorrect() && doubleStepUpload) {
            OTA_OK?.isClickable = true
            OTA_OK?.setBackgroundColor(ContextCompat.getColor(this@DeviceServicesActivity, R.color.silabs_red))
        } else if (arePartialOTAFilesCorrect() && !doubleStepUpload) {
            OTA_OK?.isClickable = true
            OTA_OK?.setBackgroundColor(ContextCompat.getColor(this@DeviceServicesActivity, R.color.silabs_red))
        } else {
            OTA_OK?.isClickable = false
            OTA_OK?.setBackgroundColor(ContextCompat.getColor(this@DeviceServicesActivity, R.color.silabs_button_inactive))
        }
    }

    private fun areFullOTAFilesCorrect(): Boolean {
        return appFileButton?.text != getString(R.string.select_application_gbl_file) && appLoaderFileButton?.text != getString(R.string.Select_Apploader_gbl_file)
    }

    private fun arePartialOTAFilesCorrect(): Boolean {
        return appFileButton?.text != getString(R.string.select_application_gbl_file)
    }

    private fun getFileName(uri: Uri?): String? {
        var result: String? = null
        if ((uri?.scheme == "content")) {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor.use { c ->
                if (c != null && c.moveToFirst()) {
                    result = c.getString(c.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri?.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result
    }

    private fun hasOtaFileCorrectExtension(filename: String?): Boolean {
        return filename?.toUpperCase(Locale.getDefault())?.contains(".GBL")!!
    }

    private fun prepareOtaFile(uri: Uri?, type: OtaFileType, filename: String?) {
        try {
            val inStream = contentResolver.openInputStream(uri!!)
            if (inStream == null) {
                showMessage(resources.getString(R.string.problem_while_preparing_the_file))
                return
            }
            val file = File(cacheDir, filename)
            val output: OutputStream = FileOutputStream(file)
            val buffer = ByteArray(4 * 1024)
            var read: Int
            while ((inStream.read(buffer).also { read = it }) != -1) {
                output.write(buffer, 0, read)
            }
            if ((type == OtaFileType.APPLICATION)) {
                appPath = file.absolutePath
                appFileButton?.text = filename
            } else {
                stackPath = file.absolutePath
                appLoaderFileButton?.text = filename
            }
            output.flush()
        } catch (e: IOException) {
            e.printStackTrace()
            showMessage(resources.getString(R.string.incorrect_file))
        }
    }

    companion object {
        private const val ABOUT_DIALOG_HTML_ASSET_FILE_PATH = "file:///android_asset/about.html"
        private const val UI_CREATION_DELAY = 0
        private const val GATT_FETCH_ON_SERVICE_DISCOVERED_DELAY = 875
        private const val WRITE_EXTERNAL_STORAGE_REQUEST_PERMISSION = 300
        private const val FILE_CHOOSER_REQUEST_CODE = 9999
        private const val TOOLBAR_OPEN_PERCENTAGE = 95
        private const val TOOLBAR_CLOSE_PERCENTAGE = 95
        private const val RECONNECTION_RETRIES = 3

        fun startActivity(context: Context, address: String) {
            val intent = Intent(context, DeviceServicesActivity::class.java)
            intent.putExtra("DEVICE_SELECTED_ADDRESS", address)
            startActivity(context, intent, null)
        }
    }
}
