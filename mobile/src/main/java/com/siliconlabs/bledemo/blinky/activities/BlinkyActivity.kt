package com.siliconlabs.bledemo.blinky.activities

import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.Base.BaseActivity
import com.siliconlabs.bledemo.Base.SelectDeviceDialog
import com.siliconlabs.bledemo.Bluetooth.BLE.GattCharacteristic
import com.siliconlabs.bledemo.Bluetooth.BLE.GattService
import com.siliconlabs.bledemo.Bluetooth.BLE.TimeoutGattCallback
import com.siliconlabs.bledemo.Bluetooth.Services.BluetoothService
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.Views.HorizontalShadow
import com.siliconlabs.bledemo.blinky.models.LightState
import com.siliconlabs.bledemo.blinky.viewmodels.BlinkyViewModel
import com.siliconlabs.bledemo.thunderboard.base.StatusFragment
import com.siliconlabs.bledemo.thunderboard.model.StatusEvent
import com.siliconlabs.bledemo.thunderboard.model.ThunderBoardDevice
import com.siliconlabs.bledemo.utils.BLEUtils
import com.siliconlabs.bledemo.utils.Notifications
import kotlinx.android.synthetic.main.actionbar.*
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class BlinkyActivity : BaseActivity() {
    private lateinit var viewModel: BlinkyViewModel
    private lateinit var bluetoothBinding: BluetoothService.Binding
    private var service: BluetoothService? = null
    private val processor = GattProcessor()
    private var isDeviceThunderboard = false

    private val bluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_OFF) finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blinky)
        viewModel = ViewModelProvider(this).get(BlinkyViewModel::class.java)

        val boardType = intent.extras?.getString(SelectDeviceDialog.MODEL_TYPE_EXTRA, "Unknown") ?: "Unknown"
        if (boardType == "BRD4184A" || boardType == "BRD4184B") isDeviceThunderboard = true

        prepareToolbar()
        bindBluetoothService()
        registerBluetoothReceiver()
        observeChanges()
    }

    private fun bindBluetoothService() {
        bluetoothBinding = object : BluetoothService.Binding(this) {
            override fun onBound(service: BluetoothService?) {
                service?.apply {
                    this@BlinkyActivity.service = this
                    if (!isGattConnected()) {
                        finish()
                    } else {
                        if (isDeviceThunderboard) {
                            this.thunderboardDevice = ThunderBoardDevice(service.connectedGatt?.device!!)
                            showPowerSourceBar()
                            loadStatusFragment()
                            showConnectionState()
                        }

                        registerGattCallback(true, processor)
                        discoverGattServices()
                    }
                }
            }
        }
        bluetoothBinding.bind()
    }

    private fun observeChanges() {
        viewModel.lightState.observe(this, Observer { state ->
            when (state) {
                LightState.TOGGLING_ON -> service?.connectedGatt?.let { processor.switchLightOn(it) }
                LightState.TOGGLING_OFF -> service?.connectedGatt?.let { processor.switchLightOff(it) }
                else -> {
                }
            }
        })
    }

    private fun getBlinkyCharacteristic(characteristic: GattCharacteristic): BluetoothGattCharacteristic? {
        val gattService = service?.connectedGatt?.getService(GattService.BlinkyExample.number)
        return gattService?.getCharacteristic(characteristic.uuid)
    }

    private fun getFirmwareRevisionCharacteristic(): BluetoothGattCharacteristic? {
        val gattService = service?.connectedGatt?.getService(GattService.DeviceInformation.number)
        return gattService?.getCharacteristic(GattCharacteristic.FirmwareRevision.uuid)
    }

    private fun getPowerSourceCharacteristic(): BluetoothGattCharacteristic? {
        val gattService = service?.connectedGatt?.getService(GattService.PowerSource.number)
        return gattService?.getCharacteristic(GattCharacteristic.PowerSource.uuid)
    }

    private fun getBatteryLevelCharacteristic(): BluetoothGattCharacteristic? {
        val gattService = service?.connectedGatt?.getService(GattService.BatteryService.number)
        return gattService?.getCharacteristic(GattCharacteristic.BatteryLevel.uuid)
    }

    private fun getDigitalCharacteristicWithNotify(): BluetoothGattCharacteristic? {
        val gattService = service?.connectedGatt?.getService(GattService.AutomationIo.number)
        gattService?.characteristics?.forEach { // There are 2 "Digital" characteristics on 4184A/B device
            if (it.uuid == GattCharacteristic.Digital.uuid &&
                    it.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0x10) return it
        }
        return null
    }

    private fun getDigitalCharacteristicWithWrite(): BluetoothGattCharacteristic? {
        val gattService = service?.connectedGatt?.getService(GattService.AutomationIo.number)
        gattService?.characteristics?.forEach {
            if (it.uuid == GattCharacteristic.Digital.uuid &&
                    it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE == 0x08) return it
        }
        return null
    }

    private fun registerBluetoothReceiver() {
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    private fun prepareToolbar() {
        setSupportActionBar(toolbar)
        findViewById<View>(R.id.iv_go_back).setOnClickListener { onBackPressed() }
    }

    private fun showPowerSourceBar() {
        findViewById<LinearLayout>(R.id.thunderboard_fragment_container).visibility = View.VISIBLE
        findViewById<HorizontalShadow>(R.id.thunderboard_shadow).visibility = View.VISIBLE
        findViewById<HorizontalShadow>(R.id.bottom_shadow).visibility = View.INVISIBLE
    }

    private fun loadStatusFragment() {
        val fragment = fragmentManager.findFragmentById(R.id.status_fragment)
        (fragment as StatusFragment).let {
            it.setBluetoothService(service)
            it.onPrepared()
            it.disableHeartbeatTimer()
        }
    }

    private fun showConnectionState() {
        service?.thunderboardDevice?.apply {
            state = BluetoothProfile.STATE_CONNECTED
            isServicesDiscovered = true
            service?.selectedDeviceStatusMonitor?.onNext(StatusEvent(this))
        }
    }

    override fun onBackPressed() {
        service?.clearConnectedGatt()
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(bluetoothReceiver)
        service?.clearConnectedGatt()
        bluetoothBinding.unbind()
    }

    private class GattCommand(val type: Type, val gatt: BluetoothGatt?, val characteristic: BluetoothGattCharacteristic?) {
        enum class Type {
            Read, Write, Notify
        }
    }

    private inner class GattProcessor : TimeoutGattCallback() {
        private val commands: Queue<GattCommand> = LinkedList()
        private val lock: Lock = ReentrantLock()
        private var processing = false

        private fun queueRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            queue(GattCommand(GattCommand.Type.Read, gatt, characteristic))
        }

        fun queueWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            queue(GattCommand(GattCommand.Type.Write, gatt, characteristic))
        }

        private fun queueNotify(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            queue(GattCommand(GattCommand.Type.Notify, gatt, characteristic))
        }

        fun switchLightOn(gatt: BluetoothGatt) {
            val characteristicToWrite =
                    if (isDeviceThunderboard) getDigitalCharacteristicWithWrite()
                    else getBlinkyCharacteristic(GattCharacteristic.LedControl)
            characteristicToWrite?.apply {
                value = byteArrayOf(1)
                queueWrite(gatt, this)
            }
        }

        fun switchLightOff(gatt: BluetoothGatt) {
            val characteristicToWrite =
                    if (isDeviceThunderboard) getDigitalCharacteristicWithWrite()
                    else getBlinkyCharacteristic(GattCharacteristic.LedControl)
            characteristicToWrite?.apply {
                value = byteArrayOf(0)
                queueWrite(gatt, this)
            }
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

        private fun processNextCommand() {
            var success = false
            val command = commands.poll()

            if (command?.gatt != null && command.characteristic != null) {
                val gatt = command.gatt
                val characteristic = command.characteristic

                success = when (command.type) {
                    GattCommand.Type.Read -> gatt.readCharacteristic(characteristic)
                    GattCommand.Type.Write -> gatt.writeCharacteristic(characteristic)
                    GattCommand.Type.Notify -> {
                        val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid)
                        val gattService = GattService.fromUuid(characteristic.service.uuid)
                        BLEUtils.setNotificationForCharacteristic(gatt, gattService, gattCharacteristic, Notifications.NOTIFY)
                    }
                }
            }
            processing = success
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
            if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                runOnUiThread { onDeviceDisconnect() }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            if (isDeviceThunderboard) {
                queueRead(gatt, getDigitalCharacteristicWithWrite())
                queueNotify(gatt, getDigitalCharacteristicWithNotify())

                queueRead(gatt, getFirmwareRevisionCharacteristic())
                queueRead(gatt, getPowerSourceCharacteristic())
                queueRead(gatt, getBatteryLevelCharacteristic())
            } else {
                queueRead(gatt, getBlinkyCharacteristic(GattCharacteristic.LedControl))
                queueNotify(gatt, getBlinkyCharacteristic(GattCharacteristic.ReportButton))
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            handleCommandProcessed()

            when (GattCharacteristic.fromUuid(characteristic.uuid)) {
                GattCharacteristic.LedControl,
                GattCharacteristic.Digital -> viewModel.handleLightStateChanges(characteristic)


                GattCharacteristic.FirmwareRevision -> {
                    val firmwareVersion = characteristic.getStringValue(0)
                    service?.thunderboardDevice?.let {
                        it.firmwareVersion = firmwareVersion
                        service?.selectedDeviceStatusMonitor?.onNext(StatusEvent(it))
                    }
                }
                GattCharacteristic.PowerSource -> {
                    val powerSource = ThunderBoardDevice.PowerSource.fromInt(
                            characteristic.getIntValue(GattCharacteristic.PowerSource.format, 0))
                    service?.thunderboardDevice?.let {
                        it.powerSource = powerSource
                        service?.selectedDeviceStatusMonitor?.onNext(StatusEvent(it))
                    }
                }
                GattCharacteristic.BatteryLevel -> {
                    val batteryLevel = characteristic.getIntValue(GattCharacteristic.BatteryLevel.format, 0)
                    service?.thunderboardDevice?.let {
                        it.batteryLevel = batteryLevel
                        service?.selectedDeviceStatusMonitor?.onNext(StatusEvent(it))
                    }
                }
                else -> { }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            handleCommandProcessed()

            val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid)
            when (gattCharacteristic) {
                GattCharacteristic.LedControl,
                GattCharacteristic.Digital -> viewModel.handleLightStateChanges(characteristic)
                else -> { }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)

            val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid)
            when (gattCharacteristic) {
                GattCharacteristic.ReportButton,
                GattCharacteristic.Digital -> viewModel.handleButtonStateChanges(characteristic)
                else -> { }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            handleCommandProcessed()
        }
    }

    private fun onDeviceDisconnect() {
        if (!isFinishing) {
            showMessage(R.string.device_has_disconnected)
            finish()
        }
    }
}
