package com.siliconlabs.bledemo.features.demo.throughput.activities

import android.bluetooth.*
import android.os.*
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.bluetooth.ble.GattCharacteristic
import com.siliconlabs.bledemo.bluetooth.ble.GattService
import com.siliconlabs.bledemo.bluetooth.ble.TimeoutGattCallback
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.base.activities.BaseDemoActivity
import com.siliconlabs.bledemo.utils.BLEUtils
import com.siliconlabs.bledemo.features.demo.throughput.models.UpdateTest
import com.siliconlabs.bledemo.features.demo.throughput.utils.PeripheralManager
import com.siliconlabs.bledemo.features.demo.throughput.viewmodels.ThroughputViewModel
import com.siliconlabs.bledemo.utils.Notifications
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class ThroughputActivity : BaseDemoActivity() {
    private lateinit var viewModel: ThroughputViewModel
    private var processor = GattProcessor()
    private var serverProcessor = GattServerProcessor()
    private var setupData = true

    private var updateTest: UpdateTest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_throughput)
        viewModel = ViewModelProvider(this).get(ThroughputViewModel::class.java)
    }


    override fun onBluetoothServiceBound() {
        PeripheralManager.stopAdvertising(service)
        showModalDialog(ConnectionStatus.READING_DEVICE_STATE)

        service?.also {
            it.registerGattCallback(false, processor)
            it.registerGattServerCallback(serverProcessor)
            it.discoverGattServices()
        }
    }

    private class GattCommand(val type: Type, val gatt: BluetoothGatt?, val characteristic: BluetoothGattCharacteristic?) {
        enum class Type {
            Read,
            Write,
            Notify,
            Indicate,
            PhyUpdate
        }
    }

    fun startUploadTest(withNotifications: Boolean) {
        updateTest = UpdateTest(service, viewModel, withNotifications)
    }

    fun stopUploadTest() {
        updateTest?.stopTransmitting()
        updateTest = null
    }

    private fun getRemoteThroughputCharacteristic(characteristic: GattCharacteristic): BluetoothGattCharacteristic? {
        val gattService = service?.connectedGatt?.getService(GattService.ThroughputTestService.number)
        return gattService?.getCharacteristic(characteristic.uuid)
    }

    private fun getRemoteThroughputInformationCharacteristic(characteristic: GattCharacteristic): BluetoothGattCharacteristic? {
        val gattService = service?.connectedGatt?.getService(GattService.ThroughputInformationService.number)
        return gattService?.getCharacteristic(characteristic.uuid)
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
                        BLEUtils.setNotificationForCharacteristic(gatt, gattService, gattCharacteristic, Notifications.INDICATE)
                    }
                    GattCommand.Type.Notify -> {
                        val gattCharacteristic = GattCharacteristic.fromUuid(characteristic!!.uuid)
                        val gattService = GattService.fromUuid(characteristic.service.uuid)
                        BLEUtils.setNotificationForCharacteristic(gatt, gattService, gattCharacteristic, Notifications.NOTIFY)
                    }
                    GattCommand.Type.PhyUpdate -> {
                        gatt.setPreferredPhy(BluetoothDevice.PHY_LE_2M_MASK, BluetoothDevice.PHY_LE_2M_MASK, BluetoothDevice.PHY_OPTION_NO_PREFERRED)
                        handler.postDelayed(phyUpdateTimeoutRunnable, 2000)
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

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                runOnUiThread { onDeviceDisconnect() }
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

    private inner class GattServerProcessor : BluetoothGattServerCallback() {

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            if (viewModel.isUploadActive) {
                updateTest?.updateUpload()
            }
        }
    }

    private fun onDeviceDisconnect() {
        if (!isFinishing) {
            showMessage(R.string.device_has_disconnected)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PeripheralManager.clearThroughputServer(service)
    }

}
