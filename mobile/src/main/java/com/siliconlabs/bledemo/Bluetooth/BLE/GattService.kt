package com.siliconlabs.bledemo.bluetooth.ble

import androidx.annotation.StringRes
import com.siliconlabs.bledemo.R
import java.util.*

/**
 * Enumeration of all available Gatt Services.
 * https://developer.bluetooth.org/gatt/services/Pages/ServicesHome.aspx
 */
enum class GattService {
    GenericAccess(0x00001800, "org.bluetooth.service.generic_access",
            GattCharacteristic.DeviceName,
            GattCharacteristic.Appearance),
    GenericAttribute(0x00001801, "org.bluetooth.service.generic_attribute", GattCharacteristic.ServiceChange),
    ImmediateAlert(0x00001802, "org.bluetooth.service.immediate_alert", GattCharacteristic.AlertLevel),
    LinkLoss(0x00001803, "org.bluetooth.service.link_loss", GattCharacteristic.AlertLevel),
    TxPower(0x00001804, "org.bluetooth.service.tx_power", GattCharacteristic.TxPowerLevel),
    HealthThermometer(0x00001809, "org.bluetooth.service.health_thermometer",
            GattCharacteristic.Temperature,
            GattCharacteristic.TemperatureType,
            GattCharacteristic.IntermediateTemperature),
    DeviceInformation(0x0000180a, "org.bluetooth.service.device_information",
            GattCharacteristic.ManufacturerName,
            GattCharacteristic.ModelNumberString,
            GattCharacteristic.SystemId),
    BatteryService(0x0000180f, "org.bluetooth.service.battery_service", GattCharacteristic.BatteryLevel),
    HudDocking(-0x23fc6ff3, "com.sensedriver.service.hud_docking", GattCharacteristic.DockStatus),
    OtaService("1d14d6ee-fd63-4fa1-bfa4-8f47b42119f0", "com.silabs.service.ota", R.string.ota_service_name,
            GattCharacteristic.OtaControl,
            GattCharacteristic.OtaData,
            GattCharacteristic.FwVersion,
            GattCharacteristic.OtaVersion),
    ThreadLightService("dd1c077d-d306-4b30-846a-4f55cc35767a", "custom.type",
            GattCharacteristic.Light,
            GattCharacteristic.TriggerSource,
            GattCharacteristic.SourceAddress),
    ConnectLightService("62792313-adf2-4fc9-974d-fab9ddf2622c", "custom.type",
            GattCharacteristic.Light,
            GattCharacteristic.TriggerSource,
            GattCharacteristic.SourceAddress),
    ZigbeeLightService("bae55b96-7d19-458d-970c-50613d801bc9", "custom.type",
            GattCharacteristic.Light,
            GattCharacteristic.TriggerSource,
            GattCharacteristic.SourceAddress),
    ProprietaryLightService("63f596e4-b583-4078-bfc3-b04225378713", "custom.type",
            GattCharacteristic.Light,
            GattCharacteristic.TriggerSource,
            GattCharacteristic.SourceAddress),
    RangeTestService("530aa649-17e6-4d62-9f20-9e393b177e63", "custom.type",
            GattCharacteristic.RangeTestDestinationId,
            GattCharacteristic.RangeTestSourceId,
            GattCharacteristic.RangeTestPacketsReceived,
            GattCharacteristic.RangeTestPacketsSend,
            GattCharacteristic.RangeTestPacketsCount,
            GattCharacteristic.RangeTestPacketsRequired,
            GattCharacteristic.RangeTestPER,
            GattCharacteristic.RangeTestMA,
            GattCharacteristic.RangeTestChannel,
            GattCharacteristic.RangeTestRadioMode,
            GattCharacteristic.RangeTestFrequency,
            GattCharacteristic.RangeTestTxPower,
            GattCharacteristic.RangeTestPayload,
            GattCharacteristic.RangeTestMaSize,
            GattCharacteristic.RangeTestLog,
            GattCharacteristic.RangeTestIsRunning),
    BlinkyExample("de8a5aac-a99b-c315-0c80-60d4cbb51224", "custom.type", R.string.blinky_service_name,
            GattCharacteristic.LedControl,
            GattCharacteristic.ReportButton),
    ThroughputTestService("bbb99e70-fff7-46cf-abc7-2d32c71820f2", "custom.type", R.string.throughput_test_service_name,
            GattCharacteristic.ThroughputIndications,
            GattCharacteristic.ThroughputNotifications,
            GattCharacteristic.ThroughputTransmissionOn,
            GattCharacteristic.ThroughputResult),
    ThroughputInformationService("ba1e0e9f-4d81-bae3-f748-3ad55da38b46", "custom.type", R.string.throughput_information_service_name,
            GattCharacteristic.ThroughputPhyStatus,
            GattCharacteristic.ThroughputConnectionInterval,
            GattCharacteristic.ThroughputSlaveLatency,
            GattCharacteristic.ThroughputSupervisionTimeout,
            GattCharacteristic.ThroughputPduSize,
            GattCharacteristic.ThroughputMtuSize
    ),
    PowerSource("ec61a454-ed00-a5e8-b8f9-de9ec026ec51", "custom.type",
            GattCharacteristic.PowerSource
    ),
    EnvironmentalSensing(0x0000181a, "org.bluetooth.service.environmental_sensing",
            GattCharacteristic.UvIndex,
            GattCharacteristic.Pressure,
            GattCharacteristic.EnvironmentTemperature,
            GattCharacteristic.Humidity,
            GattCharacteristic.SoundLevel,
            GattCharacteristic.AmbientLightReact,
            GattCharacteristic.AmbientLightSense
    ),
    IndoorAirQuality("efd658ae-c400-ef33-76e7-91b00019103b", "custom.type",
            GattCharacteristic.CO2Reading,
            GattCharacteristic.TVOCReading
    ),
    HallEffect("f598dbc5-2f00-4ec5-9936-b3d1aa4f957f", "custom.type",
            GattCharacteristic.HallState,
            GattCharacteristic.HallFieldStrength,
            GattCharacteristic.HallControlPoint
    ),
    AmbientLight("d24c4f4e-17a7-4548-852c-abf51127368b", "custom.type",
            GattCharacteristic.AmbientLightReact
    ),
    AutomationIo(0x00001815, "org.bluetooth.service.automation_io",
            GattCharacteristic.Digital
    ),
    UserInterface("fcb89c40-c600-59f3-7dc3-5ece444a401b", "custom.type",
            GattCharacteristic.RgbLeds
    ),
    Motion("a4e649f4-4be5-11e5-885d-feff819cdc9f", "custom.type",
            GattCharacteristic.Acceleration,
            GattCharacteristic.Orientation,
            GattCharacteristic.Calibration
    ),
    WifiCommissioningService("0000aabb-0000-1000-8000-00805f9b34fb", "custom.type",
            GattCharacteristic.WifiCommissioningWrite,
            GattCharacteristic.WifiCommissioningRead,
            GattCharacteristic.WifiCommissioningNotify
    );

    /**
     * The so-called "Assigned Number" of this service.
     */
    val number: UUID

    /**
     * The "Type" of this service (fully qualified name).
     */
    val type: String

    /**
     * Resource ID for custom name
     */
    @StringRes var customNameId: Int? = null

    /**
     * Available gatt characteristics for this service.
     */
    private val availableCharacteristics: Array<GattCharacteristic>

    constructor(number: Int, type: String, vararg availableCharacteristics: GattCharacteristic) {
        this.number = UUID.fromString(String.format(Locale.US, FORMAT_STR, number))
        this.type = type
        this.availableCharacteristics = arrayOf(*availableCharacteristics)
    }

    constructor(uuid: String?, type: String, vararg availableCharacteristics: GattCharacteristic) {
        number = UUID.fromString(uuid)
        this.type = type
        this.availableCharacteristics = arrayOf(*availableCharacteristics)
    }

    constructor(uuid: String?, type: String, customNameId: Int, vararg availableCharacteristics: GattCharacteristic) {
        number = UUID.fromString(uuid)
        this.type = type
        this.availableCharacteristics = arrayOf(*availableCharacteristics)
        this.customNameId = customNameId
    }

    companion object {
        private const val FORMAT_STR = "%08x-0000-1000-8000-00805f9b34fb"
        val UUID_MASK = UUID.fromString("0000ffff-0000-0000-0000-000000000000")
        fun fromUuid(uuid: UUID): GattService? {
            for (i in values().indices) {
                val service = values()[i]
                if (service.number == uuid) {
                    return service
                }
            }
            return null
        }
    }
}
