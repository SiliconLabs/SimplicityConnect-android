package com.siliconlabs.bledemo.browser.fragments

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.siliconlabs.bledemo.browser.activities.DeviceServicesActivity
import java.util.*

class LocalServicesFragment : ServicesFragment(isRemote = false) {

    override val services: List<BluetoothGattService> by lazy {
        (activity as DeviceServicesActivity).bluetoothService?.bluetoothGattServer?.services.orEmpty()
    }

    override fun getEditableService(uuid: UUID?, service: BluetoothGattService) = service

    override fun readCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic) {
        updateCurrentCharacteristicView(bluetoothGattCharacteristic.uuid)
    }
}
