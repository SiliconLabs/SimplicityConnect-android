package com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.model

import java.util.*

class TemperatureScale(locale: Locale) {

    var scale = CELSIUS

    override fun toString(): String {
        return String.format("temperatureType: %s",
                if (scale == CELSIUS) "celsius" else "fahrenheit")
    }

    companion object {
        const val CELSIUS = 0
        const val FAHRENHEIT = 1
    }

    init {
        if (Locale.US == locale) {
            scale = FAHRENHEIT
        }
    }
}