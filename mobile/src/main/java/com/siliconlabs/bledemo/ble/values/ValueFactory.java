package com.siliconlabs.bledemo.ble.values;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Values/class-instances that can be created from a Gatt Characteristic byte-array should
 * have a factory that implements this interface.
 */
public interface ValueFactory<T> {
    T create(BluetoothGattCharacteristic value);
}
