package com.siliconlabs.bledemo.Bluetooth.BLE.Values;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * The plain byte array of the characteristic.
 */
public class ByteArrayValue {
    public static class Factory implements ValueFactory<byte[]> {
        @Override
        public byte[] create(BluetoothGattCharacteristic value) {
            return value.getValue();
        }
    }
}
