package com.siliconlabs.bledemo.environment.control

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.ResourcesCompat
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.environment.model.TemperatureScale

class TemperatureControl(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : BaseControl(context, attrs, defStyleAttr) {

    fun setTemperature(temperature: Float? = null, temperatureType: Int) {
        tileValue.text =
        if (isEnabled && temperature != null) {
            String.format(
                if (temperatureType == TemperatureScale.FAHRENHEIT) context.getString(R.string.environment_temp_f) else context.getString(
                            R.string.environment_temp_c),
                if (temperatureType == TemperatureScale.FAHRENHEIT) temperature * 1.8f + 32f else temperature)
        } else {
            context.getString(R.string.environment_not_initialized)
        }
    }

    init {
        tileDescription.text = context.getString(R.string.environment_temp)
        tileIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.icon_temp, null))
        isEnabled = false

        setTemperature(null, 0)
    }
}