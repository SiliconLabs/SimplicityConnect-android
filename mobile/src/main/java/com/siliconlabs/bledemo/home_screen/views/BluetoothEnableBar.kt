package com.siliconlabs.bledemo.home_screen.views

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import com.siliconlabs.bledemo.R

class BluetoothEnableBar(context: Context, attrs: AttributeSet?) :
    NoServiceWarningBar(context, attrs) {

    override fun initTexts() {
        _binding.apply {
            with(context) {
                warningBarMessage.text = getString(R.string.bluetooth_adapter_bar_disabled)
                warningBarActionButton.text =
                    getString(R.string.bluetooth_adapter_enable_bar_enable)
                warningBarInfoButton.visibility = View.GONE
            }
        }
    }

    override fun initClickListeners() {
        _binding.warningBarActionButton.setOnClickListener {
            enableBluetooth()
        }
    }

    fun resetState() {
        _binding.apply {
            warningBarMessage.text = context.getString(R.string.bluetooth_adapter_bar_disabled)
            warningBarActionButton.visibility = View.VISIBLE
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableBluetooth() {
        BluetoothAdapter.getDefaultAdapter().enable()
        _binding.apply {
            context.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            // Add a delay to check if Bluetooth is on after enabling
            Handler(Looper.getMainLooper()).postDelayed({
                warningBarMessage.text =
                    context.getString(R.string.bluetooth_adapter_bar_turning_on)
                if (bluetoothAdapter.isEnabled) {
                    // Bluetooth is on
                    // Display message or perform actions here
                    _binding.apply {
                        warningBarMessage.text = context.getString(R.string.toast_bluetooth_enabled)
                        warningBarActionButton.visibility = View.GONE
                    }
                } else {
                    // Bluetooth is still off
                    // Display message or perform actions here
                    _binding.apply {
                        warningBarMessage.text =
                            context.getString(R.string.bluetooth_adapter_bar_disabled)
                        warningBarActionButton.visibility = View.VISIBLE
                    }
                }
            }, DELAY_CHECK_BLUETOOTH)
        }
    }

    companion object {
        private const val DELAY_CHECK_BLUETOOTH = 5000L
    }
}