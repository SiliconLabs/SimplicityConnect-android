package com.siliconlabs.bledemo.features.demo.thunderboard_demos.base.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.BatteryIndicatorBinding
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.base.models.ThunderBoardDevice

//import kotlinx.android.synthetic.main.battery_indicator.view.*

class BatteryIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private lateinit var binding: BatteryIndicatorBinding


    fun setBatteryValue(powerSource: ThunderBoardDevice.PowerSource, batteryValue: Int) {
        when (powerSource) {
            ThunderBoardDevice.PowerSource.UNKNOWN -> {

                binding.batteryPercent.setText(R.string.unknown_power)
                binding.batteryMeter.setImageResource(R.drawable.icn_signal_unknown)
            }

            ThunderBoardDevice.PowerSource.USB -> {
                binding.batteryPercent.setText(R.string.usb_power)
                binding.batteryMeter.setImageResource(R.drawable.icon_usb)
            }

            else -> {
                binding.batteryPercent.text = String.format("%d%%", batteryValue)
                binding.batteryMeter.setValue(batteryValue)
            }
        }
    }

    init {
        val inflater = LayoutInflater.from(context)
        binding = BatteryIndicatorBinding.inflate(inflater, this, false)
        addView(binding.root)
    }
}