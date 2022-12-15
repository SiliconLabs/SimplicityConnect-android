package com.siliconlabs.bledemo.features.demo.throughput.models

import android.bluetooth.BluetoothGattCharacteristic
import com.siliconlabs.bledemo.bluetooth.ble.GattCharacteristic
import com.siliconlabs.bledemo.bluetooth.ble.GattService
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import com.siliconlabs.bledemo.features.demo.throughput.viewmodels.ThroughputViewModel
import java.util.concurrent.Executors

class UpdateTest(private val service: BluetoothService?,
                 private val viewModel: ThroughputViewModel,
                 withNotifications: Boolean) {

    private var sentPacketsCounter = 0
    private var payload = byteArrayOf()
    private var uploadCharacteristic: BluetoothGattCharacteristic? = null
    private var uploadMode = Mode.Indications
    private var notificationTimestamps = mutableListOf<Long>()
    private var neededNotificationDelay: Long = 0
    private val executor = Executors.newSingleThreadExecutor()

    init {
        setStates(withNotifications)
        preparePayload()
        viewModel.toggleTestState(toggleOn = true, isUpload = true)
        transmitTestStateChange(true)
    }


    fun stopTransmitting() {
        viewModel.toggleTestState(toggleOn = false, isUpload = true)
        transmitTestStateChange(false)
        sentPacketsCounter = 0
        if (uploadMode == Mode.Notifications) {
            neededNotificationDelay = 0
            notificationTimestamps.clear()
        }
    }

    fun updateUpload() {
        executor.execute {
            if (uploadMode == Mode.Notifications) {
                checkForCongestion()
            }
            transmitData()
        }
    }

    //region Preparing transmission

    private fun setStates(isNotificationsChecked: Boolean) {
        if (isNotificationsChecked) {
            uploadMode = Mode.Notifications
            uploadCharacteristic = getLocalThroughputCharacteristic(GattCharacteristic.ThroughputNotifications)
        }
        else {
            uploadMode = Mode.Indications
            uploadCharacteristic = getLocalThroughputCharacteristic(GattCharacteristic.ThroughputIndications)
        }
    }

    private fun preparePayload() : ByteArray {
        payload = ByteArray(calculatePacketSize())
        payload[0] = 0
        for (i in 1 until payload.size) {
            /* Payload format suggested by embedded side. */
            payload[i] = ('a' + (i-1).rem(26)).toByte()
        }
        return payload
    }

    private fun calculatePacketSize() : Int {
        val mtu = viewModel.mtuSize.value!!
        val pdu = viewModel.pduSize.value!!
        return when (uploadMode) {
            Mode.Indications -> mtu - GATT_HEADER_SIZE
            Mode.Notifications -> {
                /* Formula provided by embedded team. */
                if (pdu <= mtu) {
                    pdu - L2CAP_HEADER_SIZE - GATT_HEADER_SIZE +
                            (mtu - pdu + L2CAP_HEADER_SIZE) / pdu * pdu
                }
                else {
                    if (pdu - mtu <= L2CAP_HEADER_SIZE) {
                        pdu - GATT_HEADER_SIZE - L2CAP_HEADER_SIZE
                    }
                    else {
                        mtu - GATT_HEADER_SIZE
                    }
                }
            }
        }
    }

    private fun transmitTestStateChange(toggleOn: Boolean) {
        executor.execute {
            getLocalThroughputCharacteristic(GattCharacteristic.ThroughputTransmissionOn)?.let {
                it.value = convertToggleToMessage(toggleOn)
                service?.bluetoothGattServer?.notifyCharacteristicChanged(service.connectedGatt?.device,
                        it, false)
            }
        }
    }

    private fun convertToggleToMessage(toggleOn: Boolean) : ByteArray {
        return if (toggleOn) byteArrayOf(1) else byteArrayOf(0)
    }

    //endregion

    //region Transmitting data

    private fun checkForCongestion() {
        if (notificationTimestamps.isNotEmpty()) {
            if (System.currentTimeMillis() - notificationTimestamps.last() > CONGESTION_INTERVAL) {
                /*  Congestion occurred. Too many notifications requested. */
                calculateNeededDelay(System.currentTimeMillis())
                notificationTimestamps.clear()
            }
        }
        notificationTimestamps.add(System.currentTimeMillis())
    }

    private fun calculateNeededDelay(currentTimestamp: Long) {
        neededNotificationDelay =
                (currentTimestamp - notificationTimestamps.first()) / notificationTimestamps.size
    }

    private fun transmitData() {
        if (neededNotificationDelay > 0) Thread.sleep(neededNotificationDelay)

        uploadCharacteristic?.let {
            updatePayload(it)
            service?.bluetoothGattServer?.notifyCharacteristicChanged(
                    service.connectedGatt?.device, it, uploadMode == Mode.Indications)
            viewModel.addBitsToCount(it.value.size)
        }
    }

    private fun updatePayload(characteristic: BluetoothGattCharacteristic?) {
        payload[0] = sentPacketsCounter.toByte()
        characteristic?.value = payload
        sentPacketsCounter = sentPacketsCounter.inc().rem(100)
    }

    //endregion

    private fun getLocalThroughputCharacteristic(characteristic: GattCharacteristic): BluetoothGattCharacteristic? {
        val gattService = service?.bluetoothGattServer?.getService(GattService.ThroughputTestService.number)
        return gattService?.getCharacteristic(characteristic.uuid)
    }

    companion object {
        private const val GATT_HEADER_SIZE = 3
        private const val L2CAP_HEADER_SIZE = 4
        private const val CONGESTION_INTERVAL = 80
    }
}