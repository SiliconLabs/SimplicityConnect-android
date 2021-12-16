package com.siliconlabs.bledemo.thunderboard.utils

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import timber.log.Timber
import java.util.*

object BleUtils {
    private fun findCharacteristics(gatt: BluetoothGatt?, serviceUuid: UUID?,
                                    characteristicUuid: UUID?,
                                    property: Int): List<BluetoothGattCharacteristic>? {
        if (serviceUuid == null) {
            return null
        }
        if (characteristicUuid == null) {
            return null
        }
        if (gatt == null) {
            return null
        }
        val service = gatt.getService(serviceUuid) ?: return null
        val results: MutableList<BluetoothGattCharacteristic> = ArrayList()
        for (c in service.characteristics) {
            val props = c.properties
            if (characteristicUuid == c.uuid && property == property and props) {
                results.add(c)
            }
        }
        return results
    }

    fun readCharacteristic(gatt: BluetoothGatt?, serviceUuid: UUID?,
                           characteristicUuid: UUID?): Boolean {
        if (gatt == null) {
            return false
        }
        val service = gatt.getService(serviceUuid) ?: return false
        val characteristic = service.getCharacteristic(characteristicUuid) ?: return false
        return gatt.readCharacteristic(characteristic)
    }

    fun readCharacteristic(gatt: BluetoothGatt?,
                           characteristic: BluetoothGattCharacteristic?): Boolean {
        return if (gatt == null || characteristic == null) {
            false
        } else gatt.readCharacteristic(characteristic)
    }

    fun setCharacteristicNotification(gatt: BluetoothGatt?, serviceUuid: UUID,
                                      characteristicUuid: UUID, descriptorUuid: UUID?,
                                      enable: Boolean): Boolean {
        if (gatt == null) {
            return false
        }
        val service = gatt.getService(serviceUuid) ?: return false
        val characteristic = service.getCharacteristic(characteristicUuid)
        if (characteristic == null) {
            Timber.d("could not get characteristic: %s for service: %s",
                    characteristicUuid.toString(), serviceUuid.toString())
            return false
        }
        if (!gatt.setCharacteristicNotification(characteristic, true)) {
            Timber.d("was not able to setCharacteristicNotification")
            return false
        }
        val descriptor = characteristic.getDescriptor(descriptorUuid)
        if (descriptor == null) {
            Timber.d("was not able to getDescriptor")
            return false
        }
        if (enable) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else {
            descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }
        return gatt.writeDescriptor(descriptor)
    }

    // TODO consolidate with setCharacteristicNotification - duplicated code because we didn't have
    // time to run regression
    fun unsetCharacteristicNotification(gatt: BluetoothGatt?, serviceUuid: UUID,
                                        characteristicUuid: UUID, descriptorUuid: UUID?,
                                        enable: Boolean): Boolean {
        if (gatt == null) {
            return false
        }
        val service = gatt.getService(serviceUuid) ?: return false
        val characteristic = service.getCharacteristic(characteristicUuid)
        if (characteristic == null) {
            Timber.d("could not get characteristic: %s for service: %s",
                    characteristicUuid.toString(), serviceUuid.toString())
            return false
        }
        if (!gatt.setCharacteristicNotification(characteristic, false)) {
            Timber.d("was not able to setCharacteristicNotification")
            return false
        }
        val descriptor = characteristic.getDescriptor(descriptorUuid)
        if (descriptor == null) {
            Timber.d("was not able to getDescriptor")
            return false
        }
        if (enable) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else {
            descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }
        return gatt.writeDescriptor(descriptor)
    }

    fun writeCharacteristic(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic,
                            value: Int, format: Int, offset: Int): Boolean {
        if (gatt == null) {
            return false
        }
        Timber.d("prop: %02x", characteristic.properties)
        characteristic.setValue(value, format, offset)
        return gatt.writeCharacteristic(characteristic)
    }

    fun writeCharacteristic(gatt: BluetoothGatt?, serviceUuid: UUID?, characteristicUuid: UUID?,
                            value: Int, format: Int, offset: Int): Boolean {
        if (gatt == null) {
            return false
        }
        val characteristic = gatt.getService(serviceUuid).getCharacteristic(characteristicUuid)
        return writeCharacteristic(gatt, characteristic, value, format, offset)
    }

    // TODO modify calls to writeCharacteristic() to use findCharacteristic to match iOS behavior
    fun writeCharacteristics(gatt: BluetoothGatt, serviceUuid: UUID?, characteristicUuid: UUID?,
                             value: Int, format: Int, offset: Int): Boolean {
        val characteristics = findCharacteristics(gatt, serviceUuid, characteristicUuid,
                BluetoothGattCharacteristic.PROPERTY_WRITE)
        if (characteristics == null || characteristics.size == 0) {
            return false
        }
        var submitted = false
        var result = false
        for (characteristic in characteristics) {
            if (characteristic.setValue(value, format, offset)) {
                submitted = true
                result = gatt.writeCharacteristic(characteristic)
            }
        }
        Timber.d("submitted: %s", submitted)
        return result
    }

    fun writeCharacteristic(gatt: BluetoothGatt?, serviceUuid: UUID?, characteristicUuid: UUID?,
                            value: ByteArray?): Boolean {
        if (gatt == null) {
            return false
        }
        val characteristic = gatt.getService(serviceUuid).getCharacteristic(characteristicUuid)
        val submitted = characteristic.setValue(value)
        Timber.d("submitted: %s", submitted)
        return gatt.writeCharacteristic(characteristic)
    }

    fun setCharacteristicIndication(gatt: BluetoothGatt?, serviceUuid: UUID,
                                    characteristicUuid: UUID, descriptorUuid: UUID?,
                                    enable: Boolean): Boolean {
        if (gatt == null) {
            return false
        }
        val service = gatt.getService(serviceUuid) ?: return false
        val characteristic = service.getCharacteristic(characteristicUuid)
        if (characteristic == null) {
            Timber.d("could not get characteristic: %s for service: %s",
                    characteristicUuid.toString(), serviceUuid.toString())
            return false
        }
        val descriptor = characteristic.getDescriptor(descriptorUuid)
        if (descriptor == null) {
            Timber.d("was not able to getDescriptor")
            return false
        }
        if (enable) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        } else {
            descriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        }
        return gatt.writeDescriptor(descriptor)
    }

    fun setCharacteristicIndications(gatt: BluetoothGatt, serviceUuid: UUID?,
                                     characteristicUuid: UUID?, descriptorUuid: UUID?,
                                     enable: Boolean): Boolean {
        val characteristics = findCharacteristics(gatt,
                serviceUuid, characteristicUuid, BluetoothGattCharacteristic.PROPERTY_INDICATE)
        if (characteristics == null || characteristics.size == 0) {
            return false
        }
        val submitted = false
        var result = false
        for (characteristic in characteristics) {
            val descriptor = characteristic.getDescriptor(descriptorUuid)
            if (descriptor == null) {
                Timber.d("was not able to getDescriptor")
                return false
            }
            if (enable) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            } else {
                descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            }
            result = gatt.writeDescriptor(descriptor)
        }
        Timber.d("submitted: %s", submitted)
        return result
    }
}