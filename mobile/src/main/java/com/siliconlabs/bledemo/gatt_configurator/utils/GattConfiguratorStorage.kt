package com.siliconlabs.bledemo.gatt_configurator.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.siliconlabs.bledemo.gatt_configurator.models.GattServer

class GattConfiguratorStorage(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_GATT_CONFIGURATOR_STORAGE, Context.MODE_PRIVATE)

    fun saveGattServerList(list: ArrayList<GattServer>) {
        val json = Gson().toJson(list)
        val editor = preferences.edit()
        editor.putString(KEY_GATT_SERVER_LIST, json).apply()
    }

    fun loadGattServerList(): ArrayList<GattServer> {
        val json = preferences.getString(KEY_GATT_SERVER_LIST, "")
        val type = object : TypeToken<ArrayList<GattServer>>() {}.type
        return try {
            Gson().fromJson(json, type) ?: ArrayList()
        } catch (e: JsonSyntaxException) {
            ArrayList()
        }
    }

    fun saveActiveGattServer(gattServer: GattServer?) {
        val json = Gson().toJson(gattServer)
        val editor = preferences.edit()
        editor.putString(KEY_ACTIVE_GATT_SERVER, json).apply()
    }

    fun loadActiveGattServer(): GattServer? {
        val json = preferences.getString(KEY_ACTIVE_GATT_SERVER, "")
        return Gson().fromJson(json, GattServer::class.java)
    }

    fun shouldDisplayRemovalDialog(): Boolean {
        return preferences.getBoolean(KEY_DISPLAY_REMOVAL_DIALOG, true)
    }

    fun setDisplayRemovalDialog(enable: Boolean) {
        preferences
            .edit()
            .putBoolean(KEY_DISPLAY_REMOVAL_DIALOG, enable)
            .apply()
    }

    fun setShouldDisplayLeaveGattServerConfigDialog(display: Boolean) {
        preferences.edit().putBoolean(DISPLAY_LEAVE_CONFIG_DIALOG, display).apply()
    }

    fun shouldDisplayLeaveGattServerConfigDialog() : Boolean {
        return preferences.getBoolean(DISPLAY_LEAVE_CONFIG_DIALOG, true)
    }

    companion object {
        private const val PREFS_GATT_CONFIGURATOR_STORAGE = "PREFS_GATT_CONFIGURATOR_STORAGE"
        private const val KEY_GATT_SERVER_LIST = "KEY_GATT_SERVER_LIST"
        private const val KEY_ACTIVE_GATT_SERVER = "KEY_ACTIVE_GATT_SERVER"
        private const val KEY_DISPLAY_REMOVAL_DIALOG = "KEY_DISPLAY_REMOVAL_DIALOG"
        private const val DISPLAY_LEAVE_CONFIG_DIALOG = "DISPLAY_LEAVE_CONFIG_DIALOG"
    }
}
