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
package com.siliconlabs.bledemo.bluetoothdatamodel.parsing;

import java.io.File;

// Consts - contains only static final members
public class Consts {

    public static final String DIR_XML = "xml";
    public static final String DIR_SERVICE = DIR_XML + File.separator + "services";
    public static final String DIR_CHARACTERISTIC = DIR_XML + File.separator + "characteristics";
    public static final String DIR_DESCRIPTOR = DIR_XML + File.separator + "descriptors";
    public static final String FILE_EXTENSION = ".xml";

    public static final String SERVICE_NAME = "service_name";
    public static final String UUID = "uuid";
    public static final String DEVICE_ADDRESS = "device_address";

    public static final String EMPTY_STRING = "";
    public static final String UNKNOWN_SERVICE = "Unknown Service";
    public static final String REQUIREMENT_MANDATORY = "Mandatory";
    public static final String REQUIREMENT_OPTIONAL = "Optional";

    public static final String TAG_SERVICE = "Service";
    public static final String TAG_CHARACTERISTIC = "Characteristic";
    public static final String TAG_INFORMATIVE_TEXT = "InformativeText";
    public static final String TAG_SUMMARY = "Summary";
    public static final String TAG_UNIT = "Unit";
    public static final String TAG_VALUE = "Value";
    public static final String TAG_FIELD = "Field";
    public static final String TAG_FORMAT = "Format";
    public static final String TAG_BITFIELD = "BitField";
    public static final String TAG_BIT = "Bit";
    public static final String TAG_ENUMERATIONS = "Enumerations";
    public static final String TAG_ENUMERATION = "Enumeration";
    public static final String TAG_MINIMUM = "Minimum";
    public static final String TAG_MAXIMUM = "Maximum";
    public static final String TAG_P = "p";
    public static final String TAG_REFERENCE = "Reference";
    public static final String TAG_REQUIREMENT = "Requirement";
    public static final String TAG_CHARACTERISTICS = "Characteristics";
    public static final String TAG_DESCRIPTORS = "Descriptors";
    public static final String TAG_DESCRIPTOR = "Descriptor";

    public static final String ATTRIBUTE_UUID = "uuid";
    public static final String ATTRIBUTE_NAME = "name";
    public static final String ATTRIBUTE_INDEX = "index";
    public static final String ATTRIBUTE_SIZE = "size";
    public static final String ATTRIBUTE_KEY = "key";
    public static final String ATTRIBUTE_VALUE = "value";
    public static final String ATTRIBUTE_TYPE = "type";
    public static final String ATTRIBUTE_REQUIRES = "requires";

    public static final String BLUETOOTH_BASE_UUID_PREFIX = "0000";
    public static final String BLUETOOTH_BASE_UUID_POSTFIX = "-0000-1000-8000-00805F9B34FB";
}
