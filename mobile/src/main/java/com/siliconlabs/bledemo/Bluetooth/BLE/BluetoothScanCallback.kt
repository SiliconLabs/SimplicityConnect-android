package com.siliconlabs.bledemo.Bluetooth.BLE

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.siliconlabs.bledemo.Bluetooth.Services.BluetoothService
import timber.log.Timber

class BluetoothScanCallback(private val service: BluetoothService) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        // When discovery finds a device
        if (BluetoothDevice.ACTION_FOUND == action) {
            // Get the BluetoothDevice object from the Intent
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            val name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME)
            val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, 0.toShort())

            val record = ScanRecordCompat()
            record.deviceName = name
            record.advertiseFlags = -1
            record.txPowerLevel = Int.MIN_VALUE

            val result = ScanResultCompat()
            result.rssi = rssi.toInt()
            result.device = device
            result.scanRecord = record

            Timber.d("onReceive; discovered bluetooth: address = ${device?.address}, name = ${device?.name}")
            service.addDiscoveredDevice(result)
        }
    }

}