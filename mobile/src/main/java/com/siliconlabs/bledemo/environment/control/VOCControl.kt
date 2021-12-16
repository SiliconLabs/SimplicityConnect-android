package com.siliconlabs.bledemo.environment.control

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.ResourcesCompat
import com.siliconlabs.bledemo.R

class VOCControl(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : BaseControl(context, attrs, defStyleAttr) {

    fun setVOC(vocLevel: Int? = null) {
        tileValue.text =
            if (isEnabled && vocLevel != null) {
                String.format(context.getString(R.string.environment_voc_measure), vocLevel)
            }
            else context.getString(R.string.environment_not_initialized)
    }

    init {
        tileDescription.setText(R.string.environment_vocs)
        tileIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.icon_vocs, null))
        setVOC()

        isEnabled = false
    }
}