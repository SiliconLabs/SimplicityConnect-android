package com.siliconlabs.bledemo.features.configure.gatt_configurator.models

import java.util.*

class Characteristic16Bit(val identifier: Int, val name: String) {
    fun getIdentifierAsString(): String {
        return String.format("%04X", identifier)
    }

    fun getFullName(): String {
        val hexString: String = "(0x".plus(String.format("%04X", identifier).plus(")").toUpperCase(Locale.getDefault()))
        return name.plus(" ").plus(hexString)
    }

    override fun toString(): String {
        return "0x".plus(String.format("%04X", identifier)).plus(" - ").plus(name)
    }
}