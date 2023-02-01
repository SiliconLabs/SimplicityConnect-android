package com.siliconlabs.bledemo.features.scan.browser.models.logs

import android.bluetooth.BluetoothGatt

abstract class GattOperationLog(
        gatt : BluetoothGatt,
        private val type: Type
) : Log(gatt) {

    protected fun parseType() : String {
        return when (type) {
            Type.READ_CHARACTERISTIC -> "onCharacteristicRead"
            Type.WRITE_CHARACTERISTIC -> "onCharacteristicWrite"
            Type.CHARACTERISTIC_CHANGED -> "onCharacteristicChanged"
            Type.READ_DESCRIPTOR -> "onDescriptorRead"
            Type.WRITE_DESCRIPTOR -> "onDescriptorWrite"
            Type.SERVICES_DISCOVERED -> "onServicesDiscovered"
            Type.RELIABLE_WRITE_COMPLETED -> "onReliableWriteComplete"
            Type.READ_RSSI -> "onReadRemoteRssi"
            Type.MTU_CHANGED -> "onMtuChanged"
            Type.PHY_UPDATED -> "onPhyUpdate"
        }
    }

    enum class Type {
        READ_CHARACTERISTIC,
        WRITE_CHARACTERISTIC,
        CHARACTERISTIC_CHANGED,
        READ_DESCRIPTOR,
        WRITE_DESCRIPTOR,
        SERVICES_DISCOVERED,
        RELIABLE_WRITE_COMPLETED,
        READ_RSSI,
        MTU_CHANGED,
        PHY_UPDATED
    }
}