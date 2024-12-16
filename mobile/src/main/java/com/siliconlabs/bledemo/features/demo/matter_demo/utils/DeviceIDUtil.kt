package com.siliconlabs.bledemo.features.demo.matter_demo.utils

import android.content.Context
import android.content.SharedPreferences
import com.siliconlabs.bledemo.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DeviceIDUtil {

    private const val PREFERENCE_FILE_KEY = "PREFERENCE_FILE_KEY"
    private const val DEVICE_ID_PREFS_KEY = "device_id"
    private const val DEFAULT_DEVICE_ID_DEV_ENV = 1L
    private const val DEFAULT_DEVICE_ID_PROD_ENV = 1L


    fun getNextAvailableId(context: Context): Long {
        var deviceID: Long = -1L
        if (BuildConfig.DEBUG) {
            deviceID = DEFAULT_DEVICE_ID_DEV_ENV
        } else {
            deviceID = DEFAULT_DEVICE_ID_PROD_ENV
        }
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        val prefs = getPrefs(context)
        val updatedDeviceID = "$deviceID$timestamp"
        println("DeviceID :$updatedDeviceID")
        println("DeviceID Long :${updatedDeviceID.toLong()}")
        return if (prefs.contains(DEVICE_ID_PREFS_KEY)) {
            prefs.getLong(DEVICE_ID_PREFS_KEY, updatedDeviceID.toLong())
        } else {
            prefs.edit().putLong(DEVICE_ID_PREFS_KEY, updatedDeviceID.toLong()).apply()
            updatedDeviceID.toLong()
        }
    }

    fun setNextAvailableId(context: Context, newId: Long) {
        getPrefs(context).edit().putLong(DEVICE_ID_PREFS_KEY, newId).apply()
    }

    fun getLastDeviceId(context: Context): Long = getNextAvailableId(context) - 1

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE)
    }
}