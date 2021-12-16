package com.siliconlabs.bledemo.environment.model

import java.util.*

class TemperatureScale(locale: Locale) {

    var temperatureType = CELSIUS

    override fun toString(): String {
        return String.format("temperatureType: %s",
                if (temperatureType == CELSIUS) "celsius" else "fahrenheit")
    }

    companion object {
        const val CELSIUS = 0
        const val FAHRENHEIT = 1
    }

    init {
        if (Locale.US == locale) {
            temperatureType = FAHRENHEIT
        }
    }
}