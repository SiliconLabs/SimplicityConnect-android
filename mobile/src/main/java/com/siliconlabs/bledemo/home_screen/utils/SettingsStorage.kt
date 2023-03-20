package com.siliconlabs.bledemo.home_screen.utils

import android.content.Context

class SettingsStorage(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_SETTINGS_STORAGE, Context.MODE_PRIVATE)

    fun loadScanSetting() = preferences.getInt(KEY_SCAN_TIMEOUT, SCAN_SETTING_INFINITE)

    fun saveScanSetting(secondsToTimeout: Int) {
        with (preferences.edit()) {
            putInt(KEY_SCAN_TIMEOUT, secondsToTimeout)
            apply()
        }
    }

    companion object {
        private const val PREFS_SETTINGS_STORAGE = "com.siliconlabs.bledemo.PREFS_SETTINGS_STORAGE"
        private const val KEY_SCAN_TIMEOUT = "com.siliconlabs.bledemo.KEY_SCAN_TIMEOUT"

        const val SCAN_SETTING_INFINITE = 0
        const val SCAN_SETTING_SECONDS = 15
        const val SCAN_SETTING_MINUTE = 1 * 60
        const val SCAN_SETTING_TWO_MINUTES = 2 * 60
        const val SCAN_SETTING_FIVE_MINUTES = 5 * 60
        const val SCAN_SETTING_TEN_MINUTES = 10 * 60
    }
}