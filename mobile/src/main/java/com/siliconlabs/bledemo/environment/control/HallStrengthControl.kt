package com.siliconlabs.bledemo.environment.control

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.ResourcesCompat
import com.siliconlabs.bledemo.R

class HallStrengthControl(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : BaseControl(context, attrs, defStyleAttr) {

    fun setHallStrength(hallStrength: Float? = null) {
        tileValue.text =
            if (isEnabled && hallStrength != null) {
                context.getString(R.string.environment_hall_strength_measure, hallStrength)
            }
            else context.getString(R.string.environment_not_initialized)
    }

    init {
        tileDescription.text = context.getString(R.string.environment_hall_strength)
        tileIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.icon_magneticfield, null))
        setHallStrength()

        isEnabled = false
    }
}