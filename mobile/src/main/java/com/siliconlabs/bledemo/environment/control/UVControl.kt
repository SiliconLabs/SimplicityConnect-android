package com.siliconlabs.bledemo.environment.control

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.ResourcesCompat
import com.siliconlabs.bledemo.R

class UVControl(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : BaseControl(context, attrs, defStyleAttr) {

    fun setUVIndex(uvIndex: Int? = null) {
        tileValue.text =
            if (isEnabled && uvIndex != null) {
                String.format(context.getString(R.string.environment_uv_unit), uvIndex)
            }
            else context.getString(R.string.environment_not_initialized)
    }

    init {
        tileDescription.setText(R.string.environment_uv)
        tileIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.icon_uv, null))
        setUVIndex()

        isEnabled = false
    }
}