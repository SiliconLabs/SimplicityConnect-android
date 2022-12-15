package com.siliconlabs.bledemo.features.demo.throughput.utils

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.*
import android.os.ParcelUuid
import com.siliconlabs.bledemo.features.configure.advertiser.models.AdvertiserData
import com.siliconlabs.bledemo.features.configure.advertiser.models.AdvertiserSettings
import com.siliconlabs.bledemo.bluetooth.ble.GattCharacteristic
import com.siliconlabs.bledemo.bluetooth.ble.GattService
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import com.siliconlabs.bledemo.utils.UuidConsts
import timber.log.Timber

object PeripheralManager {


    fun advertiseThroughputServer(service: BluetoothService?) {
        service?.let {
            it.bluetoothGattServer?.addService(prepareThroughputService())
            it.bluetoothAdapter?.bluetoothLeAdvertiser?.startAdvertising(
                    settings, advertiseData, scanResponseData, advertiseCallback)
        }
    }

    fun stopAdvertising(service: BluetoothService?) {
        service?.bluetoothAdapter?.bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
    }

    fun clearThroughputServer(service: BluetoothService?) {
        service?.bluetoothGattServer?.clearServices()
    }


    private fun prepareThroughputService() : BluetoothGattService {
        val service = BluetoothGattService(GattService.ThroughputTestService.number, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val indicationsChar = BluetoothGattCharacteristic(GattCharacteristic.ThroughputIndications.uuid,
                BluetoothGattCharacteristic.PROPERTY_INDICATE,
                BluetoothGattCharacteristic.PERMISSION_READ)
        val notificationsChar = BluetoothGattCharacteristic(GattCharacteristic.ThroughputNotifications.uuid,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ)
        val transmissionOnChar = BluetoothGattCharacteristic(GattCharacteristic.ThroughputTransmissionOn.uuid,
                BluetoothGattCharacteristic.PROPERTY_READ or
                        BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ or
                        BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        indicationsChar.addDescriptor(getConfigDescriptor())
        notificationsChar.addDescriptor(getConfigDescriptor())
        transmissionOnChar.addDescriptor(getConfigDescriptor())

        service.addCharacteristic(notificationsChar)
        service.addCharacteristic(indicationsChar)
        service.addCharacteristic(transmissionOnChar)

        return service
    }

    private fun getConfigDescriptor() : BluetoothGattDescriptor {
        return BluetoothGattDescriptor(UuidConsts.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                BluetoothGattDescriptor.PERMISSION_READ or
                        BluetoothGattDescriptor.PERMISSION_WRITE)
    }

    /* Advertiser fields */
    private val settings = AdvertiserSettings(AdvertiserData()).getAdvertiseSettings()
    private val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(true)
            .build()
    private val scanResponseData = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(GattService.ThroughputTestService.number))
            .setIncludeTxPowerLevel(true)
            .build()
    private val advertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Timber.d("GattServer; BLE advertisement added successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            Timber.e("GattServer; Failed to add BLE advertisement, reason: $errorCode")
        }
    }

}