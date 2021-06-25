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

import java.io.File

// Consts - contains only static final members
object Consts {

    const val DIR_XML = "xml"
    val DIR_SERVICE = DIR_XML + File.separator + "services"
    val DIR_CHARACTERISTIC = DIR_XML + File.separator + "characteristics"
    val DIR_DESCRIPTOR = DIR_XML + File.separator + "descriptors"
    const val FILE_EXTENSION = ".xml"

    const val SERVICE_NAME = "service_name"
    const val UUID = "uuid"
    const val DEVICE_ADDRESS = "device_address"

    const val EMPTY_STRING = ""
    const val UNKNOWN_SERVICE = "Unknown Service"
    const val REQUIREMENT_MANDATORY = "Mandatory"
    const val REQUIREMENT_OPTIONAL = "Optional"

    const val TAG_SERVICE = "Service"
    const val TAG_CHARACTERISTIC = "Characteristic"
    const val TAG_INFORMATIVE_TEXT = "InformativeText"
    const val TAG_SUMMARY = "Summary"
    const val TAG_UNIT = "Unit"
    const val TAG_VALUE = "Value"
    const val TAG_FIELD = "Field"
    const val TAG_FORMAT = "Format"
    const val TAG_BITFIELD = "BitField"
    const val TAG_BIT = "Bit"
    const val TAG_ENUMERATIONS = "Enumerations"
    const val TAG_ENUMERATION = "Enumeration"
    const val TAG_MINIMUM = "Minimum"
    const val TAG_MAXIMUM = "Maximum"
    const val TAG_P = "p"
    const val TAG_REFERENCE = "Reference"
    const val TAG_DECIMAL_EXPONENT = "DecimalExponent"
    const val TAG_REQUIREMENT = "Requirement"
    const val TAG_CHARACTERISTICS = "Characteristics"
    const val TAG_DESCRIPTORS = "Descriptors"
    const val TAG_DESCRIPTOR = "Descriptor"

    const val ATTRIBUTE_UUID = "uuid"
    const val ATTRIBUTE_NAME = "name"
    const val ATTRIBUTE_INDEX = "index"
    const val ATTRIBUTE_SIZE = "size"
    const val ATTRIBUTE_KEY = "key"
    const val ATTRIBUTE_VALUE = "value"
    const val ATTRIBUTE_TYPE = "type"
    const val ATTRIBUTE_REQUIRES = "requires"

    const val BLUETOOTH_BASE_UUID_PREFIX = "0000"
    const val BLUETOOTH_BASE_UUID_POSTFIX = "-0000-1000-8000-00805F9B34FB"
}