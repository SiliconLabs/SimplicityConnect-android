package com.siliconlabs.bledemo.Browser.Fragment

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.siliconlabs.bledemo.Browser.Activities.DeviceServicesActivity
import com.siliconlabs.bledemo.iop_test.models.CommonUUID
import java.util.*

class LocalServicesFragment : ServicesFragment(isRemote = false) {

    override val services: List<BluetoothGattService> by lazy {
        val list = ArrayList<BluetoothGattService>()
        list.addAll(createMandatorySystemServices())
        list.addAll((activity as DeviceServicesActivity).bluetoothService?.bluetoothGattServer?.services.orEmpty())
        list
    }

    override fun getEngineService(uuid: UUID?, service: BluetoothGattService) = service

    override fun getEngineCharacteristic(uuid: UUID?,
                                         service: BluetoothGattService,
                                         characteristic: BluetoothGattCharacteristic
    ): BluetoothGattCharacteristic = characteristic

    override fun readCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic) {
        updateCurrentCharacteristicView(bluetoothGattCharacteristic.uuid)
    }

    override fun readDescriptor(descriptor: BluetoothGattDescriptor) {
        updateDescriptorView(descriptor)
    }

    private fun createMandatorySystemServices(): Collection<BluetoothGattService> {
        val list = ArrayList<BluetoothGattService>()
        val genericAccessServiceUuid = UUID.fromString(CommonUUID.Service.UUID_GENERIC_ACCESS.toString())
        val deviceNameCharUuid = UUID.fromString(CommonUUID.Characteristic.DEVICE_NAME.toString())
        val appearanceCharUuid = UUID.fromString(CommonUUID.Characteristic.APPEARANCE.toString())
        val centralAddressResolutionCharUuid = UUID.fromString(CommonUUID.Characteristic.CENTRAL_ADDRESS_RESOLUTION.toString())
        val genericAttributeServiceUuid = UUID.fromString(CommonUUID.Service.UUID_GENERIC_ATTRIBUTE.toString())
        val serviceChangedCharUuid = UUID.fromString(CommonUUID.Characteristic.SERVICE_CHANGED.toString())

        BluetoothGattService(genericAttributeServiceUuid, 0).let {
            val bluetoothGattCharacteristicDeviceNameChar = BluetoothGattCharacteristic(serviceChangedCharUuid, 32, 0)
            it.addCharacteristic(bluetoothGattCharacteristicDeviceNameChar)
            list.add(it)
        }

        BluetoothGattService(genericAccessServiceUuid, 0).let {
            val bluetoothGattCharacteristicDeviceNameChar = BluetoothGattCharacteristic(deviceNameCharUuid, 2, 0)
            val bluetoothGattCharacteristicAppearanceChar = BluetoothGattCharacteristic(appearanceCharUuid, 2, 0)
            val bluetoothGattCharacteristicCentralAddressResolutionChar = BluetoothGattCharacteristic(centralAddressResolutionCharUuid, 2, 0)
            it.addCharacteristic(bluetoothGattCharacteristicDeviceNameChar)
            it.addCharacteristic(bluetoothGattCharacteristicAppearanceChar)
            it.addCharacteristic(bluetoothGattCharacteristicCentralAddressResolutionChar)
            list.add(it)
        }

        return list
    }
}
