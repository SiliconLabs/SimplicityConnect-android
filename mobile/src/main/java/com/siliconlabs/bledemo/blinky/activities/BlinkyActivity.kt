package com.siliconlabs.bledemo.blinky.activities

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.Base.BaseActivity
import com.siliconlabs.bledemo.Bluetooth.BLE.GattCharacteristic
import com.siliconlabs.bledemo.Bluetooth.BLE.GattService
import com.siliconlabs.bledemo.Bluetooth.BLE.TimeoutGattCallback
import com.siliconlabs.bledemo.Bluetooth.Services.BluetoothService
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.utils.BLEUtils
import com.siliconlabs.bledemo.blinky.models.LightState
import com.siliconlabs.bledemo.blinky.viewmodels.BlinkyViewModel
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

    private fun registerBluetoothReceiver() {
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    private fun prepareToolbar() {
        setSupportActionBar(toolbar)
        findViewById<View>(R.id.iv_go_back).setOnClickListener { onBackPressed() }
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
        internal enum class Type {
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
            getBlinkyCharacteristic(GattCharacteristic.LedControl)?.apply {
                value = byteArrayOf(1)
                queueWrite(gatt, this)
            }
        }

        fun switchLightOff(gatt: BluetoothGatt) {
            getBlinkyCharacteristic(GattCharacteristic.LedControl)?.apply {
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

            queueRead(gatt, getBlinkyCharacteristic(GattCharacteristic.LedControl))
            queueNotify(gatt, getBlinkyCharacteristic(GattCharacteristic.ReportButton))
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            handleCommandProcessed()

            val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid)
            if (gattCharacteristic == GattCharacteristic.LedControl) {
                viewModel.handleLightStateChanges(characteristic)
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            handleCommandProcessed()

            val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid)
            if (gattCharacteristic == GattCharacteristic.LedControl) {
                viewModel.handleLightStateChanges(characteristic)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)

            val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid)
            if (gattCharacteristic == GattCharacteristic.ReportButton) {
                viewModel.handleButtonStateChanges(characteristic)
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
