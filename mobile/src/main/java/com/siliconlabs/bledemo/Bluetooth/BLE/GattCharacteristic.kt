package com.siliconlabs.bledemo.bluetooth.ble

import android.bluetooth.BluetoothGattCharacteristic
import androidx.annotation.StringRes
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.bluetooth.ble.values.ByteArrayValue
import com.siliconlabs.bledemo.bluetooth.ble.values.TemperatureValue
import com.siliconlabs.bledemo.bluetooth.ble.values.ValueFactory
import com.siliconlabs.bledemo.utils.UuidUtils.parseIntFromUuidStart
import java.util.*

/**
 * Enumeration of the available gatt characteristics.
 * https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicsHome.aspx
 */
enum class GattCharacteristic {
    DeviceName(0x00002a00, "org.bluetooth.characteristic.gap.device_name"),
    Appearance(0x00002a01, "org.bluetooth.characteristic.gap.appearance", BluetoothGattCharacteristic.FORMAT_UINT16),
    ServiceChange(0x00002a05, "org.bluetooth.characteristic.gatt.service_changed", ByteArrayValue.Factory()),
    AlertLevel(0x00002a06, "org.bluetooth.characteristic.alert_level", BluetoothGattCharacteristic.FORMAT_UINT8),
    TxPowerLevel(0x00002a07, "org.bluetooth.characteristic.tx_power_level", BluetoothGattCharacteristic.FORMAT_SINT8),
    Temperature(0x00002a1c, "org.bluetooth.characteristic.temperature_measurement", TemperatureValue.Factory()),
    TemperatureType(0x00002a1d, "org.bluetooth.characteristic.temperature_type", BluetoothGattCharacteristic.FORMAT_UINT8),
    IntermediateTemperature(0x00002a1e, " org.bluetooth.characteristic.intermediate_temperature", TemperatureValue.Factory()),
    ManufacturerName(0x00002a29, "org.bluetooth.characteristic.manufacturer_name_string"),
    ModelNumberString(0x00002a24, "org.bluetooth.characteristic.model_number_string"),
    SystemId(0x00002a23, "org.bluetooth.characteristic.system_id,", BluetoothGattCharacteristic.FORMAT_UINT32),
    BatteryLevel(0x00002a19, "org.bluetooth.characteristic.battery_level", BluetoothGattCharacteristic.FORMAT_UINT8),
    DockStatus(-0x4885305b, "com.sensedriver.characteristic.hud.dock_status"),

    OtaControl("f7bf3564-fb6d-4e53-88a4-5e37e0326063", "com.silabs.characteristic.ota_control", R.string.ota_control_attribute_characteristic_name, BluetoothGattCharacteristic.FORMAT_UINT8),
    OtaData("984227f3-34fc-4045-a5d0-2c581f81a153", "com.silabs.characteristic.ota_data", R.string.ota_data_characteristic_name, BluetoothGattCharacteristic.FORMAT_UINT8),
    FwVersion("4f4a2368-8cca-451e-bfff-cf0e2ee23e9f", "com.silabs.characteristic.fw_version", R.string.fw_version_characteristic_name, BluetoothGattCharacteristic.FORMAT_UINT8),
    OtaVersion("4cc07bcf-0868-4b32-9dad-ba4cc41e5316", "com.silabs.characteristic.ota_version", R.string.ota_version_characteristic_name, BluetoothGattCharacteristic.FORMAT_UINT8),
    BootloaderVersion("25f05c0a-e917-46e9-b2a5-aa2be1245afe", "com.silabs.characteristic.ota_version", R.string.bootloader_version_characteristic_name, BluetoothGattCharacteristic.FORMAT_UINT8),
    ApplicationVersion("0d77cc11-4ac1-49f2-bfa9-cd96ac7a92f8", "com.silabs.characteristic.ota_version", R.string.application_version_characteristic_name, BluetoothGattCharacteristic.FORMAT_UINT8),

    Light("76e137ac-b15f-49d7-9c4c-e278e6492ad9", "custom.type", BluetoothGattCharacteristic.FORMAT_UINT8),
    TriggerSource("2f16ee52-0bfd-4597-85d4-a5141fdbae15", "custom.type", BluetoothGattCharacteristic.FORMAT_UINT8),
    SourceAddress("82a1cb54-3921-4c9c-ba34-34f78bab9a1b", "custom.type", BluetoothGattCharacteristic.FORMAT_UINT32),

    RangeTestDestinationId("41ded549-4298-4911-8c16-3088a7e41d5f", "custom.type", BluetoothGattCharacteristic.FORMAT_UINT8),
    RangeTestSourceId("9438acdf-42f5-463d-9c73-c5a3427fa731", "custom.type", BluetoothGattCharacteristic.FORMAT_UINT8),
    RangeTestPacketsReceived("6c19509b-f0d1-4f0e-84ce-464dba7c573a", "custom.type", BluetoothGattCharacteristic.FORMAT_UINT16),
    RangeTestPacketsSend("eb2438fe-a09e-4015-b511-91f52b581639", "custom.type", BluetoothGattCharacteristic.FORMAT_UINT16),
    RangeTestPacketsCount("d6781c5d-9a48-4c97-80b8-f8082030ca5d", "custom.type", BluetoothGattCharacteristic.FORMAT_UINT16),
    RangeTestPacketsRequired("6defa84c-75e1-4b5f-8729-140cdfaee745", "custom.type", BluetoothGattCharacteristic.FORMAT_UINT16),
    RangeTestPER("d1e93c9c-62e0-4962-9cb3-df86d419b5da", "custom.type", BluetoothGattCharacteristic.FORMAT_UINT16),
    RangeTestMA("cde92958-3f56-4bc6-9e6b-11b5c551e903", "custom.type", BluetoothGattCharacteristic.FORMAT_UINT16),
    RangeTestChannel("e8811f97-f736-4e52-a9f8-4b771792c114", "custom.type", BluetoothGattCharacteristic.FORMAT_UINT16),
    RangeTestRadioMode("660b91bd-1a4c-428a-9e7e-27ce8a945618", "custom.type", BluetoothGattCharacteristic.FORMAT_UINT8),
    RangeTestFrequency("3a5404eb-299b-4a3c-a76c-71bf52af1457", "custom.type", BluetoothGattCharacteristic.FORMAT_UINT16),
    RangeTestTxPower("16be0ebf-5b8d-45d8-8128-d1abb4b71788", "custom.type", BluetoothGattCharacteristic.FORMAT_SINT16),
    RangeTestPayload("0212cda0-4ae2-471a-9743-a318374f14de", "custom.type", BluetoothGattCharacteristic.FORMAT_UINT8),
    RangeTestMaSize("b9c9bc5a-f218-4e44-b632-743880e8c7c1", "custom.type", BluetoothGattCharacteristic.FORMAT_UINT8),
    RangeTestLog("d05bd818-6000-489f-8cc0-aa4b93a5edaf", "custom.type", BluetoothGattCharacteristic.FORMAT_UINT8),
    RangeTestIsRunning("3d28d0e4-2669-4784-a80a-ed8722a563c6", "custom.type", BluetoothGattCharacteristic.FORMAT_UINT8),
    RangePhyConfig("8a354244-c1ff-4318-8834-0e86efac1067", "custom.type", BluetoothGattCharacteristic.FORMAT_UINT8),
    RangePhyList("05dca698-76e2-4c30-8e22-2ce22e81b968", "custom.type", BluetoothGattCharacteristic.FORMAT_UINT8),

    LedControl("5b026510-4088-c297-46d8-be6c736a087a", "custom.type", R.string.led_control_characteristic_name),
    ReportButton("61a885a4-41c3-60d0-9a53-6d652a70d29c", "custom.type", R.string.report_button_characteristic_name),

    ThroughputIndications("6109b631-a643-4a51-83d2-2059700ad49f", "custom.type", customNameId = R.string.indications_characteristic_name),
    ThroughputNotifications("47b73dd6-dee3-4da1-9be0-f5c539a9a4be", "custom.type", customNameId = R.string.notifications_characteristic_name),
    ThroughputTransmissionOn("be6b6be1-cd8a-4106-9181-5ffe2bc67718", "custom.type", customNameId = R.string.transmission_on_characteristic_name),
    ThroughputResult("adf32227-b00f-400c-9eeb-b903a6cc291b", "custom.type", customNameId = R.string.throughput_result_characteristic_name),

    ThroughputPhyStatus("00a82b93-0feb-2739-72be-abda1f5993d0", "custom.type", customNameId = R.string.connection_phy_characteristic_name),
    ThroughputConnectionInterval("0a32f5a6-0a6c-4954-f413-a698faf2c664", "custom.type", customNameId = R.string.connection_interval_characteristic_name),
    ThroughputSlaveLatency("ff629b92-332b-e7f7-975f-0e535872ddae", "custom.type", customNameId = R.string.slave_latency_characteristic_name),
    ThroughputSupervisionTimeout("67e2c4f2-2f50-914c-a611-adb3727b056d", "custom.type", customNameId = R.string.supervision_timeout_characteristic_name),
    ThroughputPduSize("30cc364a-0739-268c-4926-36f112631e0c", "custom.type", customNameId = R.string.pdu_size_characteristic_name),
    ThroughputMtuSize("3816df2f-d974-d915-d26e-78300f25e86e", "custom.type", customNameId = R.string.mtu_size_characteristic_name);
    /**
     * The so-called "Assigned Number" of this characteristic.
     */
    val number: Int

    /**
     * The fully qualified "Type" of this characteristic.
     */
    val type: String

    /**
     * The simple type of this characteristic. If 0, type is a String, If
     */
    val format: Int

    /**
     * Resource ID for custom name
     */
    @StringRes var customNameId: Int? = null

    private val valueFactory: ValueFactory<*>?
    val uuid: UUID

    constructor(number: Int, type: String, format: Int = 0) {
        this.number = number
        this.type = type
        this.format = format
        valueFactory = null
        uuid = UUID.fromString(String.format(Locale.US, FORMAT_STR, number))
        BluetoothLEGatt.GATT_CHARACTER_DESCS.put(number, this)
    }

    constructor(number: Int, type: String, valueFactory: ValueFactory<*>?) {
        this.number = number
        this.type = type
        format = -1
        this.valueFactory = valueFactory
        uuid = UUID.fromString(String.format(Locale.US, FORMAT_STR, number))
        BluetoothLEGatt.GATT_CHARACTER_DESCS.put(number, this)
    }

    constructor(uuid: String?, type: String, customNameId: Int? = null, format: Int = 0) {
        number = parseIntFromUuidStart(uuid!!)
        this.type = type
        this.format = format
        valueFactory = null
        this.uuid = UUID.fromString(uuid)
        BluetoothLEGatt.GATT_CHARACTER_DESCS.put(number, this)
        this.customNameId = customNameId
    }

    fun <T> createValue(valueBytes: BluetoothGattCharacteristic?): T {
        checkNotNull(valueFactory) { "valueFactory is null" }
        return valueFactory.create(valueBytes!!) as T
    }

    companion object {
        private const val FORMAT_STR = "%08x-0000-1000-8000-00805f9b34fb"
        fun fromUuid(uuid: UUID): GattCharacteristic? {
            for (i in values().indices) {
                val characteristic = values()[i]
                if (characteristic.uuid == uuid) {
                    return characteristic
                }
            }
            return null
        }
    }
}