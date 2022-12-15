package com.siliconlabs.bledemo.features.demo.connected_lighting.activities

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.os.Bundle
import android.util.Log
import com.siliconlabs.bledemo.bluetooth.ble.GattCharacteristic
import com.siliconlabs.bledemo.bluetooth.ble.GattService
import com.siliconlabs.bledemo.bluetooth.ble.TimeoutGattCallback
import com.siliconlabs.bledemo.features.demo.connected_lighting.presenters.ConnectedLightingPresenter
import com.siliconlabs.bledemo.features.demo.connected_lighting.presenters.ConnectedLightingPresenter.BluetoothController
import com.siliconlabs.bledemo.features.demo.connected_lighting.models.TriggerSource
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.base.activities.BaseDemoActivity
import com.siliconlabs.bledemo.utils.BLEUtils
import com.siliconlabs.bledemo.utils.Notifications

class ConnectedLightingActivity : BaseDemoActivity(), BluetoothController {
    private var presenter: ConnectedLightingPresenter? = null
    private var gattService: GattService? = null

    private var initSourceAddress = false
    private var serviceHasBeenSet = false
    private var updateDelayed = false


    private val gattCallback: TimeoutGattCallback = object : TimeoutGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothGatt.STATE_DISCONNECTED || newState == BluetoothGatt.STATE_DISCONNECTING) {
                runOnUiThread { disconnectWithModal() }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            val characteristic = getLightCharacteristic()
            if (characteristic != null) {
                val success = service?.connectedGatt?.readCharacteristic(characteristic)!!
                if (!success) {
                    disconnectWithModal()
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)

            if (status == BluetoothGatt.GATT_SUCCESS) {
                onCharacteristicUpdate(characteristic)
            }

            if (GattCharacteristic.Light.uuid == characteristic.uuid) {
                val success = gatt.readCharacteristic(characteristic.service
                        .getCharacteristic(GattCharacteristic.TriggerSource.uuid))
                if (!success) {
                    disconnectWithModal()
                }
            } else if (GattCharacteristic.TriggerSource.uuid == characteristic.uuid) {
                if (!initSourceAddress) {
                    initSourceAddress = true
                    val success = gatt.readCharacteristic(characteristic.service
                            .getCharacteristic(GattCharacteristic.SourceAddress.uuid))
                    if (!success) {
                        disconnectWithModal()
                    }
                }
            } else if (GattCharacteristic.SourceAddress.uuid == characteristic.uuid) {
                val success: Boolean = BLEUtils.setNotificationForCharacteristic(gatt,
                        gattService,
                        GattCharacteristic.Light,
                        Notifications.INDICATE)
                if (!success) {
                    disconnectWithModal()
                }
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
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            updateDelayed = true
            onCharacteristicUpdate(characteristic)
        }

        private fun onCharacteristicUpdate(characteristic: BluetoothGattCharacteristic) {
            val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid) ?: return
            when (gattCharacteristic) {
                GattCharacteristic.Light -> runOnUiThread {
                    val value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                    val isLightOn = value != 0
                    presenter?.onLightUpdated(isLightOn)
                }
                GattCharacteristic.TriggerSource -> runOnUiThread {
                    val value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_light)
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter?.cancelPeriodicReads()
    }

    override fun onResume() {
        super.onResume()
        //get out if the service has stopped, or if the gatt connection is dead
        if (serviceHasBeenSet && service == null || service != null && !service?.isGattConnected()!!) {
            disconnectWithModal()
        }
    }

    override fun onBluetoothServiceBound() {
        serviceHasBeenSet = true
        service?.registerGattCallback(true, gattCallback)
        service?.refreshGattServices()
    }

    private fun disconnectWithModal() {
        if (!isFinishing && presenter != null) {
            presenter?.showDeviceDisconnectedDialog()
        }
    }

    override fun setLightValue(lightOn: Boolean): Boolean {
        val characteristic = getLightCharacteristic() ?: return false
        characteristic.setValue(if (lightOn) 1 else 0, GattCharacteristic.Light.format, 0)
        return service?.connectedGatt?.writeCharacteristic(characteristic)!!
    }

    override fun setPresenter(presenter: ConnectedLightingPresenter?) {
        this.presenter = presenter
    }

    override fun getLightValue(): Boolean {
        val characteristic = getLightCharacteristic()
        return characteristic != null && service?.connectedGatt?.readCharacteristic(characteristic)!!
    }

    private fun getLightCharacteristic(): BluetoothGattCharacteristic? {
        if (service == null) {
            return null
        }
        if (!service?.isGattConnected()!!) {
            return null
        }
        val gatt = service?.connectedGatt
        gattService = getGattService()

        if (gattService != null) {
            presenter?.let { it.gattService = gattService }

            return gatt?.getService(gattService?.number)?.getCharacteristic(GattCharacteristic.Light.uuid)
        }

        return null
    }

    override fun leaveDemo() {
        finish()
    }

    private fun getGattService(): GattService? {
        val gatt = service?.connectedGatt
        return when {
            gatt?.getService(GattService.ProprietaryLightService.number) != null -> {
                GattService.ProprietaryLightService
            }
            gatt?.getService(GattService.ZigbeeLightService.number) != null -> {
                GattService.ZigbeeLightService
            }
            gatt?.getService(GattService.ConnectLightService.number) != null -> {
                GattService.ConnectLightService
            }
            gatt?.getService(GattService.ThreadLightService.number) != null -> {
                GattService.ThreadLightService
            }
            else -> null
        }
    }

    companion object {
        private const val SOURCE_ADDRESS_LENGTH = 8
    }
}
