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
package com.siliconlabs.bledemo.bluetoothdatamodel.parsing;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import com.siliconlabs.bledemo.bluetoothdatamodel.datatypes.Descriptor;
import com.siliconlabs.bledemo.bluetoothdatamodel.datatypes.Service;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.siliconlabs.bledemo.bluetoothdatamodel.datatypes.Characteristic;

// Engine - contains data accessible by each part of application
// It links data models from xml resources with real Bluetooth classes  
public class Engine {
    private static Engine instance = null;

    private HashMap<UUID, Service> services;
    private HashMap<UUID, Descriptor> descriptors;
    private ConcurrentHashMap<UUID, Characteristic> characteristics;
    private HashMap<String, Unit> units;
    private HashMap<String, Integer> formats;
    private ArrayList<EngineObserver> observers;
    private boolean characteristicsLoaded = false;
    private BluetoothGattCharacteristic lastCharacteristic;
    private Vector<Device> devices;
    private static Object locker = new Object();

    final Comparator<Device> itemsComparator = new Comparator<Device>() {
        @Override
        public int compare(Device lhs, Device rhs) {
            // sort based on if the device is connected
            if (lhs.isConnected() && !rhs.isConnected()) {
                return -1;
            } else if (!lhs.isConnected() && rhs.isConnected()) {
                return 1;
            }

            // sort based on if the device has a name
            if (lhs.getName() != null && rhs.getName() == null) {
                return -1;
            } else if (lhs.getName() == null && rhs.getName() != null) {
                return 1;
            }

            // sort based on name
            if(lhs.getName() != null && rhs.getName() != null) {
                String lName = lhs.getName();
                String rName = rhs.getName();

                int nameComparison = lName.compareTo(rName);
                if(nameComparison != 0){
                    return nameComparison;
                }
            }

            // sort based on mac address (includes case for duplicate names, and empty names)
            String lhsAddress = lhs.getAddress();
            String rhsAddress = rhs.getAddress();
            return lhsAddress.compareTo(rhsAddress);
        }
    };

    public void sortDevices(){
        if (this.devices.size() > 1) {
            Collections.sort(this.devices, itemsComparator);
        }
    }

    public static Engine getInstance() {
        if (instance == null) {
            synchronized (locker) {
                if (instance == null) {
                    instance = new Engine();
                }
            }
        }
        return instance;
    }

    // Initializes class members, it must be first called
    public void init(Context context) {
        observers = new ArrayList<EngineObserver>();
        devices = new Vector<Device>();
        BluetoothXmlParser.getInstance().init(context);
        loadUnits();
        loadFormats();
        loadServices();
        loadDescriptors();
        loadCharacteristics();
    }

    // Loads descriptors from xml resource
    private void loadDescriptors() {
        try {
            descriptors = BluetoothXmlParser.getInstance().parseDescriptors();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Loads services from xml resource
    private void loadServices() {
        try {
            services = BluetoothXmlParser.getInstance().parseServices();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Loads characteristics from xml resource
    private void loadCharacteristics() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    characteristics = BluetoothXmlParser.getInstance().parseCharacteristics();
                    characteristicsLoaded = true;
                    for (EngineObserver observer : observers) {
                        observer.onCharacteristicsLoaded();
                    }
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    // List of variables formats from Bluetooth.org
    // https://developer.bluetooth.org/gatt/Pages/FormatTypes.aspx
    private void loadFormats() {
        formats = new HashMap<String, Integer>();
        formats.put("boolean", 1);
        formats.put("2bit", 1);
        formats.put("nibble", 2);
        formats.put("8bit", 1);
        formats.put("16bit", 2);
        formats.put("24bit", 3);
        formats.put("32bit", 4);
        formats.put("uint8", 1);
        formats.put("uint16", 2);
        formats.put("uint24", 3);
        formats.put("uint32", 4);
        formats.put("uint40", 5);
        formats.put("uint48", 6);
        formats.put("uint64", 8);
        formats.put("uint128", 16);
        formats.put("sint8", 1);
        formats.put("sint12", 2);
        formats.put("sint16", 2);
        formats.put("sint24", 3);
        formats.put("sint32", 4);
        formats.put("sint48", 6);
        formats.put("sint64", 8);
        formats.put("sint128", 16);
        formats.put("float32", 4);
        formats.put("float64", 8);
        formats.put("SFLOAT", 2);
        formats.put("FLOAT", 4);
        formats.put("dunit16", 2);
        formats.put("utf8s", 0);
        formats.put("utf16s", 0);
        formats.put("dunit16", 2);
        formats.put("reg-cert-data-list", 0);
        formats.put("variable", 0);
    }

    // List of units from Bluetooth.org
    // https://developer.bluetooth.org/gatt/units/Pages/default.aspx
    private void loadUnits() {
        units = new HashMap<String, Unit>();
        units.put("org.bluetooth.unit.unitless", new Unit("", ""));
        units.put("org.bluetooth.unit.length.metre", new Unit("m", "metre"));
        units.put("org.bluetooth.unit.mass.kilogram", new Unit("kg", "kilogram"));
        units.put("org.bluetooth.unit.time.second", new Unit("s", "second"));
        units.put("org.bluetooth.unit.electric_current.ampere", new Unit("A", "ampere"));
        units.put("org.bluetooth.unit.thermodynamic_temperature.kelvin", new Unit("K", "kelvin"));
        units.put("org.bluetooth.unit.amount_of_substance.mole", new Unit("mol", "mole"));
        units.put("org.bluetooth.unit.luminous_intensity.candela", new Unit("cd", "cendela"));
        units.put("org.bluetooth.unit.area.square_metres", new Unit("", "square metres"));
        units.put("org.bluetooth.unit.volume.cubic_metres", new Unit("", "cubic metres"));
        units.put("org.bluetooth.unit.velocity.metres_per_second", new Unit("", "metres per second"));
        units.put("org.bluetooth.unit.acceleration.metres_per_second_squared",
                new Unit("", "metres per second squared"));
        units.put("org.bluetooth.unit.wavenumber.reciprocal_metre", new Unit("", "reciprocal metre"));
        units.put("org.bluetooth.unit.density.kilogram_per_cubic_metre", new Unit("", "kilogram per cubic metre"));
        units.put("org.bluetooth.unit.surface_density.kilogram_per_square_metre", new Unit("",
                "kilogram per square metre"));
        units.put("org.bluetooth.unit.specific_volume.cubic_metre_per_kilogram", new Unit("",
                "cubic metre per kilogram"));
        units.put("org.bluetooth.unit.current_density.ampere_per_square_metre", new Unit("", "ampere per square metre"));
        units.put("org.bluetooth.unit.magnetic_field_strength.ampere_per_metre", new Unit("", "ampere per metre"));
        units.put("org.bluetooth.unit.amount_concentration.mole_per_cubic_metre", new Unit("", "mole per cubic metre"));
        units.put("org.bluetooth.unit.mass_concentration.kilogram_per_cubic_metre", new Unit("",
                "kilogram per cubic metre"));
        units.put("org.bluetooth.unit.luminance.candela_per_square_metre", new Unit("", "candela per square metre"));
        units.put("org.bluetooth.unit.refractive_index", new Unit("", "refractive index"));
        units.put("org.bluetooth.unit.relative_permeability", new Unit("", "relative permeability"));
        units.put("org.bluetooth.unit.plane_angle.radian", new Unit("rad", "radian"));
        units.put("org.bluetooth.unit.solid_angle.steradian", new Unit("sr", "steradian"));
        units.put("org.bluetooth.unit.frequency.hertz", new Unit("Hz", "hertz"));
        units.put("org.bluetooth.unit.force.newton", new Unit("N", "newton"));
        units.put("org.bluetooth.unit.pressure.pascal", new Unit("Pa", "pascal"));
        units.put("org.bluetooth.unit.energy.joule", new Unit("J", "joule"));
        units.put("org.bluetooth.unit.power.watt", new Unit("W", "watt"));
        units.put("org.bluetooth.unit.electric_charge.coulomb", new Unit("C", "coulomb"));
        units.put("org.bluetooth.unit.electric_potential_difference.volt", new Unit("V", "volt"));
        units.put("org.bluetooth.unit.capacitance.farad", new Unit("F", "farad"));
        units.put("org.bluetooth.unit.electric_resistance.ohm", new Unit("", "ohm"));
        units.put("org.bluetooth.unit.electric_conductance.siemens", new Unit("S", "siemens"));
        units.put("org.bluetooth.unit.magnetic_flex.weber", new Unit("Wb", "weber"));
        units.put("org.bluetooth.unit.magnetic_flex_density.tesla", new Unit("T", "tesla"));
        units.put("org.bluetooth.unit.inductance.henry", new Unit("H", "henry"));
        units.put("org.bluetooth.unit.thermodynamic_temperature.degree_celsius", new Unit( ((char) 0x00B0) + "C", "Celsius"));
        units.put("org.bluetooth.unit.luminous_flux.lumen", new Unit("lm", "lumen"));
        units.put("org.bluetooth.unit.illuminance.lux", new Unit("lx", "lux"));
        units.put("org.bluetooth.unit.activity_referred_to_a_radionuclide.becquerel", new Unit("Bq", "becquerel"));
        units.put("org.bluetooth.unit.absorbed_dose.gray", new Unit("Gy", "gray"));
        units.put("org.bluetooth.unit.dose_equivalent.sievert", new Unit("Sv", "sievert"));
        units.put("org.bluetooth.unit.catalytic_activity.katal", new Unit("kat", "katal"));
        units.put("org.bluetooth.unit.dynamic_viscosity.pascal_second", new Unit("", "pascal second"));
        units.put("org.bluetooth.unit.moment_of_force.newton_metre", new Unit("", "newton metre"));
        units.put("org.bluetooth.unit.surface_tension.newton_per_metre", new Unit("", "newton per metre"));
        units.put("org.bluetooth.unit.angular_velocity.radian_per_second", new Unit("", "radian per second"));
        units.put("org.bluetooth.unit.angular_acceleration.radian_per_second_squared", new Unit("",
                "radian per second squared"));
        units.put("org.bluetooth.unit.heat_flux_density.watt_per_square_metre", new Unit("", "watt per square metre"));
        units.put("org.bluetooth.unit.heat_capacity.joule_per_kelvin", new Unit("", "joule per kelvin"));
        units.put("org.bluetooth.unit.specific_heat_capacity.joule_per_kilogram_kelvin", new Unit("",
                "joule per kilogram kelvin"));
        units.put("org.bluetooth.unit.specific_energy.joule_per_kilogram", new Unit("", "joule per kilogram"));
        units.put("org.bluetooth.unit.thermal_conductivity.watt_per_metre_kelvin",
                new Unit("", "watt per metre kelvin"));
        units.put("org.bluetooth.unit.energy_density.joule_per_cubic_metre", new Unit("", "joule per cubic metre"));
        units.put("org.bluetooth.unit.electric_field_strength.volt_per_metre", new Unit("", "volt per metre"));
        units.put("org.bluetooth.unit.electric_charge_density.coulomb_per_cubic_metre", new Unit("",
                "coulomb per cubic metre"));
        units.put("org.bluetooth.unit.surface_charge_density.coulomb_per_square_metre", new Unit("",
                "coulomb per square metre"));
        units.put("org.bluetooth.unit.electric_flux_density.coulomb_per_square_metre", new Unit("",
                "coulomb per square metre"));
        units.put("org.bluetooth.unit.permittivity.farad_per_metre", new Unit("", "farad per metre"));
        units.put("org.bluetooth.unit.permeability.henry_per_metre", new Unit("", "henry per metre"));
        units.put("org.bluetooth.unit.molar_energy.joule_per_mole", new Unit("", "joule per mole"));
        units.put("org.bluetooth.unit.molar_entropy.joule_per_mole_kelvin", new Unit("", "joule per mole kelvin"));
        units.put("org.bluetooth.unit.exposure.coulomb_per_kilogram", new Unit("", "coulomb per kilogram"));
        units.put("org.bluetooth.unit.absorbed_dose_rate.gray_per_second", new Unit("", "gray per second"));
        units.put("org.bluetooth.unit.radiant_intensity.watt_per_steradian", new Unit("", "watt per steradian"));
        units.put("org.bluetooth.unit.radiance.watt_per_square_metre_steradian", new Unit("",
                "watt per square metre steradian"));
        units.put("org.bluetooth.unit.catalytic_activity_concentration.katal_per_cubic_metre", new Unit("",
                "katal per cubic metre"));
        units.put("org.bluetooth.unit.time.minute", new Unit("", "minute"));
        units.put("org.bluetooth.unit.time.hour", new Unit("", "hour"));
        units.put("org.bluetooth.unit.time.day", new Unit("", "day"));
        units.put("org.bluetooth.unit.plane_angle.degree", new Unit("", "degree"));
        units.put("org.bluetooth.unit.plane_angle.minute", new Unit("", "minute"));
        units.put("org.bluetooth.unit.plane_angle.second", new Unit("", "second"));
        units.put("org.bluetooth.unit.area.hectare", new Unit("Ha", "hectare"));
        units.put("org.bluetooth.unit.volume.litre", new Unit("L", "litre"));
        units.put("org.bluetooth.unit.mass.tonne", new Unit("ton", "tonne"));
        units.put("org.bluetooth.unit.pressure.bar", new Unit("", "bar"));
        units.put("org.bluetooth.unit.pressure.millimetre_of_mercury", new Unit("", "millimetre of mercury"));
        units.put("org.bluetooth.unit.length.ångström", new Unit("", "ångström"));
        units.put("org.bluetooth.unit.length.nautical_mile", new Unit("", "nautical mile"));
        units.put("org.bluetooth.unit.area.barn", new Unit("", "barn"));
        units.put("org.bluetooth.unit.velocity.knot", new Unit("", "knot"));
        units.put("org.bluetooth.unit.logarithmic_radio_quantity.neper", new Unit("", "neper"));
        units.put("org.bluetooth.unit.logarithmic_radio_quantity.bel", new Unit("", "bel"));
        units.put("org.bluetooth.unit.length.yard", new Unit("", "yard"));
        units.put("org.bluetooth.unit.length.parsec", new Unit("", "parsec"));
        units.put("org.bluetooth.unit.length.inch", new Unit("", "inch"));
        units.put("org.bluetooth.unit.length.foot", new Unit("", "foot"));
        units.put("org.bluetooth.unit.length.mile", new Unit("", "mile"));
        units.put("org.bluetooth.unit.pressure.pound_force_per_square_inch",
                new Unit("", "pound-force per square inch"));
        units.put("org.bluetooth.unit.velocity.kilometre_per_hour", new Unit("", "kilometre per hour"));
        units.put("org.bluetooth.unit.velocity.mile_per_hour", new Unit("", "mile per hour"));
        units.put("org.bluetooth.unit.angular_velocity.revolution_per_minute", new Unit("", "revolution per minute"));
        units.put("org.bluetooth.unit.energy.gram_calorie", new Unit("", "gram calorie"));
        units.put("org.bluetooth.unit.energy.kilogram_calorie", new Unit("", "kilogram calorie"));
        units.put("org.bluetooth.unit.energy.kilowatt_hour", new Unit("", "kilowatt hour"));
        units.put("org.bluetooth.unit.thermodynamic_temperature.degree_fahrenheit", new Unit(((char) 0x00B0) + "F", "Fahrenheit"));
        units.put("org.bluetooth.unit.percentage", new Unit("%", "percentage"));
        units.put("org.bluetooth.unit.per_mille", new Unit("", "per mille"));
        units.put("org.bluetooth.unit.period.beats_per_minute", new Unit("", "beats per minut"));
        units.put("org.bluetooth.unit.electric_charge.ampere_hours", new Unit("", "ampere hours"));
        units.put("org.bluetooth.unit.mass_density.milligram_per_decilitre", new Unit("", "milligram per decilitre"));
        units.put("org.bluetooth.unit.mass_density.millimole_per_litre", new Unit("", "millimole per litre"));
        units.put("org.bluetooth.unit.time.year", new Unit("", "year"));
        units.put("org.bluetooth.unit.time.month", new Unit("", "month"));
        units.put("org.bluetooth.unit.concentration.count_per_cubic_metre", new Unit("", "count per cubic metre"));
        units.put("org.bluetooth.unit.irradiance.watt_per_square_metre", new Unit("", "watt per square metre"));
    }

    // Gets all services
    public HashMap<UUID, Service> getServices() {
        return services;
    }

    // Gets all characteristics
    public ConcurrentHashMap<UUID, Characteristic> getCharacteristics() {
        return characteristics;
    }

    // Gets all devices
    public Vector<Device> getDevices() {
        return devices;
    }

    // Sets last selected characteristic
    // It is called after user click on characteristic in
    // DeviceServicesActivity
    public void setLastCharacteristic(BluetoothGattCharacteristic lastCharacteristic) {
        this.lastCharacteristic = lastCharacteristic;
    }

    // Gets last selected characteristic
    // It is called when CharacteristicFragment is starting
    public BluetoothGattCharacteristic getLastCharacteristic() {
        return lastCharacteristic;
    }

    // Adds new discovered BLE device
    public Device addBluetoothDevice(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
        Device device = getDevice(bluetoothDevice.getAddress());
        // if device isn't added in list then creates new object and adds to
        // list
        if (device == null) {
            device = new Device(bluetoothDevice, rssi, scanRecord);
            devices.add(device);
        }
        return device;
    }

    // Gets device for given device address
    public Device getDevice(String address) {
        Iterator<Device> e = devices.iterator();

        Device elem;
        while (e.hasNext()) {
            elem = e.next();
            if (elem.getAddress().equals(address)) {
                return elem;
            }
        }
        return null;
    }

    // Gets device for given device gatt object
    public Device getDevice(BluetoothGatt gatt) {
        Iterator<Device> e = devices.iterator();

        Device elem;
        while (e.hasNext()) {
            elem = e.next();
            if (elem.getBluetoothGatt() == gatt) {
                return elem;
            }
        }
        return null;
    }

    // Gets service for given UUID
    public Service getService(UUID uuid) {
        return services.get(uuid);
    }

    // Gets characteristic for given UUID
    public Characteristic getCharacteristic(UUID uuid) {
        return characteristics.get(uuid);
    }

    // Gets characteristic for given type
    public Characteristic getCharacteristicByType(String type) {
        for (Entry<UUID, Characteristic> entry : characteristics.entrySet()) {
            if (entry.getValue().getType().equals(type)) {
                return entry.getValue();
            }
        }
        return null;
    }

    // Gets descriptor for given type
    public Descriptor getDescriptorByType(String type) {
        for (Entry<UUID, Descriptor> entry : descriptors.entrySet()) {
            if (entry.getValue().getType().equals(type)) {
                return entry.getValue();
            }
        }
        return null;
    }

    // Clears device list
    // It can clear whole list or only disconnected devices
    public void clearDeviceList(boolean clearOnlyDisconnected) {
        if (!clearOnlyDisconnected) {
            devices.clear();
        } else {
            Iterator<Device> device = devices.iterator();
            while (device.hasNext()) {
                if (!device.next().isConnected()) {
                    device.remove();
                }
            }
        }
    }

    // Gets unit for given UUID
    public Unit getUnit(String uuid) {
        return units.get(uuid);
    }

    // Gets format length for given format text
    // Return length in bytes
    public int getFormat(String format) {
        return formats.get(format);
    }

    // Check if all characteristics have been already loaded
    public boolean isCharacteristicsLoaded() {
        return characteristicsLoaded;
    }

    // Adds observer
    public void addObserver(EngineObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    // Removes observer
    public void removeObserver(EngineObserver observer) {
        if (observers.contains(observer)) {
            observers.remove(observer);
        }
    }

    // Clears all data lists
    public void close() {
        if (devices != null) {
            devices.clear();
        }
        if (characteristics != null) {
            characteristics.clear();
        }
        if (services != null) {
            services.clear();
        }
        if (units != null) {
            units.clear();
        }
        if (formats != null) {
            formats.clear();
        }
    }
}
