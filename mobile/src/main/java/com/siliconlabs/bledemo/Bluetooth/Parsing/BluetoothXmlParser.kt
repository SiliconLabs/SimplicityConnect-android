/*
 * Bluegiga’s Bluetooth Smart Android SW for Bluegiga BLE modules
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

import android.content.Context
import android.util.Xml
import com.siliconlabs.bledemo.Bluetooth.DataTypes.*
import com.siliconlabs.bledemo.Bluetooth.DataTypes.Enumeration
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList

// BluetoothXmlParser - parses Bluetooth xml resources from /assets/xml/ directory
// It is used only once when application is starting
class BluetoothXmlParser {
    private lateinit var characteristics: ConcurrentHashMap<UUID, Characteristic>

    private var appContext: Context? = null
    fun init(context: Context?) {
        appContext = context
    }

    // Parses service files
    @Throws(XmlPullParserException::class, IOException::class)
    fun parseServices(): HashMap<UUID, Service> {
        val serviceFiles = appContext?.assets?.list(Consts.DIR_SERVICE)
        var inStream: InputStream? = null
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        val services = HashMap<UUID, Service>()
        for (fileName in serviceFiles!!) {
            try {
                inStream = appContext?.assets?.open(Consts.DIR_SERVICE + File.separator + fileName)
                parser.setInput(inStream, null)
                parser.nextTag()
                val uuid = readUUID(parser)
                val service = readService(parser)
                service.uuid = uuid
                services[uuid] = service
                inStream?.close()
            } catch (e: XmlPullParserException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                inStream?.close()
            }
        }
        return services
    }

    // Reads single service file
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readService(parser: XmlPullParser): Service {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_SERVICE)
        val serviceName = readServiceName(parser)
        var summary = ""
        var characteristics: ArrayList<ServiceCharacteristic> = ArrayList()
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            when (name) {
                Consts.TAG_INFORMATIVE_TEXT -> summary = readSummary(parser)
                Consts.TAG_CHARACTERISTICS -> characteristics = readCharacteristics(parser)
                else -> skip(parser)
            }
        }
        return Service(serviceName, summary, characteristics)
    }

    // Reads service characteristics
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readCharacteristics(parser: XmlPullParser): ArrayList<ServiceCharacteristic> {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_CHARACTERISTICS)
        val characteristics = ArrayList<ServiceCharacteristic>()
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            if (name == Consts.TAG_CHARACTERISTIC) {
                characteristics.add(readServiceCharacteristic(parser))
            } else {
                skip(parser)
            }
        }
        return characteristics
    }

    // Reads single service characteristic
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readServiceCharacteristic(parser: XmlPullParser): ServiceCharacteristic {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_CHARACTERISTIC)
        val characteristic = ServiceCharacteristic()
        characteristic.name = readName(parser)
        characteristic.type = readType(parser)
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            if (name == Consts.TAG_DESCRIPTORS) {
                characteristic.descriptors = readDescriptors(parser)
            } else {
                skip(parser)
            }
        }
        return characteristic
    }

    // Reads descriptors
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readDescriptors(parser: XmlPullParser): ArrayList<Descriptor> {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_DESCRIPTORS)
        val descriptors = ArrayList<Descriptor>()
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            if (name == Consts.TAG_DESCRIPTOR) {
                descriptors.add(readDescriptor(parser))
            } else {
                skip(parser)
            }
        }
        return descriptors
    }

    // Reads single descriptor
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readDescriptor(parser: XmlPullParser): Descriptor {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_DESCRIPTOR)
        val descriptor = Descriptor()
        descriptor.name = readName(parser)
        descriptor.type = readType(parser)
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            skip(parser)
        }
        return descriptor
    }

    // Reads summary
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readSummary(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_INFORMATIVE_TEXT)
        val summary = StringBuilder(Consts.EMPTY_STRING)
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            if (name == Consts.TAG_SUMMARY) {
                summary.append(readText(parser))
                if (parser.name == Consts.TAG_P) {
                    summary.append(readText(parser))
                }
            } else if (name == Consts.TAG_P) {
                summary.append(readText(parser))
            } else {
                skip(parser)
            }
        }
        while (parser.name == null || parser.name != Consts.TAG_INFORMATIVE_TEXT) {
            parser.next()
        }
        return summary.toString()
    }

    // Reads service name
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readServiceName(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_SERVICE)
        return parser.getAttributeValue(null, Consts.ATTRIBUTE_NAME)
    }

    // Reads uuid
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readUUID(parser: XmlPullParser): UUID {
        val uuid = parser.getAttributeValue(null, Consts.ATTRIBUTE_UUID)
        return UUID.fromString(Common.convert16to128UUID(uuid))
    }

    // Reads type
    private fun readType(parser: XmlPullParser): String {
        return parser.getAttributeValue(null, Consts.ATTRIBUTE_TYPE)
    }

    // Parses characteristic files
    @Throws(XmlPullParserException::class, IOException::class)
    fun parseCharacteristics(): ConcurrentHashMap<UUID, Characteristic> {
        val characteristicFiles = appContext?.assets?.list(Consts.DIR_CHARACTERISTIC)
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        characteristics = ConcurrentHashMap()
        for (fileName in characteristicFiles!!) {
            try {
                val charact = parseCharacteristic(parser, Consts.DIR_CHARACTERISTIC + File.separator + fileName)
                characteristics[charact.uuid!!] = charact
            } catch (e: XmlPullParserException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return characteristics
    }

    // Parse single characteristic for given file
    @Throws(XmlPullParserException::class, IOException::class)
    fun parseCharacteristic(parser: XmlPullParser, fileName: String?): Characteristic {
        val inStream: InputStream? = appContext?.assets?.open(fileName!!)
        parser.setInput(inStream, null)
        parser.nextTag()
        val uuid = readUUID(parser)
        val type = readType(parser)
        val charact = readCharacteristic(parser)
        charact.uuid = uuid
        charact.type = type
        inStream?.close()
        return charact
    }

    // Reads single characteristic
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readCharacteristic(parser: XmlPullParser): Characteristic {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_CHARACTERISTIC)
        val characteristic = Characteristic()
        val characteristicName = readCharacteristicName(parser)
        characteristic.name = characteristicName
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            if (name == Consts.TAG_INFORMATIVE_TEXT) {
                val summary = readSummary(parser)
                characteristic.summary = summary
            } else if (name == Consts.TAG_VALUE) {
                val fields = readFieldValue(parser, characteristic)
            } else {
                skip(parser)
            }
        }
        return characteristic
    }

    // Reads characteristic fields
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readFieldValue(parser: XmlPullParser, characteristic: Characteristic): ArrayList<Field> {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_VALUE)
        val fields = ArrayList<Field>()
        characteristic.fields = fields
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            if (name == Consts.TAG_FIELD) {
                val field = readField(parser, characteristic)
                fields.add(field)
                if (field.reference != null) {
                    addCharacteristicReference(field, field.reference)
                }
            } else {
                skip(parser)
            }
        }
        return fields
    }

    // Reads single field
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readField(parser: XmlPullParser, characteristic: Characteristic): Field {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_FIELD)
        val field = Field()
        field.name = readName(parser)
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            when (name) {
                Consts.TAG_FORMAT -> field.format = readFormat(parser)
                Consts.TAG_MINIMUM -> field.minimum = readMinimum(parser)
                Consts.TAG_MAXIMUM -> field.maximum = readMaximum(parser)
                Consts.TAG_UNIT -> field.unit = readUnit(parser)
                Consts.TAG_BITFIELD -> field.bitfield = readBitField(parser)
                Consts.TAG_ENUMERATIONS -> field.enumerations = readEnumerations(parser)
                Consts.TAG_REQUIREMENT -> field.requirement = readRequirement(parser)
                Consts.TAG_REFERENCE -> field.reference = readReference(parser)
                Consts.TAG_DECIMAL_EXPONENT -> field.decimalExponent = readDecimalExponent(parser)
                else -> skip(parser)
            }
        }
        return field
    }

    // Reads requirement
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readRequirement(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_REQUIREMENT)
        val requirement = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, Consts.TAG_REQUIREMENT)
        return requirement
    }

    // Adds characteristic reference to given field
    @Throws(XmlPullParserException::class, IOException::class)
    private fun addCharacteristicReference(field: Field, reference: String?) {
        var ref: Characteristic? = null
        for (charact in characteristics.values) {
            if (charact.type == reference) {
                ref = charact
            }
        }
        if (ref != null) {
            for (fie in ref.fields!!) {
                field.referenceFields?.add(fie)
            }
        } else {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            val newCharacteristic = parseCharacteristic(parser, Consts.DIR_CHARACTERISTIC + File.separator
                    + reference?.trim { it <= ' ' } + Consts.FILE_EXTENSION)
            characteristics[newCharacteristic.uuid!!] = newCharacteristic
            for (fie in newCharacteristic.fields!!) {
                field.referenceFields?.add(fie)
            }
        }
    }

    // Reads characteristic name
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readCharacteristicName(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_CHARACTERISTIC)
        return parser.getAttributeValue(null, Consts.ATTRIBUTE_NAME)
    }

    // Reads field format
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readFormat(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_FORMAT)
        val format = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, Consts.TAG_FORMAT)
        return format
    }

    // Reads field minimum value
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readMinimum(parser: XmlPullParser): Long {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_MINIMUM)
        val minimum = readLong(parser)
        parser.require(XmlPullParser.END_TAG, ns, Consts.TAG_MINIMUM)
        return minimum
    }

    // Reads field maximum value
    @Throws(NumberFormatException::class, XmlPullParserException::class, IOException::class)
    private fun readMaximum(parser: XmlPullParser): Long {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_MAXIMUM)
        val maximum = readLong(parser)
        parser.require(XmlPullParser.END_TAG, ns, Consts.TAG_MAXIMUM)
        return maximum
    }

    // Reads field unit
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readUnit(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_UNIT)
        val unit = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, Consts.TAG_UNIT)
        return unit
    }

    // Reads field reference
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readReference(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_REFERENCE)
        val unit = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, Consts.TAG_REFERENCE)
        return unit
    }

    // Reads field decimal exponent
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readDecimalExponent(parser: XmlPullParser): Long {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_DECIMAL_EXPONENT)
        val decimalExponent = readLong(parser)
        parser.require(XmlPullParser.END_TAG, ns, Consts.TAG_DECIMAL_EXPONENT)
        return decimalExponent
    }

    // Reads bit field
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readBitField(parser: XmlPullParser): BitField {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_BITFIELD)
        val field = BitField()
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            if (name == Consts.TAG_BIT) {
                field.bits?.add(readBit(parser))
            } else {
                skip(parser)
            }
        }
        return field
    }

    // Reads single bit
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readBit(parser: XmlPullParser): Bit {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_BIT)
        val bit = Bit()
        bit.index = readIndex(parser)
        bit.size = readSize(parser)
        bit.name = readName(parser)
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            if (name == Consts.TAG_ENUMERATIONS) {
                bit.enumerations = readEnumerations(parser)
            } else {
                skip(parser)
            }
        }
        return bit
    }

    // Reads enumerations
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readEnumerations(parser: XmlPullParser): ArrayList<Enumeration> {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_ENUMERATIONS)
        val enumerations = ArrayList<Enumeration>()
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            if (name == Consts.TAG_ENUMERATION) {
                enumerations.add(readEnumeration(parser))
            } else {
                skip(parser)
            }
        }
        return enumerations
    }

    // Reads single enumeration
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readEnumeration(parser: XmlPullParser): Enumeration {
        val enumeration = Enumeration()
        val key = readKey(parser).toInt()
        val value = readValue(parser)
        val requires = readRequires(parser)
        enumeration.key = key
        enumeration.requires = requires
        enumeration.value = value
        parser.next()
        return enumeration
    }

    // Parse descriptors
    @Throws(IOException::class, XmlPullParserException::class)
    fun parseDescriptors(): HashMap<UUID?, Descriptor> {
        val descriptorsFiles = appContext?.assets?.list(Consts.DIR_DESCRIPTOR)
        var inStream: InputStream? = null
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        val descriptors = HashMap<UUID?, Descriptor>()
        for (fileName in descriptorsFiles!!) {
            try {
                inStream = appContext?.assets?.open(Consts.DIR_DESCRIPTOR + File.separator + fileName)
                parser.setInput(inStream, null)
                parser.nextTag()
                val uuid = readUUID(parser)
                val descriptor = readDescriptor(parser)
                descriptor.uuid = uuid
                descriptors[uuid] = descriptor
                inStream?.close()
            } catch (e: XmlPullParserException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                inStream?.close()
            }
        }
        return descriptors
    }

    // Reads requires attribute
    private fun readRequires(parser: XmlPullParser): String? {
        return parser.getAttributeValue(null, Consts.ATTRIBUTE_REQUIRES)
    }

    // Reads index attribute
    private fun readIndex(parser: XmlPullParser): Int {
        return parser.getAttributeValue(null, Consts.ATTRIBUTE_INDEX).toInt()
    }

    // Reads size attribute
    private fun readSize(parser: XmlPullParser): Int {
        return parser.getAttributeValue(null, Consts.ATTRIBUTE_SIZE).toInt()
    }

    // Reads key attribute
    private fun readKey(parser: XmlPullParser): String {
        return parser.getAttributeValue(null, Consts.ATTRIBUTE_KEY)
    }

    // Reads value attribute
    private fun readValue(parser: XmlPullParser): String {
        return parser.getAttributeValue(null, Consts.ATTRIBUTE_VALUE)
    }

    // Reads name attribute
    private fun readName(parser: XmlPullParser): String? {
        return parser.getAttributeValue(null, Consts.ATTRIBUTE_NAME)
    }

    // Skips useless xml tags
    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        check(parser.eventType == XmlPullParser.START_TAG)
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    // Reads text type
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    // Reads integer type
    @Throws(NumberFormatException::class, XmlPullParserException::class, IOException::class)
    private fun readLong(parser: XmlPullParser): Long {
        var result: Long = 0
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text.trim { it <= ' ' }.toLong()
            parser.nextTag()
        }
        return result
    }

    companion object {
        private val ns: String? = null
        private val locker = Any()
        var instance: BluetoothXmlParser? = null
            get() {
                if (field == null) {
                    synchronized(locker) {
                        if (field == null) {
                            field = BluetoothXmlParser()
                        }
                    }
                }
                return field
            }
            private set
    }
}