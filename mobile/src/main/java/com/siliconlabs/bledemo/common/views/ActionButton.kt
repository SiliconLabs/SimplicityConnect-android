package com.siliconlabs.bledemo.common.views

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import com.siliconlabs.bledemo.bluetooth.ble.BluetoothDeviceInfo
import com.siliconlabs.bledemo.R


class ActionButton(context: Context, attrs: AttributeSet) : MaterialButton(context, attrs) {
    companion object {
        private val STATE_IS_ACTION_ON = intArrayOf(R.attr.is_action_on)
    }
    private var isActionOn = false

    fun setActionButtonState(connectionState: BluetoothDeviceInfo.ConnectionState) {
        when (connectionState) {
            BluetoothDeviceInfo.ConnectionState.CONNECTED -> {
                isEnabled = true
                isActionOn = true
            }
            BluetoothDeviceInfo.ConnectionState.CONNECTING -> {
                isEnabled = false
                isActionOn = false
            }
            BluetoothDeviceInfo.ConnectionState.DISCONNECTED -> {
                isEnabled = true
                isActionOn = false
            }
        }
        refreshDrawableState()
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (isActionOn) {
            mergeDrawableStates(drawableState, STATE_IS_ACTION_ON)
        }
        return drawableState
    }
}