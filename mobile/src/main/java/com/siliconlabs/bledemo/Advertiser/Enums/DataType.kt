package com.siliconlabs.bledemo.advertiser.enums

enum class DataType {
    FLAGS,
    COMPLETE_16_BIT,
    COMPLETE_128_BIT,
    COMPLETE_LOCAL_NAME,
    TX_POWER,
    MANUFACTURER_SPECIFIC_DATA;

    fun getIdentifier(): String {
        return when (this) {
            FLAGS -> "0x01"
            COMPLETE_16_BIT -> "0x03"
            COMPLETE_128_BIT -> "0x07"
            COMPLETE_LOCAL_NAME -> "0x09"
            TX_POWER -> "0x0A"
            MANUFACTURER_SPECIFIC_DATA -> "0xFF"
        }
    }
}