package com.siliconlabs.bledemo.ble.values;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Gatt Characteristic value representing a temperature measurement.
 */
public class TemperatureValue {
    public final float temperature;
    public final boolean isFahrenheit;

    public final Integer temperatureType;
    public final int[] timeStamp;

    public static class Factory implements ValueFactory<TemperatureValue> {
        @Override
        public TemperatureValue create(BluetoothGattCharacteristic value) {
            return new TemperatureValue(value);
        }
    }

    private TemperatureValue(BluetoothGattCharacteristic value) {
        // https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.temperature_measurement.xml
        final int flags = value.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) & 0x00000007;
        temperature = value.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 1);
        isFahrenheit = (flags & 0x00000001) != 0;

        switch (flags) {
            case 2:
            case 3:
                timeStamp = getTimeStamp(value, 1 + 4);
                temperatureType = null;
                break;

            case 4:
            case 5:
                timeStamp = null;
                // https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.temperature_type.xml
                temperatureType = value.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1 + 4);
                break;

            case 6:
            case 7:
                timeStamp = getTimeStamp(value, 1 + 4);
                temperatureType = value.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1 + 4 + 7);
                break;

            default:
                timeStamp = null;
                temperatureType = null;
                break;

        }
    }

    private int[] getTimeStamp(BluetoothGattCharacteristic value, int offset) {
        int year, month, day, hours, minutes, seconds;
        year = value.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
        month = value.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 2);
        day = value.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 3);
        hours = value.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 4);
        minutes = value.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 5);
        seconds = value.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 6);

        return new int[]{year, month, day, hours, minutes, seconds};
    }
}
