package com.siliconlabs.bledemo.import_export

import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.siliconlabs.bledemo.gatt_configurator.models.*
import com.siliconlabs.bledemo.gatt_configurator.import_export.*
import com.siliconlabs.bledemo.gatt_configurator.import_export.data.CharacteristicImportData
import com.siliconlabs.bledemo.gatt_configurator.import_export.data.DescriptorImportData
import com.siliconlabs.bledemo.gatt_configurator.import_export.data.ServerImportData
import com.siliconlabs.bledemo.gatt_configurator.import_export.data.ServiceImportData
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.*

@RunWith(AndroidJUnit4ClassRunner::class)
@LargeTest
class ImportExportTest {

    private val exporter = GattServerExporter()
    private lateinit var importer: GattServerImporter

    private val filePath = "data/data/com.siliconlabs.bledemo/"
    private val firstServerName = "sErv 1"
    private val secondServerName = "s2"


    @Test
    fun exportTwoServers_importTwoFiles_success() {
        val exportedServers = listOf(prepareFirstServer(), prepareSecondServer())
        val importedServers = mutableListOf<GattServer>()

        val exportContent = exporter.export(exportedServers)

        for (e in exportContent) {
            File(filePath + "${e.key}.xml").let {
                it.setReadable(true)
                it.setWritable(true)
                it.createNewFile()

                val writer = BufferedWriter(FileWriter(it))
                writer.write(e.value)
                writer.close()

                val reader = BufferedReader(FileReader(it))
                importer = GattServerImporter(reader)
                importedServers.add(importer.readFile())
                reader.close()

                it.delete()
            }
        }
        assertEquals(exportedServers, importedServers)
    }

    private fun prepareFirstServer() : GattServer {
        /* All possible imported data with various values */
        /* 1st service with 3 characteristics (2, 1, 0 descriptors) */
        /* 2nd service with 0 characteristics */
        return GattServer(
                name = firstServerName,
                services = arrayListOf(
                    Service(
                            name = "serv1",
                            uuid = Uuid("1728"),
                            type = Service.Type.PRIMARY,
                            characteristics = arrayListOf(
                                Characteristic(
                                        name = "char1",
                                        uuid = Uuid("2F12"),
                                        descriptors = arrayListOf(
                                            Descriptor(
                                                    name = "desc1",
                                                    uuid = Uuid("2112"),
                                                    properties = hashMapOf(
                                                            Property.READ to hashSetOf(Property.Type.BONDED),
                                                            Property.WRITE to hashSetOf(Property.Type.AUTHENTICATED)
                                                    ),
                                                    value = Value(
                                                            value = "32",
                                                            type = Value.Type.UTF_8,
                                                            length = 21,
                                                            variableLength = false
                                                    ),
                                                    importedData = DescriptorImportData(
                                                            attributes = mutableMapOf(
                                                                    "id" to "someId",
                                                                    "sourceId" to "anotherAtt",
                                                                    "discoverable" to "false"
                                                            ),
                                                            simpleElements = mutableMapOf(
                                                                    "informativeText" to "infoText"
                                                            ),
                                                            propertiesAttributes = mutableMapOf(
                                                                    "authenticated_read" to "true",
                                                                    "bonded_write" to "false"
                                                            )
                                                    )
                                            ),
                                            Descriptor(
                                                    name = "desc2",
                                                    uuid = Uuid("2421"),
                                                    properties = hashMapOf(
                                                            Property.WRITE to hashSetOf(Property.Type.ENCRYPTED)
                                                    ),
                                                    value = Value(
                                                            value = "13",
                                                            type = Value.Type.USER,
                                                            length = 0,
                                                            variableLength = true
                                                    ),
                                                    importedData = DescriptorImportData(
                                                            attributes = mutableMapOf(
                                                                    "id" to "weirdId",
                                                                    "sourceId" to "sor",
                                                                    "discoverable" to "true"
                                                            ),
                                                            simpleElements = mutableMapOf(
                                                                    "informativeText" to "textInfo"
                                                            ),
                                                            propertiesAttributes = mutableMapOf(
                                                                    "read" to "false",
                                                                    "encrypted_write" to "false"
                                                            )
                                                    )
                                            )
                                        ),
                                        properties = hashMapOf(
                                            Property.NOTIFY to hashSetOf(Property.Type.ENCRYPTED),
                                            Property.WRITE_WITHOUT_RESPONSE to hashSetOf(Property.Type.BONDED)
                                        ),
                                        value = Value(
                                                value = "ff0a",
                                                type = Value.Type.HEX,
                                                length = 0,
                                                variableLength = true
                                        ),
                                        importedData = CharacteristicImportData(
                                                attributes = mutableMapOf(
                                                        "id" to "someId",
                                                        "sourceId" to "someSourceId",
                                                        "const" to "true",
                                                        "instance_id" to "someInstanceId"),
                                                capabilities = mutableSetOf("cap1"),
                                                simpleElements = mutableMapOf(
                                                        "informativeText" to "infoText",
                                                        "description" to "someDescription"),
                                                propertiesAttributes = mutableMapOf(
                                                        "read" to "true",
                                                        "write" to "false",
                                                        "bonded_notify" to "false",
                                                        "indicate_requirement" to "mandatory"),
                                                aggregate = mutableListOf(
                                                        null,
                                                        "firstAgg",
                                                        "secondAgg"
                                                )
                                        )
                                ),
                                Characteristic(
                                        name = "char2",
                                        uuid = Uuid("2F82"),
                                        descriptors = arrayListOf(
                                                Descriptor(
                                                        name = "desc21",
                                                        uuid = Uuid("31E2"),
                                                        properties = hashMapOf(
                                                                Property.READ to hashSetOf(Property.Type.BONDED),
                                                                Property.WRITE to hashSetOf(Property.Type.AUTHENTICATED)
                                                        ),
                                                        value = Value(
                                                                value = "FE",
                                                                type = Value.Type.UTF_8,
                                                                length = 1,
                                                                variableLength = true
                                                        ),
                                                        importedData = DescriptorImportData(
                                                                attributes = mutableMapOf(
                                                                        "id" to "someId",
                                                                        "sourceId" to "anotherAtt",
                                                                        "discoverable" to "false"
                                                                ),
                                                                simpleElements = mutableMapOf(
                                                                        "informativeText" to "infoText"
                                                                ),
                                                                propertiesAttributes = mutableMapOf(
                                                                        "authenticated_read" to "true",
                                                                        "bonded_write" to "false"
                                                                )
                                                        )
                                                )
                                        ),
                                        properties = hashMapOf(
                                                Property.NOTIFY to hashSetOf(Property.Type.ENCRYPTED),
                                                Property.WRITE_WITHOUT_RESPONSE to hashSetOf(Property.Type.BONDED)
                                        ),
                                        value = Value(
                                                value = "ff0a",
                                                type = Value.Type.HEX,
                                                length = 0,
                                                variableLength = true
                                        ),
                                        importedData = CharacteristicImportData(
                                                attributes = mutableMapOf(
                                                        "id" to "someId",
                                                        "sourceId" to "someSourceId",
                                                        "const" to "false",
                                                        "instance_id" to "someInstanceId"),
                                                capabilities = mutableSetOf("cap1"),
                                                simpleElements = mutableMapOf(
                                                        "informativeText" to "infoText",
                                                        "description" to "someDescription"),
                                                propertiesAttributes = mutableMapOf(
                                                        "read" to "true",
                                                        "write" to "false",
                                                        "bonded_notify" to "false",
                                                        "indicate_requirement" to "mandatory"),
                                                aggregate = mutableListOf(
                                                        null,
                                                        "firstAgg",
                                                        "secondAgg"
                                                )
                                        )
                                ),
                                Characteristic(
                                        name = "char2",
                                        uuid = Uuid("2F82"),
                                        descriptors = arrayListOf(),
                                        properties = hashMapOf(
                                                Property.INDICATE to hashSetOf(Property.Type.AUTHENTICATED),
                                                Property.RELIABLE_WRITE to hashSetOf(Property.Type.ENCRYPTED)
                                        ),
                                        value = Value(
                                                value = "ff0a",
                                                type = Value.Type.HEX,
                                                length = 0,
                                                variableLength = true
                                        ),
                                        importedData = CharacteristicImportData(
                                                attributes = mutableMapOf(
                                                        "id" to "someId",
                                                        "sourceId" to "someSourceId",
                                                        "const" to "true",
                                                        "instance_id" to "someInstanceId"),
                                                capabilities = mutableSetOf(),
                                                simpleElements = mutableMapOf(
                                                        "informativeText" to "infoText",
                                                        "description" to "someDescription"),
                                                propertiesAttributes = mutableMapOf(
                                                        "read" to "true",
                                                        "write" to "false",
                                                        "bonded_notify" to "false",
                                                        "indicate_requirement" to "mandatory"),
                                                aggregate = mutableListOf(
                                                        null,
                                                        "firstAgg",
                                                        "secondAgg"
                                                )
                                        )
                                )
                            ),
                            importedData = ServiceImportData(
                                    attributes = mutableMapOf(
                                            "id" to "first_id",
                                            "sourceId" to "someSourceId",
                                            "requirement" to "c2",
                                            "advertise" to "true",
                                            "instance_id" to "someInstance_id"),
                                    capabilities = mutableSetOf("cap1"),
                                    simpleElements = mutableMapOf(
                                            "informativeText" to "infotext",
                                            "description" to "someDescription",
                                            "uri" to "someUri"),
                                    include = mutableMapOf(
                                            "second_id" to "src2"
                                    )
                            )
                    ),
                    Service(
                            name = "serv2",
                            uuid = Uuid("1FFE"),
                            type = Service.Type.SECONDARY,
                            characteristics = arrayListOf(),
                            importedData = ServiceImportData(
                                    attributes = mutableMapOf(
                                            "id" to "second_id",
                                            "sourceId" to "someSourceId",
                                            "requirement" to "c2",
                                            "advertise" to "true",
                                            "instance_id" to "someInstance_id"),
                                    capabilities = mutableSetOf("cap1"),
                                    simpleElements = mutableMapOf(
                                            "informativeText" to "infotext",
                                            "description" to "someDescription",
                                            "uri" to "someUri"),
                                    include = mutableMapOf(
                                            "first_id" to "src1"
                                    )
                            )
                    )
                ),
                isSwitchedOn = false,
                importedData = ServerImportData(
                        device = "device23",
                        gattAttributes = mutableMapOf(
                                "out" to "someOut",
                                "header" to "someHeader",
                                "name" to "serverName",
                                "prefix" to "somePrefix",
                                "generic_attribute_service" to "false",
                                "in" to "someInnn",
                                "gatt_caching" to "true",
                                "id" to "someId"
                        ),
                        capabilities = mapOf(
                                "cap1" to "true",
                                "cap2" to "false"
                        )
                )
        )
    }

    private fun prepareSecondServer() : GattServer {
        /* No imported values, no services */
        return GattServer(secondServerName)
    }
}