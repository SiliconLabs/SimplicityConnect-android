package com.siliconlabs.bledemo.environment.dialogs

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.environment.model.TemperatureScale
import com.siliconlabs.bledemo.environment.utils.PreferenceManager
import kotlinx.android.synthetic.main.dialog_settings.*

class SettingsDialog(
        context: Context,
        private val settingsHandler: SettingsHandler
) : AlertDialog(context) {

    private val prefsManager = PreferenceManager(context)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_settings)
        env_settings_go_back.setOnClickListener {
            handleSave()
            dismiss()
        }
        setOnCancelListener { handleSave() }
        loadPersonalize()
    }

    private fun handleSave() {
        saveSettings()
        settingsHandler.onSettingsSaved(prefsManager.retrievePreferences())
    }

    private fun loadPersonalize() {
        prefsManager.preferences.let {
            if (it.scale == TemperatureScale.CELSIUS) {
                temperature_toggle.check(R.id.celsius)
            } else if (it.scale == TemperatureScale.FAHRENHEIT) {
                temperature_toggle.check(R.id.fahrenheit)
            }
        }
    }

    private fun saveSettings() {
        prefsManager.preferences.let {
            it.scale =
                    if (temperature_toggle.checkedRadioButtonId == R.id.celsius) TemperatureScale.CELSIUS
                    else TemperatureScale.FAHRENHEIT
            prefsManager.savePreferences(it)
        }
    }

    interface SettingsHandler {
        fun onSettingsSaved(scale: TemperatureScale)
    }
}