package com.siliconlabs.bledemo.Advertiser.Utils

import com.siliconlabs.bledemo.utils.Converters

class Validator {
    companion object {
        private const val UUID_PATTERN = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
        private const val MIN_ADV_INTERVAL = 100
        private const val MAX_ADV_INTERVAL = 10485759
        private const val MIN_TX_POWER = -127
        private const val MAX_TX_POWER = 1
        private const val MIN_TIME_LIMIT_LOW_API = 1
        private const val MAX_TIME_LIMIT_LOW_API = 180000
        private const val MIN_TIME_LIMIT = 10
        private const val MAX_TIME_LIMIT = 655350
        private const val MIN_EVENT_LIMIT = 1
        private const val MAX_EVENT_LIMIT = 255

        fun isCompanyIdentifierValid(text: String): Boolean {
            return text.length == 4 && Converters.isHexCorrect(text)
        }

        fun isCompanyDataValid(text: String): Boolean {
            return (text.length >= 2 && text.length % 2 == 0 && Converters.isHexCorrect(text))
        }

        fun isTxPowerValid(text: String): Boolean {
            return try {
                val txPower = text.toInt()
                txPower in MIN_TX_POWER..MAX_TX_POWER
            } catch (nfe: NumberFormatException) {
                false
            }
        }

        fun isAdvertisingIntervalValid(text: String): Boolean {
            return try {
                val interval = text.toInt()
                interval in MIN_ADV_INTERVAL..MAX_ADV_INTERVAL
            } catch (nfe: NumberFormatException) {
                false
            }
        }

        fun isAdvertisingTimeLimitValid(text: String, isExtendedRange: Boolean): Boolean {
            return try {
                val limit = text.toInt()
                if (isExtendedRange) limit in MIN_TIME_LIMIT..MAX_TIME_LIMIT
                else limit in MIN_TIME_LIMIT_LOW_API..MAX_TIME_LIMIT_LOW_API
            } catch (nfe: NumberFormatException) {
                false
            }
        }

        fun isAdvertisingEventLimitValid(text: String): Boolean {
            return try {
                val limit = text.toInt()
                limit in MIN_EVENT_LIMIT..MAX_EVENT_LIMIT
            } catch (nfe: java.lang.NumberFormatException) {
                false
            }
        }

        fun validateUUID(uuid: String): Boolean {
            val regex = UUID_PATTERN.toRegex()
            return uuid.matches(regex)
        }
    }
}
