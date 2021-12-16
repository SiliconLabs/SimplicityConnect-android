package com.siliconlabs.bledemo.environment.control

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.ResourcesCompat
import com.siliconlabs.bledemo.R

class PressureControl(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : BaseControl(context, attrs, defStyleAttr) {

    fun setPressure(pressure: Int? = null) {
        tileValue.text =
            if (isEnabled && pressure != null) {
                String.format(context.getString(R.string.environment_pressure_measure), pressure)
            }
            else context.getString(R.string.environment_not_initialized)
    }

    init {
        tileDescription.text = context.getString(R.string.environment_pressure)
        tileIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.icon_airpressure, null))
        setPressure()

        isEnabled = false
    }
}