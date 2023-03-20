package com.siliconlabs.bledemo.bluetooth.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import timber.log.Timber

/** Callback returning all types of bluetooth devices: classic, LE, dual and unrecognized. Used
 * when the scanning device does not support a LE feature.
 */
class BluetoothScanCallback(private val service: BluetoothService) : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BluetoothDevice.ACTION_FOUND) { // device discovered

            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            val name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME)
            val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, 0.toShort())

            val record = ScanRecordCompat().apply {
                deviceName = name
                advertiseFlags = -1
                txPowerLevel = Int.MIN_VALUE
            }
            val result = ScanResultCompat().apply {
                this.rssi = rssi.toInt()
                this.device = device
                this.scanRecord = record
            }

            Timber.d("Discovered bluetooth device: address = ${device?.address}, name = ${device?.name}")
            service.handleScanCallback(result)
        }
    }

}