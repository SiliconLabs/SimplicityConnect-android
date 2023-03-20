package com.siliconlabs.bledemo.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.siliconlabs.bledemo.features.scan.browser.models.Mapping
import java.util.*

class SharedPrefUtils(context: Context) {
    private val mPrefs: SharedPreferences
    private val editor: SharedPreferences.Editor
    private val gson: Gson

    companion object {
        private const val MAP_KEY = "MAP_KEY"
        private const val FAVORITES_DEVICES_KEY = "FAVORITES_DEVICES_KEY"
        private const val TEMPORARY_FAVORITES_DEVICES_KEY = "TEMPORARY_FAVORITES_DEVICES_KEY"
        private const val DISPLAY_APPLICATION_LEAVE_DIALOG_KEY = "DISPLAY_APPLICATION_LEAVE_DIALOG_KEY"
        private const val DISPLAY_UNBOND_DEVICE_DIALOG_KEY = "DISPLAY_UNBOND_DEVICE_DIALOG_KEY"
        private const val DISPLAY_MANUAL_UNBOND_DEVICE_DIALOG_KEY = "DISPLAY_MANUAL_UNBOND_DEVICE_DIALOG_KEY"
        private const val CHARACTERISTIC_NAMES_KEY = "CHARACTERISTIC_NAMES_KEY"
        private const val SERVICE_NAMES_KEY = "SERVICE_NAMES_KEY"
    }

    init {
        mPrefs = context.getSharedPreferences("MODEL_PREFERENCES", Context.MODE_PRIVATE)
        editor = mPrefs.edit()
        gson = Gson()
        mergeTmpDevicesToFavorites() // for backward compatibility; only one list of favorites left
    }

    private val favoritesDevices: LinkedHashSet<String>
        get() = if (getString(FAVORITES_DEVICES_KEY) == null) {
            LinkedHashSet()
        } else {
            val type = object : TypeToken<LinkedHashSet<String?>?>() {}.type
            gson.fromJson(getString(FAVORITES_DEVICES_KEY), type)
        }

    private val temporaryFavoritesDevices: LinkedHashSet<String>
        get() = if (getString(TEMPORARY_FAVORITES_DEVICES_KEY) == null) {
            LinkedHashSet()
        } else {
            val type = object : TypeToken<LinkedHashSet<String?>?>() {}.type
            gson.fromJson(getString(TEMPORARY_FAVORITES_DEVICES_KEY), type)
        }

    private fun mergeTmpDevicesToFavorites() {
        val favoritesDevices = favoritesDevices
        val temporaryFavoritesDevices = temporaryFavoritesDevices
        favoritesDevices.addAll(temporaryFavoritesDevices)
        val json = gson.toJson(favoritesDevices)
        editor.putString(FAVORITES_DEVICES_KEY, json)
        editor.commit()
    }

    fun addDeviceToFavorites(device: String) {
        val favoritesDevices = favoritesDevices
        favoritesDevices.add(device)
        val json = gson.toJson(favoritesDevices)
        editor.putString(FAVORITES_DEVICES_KEY, json)
        editor.commit()
    }

    fun removeDeviceFromFavorites(device: String?) {
        val favoritesDevices = favoritesDevices
        favoritesDevices.remove(device)
        val json = gson.toJson(favoritesDevices)
        editor.putString(FAVORITES_DEVICES_KEY, json)
        editor.commit()

        removeDeviceFromTemporaryFavorites(device) // ensure backward compatibility
    }

    fun isFavorite(device: String?): Boolean {
        return if (getString(FAVORITES_DEVICES_KEY) == null) false else favoritesDevices.contains(device)
    }

    private fun removeDeviceFromTemporaryFavorites(device: String?) {
        val temporaryFavoritesDevices = temporaryFavoritesDevices
        temporaryFavoritesDevices.remove(device)
        val json = gson.toJson(temporaryFavoritesDevices)
        editor.putString(TEMPORARY_FAVORITES_DEVICES_KEY, json)
        editor.commit()
    }

    private fun getString(key: String): String? {
        return mPrefs.getString(key, null)
    }

    val characteristicNamesMap: HashMap<String, Mapping>
        get() {
            val defValue = Gson().toJson(HashMap<String, String>())
            val json = mPrefs.getString(CHARACTERISTIC_NAMES_KEY, defValue)
            val token: TypeToken<HashMap<String, Mapping>> = object : TypeToken<HashMap<String, Mapping>>() {}
            return gson.fromJson(json, token.type)
        }

    val serviceNamesMap: HashMap<String, Mapping>
        get() {
            val defValue = Gson().toJson(HashMap<String, String>())
            val json = mPrefs.getString(SERVICE_NAMES_KEY, defValue)
            val token: TypeToken<HashMap<String, Mapping>> = object : TypeToken<HashMap<String, Mapping>>() {}
            return gson.fromJson(json, token.type)
        }

    fun saveCharacteristicNamesMap(map: HashMap<String, Mapping>) {
        val json = gson.toJson(map)
        editor.putString(CHARACTERISTIC_NAMES_KEY, json)
        editor.apply()
    }

    fun saveServiceNamesMap(map: HashMap<String, Mapping>) {
        val json = gson.toJson(map)
        editor.putString(SERVICE_NAMES_KEY, json)
        editor.apply()
    }

    fun shouldDisplayLeaveApplicationDialog(): Boolean {
        return mPrefs.getBoolean(DISPLAY_APPLICATION_LEAVE_DIALOG_KEY, true)
    }

    fun setShouldLeaveApplicationDialog(displayDialog: Boolean) {
        editor.putBoolean(DISPLAY_APPLICATION_LEAVE_DIALOG_KEY, displayDialog)
        editor.apply()
    }

    fun shouldDisplayManualUnbondDeviceDialog(): Boolean {
        return shouldDisplayDialog(DISPLAY_MANUAL_UNBOND_DEVICE_DIALOG_KEY)
    }

    fun setShouldDisplayManualUnbondDeviceDialog(displayDialog: Boolean) {
        setShouldDisplayDialog(displayDialog, DISPLAY_MANUAL_UNBOND_DEVICE_DIALOG_KEY)
    }

    fun shouldDisplayUnbondDeviceDialog(): Boolean {
        return shouldDisplayDialog(DISPLAY_UNBOND_DEVICE_DIALOG_KEY)
    }

    fun setShouldDisplayUnbondDeviceDialog(displayDialog: Boolean) {
        setShouldDisplayDialog(displayDialog, DISPLAY_UNBOND_DEVICE_DIALOG_KEY)
    }

    private fun shouldDisplayDialog(key: String): Boolean {
        return mPrefs.getBoolean(key, true)
    }

    fun setShouldDisplayDialog(displayDialog: Boolean, key: String) {
        editor.putBoolean(key, displayDialog)
        editor.apply()
    }
}
