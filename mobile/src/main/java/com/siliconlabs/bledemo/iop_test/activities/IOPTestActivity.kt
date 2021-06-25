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
package com.siliconlabs.bledemo.iop_test.activities

import android.app.Dialog
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.os.*
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.bluetooth.ble.TimeoutGattCallback
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import com.siliconlabs.bledemo.iop_test.fragments.IOPTestFragment
import com.siliconlabs.bledemo.iop_test.fragments.IOPTestFragment.Companion.newInstance
import com.siliconlabs.bledemo.iop_test.models.ChildrenItemTestInfo
import com.siliconlabs.bledemo.iop_test.models.Common
import com.siliconlabs.bledemo.iop_test.models.Common.Companion.isSetProperty
import com.siliconlabs.bledemo.iop_test.models.CommonUUID
import com.siliconlabs.bledemo.iop_test.models.IOPTest.Companion.createDataTest
import com.siliconlabs.bledemo.iop_test.models.IOPTest.Companion.getItemTestCaseInfo
import com.siliconlabs.bledemo.iop_test.models.IOPTest.Companion.getListItemChildrenTest
import com.siliconlabs.bledemo.iop_test.models.IOPTest.Companion.getSiliconLabsTestInfo
import com.siliconlabs.bledemo.iop_test.models.ItemTestCaseInfo
import com.siliconlabs.bledemo.iop_test.utils.ErrorCodes
import com.siliconlabs.bledemo.utils.BLEUtils.Notifications
import com.siliconlabs.bledemo.utils.BLEUtils.setNotificationForCharacteristic
import com.siliconlabs.bledemo.utils.Converters
import kotlinx.android.synthetic.main.actionbar.*
import kotlinx.android.synthetic.main.activity_iop_test.*
import java.io.*
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class IOPTestActivity : AppCompatActivity() {
    private var reconnectTimer: Timer? = Timer()
    private var handler: Handler? = null

    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private var isScanning = false
    private var isTestRunning = false
    private var isConnected = false
    private var isTestFinished = false
    private var areParameters = false
    private var readScannerStartTime = true

    private var mStartTimeScanner: Long = 0
    private var mStartTimeConnection: Long = 0
    private var mStartTimeDiscover: Long = 0
    private var mEndTimeDiscover: Long = 0

    private var mIndexStartChildrenTest = -1
    private var mIndexRunning = -1
    private var countReTest = 0
    private var iopPhase3IndexStartChildrenTest = -1

    private var mBluetoothGattServiceParameters: BluetoothGattService? = null
    private var characteristicIOPPhase3Control: BluetoothGattCharacteristic? = null
    private var characteristicIOPPhase3Throughput: BluetoothGattCharacteristic? = null
    private var characteristicIOPPhase3ClientSupportedFeatures: BluetoothGattCharacteristic? = null
    private var characteristicIOPPhase3DatabaseHash: BluetoothGattCharacteristic? = null
    private var characteristicIOPPhase3IOPTestCaching: BluetoothGattCharacteristic? = null
    private var characteristicIOPPhase3IOPTestServiceChangedIndication: BluetoothGattCharacteristic? = null
    private var characteristicIOPPhase3ServiceChanged: BluetoothGattCharacteristic? = null
    private var characteristicIOPPhase3DeviceName: BluetoothGattCharacteristic? = null

    private var mBluetoothDevice: BluetoothDevice? = null
    private var mBluetoothService: BluetoothService? = null
    private var mBluetoothBinding: BluetoothService.Binding? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mListener: Listener? = null
    private var mBluetoothEnableDialog: Dialog? = null

    private val listValuesPlatformBoard: ArrayList<String> = ArrayList()
    private val listValuesParameters = ArrayList<String>()
    private val mListCharacteristics: MutableList<BluetoothGattCharacteristic> = ArrayList()
    private val characteristicsPhase3Security: MutableList<BluetoothGattCharacteristic> = ArrayList()

    private var isDisabled = false
    private val mtuValue = 255
    private var mStartTimeThroughput: Long = 0
    private var mEndTimeThroughput: Long = 0
    private var mByteNumReceived = 0
    private var mPDULength = 0
    private var mByteSpeed = 0
    private var mEndThroughputNotification = false

    private var otaSetup: Dialog? = null
    private var otaProgress: Dialog? = null
    private var loadingDialog: Dialog? = null
    private var btnOtaStart: Button? = null
    private var sizename: TextView? = null
    private var loadingLog: TextView? = null
    private var loadingHeader: TextView? = null
    private var loadingimage: ProgressBar? = null

    /**
     * OTA Setup
     */
    private val delayNoResponse = 1

    private var reliable = true
    private var boolOTAbegin = false
    private var discoverTimeout = true
    private var ota_mode = false
    private var otaProcess = false
    private var disconnectGatt = false
    private var homekit = false
    private var otaMode = false
    private var otafile: ByteArray? = null
    private var otaFilename: String = ""
    private var uploadimage: ProgressBar? = null

    private var mtu = 247
    private var mtuDivisible = 0
    private var isServiceChangedIndication = 1
    private var isConnecting = false

    private val kBits = "%.2fkbit/s"

    private var kitDescriptor: BluetoothGattDescriptor? = null
    private var pack = 0
    private var otaTime: Long = 0
    private var progressBar: ProgressBar? = null
    private var chrono: Chronometer? = null
    private var dataRate: TextView? = null
    private var dataSize: TextView? = null
    private var filename: TextView? = null
    private var steps: TextView? = null

    private val ota1DeviceName = "IOP Test Update"
    private val ota2DeviceName = "IOP Test"
    private var otaDeviceName: String? = null
    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null
    private var iopPhase3DatabaseHash: String? = null
    private var testCaseCount = 0


    private val mBondStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
            val prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1)
            val msg = "Bond state change: state " + printBondState(state) + ", previous state " + printBondState(prevState)
            Log.d(TAG, msg)
            if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                handler?.postDelayed({
                    if (getSiliconLabsTestInfo().listItemTest[POSITION_TEST_IOP3_SECURITY].getStatusTest() == Common.IOP3_TC_STATUS_PROCESSING
                          /*  || getSiliconLabsTestInfo().listItemTest[POSITION_TEST_IOP3_CACHING]
                                    .getStatusTest() == Common.IOP3_TC_STATUS_PROCESSING*/) {
                        if (mBluetoothGatt != null) {
                            mListCharacteristics.clear()
                            characteristicsPhase3Security.clear()
                            Log.d(TAG, "discovering services with 1600ms delay")
                            Log.d(TAG, "isConnected: $isConnected")
                            val result = mBluetoothGatt!!.discoverServices()
                            if (!result) {
                                Log.e(TAG, "discoverServices failed to start")
                            }
                        } else {
                            retryIOP3Failed(mIndexRunning, ++countReTest / 2)
                        }
                    }
                }, 1600)
            } else if (state == BluetoothDevice.BOND_BONDING) {
                Toast.makeText(this@IOPTestActivity, R.string.iop_test_toast_bonding, Toast.LENGTH_LONG).show()
            }
        }

        private fun printBondState(state: Int): String {
            return when (state) {
                BluetoothDevice.BOND_NONE -> "BOND_NONE"
                BluetoothDevice.BOND_BONDING -> "BOND_BONDING"
                BluetoothDevice.BOND_BONDED -> "BOND_BONDED"
                else -> state.toString()
            }
        }
    }
    private val mPairRequestReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val variant = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, -1)
            val msg = "Pairing request, variant: " + printVariant(variant)
            Log.d(TAG, msg)
        }

        private fun printVariant(variant: Int): String {
            if (variant == BluetoothDevice.PAIRING_VARIANT_PIN) {
                Toast.makeText(this@IOPTestActivity, R.string.iop_test_toast_press_passkey, Toast.LENGTH_LONG).show()
                return "PAIRING_VARIANT_PIN"
            }
            return variant.toString()
        }
    }
    private val bluetoothAdapterStateChangeListener: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                Log.d(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED mIndexRunning: $mIndexRunning,state: $state")
                when (state) {
                    BluetoothAdapter.STATE_OFF -> {
                        disconnectGatt(mBluetoothGatt)
                        handler?.removeCallbacksAndMessages(null)
                        finish()
                    }
                }
            }
        }
    }

    /**
     * Update data test failed by item
     */
    private fun updateDataTestFailed(index: Int) {
        Log.d(TAG, "updateDataTestFailed $index")
        when (index) {
            POSITION_TEST_SCANNER -> {
                scanLeDevice(false)
                getSiliconLabsTestInfo().listItemTest[index].setStatusTest(Common.IOP3_TC_STATUS_FAILED)
            }
            POSITION_TEST_CONNECTION, POSITION_TEST_DISCOVER_SERVICE, POSITION_TEST_SERVICE -> disconnectGatt(mBluetoothGatt)
            else -> {
            }
        }
        for (i in index until getSiliconLabsTestInfo().listItemTest.size) {
            getSiliconLabsTestInfo().listItemTest[i].setStatusTest(Common.IOP3_TC_STATUS_FAILED)
        }
        if (index >= POSITION_TEST_SCANNER) {
            isTestRunning = false
            isTestFinished = true
            if (isConnected) {
                isConnected = false
                mBluetoothGatt?.disconnect()
            }
        }
        runOnUiThread { updateUIFooter(isTestRunning) }
        mListener?.updateUi()
    }

    /**
     * Start test by item
     */
    private fun startItemTest(item: Int) {
        mListener?.scrollViewToPosition(item)

        mIndexRunning = item
        isTestRunning = true
        isTestFinished = false
        isConnecting = false
        Log.d(TAG, "startItemTest: setStatusTest PROCESSING, item: $item")
        getSiliconLabsTestInfo().listItemTest[item].setStatusTest(Common.IOP3_TC_STATUS_PROCESSING)
        handler?.postDelayed({
            Log.d(TAG, "startItemTest with 1000ms delay of test case")
            when (item) {
                POSITION_TEST_SCANNER -> {
                    Log.d(TAG, "scanLeDevice at POSITION_TEST_SCANNER")
                    otaDeviceName = getSiliconLabsTestInfo().fwName
                    scanLeDevice(true)
                }
                POSITION_TEST_CONNECTION -> connectToDevice(mBluetoothDevice)
                POSITION_TEST_DISCOVER_SERVICE -> mBluetoothGatt?.requestMtu(247)
                POSITION_TEST_SERVICE -> runChildrenTestCase(mIndexStartChildrenTest)
                POSITION_TEST_IOP3_THROUGHPUT -> {
                    Log.d(TAG, "POSITION_TEST_IOP3_THROUGHPUT")
                    iopPhase3RunTestCaseThroughput(1)
                }
                POSITION_TEST_IOP3_SECURITY -> {
                    Log.d(TAG, "POSITION_TEST_IOP3_SECURITY")
                    iopPhase3IndexStartChildrenTest = 0
                    iopPhase3RunTestCaseSecurity(iopPhase3IndexStartChildrenTest, 1)
                }
                /*
                POSITION_TEST_IOP3_CACHING -> {
                    isServiceChangedIndication = 1
                    iopPhase3RunTestCaseCaching(0)
                }
                */
                POSITION_TEST_IOP3_OTA_ACK -> {
                    iopPhase3IndexStartChildrenTest = 0
                    iopPhase3TestCaseOTA(iopPhase3IndexStartChildrenTest)
                }
                POSITION_TEST_IOP3_OTA_WITHOUT_ACK -> {
                    iopPhase3IndexStartChildrenTest = 1
                    iopPhase3TestCaseOTA(iopPhase3IndexStartChildrenTest)
                }
                else -> {
                }
            }
        }, 1000)

        showDetailInformationTest(testCaseCount++, false)
        mListener?.updateUi()
    }

    /**
     * Finish test by item
     */
    private fun finishItemTest(item: Int, itemTestCaseInfo: ItemTestCaseInfo) {
        Log.d(TAG, "finishItemTest, test item: $item")
        when (item) {
            POSITION_TEST_SCANNER -> {
                itemTestCaseInfo.timeStart = mStartTimeScanner
                itemTestCaseInfo.setTimeEnd(System.currentTimeMillis())
                Log.d(TAG, "finishItemTest: scanLeDevice startTime: " + itemTestCaseInfo.getTimeEnd())
                if (itemTestCaseInfo.getStatusTest() == Common.IOP3_TC_STATUS_FAILED) {
                    countReTest++
                    if (countReTest < 6) {
                        itemTestCaseInfo.setStatusTest(-1)
                        startItemTest(item)
                        return
                    }
                } else {
                    countReTest = 0
                }
            }
            POSITION_TEST_CONNECTION -> {
                itemTestCaseInfo.timeStart = mStartTimeConnection
                itemTestCaseInfo.setTimeEnd(System.currentTimeMillis())
                Log.d(TAG, "finishItemTest: POSITION_TEST_CONNECTION time " + (System.currentTimeMillis() - mStartTimeConnection))
                if (itemTestCaseInfo.getStatusTest() == Common.IOP3_TC_STATUS_FAILED) {
                    countReTest++
                    if (countReTest < 6) {
                        if (isConnected) {
                            isConnected = false
                            mBluetoothGatt?.disconnect()
                        }
                        itemTestCaseInfo.setStatusTest(Common.IOP3_TC_STATUS_PROCESSING)
                        handler?.postDelayed({ startItemTest(item) }, 3000)
                    } else {
                        countReTest = 0
                    }
                } else {
                    countReTest = 0
                }
            }
            POSITION_TEST_DISCOVER_SERVICE -> {
                itemTestCaseInfo.timeStart = mStartTimeDiscover
                itemTestCaseInfo.setTimeEnd(mEndTimeDiscover)
                Log.d(TAG, "finishItemTest: POSITION_TEST_DISCOVER_SERVICE " + (mEndTimeDiscover - mStartTimeDiscover) + "ms")
                countReTest = 0
            }
            POSITION_TEST_SERVICE -> {
                getItemTestCaseInfo(POSITION_TEST_SERVICE).checkStatusItemService()
                Log.d(TAG, "finishItemTest: POSITION_TEST_SERVICE")
                countReTest = 0
            }
            POSITION_TEST_IOP3_THROUGHPUT -> {
                val throughputAcceptable = (mPDULength) * 4 * 1000 * 65 / 1500
                itemTestCaseInfo.setThroughputBytePerSec(mByteSpeed, throughputAcceptable)
                Log.d(TAG, "finishItemTest: POSITION_TEST_IOP3_THROUGHPUT mByteSpeed $mByteSpeed throughputAcceptable $throughputAcceptable")
                countReTest = 0
            }
            POSITION_TEST_IOP3_SECURITY -> {
                getItemTestCaseInfo(POSITION_TEST_IOP3_SECURITY).checkStatusItemService()
                Log.d(TAG, "finishItemTest: POSITION_TEST_IOP3_SECURITY")
                countReTest = 0
                isTestRunning = false
                isTestFinished = true
            }
            /*
            POSITION_TEST_IOP3_CACHING -> {
                isTestRunning = false
                isTestFinished = true
            }
            */
            POSITION_TEST_IOP3_OTA_ACK, POSITION_TEST_IOP3_OTA_WITHOUT_ACK -> countReTest = 0
            else -> {
            }
        }
        if (item != POSITION_TEST_IOP3_SECURITY && countReTest == 0) {
            startItemTest(item + 1)
        }
        runOnUiThread { updateUIFooter(isTestRunning) }
        mListener?.updateUi()
    }

    private fun runnable(gatt: BluetoothGatt) {
        Thread(Runnable {
            getServicesInfo(gatt) //SHOW SERVICES
        }).start()
    }

    /**
     * Update data test after listening bluetooth gatt callback
     */
    private fun updateDataTest(characteristic: BluetoothGattCharacteristic?, type: Int, status: Int) {
        if (characteristic != null) {
            for (itemChildrenTest: ChildrenItemTestInfo in getListChildrenItemTestCase(POSITION_TEST_SERVICE)!!) {
                if (itemChildrenTest.characteristic?.uuid.toString() == characteristic.uuid.toString()) {
                    itemChildrenTest.statusRunTest = 1 // 0 running, 1 finish test
                    itemChildrenTest.valueMtu = mtu
                    if (itemChildrenTest.characteristic?.uuid.toString() == CommonUUID.Characteristic.NOTIFICATION_LENGTH_1.showUUID() || itemChildrenTest.characteristic?.uuid.toString() == CommonUUID.Characteristic.INDICATE_LENGTH_1.showUUID() || itemChildrenTest.characteristic?.uuid.toString() == CommonUUID.Characteristic.NOTIFICATION_LENGTH_MTU_3.showUUID() || itemChildrenTest.characteristic?.uuid.toString() == CommonUUID.Characteristic.INDICATE_LENGTH_MTU_3.showUUID()) {
                        itemChildrenTest.endTimeTest = System.currentTimeMillis()
                    }
                    when (type) {
                        0 -> itemChildrenTest.statusRead = status
                        1 -> itemChildrenTest.statusWrite = status
                        else -> {
                        }
                    }
                    itemChildrenTest.setDataAndCompareResult(characteristic)
                }
            }
            for (itemChildrenTest: ChildrenItemTestInfo in getListChildrenItemTestCase(POSITION_TEST_IOP3_SECURITY)!!) {
                if (itemChildrenTest.characteristic?.uuid.toString() == characteristic.uuid.toString()) {
                    itemChildrenTest.statusRunTest = 1 // 0 running, 1 finish test
                    when (type) {
                        0 -> itemChildrenTest.statusRead = status
                        1 -> itemChildrenTest.statusWrite = status
                        else -> {
                        }
                    }
                    itemChildrenTest.setDataAndCompareResult(characteristic)
                }
            }
            if (mIndexRunning == POSITION_TEST_IOP3_OTA_ACK
                    || mIndexRunning == POSITION_TEST_IOP3_OTA_WITHOUT_ACK) {
                if (!otaProcess) {
                    if (characteristicIOPPhase3DeviceName?.uuid.toString() == characteristic.uuid.toString()) {
                        val nameArray = characteristic.value
                        mDeviceName = String(nameArray, StandardCharsets.UTF_8)
                        Log.d(TAG, "connected to $mDeviceName")
                        if (mDeviceName?.replace(" ", "").equals(otaDeviceName?.replace(" ", ""), ignoreCase = true)) {
                            checkIOP3OTA(mIndexRunning, Common.IOP3_TC_STATUS_PASS)
                        } else {
                            checkIOP3OTA(mIndexRunning, Common.IOP3_TC_STATUS_FAILED)
                        }
                        finishItemTest(mIndexRunning, getSiliconLabsTestInfo().listItemTest[mIndexRunning])
                    }
                }
            } else if (characteristicIOPPhase3Control?.uuid.toString() == characteristic.uuid.toString()) {
                if (getSiliconLabsTestInfo().listItemTest[POSITION_TEST_IOP3_THROUGHPUT].getStatusTest() == Common.IOP3_TC_STATUS_PROCESSING) {
                    iopPhase3RunTestCaseThroughput(0)
                } /* else if (getSiliconLabsTestInfo().listItemTest[POSITION_TEST_IOP3_CACHING]
                                .getStatusTest() == Common.IOP3_TC_STATUS_PROCESSING) {
                    if (isServiceChangedIndication == 0) {
                        handler?.postDelayed({
                            val securityItem = getListChildrenItemTestCase(POSITION_TEST_IOP3_SECURITY)!![0]
                            readCharacteristic(securityItem.characteristic)
                        }, 2000)
                    }
                } */
            } else if (characteristicIOPPhase3Throughput?.uuid.toString() == characteristic.uuid.toString()) {
                mByteNumReceived += characteristic.value.size
                mPDULength = characteristic.value.size
            } else if (mIndexRunning == POSITION_TEST_IOP3_CACHING) {
                if (isServiceChangedIndication == 1) {
                    if (characteristicIOPPhase3IOPTestServiceChangedIndication != null) {
                        if (characteristicIOPPhase3IOPTestServiceChangedIndication?.uuid.toString() == characteristic.uuid.toString()) {
                            val itemChildrenTest = getListChildrenItemTestCase(POSITION_TEST_IOP3_CACHING)!![isServiceChangedIndication]
                            itemChildrenTest.statusRead = status
                            itemChildrenTest.statusRunTest = 1
                            itemChildrenTest.setDataAndCompareResult((itemChildrenTest.characteristic)!!)
                        }
                    }
                } else {
                    if (characteristicIOPPhase3DatabaseHash?.uuid.toString() == characteristic.uuid.toString()) {
                        if (iopPhase3DatabaseHash != null) {
                            val itemTestInfo = getListChildrenItemTestCase(POSITION_TEST_IOP3_CACHING)!![0]
                            if (!itemTestInfo.compareValueCharacteristic(iopPhase3DatabaseHash!!, Converters.getHexValue(characteristic.value))) {
                                Log.d(TAG, "iopPhase3DatabaseHash has changed")
                                mListCharacteristics.clear()
                                characteristicsPhase3Security.clear()
                                refreshServices()
                            }
                        } else {
                            setNotificationForCharacteristic(characteristicIOPPhase3ServiceChanged, Notifications.DISABLED)
                        }
                        iopPhase3DatabaseHash = Converters.getHexValue(characteristic.value)
                        Log.d(TAG, "characteristicIOPPhase3DatabaseHash: $iopPhase3DatabaseHash")
                    } else if (characteristicIOPPhase3ClientSupportedFeatures?.uuid.toString() == characteristic.uuid.toString()) {
                        readCharacteristic(characteristicIOPPhase3DatabaseHash)
                    } else if (characteristicIOPPhase3IOPTestCaching != null) {
                        if (characteristicIOPPhase3IOPTestCaching?.uuid.toString() == characteristic.uuid.toString()) {
                            val itemChildrenTest = getListChildrenItemTestCase(POSITION_TEST_IOP3_CACHING)!![isServiceChangedIndication]
                            itemChildrenTest.statusRead = status
                            itemChildrenTest.statusRunTest = 1
                            itemChildrenTest.setDataAndCompareResult((itemChildrenTest.characteristic)!!)
                            handler?.removeCallbacks(iopCachingRunnable)
                            if (itemChildrenTest.statusChildrenTest) {
                                getItemTestCaseInfo(POSITION_TEST_IOP3_CACHING).setStatusTest(Common.IOP3_TC_STATUS_PASS)
                            }
                            finishItemTest(POSITION_TEST_IOP3_CACHING, getSiliconLabsTestInfo().listItemTest[POSITION_TEST_IOP3_CACHING])
                        }
                    }
                }
            }
        }
    }

    /**
     * DISCONNECTS AND CONNECTS WITH THE SELECTED DELAY
     */
    fun reconnect(delaytoconnect: Long) {
        mBluetoothDevice = mBluetoothGatt?.device
        if (mBluetoothService?.isGattConnected()!!) {
            mBluetoothService?.clearConnectedGatt()
            mBluetoothService?.clearCache()
        }

        mBluetoothGatt?.disconnect()
        reconnectTimer?.schedule(object : TimerTask() {
            override fun run() {
                mBluetoothGatt?.close()
                mBluetoothBinding?.unbind()
            }
        }, 400)

        reconnectTimer?.schedule(object : TimerTask() {
            override fun run() {
                Log.d(TAG, "Attempting connection...")
                mBluetoothBinding = object : BluetoothService.Binding(applicationContext) {
                    override fun onBound(service: BluetoothService?) {
                        service?.isNotificationEnabled = false
                        mBluetoothGatt = mBluetoothDevice?.connectGatt(applicationContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                    }
                }
                mBluetoothBinding?.bind()
            }
        }, delaytoconnect)
    }

    /**
     * DISCONNECT GATT GENTLY AND CLEAN GLOBAL VARIABLES
     */
    private fun disconnectGatt(gatt: BluetoothGatt?) {
        val disconnectTimer = Timer()
        boolOTAbegin = false
        otaProcess = false
        disconnectGatt = true
        if (gatt != null && gatt.device != null) {
            val btGatt: BluetoothGatt = gatt
            disconnectTimer.schedule(object : TimerTask() {
                override fun run() {
                    //Getting bluetoothDevice to FetchUUID
                    btGatt.device?.let {
                        mBluetoothDevice = btGatt.device
                    }
                    btGatt.disconnect()
                    mBluetoothService?.clearConnectedGatt()
                }
            }, 200)
            disconnectTimer.schedule(object : TimerTask() {
                override fun run() {
                    mBluetoothDevice?.fetchUuidsWithSdp()
                }
            }, 300)
        } else {
            finish()
        }
    }

    /**
     * CLEANS USER INTERFACE AND FINISH ACTIVITY
     */
    fun exit(gatt: BluetoothGatt?) {
        gatt?.close()
        mBluetoothService?.connectedGatt?.close()

        mBluetoothService?.clearCache()
        mBluetoothBinding?.unbind()
        disconnectGatt = false

        handler?.postDelayed({
            if (loadingDialog != null && loadingDialog?.isShowing!!) {
                loadingDialog?.dismiss()
            }
            if (otaProgress != null && otaProgress?.isShowing!!) {
                otaProgress?.dismiss()
            }
            if (otaSetup != null && otaSetup?.isShowing!!) {
                otaSetup?.dismiss()
            }
        }, 1000)
    }

    private fun connectToDevice(bluetoothDevice: BluetoothDevice?) {
        Log.d(TAG, "connectToDevice() called with: bluetoothDevice = $bluetoothDevice")
        mStartTimeConnection = System.currentTimeMillis()

        Log.d(TAG, "connectToDevice(), postDelayed connectionRunnable")
        handler?.postDelayed(connectionRunnable, CONNECTION_PERIOD)
        mBluetoothBinding = object : BluetoothService.Binding(applicationContext) {
            override fun onBound(service: BluetoothService?) {
                mBluetoothService = service
                service?.isNotificationEnabled = false
                service?.connectGatt(bluetoothDevice!!, false, gattCallback)
                mBluetoothGatt = service?.connectedGatt!!
            }
        }
        mBluetoothBinding?.bind()
    }

    private fun saveLogFile() {
        Executors.newSingleThreadExecutor().execute {
            val file = saveDataTestToFile(getPathOfLogFile())
            handler?.post {
                shareLogDataTestByEmail(file.absolutePath)
            }
        }
    }

    private fun startTest() {
        isTestRunning = true
        isTestFinished = false

        resetFunctionTest()
        startItemTest(POSITION_TEST_SCANNER)

        updateUIFooter(isTestRunning)
    }

    /**
     * Update Ui footer
     */
    private fun updateUIFooter(isRunning: Boolean) {
        if (!isRunning) {
            btn_start_and_stop_test.text = getString(R.string.button_run_test)
            btn_start_and_stop_test.isEnabled = true
            btn_start_and_stop_test.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.silabs_blue))
            if (isTestFinished) {
                btn_share_test.visibility = View.VISIBLE
            }
        } else {
            btn_start_and_stop_test.text = getString(R.string.button_waiting)
            btn_start_and_stop_test.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.silabs_inactive_light))
            btn_start_and_stop_test.isEnabled = false
            btn_share_test.visibility = View.GONE
        }
    }

    /**
     * Show information device test and progress test item
     */
    private fun showDetailInformationTest(item: Int, isInformation: Boolean) {
        val siliconlabsTestInfo = getSiliconLabsTestInfo()
        runOnUiThread {
            if (isInformation || item == POSITION_TEST_CONNECTION) {
                tv_fw_name.text = getString(R.string.iop_test_label_fw_name, siliconlabsTestInfo.fwName)
                tv_device_name.text = getString(R.string.iop_test_label_device_name, siliconlabsTestInfo.phoneName)
            }
            val total = siliconlabsTestInfo.totalTestCase.toString()
            if (item == POSITION_TEST_SERVICE) {
                testCaseCount = item + mIndexStartChildrenTest + 1
            }
            Log.d(TAG, "The number of test case $testCaseCount")
            tv_progress.text = getString(R.string.iop_test_label_progress_count_test, testCaseCount.toString(), total)
        }
    }

    /**
     * Reset list item test and start over
     */
    private fun resetFunctionTest() {
        reliable = true
        testCaseCount = 0
        mIndexRunning = -1
        readScannerStartTime = true
        mStartTimeScanner = 0
        mStartTimeConnection = 0
        mByteNumReceived = 0
        listValuesPlatformBoard.clear()
        if (isConnected) {
            isConnected = false
            mBluetoothGatt?.disconnect()
        }
        resetData()
    }

    private fun resetData() {
        createDataTest(getSiliconLabsTestInfo().fwName)
        mListener?.updateUi()
    }

    override fun onDestroy() {
        Log.d("onDestroy", "called")
        if (isScanning) {
            scanLeDevice(false)
            Log.d("onDestroy", "scanLeDevice(false)")
        }

        handler?.removeCallbacksAndMessages(null)
        reconnectTimer = null
        handler = null

        mBluetoothService?.clearConnectedGatt()
        unregisterBroadcastReceivers()

        super.onDestroy()
    }

    private fun unregisterBroadcastReceivers() {
        unregisterReceiver(mBondStateReceiver)
        unregisterReceiver(mPairRequestReceiver)
        unregisterReceiver(bluetoothAdapterStateChangeListener)
    }

    fun setListener(listener: Listener) {
        mListener = listener
    }

    override fun onBackPressed() {
        if (isTestRunning) {
            showDialogConfirmStopTest()
        } else {
            disconnectGatt(mBluetoothGatt)
            super.onBackPressed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iop_test)
        setSupportActionBar(toolbar)
        findViewById<View>(R.id.iv_go_back).setOnClickListener { onBackPressed() }
        checkIfBluetoothIsSupported()

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        handler = Handler(Looper.getMainLooper())

        addChildrenView()
        showDetailInformationTest(POSITION_TEST_SCANNER, true)
        checkBluetoothExtendedSettings()
        registerBroadcastReceivers()
        handleClickEvents()
    }

    private fun checkIfBluetoothIsSupported() {
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            Toast.makeText(this, R.string.iop_test_toast_bluetooth_not_supported, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun checkBluetoothExtendedSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (bluetoothAdapter.isLe2MPhySupported) {
                Log.d(TAG, "2M PHY supported!")
            }
            if (bluetoothAdapter.isLeExtendedAdvertisingSupported) {
                Log.d(TAG, "LE Extended Advertising supported!")
            }
            val maxDataLength = bluetoothAdapter.leMaximumAdvertisingDataLength
            Log.d(TAG, "maxDataLength $maxDataLength")
        }
    }

    private fun registerBroadcastReceivers() {
        registerReceiver(mBondStateReceiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
        registerReceiver(mPairRequestReceiver, IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST))
        registerReceiver(bluetoothAdapterStateChangeListener, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    private fun handleClickEvents() {
        btn_start_and_stop_test.setOnClickListener {
            startTest()
        }

        btn_share_test.setOnClickListener {
            saveLogFile()
        }
    }

    /**
     * Scan Device
     */
    private fun scanLeDevice(enable: Boolean) {
        if (enable) {
            if (!isScanning) {
                isScanning = true
                handler?.postDelayed(scanRunnable, SCAN_PERIOD)
                readScannerStartTime = true
                Log.d(TAG, "Scanner Fw name: " + getSiliconLabsTestInfo().fwName)
                if (bluetoothAdapter.isEnabled) {
                    bluetoothLeScanner.startScan(scanCallback)
                }
            } else {
                Log.d(TAG, "Scan has already started")
            }
        } else {
            stopScanner()
            handler?.removeCallbacks(scanRunnable)
        }
    }

    private val scanRunnable: Runnable = Runnable {
        if (isScanning) {
            stopScanner()
            Log.d(TAG, "scanRunnable updateDataTestFailed")
            updateDataTestFailed(mIndexRunning)
        }
    }
    private val connectionRunnable: Runnable = Runnable {
        if (!isConnected) {
            Log.d(TAG, "updateDataTestFailed connectionRunnable")
            if (!otaProcess) {
                countReTest++
                retryIOP3Failed(mIndexRunning, countReTest)
            }
        }
    }

    /**
     * Stop Scan device
     */
    private fun stopScanner() {
        isScanning = false
        if (bluetoothAdapter.isEnabled) {
            bluetoothLeScanner.stopScan(scanCallback)
        }
    }

    /**
     * Dialog Confirm stop test
     */
    private fun showDialogConfirmStopTest() {
        AlertDialog.Builder(this)
                .setMessage(getString(R.string.iop_test_message_stop_test))
                .setPositiveButton(getString(R.string.button_ok)) { dialog, _ ->
                    dialog.dismiss()
                    disconnectGatt(mBluetoothGatt)
                    finish()
                }.setNegativeButton(getString(R.string.button_cancel)) { dialog, _ ->
                    dialog.dismiss()
                }.show()
    }

    /**
     * Add Fragment have not yet list item test.
     */
    private fun addChildrenView() {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, newInstance(), IOPTestFragment::class.java.name)
                .disallowAddToBackStack()
                .commit()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BLUETOOTH_SETTINGS_REQUEST_CODE) {
            if (!bluetoothAdapter.isEnabled && mBluetoothEnableDialog != null) {
                mBluetoothEnableDialog?.show()
            }
        }
    }

    /**
     * Enable indicate by characteristics
     */
    private fun setIndicationProperty(characteristic: BluetoothGattCharacteristic?, indicate: Notifications) {
        setNotificationForCharacteristic(mBluetoothGatt!!, characteristic, CLIENT_CHARACTERISTIC_CONFIG_UUID, indicate)
    }

    /**
     * Enable notification by characteristics
     */
    private fun setNotificationForCharacteristic(characteristic: BluetoothGattCharacteristic?, notifications: Notifications) {
        setNotificationForCharacteristic(mBluetoothGatt!!, characteristic, CLIENT_CHARACTERISTIC_CONFIG_UUID, notifications)
    }

    /**
     * Write down the value in characteristics
     */
    private fun writeValueToCharacteristic(characteristic: BluetoothGattCharacteristic?, value: String?, byteValues: ByteArray?) {
        var newValue: ByteArray? = null
        if (byteValues != null) {
            newValue = byteValues
        } else if (!TextUtils.isEmpty(value)) {
            newValue = Converters.hexToByteArray(value!!)
        }
        if (isSetProperty(Common.PropertyType.WRITE, characteristic!!.properties)) {
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        } else if (isSetProperty(Common.PropertyType.WRITE_NO_RESPONSE, characteristic.properties)) {
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }
        characteristic.value = newValue
        Log.d(TAG, "writeValueToCharacteristic " + characteristic.uuid.toString())
        if (!mBluetoothGatt!!.writeCharacteristic(characteristic)) {
            Log.e(TAG, String.format("ERROR: writeCharacteristic failed for characteristic: %s", characteristic.uuid))
        }
    }

    /**
     * Create values of write characteristics
     */
    private fun writeHexForMaxLengthByte(value: String, length: Int): String {
        return StringBuilder().apply {
            for (i in 0 until length) {
                append(value)
            }
        }.toString()
    }

    /**
     * Read values by characteristic
     */
    private fun readCharacteristic(characteristic: BluetoothGattCharacteristic?) {
        mBluetoothGatt?.readCharacteristic(characteristic)
    }

    /**
     * Read all service
     */
    private fun getServicesInfo(gatt: BluetoothGatt) {
        mEndTimeDiscover = System.currentTimeMillis()
        val gattServices = gatt.services
        Log.d(TAG, "getServicesInfo(), Services count: " + gattServices.size)
        var count = 0
        for (gattService: BluetoothGattService in gattServices) {
            val serviceUUID = gattService.uuid.toString()
            Log.d(TAG, "getServicesInfo(), add Characteristic of ServiceUUID $serviceUUID")
            if (serviceUUID == CommonUUID.Service.UUID_GENERIC_ATTRIBUTE.toString()) {
                val gattCharacteristics = gattService.characteristics
                for (item: BluetoothGattCharacteristic in gattCharacteristics) {
                    Log.d(TAG, "mBluetoothGattServiceGenericAttribute " + item.uuid.toString())
                    when {
                        CommonUUID.Characteristic.IOP_TEST_PHASE3_SERVICE_CHANGED.showUUID() == item.uuid.toString() -> {
                            characteristicIOPPhase3ServiceChanged = item
                        }
                        CommonUUID.Characteristic.IOP_TEST_PHASE3_CLIENT_SUPPORT_FEATURES.showUUID() == item.uuid.toString() -> {
                            characteristicIOPPhase3ClientSupportedFeatures = item
                        }
                        CommonUUID.Characteristic.IOP_TEST_PHASE3_DATABASE_HASH.showUUID() == item.uuid.toString() -> {
                            characteristicIOPPhase3DatabaseHash = item
                        }
                    }
                }
            } else if (serviceUUID == CommonUUID.Service.UUID_GENERIC_ACCESS.toString()) {
                val gattCharacteristics = gattService.characteristics
                for (item: BluetoothGattCharacteristic in gattCharacteristics) {
                    if (CommonUUID.Characteristic.IOP_DEVICE_NAME.showUUID() == item.uuid.toString()) {
                        Log.d(TAG, "characteristicIOPPhase3DeviceName " + item.uuid.toString())
                        characteristicIOPPhase3DeviceName = item
                    }
                }
            } else if (serviceUUID == CommonUUID.Service.UUID_CHARACTERISTICS_PARAMETERS_SERVICE.toString()) {
                count++
                mBluetoothGattServiceParameters = gattService
            } else if (CommonUUID.Service.UUID_PROPERTIES_SERVICE.toString() == serviceUUID || CommonUUID.Service.UUID_CHARACTERISTICS_SERVICE.toString() == serviceUUID) {
                count++
                val gattCharacteristics = gattService.characteristics
                for (item: BluetoothGattCharacteristic in gattCharacteristics) {
                    mListCharacteristics.add(item)
                }
            } else if (CommonUUID.Service.UUID_PHASE3_SERVICE.toString() == serviceUUID) {
                Log.d(TAG, "mBluetoothGattServicePhase3 " + gattService.uuid.toString())
                val gattCharacteristicsIOPPhase3 = gattService.characteristics
                for (item: BluetoothGattCharacteristic in gattCharacteristicsIOPPhase3) {
                    val charUUID = item.uuid.toString()
                    when {
                        charUUID == CommonUUID.Characteristic.IOP_TEST_PHASE3_CONTROL.showUUID() -> {
                            characteristicIOPPhase3Control = item
                        }
                        charUUID == CommonUUID.Characteristic.IOP_TEST_THROUGHPUT.showUUID() -> {
                            characteristicIOPPhase3Throughput = item
                        }
                        charUUID == CommonUUID.Characteristic.IOP_TEST_GATT_CATCHING.showUUID() -> {
                            characteristicIOPPhase3IOPTestCaching = item
                            Log.d(TAG, "characteristicIOPPhase3IOPTestCaching " + characteristicIOPPhase3IOPTestCaching?.uuid.toString())
                        }
                        charUUID == CommonUUID.Characteristic.IOP_TEST_SERVICE_CHANGED_INDICATION.showUUID() -> {
                            characteristicIOPPhase3IOPTestServiceChangedIndication = item
                            Log.d(TAG, "characteristicIOPPhase3IOPTestServiceChangedIndication " + characteristicIOPPhase3IOPTestServiceChangedIndication?.uuid.toString())
                        }
                        charUUID == CommonUUID.Characteristic.IOP_TEST_SECURITY_PAIRING.showUUID() || charUUID == CommonUUID.Characteristic.IOP_TEST_SECURITY_AUTHENTICATION.showUUID() || charUUID == CommonUUID.Characteristic.IOP_TEST_SECURITY_BONDING.showUUID() -> {
                            Log.d(TAG, "characteristicsPhase3Security $charUUID")
                            characteristicsPhase3Security.add(item)
                        }
                    }
                }
            } else if (CommonUUID.Service.UUID_BLE_OTA.toString() == serviceUUID) {
                val gattCharacteristics = gattService.characteristics
                for (gattCharacteristic: BluetoothGattCharacteristic in gattCharacteristics) {
                    val characteristicUUID = gattCharacteristic.uuid.toString()
                    Log.i(TAG, "onServicesDiscovered Characteristic UUID " + characteristicUUID + " - Properties: " + gattCharacteristic.properties)
                    if (gattCharacteristic.uuid.toString() == ota_control.toString()) {
                        if (gattCharacteristics.contains(mBluetoothGatt?.getService(ota_service)?.getCharacteristic(ota_data))) {
                            if (!gattServices.contains(mBluetoothGatt?.getService(homekit_service))) {
                                Log.i(TAG, "onServicesDiscovered Device in DFU Mode")
                                mBluetoothGatt?.requestMtu(247)
                            } else {
                                Log.i(TAG, "onServicesDiscovered OTA_Control found")
                                val gattDescriptors = gattCharacteristic.descriptors
                                for (gattDescriptor: BluetoothGattDescriptor in gattDescriptors) {
                                    val descriptor = gattDescriptor.uuid.toString()
                                    if (gattDescriptor.uuid.toString() == homekit_descriptor.toString()) {
                                        kitDescriptor = gattDescriptor
                                        Log.i(TAG, "descriptor, UUID: $descriptor")
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
        for (i in getListChildrenItemTestCase(POSITION_TEST_SERVICE)!!.indices) {
            for (j in i until mListCharacteristics.size) {
                getListChildrenItemTestCase(POSITION_TEST_SERVICE)!![i].characteristic = mListCharacteristics[j]
                break
            }
        }
        for (i in getListChildrenItemTestCase(POSITION_TEST_IOP3_SECURITY)!!.indices) {
            for (j in i until characteristicsPhase3Security.size) {
                getListChildrenItemTestCase(POSITION_TEST_IOP3_SECURITY)!![i].characteristic = characteristicsPhase3Security[j]
                break
            }
        }
        if (mIndexRunning == POSITION_TEST_DISCOVER_SERVICE) {
            if (mBluetoothGattServiceParameters != null) {
                handler?.postDelayed({ getConnectionParameters(mBluetoothGattServiceParameters, 0) }, 5000)
            }
        } else if (mIndexRunning == POSITION_TEST_IOP3_OTA_WITHOUT_ACK || mIndexRunning == POSITION_TEST_IOP3_OTA_ACK) {
            if (!otaProcess) {
                handler?.postDelayed({
                    Log.d(TAG, "read device name")
                    readCharacteristic(characteristicIOPPhase3DeviceName)
                }, 5000)
            }
        } else if (mIndexRunning == POSITION_TEST_IOP3_THROUGHPUT) {
            mEndThroughputNotification = false
            finishItemTest(POSITION_TEST_IOP3_THROUGHPUT, getSiliconLabsTestInfo().listItemTest[POSITION_TEST_IOP3_THROUGHPUT])
        } else if (mIndexRunning == POSITION_TEST_IOP3_SECURITY) {
            iopPhase3RunTestCaseSecurity(iopPhase3IndexStartChildrenTest, 0)
        } else if (mIndexRunning == POSITION_TEST_IOP3_CACHING) {
            if (characteristicIOPPhase3IOPTestCaching != null) {
                getListChildrenItemTestCase(POSITION_TEST_IOP3_CACHING)!![0].characteristic = characteristicIOPPhase3IOPTestCaching
                readCharacteristic(characteristicIOPPhase3IOPTestCaching)
            } else if (characteristicIOPPhase3IOPTestServiceChangedIndication != null) {
                getListChildrenItemTestCase(POSITION_TEST_IOP3_CACHING)!![1].characteristic = characteristicIOPPhase3IOPTestServiceChangedIndication
                readCharacteristic(characteristicIOPPhase3IOPTestServiceChangedIndication)
            }
        }
    }

    /**
     * Get Parameters and platform information
     */
    private fun getConnectionParameters(gattService: BluetoothGattService?, index: Int) {
        areParameters = true
        readCharacteristic(gattService!!.characteristics[index])
        Log.d(TAG, "getConnectionParameters(), areParameters = true")
    }

    /**
     * Convert byte value to hex value
     */
    private fun convertValuesParameters(characteristic: BluetoothGattCharacteristic) {
        if (CommonUUID.Characteristic.IOP_TEST_VERSION.showUUID() == characteristic.uuid.toString()) {
            listValuesPlatformBoard.add(Converters.getDecimalValue(characteristic.value))
            getConnectionParameters(mBluetoothGattServiceParameters, 1)
        } else {
            val values = characteristic.value
            for (i in values.indices) {
                if (i == 0) {
                    listValuesPlatformBoard.add(Converters.getDecimalValue(values[i]))
                } else if (i % 2 == 0) {
                    listValuesParameters.add(Converters.getDecimalValue(Arrays.copyOfRange(values, i, i + 1)))
                }
            }
            areParameters = false
            getSiliconLabsTestInfo().setLstValuesParameters(listValuesParameters)
            getSiliconLabsTestInfo().setLstValuesPlatform(listValuesPlatformBoard)
            mIndexStartChildrenTest = 0
            finishItemTest(POSITION_TEST_DISCOVER_SERVICE, getSiliconLabsTestInfo().listItemTest[POSITION_TEST_DISCOVER_SERVICE])
            Log.d(TAG, "convertValuesParameters(), finishItemTest(POSITION_TEST_DISCOVER_SERVICE)")
        }
    }

    /**
     * Get list item children test by index test
     */
    private fun getListChildrenItemTestCase(index: Int): List<ChildrenItemTestInfo>? {
        return getListItemChildrenTest(index)
    }

    /**
     * Set time start and End item notification and indicate
     */
    private fun setTimeStart(gattCharacteristic: BluetoothGattCharacteristic?) {
        for (item: ChildrenItemTestInfo in getListChildrenItemTestCase(POSITION_TEST_SERVICE)!!) {
            if (item.characteristic?.uuid == gattCharacteristic!!.uuid) {
                item.startTimeTest = System.currentTimeMillis()
            }
        }
    }

    /**
     * Get Path of log file in ExternalStorage
     */
    private fun getPathOfLogFile(): String {
        val name = getSiliconLabsTestInfo().getValuesPlatform(1)
        val nameDevice = getSiliconLabsTestInfo().getIopBoard(name).icName.text
        val pathFile = getSiliconLabsTestInfo().phoneName
        pathFile.replace(" ", "")
        return pathFile + "_" + nameDevice + "_" + getDate() + ".txt"
    }

    /**
     * Save log data into File
     */
    private fun saveDataTestToFile(pathFileOfLog: String?): File {
        val path = getExternalFilesDir(null)?.absolutePath + File.separator + FOLDER_NAME
        val folder = File(path)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val file = File(folder, pathFileOfLog)
        if (file.exists()) {
            file.delete()
        }
        try {
            file.createNewFile()
            var fOut: FileOutputStream? = null
            try {
                fOut = FileOutputStream(file)
                var myOutWriter: OutputStreamWriter? = null
                try {
                    myOutWriter = OutputStreamWriter(fOut)
                    myOutWriter.write(getDataLog())
                } catch (e: Exception) {
                    Log.e(TAG, e.localizedMessage ?: "saveDataTestToFile(), OutputStreamWriter exception")
                } finally {
                    myOutWriter?.close()
                }
                fOut.flush()
            } catch (e: Exception) {
                Log.e(TAG, e.localizedMessage ?: "saveDataTestToFile(), FileOutputStream exception")
            } finally {
                fOut?.close()
            }
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
        return file
    }

    /**
     * Get Date currently
     */
    private fun getDate(): String {
        return SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault()).format(Date())
    }

    /**
     * Get log data by object SiliconlabsInformation
     */
    private fun getDataLog(): String {
        return getSiliconLabsTestInfo().toString()
    }

    private fun shareLogDataTestByEmail(fileLocation: String) {
        try {
            val uri = FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", File(fileLocation))
            val message = "Please check the attachment to get the log file."
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "[Silabs] Test log")
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Exception in shareLogDataTestByEmail(): $e")
        }
    }

    /**
     * Perform the test by item test case
     */
    private fun runChildrenTestCase(index: Int) {
        showDetailInformationTest(POSITION_TEST_SERVICE, false)

        val childrenItem = getListChildrenItemTestCase(POSITION_TEST_SERVICE)!![index]
        val uuids = CommonUUID.Characteristic.values()
        val cUuid = childrenItem.characteristic?.uuid.toString()
        if (TextUtils.isEmpty(cUuid)) {
            childrenItem.statusRunTest = 0 // 0 running, 1 finish test
            if (mIndexStartChildrenTest <= 17) {
                mIndexStartChildrenTest += 1
                runChildrenTestCase(mIndexStartChildrenTest)
            } else {
                return
            }
        }
        var matchChar = -1
        for (i in uuids.indices) {
            if (cUuid.equals(uuids[i].showUUID(), ignoreCase = true)) {
                matchChar = uuids[i].id
                Log.d(TAG, "Run TestCase [" + (matchChar + 3) + "] at characteristic " + uuids[i].showUUID())
                break
            }
        }
        when (matchChar) {
            CommonUUID.ID_READ_ONLY_LENGTH_1, CommonUUID.ID_READ_ONLY_LENGTH_255 -> readCharacteristic(childrenItem.characteristic)
            CommonUUID.ID_WRITE_ONLY_LENGTH_1, CommonUUID.ID_WRITE_WITHOUT_RESPONSE_LENGTH_1 -> writeValueToCharacteristic(childrenItem.characteristic, writeHexForMaxLengthByte(Common.WRITE_VALUE_0, Common.WRITE_LENGTH_1), null)
            CommonUUID.ID_WRITE_ONLY_LENGTH_255, CommonUUID.ID_WRITE_WITHOUT_RESPONSE_LENGTH_255 -> writeValueToCharacteristic(childrenItem.characteristic, writeHexForMaxLengthByte(Common.WRITE_VALUE_0, Common.WRITE_LENGTH_255), null)
            CommonUUID.ID_NOTIFICATION_LENGTH_1, CommonUUID.ID_NOTIFICATION_LENGTH_MTU_3 -> {
                setTimeStart(childrenItem.characteristic)
                setNotificationForCharacteristic(childrenItem.characteristic, Notifications.NOTIFY)
            }
            CommonUUID.ID_INDICATE_LENGTH_1, CommonUUID.ID_INDICATE_LENGTH_MTU_3 -> {
                setTimeStart(childrenItem.characteristic)
                setIndicationProperty(childrenItem.characteristic, Notifications.INDICATE)
            }
            CommonUUID.ID_IOP_TEST_LENGTH_1, CommonUUID.ID_IOP_TEST_USER_LEN_1 -> if (!childrenItem.isWriteCharacteristic) {
                writeValueToCharacteristic(childrenItem.characteristic, writeHexForMaxLengthByte(Common.WRITE_VALUE_55, Common.WRITE_LENGTH_1), null)
            } else {
                readCharacteristic(childrenItem.characteristic)
            }
            CommonUUID.ID_IOP_TEST_LENGTH_255, CommonUUID.ID_IOP_TEST_USER_LEN_255 -> if (!childrenItem.isWriteCharacteristic) {
                writeValueToCharacteristic(childrenItem.characteristic, null, Converters.decToByteArray(createDataTestCaseLength255(mtuValue)))
            } else {
                readCharacteristic(childrenItem.characteristic)
            }
            CommonUUID.ID_IOP_TEST_LENGTH_VARIABLE_4, CommonUUID.ID_IOP_TEST_USER_LEN_VARIABLE_4 ->                 // write length 1
                if (childrenItem.getLstValueItemTest().isEmpty() && !childrenItem.isWriteCharacteristic) {
                    writeValueToCharacteristic(childrenItem.characteristic, writeHexForMaxLengthByte(Common.WRITE_VALUE_55, Common.WRITE_LENGTH_1), null)
                } else if (childrenItem.getLstValueItemTest().isEmpty() && childrenItem.isWriteCharacteristic) {
                    childrenItem.isWriteCharacteristic = false
                    childrenItem.statusWrite = -1
                    readCharacteristic(childrenItem.characteristic)
                } else if (childrenItem.getLstValueItemTest().size == 1 && !childrenItem.isWriteCharacteristic) {
                    childrenItem.statusRead = -1
                    childrenItem.isReadCharacteristic = false
                    writeValueToCharacteristic(childrenItem.characteristic, writeHexForMaxLengthByte(Common.WRITE_VALUE_66, Common.WRITE_LENGTH_4), null)
                } else if (childrenItem.getLstValueItemTest().size == 1 && childrenItem.isWriteCharacteristic) {
                    readCharacteristic(childrenItem.characteristic)
                }
            CommonUUID.ID_IOP_TEST_CONST_LENGTH_1 -> if (!childrenItem.isReadCharacteristic) {
                readCharacteristic(childrenItem.characteristic)
            } else {
                writeValueToCharacteristic(childrenItem.characteristic, writeHexForMaxLengthByte(Common.WRITE_VALUE_55, Common.WRITE_LENGTH_1), null)
            }
            CommonUUID.ID_IOP_TEST_CONST_LENGTH_255 -> if (!childrenItem.isReadCharacteristic) {
                readCharacteristic(childrenItem.characteristic)
            } else {
                writeValueToCharacteristic(childrenItem.characteristic, null, Converters.decToByteArray(createDataTestCaseLength255(mtuValue)))
            }
            else -> {
            }
        }
    }

    /**
     * Return value write to characteristics
     */
    private fun createDataTestCaseLength255(len: Int): String {
        val builder = StringBuilder()
        for (i in 0 until len) {
            builder.append(i).append(" ")
        }
        return builder.toString()
    }

    /**
     * Check status item test and the next item test
     */
    private fun checkNextTestCase(characteristic: BluetoothGattCharacteristic?, type: Int) {
        if (characteristic != null) {
            for (itemChildrenTest: ChildrenItemTestInfo in getListChildrenItemTestCase(POSITION_TEST_SERVICE)!!) {
                if (itemChildrenTest.characteristic?.uuid.toString() == characteristic.uuid.toString()) {
                    if (type == 1) {
                        itemChildrenTest.isReadCharacteristic = true
                    } else if (type == 2) {
                        itemChildrenTest.isWriteCharacteristic = true
                    }
                    if (CommonUUID.Characteristic.IOP_TEST_LENGTH_1.showUUID() == characteristic.uuid.toString() || CommonUUID.Characteristic.IOP_TEST_LENGTH_255.showUUID() == characteristic.uuid.toString() || CommonUUID.Characteristic.IOP_TEST_CONST_LENGTH_1.showUUID() == characteristic.uuid.toString() || CommonUUID.Characteristic.IOP_TEST_CONST_LENGTH_255.showUUID() == characteristic.uuid.toString() || CommonUUID.Characteristic.IOP_TEST_USER_LEN_1.showUUID() == characteristic.uuid.toString() || CommonUUID.Characteristic.IOP_TEST_USER_LEN_255.showUUID() == characteristic.uuid.toString()) {
                        mIndexStartChildrenTest = if (itemChildrenTest.isReadCharacteristic && itemChildrenTest.isWriteCharacteristic) {
                            getListChildrenItemTestCase(POSITION_TEST_SERVICE)!!.indexOf(itemChildrenTest) + 1
                        } else {
                            getListChildrenItemTestCase(POSITION_TEST_SERVICE)!!.indexOf(itemChildrenTest)
                        }
                    } else if (CommonUUID.Characteristic.NOTIFICATION_LENGTH_1.showUUID() == characteristic.uuid.toString()) {
                        if (isDisabled) {
                            mIndexStartChildrenTest = getListChildrenItemTestCase(POSITION_TEST_SERVICE)!!.indexOf(itemChildrenTest)
                            isDisabled = false
                        } else {
                            if (itemChildrenTest.statusChildrenTest) {
                                countReTest = 0
                                mIndexStartChildrenTest = getListChildrenItemTestCase(POSITION_TEST_SERVICE)!!.indexOf(itemChildrenTest) + 1
                                isDisabled = false
                            } else {
                                countReTest++
                                if (countReTest > 5) {
                                    mIndexStartChildrenTest = getListChildrenItemTestCase(POSITION_TEST_SERVICE)!!.indexOf(itemChildrenTest) + 1
                                    isDisabled = false
                                    countReTest = 0
                                } else {
                                    isDisabled = true
                                    setNotificationForCharacteristic(characteristic, Notifications.DISABLED)
                                    return
                                }
                            }
                        }
                    } else if (CommonUUID.Characteristic.INDICATE_LENGTH_1.showUUID() == characteristic.uuid.toString()) {
                        if (isDisabled) {
                            mIndexStartChildrenTest = getListChildrenItemTestCase(POSITION_TEST_SERVICE)!!.indexOf(itemChildrenTest)
                            isDisabled = false
                        } else {
                            if (itemChildrenTest.statusChildrenTest) {
                                countReTest = 0
                                mIndexStartChildrenTest = getListChildrenItemTestCase(POSITION_TEST_SERVICE)!!.indexOf(itemChildrenTest) + 1
                                isDisabled = false
                            } else {
                                countReTest++
                                if (countReTest > 5) {
                                    countReTest = 0
                                    mIndexStartChildrenTest = getListChildrenItemTestCase(POSITION_TEST_SERVICE)!!.indexOf(itemChildrenTest) + 1
                                    isDisabled = false
                                } else {
                                    isDisabled = true
                                    setIndicationProperty(characteristic, Notifications.DISABLED)
                                    return
                                }
                            }
                        }
                    } else if (CommonUUID.Characteristic.IOP_TEST_LENGTH_VARIABLE_4.showUUID() == characteristic.uuid.toString() || CommonUUID.Characteristic.IOP_TEST_USER_LEN_VARIABLE_4.showUUID() == characteristic.uuid.toString()) {
                        if (itemChildrenTest.getLstValueItemTest().size > 1 && itemChildrenTest.isReadCharacteristic) {
                            mIndexStartChildrenTest = getListChildrenItemTestCase(POSITION_TEST_SERVICE)!!.indexOf(itemChildrenTest) + 1
                            if ((CommonUUID.Characteristic.IOP_TEST_USER_LEN_VARIABLE_4.showUUID() == characteristic.uuid.toString())) {
                                finishItemTest(POSITION_TEST_SERVICE, getItemTestCaseInfo(POSITION_TEST_SERVICE))
                            }
                        } else {
                            mIndexStartChildrenTest = getListChildrenItemTestCase(POSITION_TEST_SERVICE)!!.indexOf(itemChildrenTest)
                        }
                    } else {
                        mIndexStartChildrenTest = getListChildrenItemTestCase(POSITION_TEST_SERVICE)!!.indexOf(itemChildrenTest) + 1
                    }
                    break
                }
            }
            if (mIndexRunning == POSITION_TEST_IOP3_SECURITY) {
                for (itemChildrenTest: ChildrenItemTestInfo in getListChildrenItemTestCase(POSITION_TEST_IOP3_SECURITY)!!) {
                    if (itemChildrenTest.characteristic?.uuid.toString() == characteristic.uuid.toString()) {
                        handler?.removeCallbacks(iopSecurityRunnable)
                        if (type == 1) {
                            itemChildrenTest.isReadCharacteristic = true
                        }
                        if (CommonUUID.Characteristic.IOP_TEST_SECURITY_BONDING.showUUID() != (characteristic.uuid.toString())) {
                            iopPhase3IndexStartChildrenTest++
                            Log.d(TAG, "iopPhase3IndexStartChildrenTest $iopPhase3IndexStartChildrenTest")
                            showDetailInformationTest(testCaseCount++, false)
                            iopPhase3RunTestCaseSecurity(iopPhase3IndexStartChildrenTest, 1)
                        } else {
                            finishItemTest(POSITION_TEST_IOP3_SECURITY, getSiliconLabsTestInfo().listItemTest[POSITION_TEST_IOP3_SECURITY])
                        }
                    }
                }
            } else if (mIndexRunning == POSITION_TEST_IOP3_CACHING) {
                if (characteristicIOPPhase3IOPTestServiceChangedIndication != null) {
                    if ((characteristicIOPPhase3IOPTestServiceChangedIndication?.uuid.toString() == characteristic.uuid.toString())) {
                        handler?.removeCallbacks(iopCachingRunnable)

                        isServiceChangedIndication = 0
                        showDetailInformationTest(testCaseCount++, false)
                        iopPhase3RunTestCaseCaching(0)
                    }
                }
            }
        }
        if (mIndexStartChildrenTest <= 17) {
            runChildrenTestCase(mIndexStartChildrenTest)
        }
    }

    private var nrTries = 0
    private fun retryCommand(characteristic: BluetoothGattCharacteristic) {
        nrTries++
        if (nrTries <= 5) {
            handler?.postDelayed({
                Log.d(TAG, "retryCommand(), read characteristic" + characteristic.uuid.toString())
                readCharacteristic(characteristic)
            }, 1000)
        }
    }

    /**
     * Create values for control characteristic
     */
    private fun iopPhase3WriteControlBytes(throughput: Int, security: Int, caching: Int): ByteArray {
        val hex = ByteArray(3)
        hex[0] = throughput.toByte()
        hex[1] = security.toByte()
        hex[2] = caching.toByte()
        return hex
    }

    private fun iopPhase3RunTestCaseSecurity(index: Int, isControl: Int) {
        iopPhase3IndexStartChildrenTest = index
        isConnecting = false
        val securityItem = getListChildrenItemTestCase(POSITION_TEST_IOP3_SECURITY)!![index]
        val cUuid = securityItem.characteristic?.uuid.toString()
        val uuids = CommonUUID.Characteristic.values()
        var matchChar = -1
        for (i in uuids.indices) {
            if (cUuid.equals(uuids[i].showUUID(), ignoreCase = true)) {
                matchChar = uuids[i].id
                Log.d(TAG, "Run TestCase [7." + (matchChar - CommonUUID.ID_IOP_TEST_PHASE3_THROUGHPUT) + "] at Characteristic " + uuids[i].showUUID())
                break
            }
        }
        when (matchChar) {
            CommonUUID.ID_IOP_TEST_PHASE3_SECURITY_PAIRING,
            CommonUUID.ID_IOP_TEST_PHASE3_SECURITY_AUTHENTICATION,
            CommonUUID.ID_IOP_TEST_PHASE3_SECURITY_BONDING ->
                if (isControl == 1) {
                    countReTest = 0
                    handler?.postDelayed(iopSecurityRunnable, 60000)
                    writeValueToCharacteristic(
                            characteristicIOPPhase3Control,
                            null,
                            iopPhase3WriteControlBytes(0, matchChar - CommonUUID.ID_IOP_TEST_PHASE3_CONTROL - 1, 0))
                    Log.d(TAG, "Set Control Characteristic for Security")
                } else if (!securityItem.isReadCharacteristic) {
                    Log.d(TAG, "Read Security Characteristic $cUuid")
                    readCharacteristic(securityItem.characteristic)
                }
                else -> {
                }
        }
    }

    private fun iopPhase3RunTestCaseThroughput(isControl: Int) {
        Log.d(TAG, "Run TestCase [6] at Characteristic " + characteristicIOPPhase3Throughput?.uuid.toString())
        if (isControl == 1) {
            writeValueToCharacteristic(
                    characteristicIOPPhase3Control,
                    null,
                    iopPhase3WriteControlBytes(1, 0, 0))
            Log.d(TAG, "Set Control Characteristic for Throughput")
        } else {
            Log.d(TAG, "set Notification enable for Throughput")
            mEndThroughputNotification = false
            setNotificationForCharacteristic(characteristicIOPPhase3Throughput, Notifications.NOTIFY)
            mStartTimeThroughput = System.currentTimeMillis()
            handler?.postDelayed({
                mEndTimeThroughput = System.currentTimeMillis()
                mByteSpeed = ((mByteNumReceived * 1000) / (mEndTimeThroughput - mStartTimeThroughput)).toInt()
                Log.d(TAG, "set Notification disable for throughput")
                Log.d(TAG, "Throughput is $mByteSpeed Bytes/sec")
                try {
                    setNotificationForCharacteristic(characteristicIOPPhase3Throughput, Notifications.DISABLED)
                } catch (e: Exception) {
                    Log.e(TAG, "Error Notification disable for throughput")
                }
                mEndThroughputNotification = true
            }, 5000)
        }
    }

    private fun iopPhase3RunTestCaseCaching(isControl: Int) {
        Log.d(TAG, "Run TestCase [8]")
        val itemChildrenTest: ChildrenItemTestInfo
        isConnecting = false
        if (isControl == 1) {
            if (isServiceChangedIndication == 1) {
                writeValueToCharacteristic(
                        characteristicIOPPhase3Control,
                        null,
                        iopPhase3WriteControlBytes(0, 0, 1))
                Log.d(TAG, "Set Control Characteristic for Service Changed Indications")
            } else {
                writeValueToCharacteristic(
                        characteristicIOPPhase3Control,
                        null,
                        iopPhase3WriteControlBytes(0, 0, 2))
                Log.d(TAG, "Set Control Characteristic for GATT Caching")
            }
        } else {
            itemChildrenTest = getListChildrenItemTestCase(POSITION_TEST_IOP3_CACHING)!![isServiceChangedIndication]
            itemChildrenTest.statusRunTest = 0
            itemChildrenTest.startTimeTest = System.currentTimeMillis()
            handler?.postDelayed(iopCachingRunnable, 90000)
            if (isServiceChangedIndication == 1) {
                val bonded = mBluetoothGatt?.device?.bondState == BluetoothDevice.BOND_BONDED
                if (bonded) {
                    Log.d(TAG, "Subscribe Indication for Service Changed")
                    setNotificationForCharacteristic(characteristicIOPPhase3ServiceChanged, Notifications.INDICATE)
                } else {
                    Log.d(TAG, "Connection is not trusted, not bonded")
                    iopCachingFailed()
                    handler?.removeCallbacks(iopCachingRunnable)
                }
            } else {
                Log.d(TAG, "Set on Client Supported Features")
                writeValueToCharacteristic(characteristicIOPPhase3ClientSupportedFeatures, "01", null)
            }
        }
    }

    private val iopCachingRunnable = Runnable {
        Log.d(TAG, "iopCachingRunnable")
        iopCachingFailed()
    }

    private fun iopCachingFailed() {
        val itemChildrenTest = getListChildrenItemTestCase(POSITION_TEST_IOP3_CACHING)!![isServiceChangedIndication]
        itemChildrenTest.statusRunTest = 1
        itemChildrenTest.statusChildrenTest = false
        itemChildrenTest.endTimeTest = System.currentTimeMillis()
        getSiliconLabsTestInfo().listItemTest[POSITION_TEST_IOP3_CACHING].setStatusTest(Common.IOP3_TC_STATUS_FAILED)
        finishItemTest(POSITION_TEST_IOP3_CACHING, getSiliconLabsTestInfo().listItemTest[POSITION_TEST_IOP3_CACHING])
    }

    private fun retryIOP3Failed(index: Int, mCountReTest: Int) {
        if (index == POSITION_TEST_SERVICE) {
            updateDataTestFailed(index)
        } else if (index == POSITION_TEST_CONNECTION) {
            Log.d(TAG, "retryIOP3Failed index $index")
            handler?.postDelayed({ finishItemTest(POSITION_TEST_CONNECTION, getSiliconLabsTestInfo().listItemTest[POSITION_TEST_CONNECTION]) }, 1000)
        } else {
            if (mCountReTest < 6) {
                isConnecting = false
                reconnect(2000)
            } else {
                updateDataTestFailed(index)
            }
        }
    }

    private fun checkIOP3OTA(index: Int, status: Int) {
        if (!otaProcess) {
            if (index == POSITION_TEST_IOP3_OTA_ACK || index == POSITION_TEST_IOP3_OTA_WITHOUT_ACK) {
                Log.d(TAG, "checkIOP3OTA $index, setStatusRunTest finish test")
                Log.d(TAG, "setStatusTest $status")
                val itemTestCaseInfo = getSiliconLabsTestInfo().listItemTest[index]
                itemTestCaseInfo.setStatusTest(status)
            }
        }
    }

    private val iopSecurityRunnable: Runnable = Runnable {
        Log.d(TAG, "iopSecurityRunnable $iopPhase3IndexStartChildrenTest")
        val itemChildrenTest = getListChildrenItemTestCase(POSITION_TEST_IOP3_SECURITY)!![iopPhase3IndexStartChildrenTest]
        itemChildrenTest.statusRunTest = 1 // 0 running, 1 finish test
        Log.d(TAG, "iopSecurityRunnable $iopPhase3IndexStartChildrenTest, setStatusRunTest finish test")
        itemChildrenTest.statusChildrenTest = false
        Log.d(TAG, "setStatusChildrenTest false")
        val itemTestCaseInfo = getSiliconLabsTestInfo().listItemTest[POSITION_TEST_IOP3_SECURITY]
        itemTestCaseInfo.setStatusTest(Common.IOP3_TC_STATUS_FAILED)
        updateDataTestFailed(POSITION_TEST_IOP3_SECURITY)
    }

    private fun iopPhase3TestCaseOTA(index: Int) {
        Log.d(TAG, "iopPhase3TestCaseOTA $index")
        otaMode = true
        if (index == 0) {
            runOnUiThread {
                initOtaProgress()
                initLoading()
                otaOnClick()
                mBluetoothGatt?.requestMtu(247)
            }
        } else {
            reliable = false
            otaOnClick()
        }
    }

    private fun getOtaFilename(): String {
        val board = getSiliconLabsTestInfo().getIopBoard(getSiliconLabsTestInfo().getValuesPlatform(1))
        return if(iopPhase3IndexStartChildrenTest == 0) board.ota1FileName else board.ota2FileName
    }

    private fun openOtaFileInputStream(): InputStream {
        return assets.open("iop/" + getOtaFilename())
    }

    private fun startOtaProcess() {
        otaFilename = getOtaFilename()

        val inputStream = openOtaFileInputStream()
        otafile = ByteArray(inputStream.available())
        inputStream.read(otafile)

        runOnUiThread {
            ota_mode = false
            otaSetup?.dismiss()
            boolOTAbegin = true
            otaProcess = true
            Log.d(TAG, OTA_BEGIN)
            dfuMode(OTA_BEGIN)
        }
    }

    /**
     * INITIALIZES OTA PROGRESS DIALOG
     */
    private fun initOtaProgress() {
        otaProgress = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.iop_ota_progress)

            findViewById<TextView>(R.id.device_address).text = mBluetoothGatt?.device?.address
            progressBar = findViewById(R.id.otaprogress)
            dataRate = findViewById(R.id.datarate)
            dataSize = findViewById(R.id.datasize)
            filename = findViewById(R.id.filename)
            steps = findViewById(R.id.otasteps)
            chrono = findViewById(R.id.chrono)
            btnOtaStart = findViewById(R.id.otabutton)
            sizename = findViewById(R.id.sizename)
            uploadimage = findViewById(R.id.connecting_spinner)
        }

        btnOtaStart?.setOnClickListener {
            otaProgress?.dismiss()
            ota_mode = false
            otaMode = false
            dfuMode("DISCONNECTION")
            handler?.postDelayed({ scanLeDevice(true) }, 1000)
        }
    }

    /**
     * INITIALIZES LOADING DIALOG
     */
    private fun initLoading() {
        loadingDialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.loadingdialog)

            loadingimage = findViewById(R.id.connecting_spinner)
            loadingLog = findViewById(R.id.loadingLog)
            loadingHeader = findViewById(R.id.loading_header)
        }
    }

    /**
     * START OTA BUTTON (UI, Bools)
     */
    private fun otaOnClick() {
        if (ota_mode) {
            otaProcess = true
            boolOTAbegin = false
        } else {
            otaProcess = true
            boolOTAbegin = true
        }
        runOnUiThread {
            loadingimage?.visibility = View.GONE
            loadingDialog?.dismiss()
            startOtaProcess()
        }
    }

    /**
     * OTA STATE MACHINE
     */
    @Synchronized
    private fun dfuMode(step: String) {
        when (step) {
            "INIT" -> dfuMode(OTA_BEGIN)
            "OTABEGIN" -> if (ota_mode) {
                //START OTA PROCESS -> gattCallback -> OnCharacteristicWrite
                Log.d("OTA_BEGIN", "true")
                handler?.postDelayed(WRITE_OTA_CONTROL_ZERO, 200)
            } else {
                //PUT DEVICE IN DFUMODE -> gattCallback -> OnCharacteristicWrite
                if (homekit) {
                    mBluetoothGatt?.readDescriptor(kitDescriptor)
                } else {
                    Log.d("DFU_MODE", "true")
                    handler?.postDelayed(WRITE_OTA_CONTROL_ZERO, 200)
                }
            }
            "OTAUPLOAD" -> {
                Log.d(OTA_UPLOAD, "Called")
                //Check Services
                val mBluetoothGattService = mBluetoothGatt?.getService(ota_service)
                if (mBluetoothGattService != null) {
                    val charac = mBluetoothGatt?.getService(ota_service)?.getCharacteristic(ota_data)
                    if (charac != null) {
                        charac.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                        Log.d("Instance ID", "" + charac.instanceId)

                        val datathread = otafile
                        pack = 0

                        //Set info into UI OTA Progress
                        runOnUiThread {
                            filename?.text = otaFilename
                            steps?.text = getString(R.string.iop_test_label_1_of_1)
                            sizename?.text = getString(R.string.iop_test_n_bytes, datathread?.size)
                            uploadimage?.visibility = View.VISIBLE
                            animateLoading()
                        }
                        //Start OTA_data Upload in another thread
                        val otaUpload = Thread(Runnable {
                            if (reliable) {
                                otaWriteDataReliable()
                            } else {
                                whiteOtaData(datathread)
                            }
                        })
                        otaUpload.start()
                    }
                }
            }
            "OTAEND" -> {
                Log.d(TAG, "OTAEND Called")
                handler?.postDelayed({ writeOtaControl(0x03.toByte()) }, 1000)
            }
            "DISCONNECTION" -> {
                otaProcess = false
                boolOTAbegin = false
                disconnectGatt(mBluetoothGatt)
            }
            else -> {
            }
        }
    }

    private val DFU_OTA_UPLOAD: Runnable = Runnable {
        dfuMode(OTA_UPLOAD)
    }

    /**
     * SHOWS OTA PROGRESS DIALOG IN UI
     */
    private fun showOtaProgress() {
        otaProgress?.show()
        btnOtaStart?.isClickable = false
        btnOtaStart?.setBackgroundColor(ContextCompat.getColor(this@IOPTestActivity, R.color.silabs_inactive))
        otaProgress?.setCanceledOnTouchOutside(false)
        dfuMode(OTA_BEGIN)
    }

    /**
     * SHOWS OTA SETUP DIALOG IN UI
     */
    private fun showLoading() {
        loadingDialog?.apply {
            show()
            setCanceledOnTouchOutside(false)
            animateLoading()
        }
    }

    /**
     * CREATES BAR PROGRESS ANIMATION IN LOADING AND OTA PROGRESS DIALOG
     */
    private fun animateLoading() {
        if (uploadimage != null && loadingimage != null && otaProgress != null) {
            uploadimage?.visibility = View.GONE
            loadingimage?.visibility = View.GONE
            if (loadingDialog?.isShowing!!) {
                loadingimage?.visibility = View.VISIBLE
            }
            if (otaProgress?.isShowing!!) {
                uploadimage?.visibility = View.VISIBLE
            }
        }
    }

    /**
     * USED TO CLEAN CACHE AND REDISCOVER SERVICES
     */
    private fun refreshServices() {
        if (mBluetoothGatt != null && mBluetoothGatt?.device != null) {
            refreshDeviceCache()
            mBluetoothGatt?.discoverServices()
        } else if (mBluetoothService != null && mBluetoothService?.connectedGatt != null) {
            refreshDeviceCache()
            mBluetoothService?.connectedGatt?.discoverServices()
        }
    }

    /**
     * CALLS A METHOD TO CLEAN DEVICE SERVICES
     */
    private fun refreshDeviceCache(): Boolean {
        try {
            Log.d(TAG, "refreshDevice Called")
            val localMethod = mBluetoothGatt?.javaClass?.getMethod("refresh")
            if (localMethod != null) {
                val bool: Boolean = localMethod.invoke(mBluetoothGatt, *arrayOfNulls(0)) as Boolean
                Log.d(TAG, "refreshDevice bool: $bool")
                return bool
            }
        } catch (localException: Exception) {
            Log.e(TAG, "refreshDevice An exception occured while refreshing device")
        }
        return false
    }

    private val WRITE_OTA_CONTROL_ZERO: Runnable = Runnable {
        writeOtaControl(0x00.toByte())
    }

    /**
     * (RUNNABLE) CHECKS OTA BEGIN BOX AND STARTS
     */
    private val checkBeginRunnable: Runnable = Runnable {
        chrono?.base = SystemClock.elapsedRealtime()
        chrono?.start()
    }

    fun onceAgain() {
        writeOtaControl(0x00.toByte())
    }

    /**
     * WRITES BYTE TO OTA CONTROL CHARACTERISTIC
     */
    private fun writeOtaControl(ctrl: Byte): Boolean {
        Log.e("writeOtaControl", "Called")
        if (mBluetoothGatt?.getService(ota_service) != null) {
            val charac = mBluetoothGatt?.getService(ota_service)?.getCharacteristic(ota_control)
            if (charac != null) {
                Log.d("Instance ID", "" + charac.instanceId)
                charac.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                Log.d("charac_properties", "" + charac.properties)
                val control = ByteArray(1)
                control[0] = ctrl
                charac.value = control
                mBluetoothGatt?.writeCharacteristic(charac)
                return true
            } else {
                Log.d(TAG, "characteristic null")
            }
        } else {
            Log.d("service", "null")
        }
        return false
    }

    /**
     * WRITES EBL/GBL FILES TO OTA_DATA CHARACTERISTIC
     */
    @Synchronized
    fun otaWriteDataReliable() {
        if (pack == 0) {
            //SET MTU_divisible by 4
            var minus = 0
            do {
                mtuDivisible = mtu - 3 - minus
                minus++
            } while (mtuDivisible % 4 != 0)
        }
        val writearray: ByteArray
        val pgss: Float
        if (pack + mtuDivisible > otafile!!.size - 1) {
            //SET last by 4
            var plus = 0
            var last = otafile!!.size - pack
            do {
                last += plus
                plus++
            } while (last % 4 != 0)
            writearray = ByteArray(last)
            var j = 0
            for (i in pack until pack + last) {
                if (otafile!!.size - 1 < i) {
                    writearray[j] = 0xFF.toByte()
                } else {
                    writearray[j] = otafile!![i]
                }
                j++
            }
            pgss = ((pack + last).toFloat() / (otafile!!.size - 1)) * 100
            Log.d("characte", "last: " + pack + " / " + (pack + last) + " : " + Converters.getHexValue(writearray))
        } else {
            var j = 0
            writearray = ByteArray(mtuDivisible)
            for (i in pack until pack + mtuDivisible) {
                writearray[j] = otafile!![i]
                j++
            }
            pgss = ((pack + mtuDivisible).toFloat() / (otafile!!.size - 1)) * 100
            Log.d("characte", "pack: " + pack + " / " + (pack + mtuDivisible) + " : " + Converters.getHexValue(writearray))
        }
        val charac = mBluetoothGatt?.getService(ota_service)?.getCharacteristic(ota_data)
        charac?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        charac?.value = writearray
        mBluetoothGatt?.writeCharacteristic(charac)
        val waitingTime = (System.currentTimeMillis() - otaTime)
        val bitrate = 8 * pack.toFloat() / waitingTime
        if (pack > 0) {
            handler?.post {
                runOnUiThread {
                    progressBar?.progress = pgss.toInt()
                    val datarate = String.format(Locale.US, kBits, bitrate)
                    dataRate?.text = datarate
                    dataSize?.text = getString(R.string.iop_test_n_percent, pgss.toInt())
                }
            }
        } else {
            otaTime = System.currentTimeMillis()
        }
    }

    /**
     * White with NO RESPONSE
     */
    @Synchronized
    fun whiteOtaData(dataThread: ByteArray?) {
        try {
            val value = ByteArray(mtu - 3)
            val start = System.nanoTime()
            var j = 0
            for (i in dataThread!!.indices) {
                value[j] = dataThread[i]
                j++
                if (j >= mtu - 3 || i >= (dataThread.size - 1)) {
                    var wait = System.nanoTime()
                    val charac = mBluetoothGatt?.getService(ota_service)?.getCharacteristic(ota_data)
                    charac?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    val progress = ((i + 1).toFloat() / dataThread.size) * 100
                    val bitrate = (((i + 1) * (8.0)).toFloat() / (((wait - start) / 1000000.0).toFloat()))
                    if (j < mtu - 3) {
                        val end = ByteArray(j)
                        System.arraycopy(value, 0, end, 0, j)
                        Log.d("Progress", "sent " + (i + 1) + " / " + dataThread.size + " - " + String.format("%.1f", progress) + " % - " + String.format(kBits, bitrate) + " - " + Converters.getHexValue(end))
                        runOnUiThread {
                            dataSize?.text = getString(R.string.iop_test_n_percent, progress.toInt())
                            progressBar?.progress = progress.toInt()
                        }
                        charac?.value = end
                    } else {
                        j = 0
                        Log.d("Progress", "sent " + (i + 1) + " / " + dataThread.size + " - " + String.format("%.1f", progress) + " % - " + String.format(kBits, bitrate) + " - " + Converters.getHexValue(value))
                        runOnUiThread {
                            dataSize?.text = getString(R.string.iop_test_n_percent, progress.toInt())
                            progressBar?.progress = progress.toInt()
                        }
                        charac?.value = value
                    }
                    if (mBluetoothGatt!!.writeCharacteristic(charac)) {
                        runOnUiThread {
                            val datarate = String.format(Locale.US, kBits, bitrate)
                            dataRate?.text = datarate
                        }
                        while ((System.nanoTime() - wait) / 1000000.0 < delayNoResponse) {
                        }
                    } else {
                        do {
                            while ((System.nanoTime() - wait) / 1000000.0 < delayNoResponse) {
                            }
                            wait = System.nanoTime()
                            runOnUiThread {
                                val datarate = String.format(Locale.US, kBits, bitrate)
                                dataRate?.text = datarate
                            }
                        } while (!mBluetoothGatt!!.writeCharacteristic(charac))
                    }
                }
            }
            handler?.post {
                runOnUiThread {
                    chrono?.stop()
                    uploadimage?.clearAnimation()
                    uploadimage?.visibility = View.INVISIBLE
                }
            }
            val end = System.currentTimeMillis()
            val time = (end - start) / 1000L.toFloat()
            Log.d("OTA Time - ", "" + time + "s")
            dfuMode(OTA_END)
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    /**
     * WRITES OTA CONTROL FOR HOMEKIT DEVICES
     */
    private fun homeKitOTAControl(instanceID: ByteArray) {
        //WRITE CHARACTERISTIC FOR HOMEKIT
        val value = byteArrayOf(0x00, 0x02, 0xee.toByte(), instanceID[0], instanceID[1], 0x03, 0x00, 0x01, 0x01, 0x01)
        writeGenericCharacteristic(ota_service, ota_control, value)
        Log.d(TAG, "characteristic writting: " + Converters.getHexValue(value))
    }

    /**
     * WRITES BYTE ARRAY TO A GENERIC CHARACTERISTIC
     */
    private fun writeGenericCharacteristic(service: UUID?, characteristic: UUID?, value: ByteArray?): Boolean {
        if (mBluetoothGatt != null) {
            val bluetoothGattCharacteristic = mBluetoothGatt?.getService(service)?.getCharacteristic(characteristic)
            Log.d(TAG, "characteristic exists")
            if (bluetoothGattCharacteristic != null) {
                bluetoothGattCharacteristic.value = value
                bluetoothGattCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                mBluetoothGatt?.writeCharacteristic(bluetoothGattCharacteristic)
                Log.d(TAG, "characteristic written")
            } else {
                Log.d(TAG, "characteristic null")
                return false
            }
        } else {
            Log.d("bluetoothGatt", "null")
            return false
        }
        return true
    }

    private val gattCallback: TimeoutGattCallback = object : TimeoutGattCallback() {
        override fun onTimeout() {
            super.onTimeout()
            Log.d(TAG, "updateDataTestFailed onTimeout()")
            updateDataTestFailed(mIndexRunning)
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            mBluetoothGatt?.let {
                if (mBluetoothGatt?.device?.address != gatt.device.address) {
                    return
                }
            }
            super.onConnectionStateChange(gatt, status, newState)
            Log.d(TAG, "onConnectionStateChange status $status - newState $newState")
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    handler?.removeCallbacks(connectionRunnable)
                    isConnected = true
                    isTestFinished = false
                    if (getSiliconLabsTestInfo().listItemTest[POSITION_TEST_CONNECTION].getStatusTest() == Common.IOP3_TC_STATUS_PROCESSING) {
                        Log.d(TAG, "onConnectionStateChange POSITION_TEST_CONNECTION")
                        finishItemTest(POSITION_TEST_CONNECTION, getSiliconLabsTestInfo().listItemTest[POSITION_TEST_CONNECTION])
                    } else {
                        if (!otaProcess) {
                            Log.d(TAG, "onConnectionStateChange connected mIndexRunning $mIndexRunning")
                            mBluetoothGatt?.requestMtu(247)
                        } else { //After OTA process started
                            //get information
                            Log.d(TAG, "Device: " + gatt.device)
                            Log.d(TAG, "Device Name: " + gatt.device.name)
                            if (gatt.services.isEmpty()) {
                                handler?.postDelayed({
                                    mBluetoothGatt = null //It's going to be equal gatt in Discover Services Callback...
                                    Log.d(TAG, "onConnected, start Services Discovery: " + gatt.discoverServices())
                                }, 250)

                                discoverTimeout = true
                                val timeout = Runnable {
                                    handler?.postDelayed({
                                        if (discoverTimeout) {
                                            disconnectGatt(gatt)
                                            runOnUiThread {
                                                Toast.makeText(baseContext, "DISCOVER SERVICES TIMEOUT", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }, 25000)
                                }
                                Thread(timeout).start()
                            }
                        }
                    }
                }
                BluetoothGatt.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected IOP Test device: " + System.currentTimeMillis())
                    isConnected = false
                    discoverTimeout = false
                    exit(mBluetoothGatt)
                    if (status != 0 && otaMode) {
                        if (status == 133) {
                            //reconnect after 30 seconds
                            handler?.postDelayed({
                                Log.d(TAG, "onConnectionStateChange OTA status $status")
                                retryIOP3Failed(mIndexRunning, countReTest++)
                            }, 30000)
                        }
                    } else {
                        if (!otaProcess && !isTestFinished) {
                            Log.d(TAG, "onConnectionStateChange ota_process $otaProcess,isConnecting $isConnecting")
                            if (mIndexRunning > POSITION_TEST_IOP3_OTA_WITHOUT_ACK || mIndexRunning < POSITION_TEST_IOP3_OTA_ACK) {
                                if (!isConnecting) {
                                    isConnecting = true
                                    handler?.postDelayed({
                                        Log.d(TAG, "onConnectionStateChange re-connecting")
                                        retryIOP3Failed(mIndexRunning, countReTest++)
                                    }, 5000)
                                }
                            } else {
                                if (status == 133 || status == 8) {
                                    if (!isConnecting) {
                                        isConnecting = true
                                        handler?.postDelayed({
                                            Log.d(TAG, "onConnectionStateChange status: $status")
                                            retryIOP3Failed(mIndexRunning, countReTest++)
                                        }, 30000)
                                    }
                                }
                            }
                        } else {
                            if (status == 133) {
                                handler?.postDelayed({
                                    Log.d(TAG, "onConnectionStateChange OTA status: $status")
                                    retryIOP3Failed(mIndexRunning, countReTest++)
                                }, 30000)
                            }
                        }
                        if (disconnectGatt) {
                            exit(gatt)
                        }
                        if (otaSetup != null && otaSetup?.isShowing!!) {
                            exit(gatt)
                        }
                        if (gatt.services.isEmpty()) {
                            exit(gatt)
                        }
                        if (!boolOTAbegin && !otaProcess) {
                            exit(gatt)
                        }
                    }
                }
                BluetoothGatt.STATE_CONNECTING -> Log.d(TAG, "onConnectionStateChange Connecting...")
                else -> {
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Log.d(TAG, "onServicesDiscovered(), status " + Integer.toHexString(status))
            if (mBluetoothGatt != gatt) {
                mBluetoothGatt = gatt
                handler?.postDelayed({
                    refreshServices()
                }, 2000)
            } else {
                discoverTimeout = false
                if (status != 0) {
                    runOnUiThread {
                        Toast.makeText(baseContext, ErrorCodes.getErrorName(status), Toast.LENGTH_LONG).show()
                        updateDataTestFailed(mIndexRunning)
                    }
                    handler?.postDelayed({
                        disconnectGatt(gatt)
                    }, 2000)
                } else {
                    runnable(gatt)

                    val otaServiceCheck = gatt.getService(ota_service) != null
                    if (otaServiceCheck) {
                        val otaDataCheck = gatt.getService(ota_service).getCharacteristic(ota_data) != null
                        if (otaDataCheck) {
                            val homeKitCheck = gatt.getService(homekit_service) != null
                            if (!homeKitCheck) {
                                ota_mode = true
                            }
                        } else {
                            if (boolOTAbegin) {
                                onceAgain()
                            }
                        }
                    }

                    //IF DFU_MODE, LAUNCH OTA SETUP AUTOMATICALLY
                    if (ota_mode && boolOTAbegin) {
                        handler?.postDelayed({
                            runOnUiThread {
                                loadingimage?.visibility = View.GONE
                                loadingDialog?.dismiss()
                                initOtaProgress()
                                showOtaProgress()
                            }
                        }, 1000)
                    }
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            Log.d(TAG, "onCharacteristicRead: " + characteristic.uuid.toString() + " status " + status)
            if (areParameters) {
                if (status == 0) {
                    convertValuesParameters(characteristic)
                    Log.d(TAG, "onCharacteristicRead convertValuesParameters")
                } else {
                    nrTries = 0
                    Log.d(TAG, "onCharacteristicRead retryCommand")
                    retryCommand(characteristic)
                }
            } else {
                if (status == 18) {
                    if (mIndexRunning == POSITION_TEST_IOP3_CACHING) {
                        if (isServiceChangedIndication == 0) {
                            readCharacteristic(characteristicIOPPhase3DatabaseHash)
                            return
                        }
                    }
                }
                updateDataTest(characteristic, 0, status)
                checkNextTestCase(characteristic, 1)
                // type 1: CharacteristicRead, 2:CharacteristicWrite
                if (mBluetoothGatt?.getService(ota_service) != null) {
                    if (characteristic === mBluetoothGatt?.getService(ota_service)?.getCharacteristic(ota_control)) {
                        val value = characteristic.value
                        if (value[2] == 0x05.toByte()) {
                            Log.e("homekit_descriptor", "Insecure Connection")
                            runOnUiThread {
                                Toast.makeText(this@IOPTestActivity, "Error: Not a Homekit Secure Connection", Toast.LENGTH_SHORT).show()
                            }
                        } else if (value[2] == 0x04.toByte()) {
                            Log.e("homekit_descriptor", "Wrong Address")
                        } else if (value[2] == 0x00.toByte()) {
                            Log.e("homekit_descriptor", "Entering in DFU_Mode...")
                            if (ota_mode && otaProcess) {
                                Log.e(OTA_UPLOAD, "Sent")
                                handler?.removeCallbacks(DFU_OTA_UPLOAD)
                                handler?.postDelayed(DFU_OTA_UPLOAD, 500)
                            } else if (!ota_mode && otaProcess) {
                                runOnUiThread {
                                    loadingLog?.text = getString(R.string.iop_test_label_resetting)
                                    showLoading()
                                    animateLoading()
                                }
                                handler?.postDelayed({
                                    reconnect(4000)
                                }, 200)
                            }
                        }
                    }
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Log.d(TAG, "onCharacteristicWrite: " + characteristic.uuid.toString())
            Log.d(TAG, "onCharacteristicWrite: $status")
            if (!otaProcess) {
                updateDataTest(characteristic, 1, status)
                checkNextTestCase(characteristic, 2)
            } else {
                if (characteristic.uuid == ota_control) { //OTA Control Callback Handling
                    if (characteristic.value.size == 1) {
                        if (characteristic.value[0] == 0x00.toByte()) {
                            Log.e("Callback", "Control " + Converters.getHexValue(characteristic.value) + "status: " + status)
                            if (ota_mode && otaProcess) {
                                Log.e(OTA_UPLOAD, "Sent")
                                runOnUiThread(checkBeginRunnable)
                                handler?.removeCallbacks(DFU_OTA_UPLOAD)
                                handler?.postDelayed(DFU_OTA_UPLOAD, 500)
                            } else if (!ota_mode && otaProcess) {
                                runOnUiThread {
                                    loadingLog?.text = getString(R.string.iop_test_label_resetting)
                                    showLoading()
                                    animateLoading()
                                }
                                handler?.post {
                                    reconnect(4000)
                                }
                            }
                        }
                        if (characteristic.value[0] == 0x03.toByte()) {
                            if (otaProcess) {
                                Log.e("Callback", "Control " + Converters.getHexValue(characteristic.value) + "status: " + status)
                                runOnUiThread {
                                    btnOtaStart?.setBackgroundColor(ContextCompat.getColor(this@IOPTestActivity, R.color.silabs_red))
                                    btnOtaStart?.isClickable = true
                                    btnOtaStart?.callOnClick()
                                }
                                boolOTAbegin = false
                            }
                        }
                    } else {
                        Log.i("OTA_Control", "Received: " + Converters.getHexValue(characteristic.value))
                        if (characteristic.value[0].toInt() == 0x00 && characteristic.value[1].toInt() == 0x02) {
                            Log.i("HomeKit", "Reading OTA_Control...")
                            mBluetoothGatt?.readCharacteristic(characteristic)
                        }
                    }
                }
                if (characteristic.uuid == ota_data) {   //OTA Data Callback Handling
                    if (reliable) {
                        if (otaProgress?.isShowing!!) {
                            pack += mtuDivisible
                            if (pack <= otafile?.size!! - 1) {
                                otaWriteDataReliable()
                            } else if (pack > otafile?.size!! - 1) {
                                handler?.post {
                                    runOnUiThread {
                                        chrono?.stop()
                                        uploadimage?.clearAnimation()
                                        uploadimage?.visibility = View.INVISIBLE
                                    }
                                }
                                dfuMode(OTA_END)
                            }
                        }
                    }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            Log.d(TAG, "onCharacteristicChanged: " + characteristic.uuid.toString() + " len:" + characteristic.value.size)
            updateDataTest(characteristic, -1, -1)
            checkNextTestCase(characteristic, 0)
            // type 1: CharacteristicRead, 2:CharacteristicWrite, 0:Notify
        }

        override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            super.onDescriptorRead(gatt, descriptor, status)
            Log.d(TAG, "onDescriptorRead mIndexStartChildrenTest: $mIndexStartChildrenTest")
            if (descriptor.uuid.toString() == homekit_descriptor.toString()) {
                val value = ByteArray(2)
                value[0] = 0xF2.toByte()
                value[1] = 0xFF.toByte()
                if (descriptor.value[0] == value[0] && descriptor.value[1] == value[1]) {
                    Log.i("descriptor", "getValue " + Converters.getHexValue(descriptor.value))
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            Log.d(TAG, "onDescriptorWrite status $status")
            if (mIndexStartChildrenTest <= 17 && isDisabled) {
                Log.e(TAG, "onDescriptorWrite mIndexStartChildrenTest: $mIndexStartChildrenTest")
                runChildrenTestCase(mIndexStartChildrenTest)
            } else if (mEndThroughputNotification) {
                mEndThroughputNotification = false
                if (status == 0) {
                    finishItemTest(POSITION_TEST_IOP3_THROUGHPUT, getSiliconLabsTestInfo().listItemTest[POSITION_TEST_IOP3_THROUGHPUT])
                }
            /*} else if (getSiliconLabsTestInfo().listItemTest[POSITION_TEST_IOP3_CACHING]
            .getStatusTest() == Common.IOP3_TC_STATUS_PROCESSING) {
                iopPhase3RunTestCaseCaching(1) */
            } else {
                Log.d(TAG, "do nothing")
            }
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int) {
            super.onReliableWriteCompleted(gatt, status)
            Log.d(TAG, "onReliableWriteCompleted: ")
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            Log.d(TAG, "onMtuChanged: $mtu")
            this@IOPTestActivity.mtu = mtu
            mStartTimeDiscover = System.currentTimeMillis()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "setPreferredPhy 1M")
                mBluetoothGatt?.setPreferredPhy(BluetoothDevice.PHY_LE_1M_MASK, BluetoothDevice.PHY_LE_1M_MASK, BluetoothDevice.PHY_OPTION_NO_PREFERRED)
            }
            if (mIndexRunning == POSITION_TEST_DISCOVER_SERVICE || mIndexRunning == POSITION_TEST_IOP3_THROUGHPUT) {
                mListCharacteristics.clear()
                characteristicsPhase3Security.clear()
                characteristicIOPPhase3Control = null
                refreshServices()
            } else if ((mIndexRunning == POSITION_TEST_IOP3_OTA_WITHOUT_ACK
                            || mIndexRunning == POSITION_TEST_IOP3_OTA_ACK)
                    && !otaProcess) {
                Log.d(TAG, "rediscover OTA")
                handler?.postDelayed({
                    mListCharacteristics.clear()
                    characteristicsPhase3Security.clear()
                    characteristicIOPPhase3Control = null
                    refreshServices()
                }, 5000)
            }
            if (otaProcess) {
                mBluetoothGatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
            }
        }

    }


    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device

            synchronized(readScannerStartTime) {
                if (readScannerStartTime) {
                    readScannerStartTime = false
                    mStartTimeScanner = System.currentTimeMillis()
                    Log.d("scanLeDevice", "startTime: $mStartTimeScanner")
                }
            }

            Log.d(TAG, "Scanner device name: " + device.name)
            Log.d(TAG, "Scanner device address: " + device.address)
            if (getSiliconLabsTestInfo().listItemTest[POSITION_TEST_IOP3_OTA_ACK].getStatusTest() == Common.IOP3_TC_STATUS_PROCESSING
                    || getSiliconLabsTestInfo().listItemTest[POSITION_TEST_IOP3_OTA_WITHOUT_ACK].getStatusTest() == Common.IOP3_TC_STATUS_PROCESSING) {
                otaDeviceName = if (mIndexRunning == POSITION_TEST_IOP3_OTA_ACK) {
                    ota1DeviceName
                } else {
                    ota2DeviceName
                }
                if (device.address == mDeviceAddress) {
                    Log.d(TAG, "Scanner, OTA IOP test device: " + device.address + ", device name: " + device.name)
                    mBluetoothDevice = device
                    scanLeDevice(false)
                    isScanning = false
                    handler?.removeCallbacks(scanRunnable)
                    handler?.postDelayed({
                        connectToDevice(mBluetoothDevice)
                    }, 5000)
                    mDeviceName = device.name
                    Log.d(TAG, "connect to " + device.address)
                    return
                }
            }
            if (!TextUtils.isEmpty(device.name)
                    && (device.name.replace(" ", "").equals(getSiliconLabsTestInfo().fwName.replace(" ", ""), ignoreCase = true)
                            || device.name.replace(" ", "").equals(otaDeviceName!!.replace(" ", ""), ignoreCase = true))) {
                if (getSiliconLabsTestInfo().listItemTest[POSITION_TEST_SCANNER].getStatusTest() == Common.IOP3_TC_STATUS_PASS) {
                    if (device.address != mDeviceAddress) {
                        return
                    }
                }
                mBluetoothDevice = device
                getSiliconLabsTestInfo().fwName = device.name.trim()
                Log.d(TAG, "Scanner, IOP test device: " + device.address + ", device name: " + device.name)
                scanLeDevice(false)
                if (getSiliconLabsTestInfo().listItemTest[POSITION_TEST_SCANNER].getStatusTest() == Common.IOP3_TC_STATUS_PROCESSING) {
                    mDeviceAddress = device.address
                    mDeviceName = device.name
                    finishItemTest(POSITION_TEST_SCANNER, getSiliconLabsTestInfo().listItemTest[POSITION_TEST_SCANNER])
                } else if (getSiliconLabsTestInfo().listItemTest[POSITION_TEST_IOP3_SECURITY].getStatusTest() == Common.IOP3_TC_STATUS_PROCESSING
                        /*|| getSiliconLabsTestInfo().listItemTest[POSITION_TEST_IOP3_CACHING]
                                .getStatusTest() == Common.IOP3_TC_STATUS_PROCESSING*/) {
                    val manufacturer = Build.MANUFACTURER
                    Log.d(TAG, "manufacturer: $manufacturer")
                    if (mIndexRunning == POSITION_TEST_IOP3_SECURITY) {
                        connectToDevice(mBluetoothDevice)
                    } else if (mIndexRunning == POSITION_TEST_IOP3_CACHING) {
                        connectToDevice(mBluetoothDevice)
                    }
                }
                isScanning = false
                handler?.removeCallbacks(scanRunnable)
            }
        }
    }

    interface Listener {
        fun updateUi()
        fun scrollViewToPosition(position: Int)
    }

    companion object {
        private const val FOLDER_NAME = "SiliconLabs_App"
        private const val TAG = "IOPTest"

        private val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private val ota_service = UUID.fromString("1d14d6ee-fd63-4fa1-bfa4-8f47b42119f0")
        private val ota_data = UUID.fromString("984227f3-34fc-4045-a5d0-2c581f81a153")
        private val ota_control = UUID.fromString("f7bf3564-fb6d-4e53-88a4-5e37e0326063")
        private val homekit_descriptor = UUID.fromString("dc46f0fe-81d2-4616-b5d9-6abdd796939a")
        private val homekit_service = UUID.fromString("0000003e-0000-1000-8000-0026bb765291")

        private const val OTA_BEGIN = "OTABEGIN"
        private const val OTA_UPLOAD = "OTAUPLOAD"
        private const val OTA_END = "OTAEND"

        private const val POSITION_TEST_SCANNER = 0
        private const val POSITION_TEST_CONNECTION = 1
        private const val POSITION_TEST_DISCOVER_SERVICE = 2
        private const val POSITION_TEST_SERVICE = 3

        /* Add IoP Test for P3 */
        private const val POSITION_TEST_IOP3_THROUGHPUT = 6
        private const val POSITION_TEST_IOP3_SECURITY = 7
        private const val POSITION_TEST_IOP3_CACHING = 8
        private const val POSITION_TEST_IOP3_OTA_ACK = 4
        private const val POSITION_TEST_IOP3_OTA_WITHOUT_ACK = 5

        private const val SCAN_PERIOD: Long = 10000
        private const val CONNECTION_PERIOD: Long = 10000
        private const val BLUETOOTH_SETTINGS_REQUEST_CODE = 100

        fun startActivity(context: Context) {
            val intent = Intent(context, IOPTestActivity::class.java)
            startActivity(context, intent, null)
        }
    }
}
