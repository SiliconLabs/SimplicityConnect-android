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
package com.siliconlabs.bledemo.utils

import android.text.TextUtils
import androidx.core.util.Pair
import com.google.common.math.IntMath.pow
import timber.log.Timber
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.math.abs
import kotlin.math.pow

object Converters {
    val HEX_CHARS: CharArray = "0123456789ABCDEF".toCharArray()

    fun byteToUnsignedInt(byte: Byte): Int {
        return byte.toInt() and 0xFF
    }

    fun isHexCorrect(text: String): Boolean {
        for (letter in text.toUpperCase(Locale.getDefault())) if (!HEX_CHARS.contains(letter)) return false
        return true
    }

    fun hexStringAsByteArray(text: String): ByteArray {
        if (!isHexCorrect(text))
            throw IllegalArgumentException("Incorrect hexadecimal characters")

        val length = text.length
        val array = ByteArray(length / 2)

        for (i in 0 until length step 2) {
            val hexByte: String = StringBuilder().append(text[i]).append(text[i+1]).toString()
            val result = (hexByte).toInt(16)
            if (result > 127) array[i / 2] = (result - 256).toByte()
            else array[i/2] = result.toByte()
        }

        return array
    }

    fun bytesToHex(bytes: ByteArray): String {
        val builder = StringBuilder()
        for(byte in bytes) { builder.append(String.format("%02x",byte))}
        return builder.toString()
    }

    fun hexToByteArray(hex: String): ByteArray {
        var tmpHex = hex

        if (tmpHex.isNotEmpty() && tmpHex.length % 2 != 0) {
            tmpHex = "0$tmpHex"
        }
        val len = tmpHex.length / 2
        val byteArr = ByteArray(len)
        for (i in byteArr.indices) {
            val init = i * 2
            val end = init + 2
            val temp = tmpHex.substring(init, end).toInt(16)
            byteArr[i] = (temp and 0xFF).toByte()
        }
        return byteArr
    }

    fun intToByteArray(newVal: Int, formatLength: Int): ByteArray {
        var tmpNewVal = newVal
        val tmpVal = ByteArray(formatLength)
        for (i in 0 until formatLength) {
            tmpVal[i] = (tmpNewVal and 0xff).toByte()
            tmpNewVal = tmpNewVal shr 8
        }
        return tmpVal
    }

    fun isZeroed(bytes: ByteArray): Boolean {
        for (b in bytes) {
            if (b.toInt() != 0x00) {
                return false
            }
        }
        return true
    }

    // Gets value in hexadecimal system
    fun bytesToHexWhitespaceDelimited(value: ByteArray?): String {
        if (value == null) {
            return ""
        }
        val hexChars = CharArray(value.size * 3)
        var v: Int
        for (j in value.indices) {
            v = value[j].toInt() and 0xFF
            hexChars[j * 3] = HEX_CHARS[v ushr 4]
            hexChars[j * 3 + 1] = HEX_CHARS[v and 0x0F]
            hexChars[j * 3 + 2] = ' '
        }
        return String(hexChars)
    }

    // Gets value in hexadecimal system for single byte
    fun getHexValue(b: Byte): String {
        val hexChars = CharArray(2)
        val v: Int = b.toInt() and 0xFF
        hexChars[0] = HEX_CHARS[v ushr 4]
        hexChars[1] = HEX_CHARS[v and 0x0F]
        return String(hexChars)
    }

    // Gets value in hexadecimal system
    fun getHexValue(value: ByteArray?): String {
        if (value == null) {
            return ""
        }
        val hexChars = CharArray(value.size * 3)
        var v: Int
        for (j in value.indices) {
            v = value[j].toInt() and 0xFF
            hexChars[j * 3] = HEX_CHARS[v ushr 4]
            hexChars[j * 3 + 1] = HEX_CHARS[v and 0x0F]
            hexChars[j * 3 + 2] = ' '
        }
        return String(hexChars).trim()
    }

    // Gets value in ascii system
    fun getAsciiValue(value: ByteArray?): String {
        if (value == null) {
            return ""
        }
        val builder = StringBuilder()
        for (i in value.indices) {
            if (value[i] in 32..126) builder.append(value[i].toChar()) else builder.append("\uFFFD")
        }
        return builder.toString()
    }

    // Gets value in decimal system
    fun getDecimalValue(value: ByteArray?): String {
        if (value == null) {
            return ""
        }
        val result = StringBuilder()
        for (b in value) {
            result.append(b.toInt() and 0xff).append(" ")
        }
        return result.toString()
    }

    fun calculateDecimalValue(array: ByteArray, isBigEndian: Boolean) : Int {
        val byteBasis = 256.0
        var value = 0

        if (isBigEndian) array.reverse()
        array.forEachIndexed { index, byteValue ->
            value += getIntFromTwosComplement(byteValue) * byteBasis.pow(index).toInt()
        }
        return value
    }

    fun getIntFromTwosComplement(value: Byte) : Int {
        return if (value < 0) 256 - abs(value.toInt()) else value.toInt()
    }

    fun getTwosComplementFromUnsignedInt(number: Int, bits: Int) : Int {
        var convertedNumber = 0
        for (i in 0 until bits) {
            if (number shr i and 0x01 == 0x01) {
                if (i == bits-1) convertedNumber -= pow(2, i)
                else convertedNumber += pow(2, i)
            }
        }
        return convertedNumber
    }

    // Gets value in decimal system for single byte
    fun getDecimalValue(b: Byte): String {
        var result = ""
        result += b.toInt() and 0xff
        return result
    }

    // Converts string given in decimal system to byte array
    fun decToByteArray(dec: String): ByteArray {
        if (dec.isEmpty()) {
            return byteArrayOf()
        }
        val decArray = dec.trim().split(" ")
        val byteArr = ByteArray(decArray.size)
        for (i in decArray.indices) {
            try {
                byteArr[i] = decArray[i].toInt().toByte()
            } catch (e: NumberFormatException) {
                return byteArrayOf(0)
            }
        }
        return byteArr
    }

    // Gets value in decimal system for single byte
    fun getDecimalValueFromTwosComplement(b: Byte): String {
        // the first bit of the byte in twos complement
        var result = "" + b
        if (b.toInt() and 0xa0 > 0) {
            var value = b.toInt()
            value = value.inv() and 0xff
            value = value.toInt() + 0x01

            // the sign of the value
            var sign: Int = b.toInt() ushr 7 and 0x01
            sign = if (sign > 0) -1 else 1
            result = "" + sign * value
        }
        return result
    }

    fun getDecimalValueFromTwosComplement(binaryString: String): String {
        // default to hex value
        if (binaryString.length > 64) {
            val binAsHex = BigInteger(binaryString, 2).toString(16)
            return "0x$binAsHex"
        }

        // prepend the sign up to 64 bits
        var result = ""
        val stringPrependExtendSign = StringBuilder(binaryString)
        for (i in 0 until 64 - binaryString.length) {
            stringPrependExtendSign.insert(0, binaryString.substring(0, 1))
        }

        // flip the bits (needed for negative numbers)
        val flippedBits = StringBuilder()
        for (i in 0..63) {
            if (binaryString.subSequence(0, 1) == "1") {
                // flip bits if negative twos complement negative
                if (stringPrependExtendSign.substring(i, i + 1) == "1") {
                    flippedBits.append(0)
                } else {
                    flippedBits.append(1)
                }
            }
        }

        // if prepended sign extension is negative, add one to flipped bits and make long neg.
        if (binaryString.subSequence(0, 1) == "1") {
            // finish twos complement calculation if negative twos complement number
            var flippedBitsAsLong = flippedBits.toString().toLong(2)
            flippedBitsAsLong += 1
            flippedBitsAsLong *= -1
            result = "" + flippedBitsAsLong
        } else {
            result = "" + stringPrependExtendSign.toString().toLong(2)
        }
        return result
    }

    fun convertStringTo(input: String, format: String?): Pair<ByteArray, Boolean> {
        if (TextUtils.isEmpty(input)) {
            return Pair(input.toByteArray(), true)
        }
        val returnVal: ByteArray = when (format) {
            "utf8s" -> convertToUTF8(input)
            "utf16s" -> convertToUTF16(input)
            "uint8" -> return convertToUint8(input)
            "uint16" -> return convertToUint16(input)
            "uint24" -> return convertToUint24(input)
            "uint32" -> return convertToUint32(input)
            "uint40" -> return convertToUint40(input)
            "uint48" -> return convertToUint48(input)
            "sint8" -> return convertToSint8(input)
            "sint16" -> return convertToSint16(input)
            "sint24" -> return convertToSint24(input)
            "sint32" -> return convertToSint32(input)
            "sint40" -> return convertToSint40(input)
            "sint48" -> return convertToSint48(input)
            "float32" -> return convertToFloat32(input)
            "float64" -> return convertToFloat64(input)
            "SFLOAT" -> return convertToSfloat(input)
            else -> return Pair(input.toByteArray(), true)
        }
        return Pair(returnVal, true)
    }

    fun convertToFloat32(input: String): Pair<ByteArray, Boolean> {
        return try {
            val floatVal = input.toFloat()
            val intBits = java.lang.Float.floatToIntBits(floatVal)
            val returnVal = ByteArray(4)
            returnVal[0] = (intBits and 0xff).toByte()
            returnVal[1] = (intBits ushr 8 and 0xff).toByte()
            returnVal[2] = (intBits ushr 16 and 0xff).toByte()
            returnVal[3] = (intBits ushr 24 and 0xff).toByte()
            Pair(returnVal, true)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(input.toByteArray(), false)
        }
    }

    fun convertToFloat64(input: String): Pair<ByteArray, Boolean> {
        return try {
            val floatVal = input.toDouble()
            val longBits = java.lang.Double.doubleToLongBits(floatVal)
            val returnVal = ByteArray(8)
            returnVal[0] = (longBits and 0xff).toByte()
            returnVal[1] = (longBits ushr 8 and 0xff).toByte()
            returnVal[2] = (longBits ushr 16 and 0xff).toByte()
            returnVal[3] = (longBits ushr 24 and 0xff).toByte()
            returnVal[4] = (longBits ushr 32 and 0xff).toByte()
            returnVal[5] = (longBits ushr 40 and 0xff).toByte()
            returnVal[6] = (longBits ushr 48 and 0xff).toByte()
            returnVal[7] = (longBits ushr 56 and 0xff).toByte()
            Pair(returnVal, true)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(input.toByteArray(), false)
        }
    }

    fun convertToSfloat(input: String): Pair<ByteArray, Boolean> {
        val maxMantissa = pow(2, 11)
        val maxExponent = 7
        val minExponent = -8

        try {
            var mantissa = 0
            var exponent = 0

            if (input.contains('.')) {
                val dotIndex = input.indexOf('.')
                val decimalPlaces = input.length - 1 - dotIndex
                mantissa = StringBuilder(input).deleteCharAt(dotIndex).toString().toInt()
                exponent = -decimalPlaces
            } else {
                mantissa = input.toInt()
            }

            for (i in mantissa.toString().length - 1 downTo 0) {
                if (mantissa.toString()[i] == '0') {
                    mantissa /= 10
                    exponent += 1
                }
                else break
            }

            if (abs(mantissa) > maxMantissa || exponent > maxExponent || exponent < minExponent) {
                return Pair(input.toByteArray(), false)
            }

            val leastSignificantByte = getTwosComplementFromUnsignedInt(mantissa and 0xff, 8).toByte()
            val exponentPartOfMsb = getTwosComplementFromUnsignedInt(exponent, 4) and 0x0f shl 4
            val mantissaPartOfMsb = getTwosComplementFromUnsignedInt(mantissa shr 8, 4) and 0x0f

            val sfloatByteArray = byteArrayOf(
                leastSignificantByte,
                (exponentPartOfMsb or mantissaPartOfMsb).toByte()
            )

            return Pair(sfloatByteArray, true)
        } catch (e: Exception) {
            e.printStackTrace()
            return Pair(input.toByteArray(), false)
        }
    }

    fun convertToSint8(input: String): Pair<ByteArray, Boolean> {
        return try {
            val value = input.toInt()
            val returnVal = ByteArray(1)
            returnVal[0] = (value and 0xFF).toByte()
            val inRage = isValueInRange(-128, 127, value)
            Pair(returnVal, inRage)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(input.toByteArray(), false)
        }
    }

    fun convertToSint16(input: String): Pair<ByteArray, Boolean> {
        return try {
            val value = input.toInt()
            val returnVal = ByteArray(2)
            returnVal[0] = (value and 0xFF).toByte()
            returnVal[1] = (value ushr 8 and 0xFF).toByte()
            val inRage = isValueInRange(-32768, 32767, value)
            Pair(returnVal, inRage)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(input.toByteArray(), false)
        }
    }

    fun convertToSint24(input: String): Pair<ByteArray, Boolean> {
        return try {
            val value = input.toInt()
            val returnVal = ByteArray(3)
            returnVal[0] = (value and 0xFF).toByte()
            returnVal[1] = (value ushr 8 and 0xFF).toByte()
            returnVal[2] = (value ushr 16 and 0xFF).toByte()
            val inRage = isValueInRange(-8388608, 8388607, value)
            Pair(returnVal, inRage)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(input.toByteArray(), false)
        }
    }

    fun convertToSint32(input: String): Pair<ByteArray, Boolean> {
        return try {
            val value = input.toLong()
            val returnVal = ByteArray(4)
            returnVal[0] = (value and 0xFF).toByte()
            returnVal[1] = (value ushr 8 and 0xFF).toByte()
            returnVal[2] = (value ushr 16 and 0xFF).toByte()
            returnVal[3] = (value ushr 24 and 0xFF).toByte()
            val inRage = isValueInRange(-2147483648L, 2147483647L, value)
            Pair(returnVal, inRage)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(input.toByteArray(), false)
        }
    }

    fun convertToSint40(input: String): Pair<ByteArray, Boolean> {
        return try {
            val value = input.toLong()
            val returnVal = ByteArray(5)
            returnVal[0] = (value and 0xFF).toByte()
            returnVal[1] = (value ushr 8 and 0xFF).toByte()
            returnVal[2] = (value ushr 16 and 0xFF).toByte()
            returnVal[3] = (value ushr 24 and 0xFF).toByte()
            returnVal[4] = (value ushr 32 and 0xFF).toByte()
            val inRage = isValueInRange(-140737488355328L, 140737488355327L, value)
            Pair(returnVal, inRage)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(input.toByteArray(), false)
        }
    }

    fun convertToSint48(input: String): Pair<ByteArray, Boolean> {
        return try {
            val value = input.toLong()
            val returnVal = ByteArray(6)
            returnVal[0] = (value and 0xFF).toByte()
            returnVal[1] = (value ushr 8 and 0xFF).toByte()
            returnVal[2] = (value ushr 16 and 0xFF).toByte()
            returnVal[3] = (value ushr 24 and 0xFF).toByte()
            returnVal[4] = (value ushr 32 and 0xFF).toByte()
            returnVal[5] = (value ushr 40 and 0xFF).toByte()
            val inRage = isValueInRange(-140737488355328L, 140737488355327L, value)
            Pair(returnVal, inRage)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(input.toByteArray(), false)
        }
    }

    fun convertToUint8(input: String): Pair<ByteArray, Boolean> {
        return try {
            val value = input.toInt()
            val returnVal = ByteArray(1)
            returnVal[0] = (value and 0xFF).toByte()
            val inRage = isValueInRange(0, 255, value)
            Pair(returnVal, inRage)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(input.toByteArray(), false)
        }
    }

    fun convertToUint16(input: String): Pair<ByteArray, Boolean> {
        return try {
            val value = input.toInt()
            val returnVal = ByteArray(2)
            returnVal[0] = (value and 0xFF).toByte()
            returnVal[1] = (value ushr 8 and 0xFF).toByte()
            val inRage = isValueInRange(0, 65535, value)
            Pair(returnVal, inRage)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(input.toByteArray(), false)
        }
    }

    fun convertToUint24(input: String): Pair<ByteArray, Boolean> {
        return try {
            val value = input.toInt()
            val returnVal = ByteArray(3)
            returnVal[0] = (value and 0xFF).toByte()
            returnVal[1] = (value ushr 8 and 0xFF).toByte()
            returnVal[2] = (value ushr 16 and 0xFF).toByte()
            val inRage = isValueInRange(0, 16777215L, value.toLong())
            Pair(returnVal, inRage)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(input.toByteArray(), false)
        }
    }

    fun convertToUint32(input: String): Pair<ByteArray, Boolean> {
        return try {
            val value = input.toLong()
            val returnVal = ByteArray(4)
            returnVal[0] = (value and 0xFF).toByte()
            returnVal[1] = (value ushr 8 and 0xFF).toByte()
            returnVal[2] = (value ushr 16 and 0xFF).toByte()
            returnVal[3] = (value ushr 24 and 0xFF).toByte()
            val inRage = isValueInRange(0, 4294967295L, value)
            Pair(returnVal, inRage)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(input.toByteArray(), false)
        }
    }

    fun convertToUint40(input: String): Pair<ByteArray, Boolean> {
        return try {
            val value = input.toLong()
            val returnVal = ByteArray(5)
            returnVal[0] = (value and 0xFF).toByte()
            returnVal[1] = (value ushr 8 and 0xFF).toByte()
            returnVal[2] = (value ushr 16 and 0xFF).toByte()
            returnVal[3] = (value ushr 24 and 0xFF).toByte()
            returnVal[4] = (value ushr 32 and 0xFF).toByte()
            val inRage = isValueInRange(0, 281474976710655L, value)
            Pair(returnVal, inRage)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(input.toByteArray(), false)
        }
    }

    fun convertToUint48(input: String): Pair<ByteArray, Boolean> {
        return try {
            val value = input.toLong()
            val returnVal = ByteArray(6)
            returnVal[0] = (value and 0xFF).toByte()
            returnVal[1] = (value ushr 8 and 0xFF).toByte()
            returnVal[2] = (value ushr 16 and 0xFF).toByte()
            returnVal[3] = (value ushr 24 and 0xFF).toByte()
            returnVal[4] = (value ushr 32 and 0xFF).toByte()
            returnVal[5] = (value ushr 40 and 0xFF).toByte()
            val inRage = isValueInRange(0, 281474976710655L, value)
            Pair(returnVal, inRage)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(input.toByteArray(), false)
        }
    }

    fun convertToUTF8(input: String): ByteArray {
        var returnVal: ByteArray? = null
        returnVal = input.toByteArray(StandardCharsets.UTF_8)
        return returnVal
    }

    fun convertToUTF16(input: String): ByteArray {
        var returnVal: ByteArray? = null
        returnVal = input.toByteArray(StandardCharsets.UTF_16)
        return returnVal
    }

    private fun isValueInRange(min: Int, max: Int, value: Int): Boolean {
        return if (max > min) value in min..max else value in max..min
    }

    private fun isValueInRange(min: Long, max: Long, value: Long): Boolean {
        return if (max > min) value in min..max else value in max..min
    }
}
