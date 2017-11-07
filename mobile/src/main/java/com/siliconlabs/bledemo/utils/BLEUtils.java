package com.siliconlabs.bledemo.utils;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import com.siliconlabs.bledemo.ble.GattCharacteristic;
import com.siliconlabs.bledemo.ble.GattService;

import java.util.List;
import java.util.UUID;

public class BLEUtils {
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    /**
     * Enumerated Indication State Handler
     */
    public enum Indications {
        /** Disable(d) Indications */
        DISABLED (false),
        /** Enable(d) Indications */
        ENABLED (true);

        /** Storage Field for Enabled bool passed from .CTor */
        private final boolean enabled;
        /** Storage Field for BluetoothGattDescriptor value based on Boolean */
        private final byte[] btValue;

        Indications(boolean enabled) {
            this.enabled = enabled;
            this.btValue = this.enabled ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
        }

        /**
         * Does this Value represent an Indications in an Enabled State?
         * @return Boolean, True = Enabled, False = Disabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Get the BluetoothGattDescriptor Value for This State.
         * @return BluetoothGattDescriptor value as Byte Array
         */
        public byte[] getDescriptorValue() {
            return btValue;
        }
    }

    /**
     * Set an Indication Setting for The Matching Characteristic of a Service
     * @param gatt The Bluetooth GATT
     * @param gattService the service we must find and match
     * @param gattCharacteristic the characteristic we must find and match
     * @param gattDescriptor the descriptor we must write to we must find and match
     * @param value The exact setting we are setting
     * @return Whether the instruction to write passed or failed.
     */
    public static boolean SetIndicationForCharacteristic(BluetoothGatt gatt, GattService gattService, GattCharacteristic gattCharacteristic, UUID gattDescriptor, Indications value) {
        boolean written = false;
        List<BluetoothGattService> services = gatt.getServices();
        for (BluetoothGattService service : services) {
            BluetoothGattCharacteristic characteristic = BLEUtils.getCharacteristic(service, gattService, gattCharacteristic);
            if (characteristic != null) {
                gatt.setCharacteristicNotification(characteristic, value.isEnabled());
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(gattDescriptor);
                if (descriptor != null) {
                    //writing this descriptor causes the device to send updates
                    descriptor.setValue(value.getDescriptorValue());
                    written = gatt.writeDescriptor(descriptor);
                }
                return written;
            }
        }

        return false;
    }

    /**
     * Set an Indication Setting for The Matching Characteristic of a Service
     * @param gatt The Bluetooth GATT
     * @param service the service we must find and match
     * @param characteristic the characteristic we must find and match
     * @param value The exact setting we are setting
     * @return Whether the instruction to write passed or failed.
     */
    public static boolean SetIndicationForCharacteristic(BluetoothGatt gatt, GattService service, GattCharacteristic characteristic, Indications value) {
        return SetIndicationForCharacteristic(gatt, service, characteristic, CLIENT_CHARACTERISTIC_CONFIG_UUID, value);
    }

    /**
     * Search for a specific characteristic given a BluetoothGattService
     * @param service the service to search through
     * @param targetService the service that you're looking for
     * @param targetCharacteristic the characteristic you're looking for
     * @return the characteristic, if it's found - null otherwise
     */
    public static BluetoothGattCharacteristic getCharacteristic(BluetoothGattService service, GattService targetService, GattCharacteristic targetCharacteristic) {
        if (service == null || targetService == null || targetCharacteristic == null) {
            return null;
        }
        GattService gattService = GattService.fromUuid(service.getUuid());
        if (gattService != null && gattService == targetService) {
            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            if (characteristics != null && !characteristics.isEmpty()) {
                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    GattCharacteristic gattCharacteristic = GattCharacteristic.fromUuid(characteristic.getUuid());
                    if (gattCharacteristic != null && gattCharacteristic == targetCharacteristic) {
                        return characteristic;
                    }
                }
            }
        }
        return null;
    }
}
