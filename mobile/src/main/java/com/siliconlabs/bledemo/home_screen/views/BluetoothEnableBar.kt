package com.siliconlabs.bledemo.home_screen.views

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
            warningBarMessage.text = context.getString(R.string.bluetooth_adapter_bar_turning_on)
            warningBarActionButton.visibility = View.GONE
        }
    }


}