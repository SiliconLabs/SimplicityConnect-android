package com.siliconlabs.bledemo.Bluetooth.DataTypes

import java.util.*

// ServiceCharacteristic - It's wrapper for <Characteristic> xml tag from service resources
class ServiceCharacteristic {
    var name: String? = null
    var type: String? = null
    var descriptors: ArrayList<Descriptor>? = null

    constructor() {
        descriptors = ArrayList()
    }

    constructor(name: String?, type: String?, descriptors: ArrayList<Descriptor>?) {
        this.name = name
        this.type = type
        this.descriptors = descriptors
    }

}