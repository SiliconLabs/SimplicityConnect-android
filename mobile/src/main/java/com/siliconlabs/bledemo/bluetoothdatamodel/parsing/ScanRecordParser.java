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
    private static final String HEX_PREFIX = "0x";
    private static final String AD_STRING_SHORTENED = "Shortened";
    private static final String AD_STRING_COMPLETE = "Complete";
    private static final String AD_STRING_NOT_SPECIFY = "not spec.";
    private static final String AD_STRING_FLAGS = "Flags" + SPLIT + "%s";
    private static final String AD_STRING_LOCAL_NAME = "%s local name" + SPLIT + "%s";
    private static final String AD_STRING_TX_POWER = "TX power level" + SPLIT + "%s";
    private static final String AD_STRING_CLASS_DEVICE = "Class of device" + SPLIT + "%s";
    private static final String AD_STRING_SIMPLE_PAIRING_HASH = "Simple Pairing Hash C" + SPLIT + "%s";
    private static final String AD_STRING_SIMPLE_PAIRING_RANDOMIZER = "Simple Pairing Randomizer R" + SPLIT + "%s";
    private static final String AD_STRING_TK_VALUE = "Security Manager TK Value" + SPLIT;
    private static final String AD_STRING_SECURITY_MANAGER_FLAGS = "Security Manager  Out of Band Flags" + SPLIT + "%s";
    private static final String AD_STRING_SLAVE_CONNECTION_INTERVAL = "Slave connection interval range" + SPLIT;
    private static final String AD_STRING_SERVICE_DATA = "Service Data %s" + SPLIT;
    private static final String AD_STRING_COMPANY_DATA = "Company Code " + HEX_PREFIX + "%s, " + HEX_PREFIX + "%s" + SPLIT;
    private static final String AD_STRING_UNKNOWN_TYPE = "Unknown type 0x%02x with %d bytes data" + SPLIT + "%s";

    private static final String AD_STRING_COMPANY_PARSING_ERROR = "Company Code" + SPLIT + " PARSING ERROR: ";

    private static String parseFlag(char[] charArray) {
        StringBuilder desc = new StringBuilder();
        for (int i = 0; i < charArray.length; i++) {
            if (charArray[i] != '1') continue;
            switch (i) {
                case 0:
                    desc.append("LE Limited Discoverable Mode, ");
                    break;
                case 1:
                    desc.append("LE General Discoverable Mode, ");
                    break;
                case 2:
                    desc.append("BR/EDR Not Supported, ");
                    break;
                case 3:
                    desc.append("Simultaneous LE and BR/EDR to Same Device Capable (Controller), ");
                    break;
                case 4:
                    desc.append("Simultaneous LE and BR/EDR to Same Device Capable (Host), ");
                    break;
                case 5:
                case 6:
                case 7:
                    desc.append("Reserved for future use, ");
                    break;
            }
        }
        if (desc.toString().endsWith(", ")) {
            desc = new StringBuilder(desc.substring(0, desc.length() - 2));
        }
        return desc.toString();
    }

    static char[] reverse(char[] a, int n) {
        char[] b = new char[n];
        int j = n;
        for (int i = 0; i < n; i++) {
            b[j - 1] = a[i];
            j = j - 1;
        }
        return b;
    }

    // Gets single formatted advertise data
    public static String getAdvertismentData(byte[] advertismentDataBuffer) {

        byte len = advertismentDataBuffer[0];
        byte type;
        StringBuilder advertismentData = new StringBuilder();

        if (len == 0) {
            return null;
        }

        type = advertismentDataBuffer[1];

        byte[] data = Arrays.copyOfRange(advertismentDataBuffer, 2, advertismentDataBuffer.length);
        len -= 1;

        switch (type) {
            case AD_TYPE_FLAGS:
                String hex = Converters.getHexValue(data);
                hex = hex.replaceAll("\\s+", "");
                String binaryString = Integer.toBinaryString(Integer.parseInt(hex, 16));
                char[] charArray = binaryString.toCharArray();
                char[] reverseCharArray = reverse(charArray, charArray.length);
                advertismentData = new StringBuilder(String.format(AD_STRING_FLAGS, HEX_PREFIX + hex + ": " + parseFlag(reverseCharArray)));
                break;

            case AD_TYPE_0x02:
            case AD_TYPE_0x03:
            case AD_TYPE_0x04:
            case AD_TYPE_0x05:
            case AD_TYPE_0x06:
            case AD_TYPE_0x07:
                switch (type) {
                    case AD_TYPE_0x02:
                        advertismentData = new StringBuilder("Incomplete List of 16-bit Service Class UUIDs");
                        break;
                    case AD_TYPE_0x03:
                        advertismentData = new StringBuilder("Complete List of 16-bit Service Class UUIDs");
                        break;
                    case AD_TYPE_0x04:
                        advertismentData = new StringBuilder("Incomplete List of 32-bit Service Class UUIDs");
                        break;
                    case AD_TYPE_0x05:
                        advertismentData = new StringBuilder("Complete List of 32-bit Service Class UUIDs");
                        break;
                    case AD_TYPE_0x06:
                        advertismentData = new StringBuilder("Incomplete List of 128-bit Service Class UUIDs");
                        break;
                    case AD_TYPE_0x07:
                        advertismentData = new StringBuilder("Complete List of 128-bit Service Class UUIDs");
                }
                advertismentData.append(SPLIT);

                int format = (type == AD_TYPE_0x02 || type == AD_TYPE_0x03) ? 2
                        : (type == AD_TYPE_0x04 || type == AD_TYPE_0x05) ? 4 : 16;
                for (int i = 0; i < len / format; i++) {
                    for (int j = format - 1; j >= 0; j--) {
                        advertismentData.append(HEX_PREFIX).append(Converters.getHexValue(data[format * i + j]));
                        advertismentData.append(", ");
                    }
                    advertismentData = new StringBuilder(advertismentData.substring(0, advertismentData.length() - 2));
                    advertismentData.append(", ");
                }
                advertismentData = new StringBuilder(advertismentData.substring(0, advertismentData.length() - 2));
                break;
            case AD_TYPE_0x08:
            case AD_TYPE_0x09:
                advertismentData = new StringBuilder(String.format(AD_STRING_LOCAL_NAME,
                        type == AD_TYPE_0x08 ? AD_STRING_SHORTENED : AD_STRING_COMPLETE, Converters
                                .getAsciiValue(data)));
                break;
            case AD_TYPE_0x0A:
                advertismentData = new StringBuilder(String.format(AD_STRING_TX_POWER, Converters.getDecimalValueFromTwosComplement(data[0])) + " dBm");
                break;
            case AD_TYPE_0x0D:
                advertismentData = new StringBuilder(String.format(AD_STRING_CLASS_DEVICE, HEX_PREFIX + Converters.getHexValue(data)));
                break;
            case AD_TYPE_0x0E:
                advertismentData = new StringBuilder(String.format(AD_STRING_SIMPLE_PAIRING_HASH, HEX_PREFIX + Converters.getHexValue(data)));
                break;
            case AD_TYPE_0x0F:
                advertismentData = new StringBuilder(String.format(AD_STRING_SIMPLE_PAIRING_RANDOMIZER, HEX_PREFIX + Converters.getHexValue(data)));
                break;
            case AD_TYPE_0x10:
                advertismentData = new StringBuilder(AD_STRING_TK_VALUE);
                for (int i = len - 2; i >= 0; i--) {
                    advertismentData.append(HEX_PREFIX).append(Converters.getHexValue(data[i]));
                }
                break;
            case AD_TYPE_0x11:
                advertismentData = new StringBuilder(String.format(AD_STRING_SECURITY_MANAGER_FLAGS, HEX_PREFIX + Converters.getHexValue(data)));
                break;
            case AD_TYPE_0x12:
                advertismentData = new StringBuilder(AD_STRING_SLAVE_CONNECTION_INTERVAL);
                float min = Integer.parseInt(Converters.getHexValue(data[1]) + Converters.getHexValue(data[0]), 16);
                if (min == 0xffff) {
                    advertismentData.append(AD_STRING_NOT_SPECIFY);
                } else {
                    min *= TIME_INTERVAL_FACTOR;
                    advertismentData.append(min).append("-");
                }
                float max = Integer.parseInt(Converters.getHexValue(data[3]) + Converters.getHexValue(data[2]), 16);
                if (max == 0xffff) {
                    advertismentData.append(AD_STRING_NOT_SPECIFY);
                } else {
                    max *= TIME_INTERVAL_FACTOR;
                    advertismentData.append(max);
                }
                advertismentData.append("ms");
                break;
            case AD_TYPE_0x14:
            case AD_TYPE_0x15:
                advertismentData = new StringBuilder(type == AD_TYPE_0x14 ? "List of 16-bit Service Solicitation UUIDs"
                        : "List of 128-bit Service Solicitation UUIDs");
                format = type == AD_TYPE_0x14 ? 2 : 16;
                for (int i = 0; i < len / format; i++) {
                    for (int j = format - 1; j >= 0; j--) {
                        advertismentData.append(HEX_PREFIX).append(Converters.getHexValue(data[format * i + j]));
                        advertismentData.append(", ");
                    }
                    advertismentData = new StringBuilder(advertismentData.substring(0, advertismentData.length() - 2));
                    advertismentData.append(", ");
                }
                advertismentData = new StringBuilder(advertismentData.substring(0, advertismentData.length() - 2));
                break;
            case AD_TYPE_0x16:
                advertismentData = new StringBuilder(String.format(AD_STRING_SERVICE_DATA, HEX_PREFIX + Converters.getHexValue(data[0]) + ", " + HEX_PREFIX
                        + Converters.getHexValue(data[1])));
                for (int i = 0; i < len; i++) {
                    advertismentData.append(HEX_PREFIX).append(Converters.getHexValue(data[i]));
                    advertismentData.append(", ");
                }
                advertismentData = new StringBuilder(advertismentData.substring(0, advertismentData.length() - 2));
                break;
            case (byte) AD_TYPE_0xFF:
                if (data.length >= 2) {
                    advertismentData = new StringBuilder(String.format(AD_STRING_COMPANY_DATA, Converters.getHexValue(data[1]), Converters.getHexValue(data[0])));
                } else {
                    advertismentData = new StringBuilder(AD_STRING_COMPANY_PARSING_ERROR);
                }
                for (int i = 0; i < len; i++) {
                    advertismentData.append(HEX_PREFIX).append(Converters.getHexValue(data[i]));
                    advertismentData.append(", ");
                }
                advertismentData = new StringBuilder(advertismentData.substring(0, advertismentData.length() - 2));
                break;
            case AD_TYPE_0x17:
                advertismentData = new StringBuilder("Public Target Address");
                break;
            case AD_TYPE_0x18:
                advertismentData = new StringBuilder("Random Target Address");
                break;
            case AD_TYPE_0x19:
                advertismentData = new StringBuilder("Appearance");
                break;
            case AD_TYPE_0x1A:
                advertismentData = new StringBuilder("Advertising Interval");
                break;
            case AD_TYPE_0x1B:
                advertismentData = new StringBuilder("LE Bluetooth Device Address");
                break;
            case AD_TYPE_0x1C:
                advertismentData = new StringBuilder("LE Role");
                break;
            case AD_TYPE_0x1D:
                advertismentData = new StringBuilder("Simple Pairing Hash C-256");
                break;
            case AD_TYPE_0x1E:
                advertismentData = new StringBuilder("Simple Pairing Randomizer R-256");
                break;
            case AD_TYPE_0x1F:
                advertismentData = new StringBuilder("List of 32-bit Service Solicitation UUIDs");
                break;
            case AD_TYPE_0x20:
                advertismentData = new StringBuilder("Service Data - 32-bit UUID");
                break;
            case AD_TYPE_0x21:
                advertismentData = new StringBuilder("Service Data - 128-bit UUID");
                break;
            case AD_TYPE_0x22:
                advertismentData = new StringBuilder("LE Secure Connections Confirmation Value");
                break;
            case AD_TYPE_0x23:
                advertismentData = new StringBuilder("LE Secure Connections Random Value");
                break;
            case AD_TYPE_0x24:
                advertismentData = new StringBuilder("URI");
                break;
            case AD_TYPE_0x25:
                advertismentData = new StringBuilder("Indoor Positioning");
                break;
            case AD_TYPE_0x26:
                advertismentData = new StringBuilder("Transport Discovery Data");
                break;
            case AD_TYPE_0x27:
                advertismentData = new StringBuilder("LE Supported Features");
                break;
            case AD_TYPE_0x28:
                advertismentData = new StringBuilder("Channel Map Update Indication");
                break;
            case AD_TYPE_0x29:
                advertismentData = new StringBuilder("PB-ADV");
                break;
            case AD_TYPE_0x2A:
                advertismentData = new StringBuilder("Mesh Message");
                break;
            case AD_TYPE_0x2B:
                advertismentData = new StringBuilder("Mesh Beacon");
                break;
            case AD_TYPE_0x2C:
                advertismentData = new StringBuilder("BIGInfo");
                break;
            case AD_TYPE_0x2D:
                advertismentData = new StringBuilder("Broadcast_Code");
                break;
            case AD_TYPE_0x3D:
                advertismentData = new StringBuilder("3D Information Data");
                break;
            default:
                advertismentData = new StringBuilder(String.format(AD_STRING_UNKNOWN_TYPE, type, len, HEX_PREFIX + Converters.getHexValue(data)));
                break;
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
        try {
            return Arrays.copyOfRange(data, startPos, startPos + data[startPos] + 1);
        } catch (ArrayIndexOutOfBoundsException | NullPointerException ex){
            ex.printStackTrace();
            return null;
        }
    }
}
