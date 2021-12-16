package com.siliconlabs.bledemo.environment.control

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.ResourcesCompat
import com.siliconlabs.bledemo.R

class SoundLevelControl(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : BaseControl(context, attrs, defStyleAttr) {

    fun setSoundLevel(soundLevel: Int? = null) {
        tileValue.text =
            if (isEnabled && soundLevel != null) {
                String.format(context.getString(R.string.environment_sound_level_measure), soundLevel)
            }
            else context.getString(R.string.environment_not_initialized)
    }

    init {
        tileDescription.text = context.getString(R.string.environment_sound_level)
        tileIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.icon_sound, null))
        setSoundLevel()

        isEnabled = false
    }
}