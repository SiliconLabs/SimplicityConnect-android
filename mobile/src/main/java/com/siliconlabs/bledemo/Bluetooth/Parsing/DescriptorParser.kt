package com.siliconlabs.bledemo.Bluetooth.Parsing

import android.bluetooth.BluetoothGattDescriptor
import com.siliconlabs.bledemo.utils.Converters
import com.siliconlabs.bledemo.utils.StringUtils.removeWhitespaceAndCommaIfNeeded
import java.lang.StringBuilder
import java.util.*

class DescriptorParser(val descriptor: BluetoothGattDescriptor) {

    fun getFormattedValue(): String {
        val uuid = descriptor.uuid
        val value = descriptor.value

        if (value.isEmpty()) {
            return ""
        }

        when (uuid) {
            ENVIRONMENTAL_SENSING_CONFIGURATION_UUID -> {
                return getEnvironmentalSensingConfiguration(value)
            }
            ENVIRONMENTAL_SENSING_MEASUREMENT_UUID -> {
                //Quite complex to implement - display in HEX format
            }
            ENVIRONMENTAL_SENSING_TRIGGER_SETTING_UUID -> {
                //Quite complex to implement - display in HEX format
            }
            EXTERNAL_REPORT_REFERENCE_UUID -> {
                //Display in HEX format
            }
            CHARACTERISTIC_AGGREGATE_FORMAT_UUID -> {
                //Display in HEX format
            }
            CHARACTERISTIC_EXTENDED_PROPERTIES_UUID -> {
                return getCharacteristicExtendedProperties(value)
            }
            CHARACTERISTIC_PRESENTATION_FORMAT_UUID -> {
                //Quite complex to implement - display in HEX format
            }
            CHARACTERISTIC_USER_DESCRIPTION_UUID -> {
                return getCharacteristicUserDescription(value)
            }
            CLIENT_CHARACTERISTIC_CONFIGURATION_UUID -> {
                return getClientCharacteristicConfiguration(value)
            }
            SERVER_CHARACTERISTIC_CONFIGURATION_UUID -> {
                return getServerCharacteristicConfiguration(value)
            }
            NUMBER_OF_DIGITALS_UUID -> {
                return getNumberOfDigitals(value)
            }
            REPORT_REFERENCE_UUID -> {
                return getReportReference(value)
            }
            TIME_TRIGGER_SETTING_UUID -> {
                //Display in HEX format
            }
            VALID_RANGE_UUID -> {
                return getValidRange(value)
            }
            VALUE_TRIGGER_SETTING_UUID -> {
                //Display in HEX format
            }
        }

        return "0x".plus(Converters.bytesToHex(value).toUpperCase(Locale.ROOT))
    }


    private fun getEnvironmentalSensingConfiguration(bytes: ByteArray): String {
        val firstByte = bytes[0].toInt()

        return when (firstByte) {
            0 -> "Boolean AND"
            1 -> "Boolean OR"
            else -> getUnknownValue(bytes)
        }
    }

    private fun getCharacteristicExtendedProperties(bytes: ByteArray): String {
        val firstByte = bytes[0].toInt()
        val result = StringBuilder()

        if (firstByte and 0b0000_0001 == 1) result.append("Reliable Write enabled, ")
        else result.append("Reliable Write disabled, ")

        if (firstByte and 0b0000_0010 == 2) result.append("Writable Auxiliaries enabled, ")
        else result.append("Writable Auxiliaries disabled, ")

        removeWhitespaceAndCommaIfNeeded(result)

        return result.toString()
    }


    private fun getCharacteristicUserDescription(bytes: ByteArray): String {
        return Converters.getAsciiValue(bytes)
    }


    private fun getClientCharacteristicConfiguration(bytes: ByteArray): String {
        val firstByte = bytes[0].toInt()
        val result = StringBuilder()

        if (firstByte and 0b0000_0001 == 1) result.append("Notifications enabled, ")
        else result.append("Notifications disabled, ")

        if (firstByte and 0b0000_0010 == 2) result.append("Indications enabled, ")
        else result.append("Indications disabled, ")

        removeWhitespaceAndCommaIfNeeded(result)

        return result.toString()
    }


    private fun getServerCharacteristicConfiguration(bytes: ByteArray): String {
        val firstByte = bytes[0].toInt()

        return if (firstByte and 0b0000_0001 == 1) "Broadcasts enabled"
        else "Broadcasts disabled"
    }


    private fun getNumberOfDigitals(bytes: ByteArray): String {
        return Converters.byteToUnsignedInt(bytes[0]).toString()
    }


    private fun getReportReference(bytes: ByteArray): String {
        return if (bytes.size != 2) {
            return getUnknownValue(bytes)
        } else {
            val reportId = bytes[0]
            val reportType = bytes[1]

            val result = StringBuilder()
            result.append("Report ID: 0x").append(Converters.getHexValue(reportId).toUpperCase(Locale.ROOT)).append("\n")
            result.append("Report Type: 0x").append(Converters.getHexValue(reportType).toUpperCase(Locale.ROOT))

            result.toString()
        }
    }


    private fun getValidRange(bytes: ByteArray): String {
        val size = bytes.size

        if (size % 2 != 0) {
            return getUnknownValue(bytes)
        } else {
            val result = StringBuilder()

            result.append("Lower inclusive value: 0x")
            for (i in 0 until size / 2) {
                result.append(Converters.getHexValue(bytes[i]).toUpperCase(Locale.ROOT))
            }

            result.append("\nUpper inclusive value: 0x")
            for (i in size / 2 until size) {
                result.append(Converters.getHexValue(bytes[i]).toUpperCase(Locale.ROOT))
            }

            return result.toString()
        }
    }

    private fun getUnknownValue(bytes: ByteArray): String {
        return "Unknown value: 0x".plus(Converters.bytesToHex(bytes).toUpperCase(Locale.ROOT))
    }


    companion object {
        private val ENVIRONMENTAL_SENSING_CONFIGURATION_UUID = UUID.fromString("0000290B-0000-1000-8000-00805f9b34fb")
        private val ENVIRONMENTAL_SENSING_MEASUREMENT_UUID = UUID.fromString("0000290C-0000-1000-8000-00805f9b34fb")
        private val ENVIRONMENTAL_SENSING_TRIGGER_SETTING_UUID = UUID.fromString("0000290D-0000-1000-8000-00805f9b34fb")
        private val EXTERNAL_REPORT_REFERENCE_UUID = UUID.fromString("00002907-0000-1000-8000-00805f9b34fb")
        private val CHARACTERISTIC_AGGREGATE_FORMAT_UUID = UUID.fromString("00002905-0000-1000-8000-00805f9b34fb")
        private val CHARACTERISTIC_EXTENDED_PROPERTIES_UUID = UUID.fromString("00002900-0000-1000-8000-00805f9b34fb")
        private val CHARACTERISTIC_PRESENTATION_FORMAT_UUID = UUID.fromString("00002904-0000-1000-8000-00805f9b34fb")
        private val CHARACTERISTIC_USER_DESCRIPTION_UUID = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb")
        private val CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private val SERVER_CHARACTERISTIC_CONFIGURATION_UUID = UUID.fromString("00002903-0000-1000-8000-00805f9b34fb")
        private val NUMBER_OF_DIGITALS_UUID = UUID.fromString("00002909-0000-1000-8000-00805f9b34fb")
        private val REPORT_REFERENCE_UUID = UUID.fromString("00002908-0000-1000-8000-00805f9b34fb")
        private val TIME_TRIGGER_SETTING_UUID = UUID.fromString("0000290E-0000-1000-8000-00805f9b34fb")
        private val VALID_RANGE_UUID = UUID.fromString("00002906-0000-1000-8000-00805f9b34fb")
        private val VALUE_TRIGGER_SETTING_UUID = UUID.fromString("0000290A-0000-1000-8000-00805f9b34fb")
    }

}
