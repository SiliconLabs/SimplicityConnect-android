package com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.siliconlabs.bledemo.features.demo.matter_demo.model.MatterScannedResultModel
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.model.TemperatureScale
import java.util.*

class PreferenceManager(context: Context) {

    private val sharedPreferences: SharedPreferences?
    private val locale: Locale = context.resources.configuration.locale
    var preferences: TemperatureScale
    lateinit var preferencesMatterDevices: MatterScannedResultModel

    fun retrievePreferences(): TemperatureScale {
        val jsonString = sharedPreferences!!.getString(PREFERENCES_CONTENT, null)
        return if (jsonString == null) {
            TemperatureScale(locale)
        } else {
            Gson().fromJson(jsonString, TemperatureScale::class.java)
        }
    }

   /* fun retrieveMatterDevices(): MatterScannedResultModel {
        val jsonString = sharedPreferences!!.getString(DEVICELIST_CONTENT, null)
        return if (jsonString == null) {
            MatterScannedResultModel()

        } else {
            Gson().fromJson(jsonString, MatterScannedResultModel::class.java)
        }

    }*/

    fun savePreferences(preferences: TemperatureScale) {
        this.preferences = preferences
        sharedPreferences!!.edit()
                .putString(PREFERENCES_CONTENT, Gson().toJson(preferences))
                .apply()
    }

    companion object {
        private const val PREFERENCES_KEY = "ThunderBoard"
        private const val PREFERENCES_CONTENT = "temperatureScale"
        private const val DEVICELIST_CONTENT = "matterdeviceinfo"
    }

    init {
        sharedPreferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
        preferences = retrievePreferences()
    }
}