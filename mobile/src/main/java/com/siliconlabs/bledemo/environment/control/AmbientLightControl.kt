package com.siliconlabs.bledemo.environment.control

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.ResourcesCompat
import com.siliconlabs.bledemo.R

class AmbientLightControl(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : BaseControl(context, attrs, defStyleAttr) {

    fun setAmbientLight(ambientLight: Long? = null) {
        tileValue.text =
            if (isEnabled && ambientLight != null) {
                String.format(context.getString(R.string.environment_ambient_lx), ambientLight)
            }
            else context.getString(R.string.environment_not_initialized)
    }

    init {
        tileDescription.text = context.getString(R.string.environment_ambient)
        tileIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.icon_light, null))
        setAmbientLight()

        isEnabled = false
    }
}