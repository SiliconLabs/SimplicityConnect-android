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

import androidx.core.util.Pair;
import android.text.TextUtils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

// Converters - converts value between different numeral system
public class Converters {

    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    // Gets value in hexadecimal system
    public static String getHexValue(byte[] value) {
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
    public static String getAsciiValue(byte[] value) {
        if (value == null) {
            return "";
        }

        return new String(value);
    }

    // Gets value in decimal system
    public static String getDecimalValue(byte[] value) {
        if (value == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (byte b : value) {
            result.append((int) b & 0xff).append(" ");
        }
        return result.toString();
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
        StringBuilder stringPrependExtendSign = new StringBuilder(binaryString);
        for (int i = 0; i < 64 - binaryString.length(); i++) {
            stringPrependExtendSign.insert(0, binaryString.substring(0, 1));
        }

        // flip the bits (needed for negative numbers)
        StringBuilder flippedBits = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            if (binaryString.subSequence(0, 1).equals("1")) {
                // flip bits if negative twos complement negative
                if (stringPrependExtendSign.substring(i, i + 1).equals("1")) {
                    flippedBits.append(0);
                } else {
                    flippedBits.append(1);
                }
            }
        }

        // if prepended sign extension is negative, add one to flipped bits and make long neg.
        if (binaryString.subSequence(0, 1).equals("1")) {
            // finish twos complement calculation if negative twos complement number
            long flippedBitsAsLong = Long.parseLong(flippedBits.toString(), 2);
            flippedBitsAsLong += 1;
            flippedBitsAsLong = -1 * flippedBitsAsLong;
            result = "" + flippedBitsAsLong;
        } else {
            result = "" + Long.parseLong(stringPrependExtendSign.toString(), 2);
        }

        return result;
    }

    public static Pair<byte[], Boolean> convertStringTo(String input, String format) {
        if (TextUtils.isEmpty(input)) {
            return new Pair<>(input.getBytes(), true);
        }

        byte[] returnVal;

        // note that java uses big endian
        switch (format) {
            case "utf8s":
                returnVal = convertToUTF8(input);
                break;
            case "utf16s":
                returnVal = convertToUTF16(input);
                break;
            case "uint8":
                return convertToUint8(input);
            case "uint16":
                return convertToUint16(input);
            case "uint24":
                return convertToUint24(input);
            case "uint32":
                return convertToUint32(input);
            case "uint40":
                return convertToUint40(input);
            case "uint48":
                return convertToUint48(input);
            case "sint8":
                return convertToSint8(input);
            case "sint16":
                return convertToSint16(input);
            case "sint24":
                return convertToSint24(input);
            case "sint32":
                return convertToSint32(input);
            case "sint40":
                return convertToSint40(input);
            case "sint48":
                return convertToSint48(input);
            case "float32":
                return convertToFloat32(input);
            case "float64":
                return convertToFloat64(input);
            default:
                return new Pair<>(input.getBytes(), true);
        }

        return new Pair<>(returnVal, true);
    }

    public static Pair<byte[], Boolean> convertToFloat32(String input) {
        try {
            float floatVal = Float.parseFloat(input);

            int intBits = Float.floatToIntBits(floatVal);
            byte[] returnVal = new byte[4];
            returnVal[0] = (byte) (intBits & 0xff);
            returnVal[1] = (byte) ((intBits >>> 8) & 0xff);
            returnVal[2] = (byte) ((intBits >>> 16) & 0xff);
            returnVal[3] = (byte) ((intBits >>> 24) & 0xff);

            return new Pair<>(returnVal, true);
        } catch (Exception e) {
            e.printStackTrace();
            return new Pair<>(input.getBytes(), false);
        }
    }

    public static Pair<byte[], Boolean> convertToFloat64(String input) {
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

            return new Pair<>(returnVal, true);
        } catch (Exception e) {
            e.printStackTrace();
            return new Pair<>(input.getBytes(), false);
        }
    }



    public static Pair<byte[], Boolean> convertToSint8(String input) {
        try {
            int val = Integer.parseInt(input);

            byte[] returnVal = new byte[1];
            returnVal[0] = (byte) (val & 0xFF);

            Boolean inRage = isValueInRange(-128, 127, val);

            return new Pair<>(returnVal, inRage);
        } catch (Exception e) {
            e.printStackTrace();
            return new Pair<>(input.getBytes(), false);
        }
    }

    public static Pair<byte[], Boolean> convertToSint16(String input) {
        try {
            int val = Integer.parseInt(input);

            byte[] returnVal = new byte[2];
            returnVal[0] = (byte) (val & 0xFF);
            returnVal[1] = (byte) ((val >>> 8) & 0xFF);

            Boolean inRage = isValueInRange(-32768, 32767, val);

            return new Pair<>(returnVal, inRage);
        } catch (Exception e) {
            e.printStackTrace();
            return new Pair<>(input.getBytes(), false);
        }
    }

    public static Pair<byte[], Boolean> convertToSint24(String input) {
        try {
            int val = Integer.parseInt(input);

            byte[] returnVal = new byte[3];
            returnVal[0] = (byte) (val & 0xFF);
            returnVal[1] = (byte) ((val >>> 8) & 0xFF);
            returnVal[2] = (byte) ((val >>> 16) & 0xFF);

            Boolean inRage = isValueInRange(-8388608, 8388607, val);

            return new Pair<>(returnVal, inRage);
        } catch (Exception e) {
            e.printStackTrace();
            return new Pair<>(input.getBytes(), false);
        }
    }

    public static Pair<byte[], Boolean> convertToSint32(String input) {
        try {
            long val = Long.parseLong(input);

            byte[] returnVal = new byte[4];
            returnVal[0] = (byte) (val & 0xFF);
            returnVal[1] = (byte) ((val >>> 8) & 0xFF);
            returnVal[2] = (byte) ((val >>> 16) & 0xFF);
            returnVal[3] = (byte) ((val >>> 24) & 0xFF);

            Boolean inRage = isValueInRange(-2147483648L, 2147483647L, val);

            return new Pair<>(returnVal, inRage);
        } catch (Exception e) {
            e.printStackTrace();
            return new Pair<>(input.getBytes(), false);
        }
    }

    public static Pair<byte[], Boolean> convertToSint40(String input) {
        try {
            long val = Long.parseLong(input);

            byte[] returnVal = new byte[5];
            returnVal[0] = (byte) (val & 0xFF);
            returnVal[1] = (byte) ((val >>> 8) & 0xFF);
            returnVal[2] = (byte) ((val >>> 16) & 0xFF);
            returnVal[3] = (byte) ((val >>> 24) & 0xFF);
            returnVal[4] = (byte) ((val >>> 32) & 0xFF);

            Boolean inRage = isValueInRange(-140737488355328L, 140737488355327L, val);

            return new Pair<>(returnVal, inRage);
        } catch (Exception e) {
            e.printStackTrace();
            return new Pair<>(input.getBytes(), false);
        }
    }

    public static Pair<byte[], Boolean> convertToSint48(String input) {
        try {
            long val = Long.parseLong(input);

            byte[] returnVal = new byte[6];
            returnVal[0] = (byte) (val & 0xFF);
            returnVal[1] = (byte) ((val >>> 8) & 0xFF);
            returnVal[2] = (byte) ((val >>> 16) & 0xFF);
            returnVal[3] = (byte) ((val >>> 24) & 0xFF);
            returnVal[4] = (byte) ((val >>> 32) & 0xFF);
            returnVal[5] = (byte) ((val >>> 40) & 0xFF);

            Boolean inRage = isValueInRange(-140737488355328L, 140737488355327L, val);

            return new Pair<>(returnVal, inRage);
        } catch (Exception e) {
            e.printStackTrace();
            return new Pair<>(input.getBytes(), false);
        }
    }

    public static Pair<byte[], Boolean> convertToUint8(String input) {
        try {
            int val = Integer.parseInt(input);

            byte[] returnVal = new byte[1];
            returnVal[0] = (byte) (val & 0xFF);

            Boolean inRage = isValueInRange(0, 255, val);

            return new Pair<>(returnVal, inRage);
        } catch (Exception e) {
            e.printStackTrace();
            return new Pair<>(input.getBytes(), false);
        }
    }

    public static Pair<byte[], Boolean> convertToUint16(String input) {
        try {
            int val = Integer.parseInt(input);

            byte[] returnVal = new byte[2];
            returnVal[0] = (byte) (val & 0xFF);
            returnVal[1] = (byte) ((val >>> 8) & 0xFF);

            Boolean inRage = isValueInRange(0, 65535, val);

            return new Pair<>(returnVal, inRage);
        } catch (Exception e) {
            e.printStackTrace();
            return new Pair<>(input.getBytes(), false);
        }
    }

    public static Pair<byte[], Boolean> convertToUint24(String input) {
        try {
            int val = Integer.parseInt(input);

            byte[] returnVal = new byte[3];
            returnVal[0] = (byte) (val & 0xFF);
            returnVal[1] = (byte) ((val >>> 8) & 0xFF);
            returnVal[2] = (byte) ((val >>> 16) & 0xFF);

            Boolean inRage = isValueInRange(0, 16777215L, val);

            return new Pair<>(returnVal, inRage);
        } catch (Exception e) {
            e.printStackTrace();
            return new Pair<>(input.getBytes(), false);
        }
    }

    public static Pair<byte[], Boolean> convertToUint32(String input) {
        try {
            long val = Long.parseLong(input);

            byte[] returnVal = new byte[4];
            returnVal[0] = (byte) (val & 0xFF);
            returnVal[1] = (byte) ((val >>> 8) & 0xFF);
            returnVal[2] = (byte) ((val >>> 16) & 0xFF);
            returnVal[3] = (byte) ((val >>> 24) & 0xFF);

            Boolean inRage = isValueInRange(0, 4294967295L, val);

            return new Pair<>(returnVal, inRage);
        } catch (Exception e) {
            e.printStackTrace();
            return new Pair<>(input.getBytes(), false);
        }
    }

    public static Pair<byte[], Boolean> convertToUint40(String input) {
        try {
            long val = Long.parseLong(input);

            byte[] returnVal = new byte[5];
            returnVal[0] = (byte) (val & 0xFF);
            returnVal[1] = (byte) ((val >>> 8) & 0xFF);
            returnVal[2] = (byte) ((val >>> 16) & 0xFF);
            returnVal[3] = (byte) ((val >>> 24) & 0xFF);
            returnVal[4] = (byte) ((val >>> 32) & 0xFF);

            Boolean inRage = isValueInRange(0, 281474976710655L, val);

            return new Pair<>(returnVal, inRage);
        } catch (Exception e) {
            e.printStackTrace();
            return new Pair<>(input.getBytes(), false);
        }
    }

    public static Pair<byte[], Boolean> convertToUint48(String input) {
        try {
            long val = Long.parseLong(input);

            byte[] returnVal = new byte[6];
            returnVal[0] = (byte) (val & 0xFF);
            returnVal[1] = (byte) ((val >>> 8) & 0xFF);
            returnVal[2] = (byte) ((val >>> 16) & 0xFF);
            returnVal[3] = (byte) ((val >>> 24) & 0xFF);
            returnVal[4] = (byte) ((val >>> 32) & 0xFF);
            returnVal[5] = (byte) ((val >>> 40) & 0xFF);

            Boolean inRage = isValueInRange(0, 281474976710655L, val);

            return new Pair<>(returnVal, inRage);
        } catch (Exception e) {
            e.printStackTrace();
            return new Pair<>(input.getBytes(), false);
        }
    }

    public static byte[] convertToUTF8(String input) {
        byte[] returnVal = null;
        returnVal = input.getBytes(StandardCharsets.UTF_8);
        return returnVal;
    }

    public static byte[] convertToUTF16(String input) {
        byte[] returnVal = null;
        returnVal = input.getBytes(StandardCharsets.UTF_16);
        return returnVal;
    }

    private static boolean isValueInRange(int min, int max, int val) {
        return max > min ? val >= min && val <= max : val >= max && val <= min;
    }

    private static boolean isValueInRange(long min, long max, long val) {
        return max > min ? val >= min && val <= max : val >= max && val <= min;
    }
}
