package com.siliconlabs.bledemo.advertiser.utils

import android.content.Context
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.advertiser.enums.AdvertisingMode
import com.siliconlabs.bledemo.advertiser.enums.DataType
import com.siliconlabs.bledemo.advertiser.enums.Phy
import com.siliconlabs.bledemo.advertiser.models.DataTypeItem
import com.siliconlabs.bledemo.advertiser.models.Service16Bit

class Translator(val context: Context) {

    fun getValuesAsStringList(list: List<Any>): ArrayList<String> {
        val result = ArrayList<String>()
        for (elem in list) {
            if (elem == AdvertisingMode.CONNECTABLE_SCANNABLE) result.add(context.getString(R.string.advertiser_mode_connectable_scannable))
            if (elem == AdvertisingMode.CONNECTABLE_NON_SCANNABLE) result.add(context.getString(R.string.advertiser_mode_connectable_non_scannable))
            if (elem == AdvertisingMode.NON_CONNECTABLE_SCANNABLE) result.add(context.getString(R.string.advertiser_mode_non_connectable_scannable))
            if (elem == AdvertisingMode.NON_CONNECTABLE_NON_SCANNABLE) result.add(context.getString(R.string.advertiser_mode_non_connectable_non_scannable))
            if (elem == Phy.PHY_1M) result.add(context.getString(R.string.advertising_extension_phy_le_1m))
            if (elem == Phy.PHY_2M) result.add(context.getString(R.string.advertising_extension_phy_le_2m))
            if (elem == Phy.PHY_LE_CODED) result.add(context.getString(R.string.advertising_extension_phy_le_coded))
        }
        return result
    }

    fun getPhyAsString(value: Phy): String {
        return when (value) {
            Phy.PHY_1M -> context.getString(R.string.advertising_extension_phy_le_1m)
            Phy.PHY_2M -> context.getString(R.string.advertising_extension_phy_le_2m)
            Phy.PHY_LE_CODED -> context.getString(R.string.advertising_extension_phy_le_coded)
        }
    }

    fun getStringAsPhy(value: String): Phy {
        return when (value) {
            context.getString(R.string.advertising_extension_phy_le_1m) -> Phy.PHY_1M
            context.getString(R.string.advertising_extension_phy_le_2m) -> Phy.PHY_2M
            else -> Phy.PHY_LE_CODED
        }
    }

    fun getStringAsAdvertisingMode(value: String): AdvertisingMode {
        return when (value) {
            context.getString(R.string.advertiser_mode_connectable_scannable) -> AdvertisingMode.CONNECTABLE_SCANNABLE
            context.getString(R.string.advertiser_mode_connectable_non_scannable) -> AdvertisingMode.CONNECTABLE_NON_SCANNABLE
            context.getString(R.string.advertiser_mode_non_connectable_scannable) -> AdvertisingMode.NON_CONNECTABLE_SCANNABLE
            else -> AdvertisingMode.NON_CONNECTABLE_NON_SCANNABLE
        }
    }

    fun getAdvertisingModeAsString(value: AdvertisingMode): String {
        return when (value) {
            AdvertisingMode.CONNECTABLE_SCANNABLE -> context.getString(R.string.advertiser_mode_connectable_scannable)
            AdvertisingMode.CONNECTABLE_NON_SCANNABLE -> context.getString(R.string.advertiser_mode_connectable_non_scannable)
            AdvertisingMode.NON_CONNECTABLE_SCANNABLE -> context.getString(R.string.advertiser_mode_non_connectable_scannable)
            AdvertisingMode.NON_CONNECTABLE_NON_SCANNABLE -> context.getString(R.string.advertiser_mode_non_connectable_non_scannable)
        }
    }

    fun getDataTypeAsString(value: DataType): String {
        return when (value) {
            DataType.FLAGS -> context.getString(R.string.advertiser_data_type_flags)
            DataType.COMPLETE_16_BIT -> context.getString(R.string.advertiser_data_type_complete_16bit_service)
            DataType.COMPLETE_128_BIT -> context.getString(R.string.advertiser_data_type_complete_128bit_service)
            DataType.COMPLETE_LOCAL_NAME -> context.getString(R.string.advertiser_data_type_complete_local_name)
            DataType.TX_POWER -> context.getString(R.string.advertiser_data_type_tx_power)
            DataType.MANUFACTURER_SPECIFIC_DATA -> context.getString(R.string.advertiser_data_type_manufacturer_specific_data)
        }
    }

    fun getString(value: Any?): String {
        return when (value) {
            is Phy -> getPhyAsString(value)
            is AdvertisingMode -> getAdvertisingModeAsString(value)
            is DataType -> getDataTypeAsString(value)
            else -> ""
        }
    }

    fun getAdvertisingDataTypes(): ArrayList<DataTypeItem> {
        val list = ArrayList<DataTypeItem>()
        list.add(DataTypeItem("0x09", context.getString(R.string.advertiser_data_type_complete_local_name)))
        list.add(DataTypeItem("0xFF", context.getString(R.string.advertiser_data_type_manufacturer_specific_data)))
        list.add(DataTypeItem("0x0A", context.getString(R.string.advertiser_data_type_tx_power)))
        list.add(DataTypeItem("0x03", context.getString(R.string.advertiser_data_type_complete_16bit_service)))
        list.add(DataTypeItem("0x07", context.getString(R.string.advertiser_data_type_complete_128bit_service)))
        return list
    }

    fun get16BitServices(): List<Service16Bit> {
        val list = ArrayList<Service16Bit>()
        list.add(Service16Bit(0x1800, "Generic Access"))
        list.add(Service16Bit(0x1811, "Alert Notification Service"))
        list.add(Service16Bit(0x1815, "Automation IO"))
        list.add(Service16Bit(0x180F, "Battery Service"))
        list.add(Service16Bit(0x183B, "Binary Sensor"))
        list.add(Service16Bit(0x1810, "Blood Pressure"))
        list.add(Service16Bit(0x181B, "Body Composition"))
        list.add(Service16Bit(0x181E, "Bond Management Service"))
        list.add(Service16Bit(0x181F, "Continuous Glucose Monitoring"))
        list.add(Service16Bit(0x1805, "Current Time Service"))
        list.add(Service16Bit(0x1818, "Cycling Power"))
        list.add(Service16Bit(0x1816, "Cycling Speed and Cadence"))
        list.add(Service16Bit(0x180A, "Device Information"))
        list.add(Service16Bit(0x183C, "Emergency Configuration"))
        list.add(Service16Bit(0x181A, "Environmental Sensing"))
        list.add(Service16Bit(0x1826, "Fitness Machine"))
        list.add(Service16Bit(0x1801, "Generic Attribute"))
        list.add(Service16Bit(0x1808, "Glucose"))
        list.add(Service16Bit(0x1809, "Health Thermometer"))
        list.add(Service16Bit(0x180D, "Heart Rate"))
        list.add(Service16Bit(0x1823, "HTTP Proxy"))
        list.add(Service16Bit(0x1812, "Human Interface Device"))
        list.add(Service16Bit(0x1802, "Immediate Alert"))
        list.add(Service16Bit(0x1821, "Indoor Positioning"))
        list.add(Service16Bit(0x183A, "Insulin Delivery"))
        list.add(Service16Bit(0x1820, "Internet Protocol Support Service"))
        list.add(Service16Bit(0x1803, "Link Loss"))
        list.add(Service16Bit(0x1819, "Location and Navigation"))
        list.add(Service16Bit(0x1827, "Mesh Provisioning Service"))
        list.add(Service16Bit(0x1828, "Mesh Proxy Service"))
        list.add(Service16Bit(0x1807, "Next DST Change Service"))
        list.add(Service16Bit(0x1825, "Object Transfer Service"))
        list.add(Service16Bit(0x180E, "Phone Alert Status Service"))
        list.add(Service16Bit(0x1822, "Pulse Oximeter Service"))
        list.add(Service16Bit(0x1829, "Reconnection Configuration"))
        list.add(Service16Bit(0x1806, "Reference Time Update Service"))
        list.add(Service16Bit(0x1814, "Running Speed and Cadence"))
        list.add(Service16Bit(0x1813, "Scan Parameters"))
        list.add(Service16Bit(0x1824, "Transport Discovery"))
        list.add(Service16Bit(0x1804, "Tx Power"))
        list.add(Service16Bit(0x181C, "User Data"))
        list.add(Service16Bit(0x181D, "Weight Scale"))

        return list
    }

}
