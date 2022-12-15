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

// Service - It's wrapper for <Service> xml tag
class Service {
    var name: String? = null
    var summary: String? = null
    var uuid: UUID? = null
    var characteristics: ArrayList<ServiceCharacteristic>? = null

    constructor() {
        characteristics = ArrayList()
    }

    constructor(serviceName: String?, summary: String?, characteristics: ArrayList<ServiceCharacteristic>?) {
        name = serviceName
        this.summary = summary
        this.characteristics = characteristics
    }

}