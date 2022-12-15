package com.siliconlabs.bledemo.features.demo.connected_lighting.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.siliconlabs.bledemo.R

enum class TriggerSource(val value: Int, @DrawableRes val iconId: Int, @StringRes val textResId: Int) {
    UNKNOWN(-1, android.R.color.transparent, R.string.light_demo_protocol_empty),
    BLUETOOTH(0, R.drawable.icon_bluetooth, R.string.light_demo_protocol_bluetooth),
    ZIGBEE(1, R.drawable.icon_zigbee, R.string.light_demo_protocol_zigbee),
    BUTTON(2, android.R.color.transparent, R.string.light_demo_local_control),
    PROPRIETARY(5, R.drawable.icon_proprietary, R.string.light_demo_protocol_proprietary),
    CONNECT(6, R.drawable.icon_proprietary, R.string.light_demo_protocol_connect),
    THREAD(7, R.drawable.icon_thread, R.string.light_demo_protocol_thread);

    companion object {
        fun forValue(value: Int): TriggerSource {
            for (triggerSource in values()) {
                if (triggerSource.value == value) {
                    return triggerSource
                }
            }
            return UNKNOWN
        }
    }

}