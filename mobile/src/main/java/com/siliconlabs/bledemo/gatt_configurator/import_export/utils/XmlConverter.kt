package com.siliconlabs.bledemo.gatt_configurator.import_export.utils

import com.siliconlabs.bledemo.gatt_configurator.import_export.ImportException
import com.siliconlabs.bledemo.gatt_configurator.models.Property
import com.siliconlabs.bledemo.gatt_configurator.models.Service
import com.siliconlabs.bledemo.gatt_configurator.models.Value
import java.util.HashMap
import java.util.HashSet

object XmlConverter {

    /* From XML to high-level object */
    fun toServiceType(serviceType: String) : Service.Type {
        return when (serviceType) {
            "primary" -> Service.Type.PRIMARY
            "secondary" -> Service.Type.SECONDARY
            else -> throw ImportException(
                    ImportException.ErrorType.WRONG_ATTRIBUTE_VALUE, serviceType, XmlConst.serviceTypeValues)
        }
    }

    fun toPropertyType(type: String) : Property.Type {
        return when (type) {
            "bonded" -> Property.Type.BONDED
            "encrypted" -> Property.Type.ENCRYPTED
            "authenticated" -> Property.Type.AUTHENTICATED
            else -> throw ImportException(
                    ImportException.ErrorType.WRONG_ATTRIBUTE_NAME, type, XmlConst.propertyAttributes)
        }
    }

    fun toValueType(type: String) : Value.Type {
        return when (type) {
            "utf-8" -> Value.Type.UTF_8
            "hex" -> Value.Type.HEX
            "user" -> Value.Type.USER
            else -> throw ImportException(
                    ImportException.ErrorType.WRONG_ATTRIBUTE_VALUE, type, XmlConst.valueTypeValues)
        }
    }

    fun toProperties(propertiesAttributes: Map<String, String>, forCharacteristic: Boolean) :
            HashMap<Property, HashSet<Property.Type>> { /* From <properties> attributes */
        val properties = hashMapOf<Property, HashSet<Property.Type>>()
        propertiesAttributes.forEach {
            if (it.value == "true") {
                when (it.key) {
                    "read" -> updatePropertiesWith(properties, Property.READ)
                    "write" -> updatePropertiesWith(properties, Property.WRITE)
                    "write_no_response" -> {
                        if (forCharacteristic) updatePropertiesWith(properties, Property.WRITE_WITHOUT_RESPONSE)
                        else throw ImportException(
                                ImportException.ErrorType.PROPERTY_NOT_SUPPORTED_BY_DESCRIPTOR,
                                XmlConst.write_no_response, XmlConst.descriptorPropertiesElements)
                    }
                    "reliable_write" -> {
                        if (forCharacteristic) updatePropertiesWith(properties, Property.RELIABLE_WRITE)
                        else throw ImportException(
                                ImportException.ErrorType.PROPERTY_NOT_SUPPORTED_BY_DESCRIPTOR,
                                XmlConst.reliable_write, XmlConst.descriptorPropertiesElements)
                    }
                    "notify" -> {
                        if (forCharacteristic) updatePropertiesWith(properties, Property.NOTIFY)
                        else throw ImportException(
                                ImportException.ErrorType.PROPERTY_NOT_SUPPORTED_BY_DESCRIPTOR,
                                XmlConst.notify, XmlConst.descriptorPropertiesElements)
                    }
                    "indicate" -> {
                        if (forCharacteristic) updatePropertiesWith(properties, Property.INDICATE)
                        else throw ImportException(
                                ImportException.ErrorType.PROPERTY_NOT_SUPPORTED_BY_DESCRIPTOR,
                                XmlConst.indicate, XmlConst.descriptorPropertiesElements)
                    }
                    "authenticated_read" -> updatePropertiesWith(properties, Property.READ, Property.Type.AUTHENTICATED)
                    "bonded_read" -> updatePropertiesWith(properties, Property.READ, Property.Type.BONDED)
                    "encrypted_read" -> updatePropertiesWith(properties, Property.READ, Property.Type.ENCRYPTED)
                    "authenticated_write" -> updatePropertiesWith(properties, Property.WRITE, Property.Type.AUTHENTICATED)
                    "bonded_write" -> updatePropertiesWith(properties, Property.WRITE, Property.Type.BONDED)
                    "encrypted_write" -> updatePropertiesWith(properties, Property.WRITE, Property.Type.ENCRYPTED)
                    "encrypted_notify" -> {
                        if (forCharacteristic) updatePropertiesWith(properties, Property.NOTIFY, Property.Type.ENCRYPTED)
                        else throw ImportException(
                                ImportException.ErrorType.PROPERTY_NOT_SUPPORTED_BY_DESCRIPTOR,
                                XmlConst.notify, XmlConst.descriptorPropertiesElements)
                    }
                    "authenticated_notify" -> {
                        if (forCharacteristic) updatePropertiesWith(properties, Property.NOTIFY, Property.Type.AUTHENTICATED)
                        else throw ImportException(
                                ImportException.ErrorType.PROPERTY_NOT_SUPPORTED_BY_DESCRIPTOR,
                                XmlConst.notify, XmlConst.descriptorPropertiesElements)
                    }
                    "bonded_notify" -> {
                        if (forCharacteristic) updatePropertiesWith(properties, Property.NOTIFY, Property.Type.BONDED)
                        else throw ImportException(
                                ImportException.ErrorType.PROPERTY_NOT_SUPPORTED_BY_DESCRIPTOR,
                                XmlConst.notify, XmlConst.descriptorPropertiesElements)
                    }
                }
            }
        }
        return properties
    }

    /* From high-level object to XML */
    fun fromServiceType(type: Service.Type) : String {
        return when (type) {
            Service.Type.PRIMARY -> "primary"
            Service.Type.SECONDARY -> "secondary"
        }
    }

    fun fromValueType(type: Value.Type) : String {
        return when (type) {
            Value.Type.UTF_8 -> "utf-8"
            Value.Type.HEX -> "hex"
            Value.Type.USER -> "user"
        }
    }

    fun fromProperty(type: Property) : String {
        return when (type) {
            Property.READ -> XmlConst.read
            Property.WRITE -> XmlConst.write
            Property.WRITE_WITHOUT_RESPONSE -> XmlConst.write_no_response
            Property.RELIABLE_WRITE -> XmlConst.reliable_write
            Property.NOTIFY -> XmlConst.notify
            Property.INDICATE -> XmlConst.indicate
        }
    }

    /* Other */
    fun toRestrictions(attributes: Set<String>, allowedValues: Set<String>) : Map<String, Set<String>> {
        val restrictionsMap = mutableMapOf<String, Set<String>>()
        attributes.forEach {
            restrictionsMap[it] = allowedValues
        }
        return restrictionsMap
    }


    private fun updatePropertiesWith(
            properties: HashMap<Property, HashSet<Property.Type>>,
            property: Property,
            type: Property.Type? = null) {
        if (!properties.containsKey(property)) {
            properties[property] = hashSetOf()
        }
        type?.let {
            val supportedTypes = properties[property]!!.plus(it)
            properties[property] = supportedTypes.toHashSet()
        }
    }
}