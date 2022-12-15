package com.siliconlabs.bledemo.features.configure.advertiser.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.siliconlabs.bledemo.features.configure.advertiser.models.Advertiser

class AdvertiserStorage(val context: Context) {
    companion object {
        private const val PREFS_ADVERTISER_STORAGE = "PREFS_ADVERTISER_STORAGE"
        private const val KEY_ADVERTISER_LIST = "KEY_ADVERTISER_LIST"
        private const val KEY_DISPLAY_REMOVE_SERVICES_DIALOG = "KEY_DISPLAY_REMOVE_SERVICES_DIALOG"
        private const val KEY_DISPLAY_REMOVE_ADVERTISER_DIALOG = "KEY_DISPLAY_REMOVE_ADVERTISER_DIALOG"
        private const val KEY_ADVERTISING_EXTENSION_SUPPORT = "KEY_ADVERTISING_EXTENSION_SUPPORT"
        private const val KEY_LE_2M_PHY_SUPPORTED = "KEY_LE_2M_PHY_SUPPORTED"
        private const val KEY_LE_CODED_PHY_SUPPORTED = "KEY_LE_CODED_PHY_SUPPORTED"
        private const val KEY_LE_MAX_ADVERTISING_DATA_LENGTH = "KEY_LE_MAX_ADVERTISING_DATA_LENGTH"
        private const val KEY_DISPLAY_ADVERTISER_CONFIG_LEAVE_DIALOG = "KEY_DISPLAY_ADVERTISER_CONFIG_LEAVE_DIALOG"
    }

    private val preferences = context.getSharedPreferences(PREFS_ADVERTISER_STORAGE, MODE_PRIVATE)

    fun storeAdvertiserList(list: ArrayList<Advertiser>) {
        val json = Gson().toJson(list)
        val editor = preferences.edit()
        editor.putString(KEY_ADVERTISER_LIST, json).apply()
    }

    fun loadAdvertiserList(): ArrayList<Advertiser> {
        val json = preferences.getString(KEY_ADVERTISER_LIST, "")
        val type = object : TypeToken<ArrayList<Advertiser>>() {}.type
        return try {
            Gson().fromJson(json, type) ?: ArrayList()
        } catch (e: JsonSyntaxException) {
            ArrayList()
        }
    }

    fun clearRunningAdvertisers() {
        val advertisers = loadAdvertiserList().onEach { if (it.isRunning) it.stop() }
        storeAdvertiserList(advertisers)
    }

    fun shouldDisplayRemoveAdvertiserDialog(): Boolean {
        return preferences.getBoolean(KEY_DISPLAY_REMOVE_ADVERTISER_DIALOG,true)
    }

    fun setShouldDisplayRemoveAdvertiserDialog(display: Boolean) {
        preferences.edit().putBoolean(KEY_DISPLAY_REMOVE_ADVERTISER_DIALOG, display).apply()
    }

    fun shouldDisplayRemoveServicesDialog(): Boolean {
        return preferences.getBoolean(KEY_DISPLAY_REMOVE_SERVICES_DIALOG, true)
    }

    fun setShouldDisplayRemoveServicesDialog(display: Boolean) {
        preferences.edit().putBoolean(KEY_DISPLAY_REMOVE_SERVICES_DIALOG, display).apply()
    }

    fun isAdvertisingBluetoothInfoChecked(): Boolean {
        return preferences.contains(KEY_ADVERTISING_EXTENSION_SUPPORT)
    }

    fun isAdvertisingExtensionSupported(): Boolean {
        return preferences.getBoolean(KEY_ADVERTISING_EXTENSION_SUPPORT, false)
    }

    fun setAdvertisingExtensionSupported(supported: Boolean) {
        preferences.edit().putBoolean(KEY_ADVERTISING_EXTENSION_SUPPORT, supported).apply()
    }

    fun isLe2MPhySupported(): Boolean {
        return preferences.getBoolean(KEY_LE_2M_PHY_SUPPORTED, false)
    }

    fun setLe2MPhySupported(supported: Boolean) {
        preferences.edit().putBoolean(KEY_LE_2M_PHY_SUPPORTED, supported).apply()
    }

    fun isLeCodedPhySupported(): Boolean {
        return preferences.getBoolean(KEY_LE_CODED_PHY_SUPPORTED, false)
    }

    fun setLeCodedPhySupported(supported: Boolean) {
        preferences.edit().putBoolean(KEY_LE_CODED_PHY_SUPPORTED, supported).apply()
    }

    fun setLeMaximumDataLength(value: Int) {
        preferences.edit().putInt(KEY_LE_MAX_ADVERTISING_DATA_LENGTH, value).apply()
    }

    fun getLeMaximumDataLength(): Int {
        return preferences.getInt(KEY_LE_MAX_ADVERTISING_DATA_LENGTH, -1)
    }

    fun shouldDisplayLeaveAdvertiserConfigDialog(): Boolean {
        return preferences.getBoolean(KEY_DISPLAY_ADVERTISER_CONFIG_LEAVE_DIALOG, true)
    }

    fun setShouldDisplayLeaveAdvertiserConfigDialog(displayDialog: Boolean) {
        preferences.edit().putBoolean(KEY_DISPLAY_ADVERTISER_CONFIG_LEAVE_DIALOG, displayDialog).apply()
    }

}