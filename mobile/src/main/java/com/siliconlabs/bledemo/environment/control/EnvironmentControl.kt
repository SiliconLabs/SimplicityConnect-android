package com.siliconlabs.bledemo.environment.control

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.LinearLayout
import androidx.gridlayout.widget.GridLayout
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.environment.model.TemperatureScale
import kotlinx.android.synthetic.main.environmentdemo_tile.view.*

open class EnvironmentControl(
        context: Context,
        description: String?,
        icon: Drawable?
) : LinearLayout(context, null, 0) {

    constructor(context: Context) : this(context, null, null)

    private val tileView: View = inflate(context, R.layout.environmentdemo_tile, this)


    fun setTemperature(temperature: Float, temperatureType: Int) {
        tileView.env_value.text = String.format(
                    if (temperatureType == TemperatureScale.FAHRENHEIT) context.getString(R.string.environment_temp_f) else context.getString(
                            R.string.environment_temp_c),
                    if (temperatureType == TemperatureScale.FAHRENHEIT) temperature * 1.8f + 32f else temperature)
    }

    fun setHumidity(humidity: Int) {
        tileView.env_value.text = String.format(context.getString(R.string.environment_humidity_measure), humidity)
    }

    fun setUVIndex(uvIndex: Int) {
        tileView.env_value.text = String.format(context.getString(R.string.environment_uv_unit), uvIndex)
    }

    fun setAmbientLight(ambientLight: Long) {
        tileView.env_value.text = String.format(context.getString(R.string.environment_ambient_lx), ambientLight)
    }

    fun setSoundLevel(soundLevel: Int) {
        tileView.env_value.text = String.format(context.getString(R.string.environment_sound_level_measure), soundLevel)
    }

    fun setPressure(pressure: Long) {
        tileView.env_value.text = String.format(context.getString(R.string.environment_pressure_measure), pressure)
    }

    fun setCO2(co2Level: Int) {
        tileView.env_value.text = String.format(context.getString(R.string.environment_co2_measure), co2Level)
    }

    fun setVOC(vocLevel: Int) {
        tileView.env_value.text = String.format(context.getString(R.string.environment_voc_measure), vocLevel)
    }

    fun setHallStrength(hallStrength: Int) {
        tileView.env_value.text = context.getString(R.string.environment_hall_strength_measure, hallStrength)
    }

    init {
        tileView.apply {
            env_description.text = description
            env_value.text = context.getString(R.string.environment_not_initialized)
            env_icon.setImageDrawable(icon)
        }
        layoutParams = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply {
            width = 0
        }
    }

}