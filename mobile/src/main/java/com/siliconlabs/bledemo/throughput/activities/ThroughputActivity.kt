package com.siliconlabs.bledemo.throughput.activities

import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.base.BaseActivity
import com.siliconlabs.bledemo.bluetooth.ble.GattCharacteristic
import com.siliconlabs.bledemo.bluetooth.ble.GattService
import com.siliconlabs.bledemo.bluetooth.ble.TimeoutGattCallback
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import com.siliconlabs.bledemo.throughput.viewmodels.ThroughputViewModel
import com.siliconlabs.bledemo.utils.BLEUtils
import kotlinx.android.synthetic.main.actionbar.*
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class ThroughputActivity : BaseActivity() {
    private lateinit var bluetoothBinding: BluetoothService.Binding
    private lateinit var viewModel: ThroughputViewModel
    private var service: BluetoothService? = null
    private var processor = GattProcessor()
    /*private var serverProcessor = GattServerProcessor()*/
    private var setupData = true
    private var uploadedCharacteristic: GattCharacteristic? = null

    private val bluetoothStateChangeListener: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_OFF) finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_throughput)
        viewModel = ViewModelProvider(this).get(ThroughputViewModel::class.java)

        registerBluetoothReceiver()
        prepareToolbar()
        handleServiceBinding()
    }

    private fun registerBluetoothReceiver() {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateChangeListener, filter)
    }

    private fun handleServiceBinding() {
        bluetoothBinding = object : BluetoothService.Binding(this) {
            override fun onBound(service: BluetoothService?) {
                this@ThroughputActivity.service = service

                service?.apply {
                    /*PeripheralManager.stopAdvertising(service)*/
                    if (!isGattConnected()) {
                        showMessage(R.string.toast_htm_gatt_conn_failed)
                        clearConnectedGatt()
                        bluetoothBinding.unbind()
                        finish()
                    } else {
                        showModalDialog(ConnectionStatus.READING_DEVICE_STATE)
                        service.registerGattCallback(true, processor)
                        /*service.registerGattServerCallback(serverProcessor)*/
                        discoverGattServices()
                    }
                }
            }
        }
        bluetoothBinding.bind()
    }

    private fun prepareToolbar() {
        setSupportActionBar(toolbar)
        findViewById<View>(R.id.iv_go_back).setOnClickListener { onBackPressed() }
    }

    private class GattCommand(val type: Type, val gatt: BluetoothGatt?, val characteristic: BluetoothGattCharacteristic?) {
        internal enum class Type {
            Read,
            Write,
            Notify,
            Indicate,
            PhyUpdate
        }
    }

    fun startTransmitting(isNotificationsChecked: Boolean) {
        val transmissionOnChar = getLocalThroughputCharacteristic(GattCharacteristic.ThroughputTransmissionOn)!!
        transmissionOnChar.value = byteArrayOf(1)
        viewModel.updateUpload(transmissionOnChar, GattCharacteristic.ThroughputTransmissionOn)

        service?.bluetoothGattServer?.notifyCharacteristicChanged(service?.connectedGatt?.device,
                transmissionOnChar, false)

        uploadedCharacteristic =
            if (isNotificationsChecked) GattCharacteristic.ThroughputNotifications
            else {
                GattCharacteristic.ThroughputIndications
            }
    }

    fun stopTransmitting() {
        val transmissionOnChar = getLocalThroughputCharacteristic(GattCharacteristic.ThroughputTransmissionOn)!!
        transmissionOnChar.value = byteArrayOf(0)
        viewModel.updateUpload(transmissionOnChar, GattCharacteristic.ThroughputTransmissionOn)

        service?.bluetoothGattServer?.notifyCharacteristicChanged(service?.connectedGatt?.device,
                transmissionOnChar, false)

    }

    fun transmitNotifications() {
        val dataCharacteristic = getLocalThroughputCharacteristic(GattCharacteristic.ThroughputNotifications)

        dataCharacteristic?.let {
            it.value = ByteArray(calculatePacketSize())
            it.value[0] = Random().nextInt(128).toByte()
            for (i in 1 until it.value.size) {
                it.value[i] = i.rem(128).toByte()
            }

            service?.bluetoothGattServer?.notifyCharacteristicChanged(service?.connectedGatt?.device,
                    dataCharacteristic, shouldConfirm(uploadedCharacteristic!!))
            viewModel.updateUpload(it, uploadedCharacteristic!!)
        }
    }

    fun transmitIndications() {
        /* Board is not receiving anything, so for the purposes of measuring speed a packet is
        sent once every two connection intervals. */
        val dataCharacteristic = getLocalThroughputCharacteristic(GattCharacteristic.ThroughputIndications)

        while (viewModel.isUploadActive) {
            dataCharacteristic?.let {
                it.value = ByteArray(calculatePacketSize())
                it.value[0] = Random().nextInt(128).toByte()
                for (i in 1 until it.value.size) {
                    it.value[i] = i.rem(128).toByte()
                }

                service?.bluetoothGattServer?.notifyCharacteristicChanged(service?.connectedGatt?.device,
                        dataCharacteristic, shouldConfirm(uploadedCharacteristic!!))
                viewModel.updateUpload(it, uploadedCharacteristic!!)
            }
        Thread.sleep(viewModel.connectionInterval.value!!.toLong() * 2)
        }

    }

    private fun calculatePacketSize() : Int {
        return when (uploadedCharacteristic) {
            GattCharacteristic.ThroughputIndications -> viewModel.mtuSize.value!! - GATT_HEADER_SIZE
            GattCharacteristic.ThroughputNotifications -> {
                (viewModel.mtuSize.value!! / viewModel.pduSize.value!!) *
                        viewModel.pduSize.value!! - GATT_HEADER_SIZE - L2CAP_HEADER_SIZE
            }
            else -> 0
        }
    }

    private fun getRemoteThroughputCharacteristic(characteristic: GattCharacteristic): BluetoothGattCharacteristic? {
        val gattService = service?.connectedGatt?.getService(GattService.ThroughputTestService.number)
        return gattService?.getCharacteristic(characteristic.uuid)
    }

    private fun getRemoteThroughputInformationCharacteristic(characteristic: GattCharacteristic): BluetoothGattCharacteristic? {
        val gattService = service?.connectedGatt?.getService(GattService.ThroughputInformationService.number)
        return gattService?.getCharacteristic(characteristic.uuid)
    }

    private fun getLocalThroughputCharacteristic(characteristic: GattCharacteristic): BluetoothGattCharacteristic? {
        val gattService = service?.bluetoothGattServer?.getService(GattService.ThroughputTestService.number)
        return gattService?.getCharacteristic(characteristic.uuid)
    }

    private fun shouldConfirm(characteristic: GattCharacteristic) : Boolean {
        return characteristic == GattCharacteristic.ThroughputIndications
    }

    private inner class GattProcessor : TimeoutGattCallback() {
        private val handler: Handler = Handler(Looper.getMainLooper())
        private val commands: Queue<GattCommand> = LinkedList()
        private val lock: Lock = ReentrantLock()
        private var processing = false

        private val phyUpdateTimeoutRunnable = Runnable {
            handleCommandProcessed()
        }

        private fun queueRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            queue(GattCommand(GattCommand.Type.Read, gatt, characteristic))
        }

        fun queueWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            queue(GattCommand(GattCommand.Type.Write, gatt, characteristic))
        }

        private fun queueIndicate(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            queue(GattCommand(GattCommand.Type.Indicate, gatt, characteristic))
        }

        private fun queueNotify(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            queue(GattCommand(GattCommand.Type.Notify, gatt, characteristic))
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

        private fun processNextCommand() {
            var success = false
            val command = commands.poll()

            command?.gatt?.let { gatt ->
                val characteristic = command.characteristic

                success = when (command.type) {
                    GattCommand.Type.Read -> gatt.readCharacteristic(characteristic)
                    GattCommand.Type.Write -> gatt.writeCharacteristic(characteristic)
                    GattCommand.Type.Indicate -> {
                        val gattCharacteristic = GattCharacteristic.fromUuid(characteristic!!.uuid)
                        val gattService = GattService.fromUuid(characteristic.service.uuid)
                        BLEUtils.setNotificationForCharacteristic(gatt, gattService, gattCharacteristic, BLEUtils.Notifications.INDICATE)
                    }
                    GattCommand.Type.Notify -> {
                        val gattCharacteristic = GattCharacteristic.fromUuid(characteristic!!.uuid)
                        val gattService = GattService.fromUuid(characteristic.service.uuid)
                        BLEUtils.setNotificationForCharacteristic(gatt, gattService, gattCharacteristic, BLEUtils.Notifications.NOTIFY)
                    }
                    GattCommand.Type.PhyUpdate -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            gatt.setPreferredPhy(BluetoothDevice.PHY_LE_2M_MASK, BluetoothDevice.PHY_LE_2M_MASK, BluetoothDevice.PHY_OPTION_NO_PREFERRED)
                            handler.postDelayed(phyUpdateTimeoutRunnable, 2000)
                        } else {
                            handleCommandProcessed()
                        }
                        true
                    }
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

        private fun clearCommandQueue() {
            lock.lock()
            try {
                commands.clear()
            } finally {
                lock.unlock()
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

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            gatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
            addToQueue(GattCommand(GattCommand.Type.PhyUpdate, gatt, null))

            addToQueue(GattCommand(GattCommand.Type.Notify, gatt, getRemoteThroughputInformationCharacteristic(GattCharacteristic.ThroughputPhyStatus)))
            addToQueue(GattCommand(GattCommand.Type.Notify, gatt, getRemoteThroughputInformationCharacteristic(GattCharacteristic.ThroughputConnectionInterval)))
            addToQueue(GattCommand(GattCommand.Type.Notify, gatt, getRemoteThroughputInformationCharacteristic(GattCharacteristic.ThroughputSlaveLatency)))
            addToQueue(GattCommand(GattCommand.Type.Notify, gatt, getRemoteThroughputInformationCharacteristic(GattCharacteristic.ThroughputSupervisionTimeout)))
            addToQueue(GattCommand(GattCommand.Type.Notify, gatt, getRemoteThroughputInformationCharacteristic(GattCharacteristic.ThroughputPduSize)))
            addToQueue(GattCommand(GattCommand.Type.Notify, gatt, getRemoteThroughputInformationCharacteristic(GattCharacteristic.ThroughputMtuSize)))

            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getRemoteThroughputInformationCharacteristic(GattCharacteristic.ThroughputPhyStatus)))
            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getRemoteThroughputInformationCharacteristic(GattCharacteristic.ThroughputConnectionInterval)))
            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getRemoteThroughputInformationCharacteristic(GattCharacteristic.ThroughputSlaveLatency)))
            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getRemoteThroughputInformationCharacteristic(GattCharacteristic.ThroughputSupervisionTimeout)))
            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getRemoteThroughputInformationCharacteristic(GattCharacteristic.ThroughputPduSize)))
            addToQueue(GattCommand(GattCommand.Type.Read, gatt, getRemoteThroughputInformationCharacteristic(GattCharacteristic.ThroughputMtuSize)))

            addToQueue(GattCommand(GattCommand.Type.Notify, gatt, getRemoteThroughputCharacteristic(GattCharacteristic.ThroughputNotifications)))
            addToQueue(GattCommand(GattCommand.Type.Indicate, gatt, getRemoteThroughputCharacteristic(GattCharacteristic.ThroughputIndications)))
            addToQueue(GattCommand(GattCommand.Type.Notify, gatt, getRemoteThroughputCharacteristic(GattCharacteristic.ThroughputTransmissionOn)))
            queueIndicate(gatt, getRemoteThroughputCharacteristic(GattCharacteristic.ThroughputResult))
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            handleCommandProcessed()

            val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid)
            gattCharacteristic?.let {
                viewModel.updateDownload(characteristic, it)
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            handleCommandProcessed()
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            handleCommandProcessed()
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid)
            gattCharacteristic?.let {
                viewModel.updateDownload(characteristic, it)
            }
        }

        override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status)
            handler.removeCallbacks(phyUpdateTimeoutRunnable)
            handleCommandProcessed()
        }
    }
/*
    private inner class GattServerProcessor : BluetoothGattServerCallback() {

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            Timber.d("Notification sent")
            if (viewModel.isUploadActive) {
                when (uploadedCharacteristic) {
                    GattCharacteristic.ThroughputNotifications -> transmitNotifications()
                    GattCharacteristic.ThroughputIndications -> transmitIndications()
                    else -> { }
                }
            }
        }
    }
*/
    override fun onDestroy() {
        super.onDestroy()

        /*PeripheralManager.clearThroughputServer(service)*/
        unregisterReceiver(bluetoothStateChangeListener)
        service?.clearConnectedGatt()
        bluetoothBinding.unbind()
    }

    companion object {
        private const val GATT_HEADER_SIZE = 3
        private const val L2CAP_HEADER_SIZE = 4
    }
}