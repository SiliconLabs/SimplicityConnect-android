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
package com.siliconlabs.bledemo.Bluetooth.DataTypes

import com.siliconlabs.bledemo.Bluetooth.Parsing.Engine
import com.siliconlabs.bledemo.Browser.Sig.GlucoseManagement
import java.util.*

// Field - It's wrapper for <Field> xml tag
class Field {
    var name: String? = null
    var unit: String? = null
    var format: String? = null
    var type: String? = null
    var requirements = arrayListOf<String>()
    var reference: String? = null
    var minimum: Long = 0
    var maximum: Long = 0
    var enumerations: ArrayList<Enumeration>? = null
    var bitfield: BitField? = null
    var referenceFields: ArrayList<Field>? = null
    var decimalExponent: Long = 0
    var multiplier: Long = 1

    init {
        referenceFields = ArrayList()
    }

    fun getSizeInBytes() : Int {
        return format?.let { field ->
            Engine.getFormat(field)
        } ?: referenceFields?.sumBy { refField ->
            refField.getSizeInBytes()
        } ?: 0
    }

    fun getBitField() : ArrayList<Field> {
        val bitFields = arrayListOf<Field>()
        if (bitfield != null) {
            bitFields.add(this)
        } else if (referenceFields?.size ?: 0 > 0 ) {
            for (subField in referenceFields!!) {
                bitFields.addAll(subField.getBitField())
            }
        }
        return bitFields
    }

    fun isNumberFormat(): Boolean {
        return when (format) {
            "uint8", "uint16", "uint24", "uint32", "uint40", "uint48", "sint8", "sint16",
            "sint24", "sint32", "sint40", "sint48", "float32", "float64", "SFLOAT", "FLOAT" -> true
            else -> false
        }
    }

    fun isStringFormat(): Boolean {
        return when (format?.toLowerCase(Locale.getDefault())) {
            "utf8s", "utf16s" -> true
            else -> false
        }
    }

    fun isFloatFormat(): Boolean {
        return when (format) {
            "FLOAT", "SFLOAT", "float32", "float64" -> true
            else -> false
        }
    }

    fun isNibbleFormat(): Boolean {
        return when (format) {
            "nibble" -> true
            "4bit" -> true
            else -> false
        }
    }

    fun isFirstNibbleInSchema() : Boolean {
        return when (name) {
            "Type","CGM Type" -> true
            else -> false
        }
    }

    fun isMostSignificantNibble() : Boolean {
        return when (name) {
            "CGM Sample Location" -> true
            "CGM Type" -> false
            else -> false
        }
    }

    fun isFullByteSintFormat(): Boolean {
        return when (format) {
            "sint8", "sint16", "sint24", "sint32", "sint48", "sint64", "sint128" -> true
            else -> false
        }
    }

    fun isFullByteUintFormat(): Boolean {
        return when (format) {
            "uint8", "uint16", "uint24", "uint32", "uint48", "uint64", "uint128" -> true
            else -> false
        }
    }

    fun getVariableFieldLength(char: Characteristic?, charValue: ByteArray) : Int {
        return when (name) {
            "Sensor Status Annunciation" -> {
                val flagsByte = charValue[1].toInt()
                val warningOctetPresent = ((flagsByte shr 5) and 0x01) == 0x01
                val calTempOctetPresent = ((flagsByte shr 6) and 0x01) == 0x01
                val statusOctetPresent = ((flagsByte shr 7) and 0x01) == 0x01

                var length = 0
                if (warningOctetPresent) length++
                if (calTempOctetPresent) length++
                if (statusOctetPresent) length++
                length
            }
            "Operand" -> {
                if (GlucoseManagement.isRecordAccessControlPoint(char)) 2
                else 0
            }
            else -> 0
        }
    }

}