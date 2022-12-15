package com.siliconlabs.bledemo.home_screen.views;

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.BluetoothEnableBarBinding

class BluetoothEnableBar(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {

    private val viewBinding = BluetoothEnableBarBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        viewBinding.bluetoothEnableBtn.setOnClickListener { changeEnableBluetoothAdapterToConnecting() }
    }

    fun resetState() {
        viewBinding.apply {
            root.setBackgroundColor(ContextCompat.getColor(context, R.color.blue_primary))
            bluetoothEnableMsg.text = context.getString(R.string.bluetooth_adapter_bar_disabled)
            bluetoothEnableBtn.visibility = View.VISIBLE
        }
    }

    fun hide() {
        viewBinding.root.visibility = View.GONE
    }

    private fun changeEnableBluetoothAdapterToConnecting() {
        BluetoothAdapter.getDefaultAdapter().enable()
        viewBinding.apply {
            root.setBackgroundColor(ContextCompat.getColor(context, R.color.blue_primary))
            bluetoothEnableMsg.text = context.getString(R.string.bluetooth_adapter_bar_turning_on)
            bluetoothEnableBtn.visibility = View.GONE
        }
    }
}