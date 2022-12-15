package com.siliconlabs.bledemo.features.demo.thunderboard_demos.base.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.base.models.ThunderBoardDevice
import kotlinx.android.synthetic.main.battery_indicator.view.*

class BatteryIndicator @JvmOverloads constructor(context: Context,
                                                 attrs: AttributeSet? = null,
                                                 defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {


    fun setBatteryValue(powerSource: ThunderBoardDevice.PowerSource, batteryValue: Int) {
        when (powerSource) {
            ThunderBoardDevice.PowerSource.UNKNOWN -> {
                battery_percent.setText(R.string.unknown_power)
                battery_meter.setImageResource(R.drawable.icn_signal_unknown)
            }
            ThunderBoardDevice.PowerSource.USB -> {
                battery_percent.setText(R.string.usb_power)
                battery_meter.setImageResource(R.drawable.icon_usb)
            }
            else -> {
                battery_percent.text = String.format("%d%%", batteryValue)
                battery_meter.setValue(batteryValue)
            }
        }
    }

    init {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.battery_indicator, this, false)
        addView(view)
    }
}