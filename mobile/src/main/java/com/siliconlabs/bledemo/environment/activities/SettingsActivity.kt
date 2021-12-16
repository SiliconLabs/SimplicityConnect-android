package com.siliconlabs.bledemo.environment.activities

import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import androidx.appcompat.widget.Toolbar
import butterknife.BindView
import butterknife.ButterKnife
import com.siliconlabs.bledemo.Base.BaseActivity
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.environment.model.TemperatureScale
import com.siliconlabs.bledemo.thunderboard.utils.PreferenceManager
import javax.inject.Inject

class SettingsActivity : BaseActivity() {
    @Inject
    lateinit var prefsManager: PreferenceManager

    @BindView(R.id.temperature_toggle)
    lateinit var temperatureToggle: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        ButterKnife.bind(this)
        getDaggerComponent().inject(this)
        prepareToolbar()
    }

    public override fun onResume() {
        super.onResume()
        loadPersonalize()
    }

    public override fun onPause() {
        super.onPause()
        saveSettings()
    }

    private fun prepareToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        findViewById<View>(R.id.iv_go_back).setOnClickListener { view: View? -> onBackPressed() }
    }

    private fun loadPersonalize() {
        prefsManager.preferences?.let {
            if (it.temperatureType == TemperatureScale.CELSIUS) {
                temperatureToggle.check(R.id.celsius)
            } else if (it.temperatureType == TemperatureScale.FAHRENHEIT) {
                temperatureToggle.check(R.id.fahrenheit)
            }
        }
    }

    private fun saveSettings() {
        prefsManager.preferences?.let {
            it.temperatureType =
                    if (temperatureToggle.checkedRadioButtonId == R.id.celsius) TemperatureScale.CELSIUS
                    else TemperatureScale.FAHRENHEIT
            prefsManager.savePreferences(it)
        }
    }
}