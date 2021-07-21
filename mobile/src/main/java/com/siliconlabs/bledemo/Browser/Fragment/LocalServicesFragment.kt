package com.siliconlabs.bledemo.Browser.Fragment

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.siliconlabs.bledemo.Browser.Activities.DeviceServicesActivity
import java.util.*

class LocalServicesFragment : ServicesFragment(isRemote = false) {

    override val services: List<BluetoothGattService> by lazy {
        (activity as DeviceServicesActivity).bluetoothService?.bluetoothGattServer?.services.orEmpty()
    }

    override fun getEditableService(uuid: UUID?, service: BluetoothGattService) = service

    override fun readCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic) {
        updateCurrentCharacteristicView(bluetoothGattCharacteristic.uuid)
    }

    override fun readDescriptor(descriptor: BluetoothGattDescriptor) {
        updateDescriptorView(descriptor)
    }
}
