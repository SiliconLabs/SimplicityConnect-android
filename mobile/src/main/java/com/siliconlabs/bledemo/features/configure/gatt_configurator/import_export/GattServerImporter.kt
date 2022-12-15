package com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export

import android.util.Xml
import com.siliconlabs.bledemo.bluetooth.parsing.Engine
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.*
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.ImportException.ErrorType.*
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.utils.XmlConst
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.utils.XmlConverter
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.utils.XmlParser
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class GattServerImporter(private val reader: BufferedReader) {

    private val parser: XmlPullParser = Xml.newPullParser()
    private val importedServer = GattServer("")
    private val xmlParser: XmlParser

    private val gattOneTimeElements = XmlConst.gattOneTimeElements.toMutableSet()
    private val serviceOneTimeElements = XmlConst.serviceOneTimeElements.toMutableSet()
    private val characteristicOneTimeElements = XmlConst.characteristicOneTimeElements.toMutableSet()
    private val descriptorOneTimeElements = XmlConst.descriptorOneTimeElements.toMutableSet()
    private val propertiesOneTimeElements = XmlConst.propertiesOneTimeElements.toMutableSet()

    init {
        parser.setInput(reader)
        xmlParser = XmlParser(parser)
    }

    fun readFile() : GattServer {
        return try {
            readRootElement()
            reader.close()
            importedServer
        } catch (err: XmlPullParserException) {
            throw ImportException(PARSING_ERROR)
        }

    }

    private fun readRootElement() {
        xmlParser.parseRootTagOpening()
        when (parser.name) {
            XmlConst.project -> {
                importedServer.importedData.device =
                        xmlParser.parseTagAttributes(XmlConst.projectAttributes)["device"]
                xmlParser.parseInside(XmlConst.project) {
                    when (parser.name) {
                        XmlConst.gatt -> readGatt()
                        else -> throw ImportException(WRONG_TAG_NAME, parser.name, XmlConst.projectElements)
                    }
                }
            }
            XmlConst.gatt -> readGatt()
            else -> throw ImportException(WRONG_TAG_NAME, parser.name, XmlConst.rootElements)
        }
        checkIncludes()
    }

    private fun checkIncludes() {
        val definedIds = mutableSetOf<String>()
        importedServer.services.let {
            it.forEach { service ->
                service.importedData.attributes["id"]?.let { id ->
                    definedIds.add(id)
            }}

            it.forEach { service ->
                val allowedIds = definedIds.filter {
                    id -> id != service.importedData.attributes["id"]
                }.toSet()

                service.importedData.include.forEach { include ->
                    if (!allowedIds.contains(include.key)) {
                        throw ImportException(WRONG_INCLUDE_ID_DECLARED,
                        include.key, allowedIds)
                    }
                }
            }
        }
    }

    private fun readGatt() {
        importedServer.importedData.gattAttributes = XmlConst.defaultGattAttributes.plus(
                xmlParser.parseTagAttributes(XmlConst.gattAttributes, mapOf(
                        "generic_attribute_service" to XmlConst.booleanValues,
                        "gatt_caching" to XmlConst.booleanValues)))
        importedServer.name =
                importedServer.importedData.gattAttributes.getOrDefault("name", XmlConst.defaultServerName)

        xmlParser.parseInside(XmlConst.gatt) {
            when (parser.name) {
                XmlConst.capabilities_declare -> {
                    parseOneTimeTag(XmlConst.capabilities_declare, gattOneTimeElements)
                    readCapabilities()
                }
                XmlConst.service -> readService()
                else -> throw ImportException(WRONG_TAG_NAME, parser.name, XmlConst.gattElements)
            }
        }
    }

    private fun readCapabilities() {
        val capabilities = mutableMapOf<String, String>()
        xmlParser.parseInside(XmlConst.capabilities_declare) {
            if (parser.name != XmlConst.capability) {
                throw ImportException(WRONG_TAG_NAME, parser.name, XmlConst.capabilitiesElements)
            }
            val attribute = xmlParser.parseTagAttributes(setOf(XmlConst.enable),
                    mapOf(XmlConst.enable to XmlConst.booleanValues))
            val capValue = xmlParser.parseTagValue(XmlConst.capabilityNameRegex)
            capabilities[capValue] = attribute.getOrDefault(XmlConst.enable, "true")
        }
        if (capabilities.size < XmlConst.MIN_CAPABILITIES_NUMBER) {
            throw ImportException(NO_CAPABILITIES_DECLARED)
        }
        if (capabilities.size > XmlConst.MAX_CAPABILITIES_NUMBER) {
            throw ImportException(TOO_MANY_CAPABILITIES_DECLARED)
        }
        importedServer.importedData.capabilities = capabilities
    }

    private fun readService() {
        Service().let {
            readServiceAttributes(it)
            xmlParser.parseInside(XmlConst.service) { parseServiceElement(parser.name, it) }
            serviceOneTimeElements.addAll(XmlConst.serviceOneTimeElements)
            importedServer.services.add(it)
        }
    }

    private fun readServiceAttributes(service: Service) {
        xmlParser.parseTagAttributes(XmlConst.serviceAttributes, mapOf(
                "requirement" to XmlConst.requirementValues, "advertise" to XmlConst.booleanValues))
                .forEach {
            when (it.key) {
                "uuid" -> service.uuid = Uuid(it.value)
                "name" -> service.name = it.value
                "type" -> service.type = XmlConverter.toServiceType(it.value)
                else -> service.importedData.attributes[it.key] = it.value
            }
        }
        if (service.uuid == null) throw ImportException(MANDATORY_ATTRIBUTE_MISSING, "uuid")
        Engine.services[service.uuid!!.getAs128BitUuid()]?.let {
            service.name = it.name!!
        }
        if (service.name.isBlank()) service.name = XmlConst.defaultServiceName
    }

    private fun parseServiceElement(tagName: String, service: Service) {
        when (tagName) {
            XmlConst.capabilities -> parseCapabilities(
                    service.importedData.capabilities,
                    importedServer.importedData.capabilities.keys,
                    serviceOneTimeElements)
            XmlConst.informativeText -> parseSimpleElement(
                    tagName, service.importedData.simpleElements, serviceOneTimeElements)
            XmlConst.description -> parseSimpleElement(
                    tagName, service.importedData.simpleElements, serviceOneTimeElements)
            XmlConst.uri -> parseSimpleElement(
                    tagName, service.importedData.simpleElements, serviceOneTimeElements)
            XmlConst.include -> parseIncludes(service)
            XmlConst.characteristic -> readCharacteristic(service)
            else -> throw ImportException(WRONG_TAG_NAME, tagName, XmlConst.serviceElements)
        }
    }

    private fun parseCapabilities(listedCapabilities: MutableSet<String>,
                                  allowedCapabilities: Set<String>, oneTimeElements: MutableSet<String>) {
        parseOneTimeTag(XmlConst.capabilities, oneTimeElements)
        xmlParser.parseInside(XmlConst.capabilities) {
            when (parser.name) {
                XmlConst.capability -> listedCapabilities.add(xmlParser.parseTagValue(allowedCapabilities))
                else -> throw ImportException(WRONG_TAG_NAME, parser.name, XmlConst.capabilitiesElements)
            }
        }
    }

    private fun parseIncludes(service: Service) {
        val attributes = xmlParser.parseTagAttributes(setOf("id", "sourceId"))
        val key = attributes["id"] ?: throw ImportException(MANDATORY_ATTRIBUTE_MISSING, "id")
        val value = attributes["sourceId"] ?: throw ImportException(MANDATORY_ATTRIBUTE_MISSING, "sourceId")

        service.importedData.include[key] = value
        xmlParser.parseTagValue() /* Empty. END_TAG reached. */

    }

    private fun readCharacteristic(service: Service) {
        Characteristic().let {
            it.properties.clear()
            readCharacteristicAttributes(it)
            xmlParser.parseInside(XmlConst.characteristic) {
                parseCharacteristicElement(parser.name, it, service)
            }
            characteristicOneTimeElements.addAll(XmlConst.characteristicOneTimeElements)

            if (it.properties.isEmpty()) throw ImportException(NO_PROPERTIES_DECLARED)
            service.characteristics.add(it)
        }
    }

    private fun readCharacteristicAttributes(characteristic: Characteristic) {
        xmlParser.parseTagAttributes(XmlConst.characteristicAttributes,
                mapOf("const" to XmlConst.booleanValues))
                .forEach {
            when (it.key) {
                "uuid" -> characteristic.uuid = Uuid(it.value)
                "name" -> characteristic.name = it.value
                else -> characteristic.importedData.attributes[it.key] = it.value
            }
        }
        if (characteristic.uuid == null) throw ImportException(MANDATORY_ATTRIBUTE_MISSING, "uuid")
        Engine.characteristics[characteristic.uuid!!.getAs128BitUuid()]?.let {
            characteristic.name = it.name!!
        }
        if (characteristic.name.isBlank()) characteristic.name = XmlConst.defaultCharacteristicName
    }

    private fun parseCharacteristicElement(tagName: String, characteristic: Characteristic, service: Service) {
        when (tagName) {
            XmlConst.capabilities -> {
                val allowedCapabilities =
                        if (service.importedData.capabilities.isNotEmpty()) service.importedData.capabilities
                        else importedServer.importedData.capabilities.keys
                parseCapabilities(characteristic.importedData.capabilities,
                        allowedCapabilities, characteristicOneTimeElements)
            }
            XmlConst.properties -> parseProperties(
                    characteristic.properties,
                    characteristic.importedData.propertiesAttributes,
                    characteristicOneTimeElements,
                    true)
            XmlConst.value -> characteristic.value = parseValue()
            XmlConst.descriptor -> readDescriptor(characteristic)
            XmlConst.informativeText -> parseSimpleElement(
                    tagName, characteristic.importedData.simpleElements, characteristicOneTimeElements)
            XmlConst.description -> parseSimpleElement(
                    tagName, characteristic.importedData.simpleElements, characteristicOneTimeElements)
            XmlConst.aggregate -> parseAggregate(characteristic)
            else -> throw ImportException(WRONG_TAG_NAME, tagName, XmlConst.characteristicElements)
        }
    }

    private fun parseProperties(properties: HashMap<Property, HashSet<Property.Type>>,
                                propertiesAttributes: MutableMap<String, String>,
                                oneTimeElements: MutableSet<String>,
                                forCharacteristic: Boolean) {
        parseOneTimeTag(XmlConst.properties, oneTimeElements)

        propertiesAttributes.putAll(xmlParser.parseTagAttributes(
                XmlConst.propertiesAttributes.plus(XmlConst.propertiesRequirementAttributes),
                XmlConverter.toRestrictions(
                        XmlConst.propertiesAttributes, XmlConst.booleanValues).plus(
                XmlConverter.toRestrictions(
                        XmlConst.propertiesRequirementAttributes, XmlConst.propertiesRequirementValues))
        ))
        xmlParser.parseInside(XmlConst.properties) {
            parseOneTimeTag(parser.name, propertiesOneTimeElements)
            when (parser.name) {
                XmlConst.read -> properties[Property.READ] = parseProperty()
                XmlConst.write -> properties[Property.WRITE] = parseProperty()
                XmlConst.write_no_response ->
                    if (forCharacteristic) properties[Property.WRITE_WITHOUT_RESPONSE] = parseProperty()
                    else throw ImportException(PROPERTY_NOT_SUPPORTED_BY_DESCRIPTOR,
                            XmlConst.write_no_response, XmlConst.descriptorPropertiesElements)
                XmlConst.reliable_write -> {
                    if (forCharacteristic) properties[Property.RELIABLE_WRITE] = parseProperty()
                    else throw ImportException(PROPERTY_NOT_SUPPORTED_BY_DESCRIPTOR,
                            XmlConst.reliable_write, XmlConst.descriptorPropertiesElements)
                }
                XmlConst.indicate -> {
                    if (forCharacteristic) properties[Property.INDICATE] = parseProperty()
                    else throw ImportException(PROPERTY_NOT_SUPPORTED_BY_DESCRIPTOR,
                            XmlConst.indicate, XmlConst.descriptorPropertiesElements)
                }
                XmlConst.notify -> {
                    if (forCharacteristic) properties[Property.NOTIFY] = parseProperty()
                    else throw ImportException(PROPERTY_NOT_SUPPORTED_BY_DESCRIPTOR,
                            XmlConst.notify, XmlConst.descriptorPropertiesElements)
                }
                else -> throw ImportException(WRONG_TAG_NAME, parser.name, XmlConst.characteristicPropertiesElements)
            }
            xmlParser.parseTagValue() /* Empty value. END_TAG reached. */
        }
        propertiesOneTimeElements.addAll(XmlConst.propertiesOneTimeElements)
        if (properties.isEmpty()) { /* Only if there is no <property> tag */
            properties.putAll(XmlConverter.toProperties(propertiesAttributes, forCharacteristic))
        }
    }

    private fun parseProperty() : HashSet<Property.Type> {
        val propertyTypes: HashSet<Property.Type> = HashSet()
        xmlParser.parseTagAttributes(XmlConst.propertyAttributes).forEach {
            when (it.value) {
                "true" -> propertyTypes.add(XmlConverter.toPropertyType(it.key))
                "false" -> {}
                else -> throw ImportException(WRONG_ATTRIBUTE_VALUE, it.value, XmlConst.booleanValues)
            }
        }
        return propertyTypes
    }

    private fun parseValue() : Value {
        val attributes = xmlParser.parseTagAttributes(XmlConst.valueAttributes,
                mapOf("type" to XmlConst.valueTypeValues,
                      "variable_length" to XmlConst.booleanValues))
        val value =
                if (attributes["type"] == "hex") xmlParser.parseTagValue(XmlConst.hexValueRegex)
                else xmlParser.parseTagValue()

        return Value(
                value,
                XmlConverter.toValueType(attributes.getOrDefault("type", "utf-8")),
                attributes.getOrDefault("length", "0").toInt(),
                attributes.getOrDefault("variable_length", "false").toBoolean())
    }

    private fun parseAggregate(characteristic: Characteristic) {
        parseOneTimeTag(XmlConst.aggregate, characteristicOneTimeElements)
        xmlParser.parseTagAttributes(XmlConst.aggregateAttributes)["id"]?.let {
            characteristic.importedData.aggregate.add(it)
        } ?: characteristic.importedData.aggregate.add(null)

        xmlParser.parseInside(XmlConst.aggregate) {
            when (parser.name) {
                XmlConst.attribute -> {
                    characteristic.importedData.aggregate.add(parseAggregateElement())
                    xmlParser.parseTagValue() /* Empty. END_TAG reached. */
                }
                else -> throw ImportException(WRONG_TAG_NAME, parser.name, XmlConst.aggregateElements)
            }
        }


    }

    private fun parseAggregateElement() : String {
        return xmlParser.parseTagAttributes(XmlConst.aggregateAttributes)["id"] ?:
        throw ImportException(MANDATORY_ATTRIBUTE_MISSING, "id")
    }

    private fun readDescriptor(characteristic: Characteristic) {
        Descriptor().let {
            it.properties.clear()
            readDescriptorAttributes(it)
            xmlParser.parseInside(XmlConst.descriptor) { parseDescriptorElement(parser.name, it) }

            if (it.properties.isEmpty()) throw ImportException(NO_PROPERTIES_DECLARED)
            descriptorOneTimeElements.addAll(XmlConst.descriptorOneTimeElements)
            characteristic.descriptors.add(it)
        }
    }

    private fun readDescriptorAttributes(descriptor: Descriptor) {
        xmlParser.parseTagAttributes(XmlConst.descriptorAttributes,
                mapOf("const" to XmlConst.booleanValues, "discoverable" to XmlConst.booleanValues)).
        forEach {
            when (it.key) {
                "uuid" -> descriptor.uuid = Uuid(it.value)
                "name" -> descriptor.name = it.value
                else -> descriptor.importedData.attributes[it.key] = it.value
            }
        }
        if (descriptor.uuid == null) throw ImportException(MANDATORY_ATTRIBUTE_MISSING, "uuid")
        Engine.descriptors[descriptor.uuid!!.getAs128BitUuid()]?.let {
            descriptor.name = it.name!!
        }
        if (descriptor.name.isBlank()) descriptor.name = XmlConst.defaultDescriptorName
    }

    private fun parseDescriptorElement(tagName: String, descriptor: Descriptor) {
        when (tagName) {
            XmlConst.properties -> parseProperties(
                    descriptor.properties,
                    descriptor.importedData.propertiesAttributes,
                    descriptorOneTimeElements,
                    false)
            XmlConst.value -> descriptor.value = parseValue()
            XmlConst.informativeText -> parseSimpleElement(
                    tagName, descriptor.importedData.simpleElements, descriptorOneTimeElements)
            else -> throw ImportException(WRONG_TAG_NAME, tagName, XmlConst.descriptorElements)
        }
    }

    private fun parseSimpleElement(tagName: String, elements: MutableMap<String, String>, oneTimeElements: MutableSet<String>) {
        parseOneTimeTag(tagName, oneTimeElements)
        elements[tagName] = xmlParser.parseTagValue()
    }
    
    private fun parseOneTimeTag(tagName: String, oneTimeElements: MutableSet<String>) {
        if (!oneTimeElements.contains(tagName)) {
            throw ImportException(TAG_MAXIMUM_OCCURRENCE_EXCEEDED, tagName)
        }
        else {
            oneTimeElements.remove(tagName)
        }
    }


}