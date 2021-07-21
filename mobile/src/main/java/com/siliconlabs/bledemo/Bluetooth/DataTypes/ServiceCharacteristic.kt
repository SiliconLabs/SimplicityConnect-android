package com.siliconlabs.bledemo.Bluetooth.DataTypes

import com.siliconlabs.bledemo.Bluetooth.Parsing.Property
import java.util.*
import kotlin.collections.HashMap

// ServiceCharacteristic - It's wrapper for <Characteristic> xml tag from service resources
class ServiceCharacteristic {
    var name: String? = null
    var type: String? = null
    var descriptors: ArrayList<Descriptor>? = null
    var properties = HashMap<Property, Property.Type>()


    fun isReadPropertyMandatory(): Boolean {
        return properties[Property.READ] == Property.Type.MANDATORY
    }

    fun isWritePropertyMandatory(): Boolean {
        return properties[Property.WRITE] == Property.Type.MANDATORY
    }

    fun isWriteWithoutResponsePropertyMandatory(): Boolean {
        return properties[Property.WRITE_WITHOUT_RESPONSE] == Property.Type.MANDATORY
    }

    fun isReliableWritePropertyMandatory(): Boolean {
        return properties[Property.RELIABLE_WRITE] == Property.Type.MANDATORY
    }

    fun isNotifyPropertyMandatory(): Boolean {
        return properties[Property.NOTIFY] == Property.Type.MANDATORY
    }

    fun isIndicatePropertyMandatory(): Boolean {
        return properties[Property.INDICATE] == Property.Type.MANDATORY
    }

    constructor() {
        descriptors = ArrayList()
    }

    constructor(name: String?, type: String?, descriptors: ArrayList<Descriptor>?) {
        this.name = name
        this.type = type
        this.descriptors = descriptors
    }

}