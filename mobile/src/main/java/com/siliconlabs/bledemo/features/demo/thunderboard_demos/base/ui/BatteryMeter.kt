package com.siliconlabs.bledemo.features.demo.thunderboard_demos.base.ui

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.siliconlabs.bledemo.R

class BatteryMeter @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                                             defStyleAttr: Int = 0) : AppCompatImageView(
        context, attrs, defStyleAttr) {
    fun setValue(value: Int) {
        val imageResource = when (value) {
            in 0..9 -> R.drawable.icon_battery_0
            in 10..25 -> R.drawable.icon_battery_1
            in 26..50 -> R.drawable.icon_battery_2
            in 51..75 -> R.drawable.icon_battery_3
            in 76..100 -> R.drawable.icon_battery_4
            else -> R.drawable.icon_battery_0
        }
        setImageResource(imageResource)
    }
}