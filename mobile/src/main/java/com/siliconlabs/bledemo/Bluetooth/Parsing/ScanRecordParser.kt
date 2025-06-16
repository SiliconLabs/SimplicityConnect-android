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
package com.siliconlabs.bledemo.bluetooth.parsing

import com.siliconlabs.bledemo.utils.Converters
import java.util.*

// ScanRecordParser - parses advertise data from BLE device
object ScanRecordParser {
    private const val TIME_INTERVAL_FACTOR = 1.25
    private const val AD_TYPE_FLAGS: Byte = 0x01
    private const val AD_TYPE_0x02: Byte = 0x02
    private const val AD_TYPE_0x03: Byte = 0x03
    private const val AD_TYPE_0x04: Byte = 0x04
    private const val AD_TYPE_0x05: Byte = 0x05
    private const val AD_TYPE_0x06: Byte = 0x06
    private const val AD_TYPE_0x07: Byte = 0x07
    private const val AD_TYPE_0x08: Byte = 0x08
    private const val AD_TYPE_0x09: Byte = 0x09
    private const val AD_TYPE_0x0A: Byte = 0x0A
    private const val AD_TYPE_0x0D: Byte = 0x0D
    private const val AD_TYPE_0x0E: Byte = 0x0E
    private const val AD_TYPE_0x0F: Byte = 0x0F
    private const val AD_TYPE_0x10: Byte = 0x10
    private const val AD_TYPE_0x11: Byte = 0x11
    private const val AD_TYPE_0x12: Byte = 0x12
    private const val AD_TYPE_0x14: Byte = 0x14
    private const val AD_TYPE_0x15: Byte = 0x15
    private const val AD_TYPE_0x16: Byte = 0x16
    private const val AD_TYPE_0x17: Byte = 0x17
    private const val AD_TYPE_0x18: Byte = 0x18
    private const val AD_TYPE_0x19: Byte = 0x19
    private const val AD_TYPE_0x1A: Byte = 0x1A
    private const val AD_TYPE_0x1B: Byte = 0x1B
    private const val AD_TYPE_0x1C: Byte = 0x1C
    private const val AD_TYPE_0x1D: Byte = 0x1D
    private const val AD_TYPE_0x1E: Byte = 0x1E
    private const val AD_TYPE_0x1F: Byte = 0x1F
    private const val AD_TYPE_0x20: Byte = 0x20
    private const val AD_TYPE_0x21: Byte = 0x21
    private const val AD_TYPE_0x22: Byte = 0x22
    private const val AD_TYPE_0x23: Byte = 0x23
    private const val AD_TYPE_0x24: Byte = 0x24
    private const val AD_TYPE_0x25: Byte = 0x25
    private const val AD_TYPE_0x26: Byte = 0x26
    private const val AD_TYPE_0x27: Byte = 0x27
    private const val AD_TYPE_0x28: Byte = 0x28
    private const val AD_TYPE_0x29: Byte = 0x29
    private const val AD_TYPE_0x2A: Byte = 0x2A
    private const val AD_TYPE_0x2B: Byte = 0x2B
    private const val AD_TYPE_0x2C: Byte = 0x2C
    private const val AD_TYPE_0x2D: Byte = 0x2D
    private const val AD_TYPE_0x3D: Byte = 0x3D
    private const val AD_TYPE_0xFF: Byte = 0xFF.toByte()
    const val SPLIT = "&&"
    private const val AD_STRING_NOT_SPECIFY = "not spec."
    private const val AD_STRING_PARSING_ERROR = "PARSING ERROR"
    private const val AD_STRING_NO_DATA = "No data"
    private const val AD_STRING_INVALID_DATA = "Invalid data:"

    // Gets single formatted advertise data
    private fun getAdvertisementData(advertisementDataBuffer: ByteArray): String? {
        val len = advertisementDataBuffer[0]
        val advertisementData: StringBuilder
        if (len.toInt() == 0) {
            return null
        }
        val type: Byte = advertisementDataBuffer[1]
        val data = advertisementDataBuffer.copyOfRange(2, advertisementDataBuffer.size)
        when (type) {
            AD_TYPE_FLAGS -> {
                advertisementData = StringBuilder("Flags")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else {
                    advertisementData.append(parseFlag(data))
                }
            }
            AD_TYPE_0x02 -> {
                advertisementData = StringBuilder("Incomplete List of 16-bit Service Class UUIDs")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size % 2 == 0) {
                    advertisementData.append(get16bitServicesUUIDsWithNamesAsString(data))
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x03 -> {
                advertisementData = StringBuilder("Complete List of 16-bit Service Class UUIDs")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size % 2 == 0) {
                    advertisementData.append(get16bitServicesUUIDsWithNamesAsString(data))
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x04 -> {
                advertisementData = StringBuilder("Incomplete List of 32-bit Service Class UUIDs")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size % 4 == 0) {
                    advertisementData.append(get32bitServiceUUIDsAsString(data))
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x05 -> {
                advertisementData = StringBuilder("Complete List of 32-bit Service Class UUIDs")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size % 4 == 0) {
                    advertisementData.append(get32bitServiceUUIDsAsString(data))
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x06 -> {
                advertisementData = StringBuilder("Incomplete List of 128-bit Service Class UUIDs")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size % 16 == 0) {
                    advertisementData.append(get128bitServiceUUIDsAsString(data))
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x07 -> {
                advertisementData = StringBuilder("Complete List of 128-bit Service Class UUIDs")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size % 16 == 0) {
                    advertisementData.append(get128bitServiceUUIDsAsString(data))
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x08 -> {
                advertisementData = StringBuilder("Shortened Local Name")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else {
                    advertisementData.append(Converters.getAsciiValue(data))
                }
            }
            AD_TYPE_0x09 -> {
                advertisementData = StringBuilder("Complete Local Name")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else {
                    advertisementData.append(Converters.getAsciiValue(data))
                }
            }
            AD_TYPE_0x0A -> {
                advertisementData = StringBuilder("TX Power Level")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size == 1) {
                    advertisementData.append(data[0]).append(" dBm")
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x0D -> {
                advertisementData = StringBuilder("Class Of Device")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size == 3) {
                    advertisementData.append(getRawDataOctets0x__AsString(data))
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x0E -> {
                advertisementData = StringBuilder("Simple Pairing Hash C/C-192")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else {
                    advertisementData.append(getRawDataOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x0F -> {
                advertisementData = StringBuilder("Simple Pairing Randomizer R/R-192")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else {
                    advertisementData.append(getRawDataOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x10 -> {
                advertisementData = StringBuilder("Device ID / Security Manager TK Value")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else {
                    advertisementData.append(getRawDataOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x11 -> {
                advertisementData = StringBuilder("Security Manager Out of Band Flags")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else {
                    advertisementData.append(getRawDataOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x12 -> {
                advertisementData = StringBuilder("Slave Connection Interval Range")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size == 4) {
                    advertisementData.append(getSlaveConnectionIntervalRangeAsString(data))
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x14 -> {
                advertisementData = StringBuilder("List of 16-bit Service Solicitation UUIDs")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size % 2 == 0) {
                    advertisementData.append(get16bitServiceUUIDsAsString(data))
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x15 -> {
                advertisementData = StringBuilder("List of 128-bit Service Solicitation UUIDs")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size % 16 == 0) {
                    advertisementData.append(get128bitServiceUUIDsAsString(data))
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x16 -> {
                advertisementData = StringBuilder("Service Data")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size >= 2) {
                    advertisementData.append(get16bitUUIDServiceDataAsString(data))
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0xFF -> {
                advertisementData = StringBuilder("Manufacturer Specific Data")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size >= 2) {
                    advertisementData.append(getManufacturerSpecificData(data))
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x17 -> {
                advertisementData = StringBuilder("Public Target Address")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else {
                    advertisementData.append(prepareSixOctetAddresses(data))
                }
            }
            AD_TYPE_0x18 -> {
                advertisementData = StringBuilder("Random Target Address")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else {
                    advertisementData.append(prepareSixOctetAddresses(data))
                }
            }
            AD_TYPE_0x19 -> {
                advertisementData = StringBuilder("Appearance")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size == 2) {
                    advertisementData.append(getAppearanceAsString(data))
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x1A -> {
                advertisementData = StringBuilder("Advertising Interval")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size == 2) {
                    advertisementData.append(getAdvertisingIntervalAsString(data))
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x1B -> {
                advertisementData = StringBuilder("LE Bluetooth Device Address")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size == 7) {
                    advertisementData.append(getLEBluetoothDeviceAddressAsString(data))
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x1C -> {
                advertisementData = StringBuilder("LE Role")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size == 1) {
                    advertisementData.append(getLERoleAsString(data))
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x1D -> {
                advertisementData = StringBuilder("Simple Pairing Hash C-256")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size == 16) {
                    advertisementData.append(getRawDataOctets0x__AsString(data))
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x1E -> {
                advertisementData = StringBuilder("Simple Pairing Randomizer R-256")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size == 16) {
                    advertisementData.append(getRawDataOctets0x__AsString(data))
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x1F -> {
                advertisementData = StringBuilder("List of 32-bit Service Solicitation UUIDs")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size % 4 == 0) {
                    advertisementData.append(get32bitServiceUUIDsAsString(data))
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x20 -> {
                advertisementData = StringBuilder("Service Data - 32-bit UUID")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size >= 4) {
                    advertisementData.append(get32bitUUIDServiceDataAsString(data))
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x21 -> {
                advertisementData = StringBuilder("Service Data - 128-bit UUID")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size >= 16) {
                    advertisementData.append(get128bitUUIDServiceDataAsString(data))
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x22 -> {
                advertisementData = StringBuilder("LE Secure Connections Confirmation Value")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size == 16) {
                    advertisementData.append(getRawDataOctets0x__AsString(data))
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x23 -> {
                advertisementData = StringBuilder("LE Secure Connections Random Value")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else if (data.size == 16) {
                    advertisementData.append(getRawDataOctets0x__AsString(data))
                } else {
                    advertisementData.append(getParsingErrorOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x24 -> {
                advertisementData = StringBuilder("URI")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else {
                    advertisementData.append(getUriAsString(data))
                }
            }
            AD_TYPE_0x25 -> {
                advertisementData = StringBuilder("Indoor Positioning")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else {
                    advertisementData.append(getRawDataOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x26 -> {
                advertisementData = StringBuilder("Transport Discovery Data")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else {
                    advertisementData.append(getRawDataOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x27 -> {
                advertisementData = StringBuilder("LE Supported Features")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else {
                    advertisementData.append(getRawDataOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x28 -> {
                advertisementData = StringBuilder("Channel Map Update Indication")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else {
                    advertisementData.append(getRawDataOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x29 -> {
                advertisementData = StringBuilder("PB-ADV")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else {
                    advertisementData.append(getRawDataOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x2A -> {
                advertisementData = StringBuilder("Mesh Message")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else {
                    advertisementData.append(getRawDataOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x2B -> {
                advertisementData = StringBuilder("Mesh Beacon")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else {
                    advertisementData.append(getRawDataOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x2C -> {
                advertisementData = StringBuilder("BIGInfo")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else {
                    advertisementData.append(getRawDataOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x2D -> {
                advertisementData = StringBuilder("Broadcast_Code")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else {
                    advertisementData.append(getRawDataOctets0x__AsString(data))
                }
            }
            AD_TYPE_0x3D -> {
                advertisementData = StringBuilder("3D Information Data")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else {
                    advertisementData.append(getRawDataOctets0x__AsString(data))
                }
            }
            else -> {
                advertisementData = StringBuilder("Unknown Type")
                advertisementData.append(SPLIT)
                if (data.isEmpty()) {
                    advertisementData.append(AD_STRING_NO_DATA)
                } else {
                    advertisementData.append(getRawDataOctets0x__AsString(data))
                }
            }
        }
        return advertisementData.toString()
    }

    private fun getSchemeNameStringFromValue(value: Int): String {
        return when (value) {
            0x01 -> ""
            0x02 -> "aaa:"
            0x03 -> "aaas:"
            0x04 -> "about:"
            0x05 -> "acap:"
            0x06 -> "acct:"
            0x07 -> "cap:"
            0x08 -> "cid:"
            0x09 -> "coap:"
            0x0A -> "coaps:"
            0x0B -> "crid:"
            0x0C -> "data:"
            0x0D -> "dav:"
            0x0E -> "dict:"
            0x0F -> "dns:"
            0x10 -> "file:"
            0x11 -> "ftp:"
            0x12 -> "geo:"
            0x13 -> "go:"
            0x14 -> "gopher:"
            0x15 -> "h323:"
            0x16 -> "http:"
            0x17 -> "https:"
            0x18 -> "iax:"
            0x19 -> "icap:"
            0x1A -> "im:"
            0x1B -> "imap:"
            0x1C -> "info:"
            0x1D -> "ipp:"
            0x1E -> "ipps:"
            0x1F -> "iris:"
            0x20 -> "iris.beep:"
            0x21 -> "iris.xpc:"
            0x22 -> "iris.xpcs:"
            0x23 -> "iris.lwz:"
            0x24 -> "jabber:"
            0x25 -> "ldap:"
            0x26 -> "mailto:"
            0x27 -> "mid:"
            0x28 -> "msrp:"
            0x29 -> "msrps:"
            0x2A -> "mtqp:"
            0x2B -> "mupdate:"
            0x2C -> "news:"
            0x2D -> "nfs:"
            0x2E -> "ni:"
            0x2F -> "nih:"
            0x30 -> "nntp:"
            0x31 -> "opaquelocktoken:"
            0x32 -> "pop:"
            0x33 -> "pres:"
            0x34 -> "reload:"
            0x35 -> "rtsp:"
            0x36 -> "rtsps:"
            0x37 -> "rtspu:"
            0x38 -> "service:"
            0x39 -> "session:"
            0x3A -> "shttp:"
            0x3B -> "sieve:"
            0x3C -> "sip:"
            0x3D -> "sips:"
            0x3E -> "sms:"
            0x3F -> "snmp:"
            0x40 -> "soap.beep:"
            0x41 -> "soap.beeps:"
            0x42 -> "stun:"
            0x43 -> "stuns:"
            0x44 -> "tag:"
            0x45 -> "tel:"
            0x46 -> "telnet:"
            0x47 -> "tftp:"
            0x48 -> "thismessage:"
            0x49 -> "tn3270:"
            0x4A -> "tip:"
            0x4B -> "turn:"
            0x4C -> "turns:"
            0x4D -> "tv:"
            0x4E -> "urn:"
            0x4F -> "vemmi:"
            0x50 -> "ws:"
            0x51 -> "wss:"
            0x52 -> "xcon:"
            0x53 -> "xcon-userid:"
            0x54 -> "xmlrpc.beep:"
            0x55 -> "xmlrpc.beeps:"
            0x56 -> "xmpp:"
            0x57 -> "z39.50r:"
            0x58 -> "z39.50s:"
            0x59 -> "acr:"
            0x5A -> "adiumxtra:"
            0x5B -> "afp:"
            0x5C -> "afs:"
            0x5D -> "aim:"
            0x5E -> "apt:"
            0x5F -> "attachment;"
            0x60 -> "aw:"
            0x61 -> "barion:"
            0x62 -> "beshare:"
            0x63 -> "bitcoin:"
            0x64 -> "bolo:"
            0x65 -> "callto:"
            0x66 -> "chrome:"
            0x67 -> "chrome-extension:"
            0x68 -> "com-eventbrite-attendee:"
            0x69 -> "content:"
            0x6A -> "cvs:"
            0x6B -> "dlna-playsingle:"
            0x6C -> "dlna-playcontainer:"
            0x6D -> "dtn:"
            0x6E -> "dvb:"
            0x6F -> "ed2k:"
            0x70 -> "facetime:"
            0x71 -> "feed:"
            0x72 -> "feedready:"
            0x73 -> "finger:"
            0x74 -> "fish:"
            0x75 -> "gg:"
            0x76 -> "git:"
            0x77 -> "gizmoproject:"
            0x78 -> "gtalk:"
            0x79 -> "ham:"
            0x7A -> "hcp:"
            0x7B -> "icon:"
            0x7C -> "ipc:"
            0x7D -> "irc:"
            0x7E -> "irc6:"
            0x7F -> "ircs:"
            0x80 -> "itms:"
            0x81 -> "jar:"
            0x82 -> "jms:"
            0x83 -> "keyparc;"
            0x84 -> "lastfm:"
            0x85 -> "ldaps:"
            0x86 -> "magnet:"
            0x87 -> "maps:"
            0x88 -> "market:"
            0x89 -> "message:"
            0x8A -> "mms:"
            0x8B -> "ms-help:"
            0x8C -> "ms-settings-power:"
            0x8D -> "msnim:"
            0x8E -> "mumble"
            0x8F -> "mvn:"
            0x90 -> "notes:"
            0x91 -> "oid:"
            0x92 -> "palm:"
            0x93 -> "paparazzi:"
            0x94 -> "pkcs11:"
            0x95 -> "platform:"
            0x96 -> "proxy:"
            0x97 -> "psyc:"
            0x98 -> "query:"
            0x99 -> "res:"
            0x9A -> "resource:"
            0x9B -> "rmi:"
            0x9C -> "rsync:"
            0x9D -> "rtmfp:"
            0x9E -> "rtmp:"
            0x9F -> "secondlife:"
            0xA0 -> "sftp:"
            0xA1 -> "sgn:"
            0xA2 -> "skype:"
            0xA3 -> "smb:"
            0xA4 -> "smtp:"
            0xA5 -> "soldat:"
            0xA6 -> "spotify:"
            0xA7 -> "ssh:"
            0xA8 -> "steam:"
            0xA9 -> "submit:"
            0xAA -> "svn:"
            0xAB -> "teamspeak:"
            0xAC -> "teliaeid:"
            0xAD -> "things:"
            0xAE -> "udp:"
            0xAF -> "unreal:"
            0xB0 -> "ut2004:"
            0xB1 -> "ventrilo:"
            0xB2 -> "view-source:"
            0xB3 -> "webcal:"
            0xB4 -> "wtai:"
            0xB5 -> "wyciwyg:"
            0xB6 -> "xfire:"
            0xB7 -> "xri:"
            0xB8 -> "ymsgr:"
            0xB9 -> "example:"
            0xBA -> "ms-settings-cloudstorage:"
            else -> "[unknown scheme (00" + Converters.getHexValue(value.toByte()) + ")]:"
        }
    }

    private fun getAppearanceValueDescriptionFromValue(value: Int): String {
        return when (value) {
            64 -> "Generic Phone (Generic category)"
            128 -> "Generic Computer (Generic category)"
            192 -> "Generic Watch (Generic category)"
            193 -> "Watch: Sports Watch (Watch subtype)"
            256 -> "Generic Clock (Generic category)"
            320 -> "Generic Display (Generic category)"
            384 -> "Generic Remote Control (Generic category)"
            448 -> "Generic Eye-glasses (Generic category)"
            512 -> "Generic Tag (Generic category)"
            576 -> "Generic Keyring (Generic category)"
            640 -> "Generic Media Player (Generic category)"
            704 -> "Generic Barcode Scanner (Generic category)"
            768 -> "Generic Thermometer (Generic category)"
            769 -> "Thermometer: Ear (Thermometer subtype)"
            832 -> "Generic Heart rate Sensor (Generic category)"
            833 -> "Heart Rate Sensor: Heart Rate Belt (Heart Rate Sensor subtype)"
            896 -> "Generic Blood Pressure (Generic category)"
            897 -> "Blood Pressure: Arm (Blood Pressure subtype)"
            898 -> "Blood Pressure: Wrist (Blood Pressure subtype)"
            960 -> "Human Interface Device (HID) (HID Generic)"
            961 -> "Keyboard (HID subtype)"
            962 -> "Mouse (HID subtype)"
            963 -> "Joystick (HID subtype)"
            964 -> "Gamepad (HID subtype)"
            965 -> "Digitizer Tablet (HID subtype)"
            966 -> "Card Reader (HID subtype)"
            967 -> "Digital Pen (HID subtype)"
            968 -> "Barcode Scanner (HID subtype)"
            1024 -> "Generic Glucose Meter (Generic category)"
            1088 -> "Generic Running Walking Sensor (Generic category)"
            1089 -> "Running Walking Sensor: In-Shoe (Running Walking Sensor subtype)"
            1090 -> "Running Walking Sensor: On-Shoe (Running Walking Sensor subtype)"
            1091 -> "Running Walking Sensor: On-Hip (Running Walking Sensor subtype)"
            1152 -> "Generic: Cycling (Generic category)"
            1153 -> "Cycling: Cycling Computer (Cycling subtype)"
            1154 -> "Cycling: Speed Sensor (Cycling subtype)"
            1155 -> "Cycling: Cadence Sensor (Cycling subtype)"
            1156 -> "Cycling: Power Sensor (Cycling subtype)"
            1157 -> "Cycling: Speed and Cadence Sensor (Cycling subtype)"
            3136 -> "Generic: Pulse Oximeter (Pulse Oximeter Generic Category)"
            3137 -> "Fingertip (Pulse Oximeter subtype)"
            3138 -> "Wrist Worn (Pulse Oximeter subtype)"
            3200 -> "Generic: Weight Scale (Weight Scale Generic Category)"
            3264 -> "Generic Personal Mobility Device (Personal Mobility Device)"
            3265 -> "Powered Wheelchair (Personal Mobility Device)"
            3266 -> "Mobility Scooter (Personal Mobility Device)"
            3328 -> "Generic Continuous Glucose Monitor (Continuous Glucose Monitor)"
            3392 -> "Generic Insulin Pump (Insulin Pump)"
            3393 -> "Insulin Pump, durable pump (Insulin Pump)"
            3396 -> "Insulin Pump, patch pump (Insulin Pump)"
            3400 -> "Insulin Pen (Insulin Pump)"
            3456 -> "Generic Medication Delivery (Medication Delivery)"
            5184 -> "Generic: Outdoor Sports Activity (Outdoor Sports Activity Generic Category)"
            5185 -> "Location Display Device (Outdoor Sports Activity subtype)"
            5186 -> "Location and Navigation Display Device (Outdoor Sports Activity subtype)"
            5187 -> "Location Pod (Outdoor Sports Activity subtype)"
            5188 -> "Location and Navigation Pod (Outdoor Sports Activity subtype)"
            else -> "Unknown"
        }
    }

    private fun get16BitServiceName(value: Int): String {
        return when (value) {
            0x1800 -> "Generic Access"
            0x1811 -> "Alert Notification Service"
            0x1815 -> "Automation IO"
            0x180F -> "Battery Service"
            0x183B -> "Binary Sensor"
            0x1810 -> "Blood Pressure"
            0x181B -> "Body Composition"
            0x181E -> "Bond Management Service"
            0x181F -> "Continuous Glucose Monitoring"
            0x1805 -> "Current Time Service"
            0x1818 -> "Cycling Power"
            0x1816 -> "Cycling Speed and Cadence"
            0x180A -> "Device Information"
            0x183C -> "Emergency Configuration"
            0x181A -> "Environmental Sensing"
            0x1826 -> "Fitness Machine"
            0x1801 -> "Generic Attribute"
            0x1808 -> "Glucose"
            0x1809 -> "Health Thermometer"
            0x180D -> "Heart Rate"
            0x1823 -> "HTTP Proxy"
            0x1812 -> "Human Interface Device"
            0x1802 -> "Immediate Alert"
            0x1821 -> "Indoor Positioning"
            0x183A -> "Insulin Delivery"
            0x1820 -> "Internet Protocol Support Service"
            0x1803 -> "Link Loss"
            0x1819 -> "Location and Navigation"
            0x1827 -> "Mesh Provisioning Service"
            0x1828 -> "Mesh Proxy Service"
            0x1807 -> "Next DST Change Service"
            0x1825 -> "Object Transfer Service"
            0x180E -> "Phone Alert Status Service"
            0x1822 -> "Pulse Oximeter Service"
            0x1829 -> "Reconnection Configuration"
            0x1806 -> "Reference Time Update Service"
            0x1814 -> "Running Speed and Cadence"
            0x1813 -> "Scan Parameters"
            0x1824 -> "Transport Discovery"
            0x1804 -> "Tx Power"
            0x181C -> "User Data"
            0x181D -> "Weight Scale"
            else -> "Unknown Service UUID"
        }
    }

    private fun getRawDataOctets0x__AsString(data: ByteArray): String {
        val builder = StringBuilder()
        for (i in data.indices) {
            builder.append("0x").append(Converters.getHexValue(data[i]))
            if (i != data.size - 1) builder.append(", ")
        }
        return builder.toString()
    }

    private fun get16bitServiceUUIDsAsString(data: ByteArray): String {
        val builder = StringBuilder()
        val addressess = data.size / 2
        if (data.size % 2 != 0) return ""
        for (i in 1..addressess) {
            builder.append("0x")
            for (j in i * 2 - 1 downTo (i - 1) * 2) {
                builder.append(Converters.getHexValue(data[j]))
            }
            if (i != addressess) builder.append(", ")
        }
        return builder.toString()
    }

    private fun get16bitServicesUUIDsWithNamesAsString(data: ByteArray): String {
        val builder = StringBuilder()
        val addressess = data.size / 2
        if (data.size % 2 != 0) return ""
        for (i in 1..addressess) {
            val valueBuilder = StringBuilder()
            for (j in i * 2 - 1 downTo (i - 1) * 2) {
                valueBuilder.append(Converters.getHexValue(data[j]))
            }
            val value = valueBuilder.toString().toInt(16)
            builder.append("0x").append(valueBuilder.toString()).append(" - ").append(get16BitServiceName(value))
            if (i != addressess) builder.append(", ").append("<br/>")
        }
        return builder.toString()
    }

    private fun get128bitServiceUUIDsAsString(data: ByteArray): String {
        val builder = StringBuilder()
        val addressess = data.size / 16
        if (data.size % 16 != 0) return ""
        for (i in 1..addressess) {
            var lineCounter = 0
            for (j in i * 16 - 1 downTo (i - 1) * 16) {
                lineCounter++
                builder.append(Converters.getHexValue(data[j]).lowercase(Locale.getDefault()))
                if (lineCounter == 4 || lineCounter == 6 || lineCounter == 8 || lineCounter == 10) builder.append("-")
            }
            if (i != addressess) builder.append(", ")
        }
        return builder.toString()
    }

    private fun get32bitServiceUUIDsAsString(data: ByteArray): String {
        val builder = StringBuilder()
        val addressess = data.size / 4
        if (data.size % 4 != 0) return ""
        for (i in 1..addressess) {
            builder.append("0x")
            for (j in i * 4 - 1 downTo (i - 1) * 4) {
                builder.append(Converters.getHexValue(data[j]))
            }
            if (i != addressess) builder.append(", ")
        }
        return builder.toString()
    }

    private fun get128bitUUIDServiceDataAsString(data: ByteArray): String {
        val builder = StringBuilder()
        if (data.size >= 16) {
            builder.append("UUID: ")
            for (i in 15 downTo 0) {
                builder.append(Converters.getHexValue(data[i]).lowercase(Locale.getDefault()))
                if (i == 6 || i == 8 || i == 10 || i == 12) builder.append("-")
            }
            builder.append(" Data: ")
            if (data.size > 16) {
                builder.append("0x")
                for (i in 16 until data.size) {
                    builder.append(Converters.getHexValue(data[i]))
                }
            }
        } else {
            return ""
        }
        return builder.toString()
    }

    private fun get32bitUUIDServiceDataAsString(data: ByteArray): String {
        val builder = StringBuilder()
        if (data.size >= 4) {
            builder.append("UUID: 0x")
            for (i in 3 downTo 0) {
                builder.append(Converters.getHexValue(data[i]))
            }
            builder.append(" Data: ")
            if (data.size > 4) {
                builder.append("0x")
                for (i in 4 until data.size) {
                    builder.append(Converters.getHexValue(data[i]))
                }
            }
        } else {
            return ""
        }
        return builder.toString()
    }

    private fun get16bitUUIDServiceDataAsString(data: ByteArray): String {
        val builder = StringBuilder()
        if (data.size >= 2) {
            builder.append("UUID: 0x")
            for (i in 1 downTo 0) {
                builder.append(Converters.getHexValue(data[i]))
            }
            builder.append(" Data: ")
            if (data.size > 2) {
                builder.append("0x")
                for (i in 2 until data.size) {
                    builder.append(Converters.getHexValue(data[i]))
                }
            }
        } else {
            return ""
        }
        return builder.toString()
    }

    private fun getParsingErrorOctets0x__AsString(data: ByteArray): String {
        val builder = StringBuilder()
        builder.append(AD_STRING_PARSING_ERROR).append(": ")
        for (i in data.indices) {
            builder.append("0x").append(Converters.getHexValue(data[i]))
            if (i != data.size - 1) builder.append(", ")
        }
        return builder.toString()
    }

    private fun getUriAsString(data: ByteArray): String {
        val builder = StringBuilder()
        if (data.isNotEmpty()) {
            builder.append(getSchemeNameStringFromValue(Converters.getHexValue(data[0]).toInt(16))).append(" ")
            if (data.size > 1) {
                for (i in 1 until data.size) {
                    builder.append(data[i].toChar())
                }
            }
        }
        return builder.toString()
    }

    private fun getLEBluetoothDeviceAddressAsString(data: ByteArray): String {
        val builder = StringBuilder()
        if (data.size == 7) {
            for (i in 6 downTo 1) {
                builder.append(Converters.getHexValue(data[i]))
                if (i != 1) builder.append(":")
            }
            val flag = data[0].toInt() and 0x01
            if (flag == 0) {
                builder.append(" (Public Device Address)")
            } else {
                builder.append(" (Random Device Address)")
            }
        } else {
            return ""
        }
        return builder.toString()
    }

    private fun getManufacturerSpecificData(data: ByteArray): String {
        val builder = StringBuilder()
        if (data.size >= 2) {
            builder.append("Company Code: 0x").append(Converters.getHexValue(data[1])).append(Converters.getHexValue(data[0])).append("").append("<br/>")
            builder.append("Data: ")
            if (data.size > 2) {
                builder.append("0x")
                for (i in 2 until data.size) {
                    builder.append(Converters.getHexValue(data[i]))
                }
            }
        } else {
            return ""
        }
        return builder.toString()
    }

    private fun getAppearanceAsString(data: ByteArray): String {
        val builder = StringBuilder()
        if (data.size == 2) {
            val hexValue = Converters.getHexValue(data[1]) + Converters.getHexValue(data[0])
            builder.append("0x").append(hexValue)
            builder.append(" [").append(hexValue.toInt(16)).append("]")
            builder.append(" ").append(getAppearanceValueDescriptionFromValue(hexValue.toInt(16)))
        } else {
            return ""
        }
        return builder.toString()
    }

    private fun getAdvertisingIntervalAsString(data: ByteArray): String {
        val builder = StringBuilder()
        if (data.size == 2) {
            val hexValue = Converters.getHexValue(data[1]) + Converters.getHexValue(data[0])
            val ms = 0.625 * hexValue.toInt(16)
            builder.append(ms).append(" ms")
        } else {
            return ""
        }
        return builder.toString()
    }

    private fun getSlaveConnectionIntervalRangeAsString(data: ByteArray): String {
        val builder = StringBuilder()
        if (data.size == 4) {
            var min = (Converters.getHexValue(data[1]) + Converters.getHexValue(data[0])).toInt(16).toFloat()
            if (min == 0xffff.toFloat()) {
                builder.append(AD_STRING_NOT_SPECIFY)
            } else if (0x0006 <= min && min <= 0x0C80) {
                min *= TIME_INTERVAL_FACTOR.toFloat()
                builder.append(min).append("ms").append(" - ")
            } else {
                builder.append("(0x").append(Converters.getHexValue(data[1])).append(Converters.getHexValue(data[0])).append(") OUT OF RANGE")
                builder.append(" - ")
            }
            var max = (Converters.getHexValue(data[3]) + Converters.getHexValue(data[2])).toInt(16).toFloat()
            if (max == 0xffff.toFloat()) {
                builder.append(AD_STRING_NOT_SPECIFY)
            } else if (0x0006 <= max && max <= 0x0C80) {
                max *= TIME_INTERVAL_FACTOR.toFloat()
                builder.append(max)
                builder.append("ms")
            } else {
                builder.append("(0x").append(Converters.getHexValue(data[3])).append(Converters.getHexValue(data[2])).append(") OUT OF RANGE")
            }
        } else {
            return ""
        }
        return builder.toString()
    }

    private fun parseFlag(data: ByteArray): String {
        val builder = StringBuilder()
        if (data.isNotEmpty()) {
            builder.append("0x")
            for (i in data.indices.reversed()) {
                builder.append(Converters.getHexValue(data[i]))
            }
            builder.append(": ")
            val firstByte = data[0]
            var added = 0

            // Check bit 0
            if (firstByte.toInt() and  1 == 1) {
                builder.append("LE Limited Discoverable Mode, ")
                added++
            }

            // Check bit 1
            if (firstByte.toInt() and 2 == 2) {
                builder.append("LE General Discoverable Mode, ")
                added++
            }

            // Check bit 2
            if (firstByte.toInt() and 4 == 4) {
                builder.append("BR/EDR Not Supported, ")
                added++
            }

            // Check bit 3
            if (firstByte.toInt() and 8 == 8) {
                builder.append("Simultaneous LE and BR/EDR to Same Device Capable (Controller), ")
                added++
            }

            // Check bit 4
            if (firstByte.toInt() and 16 == 16) {
                builder.append("Simultaneous LE and BR/EDR to Same Device Capable (Host), ")
                added++
            }

            //Remove ,_ at the end of String Builder
            if (added > 0 && builder.length >= 2) {
                builder.deleteCharAt(builder.length - 1)
                builder.deleteCharAt(builder.length - 1)
            }

            // Check bit 5-7
            /*if ((firstByte & 0b1110_0000) != 0) {
                builder.append("Reserved for future use, ");
            }*/
        } else {
            return ""
        }
        return builder.toString()
    }

    private fun getLERoleAsString(data: ByteArray): String {
        val builder = StringBuilder()
        if (data.size == 1) {
            val flag = data[0].toInt()
            when (flag) {
                0x00 -> {
                    builder.append("0x00")
                    builder.append(" (Only Peripheral Role supported)")
                }
                0x01 -> {
                    builder.append("0x01")
                    builder.append(" (Only Central Role supported)")
                }
                0x02 -> {
                    builder.append("0x02")
                    builder.append(" (Peripheral and Central Role supported, Peripheral Role preferred for connection establishment)")
                }
                0x03 -> {
                    builder.append("0x03")
                    builder.append(" (Peripheral and Central Role supported, Central Role preferred for connection establishment)")
                }
                else -> {
                    builder.append("0x").append(Converters.getHexValue(data[0]))
                    builder.append(" (Reserved for future use)")
                }
            }
        } else {
            return ""
        }
        return builder.toString()
    }

    private fun prepareSixOctetAddresses(data: ByteArray): String {
        val advertismentData = StringBuilder()
        val addresses = data.size / 6
        val invalidValues = data.size % 6
        var builder: StringBuilder
        for (i in 1..addresses) {
            builder = StringBuilder()
            for (j in i * 6 - 1 downTo (i - 1) * 6) {
                builder.append(Converters.getHexValue(data[j]))
                if (j % 6 != 0) {
                    builder.append(":")
                }
            }
            advertismentData.append(builder.toString())
            if (i < addresses) advertismentData.append(", ")
        }
        if (invalidValues > 0) {
            builder = StringBuilder()
            if (addresses > 0) {
                builder.append(", ").append(AD_STRING_INVALID_DATA).append(" ")
            } else {
                builder.append(AD_STRING_INVALID_DATA).append(" ")
            }
            var it = addresses * 6
            while (it < data.size) {
                builder.append(Converters.getHexValue(data[it]))
                it++
            }
            advertismentData.append(builder.toString())
        }
        return advertismentData.toString()
    }

    // Gets whole formatted advertise data
    fun getAdvertisements(scanRecord: ByteArray?): ArrayList<String?> {
        val advertData = ArrayList<String?>()
        var pos = 0
        var adData = getNextAdType(scanRecord, pos)
        while (adData != null && adData[0].toInt() != 0) {
            val data = getAdvertisementData(adData)
            advertData.add(data)
            pos += adData.size
            adData = getNextAdType(scanRecord, pos)
        }
        return advertData
    }

    fun getRawAdvertisingDate(scanRecord: ByteArray?): String {
        val stringBuilder = StringBuilder()
        var pos = 0
        var adData = getNextAdType(scanRecord, pos)
        while (adData != null && adData[0].toInt() != 0) {
            for (i in 0..adData[0]) {
                val value = Integer.toHexString(adData[i].toInt())
                when (value.length) {
                    2 -> stringBuilder.append(value)
                    1 -> {
                        stringBuilder.append("0")
                        stringBuilder.append(value)
                    }
                    else -> {
                        stringBuilder.append(value[value.length - 2])
                        stringBuilder.append(value[value.length - 1])
                    }
                }
            }
            pos += adData.size
            adData = getNextAdType(scanRecord, pos)
        }
        return stringBuilder.toString()
    }

    // Gets next advertise data for given start position
    private fun getNextAdType(data: ByteArray?, startPos: Int): ByteArray? {
        return if (data == null || startPos >= data.size) null else try {
            Arrays.copyOfRange(data, startPos, startPos + data[startPos] + 1)
        } catch (ex: Exception) {
            null
        }
    }
}
