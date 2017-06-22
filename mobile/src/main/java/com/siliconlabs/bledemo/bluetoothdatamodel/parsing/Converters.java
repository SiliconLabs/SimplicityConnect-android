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

import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

// Converters - converts value between different numeral system
public class Converters {

    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    // Gets value in hexadecimal system
    public static String getHexValue(byte value[]) {
        if (value == null) {
            return "";
        }

        char[] hexChars = new char[value.length * 3];
        int v;
        for (int j = 0; j < value.length; j++) {
            v = value[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            hexChars[j * 3 + 2] = ' ';
        }
        return new String(hexChars);
    }

    // Gets value in hexadecimal system for single byte
    public static String getHexValue(byte b) {
        char[] hexChars = new char[2];
        int v;
        v = b & 0xFF;
        hexChars[0] = hexArray[v >>> 4];
        hexChars[1] = hexArray[v & 0x0F];
        return new String(hexChars);
    }

    // Gets value in ascii system
    public static String getAsciiValue(byte value[]) {
        if (value == null) {
            return "";
        }

        return new String(value);
    }

    // Gets value in decimal system
    public static String getDecimalValue(byte value[]) {
        if (value == null) {
            return "";
        }

        String result = "";
        for (byte b : value) {
            result += ((int) b & 0xff) + " ";
        }
        return result;
    }

    // Gets value in decimal system for single byte
    public static String getDecimalValue(byte b) {
        String result = "";
        result += ((int) b & 0xff);

        return result;
    }

    // Gets value in decimal system for single byte
    public static String getDecimalValueFromTwosComplement(byte b) {
        // the first bit of the byte in twos complement
        String result = "" + b;

        if ((b & 0xa0) > 0) {
            int val = b;
            val = ~val & 0xff;
            val = val + 0x01;

            // the sign of the value
            int sign = (b >>> 7) & 0x01;
            sign = sign > 0 ? -1 : 1;

            result = "" + (sign * val);
        }
        return result;
    }

    public static String getDecimalValueFromTwosComplement(String binaryString) {
        // default to hex value

        if (binaryString.length() > 64) {
            String binAsHex = (new BigInteger(binaryString, 2)).toString(16);
            return "0x" + binAsHex;
        }

        // prepend the sign up to 64 bits
        String result = "";
        String stringPrependExtendSign = binaryString;
        for (int i = 0; i < 64 - binaryString.length(); i++) {
            stringPrependExtendSign = binaryString.substring(0, 1) + stringPrependExtendSign;
        }

        // flip the bits (needed for negative numbers)
        String flippedBits = "";
        for (int i = 0; i < 64; i++) {
            if (binaryString.subSequence(0, 1).equals("1")) {
                // flip bits if negative twos complement negative
                if (stringPrependExtendSign.substring(i, i + 1).equals("1")) {
                    flippedBits += 0;
                } else {
                    flippedBits += 1;
                }
            }
        }

        // if prepended sign extension is negative, add one to flipped bits and make long neg.
        if (binaryString.subSequence(0, 1).equals("1")) {
            // finish twos complement calculation if negative twos complement number
            long flippedBitsAsLong = Long.parseLong(flippedBits, 2);
            flippedBitsAsLong += 1;
            flippedBitsAsLong = -1 * flippedBitsAsLong;
            result = "" + flippedBitsAsLong;
        } else {
            result = "" + Long.parseLong(stringPrependExtendSign, 2);
        }

        return result;
    }

    public static byte[] convertStringTo(String input, String format) {
        if (TextUtils.isEmpty(input)) {
            return input.getBytes();
        }

        byte[] returnVal = null;

        // note that java uses big endian
        switch (format) {
            case "utf8s":
                returnVal = convertToUTF8(input);
                break;
            case "utf16s":
                returnVal = convertToUTF16(input);
                break;
            case "uint8":
                returnVal = convertToUint8(input);
                break;
            case "uint16":
                returnVal = convertToUint16(input);
                break;
            case "uint24":
                returnVal = convertToUint24(input);
                break;
            case "uint32":
                returnVal = convertToUint32(input);
                break;
            case "uint40":
                returnVal = convertToUint40(input);
                break;
            case "uint48":
                returnVal = convertToUint48(input);
                break;
            case "sint8":
                returnVal = convertToSint8(input);
                break;
            case "sint16":
                returnVal = convertToSint16(input);
                break;
            case "sint24":
                returnVal = convertToSint24(input);
                break;
            case "sint32":
                returnVal = convertToSint32(input);
                break;
            case "sint40":
                returnVal = convertToSint40(input);
                break;
            case "sint48":
                returnVal = convertToSint48(input);
                break;
            case "float32":
                returnVal = convertToFloat32(input);
                break;
            case "float64":
                returnVal = convertToFloat64(input);
                break;
            default:
                returnVal = input.getBytes();
        }

        return returnVal;
    }

    public static byte[] convertToFloat32(String input) {
        try {
            float floatVal = Float.parseFloat(input);
            int intBits = Float.floatToIntBits(floatVal);
            byte[] returnVal = new byte[4];
            returnVal[0] = (byte) (intBits & 0xff);
            returnVal[1] = (byte) ((intBits >>> 8) & 0xff);
            returnVal[2] = (byte) ((intBits >>> 16) & 0xff);
            returnVal[3] = (byte) ((intBits >>> 24) & 0xff);
            return returnVal;
        } catch (Exception e) {
            e.printStackTrace();
            return input.getBytes();
        }
    }

    public static byte[] convertToFloat64(String input) {
        try {
            double floatVal = Double.parseDouble(input);
            long longBits = Double.doubleToLongBits(floatVal);
            byte[] returnVal = new byte[8];
            returnVal[0] = (byte) (longBits & 0xff);
            returnVal[1] = (byte) ((longBits >>> 8) & 0xff);
            returnVal[2] = (byte) ((longBits >>> 16) & 0xff);
            returnVal[3] = (byte) ((longBits >>> 24) & 0xff);
            returnVal[4] = (byte) ((longBits >>> 32) & 0xff);
            returnVal[5] = (byte) ((longBits >>> 40) & 0xff);
            returnVal[6] = (byte) ((longBits >>> 48) & 0xff);
            returnVal[7] = (byte) ((longBits >>> 56) & 0xff);
            return returnVal;
        } catch (Exception e) {
            e.printStackTrace();
            return input.getBytes();
        }
    }

    public static byte[] convertToSint8(String input) {
        try {
            int val = Integer.parseInt(input);

            byte[] returnVal = new byte[1];
            returnVal[0] = (byte) (val & 0xFF);

            return returnVal;
        } catch (Exception e) {
            e.printStackTrace();
            return input.getBytes();
        }
    }

    public static byte[] convertToSint16(String input) {
        try {
            int val = Integer.parseInt(input);

            byte[] returnVal = new byte[2];
            returnVal[0] = (byte) (val & 0xFF);
            returnVal[1] = (byte) ((val >>> 8) & 0xFF);

            return returnVal;
        } catch (Exception e) {
            e.printStackTrace();
            return input.getBytes();
        }
    }

    public static byte[] convertToSint24(String input) {
        try {
            int val = Integer.parseInt(input);

            byte[] returnVal = new byte[3];
            returnVal[0] = (byte) (val & 0xFF);
            returnVal[1] = (byte) ((val >>> 8) & 0xFF);
            returnVal[2] = (byte) ((val >>> 16) & 0xFF);

            return returnVal;
        } catch (Exception e) {
            e.printStackTrace();
            return input.getBytes();
        }
    }

    public static byte[] convertToSint32(String input) {
        try {
            long val = Long.parseLong(input);

            byte[] returnVal = new byte[4];
            returnVal[0] = (byte) (val & 0xFF);
            returnVal[1] = (byte) ((val >>> 8) & 0xFF);
            returnVal[2] = (byte) ((val >>> 16) & 0xFF);
            returnVal[3] = (byte) ((val >>> 24) & 0xFF);

            return returnVal;
        } catch (Exception e) {
            e.printStackTrace();
            return input.getBytes();
        }
    }

    public static byte[] convertToSint40(String input) {
        try {
            long val = Long.parseLong(input);

            byte[] returnVal = new byte[5];
            returnVal[0] = (byte) (val & 0xFF);
            returnVal[1] = (byte) ((val >>> 8) & 0xFF);
            returnVal[2] = (byte) ((val >>> 16) & 0xFF);
            returnVal[3] = (byte) ((val >>> 24) & 0xFF);
            returnVal[4] = (byte) ((val >>> 32) & 0xFF);

            return returnVal;
        } catch (Exception e) {
            e.printStackTrace();
            return input.getBytes();
        }
    }

    public static byte[] convertToSint48(String input) {
        try {
            long val = Long.parseLong(input);

            byte[] returnVal = new byte[6];
            returnVal[0] = (byte) (val & 0xFF);
            returnVal[1] = (byte) ((val >>> 8) & 0xFF);
            returnVal[2] = (byte) ((val >>> 16) & 0xFF);
            returnVal[3] = (byte) ((val >>> 24) & 0xFF);
            returnVal[4] = (byte) ((val >>> 32) & 0xFF);
            returnVal[5] = (byte) ((val >>> 40) & 0xFF);

            return returnVal;
        } catch (Exception e) {
            e.printStackTrace();
            return input.getBytes();
        }
    }

    public static byte[] convertToUint8(String input) {
        try {
            int val = Integer.parseInt(input);

            byte[] returnVal = new byte[1];
            returnVal[0] = (byte) (val & 0xFF);

            return returnVal;
        } catch (Exception e) {
            e.printStackTrace();
            return input.getBytes();
        }
    }

    public static byte[] convertToUint16(String input) {
        try {
            int val = Integer.parseInt(input);

            byte[] returnVal = new byte[2];
            returnVal[0] = (byte) (val & 0xFF);
            returnVal[1] = (byte) ((val >>> 8) & 0xFF);

            return returnVal;
        } catch (Exception e) {
            e.printStackTrace();
            return input.getBytes();
        }
    }

    public static byte[] convertToUint24(String input) {
        try {
            int val = Integer.parseInt(input);

            byte[] returnVal = new byte[3];
            returnVal[0] = (byte) (val & 0xFF);
            returnVal[1] = (byte) ((val >>> 8) & 0xFF);
            returnVal[2] = (byte) ((val >>> 16) & 0xFF);

            return returnVal;
        } catch (Exception e) {
            e.printStackTrace();
            return input.getBytes();
        }
    }

    public static byte[] convertToUint32(String input) {
        try {
            long val = Long.parseLong(input);

            byte[] returnVal = new byte[4];
            returnVal[0] = (byte) (val & 0xFF);
            returnVal[1] = (byte) ((val >>> 8) & 0xFF);
            returnVal[2] = (byte) ((val >>> 16) & 0xFF);
            returnVal[3] = (byte) ((val >>> 24) & 0xFF);

            return returnVal;
        } catch (Exception e) {
            e.printStackTrace();
            return input.getBytes();
        }
    }

    public static byte[] convertToUint40(String input) {
        try {
            long val = Long.parseLong(input);

            byte[] returnVal = new byte[5];
            returnVal[0] = (byte) (val & 0xFF);
            returnVal[1] = (byte) ((val >>> 8) & 0xFF);
            returnVal[2] = (byte) ((val >>> 16) & 0xFF);
            returnVal[3] = (byte) ((val >>> 24) & 0xFF);
            returnVal[4] = (byte) ((val >>> 32) & 0xFF);

            return returnVal;
        } catch (Exception e) {
            e.printStackTrace();
            return input.getBytes();
        }
    }

    public static byte[] convertToUint48(String input) {
        try {
            long val = Long.parseLong(input);

            byte[] returnVal = new byte[6];
            returnVal[0] = (byte) (val & 0xFF);
            returnVal[1] = (byte) ((val >>> 8) & 0xFF);
            returnVal[2] = (byte) ((val >>> 16) & 0xFF);
            returnVal[3] = (byte) ((val >>> 24) & 0xFF);
            returnVal[4] = (byte) ((val >>> 32) & 0xFF);
            returnVal[5] = (byte) ((val >>> 40) & 0xFF);

            return returnVal;
        } catch (Exception e) {
            e.printStackTrace();
            return input.getBytes();
        }
    }

    public static byte[] convertToUTF8(String input) {
        byte[] returnVal = null;
        try {
            returnVal = input.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            returnVal = input.getBytes();
            e.printStackTrace();
        }
        return returnVal;
    }

    public static byte[] convertToUTF16(String input) {
        byte[] returnVal = null;
        try {
            returnVal = input.getBytes("UTF-16");
        } catch (UnsupportedEncodingException e) {
            returnVal = input.getBytes();
            e.printStackTrace();
        }
        return returnVal;
    }
}
