package com.siliconlabs.bledemo.views;

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.bluetooth_enable_bar.view.*

class BluetoothEnableBar(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    constructor(context: Context) : this(context, null) {}

    fun show() {
        bluetooth_enable_msg?.setText(com.siliconlabs.bledemo.R.string.bluetooth_adapter_bar_disabled)
        setBackgroundColor(ContextCompat.getColor(context, com.siliconlabs.bledemo.R.color.silabs_red_dark))
        bluetooth_enable_btn.visibility = View.VISIBLE
        visibility = View.VISIBLE
    }

    fun hide() {
        visibility = View.GONE
    }

    fun changeEnableBluetoothAdapterToConnecting() {
        BluetoothAdapter.getDefaultAdapter().enable()
        bluetooth_enable_btn.visibility = View.GONE
        bluetooth_enable_msg?.setText(com.siliconlabs.bledemo.R.string.bluetooth_adapter_bar_turning_on)
        setBackgroundColor(ContextCompat.getColor(context, com.siliconlabs.bledemo.R.color.silabs_blue))
    }
}