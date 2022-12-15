package com.siliconlabs.bledemo.import_export

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.ImportException
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.ImportException.ErrorType.*
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.utils.XmlConst
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class ImportDescriptorTest : ImportBaseTest() {
    
    private val exampleUuid = "DDDD"

    @Test
    fun uuidMissing_error() {
        testDescriptor {
            printer.openTag(XmlConst.descriptor)
            printer.closeTag(XmlConst.descriptor)
        }
        readForError(ImportException(MANDATORY_ATTRIBUTE_MISSING, "uuid"))
    }

    @Test
    fun nameAttribute_defaultValue_success() {
        testDescriptor {
            printer.openTag(XmlConst.descriptor, mapOf("uuid" to exampleUuid))
            printDefaultProperties()
            printer.closeTag(XmlConst.descriptor)
        }
        val retrievedServer = readForSuccess()
        Assert.assertEquals(XmlConst.defaultDescriptorName, retrievedServer.services[0]
                .characteristics[0].descriptors[0].name)
    }

    @Test
    fun sigUuidFound_renameDescriptor_success() {
        testDescriptor {
            printer.openTag(XmlConst.descriptor, mapOf("uuid" to "2908"))
            printDefaultProperties()
            printer.closeTag(XmlConst.descriptor)
        }
        val retrievedServer = readForSuccess()
        Assert.assertEquals("Report Reference",
                retrievedServer.services[0].characteristics[0].descriptors[0].name)
    }

    @Test
    fun constAttribute_wrongValue_error() {
        testDescriptor {
            printer.openTag(XmlConst.descriptor, mapOf("uuid" to exampleUuid, "const" to "notBoolean"))
            printer.closeTag(XmlConst.descriptor)
        }
        readForError(ImportException(WRONG_ATTRIBUTE_VALUE, "notBoolean", XmlConst.booleanValues))
    }

    @Test
    fun discoverableAttribute_wrongValue_error() {
        testDescriptor {
            printer.openTag(XmlConst.descriptor, mapOf("uuid" to exampleUuid, "discoverable" to "notBoolean"))
            printer.closeTag(XmlConst.descriptor)
        }
        readForError(ImportException(WRONG_ATTRIBUTE_VALUE, "notBoolean", XmlConst.booleanValues))
    }

    @Test
    fun noPropertiesDeclared_error() {
        testDescriptor {
            printer.openTag(XmlConst.descriptor, mapOf("uuid" to exampleUuid))
            printer.closeTag(XmlConst.descriptor)
        }
        readForError(ImportException(NO_PROPERTIES_DECLARED))
    }


    @Test
    fun propertyNotSupported_declaredInTag_error() {
        testDescriptor {
            printer.openTag(XmlConst.descriptor, mapOf("uuid" to exampleUuid))
            printer.openTag(XmlConst.properties)
            printer.openTag(XmlConst.indicate)
            printer.closeTag(XmlConst.indicate)
            printer.closeTag(XmlConst.properties)
            printer.closeTag(XmlConst.descriptor)
        }
        readForError(ImportException(PROPERTY_NOT_SUPPORTED_BY_DESCRIPTOR,
                XmlConst.indicate, XmlConst.descriptorPropertiesElements))
    }

    @Test
    fun propertyNotSupported_declaredInAttribute_error() {
        testDescriptor {
            printer.openTag(XmlConst.descriptor, mapOf("uuid" to exampleUuid))
            printer.openTag(XmlConst.properties, mapOf("bonded_notify" to "true"))
            printer.closeTag(XmlConst.properties)
            printer.closeTag(XmlConst.descriptor)
        }
        readForError(ImportException(PROPERTY_NOT_SUPPORTED_BY_DESCRIPTOR,
                XmlConst.notify, XmlConst.descriptorPropertiesElements))
    }

    @Test
    fun simpleElementSecondOccurrence() {
        testDescriptor {
            printer.openTag(XmlConst.descriptor, mapOf("uuid" to exampleUuid))
            printDefaultProperties()
            printer.openTag(XmlConst.informativeText, tagValue = "info1")
            printer.closeTag(XmlConst.informativeText)
            printer.openTag(XmlConst.informativeText, tagValue = "info2")
            printer.closeTag(XmlConst.informativeText)
            printer.closeTag(XmlConst.descriptor)
        }
        readForError(ImportException(TAG_MAXIMUM_OCCURRENCE_EXCEEDED, XmlConst.informativeText))
    }

    @Test
    fun propertiesElementSecondOccurrence() {
        testDescriptor {
            printer.openTag(XmlConst.descriptor, mapOf("uuid" to exampleUuid))
            printDefaultProperties()
            printDefaultProperties()
            printer.closeTag(XmlConst.descriptor)
        }
        readForError(ImportException(TAG_MAXIMUM_OCCURRENCE_EXCEEDED, XmlConst.properties))
    }

    @Test
    fun oneTimeDescriptorElements_notReset_afterComplexTagWithOneTimeElements_secondOccurrence_error() {
        testDescriptor {
            printer.openTag(XmlConst.descriptor, mapOf("uuid" to exampleUuid))
            printer.openTag(XmlConst.informativeText, tagValue = "text1")
            printer.closeTag(XmlConst.informativeText)
            printDefaultProperties()

            printer.openTag(XmlConst.informativeText, tagValue = "text2")
            printer.closeTag(XmlConst.informativeText)
            printer.closeTag(XmlConst.descriptor)
        }
        readForError(ImportException(TAG_MAXIMUM_OCCURRENCE_EXCEEDED, XmlConst.informativeText))
    }

    @Test
    fun oneTimeCharacteristicElements_reset_forNextCharacteristic_success() {
        testDescriptor {
            printer.openTag(XmlConst.descriptor, mapOf("uuid" to "2A90"))
            printer.openTag(XmlConst.informativeText, tagValue = "text1")
            printer.closeTag(XmlConst.informativeText)
            printDefaultProperties()
            printer.closeTag(XmlConst.descriptor)

            printer.openTag(XmlConst.descriptor, mapOf("uuid" to "2A91"))
            printer.openTag(XmlConst.informativeText, tagValue = "text2")
            printer.closeTag(XmlConst.informativeText)
            printDefaultProperties()
            printer.closeTag(XmlConst.descriptor)
        }
        val retrievedServer = readForSuccess()
        Assert.assertEquals(2, retrievedServer.services[0].characteristics[0].descriptors.size)
        Assert.assertEquals("text1", retrievedServer.services[0].characteristics[0]
                .descriptors[0].importedData.simpleElements["informativeText"])
        Assert.assertEquals("text2", retrievedServer.services[0].characteristics[0]
                .descriptors[1].importedData.simpleElements["informativeText"])
    }


    private fun testDescriptor(test: () -> Unit) {
        printer.openTag(XmlConst.gatt)
        printer.openTag(XmlConst.service, mapOf("uuid" to "1721"))
        printer.openTag(XmlConst.characteristic, mapOf("uuid" to "2A90"))
        printDefaultProperties()
        test()
        printer.closeTag(XmlConst.characteristic)
        printer.closeTag(XmlConst.service)
        printer.closeTag(XmlConst.gatt)
    }
}