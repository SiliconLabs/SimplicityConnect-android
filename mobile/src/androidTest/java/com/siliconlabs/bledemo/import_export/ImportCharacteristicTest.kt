package com.siliconlabs.bledemo.import_export

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.Property
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.Value
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.ImportException
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.ImportException.ErrorType.*
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.utils.XmlConst
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class ImportCharacteristicTest : ImportBaseTest() {
    
    private val exampleUuid = "45BE"

    @Test
    fun uuidMissing_error() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic)
            printer.closeTag(XmlConst.characteristic)
        }
        readForError(ImportException(MANDATORY_ATTRIBUTE_MISSING, "uuid"))
    }

    @Test
    fun constAttribute_wrongValue_error() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid, "const" to "notBoolean"))
            printer.closeTag(XmlConst.characteristic)
        }
        readForError(ImportException(WRONG_ATTRIBUTE_VALUE, "notBoolean", XmlConst.booleanValues
        ))
    }

    @Test
    fun nameAttribute_defaultValue_success() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printDefaultProperties()
            printer.closeTag(XmlConst.characteristic)
        }
        val retrievedServer = readForSuccess()
        assertEquals(1, retrievedServer.services.size)
        assertEquals(1, retrievedServer.services[0].characteristics.size)
        assertEquals(XmlConst.defaultCharacteristicName,
                retrievedServer.services[0].characteristics[0].name)
    }
    
    @Test
    fun sigUuidFound_renameCharacteristic_success() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to "2a19"))
            printDefaultProperties()
            printer.closeTag(XmlConst.characteristic)
        }
        val retrievedServer = readForSuccess()
        assertEquals(1, retrievedServer.services[0].characteristics.size)
        assertEquals("Battery Level", retrievedServer.services[0].characteristics[0].name)
    }

    @Test
    fun allCapabilitiesInherited_wrongCapabilityListed_error() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printDefaultProperties()
            printer.openTag(XmlConst.capabilities)
            printer.openTag(XmlConst.capability, tagValue = "definitely not cap_1")
            printer.closeTag(XmlConst.capability)
            printer.closeTag(XmlConst.capabilities)
            printer.closeTag(XmlConst.characteristic)
        }
        readForError(ImportException(WRONG_CAPABILITY_LISTED, "definitely not cap_1", setOf("cap_1", "cap_2")))
    }

    @Test
    fun allCapabilitiesInherited_correctCapabilityListed_success() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printDefaultProperties()
            printer.openTag(XmlConst.capabilities)
            printer.openTag(XmlConst.capability, tagValue = "cap_1")
            printer.closeTag(XmlConst.capability)
            printer.closeTag(XmlConst.capabilities)
            printer.closeTag(XmlConst.characteristic)
        }
        val retrievedServer = readForSuccess()
        assertEquals(1, retrievedServer.services.size)
        assertEquals(1, retrievedServer.services[0].characteristics.size)
        Assert.assertTrue(retrievedServer.services[0].characteristics[0]
                .importedData.capabilities.contains("cap_1"))
    }

    @Test
    fun subsetOfCapabilitiesInherited_wrongCapabilityListed_error() {
        testCharacteristic_withServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printDefaultProperties()
            printer.openTag(XmlConst.capabilities)
            printer.openTag(XmlConst.capability, tagValue = "cap_2") /* Declared but not by a service */
            printer.closeTag(XmlConst.capability)
            printer.closeTag(XmlConst.capabilities)
            printer.closeTag(XmlConst.characteristic)
        }
        readForError(ImportException(WRONG_CAPABILITY_LISTED, "cap_2", setOf("cap_1")))
    }

    @Test
    fun subsetOfCapabilitiesInherited_correctCapabilityListed_success() {
        testCharacteristic_withServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printDefaultProperties()
            printer.openTag(XmlConst.capabilities)
            printer.openTag(XmlConst.capability, tagValue = "cap_1")
            printer.closeTag(XmlConst.capability)
            printer.closeTag(XmlConst.capabilities)
            printer.closeTag(XmlConst.characteristic)
        }
        val retrievedServer = readForSuccess()
        assertEquals(1, retrievedServer.services.size)
        assertEquals(1, retrievedServer.services[0].characteristics.size)
        Assert.assertTrue(
                retrievedServer.services[0].characteristics[0].importedData.capabilities.contains(
                        "cap_1"))
    }

    @Test
    fun simpleElementSecondOccurrence_error() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printDefaultProperties()
            printer.openTag(XmlConst.description, tagValue = "descriptionOne")
            printer.closeTag(XmlConst.description)
            printer.openTag(XmlConst.description, tagValue = "descriptionTwo")
            printer.closeTag(XmlConst.description)
            printer.closeTag(XmlConst.characteristic)
        }
        readForError(ImportException(TAG_MAXIMUM_OCCURRENCE_EXCEEDED, XmlConst.description))
    }

    @Test
    fun propertiesSecondOccurrence_error() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printDefaultProperties()
            printDefaultProperties()
            printer.closeTag(XmlConst.characteristic)
        }
        readForError(ImportException(TAG_MAXIMUM_OCCURRENCE_EXCEEDED, XmlConst.properties))
    }

    @Test
    fun capabilitiesSecondOccurrence_error() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printDefaultProperties()
            printer.openTag(XmlConst.capabilities)
            printer.openTag(XmlConst.capability, tagValue = "cap_1")
            printer.closeTag(XmlConst.capability)
            printer.closeTag(XmlConst.capabilities)

            printer.openTag(XmlConst.capabilities)
            printer.openTag(XmlConst.capability, tagValue = "cap_1")
            printer.closeTag(XmlConst.capability)
            printer.closeTag(XmlConst.capabilities)
            printer.closeTag(XmlConst.characteristic)
        }
        readForError(ImportException(TAG_MAXIMUM_OCCURRENCE_EXCEEDED, XmlConst.capabilities))
    }

    @Test
    fun propertiesTag_propertySecondOccurrence_error() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printer.openTag(XmlConst.properties)
            printer.openTag(XmlConst.read)
            printer.closeTag(XmlConst.read)
            printer.openTag(XmlConst.read)
            printer.closeTag(XmlConst.read)
            printer.closeTag(XmlConst.properties)
            printer.closeTag(XmlConst.characteristic)
        }
        readForError(ImportException(TAG_MAXIMUM_OCCURRENCE_EXCEEDED, XmlConst.read
        ))
    }

    @Test
    fun noPropertiesDeclared_error() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printer.closeTag(XmlConst.characteristic)
        }
        readForError(ImportException(NO_PROPERTIES_DECLARED))
    }

    @Test
    fun propertiesTag_noPropertyDeclared_error() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printer.openTag(XmlConst.properties)
            printer.closeTag(XmlConst.properties)
            printer.closeTag(XmlConst.characteristic)
        }
        readForError(ImportException(NO_PROPERTIES_DECLARED))
    }

    @Test
    fun propertiesDeclaredThroughAttributes_success() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printer.openTag(XmlConst.properties,
                    mapOf("authenticated_read" to "true", "bonded_read" to "true",
                            "encrypted_read" to "false", "write" to "true"))
            printer.closeTag(XmlConst.properties)
            printer.closeTag(XmlConst.characteristic)
        }
        val retrievedServer = readForSuccess()
        assertEquals(1, retrievedServer.services.size)
        assertEquals(1, retrievedServer.services[0].characteristics.size)
        assertEquals(2, retrievedServer.services[0].characteristics[0].properties.size)
        Assert.assertTrue(retrievedServer.services[0].characteristics[0].properties.containsKey(
                Property.READ))
        Assert.assertTrue(retrievedServer.services[0].characteristics[0].properties.containsKey(
                Property.WRITE))
        assertEquals(setOf(Property.Type.AUTHENTICATED, Property.Type.BONDED),
                retrievedServer.services[0].characteristics[0].properties[Property.READ])
        assertEquals(emptySet<Property.Type>(),
                retrievedServer.services[0].characteristics[0].properties[Property.WRITE])
    }

    @Test
    fun propertiesTag_propertyAttributes_wrongAttributeValue_error() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printer.openTag(XmlConst.properties, mapOf("authenticated_read" to "notBoolean"))
            printer.closeTag(XmlConst.properties)
            printer.closeTag(XmlConst.characteristic)
        }
        readForError(ImportException(WRONG_ATTRIBUTE_VALUE, "notBoolean", XmlConst.booleanValues
        ))
    }

    @Test
    fun propertiesTag_requirementAttributes_wrongAttributeValue_error() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printer.openTag(XmlConst.properties, mapOf("read_requirement" to "true"))
            printer.closeTag(XmlConst.properties)
            printer.closeTag(XmlConst.characteristic)
        }
        readForError(ImportException(WRONG_ATTRIBUTE_VALUE, "true", XmlConst.propertiesRequirementValues
        ))
    }

    @Test
    fun valueTag_typeAttribute_wrongValue_error() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printDefaultProperties()
            printer.openTag(XmlConst.value, mapOf("type" to "weirdType"))
            printer.closeTag(XmlConst.value)
            printer.closeTag(XmlConst.characteristic)
        }
        readForError(ImportException(WRONG_ATTRIBUTE_VALUE, "weirdType", XmlConst.valueTypeValues
        ))
    }

    @Test
    fun valueTag_variableLengthAttribute_wrongValue_error() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printDefaultProperties()
            printer.openTag(XmlConst.value, mapOf("variable_length" to "notBoolean"))
            printer.closeTag(XmlConst.value)
            printer.closeTag(XmlConst.characteristic)
        }
        readForError(ImportException(WRONG_ATTRIBUTE_VALUE, "notBoolean", XmlConst.booleanValues
        ))
    }

    @Test
    fun valueTag_defaultAttributeValues_success() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printDefaultProperties()
            printer.openTag(XmlConst.value, tagValue = "someValue")
            printer.closeTag(XmlConst.value)
            printer.closeTag(XmlConst.characteristic)
        }
        val retrievedServer = readForSuccess()
        assertEquals(Value.Type.UTF_8,
                retrievedServer.services[0].characteristics[0].value!!.type)
        assertEquals(0, retrievedServer.services[0].characteristics[0].value!!.length)
        assertEquals(false,
                retrievedServer.services[0].characteristics[0].value!!.variableLength)
        assertEquals("someValue",
                retrievedServer.services[0].characteristics[0].value!!.value)
    }

    @Test
    fun valueTag_emptyValue_success() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printDefaultProperties()
            printer.openTag(XmlConst.value)
            printer.closeTag(XmlConst.value)
            printer.closeTag(XmlConst.characteristic)
        }
        val retrievedServer = readForSuccess()
        assertEquals("", retrievedServer.services[0].characteristics[0].value!!.value)
    }

    @Test
    fun valueTag_typeHex_wrongValue_error() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printDefaultProperties()
            printer.openTag(XmlConst.value, mapOf("type" to "hex"), tagValue = "DER")
            printer.closeTag(XmlConst.value)
            printer.closeTag(XmlConst.characteristic)
        }
        readForError(ImportException(WRONG_TAG_VALUE, "DER", setOf(XmlConst.hexValueRegex.toString())))
    }

    @Test
    fun valueTag_typeHex_correctValue_error() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printDefaultProperties()
            printer.openTag(XmlConst.value, mapOf("type" to "hex"), tagValue = "a92FE0")
            printer.closeTag(XmlConst.value)
            printer.closeTag(XmlConst.characteristic)
        }
        val retrievedServer = readForSuccess()
        assertEquals("a92FE0", retrievedServer.services[0].characteristics[0].value?.value)
    }

    @Test
    fun aggregateTag_withoutAttribute_success() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printDefaultProperties()
            printer.openTag(XmlConst.aggregate)
            printer.closeTag(XmlConst.aggregate)
            printer.closeTag(XmlConst.characteristic)
        }
        val retrievedServer = readForSuccess()
        assertEquals(1,
                retrievedServer.services[0].characteristics[0].importedData.aggregate.size)
        Assert.assertNull(retrievedServer.services[0].characteristics[0].importedData.aggregate[0])
    }

    @Test
    fun aggregateTag_attributeAttributeMissing_error() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printDefaultProperties()
            printer.openTag(XmlConst.aggregate)
            printer.openTag(XmlConst.attribute)
            printer.closeTag(XmlConst.attribute)
            printer.closeTag(XmlConst.aggregate)
            printer.closeTag(XmlConst.characteristic)
        }
        readForError(ImportException(
                MANDATORY_ATTRIBUTE_MISSING,
                "id"
        ))
    }

    @Test
    fun aggregateTag_parseAttributeTags_success() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printDefaultProperties()
            printer.openTag(XmlConst.aggregate, mapOf("id" to "aggAtt"))
            printer.openTag(XmlConst.attribute, mapOf("id" to "att1"))
            printer.closeTag(XmlConst.attribute)
            printer.openTag(XmlConst.attribute, mapOf("id" to "att2"))
            printer.closeTag(XmlConst.attribute)
            printer.closeTag(XmlConst.aggregate)
            printer.closeTag(XmlConst.characteristic)
        }
        val retrievedServer = readForSuccess()
        assertEquals(3,
                retrievedServer.services[0].characteristics[0].importedData.aggregate.size)
        assertEquals("aggAtt",
                retrievedServer.services[0].characteristics[0].importedData.aggregate[0])
        assertEquals("att1",
                retrievedServer.services[0].characteristics[0].importedData.aggregate[1])
        assertEquals("att2",
                retrievedServer.services[0].characteristics[0].importedData.aggregate[2])
    }

    @Test
    fun oneTimeCharacteristicElements_notReset_afterComplexTagWithOneTimeElements_secondOccurrence_error() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printer.openTag(XmlConst.informativeText, tagValue = "text1")
            printer.closeTag(XmlConst.informativeText)
            printDefaultProperties()
            printer.openTag(XmlConst.descriptor, mapOf("uuid" to exampleUuid))
            printDefaultProperties()
            printer.closeTag(XmlConst.descriptor)
            printer.openTag(XmlConst.informativeText, tagValue = "text2")
            printer.closeTag(XmlConst.informativeText)
            printer.closeTag(XmlConst.characteristic)
        }
        readForError(ImportException(TAG_MAXIMUM_OCCURRENCE_EXCEEDED, XmlConst.informativeText))
    }

    @Test
    fun oneTimeCharacteristicElements_reset_forNextCharacteristic_success() {
        testCharacteristic_withoutServiceCapabilitiesDeclared {
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to exampleUuid))
            printer.openTag(XmlConst.informativeText, tagValue = "text1")
            printer.closeTag(XmlConst.informativeText)
            printDefaultProperties()
            printer.closeTag(XmlConst.characteristic)

            printer.openTag(XmlConst.characteristic, mapOf("uuid" to "2A91"))
            printer.openTag(XmlConst.informativeText, tagValue = "text2")
            printer.closeTag(XmlConst.informativeText)
            printDefaultProperties()
            printer.closeTag(XmlConst.characteristic)
        }
        val retrievedServer = readForSuccess()
        assertEquals(2, retrievedServer.services[0].characteristics.size)
        assertEquals("text1", retrievedServer.services[0].characteristics[0]
                .importedData.simpleElements["informativeText"])
        assertEquals("text2", retrievedServer.services[0].characteristics[1]
                .importedData.simpleElements["informativeText"])
    }



    private fun testCharacteristic_withServiceCapabilitiesDeclared(test: () -> Unit) {
        printer.openTag(XmlConst.gatt)
        printer.openTag(XmlConst.capabilities_declare)
        printer.openTag(XmlConst.capability, tagValue = "cap_1")
        printer.closeTag(XmlConst.capability)
        printer.openTag(XmlConst.capability, tagValue = "cap_2")
        printer.closeTag(XmlConst.capability)
        printer.closeTag(XmlConst.capabilities_declare)
        printer.openTag(XmlConst.service, mapOf("uuid" to "1721"))
        printer.openTag(XmlConst.capabilities)
        printer.openTag(XmlConst.capability, tagValue = "cap_1") /* Subset chosen */
        printer.closeTag(XmlConst.capability)
        printer.closeTag(XmlConst.capabilities)
        test()
        printer.closeTag(XmlConst.service)
        printer.closeTag(XmlConst.gatt)
    }

    private fun testCharacteristic_withoutServiceCapabilitiesDeclared(test: () -> Unit) {
        printer.openTag(XmlConst.gatt)
        printer.openTag(XmlConst.capabilities_declare)
        printer.openTag(XmlConst.capability, tagValue = "cap_1")
        printer.closeTag(XmlConst.capability)
        printer.openTag(XmlConst.capability, tagValue = "cap_2")
        printer.closeTag(XmlConst.capability)
        printer.closeTag(XmlConst.capabilities_declare)
        printer.openTag(XmlConst.service, mapOf("uuid" to "1721"))
        test()
        printer.closeTag(XmlConst.service)
        printer.closeTag(XmlConst.gatt)
    }


}