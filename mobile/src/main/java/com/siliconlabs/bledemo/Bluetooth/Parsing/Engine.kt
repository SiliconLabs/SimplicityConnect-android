/*
 * Bluegiga's Bluetooth Smart Android SW for Bluegiga BLE modules
 * Contact: support@bluegiga.com.
 *
 * This is free software distributed under the terms of the MIT license reproduced below.
 *
 * Copyright (c) 2013, Bluegiga Technologies
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files ("Software")
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF 
 * ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A  PARTICULAR PURPOSE.
 */
package com.siliconlabs.bledemo.Bluetooth.Parsing

import android.bluetooth.BluetoothGatt
import android.content.Context
import com.siliconlabs.bledemo.Bluetooth.DataTypes.Characteristic
import com.siliconlabs.bledemo.Bluetooth.DataTypes.Descriptor
import com.siliconlabs.bledemo.Bluetooth.DataTypes.Service
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

// Engine - contains data accessible by each part of application
// It links data models from xml resources with real Bluetooth classes
class Engine {
    private lateinit var units: HashMap<String, Unit>
    private lateinit var formats: HashMap<String, Int>
    private var services: HashMap<UUID, Service>? = null
    private var descriptors: HashMap<UUID?, Descriptor>? = null
    private var characteristics: ConcurrentHashMap<UUID, Characteristic>? = null

    var devices: Vector<Device>? = null
    var isCharacteristicsLoaded = false

    // Initializes class members, it must be first called
    fun init(context: Context?) {
        devices = Vector()
        BluetoothXmlParser.instance?.init(context)
        loadUnits()
        loadFormats()
        loadServices()
        loadDescriptors()
        loadCharacteristics()
    }

    // Loads descriptors from xml resource
    private fun loadDescriptors() {
        try {
            descriptors = BluetoothXmlParser.instance?.parseDescriptors()
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // Loads services from xml resource
    private fun loadServices() {
        try {
            services = BluetoothXmlParser.instance?.parseServices()!!
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // Loads characteristics from xml resource
    private fun loadCharacteristics() {
        Thread(Runnable {
            try {
                characteristics = BluetoothXmlParser.instance?.parseCharacteristics()!!
                isCharacteristicsLoaded = true
            } catch (e: XmlPullParserException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }).start()
    }

    // List of variables formats from Bluetooth.org
    // https://developer.bluetooth.org/gatt/Pages/FormatTypes.aspx
    private fun loadFormats() {
        formats = HashMap()
        formats["boolean"] = 1
        formats["2bit"] = 1
        formats["nibble"] = 2
        formats["4bit"] = 1
        formats["8bit"] = 1
        formats["16bit"] = 2
        formats["24bit"] = 3
        formats["32bit"] = 4
        formats["uint8"] = 1
        formats["uint12"] = 2
        formats["uint16"] = 2
        formats["uint24"] = 3
        formats["uint32"] = 4
        formats["uint40"] = 5
        formats["uint48"] = 6
        formats["uint64"] = 8
        formats["uint128"] = 16
        formats["sint8"] = 1
        formats["sint12"] = 2
        formats["sint16"] = 2
        formats["sint24"] = 3
        formats["sint32"] = 4
        formats["sint48"] = 6
        formats["sint64"] = 8
        formats["sint128"] = 16
        formats["float32"] = 4
        formats["float64"] = 8
        formats["SFLOAT"] = 2
        formats["FLOAT"] = 4
        formats["duint16"] = 2
        formats["utf8s"] = 0
        formats["utf16s"] = 0
        formats["reg-cert-data-list"] = 0
        formats["variable"] = 0
    }

    // List of units from Bluetooth.org
    // https://developer.bluetooth.org/gatt/units/Pages/default.aspx
    private fun loadUnits() {
        units = HashMap()
        units["org.bluetooth.unit.unitless"] = Unit("", "")
        units["org.bluetooth.unit.length.metre"] = Unit("m", "metre")
        units["org.bluetooth.unit.mass.kilogram"] = Unit("kg", "kilogram")
        units["org.bluetooth.unit.time.second"] = Unit("s", "second")
        units["org.bluetooth.unit.electric_current.ampere"] = Unit("A", "ampere")
        units["org.bluetooth.unit.thermodynamic_temperature.kelvin"] = Unit("K", "kelvin")
        units["org.bluetooth.unit.amount_of_substance.mole"] = Unit("mol", "mole")
        units["org.bluetooth.unit.luminous_intensity.candela"] = Unit("cd", "cendela")
        units["org.bluetooth.unit.area.square_metres"] = Unit("", "square metres")
        units["org.bluetooth.unit.volume.cubic_metres"] = Unit("", "cubic metres")
        units["org.bluetooth.unit.velocity.metres_per_second"] = Unit("", "metres per second")
        units["org.bluetooth.unit.acceleration.metres_per_second_squared"] = Unit("", "metres per second squared")
        units["org.bluetooth.unit.wavenumber.reciprocal_metre"] = Unit("", "reciprocal metre")
        units["org.bluetooth.unit.density.kilogram_per_cubic_metre"] = Unit("", "kilogram per cubic metre")
        units["org.bluetooth.unit.surface_density.kilogram_per_square_metre"] = Unit("",
                "kilogram per square metre")
        units["org.bluetooth.unit.specific_volume.cubic_metre_per_kilogram"] = Unit("",
                "cubic metre per kilogram")
        units["org.bluetooth.unit.current_density.ampere_per_square_metre"] = Unit("", "ampere per square metre")
        units["org.bluetooth.unit.magnetic_field_strength.ampere_per_metre"] = Unit("", "ampere per metre")
        units["org.bluetooth.unit.amount_concentration.mole_per_cubic_metre"] = Unit("", "mole per cubic metre")
        units["org.bluetooth.unit.mass_concentration.kilogram_per_cubic_metre"] = Unit("",
                "kilogram per cubic metre")
        units["org.bluetooth.unit.luminance.candela_per_square_metre"] = Unit("", "candela per square metre")
        units["org.bluetooth.unit.refractive_index"] = Unit("", "refractive index")
        units["org.bluetooth.unit.relative_permeability"] = Unit("", "relative permeability")
        units["org.bluetooth.unit.plane_angle.radian"] = Unit("rad", "radian")
        units["org.bluetooth.unit.solid_angle.steradian"] = Unit("sr", "steradian")
        units["org.bluetooth.unit.frequency.hertz"] = Unit("Hz", "hertz")
        units["org.bluetooth.unit.force.newton"] = Unit("N", "newton")
        units["org.bluetooth.unit.pressure.pascal"] = Unit("Pa", "pascal")
        units["org.bluetooth.unit.energy.joule"] = Unit("J", "joule")
        units["org.bluetooth.unit.power.watt"] = Unit("W", "watt")
        units["org.bluetooth.unit.electric_charge.coulomb"] = Unit("C", "coulomb")
        units["org.bluetooth.unit.electric_potential_difference.volt"] = Unit("V", "volt")
        units["org.bluetooth.unit.capacitance.farad"] = Unit("F", "farad")
        units["org.bluetooth.unit.electric_resistance.ohm"] = Unit("", "ohm")
        units["org.bluetooth.unit.electric_conductance.siemens"] = Unit("S", "siemens")
        units["org.bluetooth.unit.magnetic_flex.weber"] = Unit("Wb", "weber")
        units["org.bluetooth.unit.magnetic_flex_density.tesla"] = Unit("T", "tesla")
        units["org.bluetooth.unit.inductance.henry"] = Unit("H", "henry")
        units["org.bluetooth.unit.thermodynamic_temperature.degree_celsius"] = Unit(0x00B0.toChar().toString() + "C", "Celsius")
        units["org.bluetooth.unit.luminous_flux.lumen"] = Unit("lm", "lumen")
        units["org.bluetooth.unit.illuminance.lux"] = Unit("lx", "lux")
        units["org.bluetooth.unit.activity_referred_to_a_radionuclide.becquerel"] = Unit("Bq", "becquerel")
        units["org.bluetooth.unit.absorbed_dose.gray"] = Unit("Gy", "gray")
        units["org.bluetooth.unit.dose_equivalent.sievert"] = Unit("Sv", "sievert")
        units["org.bluetooth.unit.catalytic_activity.katal"] = Unit("kat", "katal")
        units["org.bluetooth.unit.dynamic_viscosity.pascal_second"] = Unit("", "pascal second")
        units["org.bluetooth.unit.moment_of_force.newton_metre"] = Unit("", "newton metre")
        units["org.bluetooth.unit.surface_tension.newton_per_metre"] = Unit("", "newton per metre")
        units["org.bluetooth.unit.angular_velocity.radian_per_second"] = Unit("", "radian per second")
        units["org.bluetooth.unit.angular_acceleration.radian_per_second_squared"] = Unit("",
                "radian per second squared")
        units["org.bluetooth.unit.heat_flux_density.watt_per_square_metre"] = Unit("", "watt per square metre")
        units["org.bluetooth.unit.heat_capacity.joule_per_kelvin"] = Unit("", "joule per kelvin")
        units["org.bluetooth.unit.specific_heat_capacity.joule_per_kilogram_kelvin"] = Unit("",
                "joule per kilogram kelvin")
        units["org.bluetooth.unit.specific_energy.joule_per_kilogram"] = Unit("", "joule per kilogram")
        units["org.bluetooth.unit.thermal_conductivity.watt_per_metre_kelvin"] = Unit("", "watt per metre kelvin")
        units["org.bluetooth.unit.energy_density.joule_per_cubic_metre"] = Unit("", "joule per cubic metre")
        units["org.bluetooth.unit.electric_field_strength.volt_per_metre"] = Unit("", "volt per metre")
        units["org.bluetooth.unit.electric_charge_density.coulomb_per_cubic_metre"] = Unit("",
                "coulomb per cubic metre")
        units["org.bluetooth.unit.surface_charge_density.coulomb_per_square_metre"] = Unit("",
                "coulomb per square metre")
        units["org.bluetooth.unit.electric_flux_density.coulomb_per_square_metre"] = Unit("",
                "coulomb per square metre")
        units["org.bluetooth.unit.permittivity.farad_per_metre"] = Unit("", "farad per metre")
        units["org.bluetooth.unit.permeability.henry_per_metre"] = Unit("", "henry per metre")
        units["org.bluetooth.unit.molar_energy.joule_per_mole"] = Unit("", "joule per mole")
        units["org.bluetooth.unit.molar_entropy.joule_per_mole_kelvin"] = Unit("", "joule per mole kelvin")
        units["org.bluetooth.unit.exposure.coulomb_per_kilogram"] = Unit("", "coulomb per kilogram")
        units["org.bluetooth.unit.absorbed_dose_rate.gray_per_second"] = Unit("", "gray per second")
        units["org.bluetooth.unit.radiant_intensity.watt_per_steradian"] = Unit("", "watt per steradian")
        units["org.bluetooth.unit.radiance.watt_per_square_metre_steradian"] = Unit("",
                "watt per square metre steradian")
        units["org.bluetooth.unit.catalytic_activity_concentration.katal_per_cubic_metre"] = Unit("",
                "katal per cubic metre")
        units["org.bluetooth.unit.time.minute"] = Unit("", "minute")
        units["org.bluetooth.unit.time.hour"] = Unit("", "hour")
        units["org.bluetooth.unit.time.day"] = Unit("", "day")
        units["org.bluetooth.unit.plane_angle.degree"] = Unit("", "degree")
        units["org.bluetooth.unit.plane_angle.minute"] = Unit("", "minute")
        units["org.bluetooth.unit.plane_angle.second"] = Unit("", "second")
        units["org.bluetooth.unit.area.hectare"] = Unit("Ha", "hectare")
        units["org.bluetooth.unit.volume.litre"] = Unit("L", "litre")
        units["org.bluetooth.unit.mass.tonne"] = Unit("ton", "tonne")
        units["org.bluetooth.unit.pressure.bar"] = Unit("", "bar")
        units["org.bluetooth.unit.pressure.millimetre_of_mercury"] = Unit("", "millimetre of mercury")
        units["org.bluetooth.unit.length.ångström"] = Unit("", "ångström")
        units["org.bluetooth.unit.length.nautical_mile"] = Unit("", "nautical mile")
        units["org.bluetooth.unit.area.barn"] = Unit("", "barn")
        units["org.bluetooth.unit.velocity.knot"] = Unit("", "knot")
        units["org.bluetooth.unit.logarithmic_radio_quantity.neper"] = Unit("", "neper")
        units["org.bluetooth.unit.logarithmic_radio_quantity.bel"] = Unit("", "bel")
        units["org.bluetooth.unit.length.yard"] = Unit("", "yard")
        units["org.bluetooth.unit.length.parsec"] = Unit("", "parsec")
        units["org.bluetooth.unit.length.inch"] = Unit("", "inch")
        units["org.bluetooth.unit.length.foot"] = Unit("", "foot")
        units["org.bluetooth.unit.length.mile"] = Unit("", "mile")
        units["org.bluetooth.unit.pressure.pound_force_per_square_inch"] = Unit("", "pound-force per square inch")
        units["org.bluetooth.unit.velocity.kilometre_per_hour"] = Unit("", "kilometre per hour")
        units["org.bluetooth.unit.velocity.mile_per_hour"] = Unit("", "mile per hour")
        units["org.bluetooth.unit.angular_velocity.revolution_per_minute"] = Unit("", "revolution per minute")
        units["org.bluetooth.unit.energy.gram_calorie"] = Unit("", "gram calorie")
        units["org.bluetooth.unit.energy.kilogram_calorie"] = Unit("", "kilogram calorie")
        units["org.bluetooth.unit.energy.kilowatt_hour"] = Unit("", "kilowatt hour")
        units["org.bluetooth.unit.thermodynamic_temperature.degree_fahrenheit"] = Unit(0x00B0.toChar().toString() + "F", "Fahrenheit")
        units["org.bluetooth.unit.percentage"] = Unit("%", "percentage")
        units["org.bluetooth.unit.per_mille"] = Unit("", "per mille")
        units["org.bluetooth.unit.period.beats_per_minute"] = Unit("", "beats per minute")
        units["org.bluetooth.unit.electric_charge.ampere_hours"] = Unit("", "ampere hours")
        units["org.bluetooth.unit.mass_density.milligram_per_decilitre"] = Unit("", "milligram per decilitre")
        units["org.bluetooth.unit.mass_density.millimole_per_litre"] = Unit("", "millimole per litre")
        units["org.bluetooth.unit.time.year"] = Unit("", "year")
        units["org.bluetooth.unit.time.month"] = Unit("", "month")
        units["org.bluetooth.unit.concentration.count_per_cubic_metre"] = Unit("", "count per cubic metre")
        units["org.bluetooth.unit.irradiance.watt_per_square_metre"] = Unit("", "watt per square metre")
    }


    // Gets device for given device address
    fun getDevice(address: String): Device? {
        val e: Iterator<Device> = devices?.iterator()!!
        var elem: Device
        while (e.hasNext()) {
            elem = e.next()
            if (elem.address == address) {
                return elem
            }
        }
        return null
    }

    // Gets device for given device gatt object
    fun getDevice(gatt: BluetoothGatt): Device? {
        val e: Iterator<Device> = devices?.iterator()!!
        var elem: Device
        while (e.hasNext()) {
            elem = e.next()
            if (elem.bluetoothGatt == gatt) {
                return elem
            }
        }
        return null
    }

    // Gets service for given UUID
    fun getService(uuid: UUID?): Service? {
        return services!![uuid]
    }

    // Gets characteristic for given UUID
    fun getCharacteristic(uuid: UUID?): Characteristic? {
        return characteristics!![uuid]
    }

    fun getDescriptorByUUID(uuid: UUID?): Descriptor? {
        return descriptors!![uuid]
    }

    // Gets unit for given UUID
    fun getUnit(uuid: String?): Unit? {
        return units[uuid]
    }

    // Gets format length for given format text
    // Return length in bytes
    fun getFormat(format: String?): Int? {
        return formats[format]!!
    }

    // Clears all data lists
    fun close() {
        devices?.clear()
        characteristics?.clear()
        services?.clear()
        units.clear()
        formats.clear()
    }

    companion object {
        var instance: Engine? = null
            get() {
                if (field == null) {
                    synchronized(locker) {
                        if (field == null) {
                            field = Engine()
                        }
                    }
                }
                return field
            }
            private set
        private val locker = Any()
    }
}