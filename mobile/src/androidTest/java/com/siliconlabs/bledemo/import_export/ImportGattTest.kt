package com.siliconlabs.bledemo.import_export

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.ImportException
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.ImportException.ErrorType.*
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.utils.XmlConst
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class ImportGattTest : ImportBaseTest() {

    //region General XML syntax

    @Test
    fun validateXmlSyntax_noStartTag_error() {
        serializer.text("definitely not a root tag")
        readForError(ImportException(PARSING_ERROR))
    }

    @Test
    fun validateXmlSyntax_endTagMissing_error() {
        printer.openTag(XmlConst.gatt, mapOf("out" to "value1"))
        readForError(ImportException(PARSING_ERROR))
    }

    @Test
    fun validateXmlSyntax_attributeValueMissing_error() {
        serializer.flush()
        writer.append("<gatt attr=></gatt>")
        writer.close()
        readForError(ImportException(PARSING_ERROR))
    }

    @Test
    fun validateXmlSyntax_wrongTagEnding_error() {
        serializer.flush()
        writer.append("<gatt></weird_tag>")
        writer.close()
        readForError(ImportException(PARSING_ERROR))
    }

    //endregion
    //region General tag errors

    @Test
    fun validateRootTag_wrongName_error() {
        printer.openTag("weird_tag")
        printer.closeTag( "weird_tag")
        readForError(ImportException(WRONG_TAG_NAME, "weird_tag", XmlConst.rootElements))
    }

    @Test
    fun validateTag_wrongName_error() {
        printer.openTag(XmlConst.gatt)
        printer.openTag("capabilities_declareee")
        printer.closeTag("capabilities_declareee")
        printer.closeTag(XmlConst.gatt)
        readForError(ImportException(WRONG_TAG_NAME, "capabilities_declareee", XmlConst.gattElements))
    }

    @Test
    fun validateTag_mandatoryAttributeMissing_error() {
        printer.openTag(XmlConst.gatt)
        printer.openTag(XmlConst.service)
        printer.closeTag(XmlConst.service)
        printer.closeTag(XmlConst.gatt)
        readForError(ImportException(MANDATORY_ATTRIBUTE_MISSING, "uuid"))
    }

    @Test
    fun validateTag_valueInsteadOfNestedTag_error() {
        printer.openTag(XmlConst.gatt, tagValue = "unexpectedValue")
        printer.closeTag(XmlConst.gatt)
        readForError(ImportException(NESTED_TAG_EXPECTED))
    }

    @Test
    fun validateAttribute_wrongName() {
        printer.openTag(XmlConst.gatt, mapOf("randomAttName" to "attValue"))
        printer.closeTag(XmlConst.gatt)
        readForError(ImportException(WRONG_ATTRIBUTE_NAME, "randomAttName", XmlConst.gattAttributes))
    }

    @Test
    fun validateAttribute_nameDuplicated_error() {
        serializer.flush()
        writer.append("<gatt header=\"header1\" header=\"header2\"></gatt>")
        writer.close()
        readForError(ImportException(ATTRIBUTE_NAME_DUPLICATED, "header"))
    }

    @Test
    fun validateValue_emptyValue_success() {
        printer.openTag(XmlConst.gatt)
        printer.openTag(XmlConst.service, mapOf("uuid" to "1234"))
        printer.openTag(XmlConst.informativeText)
        printer.closeTag(XmlConst.informativeText)
        printer.closeTag(XmlConst.service)
        printer.closeTag(XmlConst.gatt)
        val retrievedServer = readForSuccess()
        assertEquals(1, retrievedServer.services.size)
        assertEquals("", retrievedServer.services[0].importedData.simpleElements["informativeText"])
    }

    //endregion
    //region Tag-specific errors

    @Test
    fun projectTag_parseWhenExists_defaultAttributeValue_success() {
        printer.openTag(XmlConst.project)
        printer.openTag(XmlConst.gatt)
        printer.closeTag(XmlConst.gatt)
        printer.closeTag(XmlConst.project)
        val retrievedServer = readForSuccess()
        assertNull(retrievedServer.importedData.device)
        assertEquals(XmlConst.defaultGattAttributes, retrievedServer.importedData.gattAttributes)
    }

    @Test
    fun projectTag_parseWhenExists_customAttributeValue_success() {
        printer.openTag(XmlConst.project, mapOf("device" to "thisIsDeviceName"))
        printer.openTag(XmlConst.gatt)
        printer.closeTag(XmlConst.gatt)
        printer.closeTag(XmlConst.project)
        val retrievedServer = readForSuccess()
        assertEquals("thisIsDeviceName", retrievedServer.importedData.device)
        assertEquals(XmlConst.defaultGattAttributes, retrievedServer.importedData.gattAttributes)
    }

    @Test
    fun gattTag_gattCachingAttribute_wrongValue_error() {
        printer.openTag(XmlConst.gatt, mapOf("gatt_caching" to "notBoolean"))
        printer.closeTag(XmlConst.gatt)
        readForError(ImportException(WRONG_ATTRIBUTE_VALUE, "notBoolean", XmlConst.booleanValues))
    }

    @Test
    fun gattTag_genericAttributeServiceAttribute_wrongValue_error() {
        printer.openTag(XmlConst.gatt, mapOf("generic_attribute_service" to "notBoolean"))
        printer.closeTag(XmlConst.gatt)
        readForError(ImportException(WRONG_ATTRIBUTE_VALUE, "notBoolean", XmlConst.booleanValues))
    }

    @Test
    fun gattTag_defaultAttributeValues_success() {
        printer.openTag(XmlConst.gatt)
        printer.closeTag(XmlConst.gatt)
        val retrievedServer = readForSuccess()
        assertEquals(XmlConst.defaultServerName, retrievedServer.name)
        assertEquals(XmlConst.defaultGattAttributes,
                retrievedServer.importedData.gattAttributes)
    }

    @Test
    fun gattTag_customAttributeValues_success() {
        printer.openTag(XmlConst.gatt, mapOf("name" to "value1", "prefix" to "value2", "id" to "value3"))
        printer.closeTag(XmlConst.gatt)
        val retrievedServer = readForSuccess()
        assertEquals("value1", retrievedServer.name)
        assertEquals(mapOf(
                "out" to "gatt_db.c",
                "header" to "gatt_db.h",
                "name" to "value1",
                "prefix" to "value2",
                "generic_attribute_service" to "false",
                "id" to "value3"),
                retrievedServer.importedData.gattAttributes)
    }

    @Test
    fun capabilitiesDeclareTag_defaultAttributeValues_success() {
        testCapabilitiesDeclare {
            printer.openTag(XmlConst.capability, tagValue = "capability1")
            printer.closeTag(XmlConst.capability)
        }
        val retrievedServer = readForSuccess()
        assertEquals(1, retrievedServer.importedData.capabilities.size)
        assertTrue(retrievedServer.importedData.capabilities.containsKey("capability1"))
        assertEquals("true", retrievedServer.importedData.capabilities["capability1"])
    }

    @Test
    fun capabilitiesDeclareTag_wrongAttributeValue_error() {
        testCapabilitiesDeclare {
            printer.openTag(XmlConst.capability, mapOf("enable" to "notBoolean"), "cap1")
            printer.closeTag(XmlConst.capability)
        }
        readForError(ImportException(WRONG_ATTRIBUTE_VALUE, "notBoolean", XmlConst.booleanValues))
    }

    @Test
    fun capabilitiesDeclareTag_wrongTagValue_emptyValue_error() {
        testCapabilitiesDeclare {
            printer.openTag(XmlConst.capability, tagValue = "")
            printer.closeTag(XmlConst.capability)
        }
        readForError(ImportException(WRONG_TAG_VALUE, "", setOf(XmlConst.capabilityNameRegex.toString())))
    }

    @Test
    fun capabilitiesDeclareTag_wrongTagValue_specialCharacter_error() {
        testCapabilitiesDeclare {
            printer.openTag(XmlConst.capability, tagValue = "f$3dws")
            printer.closeTag(XmlConst.capability)
        }
        readForError(ImportException(WRONG_TAG_VALUE, "f$3dws", setOf(XmlConst.capabilityNameRegex.toString())))
    }

    @Test
    fun capabilitiesDeclareTag_wrongTagValue_startedWithDigit_error() {
        testCapabilitiesDeclare {
            printer.openTag(XmlConst.capability, tagValue = "1toEg")
            printer.closeTag(XmlConst.capability)
        }
        readForError(ImportException(WRONG_TAG_VALUE, "1toEg", setOf(XmlConst.capabilityNameRegex.toString())))
    }

    @Test
    fun capabilitiesDeclareTag_correctTagValue_success() {
        testCapabilitiesDeclare {
            printer.openTag(XmlConst.capability, tagValue = "_PDiu8y")
            printer.closeTag(XmlConst.capability)
        }
        val retrievedServer = readForSuccess()
        assertEquals(1, retrievedServer.importedData.capabilities.size)
        assertTrue(retrievedServer.importedData.capabilities.containsKey("_PDiu8y"))
    }

    @Test
    fun capabilitiesDeclareTag_noCapabilitiesDeclared_error() {
        testCapabilitiesDeclare { }
        readForError(ImportException(NO_CAPABILITIES_DECLARED))
    }

    @Test
    fun capabilitiesDeclareTag_tooManyCapabilities_error() {
        testCapabilitiesDeclare {
            for (i in 0 until 20) {
                printer.openTag(XmlConst.capability, tagValue = "cap_$i")
                printer.closeTag(XmlConst.capability)
            }
        }
        readForError(ImportException(TOO_MANY_CAPABILITIES_DECLARED))
    }

    @Test
    fun capabilitiesDeclareTag_secondOccurrence_error() {
        printer.openTag(XmlConst.gatt)
        printer.openTag(XmlConst.capabilities_declare)
        printer.openTag(XmlConst.capability, tagValue = "aCapability")
        printer.closeTag(XmlConst.capability)
        printer.closeTag(XmlConst.capabilities_declare)
        printer.openTag(XmlConst.capabilities_declare)
        printer.closeTag(XmlConst.capabilities_declare)
        printer.closeTag(XmlConst.gatt)
        readForError(ImportException(TAG_MAXIMUM_OCCURRENCE_EXCEEDED, XmlConst.capabilities_declare))
    }

    @Test
    fun capabilitiesDeclareTag_secondOccurrence_afterServiceTag_error() {
        printer.openTag(XmlConst.gatt)

        printer.openTag(XmlConst.capabilities_declare)
        printer.openTag(XmlConst.capability, tagValue = "aCapability")
        printer.closeTag(XmlConst.capability)
        printer.closeTag(XmlConst.capabilities_declare)

        printer.openTag(XmlConst.service, mapOf("uuid" to "1721"))
        printer.closeTag(XmlConst.service)

        printer.openTag(XmlConst.capabilities_declare)
        printer.openTag(XmlConst.capability, tagValue = "bCapability")
        printer.closeTag(XmlConst.capability)
        printer.closeTag(XmlConst.capabilities_declare)

        printer.closeTag(XmlConst.gatt)
        readForError(ImportException(TAG_MAXIMUM_OCCURRENCE_EXCEEDED, XmlConst.capabilities_declare))
    }

    @Test
    fun capabilitiesDeclareTag_betweenServices_success() {
        printer.openTag(XmlConst.gatt)

        printer.openTag(XmlConst.service, mapOf("uuid" to "1721"))
        printer.closeTag(XmlConst.service)

        printer.openTag(XmlConst.capabilities_declare)
        printer.openTag(XmlConst.capability, tagValue = "aCapability")
        printer.closeTag(XmlConst.capability)
        printer.closeTag(XmlConst.capabilities_declare)

        printer.openTag(XmlConst.service, mapOf("uuid" to "1722"))
        printer.closeTag(XmlConst.service)

        printer.closeTag(XmlConst.gatt)
        val retrievedServer = readForSuccess()
        assertTrue(retrievedServer.importedData.capabilities.containsKey("aCapability"))
    }

    //endregion

    private fun testCapabilitiesDeclare(test: () -> Unit) {
        printer.openTag(XmlConst.gatt)
        printer.openTag(XmlConst.capabilities_declare)
        test()
        printer.closeTag(XmlConst.capabilities_declare)
        printer.closeTag(XmlConst.gatt)
    }
}