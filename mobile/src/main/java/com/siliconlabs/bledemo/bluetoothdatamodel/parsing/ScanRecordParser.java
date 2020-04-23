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

import java.util.ArrayList;
import java.util.Arrays;

// ScanRecordParser - parses advertise data from BLE device
public class ScanRecordParser {
    private static final double TIME_INTERVAL_FACTOR = 1.25;
    private static final int AD_TYPE_FLAGS = 0x01;
    private static final int AD_TYPE_0x02 = 0x02;
    private static final int AD_TYPE_0x03 = 0x03;
    private static final int AD_TYPE_0x04 = 0x04;
    private static final int AD_TYPE_0x05 = 0x05;
    private static final int AD_TYPE_0x06 = 0x06;
    private static final int AD_TYPE_0x07 = 0x07;
    private static final int AD_TYPE_0x08 = 0x08;
    private static final int AD_TYPE_0x09 = 0x09;
    private static final int AD_TYPE_0x0A = 0x0A;
    private static final int AD_TYPE_0x0D = 0x0D;
    private static final int AD_TYPE_0x0E = 0x0E;
    private static final int AD_TYPE_0x0F = 0x0F;
    private static final int AD_TYPE_0x10 = 0x10;
    private static final int AD_TYPE_0x11 = 0x11;
    private static final int AD_TYPE_0x12 = 0x12;
    private static final int AD_TYPE_0x14 = 0x14;
    private static final int AD_TYPE_0x15 = 0x15;
    private static final int AD_TYPE_0x16 = 0x16;
    private static final int AD_TYPE_0x17 = 0x17;
    private static final int AD_TYPE_0x18 = 0x18;
    private static final int AD_TYPE_0x19 = 0x19;
    private static final int AD_TYPE_0x1A = 0x1A;
    private static final int AD_TYPE_0x1B = 0x1B;
    private static final int AD_TYPE_0x1C = 0x1C;
    private static final int AD_TYPE_0x1D = 0x1D;
    private static final int AD_TYPE_0x1E = 0x1E;
    private static final int AD_TYPE_0x1F = 0x1F;
    private static final int AD_TYPE_0x20 = 0x20;
    private static final int AD_TYPE_0x21 = 0x21;
    private static final int AD_TYPE_0x22 = 0x22;
    private static final int AD_TYPE_0x23 = 0x23;
    private static final int AD_TYPE_0x24 = 0x24;
    private static final int AD_TYPE_0x25 = 0x25;
    private static final int AD_TYPE_0x26 = 0x26;
    private static final int AD_TYPE_0x27 = 0x27;
    private static final int AD_TYPE_0x28 = 0x28;
    private static final int AD_TYPE_0x29 = 0x29;
    private static final int AD_TYPE_0x2A = 0x2A;
    private static final int AD_TYPE_0x2B = 0x2B;
    private static final int AD_TYPE_0x2C = 0x2C;
    private static final int AD_TYPE_0x2D = 0x2D;
    private static final int AD_TYPE_0x3D = 0x3D;
    private static final int AD_TYPE_0xFF = 0xFF;

    public static final String SPLIT = "&&";
    private static final String AD_STRING_NOT_SPECIFY = "not spec.";

    private static final String AD_STRING_PARSING_ERROR = "PARSING ERROR";
    private static final String AD_STRING_NO_DATA = "No data";
    private static final String AD_STRING_INVALID_DATA = "Invalid data:";


    // Gets single formatted advertise data
    public static String getAdvertismentData(byte[] advertismentDataBuffer) {

        byte len = advertismentDataBuffer[0];
        byte type;
        StringBuilder advertismentData;

        if (len == 0) {
            return null;
        }

        type = advertismentDataBuffer[1];

        byte[] data = Arrays.copyOfRange(advertismentDataBuffer, 2, advertismentDataBuffer.length);

        switch (type) {
            case AD_TYPE_FLAGS:
                advertismentData = new StringBuilder("Flags");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else {
                    advertismentData.append(parseFlag(data));
                }

                break;
            case AD_TYPE_0x02:
                advertismentData = new StringBuilder("Incomplete List of 16-bit Service Class UUIDs");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else if (data.length % 2 == 0) {
                    advertismentData.append(get16bitServiceUUIDsAsString(data));
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x03:
                advertismentData = new StringBuilder("Complete List of 16-bit Service Class UUIDs");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else if (data.length % 2 == 0) {
                    advertismentData.append(get16bitServiceUUIDsAsString(data));
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x04:
                advertismentData = new StringBuilder("Incomplete List of 32-bit Service Class UUIDs");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else if (data.length % 4 == 0) {
                    advertismentData.append(get32bitServiceUUIDsAsString(data));
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x05:
                advertismentData = new StringBuilder("Complete List of 32-bit Service Class UUIDs");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else if (data.length % 4 == 0) {
                    advertismentData.append(get32bitServiceUUIDsAsString(data));
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x06:
                advertismentData = new StringBuilder("Incomplete List of 128-bit Service Class UUIDs");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else if (data.length % 16 == 0) {
                    advertismentData.append(get128bitServiceUUIDsAsString(data));
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x07:
                advertismentData = new StringBuilder("Complete List of 128-bit Service Class UUIDs");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else if (data.length % 16 == 0) {
                    advertismentData.append(get128bitServiceUUIDsAsString(data));
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x08:
                advertismentData = new StringBuilder("Shortened Local Name");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else {
                    advertismentData.append(Converters.getAsciiValue(data));
                }

                break;
            case AD_TYPE_0x09:
                advertismentData = new StringBuilder("Complete Local Name");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else {
                    advertismentData.append(Converters.getAsciiValue(data));
                }

                break;
            case AD_TYPE_0x0A:
                advertismentData = new StringBuilder("TX Power Level");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else if (data.length == 1) {
                    advertismentData.append(data[0]).append(" dBm");
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x0D:
                advertismentData = new StringBuilder("Class Of Device");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else if (data.length == 3) {
                    advertismentData.append(getRawDataOctets0x__AsString(data));
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x0E:
                advertismentData = new StringBuilder("Simple Pairing Hash C/C-192");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else {
                    advertismentData.append(getRawDataOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x0F:
                advertismentData = new StringBuilder("Simple Pairing Randomizer R/R-192");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else {
                    advertismentData.append(getRawDataOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x10:
                advertismentData = new StringBuilder("Device ID / Security Manager TK Value");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else {
                    advertismentData.append(getRawDataOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x11:
                advertismentData = new StringBuilder("Security Manager Out of Band Flags");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else {
                    advertismentData.append(getRawDataOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x12:
                advertismentData = new StringBuilder("Slave Connection Interval Range");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else if (data.length == 4) {
                    advertismentData.append(getSlaveConnectionIntervalRangeAsString(data));
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x14:
                advertismentData = new StringBuilder("List of 16-bit Service Solicitation UUIDs");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else if (data.length % 2 == 0) {
                    advertismentData.append(get16bitServiceUUIDsAsString(data));
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x15:
                advertismentData = new StringBuilder("List of 128-bit Service Solicitation UUIDs");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else if (data.length % 16 == 0) {
                    advertismentData.append(get128bitServiceUUIDsAsString(data));
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x16:
                advertismentData = new StringBuilder("Service Data");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else if (data.length >= 2) {
                    advertismentData.append(get16bitUUIDServiceDataAsString(data));
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }

                break;
            case (byte) AD_TYPE_0xFF:
                advertismentData = new StringBuilder("Manufacturer Specific Data");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else if (data.length >= 2) {
                    advertismentData.append(getManufacturerSpecificData(data));
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }
                break;
            case AD_TYPE_0x17:
                advertismentData = new StringBuilder("Public Target Address");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else {
                    advertismentData.append(prepareSixOctetAddresses(data));
                }

                break;
            case AD_TYPE_0x18:
                advertismentData = new StringBuilder("Random Target Address");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else {
                    advertismentData.append(prepareSixOctetAddresses(data));
                }

                break;
            case AD_TYPE_0x19:
                advertismentData = new StringBuilder("Appearance");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else if (data.length == 2) {
                    advertismentData.append(getAppearanceAsString(data));
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x1A:
                advertismentData = new StringBuilder("Advertising Interval");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else if (data.length == 2) {
                    advertismentData.append(getAdvertisingIntervalAsString(data));
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x1B:
                advertismentData = new StringBuilder("LE Bluetooth Device Address");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else if (data.length == 7) {
                    advertismentData.append(getLEBluetoothDeviceAddressAsString(data));
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x1C:
                advertismentData = new StringBuilder("LE Role");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                    break;
                } else if (data.length == 1) {
                    advertismentData.append(getLERoleAsString(data));
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x1D:
                advertismentData = new StringBuilder("Simple Pairing Hash C-256");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else if (data.length == 16) {
                    advertismentData.append(getRawDataOctets0x__AsString(data));
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }
                break;
            case AD_TYPE_0x1E:
                advertismentData = new StringBuilder("Simple Pairing Randomizer R-256");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else if (data.length == 16) {
                    advertismentData.append(getRawDataOctets0x__AsString(data));
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }
                break;
            case AD_TYPE_0x1F:
                advertismentData = new StringBuilder("List of 32-bit Service Solicitation UUIDs");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else if (data.length % 4 == 0) {
                    advertismentData.append(get32bitServiceUUIDsAsString(data));
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x20:
                advertismentData = new StringBuilder("Service Data - 32-bit UUID");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else if (data.length >= 4) {
                    advertismentData.append(get32bitUUIDServiceDataAsString(data));
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x21:
                advertismentData = new StringBuilder("Service Data - 128-bit UUID");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else if (data.length >= 16) {
                    advertismentData.append(get128bitUUIDServiceDataAsString(data));
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x22:
                advertismentData = new StringBuilder("LE Secure Connections Confirmation Value");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else if (data.length == 16) {
                    advertismentData.append(getRawDataOctets0x__AsString(data));
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x23:
                advertismentData = new StringBuilder("LE Secure Connections Random Value");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else if (data.length == 16) {
                    advertismentData.append(getRawDataOctets0x__AsString(data));
                } else {
                    advertismentData.append(getParsingErrorOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x24:
                advertismentData = new StringBuilder("URI");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else {
                    advertismentData.append(getUriAsString(data));
                }

                break;
            case AD_TYPE_0x25:
                advertismentData = new StringBuilder("Indoor Positioning");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else {
                    advertismentData.append(getRawDataOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x26:
                advertismentData = new StringBuilder("Transport Discovery Data");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else {
                    advertismentData.append(getRawDataOctets0x__AsString(data));
                }

                break;
            case AD_TYPE_0x27:
                advertismentData = new StringBuilder("LE Supported Features");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else {
                    advertismentData.append(getRawDataOctets0x__AsString(data));
                }
                break;
            case AD_TYPE_0x28:
                advertismentData = new StringBuilder("Channel Map Update Indication");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else {
                    advertismentData.append(getRawDataOctets0x__AsString(data));
                }
                break;
            case AD_TYPE_0x29:
                advertismentData = new StringBuilder("PB-ADV");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else {
                    advertismentData.append(getRawDataOctets0x__AsString(data));
                }
                break;
            case AD_TYPE_0x2A:
                advertismentData = new StringBuilder("Mesh Message");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                    break;
                } else {
                    advertismentData.append(getRawDataOctets0x__AsString(data));
                }
                break;
            case AD_TYPE_0x2B:
                advertismentData = new StringBuilder("Mesh Beacon");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else {
                    advertismentData.append(getRawDataOctets0x__AsString(data));
                }
                break;
            case AD_TYPE_0x2C:
                advertismentData = new StringBuilder("BIGInfo");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else {
                    advertismentData.append(getRawDataOctets0x__AsString(data));
                }
                break;
            case AD_TYPE_0x2D:
                advertismentData = new StringBuilder("Broadcast_Code");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else {
                    advertismentData.append(getRawDataOctets0x__AsString(data));
                }
                break;
            case AD_TYPE_0x3D:
                advertismentData = new StringBuilder("3D Information Data");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else {
                    advertismentData.append(getRawDataOctets0x__AsString(data));
                }
                break;
            default:
                advertismentData = new StringBuilder("Unknown Type");
                advertismentData.append(SPLIT);

                if (data.length <= 0) {
                    advertismentData.append(AD_STRING_NO_DATA);
                } else {
                    advertismentData.append(getRawDataOctets0x__AsString(data));
                }
                break;
        }

        return advertismentData.toString();
    }


    private static String getSchemeNameStringFromValue(int value) {

        switch (value) {
            case 0x01:
                return "";
            case 0x02:
                return "aaa:";
            case 0x03:
                return "aaas:";
            case 0x04:
                return "about:";
            case 0x05:
                return "acap:";
            case 0x06:
                return "acct:";
            case 0x07:
                return "cap:";
            case 0x08:
                return "cid:";
            case 0x09:
                return "coap:";
            case 0x0A:
                return "coaps:";
            case 0x0B:
                return "crid:";
            case 0x0C:
                return "data:";
            case 0x0D:
                return "dav:";
            case 0x0E:
                return "dict:";
            case 0x0F:
                return "dns:";
            case 0x10:
                return "file:";
            case 0x11:
                return "ftp:";
            case 0x12:
                return "geo:";
            case 0x13:
                return "go:";
            case 0x14:
                return "gopher:";
            case 0x15:
                return "h323:";
            case 0x16:
                return "http:";
            case 0x17:
                return "https:";
            case 0x18:
                return "iax:";
            case 0x19:
                return "icap:";
            case 0x1A:
                return "im:";
            case 0x1B:
                return "imap:";
            case 0x1C:
                return "info:";
            case 0x1D:
                return "ipp:";
            case 0x1E:
                return "ipps:";
            case 0x1F:
                return "iris:";
            case 0x20:
                return "iris.beep:";
            case 0x21:
                return "iris.xpc:";
            case 0x22:
                return "iris.xpcs:";
            case 0x23:
                return "iris.lwz:";
            case 0x24:
                return "jabber:";
            case 0x25:
                return "ldap:";
            case 0x26:
                return "mailto:";
            case 0x27:
                return "mid:";
            case 0x28:
                return "msrp:";
            case 0x29:
                return "msrps:";
            case 0x2A:
                return "mtqp:";
            case 0x2B:
                return "mupdate:";
            case 0x2C:
                return "news:";
            case 0x2D:
                return "nfs:";
            case 0x2E:
                return "ni:";
            case 0x2F:
                return "nih:";
            case 0x30:
                return "nntp:";
            case 0x31:
                return "opaquelocktoken:";
            case 0x32:
                return "pop:";
            case 0x33:
                return "pres:";
            case 0x34:
                return "reload:";
            case 0x35:
                return "rtsp:";
            case 0x36:
                return "rtsps:";
            case 0x37:
                return "rtspu:";
            case 0x38:
                return "service:";
            case 0x39:
                return "session:";
            case 0x3A:
                return "shttp:";
            case 0x3B:
                return "sieve:";
            case 0x3C:
                return "sip:";
            case 0x3D:
                return "sips:";
            case 0x3E:
                return "sms:";
            case 0x3F:
                return "snmp:";
            case 0x40:
                return "soap.beep:";
            case 0x41:
                return "soap.beeps:";
            case 0x42:
                return "stun:";
            case 0x43:
                return "stuns:";
            case 0x44:
                return "tag:";
            case 0x45:
                return "tel:";
            case 0x46:
                return "telnet:";
            case 0x47:
                return "tftp:";
            case 0x48:
                return "thismessage:";
            case 0x49:
                return "tn3270:";
            case 0x4A:
                return "tip:";
            case 0x4B:
                return "turn:";
            case 0x4C:
                return "turns:";
            case 0x4D:
                return "tv:";
            case 0x4E:
                return "urn:";
            case 0x4F:
                return "vemmi:";
            case 0x50:
                return "ws:";
            case 0x51:
                return "wss:";
            case 0x52:
                return "xcon:";
            case 0x53:
                return "xcon-userid:";
            case 0x54:
                return "xmlrpc.beep:";
            case 0x55:
                return "xmlrpc.beeps:";
            case 0x56:
                return "xmpp:";
            case 0x57:
                return "z39.50r:";
            case 0x58:
                return "z39.50s:";
            case 0x59:
                return "acr:";
            case 0x5A:
                return "adiumxtra:";
            case 0x5B:
                return "afp:";
            case 0x5C:
                return "afs:";
            case 0x5D:
                return "aim:";
            case 0x5E:
                return "apt:";
            case 0x5F:
                return "attachment;";
            case 0x60:
                return "aw:";
            case 0x61:
                return "barion:";
            case 0x62:
                return "beshare:";
            case 0x63:
                return "bitcoin:";
            case 0x64:
                return "bolo:";
            case 0x65:
                return "callto:";
            case 0x66:
                return "chrome:";
            case 0x67:
                return "chrome-extension:";
            case 0x68:
                return "com-eventbrite-attendee:";
            case 0x69:
                return "content:";
            case 0x6A:
                return "cvs:";
            case 0x6B:
                return "dlna-playsingle:";
            case 0x6C:
                return "dlna-playcontainer:";
            case 0x6D:
                return "dtn:";
            case 0x6E:
                return "dvb:";
            case 0x6F:
                return "ed2k:";
            case 0x70:
                return "facetime:";
            case 0x71:
                return "feed:";
            case 0x72:
                return "feedready:";
            case 0x73:
                return "finger:";
            case 0x74:
                return "fish:";
            case 0x75:
                return "gg:";
            case 0x76:
                return "git:";
            case 0x77:
                return "gizmoproject:";
            case 0x78:
                return "gtalk:";
            case 0x79:
                return "ham:";
            case 0x7A:
                return "hcp:";
            case 0x7B:
                return "icon:";
            case 0x7C:
                return "ipc:";
            case 0x7D:
                return "irc:";
            case 0x7E:
                return "irc6:";
            case 0x7F:
                return "ircs:";
            case 0x80:
                return "itms:";
            case 0x81:
                return "jar:";
            case 0x82:
                return "jms:";
            case 0x83:
                return "keyparc;";
            case 0x84:
                return "lastfm:";
            case 0x85:
                return "ldaps:";
            case 0x86:
                return "magnet:";
            case 0x87:
                return "maps:";
            case 0x88:
                return "market:";
            case 0x89:
                return "message:";
            case 0x8A:
                return "mms:";
            case 0x8B:
                return "ms-help:";
            case 0x8C:
                return "ms-settings-power:";
            case 0x8D:
                return "msnim:";
            case 0x8E:
                return "mumble";
            case 0x8F:
                return "mvn:";
            case 0x90:
                return "notes:";
            case 0x91:
                return "oid:";
            case 0x92:
                return "palm:";
            case 0x93:
                return "paparazzi:";
            case 0x94:
                return "pkcs11:";
            case 0x95:
                return "platform:";
            case 0x96:
                return "proxy:";
            case 0x97:
                return "psyc:";
            case 0x98:
                return "query:";
            case 0x99:
                return "res:";
            case 0x9A:
                return "resource:";
            case 0x9B:
                return "rmi:";
            case 0x9C:
                return "rsync:";
            case 0x9D:
                return "rtmfp:";
            case 0x9E:
                return "rtmp:";
            case 0x9F:
                return "secondlife:";
            case 0xA0:
                return "sftp:";
            case 0xA1:
                return "sgn:";
            case 0xA2:
                return "skype:";
            case 0xA3:
                return "smb:";
            case 0xA4:
                return "smtp:";
            case 0xA5:
                return "soldat:";
            case 0xA6:
                return "spotify:";
            case 0xA7:
                return "ssh:";
            case 0xA8:
                return "steam:";
            case 0xA9:
                return "submit:";
            case 0xAA:
                return "svn:";
            case 0xAB:
                return "teamspeak:";
            case 0xAC:
                return "teliaeid:";
            case 0xAD:
                return "things:";
            case 0xAE:
                return "udp:";
            case 0xAF:
                return "unreal:";
            case 0xB0:
                return "ut2004:";
            case 0xB1:
                return "ventrilo:";
            case 0xB2:
                return "view-source:";
            case 0xB3:
                return "webcal:";
            case 0xB4:
                return "wtai:";
            case 0xB5:
                return "wyciwyg:";
            case 0xB6:
                return "xfire:";
            case 0xB7:
                return "xri:";
            case 0xB8:
                return "ymsgr:";
            case 0xB9:
                return "example:";
            case 0xBA:
                return "ms-settings-cloudstorage:";
            default:
                return "[unknown scheme (00" + Converters.getHexValue((byte) value) + ")]:";
        }
    }

    private static String getAppearanceValueDescriptionFromValue(int value) {

        switch (value) {
            case 64:
                return "Generic Phone (Generic category)";
            case 128:
                return "Generic Computer (Generic category)";
            case 192:
                return "Generic Watch (Generic category)";
            case 193:
                return "Watch: Sports Watch (Watch subtype)";
            case 256:
                return "Generic Clock (Generic category)";
            case 320:
                return "Generic Display (Generic category)";
            case 384:
                return "Generic Remote Control (Generic category)";
            case 448:
                return "Generic Eye-glasses (Generic category)";
            case 512:
                return "Generic Tag (Generic category)";
            case 576:
                return "Generic Keyring (Generic category)";
            case 640:
                return "Generic Media Player (Generic category)";
            case 704:
                return "Generic Barcode Scanner (Generic category)";
            case 768:
                return "Generic Thermometer (Generic category)";
            case 769:
                return "Thermometer: Ear (Thermometer subtype)";
            case 832:
                return "Generic Heart rate Sensor (Generic category)";
            case 833:
                return "Heart Rate Sensor: Heart Rate Belt (Heart Rate Sensor subtype)";
            case 896:
                return "Generic Blood Pressure (Generic category)";
            case 897:
                return "Blood Pressure: Arm (Blood Pressure subtype)";
            case 898:
                return "Blood Pressure: Wrist (Blood Pressure subtype)";
            case 960:
                return "Human Interface Device (HID) (HID Generic)";
            case 961:
                return "Keyboard (HID subtype)";
            case 962:
                return "Mouse (HID subtype)";
            case 963:
                return "Joystick (HID subtype)";
            case 964:
                return "Gamepad (HID subtype)";
            case 965:
                return "Digitizer Tablet (HID subtype)";
            case 966:
                return "Card Reader (HID subtype)";
            case 967:
                return "Digital Pen (HID subtype)";
            case 968:
                return "Barcode Scanner (HID subtype)";
            case 1024:
                return "Generic Glucose Meter (Generic category)";
            case 1088:
                return "Generic Running Walking Sensor (Generic category)";
            case 1089:
                return "Running Walking Sensor: In-Shoe (Running Walking Sensor subtype)";
            case 1090:
                return "Running Walking Sensor: On-Shoe (Running Walking Sensor subtype)";
            case 1091:
                return "Running Walking Sensor: On-Hip (Running Walking Sensor subtype)";
            case 1152:
                return "Generic: Cycling (Generic category)";
            case 1153:
                return "Cycling: Cycling Computer (Cycling subtype)";
            case 1154:
                return "Cycling: Speed Sensor (Cycling subtype)";
            case 1155:
                return "Cycling: Cadence Sensor (Cycling subtype)";
            case 1156:
                return "Cycling: Power Sensor (Cycling subtype)";
            case 1157:
                return "Cycling: Speed and Cadence Sensor (Cycling subtype)";
            case 3136:
                return "Generic: Pulse Oximeter (Pulse Oximeter Generic Category)";
            case 3137:
                return "Fingertip (Pulse Oximeter subtype)";
            case 3138:
                return "Wrist Worn (Pulse Oximeter subtype)";
            case 3200:
                return "Generic: Weight Scale (Weight Scale Generic Category)";
            case 3264:
                return "Generic Personal Mobility Device (Personal Mobility Device)";
            case 3265:
                return "Powered Wheelchair (Personal Mobility Device)";
            case 3266:
                return "Mobility Scooter (Personal Mobility Device)";
            case 3328:
                return "Generic Continuous Glucose Monitor (Continuous Glucose Monitor)";
            case 3392:
                return "Generic Insulin Pump (Insulin Pump)";
            case 3393:
                return "Insulin Pump, durable pump (Insulin Pump)";
            case 3396:
                return "Insulin Pump, patch pump (Insulin Pump)";
            case 3400:
                return "Insulin Pen (Insulin Pump)";
            case 3456:
                return "Generic Medication Delivery (Medication Delivery)";
            case 5184:
                return "Generic: Outdoor Sports Activity (Outdoor Sports Activity Generic Category)";
            case 5185:
                return "Location Display Device (Outdoor Sports Activity subtype)";
            case 5186:
                return "Location and Navigation Display Device (Outdoor Sports Activity subtype)";
            case 5187:
                return "Location Pod (Outdoor Sports Activity subtype)";
            case 5188:
                return "Location and Navigation Pod (Outdoor Sports Activity subtype)";
            default:
                return "Unknown";
        }
    }

    private static String getRawDataOctets0x__AsString(byte[] data) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < data.length; i++) {
            builder.append("0x").append(Converters.getHexValue(data[i]));
            if (i != data.length - 1) builder.append(", ");
        }
        return builder.toString();
    }

    private static String get16bitServiceUUIDsAsString(byte[] data) {
        StringBuilder builder = new StringBuilder();
        int addressess = data.length / 2;

        if (data.length % 2 != 0) return "";

        for (int i = 1; i <= addressess; i++) {
            builder.append("0x");
            for (int j = (i * 2) - 1; j >= (i - 1) * 2; j--) {
                builder.append(Converters.getHexValue(data[j]));
            }
            if (i != addressess) builder.append(", ");
        }
        return builder.toString();
    }

    private static String get128bitServiceUUIDsAsString(byte[] data) {
        StringBuilder builder = new StringBuilder();
        int addressess = data.length / 16;

        if (data.length % 16 != 0) return "";

        for (int i = 1; i <= addressess; i++) {
            int lineCounter = 0;
            for (int j = (i * 16) - 1; j >= (i - 1) * 16; j--) {
                lineCounter++;
                builder.append(Converters.getHexValue(data[j]).toLowerCase());
                if (lineCounter == 4 || lineCounter == 6 || lineCounter == 8 || lineCounter == 10)
                    builder.append("-");
            }
            if (i != addressess) builder.append(", ");
        }

        return builder.toString();
    }

    private static String get32bitServiceUUIDsAsString(byte[] data) {
        StringBuilder builder = new StringBuilder();
        int addressess = data.length / 4;

        if (data.length % 4 != 0) return "";

        for (int i = 1; i <= addressess; i++) {
            builder.append("0x");
            for (int j = (i * 4) - 1; j >= (i - 1) * 4; j--) {
                builder.append(Converters.getHexValue(data[j]));
            }
            if (i != addressess) builder.append(", ");
        }
        return builder.toString();
    }

    private static String get128bitUUIDServiceDataAsString(byte[] data) {
        StringBuilder builder = new StringBuilder();

        if (data.length >= 16) {
            builder.append("UUID: ");
            for (int i = 15; i >= 0; i--) {
                builder.append(Converters.getHexValue(data[i]).toLowerCase());
                if (i == 6 || i == 8 || i == 10 || i == 12) builder.append("-");
            }
            builder.append(" Data: ");
            if (data.length > 16) {
                builder.append("0x");
                for (int i = 16; i < data.length; i++) {
                    builder.append(Converters.getHexValue(data[i]));
                }
            }
        } else {
            return "";
        }
        return builder.toString();
    }

    private static String get32bitUUIDServiceDataAsString(byte[] data) {
        StringBuilder builder = new StringBuilder();

        if (data.length >= 4) {
            builder.append("UUID: 0x");
            for (int i = 3; i >= 0; i--) {
                builder.append(Converters.getHexValue(data[i]));
            }
            builder.append(" Data: ");
            if (data.length > 4) {
                builder.append("0x");
                for (int i = 4; i < data.length; i++) {
                    builder.append(Converters.getHexValue(data[i]));
                }
            }
        } else {
            return "";
        }
        return builder.toString();
    }

    private static String get16bitUUIDServiceDataAsString(byte[] data) {
        StringBuilder builder = new StringBuilder();

        if (data.length >= 2) {
            builder.append("UUID: 0x");
            for (int i = 1; i >= 0; i--) {
                builder.append(Converters.getHexValue(data[i]));
            }
            builder.append(" Data: ");
            if (data.length > 2) {
                builder.append("0x");
                for (int i = 2; i < data.length; i++) {
                    builder.append(Converters.getHexValue(data[i]));
                }
            }
        } else {
            return "";
        }
        return builder.toString();
    }

    private static String getParsingErrorOctets0x__AsString(byte[] data) {
        StringBuilder builder = new StringBuilder();

        builder.append(AD_STRING_PARSING_ERROR).append(": ");
        for (int i = 0; i < data.length; i++) {
            builder.append("0x").append(Converters.getHexValue(data[i]));
            if (i != data.length - 1) builder.append(", ");
        }
        return builder.toString();
    }

    private static String getUriAsString(byte[] data) {
        StringBuilder builder = new StringBuilder();

        if (data.length >= 1) {
            builder.append(getSchemeNameStringFromValue(Integer.parseInt(Converters.getHexValue(data[0]), 16))).append(" ");
            if (data.length > 1) {
                for (int i = 1; i < data.length; i++) {
                    builder.append((char) data[i]);
                }
            }
        }
        return builder.toString();
    }

    private static String getLEBluetoothDeviceAddressAsString(byte[] data) {
        StringBuilder builder = new StringBuilder();

        if (data.length == 7) {
            for (int i = 6; i > 0; i--) {
                builder.append(Converters.getHexValue(data[i]));
                if (i != 1) builder.append(":");
            }

            byte flag = (byte) (data[0] & (byte) 0x01);
            if (flag == 0) {
                builder.append(" (Public Device Address)");
            } else {
                builder.append(" (Random Device Address)");
            }
        } else {
            return "";
        }
        return builder.toString();
    }

    private static String getManufacturerSpecificData(byte[] data) {
        StringBuilder builder = new StringBuilder();

        if (data.length >= 2) {
            builder.append("Company Code: 0x").append(Converters.getHexValue(data[1])).append(Converters.getHexValue(data[0])).append("").append("<br/>");
            builder.append("Data: ");

            if (data.length > 2) {
                builder.append("0x");
                for (int i = 2; i < data.length; i++) {
                    builder.append(Converters.getHexValue(data[i]));
                }
            }

        } else {
            return "";
        }
        return builder.toString();
    }

    private static String getAppearanceAsString(byte[] data) {
        StringBuilder builder = new StringBuilder();

        if (data.length == 2) {
            String hexValue = Converters.getHexValue(data[1]) + Converters.getHexValue(data[0]);
            builder.append("0x").append(hexValue);
            builder.append(" [").append(Integer.parseInt(hexValue, 16)).append("]");
            builder.append(" ").append(getAppearanceValueDescriptionFromValue(Integer.parseInt(hexValue, 16)));
        } else {
            return "";
        }
        return builder.toString();
    }

    private static String getAdvertisingIntervalAsString(byte[] data) {
        StringBuilder builder = new StringBuilder();

        if (data.length == 2) {
            String hexValue = Converters.getHexValue(data[1]) + Converters.getHexValue(data[0]);
            double ms = 0.625 * Integer.parseInt(hexValue, 16);
            builder.append(ms).append(" ms");
        } else {
            return "";
        }

        return builder.toString();
    }

    private static String getSlaveConnectionIntervalRangeAsString(byte[] data) {
        StringBuilder builder = new StringBuilder();

        if (data.length == 4) {
            float min = Integer.parseInt(Converters.getHexValue(data[1]) + Converters.getHexValue(data[0]), 16);
            if (min == 0xffff) {
                builder.append(AD_STRING_NOT_SPECIFY);
            } else if (0x0006 <= min && min <= 0x0C80) {
                min *= TIME_INTERVAL_FACTOR;
                builder.append(min).append("ms").append(" - ");
            } else {
                builder.append("(0x").append(Converters.getHexValue(data[1])).append(Converters.getHexValue(data[0])).append(") OUT OF RANGE");
                builder.append(" - ");
            }
            float max = Integer.parseInt(Converters.getHexValue(data[3]) + Converters.getHexValue(data[2]), 16);
            if (max == 0xffff) {
                builder.append(AD_STRING_NOT_SPECIFY);
            } else if (0x0006 <= max && max <= 0x0C80) {
                max *= TIME_INTERVAL_FACTOR;
                builder.append(max);
                builder.append("ms");
            } else {
                builder.append("(0x").append(Converters.getHexValue(data[3])).append(Converters.getHexValue(data[2])).append(") OUT OF RANGE");
            }

        } else {
            return "";
        }

        return builder.toString();
    }

    private static String parseFlag(byte[] data) {

        StringBuilder builder = new StringBuilder();

        if (data.length > 0) {

            builder.append("0x");
            for (int i = data.length - 1; i >= 0; i--) {
                builder.append(Converters.getHexValue(data[i]));
            }
            builder.append(": ");

            byte firstByte = data[0];
            int added = 0;

            // Check bit 0
            if ((firstByte & 0b0000_0001) == 1) {
                builder.append("LE Limited Discoverable Mode, ");
                added++;
            }

            // Check bit 1
            if ((firstByte & 0b0000_0010) == 2) {
                builder.append("LE General Discoverable Mode, ");
                added++;
            }

            // Check bit 2
            if ((firstByte & 0b0000_0100) == 4) {
                builder.append("BR/EDR Not Supported, ");
                added++;
            }

            // Check bit 3
            if ((firstByte & 0b0000_1000) == 8) {
                builder.append("Simultaneous LE and BR/EDR to Same Device Capable (Controller), ");
                added++;
            }

            // Check bit 4
            if ((firstByte & 0b0001_0000) == 16) {
                builder.append("Simultaneous LE and BR/EDR to Same Device Capable (Host), ");
                added++;
            }

            //Remove ,_ at the end of String Builder
            if (added > 0 && builder.length() >= 2) {
                builder.deleteCharAt(builder.length() - 1);
                builder.deleteCharAt(builder.length() - 1);
            }

            // Check bit 5-7
            /*if ((firstByte & 0b1110_0000) != 0) {
                builder.append("Reserved for future use, ");
            }*/

        } else {
            return "";
        }

        return builder.toString();

    }

    private static String getLERoleAsString(byte[] data) {
        StringBuilder builder = new StringBuilder();

        if (data.length == 1) {
            int flag = data[0];

            switch (flag) {
                case 0x00:
                    builder.append("0x00");
                    builder.append(" (Only Peripheral Role supported)");
                    break;
                case 0x01:
                    builder.append("0x01");
                    builder.append(" (Only Central Role supported)");
                    break;
                case 0x02:
                    builder.append("0x02");
                    builder.append(" (Peripheral and Central Role supported, Peripheral Role preferred for connection establishment)");
                    break;
                case 0x03:
                    builder.append("0x03");
                    builder.append(" (Peripheral and Central Role supported, Central Role preferred for connection establishment)");
                    break;
                default:
                    builder.append("0x").append(Converters.getHexValue(data[0]));
                    builder.append(" (Reserved for future use)");
                    break;
            }
        } else {
            return "";
        }

        return builder.toString();
    }


    private static String prepareSixOctetAddresses(byte[] data) {
        StringBuilder advertismentData = new StringBuilder();

        int addresses = data.length / 6;
        int invalidValues = data.length % 6;
        StringBuilder builder;

        for (int i = 1; i <= addresses; i++) {
            builder = new StringBuilder();
            for (int j = (i * 6) - 1; j >= (i - 1) * 6; j--) {
                builder.append(Converters.getHexValue(data[j]));
                if (j % 6 != 0) {
                    builder.append(":");
                }
            }
            advertismentData.append(builder.toString());
            if (i < addresses) advertismentData.append(", ");
        }

        if (invalidValues > 0) {
            builder = new StringBuilder();
            if (addresses > 0) {
                builder.append(", ").append(AD_STRING_INVALID_DATA).append(" ");
            } else {
                builder.append(AD_STRING_INVALID_DATA).append(" ");
            }

            int it = addresses * 6;

            while (it < data.length) {
                builder.append(Converters.getHexValue(data[it]));
                it++;
            }
            advertismentData.append(builder.toString());
        }
        return advertismentData.toString();
    }

    // Gets whole formatted advertise data
    public static ArrayList<String> getAdvertisements(byte[] scanRecord) {
        ArrayList<String> advertData = new ArrayList<>();
        int pos = 0;
        byte[] adData = getNextAdType(scanRecord, pos);
        while (adData != null && adData[0] != 0) {
            String data = getAdvertismentData(adData);
            advertData.add(data);
            pos += adData.length;
            adData = getNextAdType(scanRecord, pos);
        }
        return advertData;
    }

    public static String getRawAdvertisingDate(byte[] scanRecord) {
        StringBuilder stringBuilder = new StringBuilder();
        int pos = 0;
        byte[] adData = getNextAdType(scanRecord, pos);

        while (adData != null && adData[0] != 0) {

            for (int i = 0; i <= adData[0]; i++) {

                String value = Integer.toHexString((int) adData[i]);

                if (value.length() == 2) {
                    stringBuilder.append(value);
                } else if (value.length() == 1) {
                    stringBuilder.append("0");
                    stringBuilder.append(value);
                } else {
                    stringBuilder.append(value.charAt(value.length() - 2));
                    stringBuilder.append(value.charAt(value.length() - 1));
                }
            }
            pos += adData.length;
            adData = getNextAdType(scanRecord, pos);
        }
        return stringBuilder.toString();
    }

    // Gets next advertise data for given start position
    public static byte[] getNextAdType(byte[] data, int startPos) {
        if (startPos >= data.length) return null;
        try {
            return Arrays.copyOfRange(data, startPos, startPos + data[startPos] + 1);
        } catch (ArrayIndexOutOfBoundsException | NullPointerException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
