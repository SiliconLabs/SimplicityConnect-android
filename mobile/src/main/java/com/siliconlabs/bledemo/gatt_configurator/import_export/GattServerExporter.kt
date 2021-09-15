package com.siliconlabs.bledemo.gatt_configurator.import_export

import android.util.Xml
import com.siliconlabs.bledemo.gatt_configurator.import_export.utils.XmlConst
import com.siliconlabs.bledemo.gatt_configurator.import_export.utils.XmlConverter
import com.siliconlabs.bledemo.gatt_configurator.import_export.utils.XmlPrinter
import com.siliconlabs.bledemo.gatt_configurator.models.*
import org.xmlpull.v1.XmlSerializer
import java.io.BufferedWriter
import java.io.StringWriter
import java.util.*

class GattServerExporter(private val serializer: XmlSerializer = Xml.newSerializer()) {

    private lateinit var printer: XmlPrinter

    fun export(chosenServers: List<GattServer>) : Map<String, String> {
        val fileNames = convertNames(chosenServers)
        val outputs = mutableMapOf<String, String>()
        chosenServers.forEachIndexed { index, server ->

            val content = StringWriter()
            serializer.setOutput(BufferedWriter(content))
            printer = XmlPrinter(serializer)
            populateFile(server)

            outputs[fileNames[index]] = content.toString()
        }
        return outputs
    }

    private fun convertNames(servers: List<GattServer>) : List<String> {
        return servers.map { server ->
            server.name.toLowerCase(Locale.getDefault()).replace(" ", "_")
        }.toMutableList().apply {
            while (this.size != this.distinct().size) {
                this.groupingBy { it }.eachCount().filter { it.value > 1 }.forEach { repeated ->
                    var occurrence = 1
                    this.forEachIndexed { index, singleName ->
                        if (singleName == repeated.key) {
                            if (occurrence > 1) this[index] += "_${occurrence}"
                            occurrence++
                        }
                    }
                }
            }
        }.toList()
    }

    private fun populateFile(server: GattServer) {
        serializer.startDocument("UTF-8", null)
        serializer.text("\n")

        server.let {
            populateRootElement(it)
            populateCapabilityDeclaration(it)
            populateServices(it)
        }

        printer.closeTag(XmlConst.gatt)
        server.importedData.device?.let {
            printer.closeTag(XmlConst.project)
        }
        serializer.endDocument()
    }

    private fun populateRootElement(server: GattServer) {
        server.importedData.device?.let {
            printer.openTag(XmlConst.project, mapOf("device" to it), breakLine = true)
        }
        printer.openTag(XmlConst.gatt, getGattAttributes(server), breakLine = true)
        serializer.text("\n")
    }

    private fun populateCapabilityDeclaration(server: GattServer) {
        server.importedData.capabilities.let {
            if (it.isNotEmpty()) {
                printer.openTag(XmlConst.capabilities_declare, breakLine = true, increaseIndent = true)
                it.forEach { entry ->
                    printer.openTag(XmlConst.capability, mapOf(XmlConst.enable to entry.value), entry.key)
                    printer.closeTag(XmlConst.capability)
                }
                printer.closeTag(XmlConst.capabilities_declare, decreaseIndent = true)
            }
        }
    }

    private fun populateServices(server: GattServer) {
        serializer.text("\n")
        server.services.forEach {
            printer.openTag(XmlConst.service, getServiceAttributes(it), breakLine = true, increaseIndent = true)

            populateAdditionalServiceElements(it)
            populateCharacteristics(it)

            printer.closeTag(XmlConst.service, decreaseIndent = true)
        }
    }

    private fun populateAdditionalServiceElements(service: Service) {
        service.importedData.let {
            populateCapabilityList(it.capabilities)
            populateSimpleElements(it.simpleElements)
            it.include.forEach { entry ->
                printer.openTag(XmlConst.include, mapOf("id" to entry.key, "sourceId" to entry.value))
                printer.closeTag(XmlConst.include)
            }
        }
    }

    private fun populateCharacteristics(service: Service) {
        service.characteristics.forEach {
            printer.openTag(XmlConst.characteristic, getCharacteristicAttributes(it),
                    breakLine = true, increaseIndent = true)

            populateAdditionalCharacteristicElements(it)
            populateProperties(it.importedData.propertiesAttributes, it.properties)
            populateValue(it.value)
            populateDescriptors(it)

            printer.closeTag(XmlConst.characteristic, decreaseIndent = true)
        }
    }

    private fun populateAdditionalCharacteristicElements(characteristic: Characteristic) {
        characteristic.importedData.let {
            populateCapabilityList(it.capabilities)
            populateSimpleElements(it.simpleElements)
            populateAggregate(it.aggregate)
        }
    }

    private fun populateDescriptors(characteristic: Characteristic) {
        characteristic.descriptors.forEach {
            printer.openTag(XmlConst.descriptor, getDescriptorAttributes(it),
                    breakLine = true, increaseIndent = true)

            populateSimpleElements(it.importedData.simpleElements)
            populateProperties(it.importedData.propertiesAttributes, it.properties)
            populateValue(it.value)

            printer.closeTag(XmlConst.descriptor, decreaseIndent = true)
        }
    }

    private fun populateSimpleElements(map: Map<String, String>) {
        map.forEach {
            printer.openTag(it.key, tagValue = it.value)
            printer.closeTag(it.key)
        }
    }

    private fun populateAggregate(aggregate: MutableList<String?>) {
        if (aggregate.isNotEmpty()) {
            aggregate.let {
                it[0]?.let { aggregateAttribute ->
                    printer.openTag(XmlConst.aggregate, mapOf("id" to aggregateAttribute),
                            breakLine = true, increaseIndent = true)
                } ?: printer.openTag(XmlConst.aggregate, breakLine = true, increaseIndent = true)

                for (i in 1 until it.size) {
                    printer.openTag(XmlConst.attribute, mapOf("id" to it[i]!!))
                    printer.closeTag(XmlConst.attribute)
                }
                printer.closeTag(XmlConst.aggregate, decreaseIndent = true)
            }
        }
    }

    private fun populateValue(value: Value?) {
        value?.let {
            if (it.value.isNotBlank()) {
                printer.openTag(XmlConst.value,
                        mapOf(
                            "type" to XmlConverter.fromValueType(it.type!!),
                            "length" to it.length.toString(),
                            "variable_length" to it.variableLength.toString()),
                        it.value)
                printer.closeTag(XmlConst.value)
            }
        }
    }

    private fun populateProperties(
            attributes: Map<String, String>?,
            properties: Map<Property, HashSet<Property.Type>>) {
        printer.openTag(XmlConst.properties, attributes, breakLine = true, increaseIndent = true)

        properties.forEach {
            printer.openTag(
                    XmlConverter.fromProperty(it.key),
                    mapProperties(it.value)
            )
            printer.closeTag(XmlConverter.fromProperty(it.key))
        }
        printer.closeTag(XmlConst.properties, decreaseIndent = true)
    }

    private fun populateCapabilityList(values: Set<String>) {
        if (values.isNotEmpty()) {
            printer.openTag(XmlConst.capabilities, breakLine = true, increaseIndent = true)
            values.forEach {
                printer.openTag(XmlConst.capability, tagValue = it)
                printer.closeTag(XmlConst.capability)
            }
            printer.closeTag(XmlConst.capabilities, decreaseIndent = true)
        }
    }

    private fun getGattAttributes(server: GattServer) : Map<String, String> {
        val allAttributes = joinAttributes(
                XmlConst.defaultGattAttributes.toMutableMap(),
                server.importedData.gattAttributes
        )
        server.importedData.gattAttributes = joinAttributes(
                allAttributes.toMutableMap(),
                mapOf("name" to server.name)
        )
        return server.importedData.gattAttributes
    }

    private fun getServiceAttributes(service: Service) : Map<String, String> {
        return joinAttributes(
                mutableMapOf(
                        "name" to service.name,
                        "uuid" to service.uuid!!.getAsFormattedText(false),
                        "type" to XmlConverter.fromServiceType(service.type)
                ),
                service.importedData.attributes
        )
    }

    private fun getCharacteristicAttributes(characteristic: Characteristic) : Map<String, String> {
        return joinAttributes(
                mutableMapOf(
                        "name" to characteristic.name,
                        "uuid" to characteristic.uuid!!.getAsFormattedText(false)
                ),
                characteristic.importedData.attributes
        )
    }

    private fun getDescriptorAttributes(descriptor: Descriptor) : Map<String, String> {
        return joinAttributes(
                mutableMapOf(
                        "name" to descriptor.name,
                        "uuid" to descriptor.uuid!!.getAsFormattedText(false)
                ),
                descriptor.importedData.attributes
        )
    }

    private fun joinAttributes(
            mandatory: MutableMap<String, String>,
            optional: Map<String, String>) : Map<String, String>{
        mandatory.putAll(optional)
        return mandatory.toMap()
    }

    private fun mapProperties(set: HashSet<Property.Type>?) : Map<String, String> {
        set?.let {
            return mapOf(
                "authenticated" to it.contains(Property.Type.AUTHENTICATED).toString(),
                "bonded" to it.contains(Property.Type.BONDED).toString(),
                "encrypted" to it.contains(Property.Type.ENCRYPTED).toString()
            )
        } ?:
            return mapOf(
                "authenticated" to false.toString(),
                "bonded" to false.toString(),
                "encrypted" to false.toString()
            )
    }

}