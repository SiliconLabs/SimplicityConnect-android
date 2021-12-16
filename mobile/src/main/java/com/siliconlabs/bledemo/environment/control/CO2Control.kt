package com.siliconlabs.bledemo.environment.control

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.ResourcesCompat
import com.siliconlabs.bledemo.R

class CO2Control(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : BaseControl(context, attrs, defStyleAttr) {

    fun setCO2(co2Level: Int? = null) {
        tileValue.text =
            if (isEnabled && co2Level != null) {
                String.format(context.getString(R.string.environment_co2_measure), co2Level)
            }
            else context.getString(R.string.environment_not_initialized)
    }

    init {
        tileDescription.text = context.getString(R.string.environment_co2)
        tileIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.icon_co2, null))
        setCO2()

        isEnabled = false
    }
}