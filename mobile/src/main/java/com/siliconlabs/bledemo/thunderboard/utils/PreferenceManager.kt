package com.siliconlabs.bledemo.thunderboard.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.siliconlabs.bledemo.environment.model.TemperatureScale
import com.siliconlabs.bledemo.thunderboard.injection.scope.ForApplication
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceManager @Inject constructor(@ForApplication context: Context) {

    private val sharedPreferences: SharedPreferences?
    private val locale: Locale = context.resources.configuration.locale
    var preferences: TemperatureScale?

    private fun retrievePreferences(): TemperatureScale {
        return if (preferences != null) {
            preferences!!
        } else {
            val jsonString = sharedPreferences!!.getString(
                    PREFERENCES_CONTENT,
                    null)
            if (jsonString == null) {
                TemperatureScale(locale)
            } else {
                Gson().fromJson(jsonString, TemperatureScale::class.java)
            }
        }
    }

    fun savePreferences(preferences: TemperatureScale) {
        Timber.d(preferences.toString())
        this.preferences = preferences
        sharedPreferences!!.edit()
                .putString(PREFERENCES_CONTENT, Gson().toJson(preferences))
                .apply()
    }

    fun clear() {
        sharedPreferences?.edit()?.clear()?.apply()
    }

    companion object {
        private const val PREFERENCES_KEY = "ThunderBoard"
        private const val PREFERENCES_CONTENT = "temperatureScale"
    }

    init {
        sharedPreferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
        preferences = retrievePreferences()
        Timber.d(preferences.toString())
    }
}