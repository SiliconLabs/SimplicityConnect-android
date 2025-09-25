package com.siliconlabs.bledemo.features.iop_test.models

class Common {
    companion object {
        const val WRITE_LENGTH_1 = 1
        const val WRITE_LENGTH_4 = 4
        const val WRITE_LENGTH_55 = 55
        const val WRITE_LENGTH_255 = 255

        const val WRITE_VALUE_0 = "0"
        const val WRITE_VALUE_00 = "00"
        const val WRITE_VALUE_55 = "55"
        const val WRITE_VALUE_66 = "66"
        const val TIME_OUT_TEST = 1000
        const val TIME_OUT_DISCOVER = 5000
        const val TIME_OUT_NOTIFICATION_INDICATE_TEST = 300
        const val IOP3_TC_STATUS_FAILED = 0
        const val IOP3_TC_STATUS_PASS = 1
        const val IOP3_TC_STATUS_PROCESSING = 2
        const val IOP3_TC_STATUS_WAITING = 3
        const val IOP3_TC_STATUS_NOT_RUN = 4

        // Checks if given property is set
        fun isSetProperty(property: PropertyType, properties: Int): Boolean {
            return properties shr property.ordinal and 1 != 0
        }
    }

    enum class PropertyType(val value: Int) {
        BROADCAST(1),
        READ(2),
        WRITE_NO_RESPONSE(4),
        WRITE(8),
        NOTIFY(16),
        INDICATE(32),
        SIGNED_WRITE(64),
        EXTENDED_PROPS(128);
    }
}