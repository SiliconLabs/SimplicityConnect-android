package com.siliconlabs.bledemo.import_export

import android.util.Xml
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.GattServer
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.*
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.utils.XmlConst
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.utils.XmlPrinter
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.xmlpull.v1.XmlSerializer
import java.io.*

open class ImportBaseTest {

    private lateinit var reader: BufferedReader
    protected lateinit var writer: BufferedWriter
    protected val serializer: XmlSerializer = Xml.newSerializer()
    protected lateinit var printer: XmlPrinter

    companion object {
        private val filePath = File("data/data/com.siliconlabs.bledemo/example.xml")

        @JvmStatic
        @BeforeClass
        fun createFile() {
            filePath.setReadable(true)
            filePath.setWritable(true)
            filePath.createNewFile()
        }

        @JvmStatic
        @AfterClass
        fun deleteFile() {
            filePath.delete()
        }
    }

    @Before
    fun setup() {
        PrintWriter(filePath).close() /* Clear file for the next test. */

        reader = BufferedReader(FileReader(filePath))
        writer = BufferedWriter(FileWriter(filePath))
        serializer.setOutput(writer)
        printer = XmlPrinter(serializer)

        serializer.startDocument("UTF-8", null)
        serializer.text("\n")
    }

    protected fun readForError(expectedError: ImportException) {
        serializer.endDocument()
        try {
            GattServerImporter(reader).readFile()
            fail()
        } catch (err: ImportException) {
            assertEquals(expectedError.errorType, err.errorType)
            assertEquals(expectedError.provided, err.provided)
            assertEquals(expectedError.expected, err.expected)
        }
    }

    protected fun readForSuccess() : GattServer {
        serializer.endDocument()
        return GattServerImporter(reader).readFile()
    }

    protected fun printDefaultProperties() { /* To prevent test failing when testing other cases */
        printer.openTag(XmlConst.properties, mapOf("authenticated_read" to "true"))
        printer.closeTag(XmlConst.properties)
    }

}