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
package com.siliconlabs.bledemo.bluetooth.data_types

import java.util.*

// Field - It's wrapper for <Field> xml tag
class Field {
    var name: String? = null
    var unit: String? = null
    var format: String? = null
    var type: String? = null
    var requirement: String? = null
    var reference: String? = null
    var minimum: Long = 0
    var maximum: Long = 0
    var enumerations: ArrayList<Enumeration>? = null
    var bitfield: BitField? = null
    var referenceFields: ArrayList<Field>? = null
    var decimalExponent: Long = 0

    constructor() {
        referenceFields = ArrayList()
    }

    constructor(name: String?, unit: String?, format: String?, type: String?, requirement: String?, reference: String?,
                minimum: Int, maximum: Int, enumerations: ArrayList<Enumeration>?, bitfield: BitField?, decimalExponent: Int) {
        this.name = name
        this.unit = unit
        this.format = format
        this.type = type
        this.minimum = minimum.toLong()
        this.maximum = maximum.toLong()
        this.enumerations = enumerations
        this.bitfield = bitfield
        this.requirement = requirement
        this.reference = reference
        this.decimalExponent = decimalExponent.toLong()
    }

}