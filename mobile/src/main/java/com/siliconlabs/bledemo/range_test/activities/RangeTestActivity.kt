package com.siliconlabs.bledemo.range_test.activities

import android.bluetooth.*
import android.content.DialogInterface
import android.content.IntentFilter
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.siliconlabs.bledemo.base.BaseActivity
import com.siliconlabs.bledemo.base.SelectDeviceDialog
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.range_test.fragments.RangeTestFragment
import com.siliconlabs.bledemo.range_test.fragments.RangeTestModeDialog
import com.siliconlabs.bledemo.range_test.models.RangeTestAdvertisementHandler
import com.siliconlabs.bledemo.range_test.models.RangeTestMode
import com.siliconlabs.bledemo.range_test.models.RangeTestValues
import com.siliconlabs.bledemo.range_test.presenters.RangeTestPresenter
import com.siliconlabs.bledemo.range_test.presenters.RangeTestPresenter.Controller
import com.siliconlabs.bledemo.range_test.presenters.RangeTestPresenter.RangeTestView
import com.siliconlabs.bledemo.utils.BLEUtils
import com.siliconlabs.bledemo.utils.BLEUtils.setNotificationForCharacteristic
import com.siliconlabs.bledemo.bluetooth.ble.*
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import kotlinx.android.synthetic.main.actionbar.*
import kotlinx.android.synthetic.main.activity_range_test.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.abs

/**
 * @author Comarch S.A.
 */
class RangeTestActivity : BaseActivity(), Controller, BluetoothStateReceiver.BluetoothStateListener {

    private var activeDeviceId = 1
    private var deviceInfo1: BluetoothDeviceInfo? = null
    private var deviceInfo2: BluetoothDeviceInfo? = null
    private var test1Running: Boolean = false
    private var test2Running: Boolean = false
    private var mode1: RangeTestMode? = null
    private var mode2: RangeTestMode? = null

    private var advertisementHandler: RangeTestAdvertisementHandler? = null
    private var stateReceiver: BluetoothStateReceiver? = null
    private var binding: BluetoothService.Binding? = null
    private var service: BluetoothService? = null

    private var presenter = RangeTestPresenter()
    private var processor = GattProcessor()

    private var setupData = true
    private var testWasRunning = false
    private var timerStarted = false
    private var reconnecting = false

    private var retryAttempts = 0
    private var reconnectionRetry = 0
    private val mainHandler = Handler(Looper.getMainLooper())

    private val switchDeviceRunnable = Runnable {
        service?.getConnectedGatt(getDeviceInfo()?.address)?.connect()
        initTest(service)
    }

    private val reconnectionRunnable = Runnable {
        val gatt: BluetoothGatt? = service?.getConnectedGatt(getDeviceInfo()?.address)
        val device: BluetoothDevice?

        if (service == null) {
            handleConnectionError()
            return@Runnable
        }

        if (gatt == null) {
            handleConnectionError()
            return@Runnable
        }

        device = gatt.device
        if (device == null) {
            handleConnectionError()
            return@Runnable
        }

        Toast.makeText(this@RangeTestActivity, R.string.demo_range_toast_reconnecting, Toast.LENGTH_SHORT).show()

        if (!service?.connectGatt(device, false, null)!!) {
            handleConnectionError()
        }
    }


    private var discoveryReadyLatch = CountDownLatch(1)
    private var txSentPackets = 0

    private val txUpdateTimer: CountDownTimer = object : CountDownTimer(Long.MAX_VALUE, 1000 / 13) {
        override fun onTick(millisUntilFinished: Long) {
            ++txSentPackets
            presenter.onPacketSentUpdated(txSentPackets)
        }

        override fun onFinish() {
            // nothing
        }
    }

    private fun retryConnectionAttempt() {
        retryAttempts++
        runOnUiThread {
            Handler(Looper.getMainLooper()).postDelayed({
                service?.connectGatt(getDeviceInfo()?.device!!, false, timeoutGattCallback)
            }, 1000)
        }
    }

    private val timeoutGattCallback = object : TimeoutGattCallback() {

        override fun onTimeout() {
            super.onTimeout()

            runOnUiThread {
                showMessage(R.string.toast_connection_timed_out)
                dismissModalDialog()
                connectToSecondDevice()
            }
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothAdapter.STATE_CONNECTED) {
                    retryAttempts = 0
                    initTest(service)
                }

            } else if (status == 133 && retryAttempts < RECONNECTION_RETRIES) {
                retryConnectionAttempt()
            } else {
                runOnUiThread {
                    showMessage(ErrorCodes.getFailedConnectingToDeviceMessage(getDeviceInfo()?.name, status))
                    dismissModalDialog()
                    connectToSecondDevice()
                }
            }
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_range_test)
        prepareToolbar()

        selectTab(tv_device1_tab)
        changeDevice(tv_device1_tab, 1)

        tv_device1_tab.setOnClickListener {
            changeDevice(tv_device1_tab, 1)
        }

        tv_device2_tab.setOnClickListener {
            changeDevice(tv_device2_tab, 2)
        }

        txUpdateTimer.cancel()

        stateReceiver = BluetoothStateReceiver(this)
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(stateReceiver, filter)
    }

    private fun getDeviceInfo(): BluetoothDeviceInfo? {
        return if (activeDeviceId == 1) deviceInfo1
        else deviceInfo2
    }

    private fun getDeviceInfoAt(which: Int): BluetoothDeviceInfo? {
        return if (which == 1) deviceInfo1
        else deviceInfo2
    }

    private fun switchCurrentDevice() {
        activeDeviceId = if (activeDeviceId == 1) 2
        else 1
    }

    private fun setDeviceInfo(deviceInfo: BluetoothDeviceInfo?) {
        if (activeDeviceId == 1) deviceInfo1 = deviceInfo
        else deviceInfo2 = deviceInfo
    }

    private fun setDeviceInfoAt(which: Int, deviceInfo: BluetoothDeviceInfo?) {
        if (which == 1) deviceInfo1 = deviceInfo
        else deviceInfo2 = deviceInfo
    }

    private fun getMode(): RangeTestMode? {
        return if (activeDeviceId == 1) mode1
        else mode2
    }

    private fun setMode(mode: RangeTestMode?) {
        if (activeDeviceId == 1) mode1 = mode
        else mode2 = mode
    }

    private fun setModeAt(which: Int, mode: RangeTestMode?) {
        if (which == 1) mode1 = mode
        else mode2 = mode
    }

    private fun getTestRunning(): Boolean {
        return if (activeDeviceId == 1) test1Running
        else test2Running
    }

    private fun setTestRunning(running: Boolean) {
        if (activeDeviceId == 1) test1Running = running
        else test2Running = running
    }

    private fun setTestRunningAt(which: Int, running: Boolean) {
        if (which == 1) test1Running = running
        else test2Running = running
    }

    private fun resetDeviceAt(which: Int) {
        setDeviceInfoAt(which, null)
        setModeAt(which, null)
        setTestRunningAt(which, false)
    }

    private fun changeDevice(tvTab: TextView, which: Int) {
        activeDeviceId = which

        if (getDeviceInfo() == null) {
            val connectType = BluetoothService.GattConnectType.RANGE_TEST
            val selectDeviceDialog = SelectDeviceDialog.newDialog(R.string.title_Range_Test, R.string.main_menu_description_range_test, null, connectType)
            selectDeviceDialog.setCallback(object : SelectDeviceDialog.Callback {
                override fun getBluetoothDeviceInfo(info: BluetoothDeviceInfo?) {
                    removeRangeTestFragment()
                    doInitialSetupForDeviceChange()

                    runOnUiThread {
                        showModalDialog(ConnectionStatus.CONNECTING, DialogInterface.OnCancelListener {
                            getDeviceInfo()?.address?.let {
                                service?.clearConnectedGatt()
                                resetDeviceAt(which)
                            }
                            tvTab.text = "No Device"
                        })
                        tvTab.text = info?.device?.name
                        selectTab(tvTab)
                    }

                    setDeviceInfo(info)

                    binding = object : BluetoothService.Binding(this@RangeTestActivity) {
                        override fun onBound(service: BluetoothService?) {
                            this@RangeTestActivity.service = service
                            service?.connectGatt(getDeviceInfo()?.device!!, false, timeoutGattCallback)
                        }
                    }
                    binding?.bind()
                }

                override fun onCancel() {
                    switchCurrentDevice()
                }
            })
            supportFragmentManager
                    .beginTransaction()
                    .add(selectDeviceDialog, "tag_select_device")
                    .commitAllowingStateLoss()
        } else {
            removeRangeTestFragment()
            doInitialSetupForDeviceChange()

            runOnUiThread {
                showModalDialog(ConnectionStatus.READING_DEVICE_STATE)
                selectTab(tvTab)
            }

            mainHandler.apply {
                removeCallbacks(switchDeviceRunnable)
                postDelayed(switchDeviceRunnable, SWITCH_DEVICE_TIMEOUT_MS)
            }
        }
    }

    private fun doInitialSetupForDeviceChange() {
        presenter = RangeTestPresenter()
        processor = GattProcessor()
        advertisementHandler?.stopListening()
        advertisementHandler = null
    }

    private fun prepareToolbar() {
        setSupportActionBar(toolbar)
        findViewById<View>(R.id.iv_go_back).setOnClickListener { onBackPressed() }
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing) {
            service?.registerGattCallback(false, null)
            binding?.unbind()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        stateReceiver?.let { unregisterReceiver(it) }

        service?.clearAllGatts()

        advertisementHandler?.stopListening()
        advertisementHandler = null
    }

    override fun setView(view: RangeTestView?) {
        presenter.setView(view)

        if (view == null) {
            handleTxTimer(false)
        }
    }

    override fun initTestMode(mode: RangeTestMode?) {
        initTestView(mode, true)
    }

    override fun cancelTestMode() {
        finish()
    }

    override fun updateTxPower(power: Int) {
        withGatt(SetValueGattAction(GattCharacteristic.RangeTestTxPower, power))
    }

    override fun updatePayloadLength(length: Int) {
        withGatt(SetValueGattAction(GattCharacteristic.RangeTestPayload, length))
    }

    override fun updateMaWindowSize(size: Int) {
        withGatt(SetValueGattAction(GattCharacteristic.RangeTestMaSize, size))
    }

    override fun updateChannel(channel: Int) {
        withGatt(SetValueGattAction(GattCharacteristic.RangeTestChannel, channel))
    }

    override fun updatePacketCount(count: Int) {
        withGatt(SetValueGattAction(GattCharacteristic.RangeTestPacketsRequired, count))
    }

    override fun updateRemoteId(id: Int) {
        withGatt(SetValueGattAction(GattCharacteristic.RangeTestDestinationId, id))
    }

    override fun updateSelfId(id: Int) {
        withGatt(SetValueGattAction(GattCharacteristic.RangeTestSourceId, id))
    }

    override fun updateUartLogEnabled(enabled: Boolean) {
        withGatt(SetValueGattAction(GattCharacteristic.RangeTestLog, enabled))
    }

    override fun toggleRunningState() {
        withGatt(SetValueGattAction(GattCharacteristic.RangeTestIsRunning, !getTestRunning()))
    }

    override fun updatePhyConfig(id: Int) {
        withGatt(SetValueGattAction(GattCharacteristic.RangePhyConfig, id))
    }

    private fun initTestView(mode: RangeTestMode?, writeCharacteristic: Boolean) {
        setMode(mode)

        if (writeCharacteristic) {
            withGatt(SetValueGattAction(GattCharacteristic.RangeTestRadioMode, mode?.code!!))
        }

        showRangeTestFragment(getMode())
    }

    private fun connectToSecondDevice() {

        // Set previous device to null
        resetDeviceAt(activeDeviceId)

        if (deviceInfo1 == null && deviceInfo2 == null) {
            if (!isFinishing) finish()
        }

        // Switch to second device
        switchCurrentDevice()
        val tvNewTab = if (activeDeviceId == 1) tv_device1_tab else tv_device2_tab
        val tvOldTab = if (activeDeviceId == 1) tv_device2_tab else tv_device1_tab

        selectTab(tvNewTab)
        tvOldTab.text = "No Device"

        changeDevice(tvNewTab, activeDeviceId)
    }

    private fun initTest(service: BluetoothService?) {
        val gatt: BluetoothGatt? = service?.getConnectedGatt(getDeviceInfo()?.address)
        val device: BluetoothDevice?

        if (gatt == null || !service.isGattConnected(getDeviceInfo()?.address)) {
            runOnUiThread {
                Toast.makeText(this@RangeTestActivity, "Disconnected: ${getDeviceInfo()?.device?.name}", Toast.LENGTH_SHORT).show()
                connectToSecondDevice()
            }
        }

        device = gatt?.device
        if (device == null) {
            if (!isFinishing) finish()
            return
        }

        this.service = service

        advertisementHandler = object : RangeTestAdvertisementHandler(this, device.address) {
            override fun handleAdvertisementRecord(manufacturerData: Int, companyId: Int, structureType: Int, rssi: Int, packetCount: Int, packetReceived: Int) {
                if (getMode() == RangeTestMode.Rx && structureType == 0) {
                    cancelReconnect()
                    presenter.onTestDataReceived(rssi, packetCount, packetReceived)
                } else if (!reconnecting) {
                    scheduleReconnect()
                }
            }
        }

        setupData = true
        service?.registerGattCallback(false, processor)
        service?.refreshGattServices()
    }

    private fun scheduleReconnect() {
        reconnecting = true
        mainHandler.removeCallbacks(reconnectionRunnable)
        mainHandler.postDelayed(reconnectionRunnable, RECONNECTION_DELAY_MS)
    }

    private fun cancelReconnect() {
        reconnecting = false
        mainHandler.removeCallbacks(reconnectionRunnable)
    }

    private fun withGatt(action: GattAction) {
        var gatt: BluetoothGatt? = service?.getConnectedGatt(getDeviceInfo()?.address)

        if (service == null || !service?.isGattConnected()!!) {
            handleConnectionError()
            return
        }

        if (gatt == null) {
            handleConnectionError()
            return
        }

        if (discoveryReadyLatch.count > 0) {
            try {
                discoveryReadyLatch.await(8, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                handleConnectionError()
                return
            }
        }

        action.run(gatt)
    }

    private fun handleConnectionError() {
        runOnUiThread {
            Toast.makeText(this@RangeTestActivity, R.string.demo_range_toast_error, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showModeSelectionDialog() {
        val modeDialog = RangeTestModeDialog()
        modeDialog.show(supportFragmentManager, "RANGE_TEST_MODE")
    }

    private fun removeRangeTestFragment() {
        val fragment = supportFragmentManager.findFragmentByTag("tag_show_range_test_fragment")
        fragment?.let {
            supportFragmentManager
                    .beginTransaction()
                    .remove(fragment)
                    .commit()
        }
    }

    private fun showRangeTestFragment(mode: RangeTestMode?) {
        val arguments = Bundle()
        arguments.putInt(RangeTestFragment.ARG_MODE, mode?.code!!)

        val fragment = RangeTestFragment()
        fragment.arguments = arguments

        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment, fragment, "tag_show_range_test_fragment")
                .commit()
    }

    private fun getGenericAccessCharacteristic(characteristic: GattCharacteristic): BluetoothGattCharacteristic? {
        val gattService = service?.connectedGatt?.getService(GattService.GenericAccess.number)
        return gattService?.getCharacteristic(characteristic.uuid)
    }

    private fun getDeviceInformationCharacteristic(characteristic: GattCharacteristic): BluetoothGattCharacteristic? {
        val gattService = service?.connectedGatt?.getService(GattService.DeviceInformation.number)
        return gattService?.getCharacteristic(characteristic.uuid)
    }

    private fun getRangeTestCharacteristic(characteristic: GattCharacteristic): BluetoothGattCharacteristic? {
        val gattService = service?.connectedGatt?.getService(GattService.RangeTestService.number)
        return gattService?.getCharacteristic(characteristic.uuid)
    }

    private fun handleTxTimer(running: Boolean) {
        if (running) {
            txSentPackets = 0
            timerStarted = true
            txUpdateTimer.cancel()
            txUpdateTimer.start()
        } else {
            timerStarted = false
            txUpdateTimer.cancel()
        }
    }

    override fun onBluetoothStateChanged(enabled: Boolean) {
        if (!enabled) {
            finish()
        }
    }

    private class GattCommand {

        internal enum class Type {
            Read, Write, Subscribe, ReadDescriptor
        }

        val type: Type
        val gatt: BluetoothGatt?
        val characteristic: BluetoothGattCharacteristic?
        var descriptor: BluetoothGattDescriptor? = null

        constructor(type: Type, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            this.type = type
            this.gatt = gatt
            this.characteristic = characteristic
        }

        constructor(type: Type, gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, descriptor: BluetoothGattDescriptor) {
            this.type = type
            this.gatt = gatt
            this.characteristic = characteristic
            this.descriptor = descriptor
        }
    }

    private inner class GattProcessor : TimeoutGattCallback() {
        private val commands: Queue<GattCommand> = LinkedList()
        private val lock: Lock = ReentrantLock()
        private var processing = false

        private fun queueRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic?) {
            queue(GattCommand(GattCommand.Type.Read, gatt, characteristic))
        }

        private fun queueReadDescriptor(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, descriptor: BluetoothGattDescriptor) {
            queue(GattCommand(GattCommand.Type.ReadDescriptor, gatt, characteristic, descriptor))
        }

        fun queueWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) {
            queue(GattCommand(GattCommand.Type.Write, gatt, characteristic))
        }

        private fun queueSubscribe(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic?) {
            queue(GattCommand(GattCommand.Type.Subscribe, gatt, characteristic))
        }

        private fun addToQueue(gattCommand: GattCommand) {
            commands.add(gattCommand)
        }

        private fun queue(command: GattCommand) {
            lock.lock()
            try {
                commands.add(command)
                if (!processing) {
                    processNextCommand()
                }
            } finally {
                lock.unlock()
            }
        }

        private fun clearCommandQueue() {
            lock.lock()
            try {
                commands.clear()
            } finally {
                lock.unlock()
            }
        }

        private fun processNextCommand() {
            var success = false

            val command = commands.poll()

            if (command?.gatt != null && command.characteristic != null) {
                val gatt = command.gatt
                val characteristic = command.characteristic

                success = when (command.type) {
                    GattCommand.Type.Read -> gatt.readCharacteristic(characteristic)
                    GattCommand.Type.Write -> gatt.writeCharacteristic(characteristic)
                    GattCommand.Type.Subscribe -> {
                        val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid)
                        val gattService = GattService.fromUuid(characteristic.service.uuid)
                        setNotificationForCharacteristic(gatt, gattService, gattCharacteristic, BLEUtils.Notifications.INDICATE)
                    }
                    GattCommand.Type.ReadDescriptor -> gatt.readDescriptor(command.descriptor)
                }
            }

            processing = success

            dismissModalDialogWhenSetupCompleted()
        }

        private fun dismissModalDialogWhenSetupCompleted() {
            if (setupData && commands.isEmpty()) {
                setupData = false
                runOnUiThread { dismissModalDialog() }
            }
        }

        private fun handleCommandProcessed() {
            lock.lock()
            try {
                if (commands.isEmpty()) {
                    processing = false
                } else {
                    processNextCommand()
                }
            } finally {
                lock.unlock()
            }
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)


            testWasRunning = false

            if (reconnecting && newState != BluetoothProfile.STATE_CONNECTING && newState != BluetoothProfile.STATE_CONNECTED) {
                if (reconnectionRetry >= RECONNECTION_RETRIES) {
                    handleConnectionError()
                } else {
                    ++reconnectionRetry
                    scheduleReconnect()
                }
            }

            if (getMode() == RangeTestMode.Rx && status == 19 && newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (isFinishing) {
                    return
                }

                synchronized(this@RangeTestActivity) {
                    if (advertisementHandler != null) {
                        advertisementHandler?.startListening()
                    } else {
                        handleConnectionError()
                        return
                    }
                }

                setTestRunning(true)
                setKeepScreenOn(true)
                presenter.onRunningStateUpdated(true)
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                reconnecting = false
                reconnectionRetry = 0
                cancelReconnect()

                if (getMode() == RangeTestMode.Rx) {
                    synchronized(this@RangeTestActivity) {
                        advertisementHandler?.stopListening()
                    }

                    setTestRunning(false)
                    setKeepScreenOn(false)
                    presenter.onRunningStateUpdated(false)
                }

                clearCommandQueue()
                discoveryReadyLatch = CountDownLatch(1)
                service?.refreshGattServices()
            }

            if (status != 19 && newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread {
                    dismissModalDialog()
                    if (service?.connectedGatt == gatt) {
                        showMessage(ErrorCodes.getDeviceDisconnectedMessage(getDeviceInfo()?.name, status))
                        connectToSecondDevice()
                    } else {
                        val resetDeviceId = if (activeDeviceId == 1) 2 else 1
                        showMessage(ErrorCodes.getDeviceDisconnectedMessage(getDeviceInfoAt(resetDeviceId)?.name, status))

                        resetDeviceAt(resetDeviceId)

                        if (resetDeviceId == 1) tv_device1_tab.text = "No Device"
                        else tv_device2_tab.text = "No Device"
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)

            if (status != BluetoothGatt.GATT_SUCCESS) {
                handleConnectionError()
                return
            }

            discoveryReadyLatch.countDown()

            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestIsRunning)))
            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestRadioMode)))

            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getGenericAccessCharacteristic(GattCharacteristic.DeviceName)))
            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getDeviceInformationCharacteristic(GattCharacteristic.ModelNumberString)))

            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getRangeTestCharacteristic(GattCharacteristic.RangePhyList)))
            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getRangeTestCharacteristic(GattCharacteristic.RangePhyConfig)))
            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestTxPower)))
            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPacketsSend)))
            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestDestinationId)))
            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestSourceId)))
            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPacketsReceived)))
            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPacketsCount)))
            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPacketsRequired)))
            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPER)))
            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestMA)))
            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestChannel)))
            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPayload)))
            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestMaSize)))
            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestLog)))

            addToQueue(GattCommand(GattCommand.Type.Subscribe, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestIsRunning)))
            addToQueue(GattCommand(GattCommand.Type.Subscribe, gatt, getRangeTestCharacteristic(GattCharacteristic.RangePhyConfig)))
            addToQueue(GattCommand(GattCommand.Type.Subscribe, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestTxPower)))
            addToQueue(GattCommand(GattCommand.Type.Subscribe, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPacketsSend)))
            addToQueue(GattCommand(GattCommand.Type.Subscribe, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestDestinationId)))
            addToQueue(GattCommand(GattCommand.Type.Subscribe, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestSourceId)))
            addToQueue(GattCommand(GattCommand.Type.Subscribe, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPacketsReceived)))
            addToQueue(GattCommand(GattCommand.Type.Subscribe, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPacketsCount)))
            addToQueue(GattCommand(GattCommand.Type.Subscribe, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPacketsRequired)))
            addToQueue(GattCommand(GattCommand.Type.Subscribe, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPER)))
            addToQueue(GattCommand(GattCommand.Type.Subscribe, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestMA)))
            addToQueue(GattCommand(GattCommand.Type.Subscribe, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestChannel)))
            addToQueue(GattCommand(GattCommand.Type.Subscribe, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestRadioMode)))
            addToQueue(GattCommand(GattCommand.Type.Subscribe, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPayload)))
            addToQueue(GattCommand(GattCommand.Type.Subscribe, gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestMaSize)))

            // Add to queue and start executing
            queueSubscribe(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestLog))

            if (setupData) runOnUiThread { showModalDialog(ConnectionStatus.READING_DEVICE_STATE) }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)

            if (status != BluetoothGatt.GATT_SUCCESS) {
                handleConnectionError()
                return
            }

            handleCommandProcessed()

            val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid)

            if (gattCharacteristic != null) {
                updatePresenter(gatt, characteristic, gattCharacteristic)

                if (gattCharacteristic === GattCharacteristic.RangeTestTxPower || gattCharacteristic === GattCharacteristic.RangeTestPayload || gattCharacteristic === GattCharacteristic.RangeTestMaSize) {
                    val descriptors = characteristic.descriptors
                    if (descriptors.size > 1) {
                        queueReadDescriptor(gatt, characteristic, descriptors[descriptors.size - 1])
                    }
                }
            }
        }

        override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            super.onDescriptorRead(gatt, descriptor, status)

            if (status != BluetoothGatt.GATT_SUCCESS) {
                handleConnectionError()
                return
            }

            handleCommandProcessed()

            val gattCharacteristic = GattCharacteristic.fromUuid(descriptor.characteristic.uuid)

            gattCharacteristic?.let {
                updatePresenter(descriptor, it)
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)

            if (status != BluetoothGatt.GATT_SUCCESS) {
                handleConnectionError()
                return
            }

            handleCommandProcessed()

            val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid)

            if (getMode() == RangeTestMode.Tx && gattCharacteristic != null) {
                when (gattCharacteristic) {
                    GattCharacteristic.RangeTestIsRunning -> {
                        setTestRunning(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) != 0)
                        setKeepScreenOn(getTestRunning())
                        handleTxTimer(getTestRunning())
                        presenter.onRunningStateUpdated(getTestRunning())
                    }
                    GattCharacteristic.RangeTestPacketsRequired -> {
                        val packetsRequired = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)
                        val repeat = packetsRequired == RangeTestValues.PACKET_COUNT_REPEAT
                        presenter.onPacketRequiredUpdated(packetsRequired)
                        presenter.onPacketCountRepeatUpdated(repeat)
                    }

                    else -> {
                    }
                }
            }

            if (gattCharacteristic === GattCharacteristic.RangePhyConfig) {
                queueRead(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPayload))
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)

            if (status != BluetoothGatt.GATT_SUCCESS) {
                handleConnectionError()
                return
            }

            handleCommandProcessed()
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)

            val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid)

            gattCharacteristic?.let {
                updatePresenter(gatt, characteristic, it)
            }
        }

        private fun updatePresenter(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, gattCharacteristic: GattCharacteristic) {
            if (gattCharacteristic.format != 0) {
                when (gattCharacteristic.format) {
                    BluetoothGattCharacteristic.FORMAT_UINT8,
                    BluetoothGattCharacteristic.FORMAT_UINT16,
                    BluetoothGattCharacteristic.FORMAT_UINT32,
                    BluetoothGattCharacteristic.FORMAT_SINT8,
                    BluetoothGattCharacteristic.FORMAT_SINT16,
                    BluetoothGattCharacteristic.FORMAT_SINT32 ->
                        Log.d("RangeTest", "Update: " + gattCharacteristic.name + " -> value: " + characteristic.getIntValue(gattCharacteristic.format, 0))
                    else -> {
                    }
                }
            }

            when (gattCharacteristic) {
                GattCharacteristic.DeviceName -> {
                    val deviceName = characteristic.getStringValue(0)
                    presenter.onDeviceNameUpdated(deviceName)
                }
                GattCharacteristic.ModelNumberString -> {
                    var modelNumber = characteristic.getStringValue(0)

                    val patternDuplicateOpn = "opn\\[.*]"
                    modelNumber = modelNumber.replaceFirst(patternDuplicateOpn.toRegex(), "")

                    presenter.onModelNumberUpdated(modelNumber)
                }
                GattCharacteristic.RangeTestDestinationId -> {
                    val remoteId = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                    presenter.onRemoteIdUpdated(remoteId)
                }
                GattCharacteristic.RangeTestSourceId -> {
                    val selfId = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                    presenter.onSelfIdUpdated(selfId)
                }
                GattCharacteristic.RangeTestPacketsReceived -> {
                    val uInt16Max = 0xFFFF
                    val packetsReceived = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)
                    presenter.onPacketReceivedUpdated(if (packetsReceived == uInt16Max) 0 else packetsReceived)
                }
                GattCharacteristic.RangeTestPacketsSend -> {
                    val packetsSent = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)
                    txSentPackets = packetsSent
                    if (getTestRunning()) {
                        if (!timerStarted && packetsSent > 0) {
                            handleTxTimer(true)
                        }
                        presenter.onPacketSentUpdated(packetsSent)
                    } else if (testWasRunning) {
                        testWasRunning = false
                        presenter.onPacketSentUpdated(packetsSent)
                    }
                }
                GattCharacteristic.RangeTestPacketsCount -> {
                    val packetsCount = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)
                    presenter.onPacketCountUpdated(packetsCount)
                }
                GattCharacteristic.RangeTestPacketsRequired -> {
                    val packetsRequired = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)
                    var repeat = packetsRequired == RangeTestValues.PACKET_COUNT_REPEAT

                    presenter.onPacketRequiredUpdated(packetsRequired)
                    presenter.onPacketCountRepeatUpdated(repeat)
                }
                GattCharacteristic.RangeTestPER -> {
                    val per = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)
                    presenter.onPerUpdated(per / 10f)
                }
                GattCharacteristic.RangeTestMA -> {
                    val ma = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)
                    presenter.onMaUpdated(ma / 10f)
                }
                GattCharacteristic.RangeTestChannel -> {
                    val channel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)
                    presenter.onChannelNumberUpdated(channel)
                }
                GattCharacteristic.RangeTestRadioMode -> {
                    val radioMode = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                    val rangeTestMode = RangeTestMode.fromCode(radioMode)

                    if (service?.connectedGatt == gatt) {
                        if (getMode() == null && getTestRunning()) {
                            initTestView(rangeTestMode, false)
                            presenter.onRunningStateUpdated(getTestRunning())
                        } else if (getMode() != null) {
                            initTestView(rangeTestMode, false)
                        }
                    } else {
                        setModeAt(if (activeDeviceId == 1) 2 else 1, rangeTestMode)
                    }
                }
                GattCharacteristic.RangeTestTxPower -> {
                    val txPower = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, 0)
                    presenter.onTxPowerUpdated(txPower)
                }
                GattCharacteristic.RangeTestPayload -> {
                    val payloadLength = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                    presenter.onPayloadLengthUpdated(payloadLength)
                }
                GattCharacteristic.RangeTestMaSize -> {
                    val maWindowSize = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                    presenter.onMaWindowSizeUpdated(maWindowSize)
                }
                GattCharacteristic.RangeTestLog -> {
                    val log = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                    presenter.onUartLogEnabledUpdated(log != 0)
                }
                GattCharacteristic.RangeTestIsRunning -> {
                    setTestRunning(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) != 0)
                    if (getMode() == null && !getTestRunning()) {
                        showModeSelectionDialog()
                    } else if (getMode() == RangeTestMode.Tx) {
                        handleTxTimer(getTestRunning())
                        if (!getTestRunning() && service != null) {
                            val gatt = service?.connectedGatt
                            if (gatt != null) {
                                testWasRunning = true
                                queueRead(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPacketsSend))
                            }
                        }
                        presenter.onRunningStateUpdated(getTestRunning())
                    }
                }
                GattCharacteristic.RangePhyConfig -> {
                    val phyConfig = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                    presenter.onPhyConfigUpdated(phyConfig)

                    val gatt = service?.connectedGatt
                    if (gatt != null) {
                        queueRead(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPayload))
                    }
                }
                GattCharacteristic.RangePhyList -> {
                    val phyConfigs = LinkedHashMap<Int, String>()

                    val phyConfigsList = characteristic.getStringValue(0)
                    val phyConfigsListSplit = phyConfigsList.split(",".toRegex()).toTypedArray()

                    for (phyConfigPair in phyConfigsListSplit) {
                        val phyConfigPairSplit = phyConfigPair.split(":".toRegex()).toTypedArray()
                        if (phyConfigPairSplit.size == 2) {
                            val id = Integer.valueOf(phyConfigPairSplit[0])
                            val name = phyConfigPairSplit[1]
                            phyConfigs[id] = name
                        }
                    }

                    presenter.onPhyMapUpdated(phyConfigs)
                }
            }
        }

        private fun updatePresenter(descriptor: BluetoothGattDescriptor, gattCharacteristic: GattCharacteristic) {
            if (gattCharacteristic.format != 0) {
                when (gattCharacteristic.format) {
                    BluetoothGattCharacteristic.FORMAT_UINT8,
                    BluetoothGattCharacteristic.FORMAT_UINT16,
                    BluetoothGattCharacteristic.FORMAT_UINT32,
                    BluetoothGattCharacteristic.FORMAT_SINT8,
                    BluetoothGattCharacteristic.FORMAT_SINT16,
                    BluetoothGattCharacteristic.FORMAT_SINT32 ->
                        Log.d("RangeTest", "Update: " + gattCharacteristic.name + " -> descriptor: " + descriptor.toString())
                    else -> {
                    }
                }
            }

            val descriptorValues = descriptor.value

            if (descriptorValues.size % 2 != 0) {
                return
            }

            val firstValueArray = Arrays.copyOfRange(descriptorValues, 0, descriptorValues.size / 2)
            val secondValueArray = Arrays.copyOfRange(descriptorValues, descriptorValues.size / 2, descriptorValues.size)
            val wrappedFirstValue = ByteBuffer.wrap(firstValueArray).order(ByteOrder.LITTLE_ENDIAN)
            val wrappedSecondValue = ByteBuffer.wrap(secondValueArray).order(ByteOrder.LITTLE_ENDIAN)
            val rangeFrom: Int
            val rangeTo: Int

            when (gattCharacteristic) {
                GattCharacteristic.RangeTestTxPower -> {
                    rangeFrom = wrappedFirstValue.short.toInt()
                    rangeTo = wrappedSecondValue.short.toInt()
                    presenter.onTxPowerRangeUpdated(rangeFrom, rangeTo)
                }
                GattCharacteristic.RangeTestPayload -> {
                    rangeFrom = firstValueArray[0].toInt()
                    rangeTo = secondValueArray[0].toInt()
                    presenter.onPayloadLengthRangeUpdated(rangeFrom, rangeTo)
                }
                GattCharacteristic.RangeTestMaSize -> {
                    rangeFrom = abs(firstValueArray[0].toInt())
                    rangeTo = abs(secondValueArray[0].toInt())
                    presenter.onMaWindowSizeRangeUpdated(rangeFrom, rangeTo)
                }
                else -> {
                }
            }
        }
    }

    private fun setKeepScreenOn(enabled: Boolean) {
        runOnUiThread {
            if (enabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    private interface GattAction {
        fun run(gatt: BluetoothGatt?)
    }

    private inner class SetValueGattAction : GattAction {

        private val characteristic: GattCharacteristic
        private val value: Int

        internal constructor(characteristic: GattCharacteristic, value: Boolean) {
            this.characteristic = characteristic
            this.value = if (value) 1 else 0
        }

        internal constructor(characteristic: GattCharacteristic, value: Int) {
            this.characteristic = characteristic
            this.value = value
        }

        override fun run(gatt: BluetoothGatt?) {
            val characteristic = getRangeTestCharacteristic(characteristic) ?: return

            val currentValue = getCurrentValueOf(characteristic)
            if (currentValue == null || currentValue == value) {
                return
            }

            writeValueFor(gatt, characteristic)
        }

        private fun getCurrentValueOf(characteristic: BluetoothGattCharacteristic): Int? {
            val value = characteristic.value

            return if (value == null || value.isEmpty()) {
                null
            } else characteristic.getIntValue(this.characteristic.format, 0)
        }

        private fun writeValueFor(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) {
            characteristic.setValue(value, this.characteristic.format, 0)
            processor.queueWrite(gatt, characteristic)
        }
    }

    private fun selectTab(tvTab: TextView) {
        setTabUnselected(tv_device1_tab)
        setTabUnselected(tv_device2_tab)
        setTabSelected(tvTab)
    }

    private fun setTabSelected(textView: TextView?) {
        textView?.background = ContextCompat.getDrawable(this, R.drawable.btn_rounded_white)
        textView?.setTextColor(ContextCompat.getColor(this, R.color.silabs_red))
        textView?.typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }

    private fun setTabUnselected(textView: TextView?) {
        textView?.background = ContextCompat.getDrawable(this, R.drawable.btn_rounded_red_dark)
        textView?.setTextColor(ContextCompat.getColor(this, R.color.silabs_white))
        textView?.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    companion object {
        private const val RECONNECTION_RETRIES = 3
        private const val RECONNECTION_DELAY_MS: Long = 2000
        private const val SWITCH_DEVICE_TIMEOUT_MS: Long = 1500
    }
}