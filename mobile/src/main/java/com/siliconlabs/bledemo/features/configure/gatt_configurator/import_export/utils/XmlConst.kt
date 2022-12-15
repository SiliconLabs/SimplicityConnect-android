package com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.utils

object XmlConst {
    /* Allowed tag names */
    const val project = "project"
    const val gatt = "gatt"
    const val capabilities_declare = "capabilities_declare"
    const val capability = "capability"
    const val service = "service"

    const val capabilities = "capabilities"
    const val informativeText = "informativeText"
    const val description = "description"
    const val uri = "uri"
    const val include = "include"
    const val characteristic = "characteristic"

    const val properties = "properties"
    const val value = "value"
    const val descriptor = "descriptor"
    const val aggregate = "aggregate"
    const val attribute = "attribute"

    const val read = "read"
    const val write = "write"
    const val write_no_response = "write_no_response"
    const val reliable_write = "reliable_write"
    const val indicate = "indicate"
    const val notify = "notify"

    /* Allowed tag elements */
    val rootElements = setOf(project, gatt)
    val projectElements = setOf(gatt)
    val gattElements = setOf(capabilities_declare, service)
    val capabilitiesElements = setOf(capability)
    val serviceElements = setOf(capabilities, informativeText, description, uri, include, characteristic)
    val characteristicElements = setOf(capabilities, properties, value, descriptor, aggregate)
    val descriptorElements = setOf(properties, value, informativeText)
    val characteristicPropertiesElements = setOf(read, write, write_no_response, reliable_write, indicate, notify)
    val descriptorPropertiesElements = setOf(read, write)
    val aggregateElements = setOf(attribute)



    /* Allowed attribute names */
    val projectAttributes = setOf("device")
    val gattAttributes = setOf("in", "out", "header", "prefix", "generic_attribute_service",
            "gatt_caching", "name", "id")
    const val enable = "enable"
    val serviceAttributes = setOf("uuid", "name", "type", "id", "sourceId", "requirement",
            "advertise", "instance_id")
    val characteristicAttributes = setOf("uuid", "name", "id", "sourceId", "const", "instance_id")
    val propertyAttributes = setOf("authenticated", "bonded", "encrypted")
    val propertiesAttributes = setOf("read", "const", "write", "write_no_response",
            "notify", "indicate", "authenticated_read", "bonded_read", "encrypted_read",
            "authenticated_write", "bonded_write", "encrypted_write", "reliable_write",
            "discoverable", "encrypted_notify", "authenticated_notify", "bonded_notify")
    val propertiesRequirementAttributes = setOf(
            "read_requirement", "const_requirement", "write_requirement", "write_no_response_requirement",
            "notify_requirement", "indicate_requirement", "authenticated_read_requirement",
            "bonded_read_requirement", "encrypted_read_requirement", "authenticated_write_requirement",
            "bonded_write_requirement", "encrypted_write_requirement", "reliable_write_requirement",
            "discoverable_requirement", "encrypted_notify_requirement", "authenticated_notify_requirement",
            "bonded_notify_requirement")
    val valueAttributes = setOf("type", "length", "variable_length")
    val aggregateAttributes = setOf("id")
    val descriptorAttributes = setOf("uuid", "name", "id", "sourceId", "const", "discoverable", "instance_id")


    /* Allowed attribute values */
    val booleanValues = setOf("true", "false")
    val serviceTypeValues = setOf("primary, secondary")
    val requirementValues = setOf("mandatory", "optional", "conditional", "c1", "c2", "c2_or_c3",
            "c3", "c4", "c5", "c6", "c7", "c8", "c9", "c10", "c11", "c12", "c13", "c14", "c15",
            "c16", "c17", "c18", "c19", "c20")
    val propertiesRequirementValues = setOf("mandatory", "optional", "excluded", "c1", "c2",  "c3",
            "c4", "c5", "c6", "c7", "c8", "c9", "c10")
    val valueTypeValues = setOf("utf-8", "hex", "user")


    /* Default attribute values */
    const val defaultServerName = "Custom BLE GATT"
    val defaultGattAttributes = mapOf(
            "out" to "gatt_db.c",
            "header" to "gatt_db.h",
            "name" to "Custom BLE GATT",
            "prefix" to "gattdb_",
            "generic_attribute_service" to "false"
    )
    const val defaultServiceName = "Unknown service"
    const val defaultCharacteristicName = "Unknown characteristic"
    const val defaultDescriptorName = "Unknown descriptor"

    /* One time elements of complex tags */
    val gattOneTimeElements = setOf(capabilities_declare)
    val serviceOneTimeElements = setOf(capabilities, informativeText, description, uri)
    val characteristicOneTimeElements = setOf(capabilities, informativeText, aggregate, value,
            properties, description)
    val descriptorOneTimeElements = setOf(properties, value, informativeText)
    val propertiesOneTimeElements = setOf(read, write, write_no_response, reliable_write, indicate, notify)

    /* Numbers */
    const val MIN_CAPABILITIES_NUMBER = 1
    const val MAX_CAPABILITIES_NUMBER = 16
    val capabilityNameRegex = Regex("([a-zA-Z_])([0-9a-zA-Z_]*)")
    val hexValueRegex = Regex("[0-9a-fA-F]*")
}