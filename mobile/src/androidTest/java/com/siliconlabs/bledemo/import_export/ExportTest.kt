package com.siliconlabs.bledemo.import_export

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.siliconlabs.bledemo.gatt_configurator.models.GattServer
import com.siliconlabs.bledemo.gatt_configurator.import_export.GattServerExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class ExportTest {

    private val exporter = GattServerExporter()

    @Test
    fun serverName_changeBlanksToUnderscores() {
        val exportedServers = listOf(
                GattServer("Server Name With Spaces")
        )
        val serverPairs = exporter.export(exportedServers)
        assertEquals(1, serverPairs.size)
        assertTrue(serverPairs.contains("server_name_with_spaces"))
    }

    @Test
    fun serverName_sameNameForThreeServers_addNumbers() {
        val exportedServers = listOf(
                GattServer("First Server Name"),
                GattServer("First Server Name"),
                GattServer("First Server Name")
        )
        val serverPairs = exporter.export(exportedServers)
        assertEquals(3, serverPairs.size)
        assertTrue(serverPairs.contains("first_server_name"))
        assertTrue(serverPairs.contains("first_server_name_2"))
        assertTrue(serverPairs.contains("first_server_name_3"))
    }

    @Test
    fun serverName_sameNameForThreeServers_withUnderscore_addNumbers() {
        val exportedServers = listOf(
                GattServer("First Server_2"),
                GattServer("First Server_2"),
                GattServer("First Server_2")
        )
        val serverPairs = exporter.export(exportedServers)
        assertEquals(3, serverPairs.size)
        assertTrue(serverPairs.contains("first_server_2"))
        assertTrue(serverPairs.contains("first_server_2_2"))
        assertTrue(serverPairs.contains("first_server_2_3"))
    }

    @Test
    fun serverName_twoChecksNeeded_addNumbers() {
        val exportedServers = listOf(
                GattServer("first_server_2"),
                GattServer("First Server"),
                GattServer("First Server")
        )
        val serverPairs = exporter.export(exportedServers)
        assertEquals(3, serverPairs.size)
        assertTrue(serverPairs.contains("first_server_2"))
        assertTrue(serverPairs.contains("first_server"))
        assertTrue(serverPairs.contains("first_server_2_2"))
    }

    @Test
    fun serverName_twoSets_addNumbers() {
        val exportedServers = listOf(
                GattServer("First_SeRVer"),
                GattServer("first_Server"),
                GattServer("second_SERVER"),
                GattServer("SECOND_SERVER")
        )
        val serverPairs = exporter.export(exportedServers)
        assertEquals(4, serverPairs.size)
        assertTrue(serverPairs.contains("first_server"))
        assertTrue(serverPairs.contains("first_server_2"))
        assertTrue(serverPairs.contains("second_server"))
        assertTrue(serverPairs.contains("second_server_2"))
    }
}