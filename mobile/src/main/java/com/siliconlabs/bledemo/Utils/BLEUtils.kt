package com.siliconlabs.bledemo.Utils

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.siliconlabs.bledemo.Bluetooth.BLE.GattCharacteristic
import com.siliconlabs.bledemo.Bluetooth.BLE.GattService
import java.util.*

object BLEUtils {
    val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private fun setNotificationForCharacteristic(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic?, gattDescriptor: UUID?, value: Notifications): Boolean {
        var written = false
        if (characteristic != null) {
            gatt.setCharacteristicNotification(characteristic, value.isEnabled)
            val descriptor = characteristic.getDescriptor(gattDescriptor)
            if (descriptor != null) {
                //writing this descriptor causes the device to send updates
                descriptor.value = value.descriptorValue
                written = gatt.writeDescriptor(descriptor)
            }
            return written
        }
        return false
    }

    fun setNotificationForCharacteristic(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic?, value: Notifications): Boolean {
        return setNotificationForCharacteristic(gatt, characteristic, CLIENT_CHARACTERISTIC_CONFIG_UUID, value)
    }

    /**
     * Set a Notification Setting for The Matching Characteristic of a Service
     *
     * @param gatt               The Bluetooth GATT
     * @param gattService        the service we must find and match
     * @param gattCharacteristic the characteristic we must find and match
     * @param gattDescriptor     the descriptor we must write to we must find and match
     * @param value              The exact setting we are setting
     * @return Whether the instruction to write passed or failed.
     */
    fun setNotificationForCharacteristic(gatt: BluetoothGatt, gattService: GattService?, gattCharacteristic: GattCharacteristic?, gattDescriptor: UUID?, value: Notifications): Boolean {
        var written = false
        val services = gatt.services
        for (service in services) {
            val characteristic = getCharacteristic(service, gattService, gattCharacteristic)
            if (characteristic != null) {
                gatt.setCharacteristicNotification(characteristic, value.isEnabled)
                val descriptor = characteristic.getDescriptor(gattDescriptor)
                if (descriptor != null) {
                    //writing this descriptor causes the device to send updates
                    descriptor.value = value.descriptorValue
                    written = gatt.writeDescriptor(descriptor)
                }
                return written
            }
        }
        return false
    }

    /**
     * Set a Notification Setting for The Matching Characteristic of a Service
     *
     * @param gatt           The Bluetooth GATT
     * @param service        the service we must find and match
     * @param characteristic the characteristic we must find and match
     * @param value          The exact setting we are setting
     * @return Whether the instruction to write passed or failed.
     */
    fun setNotificationForCharacteristic(gatt: BluetoothGatt, service: GattService?, characteristic: GattCharacteristic?, value: Notifications): Boolean {
        return setNotificationForCharacteristic(gatt, service, characteristic, CLIENT_CHARACTERISTIC_CONFIG_UUID, value)
    }

    /**
     * Search for a specific characteristic given a BluetoothGattService
     *
     * @param service              the service to search through
     * @param targetService        the service that you're looking for
     * @param targetCharacteristic the characteristic you're looking for
     * @return the characteristic, if it's found - null otherwise
     */
    fun getCharacteristic(service: BluetoothGattService?, targetService: GattService?, targetCharacteristic: GattCharacteristic?): BluetoothGattCharacteristic? {
        if (service == null || targetService == null || targetCharacteristic == null) {
            return null
        }
        val gattService = GattService.fromUuid(service.uuid)
        if (gattService != null && gattService == targetService) {
            val characteristics = service.characteristics
            if (characteristics != null && !characteristics.isEmpty()) {
                for (characteristic in characteristics) {
                    val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid)
                    if (gattCharacteristic != null && gattCharacteristic == targetCharacteristic) {
                        return characteristic
                    }
                }
            }
        }
        return null
    }

    /**
     * Enumerated Notification State Handler
     */
    enum class Notifications(private val type: Types?,
                             /**
                              * Storage Field for Enabled bool passed from .CTor
                              */
                             val isEnabled: Boolean) {
        DISABLED(null, false), NOTIFY(Types.NOTIFICATIONS, true), INDICATE(Types.INDICATIONS, true);

        private enum class Types {
            INDICATIONS, NOTIFICATIONS
        }

        /**
         * Does this Value represent an Indications in an Enabled State?
         *
         * @return Boolean, True = Enabled, False = Disabled
         */

        /**
         * Get the BluetoothGattDescriptor Value for This State.
         *
         * @return BluetoothGattDescriptor value as Byte Array
         */
        /**
         * Storage Field for BluetoothGattDescriptor value based on Boolean
         */
        val descriptorValue: ByteArray

        override fun toString(): String {
            return if (!isEnabled) {
                "disabled"
            } else if (type == Types.NOTIFICATIONS) {
                "notify"
            } else {
                "indicate"
            }
        }

        init {
            descriptorValue = if (isEnabled) if (type == Types.NOTIFICATIONS) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.ENABLE_INDICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }
    }
}