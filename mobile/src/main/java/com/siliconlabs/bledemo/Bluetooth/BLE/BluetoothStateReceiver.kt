package com.siliconlabs.bledemo.bluetooth.ble

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * @author Comarch S.A.
 */
class BluetoothStateReceiver(private val bluetoothStateListener: BluetoothStateListener) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {

            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            when (state) {
                BluetoothAdapter.ERROR, BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF -> notifyState(false)
                BluetoothAdapter.STATE_ON -> notifyState(true)
            }
        }
    }

    private fun notifyState(enabled: Boolean) {
        bluetoothStateListener.onBluetoothStateChanged(enabled)
    }

    interface BluetoothStateListener {
        fun onBluetoothStateChanged(enabled: Boolean)
    }

}