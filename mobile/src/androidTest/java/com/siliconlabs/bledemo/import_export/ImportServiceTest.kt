package com.siliconlabs.bledemo.import_export

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.Service
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.ImportException
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.ImportException.ErrorType.*
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.utils.XmlConst
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class ImportServiceTest : ImportBaseTest() {
    
    private val exampleUuid = "1328"

    @Test
    fun uuidMissing_error() {
        testService {
            printer.openTag(XmlConst.service)
            printer.closeTag(XmlConst.service)
        }
        readForError(ImportException(MANDATORY_ATTRIBUTE_MISSING, "uuid"))
    }

    @Test
    fun typeAttribute_wrongValue_error() {
        testService {
            printer.openTag(XmlConst.service, mapOf("uuid" to exampleUuid, "type" to "someType"))
            printer.closeTag(XmlConst.service)
        }
        readForError(ImportException(WRONG_ATTRIBUTE_VALUE, "someType", XmlConst.serviceTypeValues))
    }

    @Test
    fun requirementAttribute_wrongValue_error() {
        testService {
            printer.openTag(XmlConst.service, mapOf("uuid" to exampleUuid, "requirement" to "aWeirdReq"))
            printer.closeTag(XmlConst.service)
        }
        readForError(ImportException(WRONG_ATTRIBUTE_VALUE, "aWeirdReq", XmlConst.requirementValues))
    }

    @Test
    fun advertiseAttribute_wrongValue_error() {
        testService {
            printer.openTag(XmlConst.service, mapOf("uuid" to exampleUuid, "advertise" to "notBoolean"))
            printer.closeTag(XmlConst.service)
        }
        readForError(ImportException(WRONG_ATTRIBUTE_VALUE, "notBoolean", XmlConst.booleanValues))
    }

    @Test
    fun defaultAttributeValues_success() {
        testService {
            printer.openTag(XmlConst.service, mapOf("uuid" to exampleUuid))
            printer.closeTag(XmlConst.service)
        }
        val retrievedServer = readForSuccess()
        assertEquals(1, retrievedServer.services.size)
        assertEquals(XmlConst.defaultServiceName, retrievedServer.services[0].name)
        assertEquals(Service.Type.PRIMARY, retrievedServer.services[0].type)
    }

    @Test
    fun sigUuidFound_renameService_success() {
        testService {
            printer.openTag(XmlConst.service, mapOf("uuid" to "1810"))
            printer.closeTag(XmlConst.service)
        }
        val retrievedServer = readForSuccess()
        assertEquals(1, retrievedServer.services.size)
        assertEquals("Blood Pressure", retrievedServer.services[0].name)
    }

    @Test
    fun wrongCapabilityListed_error() {
        testService {
            printer.openTag(XmlConst.service, mapOf("uuid" to exampleUuid))
            printer.openTag(XmlConst.capabilities)
            printer.openTag(XmlConst.capability, tagValue = "definitely not cap_1")
            printer.closeTag(XmlConst.capability)
            printer.closeTag(XmlConst.capabilities)
            printer.closeTag(XmlConst.service)
        }
        readForError(ImportException(WRONG_CAPABILITY_LISTED, "definitely not cap_1", setOf("cap_1")))
    }

    @Test
    fun oneTimeElementSecondOccurrence_error() {
        testService {
            printer.openTag(XmlConst.service, mapOf("uuid" to exampleUuid))
            printer.openTag(XmlConst.uri, tagValue = "someUriValue")
            printer.closeTag(XmlConst.uri)
            printer.openTag(XmlConst.uri, tagValue = "differentUriValue")
            printer.closeTag(XmlConst.uri)
            printer.closeTag(XmlConst.service)
        }
        readForError(ImportException(TAG_MAXIMUM_OCCURRENCE_EXCEEDED, XmlConst.uri))
    }

    @Test
    fun includeTag_mandatoryAttributeMissing_error() {
        testService {
            printer.openTag(XmlConst.service, mapOf("uuid" to exampleUuid))
            printer.openTag(XmlConst.include, mapOf("id" to "someId"))
            printer.closeTag(XmlConst.include)
            printer.closeTag(XmlConst.service)
        }
        readForError(ImportException(MANDATORY_ATTRIBUTE_MISSING, "sourceId"))
    }

    @Test
    fun oneTimeServiceElements_notReset_afterComplexTagWithOneTimeElements_secondOccurrence_error() {
        testService {
            printer.openTag(XmlConst.service, mapOf("uuid" to exampleUuid))
            printer.openTag(XmlConst.informativeText, tagValue = "text1")
            printer.closeTag(XmlConst.informativeText)
            printer.openTag(XmlConst.characteristic, mapOf("uuid" to "2A20"))
            printDefaultProperties()
            printer.closeTag(XmlConst.characteristic)
            printer.openTag(XmlConst.informativeText, tagValue = "text2")
            printer.closeTag(XmlConst.informativeText)
            printer.closeTag(XmlConst.service)
        }
        readForError(ImportException(TAG_MAXIMUM_OCCURRENCE_EXCEEDED, XmlConst.informativeText))
    }

    @Test
    fun oneTimeServiceElements_reset_forNextService_success() {
        testService {
            printer.openTag(XmlConst.service, mapOf("uuid" to exampleUuid))
            printer.openTag(XmlConst.informativeText, tagValue = "text1")
            printer.closeTag(XmlConst.informativeText)
            printer.closeTag(XmlConst.service)

            printer.openTag(XmlConst.service, mapOf("uuid" to "1722"))
            printer.openTag(XmlConst.informativeText, tagValue = "text2")
            printer.closeTag(XmlConst.informativeText)
            printer.closeTag(XmlConst.service)
        }
        val retrievedServer = readForSuccess()
        assertEquals(2, retrievedServer.services.size)
        assertEquals("text1", retrievedServer.services[0].importedData.simpleElements["informativeText"])
        assertEquals("text2", retrievedServer.services[1].importedData.simpleElements["informativeText"])
    }

    @Test
    fun includeTag_wrongIdIncluded_notDeclaredAnywhere_error() {
        testService {
            printer.openTag(XmlConst.service, mapOf("uuid" to exampleUuid, "id" to "id1"))
            printer.openTag(XmlConst.include, mapOf("id" to "weirdId", "sourceId" to "s1"))
            printer.closeTag(XmlConst.include)
            printer.closeTag(XmlConst.service)
        }
        readForError(ImportException(WRONG_INCLUDE_ID_DECLARED, "weirdId", setOf()))
    }

    @Test
    fun includeTag_wrongIdIncluded_fromTheExactSameService_error() {
        testService {
            printer.openTag(XmlConst.service, mapOf("uuid" to exampleUuid, "id" to "id1"))
            printer.openTag(XmlConst.include, mapOf("id" to "id1", "sourceId" to "s1"))
            printer.closeTag(XmlConst.include)
            printer.closeTag(XmlConst.service)
        }
        readForError(ImportException(WRONG_INCLUDE_ID_DECLARED, "id1", setOf()))
    }

    @Test
    fun includeTag_correctIdDeclared_success() {
        testService {
            printer.openTag(XmlConst.service, mapOf("uuid" to exampleUuid, "id" to "id1"))
            printer.openTag(XmlConst.include, mapOf("id" to "id2", "sourceId" to "s1"))
            printer.closeTag(XmlConst.include)
            printer.closeTag(XmlConst.service)

            printer.openTag(XmlConst.service, mapOf("uuid" to "1722", "id" to "id2"))
            printer.closeTag(XmlConst.service)
        }
        readForSuccess()
    }

    private fun testService(test: () -> Unit) {
        printer.openTag(XmlConst.gatt)
        printer.openTag(XmlConst.capabilities_declare) /* For validating capabilities in services. */
        printer.openTag(XmlConst.capability, tagValue = "cap_1")
        printer.closeTag(XmlConst.capability)
        printer.closeTag(XmlConst.capabilities_declare)
        test()
        printer.closeTag(XmlConst.gatt)
    }
}