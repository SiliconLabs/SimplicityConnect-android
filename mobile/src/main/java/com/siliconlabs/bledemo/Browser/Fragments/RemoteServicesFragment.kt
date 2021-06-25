package com.siliconlabs.bledemo.browser.fragments

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.siliconlabs.bledemo.bluetooth.parsing.Engine
import java.util.*

class RemoteServicesFragment : ServicesFragment(isRemote = true) {

    override val services: List<BluetoothGattService> by lazy {
        bluetoothGatt?.services.orEmpty()
    }

    override fun getEditableService(
        uuid: UUID?,
        service: BluetoothGattService
    ) = Engine.getService(uuid)?.let { bluetoothGatt?.getService(uuid) } ?: service

    override fun readCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.readCharacteristic(bluetoothGattCharacteristic)
    }
}
