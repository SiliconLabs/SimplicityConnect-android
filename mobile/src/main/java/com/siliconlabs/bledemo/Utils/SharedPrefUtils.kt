package com.siliconlabs.bledemo.Utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.siliconlabs.bledemo.Browser.Models.Mapping
import java.util.*

class SharedPrefUtils(context: Context) {
    private val mPrefs: SharedPreferences
    private val editor: SharedPreferences.Editor
    private val gson: Gson

    companion object {
        private const val MAP_KEY = "MAP_KEY"
        private const val LAST_FILTER_KEY = "LAST_FILTER_KEY"
        private const val FAVORITES_DEVICES_KEY = "FAVORITES_DEVICES_KEY"
        private const val TEMPORARY_FAVORITES_DEVICES_KEY = "TEMPORARY_FAVORITES_DEVICES_KEY"
        private const val DISPLAY_BROWSER_LEAVE_DIALOG_KEY = "DISPLAY_BROWSER_LEAVE_DIALOG_KEY"
        private const val DISPLAY_UNBOND_DEVICE_DIALOG_KEY = "DISPLAY_UNBOND_DEVICE_DIALOG_KEY"
        private const val CHARACTERISTIC_NAMES_KEY = "CHARACTERISTIC_NAMES_KEY"
        private const val SERVICE_NAMES_KEY = "SERVICE_NAMES_KEY"
    }

    init {
        mPrefs = context.getSharedPreferences("MODEL_PREFERENCES", Context.MODE_PRIVATE)
        editor = mPrefs.edit()
        gson = Gson()
    }

    val mapFilter: HashMap<String, FilterDeviceParams?>
        get() = if (getString(MAP_KEY) == null) {
            HashMap()
        } else {
            val type = object : TypeToken<HashMap<String?, FilterDeviceParams?>?>() {}.type
            gson.fromJson(getString(MAP_KEY), type)
        }

    val favoritesDevices: LinkedHashSet<String>
        get() = if (getString(FAVORITES_DEVICES_KEY) == null) {
            LinkedHashSet()
        } else {
            val type = object : TypeToken<LinkedHashSet<String?>?>() {}.type
            gson.fromJson(getString(FAVORITES_DEVICES_KEY), type)
        }

    val temporaryFavoritesDevices: LinkedHashSet<String>
        get() = if (getString(TEMPORARY_FAVORITES_DEVICES_KEY) == null) {
            LinkedHashSet()
        } else {
            val type = object : TypeToken<LinkedHashSet<String?>?>() {}.type
            gson.fromJson(getString(TEMPORARY_FAVORITES_DEVICES_KEY), type)
        }

    fun mergeTmpDevicesToFavorites() {
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

    fun addDeviceToTemporaryFavorites(device: String) {
        val temporaryFavoritesDevices = temporaryFavoritesDevices
        temporaryFavoritesDevices.add(device)
        val json = gson.toJson(temporaryFavoritesDevices)
        editor.putString(TEMPORARY_FAVORITES_DEVICES_KEY, json)
        editor.commit()
    }

    fun removeDeviceFromFavorites(device: String?) {
        val favoritesDevices = favoritesDevices
        favoritesDevices.remove(device)
        val json = gson.toJson(favoritesDevices)
        editor.putString(FAVORITES_DEVICES_KEY, json)
        editor.commit()
    }

    fun removeDeviceFromTemporaryFavorites(device: String?) {
        val temporaryFavoritesDevices = temporaryFavoritesDevices
        temporaryFavoritesDevices.remove(device)
        val json = gson.toJson(temporaryFavoritesDevices)
        editor.putString(TEMPORARY_FAVORITES_DEVICES_KEY, json)
        editor.commit()
    }

    fun isFavorite(device: String?): Boolean {
        return if (getString(FAVORITES_DEVICES_KEY) == null) false else favoritesDevices.contains(device)
    }

    fun isTemporaryFavorite(device: String?): Boolean {
        return if (getString(TEMPORARY_FAVORITES_DEVICES_KEY) == null) false else temporaryFavoritesDevices.contains(device)
    }

    private fun getString(key: String): String? {
        return mPrefs.getString(key, null)
    }

    fun addToMapFilterAndSave(key: String, filterDeviceParam: FilterDeviceParams?) {
        val mapFilter = mapFilter
        mapFilter[key] = filterDeviceParam
        val json = gson.toJson(mapFilter)
        editor.putString(MAP_KEY, json)
        editor.commit()
    }

    fun updateMapFilter(currentMap: HashMap<String, FilterDeviceParams?>?) {
        val json = gson.toJson(currentMap)
        editor.putString(MAP_KEY, json)
        editor.commit()
    }

    var lastFilter: FilterDeviceParams?
        get() {
            if (getString(LAST_FILTER_KEY) != null) {
                val filterDeviceParams = gson.fromJson(getString(LAST_FILTER_KEY), FilterDeviceParams::class.java)
                return if (filterDeviceParams.isEmptyFilter) null else filterDeviceParams
            }
            return null
        }
        set(lastFilter) {
            val json = gson.toJson(lastFilter)
            editor.putString(LAST_FILTER_KEY, json)
            editor.commit()
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

    fun shouldDisplayLeaveBrowserDialog(): Boolean {
        return mPrefs.getBoolean(DISPLAY_BROWSER_LEAVE_DIALOG_KEY, true)
    }

    fun setShouldDisplayLeaveBrowserDialog(displayDialog: Boolean) {
        editor.putBoolean(DISPLAY_BROWSER_LEAVE_DIALOG_KEY, displayDialog)
        editor.apply()
    }

    fun shouldDisplayUnbondDeviceDialog(): Boolean {
        return mPrefs.getBoolean(DISPLAY_UNBOND_DEVICE_DIALOG_KEY, true)
    }

    fun setShouldDisplayUnbondDeviceDialog(displayDialog: Boolean) {
        editor.putBoolean(DISPLAY_UNBOND_DEVICE_DIALOG_KEY, displayDialog)
        editor.apply()
    }
}