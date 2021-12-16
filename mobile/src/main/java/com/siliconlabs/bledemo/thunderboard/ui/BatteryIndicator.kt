package com.siliconlabs.bledemo.thunderboard.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.thunderboard.model.ThunderBoardDevice

class BatteryIndicator @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                                                 defStyleAttr: Int = 0) : FrameLayout(
        context, attrs, defStyleAttr) {

    @BindView(R.id.battery_percent)
    lateinit var batteryPercent: TextView

    @BindView(R.id.battery_meter)
    lateinit var batteryMeter: BatteryMeter

    fun setBatteryValue(powerSource: ThunderBoardDevice.PowerSource, batteryValue: Int) {
        if (powerSource == ThunderBoardDevice.PowerSource.UNKNOWN) {
            batteryPercent.setText(R.string.unknown_power)
            batteryMeter.setImageResource(R.drawable.icn_signal_unknown)
        } else if (powerSource == ThunderBoardDevice.PowerSource.USB) {
            batteryPercent.setText(R.string.usb_power)
            batteryMeter.setImageResource(R.drawable.icon_usb)
        } else {
            batteryPercent.text = String.format("%d%%", batteryValue)
            batteryMeter.setValue(batteryValue)
        }
    }

    init {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.battery_indicator, this, false)
        addView(view)
        ButterKnife.bind(this, view)
    }
}