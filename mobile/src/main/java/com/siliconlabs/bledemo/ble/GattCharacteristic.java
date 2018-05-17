package com.siliconlabs.bledemo.ble;

import android.bluetooth.BluetoothGattCharacteristic;

import com.siliconlabs.bledemo.ble.values.ByteArrayValue;
import com.siliconlabs.bledemo.ble.values.TemperatureValue;
import com.siliconlabs.bledemo.ble.values.ValueFactory;
import com.siliconlabs.bledemo.utils.UuidUtils;

import java.util.Locale;
import java.util.UUID;

/**
 * Enumeration of the available gatt characteristics.
 * https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicsHome.aspx
 */
public enum GattCharacteristic {
    DeviceName(0x00002a00, "org.bluetooth.characteristic.gap.device_name"),
    Appearance(0x00002a01, "org.bluetooth.characteristic.gap.appearance", BluetoothGattCharacteristic.FORMAT_UINT16),
    ServiceChange(0x00002a05, "org.bluetooth.characteristic.gatt.service_changed", new ByteArrayValue.Factory()),
    AlertLevel(0x00002a06, "org.bluetooth.characteristic.alert_level", BluetoothGattCharacteristic.FORMAT_UINT8),
    TxPowerLevel(0x00002a07, "org.bluetooth.characteristic.tx_power_level", BluetoothGattCharacteristic.FORMAT_SINT8),
    Temperature(0x00002a1c, "org.bluetooth.characteristic.temperature_measurement", new TemperatureValue.Factory()),
    TemperatureType(0x00002a1d, " org.bluetooth.characteristic.temperature_type", BluetoothGattCharacteristic.FORMAT_UINT8),
    IntermediateTemperature(0x00002a1e, " org.bluetooth.characteristic.intermediate_temperature", new TemperatureValue.Factory()),
    ManufacturerName(0x00002a29, "org.bluetooth.characteristic.manufacturer_name_string"),
    ModelNumberString(0x00002a24, "org.bluetooth.characteristic.model_number_string"),
    SystemId(0x00002a23, "org.bluetooth.characteristic.system_id,", BluetoothGattCharacteristic.FORMAT_UINT32),
    BatteryLevel(0x00002a19, "org.bluetooth.characteristic.battery_level", BluetoothGattCharacteristic.FORMAT_UINT8),
    DockStatus(0xb77acfa5, "com.sensedriver.characteristic.hud.dock_status"),
    OtaControl(0xf7bf3564, "com.silabs.characteristic.ota_control",BluetoothGattCharacteristic.FORMAT_UINT8),
    OtaData(0x984227F3, "com.silabs.characteristic.ota_data",BluetoothGattCharacteristic.FORMAT_UINT8),
    FwVersion(0x4f4a2368, "com.silabs.characteristic.fw_version", BluetoothGattCharacteristic.FORMAT_UINT8),
    OtaVersion(0x4cc07bcf, "com.silabs.characteristic.ota_version", BluetoothGattCharacteristic.FORMAT_UINT8),
    Light("76e137ac-b15f-49d7-9c4c-e278e6492ad9", "custom.type", BluetoothGattCharacteristic.FORMAT_UINT8),
    TriggerSource("2f16ee52-0bfd-4597-85d4-a5141fdbae15", "custom.type", BluetoothGattCharacteristic.FORMAT_UINT8),
    SourceAddress("82a1cb54-3921-4c9c-ba34-34f78bab9a1b", "custom.type", BluetoothGattCharacteristic.FORMAT_UINT32);

    private static final String FORMAT_STR = "%08x-0000-1000-8000-00805f9b34fb";
    /**
     * The so-called "Assigned Number" of this characteristic.
     */
    public final int number;
    /**
     * The fullly qualified "Type" of this characteristic.
     */
    public final String type;
    /**
     * The simple type of this characterstic. If 0, type is a String, If
     */
    public final int format;
    private final ValueFactory<?> valueFactory;
    public final UUID uuid;

    GattCharacteristic(int number, String type) {
        this(number, type, 0);
    }

    GattCharacteristic(int number, String type, int format) {
        this.number = number;
        this.type = type;
        this.format = format;
        this.valueFactory = null;
        this.uuid = UUID.fromString(String.format(Locale.US, FORMAT_STR, number));

        BluetoothLEGatt.GATT_CHARACTER_DESCS.put(number, this);
    }

    GattCharacteristic(int number, String type, ValueFactory valueFactory) {
        this.number = number;
        this.type = type;
        this.format = -1;
        this.valueFactory = valueFactory;
        this.uuid = UUID.fromString(String.format(Locale.US, FORMAT_STR, number));

        BluetoothLEGatt.GATT_CHARACTER_DESCS.put(number, this);
    }

    GattCharacteristic(String uuid, String type, int format) {
        this.number = UuidUtils.parseIntFromUuidStart(uuid);
        this.type = type;
        this.format = format;
        this.valueFactory = null;
        this.uuid = UUID.fromString(uuid);

        BluetoothLEGatt.GATT_CHARACTER_DESCS.put(number, this);
    }

    public <T> T createValue(BluetoothGattCharacteristic valueBytes) {
        if (valueFactory == null) {
            throw new IllegalStateException("valueFactory is null");
        }

        //noinspection unchecked
        return (T) valueFactory.create(valueBytes);
    }

    public static GattCharacteristic fromUuid(UUID uuid) {
        for (int i = 0; i < values().length; i++) {
            GattCharacteristic characteristic = values()[i];
            if (characteristic.uuid.equals(uuid)) {
                return characteristic;
            }
        }
        return null;
    }
}
