package com.siliconlabs.bledemo.HealthThermometer.Fragments

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.siliconlabs.bledemo.HealthThermometer.Models.TemperatureReading
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.fragment_health_thermometer.*

class HealthThermometerActivityFragment : Fragment() {
    private var currentReading: TemperatureReading? = null
    private var currentType: TemperatureReading.Type? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_health_thermometer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        thermo_large_temperature.setFontFamily("sans-serif-thin", Typeface.NORMAL)
        type_switch.setOnCheckedChangeListener { _, isChecked -> onTabClick(!isChecked) }
    }

    private fun onTabClick(fahrenheit: Boolean) {
        currentType = if (fahrenheit) TemperatureReading.Type.FAHRENHEIT else TemperatureReading.Type.CELSIUS
        thermo_large_temperature.setCurrentType(currentType)
    }

    fun setCurrentReading(temperatureReading: TemperatureReading?) {
        currentReading = temperatureReading
        refreshUi()
    }

    fun setDeviceName(deviceName: String?) {
        thermometer_device_name?.text = deviceName
    }

    private fun refreshUi() {
        if (currentReading != null && isAdded) {
            thermo_large_temperature?.setTemperature(currentReading)
            thermo_type_value_text?.text = getString(currentReading?.htmType?.nameResId!!)
            thermo_large_time_text?.text = currentReading?.getFormattedTime()
        }
    }
}