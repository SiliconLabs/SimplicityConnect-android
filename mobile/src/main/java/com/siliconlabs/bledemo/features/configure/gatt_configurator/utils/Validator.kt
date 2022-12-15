package com.siliconlabs.bledemo.features.configure.gatt_configurator.utils

import java.util.*

class Validator {
    companion object {
        private const val UUID_128BIT_PATTERN = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
        private const val UUID_16BIT_PATTERN = "[0-9a-fA-F]{4}"

        fun is128BitUuidValid(uuid: String): Boolean {
            val regex = UUID_128BIT_PATTERN.toRegex()
            return uuid.matches(regex)
        }

        fun is16BitUuidValid(uuid: String): Boolean {
            val regex = UUID_16BIT_PATTERN.toRegex()
            return uuid.matches(regex)
        }

        fun isHexValid(text: String): Boolean {
            val result = text.toUpperCase(Locale.ROOT).filter { it in 'A'..'F' || it in '0'..'9' }
            return result.isNotEmpty() && result.length % 2 == 0
        }
    }
}