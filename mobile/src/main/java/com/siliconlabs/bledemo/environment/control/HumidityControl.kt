package com.siliconlabs.bledemo.environment.control

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.ResourcesCompat
import com.siliconlabs.bledemo.R

class HumidityControl(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : BaseControl(context, attrs, defStyleAttr) {

    fun setHumidity(humidity: Int? = null) {
        tileValue.text =
            if (isEnabled && humidity != null) {
                String.format(context.getString(R.string.environment_humidity_measure), humidity)
            }
            else context.getString(R.string.environment_not_initialized)
    }

    init {
        tileDescription.text = context.getString(R.string.environment_humidity)
        tileIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.icon_environment, null))
        setHumidity()

        isEnabled = false
    }
}