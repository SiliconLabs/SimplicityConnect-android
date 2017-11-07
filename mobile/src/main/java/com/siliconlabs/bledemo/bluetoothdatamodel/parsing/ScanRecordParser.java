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
    private static final int AD_TYPE_SHORT_16_SERVICE = 0x02;
    private static final int AD_TYPE_COMPLETE_16_SERVICE = 0x03;
    private static final int AD_TYPE_SHORT_32_SERVICE = 0x04;
    private static final int AD_TYPE_COMPLETE_32_SERVICE = 0x05;
    private static final int AD_TYPE_SHORT_128_SERVICE = 0x06;
    private static final int AD_TYPE_COMPLETE_128_SERVICE = 0x07;
    private static final int AD_TYPE_SHORT_LOCAL_NAME = 0x08;
    private static final int AD_TYPE_COMPLETE_LOCAL_NAME = 0x09;
    private static final int AD_TYPE_TX_POWER = 0x0A;
    private static final int AD_TYPE_CLASS_OF_DEVICE = 0x0D;
    private static final int AD_TYPE_SIMPLE_PAIRING_HASH = 0x0E;
    private static final int AD_TYPE_SIMPLE_PAIRING_RANDOMIZER = 0x0F;
    private static final int AD_TYPE_TK_VALUE = 0x10;
    private static final int AD_TYPE_SECURITY_MANAGER_FLAGS = 0x11;
    private static final int AD_TYPE_SLAVE_CONNECTION_INTERVAL = 0x12;
    private static final int AD_TYPE_SERVICE_SOLICATION_16 = 0x14;
    private static final int AD_TYPE_SERVICE_SOLICATION_128 = 0x15;
    private static final int AD_TYPE_SERVICE_DATA = 0x16;
    private static final int AD_TYPE_MANUFACTURER_DATA = 0xFF;

    private static final String AD_STRING_SHORTENED = "Shortened";
    private static final String AD_STRING_COMPLETE = "Complete";
    private static final String AD_STRING_NOT_SPECIFY = "not spec.";
    private static final String AD_STRING_FLAGS = "Flags: %s";
    private static final String AD_STRING_SERVICE = "%s service classes(%d-bit): ";
    private static final String AD_STRING_LOCAL_NAME = "%s local name: %s";
    private static final String AD_STRING_TX_POWER = "TX power level: %s";
    private static final String AD_STRING_CLASS_DEVICE = "Class of device: %s";
    private static final String AD_STRING_SIMPLE_PAIRING_HASH = "Simple Pairing Hash C: %s";
    private static final String AD_STRING_SIMPLE_PAIRING_RANDOMIZER = "Simple Pairing Randomizer R: %s";
    private static final String AD_STRING_TK_VALUE = "Security Manager TK Value: ";
    private static final String AD_STRING_SECURITY_MANAGER_FLAGS = "Security Manager OOB Flags: %s";
    private static final String AD_STRING_SLAVE_CONNECTION_INTERVAL = "Connection interval range: ";
    private static final String AD_STRING_SERVICE_DATA = "Service: 0x%s:";
    private static final String AD_STRING_COMPANY_DATA = "Company Code: 0x%s, 0x%s:";
    private static final String AD_STRING_UNKNOWN_TYPE = "Unknown type 0x%02x with %d bytes data: %s";

    // Gets single formatted advertise data
    public static String getAdvertismentData(byte[] advertismentDataBuffer) {

        byte len = advertismentDataBuffer[0];
        byte type;
        String advertismentData = "";

        if (len == 0) {
            return null;
        }

        type = advertismentDataBuffer[1];

        byte[] data = Arrays.copyOfRange(advertismentDataBuffer, 2, advertismentDataBuffer.length);
        len -= 1;

        switch (type) {
            case AD_TYPE_FLAGS:
                advertismentData = String.format(AD_STRING_FLAGS, Converters.getHexValue(data));
                break;
            case AD_TYPE_SHORT_16_SERVICE:
            case AD_TYPE_COMPLETE_16_SERVICE:
            case AD_TYPE_SHORT_32_SERVICE:
            case AD_TYPE_COMPLETE_32_SERVICE:
            case AD_TYPE_SHORT_128_SERVICE:
            case AD_TYPE_COMPLETE_128_SERVICE:
                int format = (type == AD_TYPE_SHORT_16_SERVICE || type == AD_TYPE_COMPLETE_16_SERVICE) ? 2
                        : (type == AD_TYPE_SHORT_32_SERVICE || type == AD_TYPE_COMPLETE_32_SERVICE) ? 4 : 16;
                advertismentData = String.format(AD_STRING_SERVICE, type % 2 == 0 ? AD_STRING_SHORTENED
                        : AD_STRING_COMPLETE, format * 8);
                for (int i = 0; i < len / format; i++) {
                    for (int j = format - 1; j >= 0; j--) {
                        advertismentData += Converters.getHexValue(data[format * i + j]);
                    }
                    advertismentData += ", ";
                }
                advertismentData = advertismentData.substring(0, advertismentData.length() - 2);
                break;
            case AD_TYPE_SHORT_LOCAL_NAME:
            case AD_TYPE_COMPLETE_LOCAL_NAME:
                advertismentData = String.format(AD_STRING_LOCAL_NAME,
                        type == AD_TYPE_SHORT_LOCAL_NAME ? AD_STRING_SHORTENED : AD_STRING_COMPLETE, Converters
                                .getAsciiValue(data));
                break;
            case AD_TYPE_TX_POWER:
                advertismentData = String.format(AD_STRING_TX_POWER, Converters.getDecimalValueFromTwosComplement(data[0])) + " dBm";
                break;
            case AD_TYPE_CLASS_OF_DEVICE:
                advertismentData = String.format(AD_STRING_CLASS_DEVICE, Converters.getHexValue(data));
                break;
            case AD_TYPE_SIMPLE_PAIRING_HASH:
                advertismentData = String.format(AD_STRING_SIMPLE_PAIRING_HASH, Converters.getHexValue(data));
                break;
            case AD_TYPE_SIMPLE_PAIRING_RANDOMIZER:
                advertismentData = String.format(AD_STRING_SIMPLE_PAIRING_RANDOMIZER, Converters.getHexValue(data));
                break;
            case AD_TYPE_TK_VALUE:
                advertismentData = AD_STRING_TK_VALUE;
                for (int i = len - 2; i >= 0; i--) {
                    advertismentData += Converters.getHexValue(data[i]);
                }
                break;
            case AD_TYPE_SECURITY_MANAGER_FLAGS:
                advertismentData = String.format(AD_STRING_SECURITY_MANAGER_FLAGS, Converters.getHexValue(data));
                break;
            case AD_TYPE_SLAVE_CONNECTION_INTERVAL:
                advertismentData = AD_STRING_SLAVE_CONNECTION_INTERVAL;
                float min = Integer.parseInt(Converters.getHexValue(data[1]) + Converters.getHexValue(data[0]), 16);
                if (min == 0xffff) {
                    advertismentData += AD_STRING_NOT_SPECIFY;
                } else {
                    min *= TIME_INTERVAL_FACTOR;
                    advertismentData += min + "-";
                }
                float max = Integer.parseInt(Converters.getHexValue(data[3]) + Converters.getHexValue(data[2]), 16);
                if (max == 0xffff) {
                    advertismentData += AD_STRING_NOT_SPECIFY;
                } else {
                    max *= TIME_INTERVAL_FACTOR;
                    advertismentData += max;
                }
                advertismentData += "ms";
                break;
            case AD_TYPE_SERVICE_SOLICATION_16:
            case AD_TYPE_SERVICE_SOLICATION_128:
                advertismentData = String.format("Service solication(%d-bit): ", type == AD_TYPE_SERVICE_SOLICATION_16 ? 16
                        : 128);
                format = type == AD_TYPE_SERVICE_SOLICATION_16 ? 2 : 16;
                for (int i = 0; i < len / format; i++) {
                    for (int j = format - 1; j >= 0; j--) {
                        advertismentData += Converters.getHexValue(data[format * i + j]);
                    }
                    advertismentData += ", ";
                }
                advertismentData = advertismentData.substring(0, advertismentData.length() - 2);
                break;
            case AD_TYPE_SERVICE_DATA:
                advertismentData = String.format(AD_STRING_SERVICE_DATA, Converters.getHexValue(data[0])
                        + Converters.getHexValue(data[1]));
                for (int i = 0; i < len; i++) {
                    advertismentData += Converters.getHexValue(data[i]);
                }
                break;
            case (byte) AD_TYPE_MANUFACTURER_DATA:
                advertismentData = String.format(AD_STRING_COMPANY_DATA, Converters.getHexValue(data[1])
                        , Converters.getHexValue(data[0]));
                for (int i = 0; i < len; i++) {
                    advertismentData += Converters.getHexValue(data[i]);
                }
                break;
            default:
                advertismentData = String.format(AD_STRING_UNKNOWN_TYPE, type, len, Converters.getHexValue(data));
                break;
        }

        return advertismentData;
    }

    // Gets whole formatted advertise data
    public static ArrayList<String> getAdvertisements(byte[] scanRecord) {
        ArrayList<String> advertData = new ArrayList<String>();
        int pos = 0;
        byte[] adData = getNextAdType(scanRecord, pos);
        while (adData[0] != 0) {
            String data = getAdvertismentData(adData);
            advertData.add(data);
            pos += adData.length;
            adData = getNextAdType(scanRecord, pos);
        }
        return advertData;
    }

    // Gets next advertise data for given start position
    public static byte[] getNextAdType(byte[] data, int startPos) {
        return Arrays.copyOfRange(data, startPos, startPos + data[startPos] + 1);
    }
}
