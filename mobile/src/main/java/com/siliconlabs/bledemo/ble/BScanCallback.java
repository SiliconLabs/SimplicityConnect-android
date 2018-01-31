package com.siliconlabs.bledemo.ble;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import timber.log.Timber;

class BScanCallback extends BroadcastReceiver {
    final BlueToothService service;

    BScanCallback(BlueToothService service) {
        this.service = service;
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        // When discovery finds a device
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            // Get the BluetoothDevice object from the Intent
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
            int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0);

            ScanRecordCompat record = new ScanRecordCompat();
            record.setDeviceName(name);
            record.setAdvertiseFlags(-1);
            record.setTxPowerLevel(Integer.MIN_VALUE);
            ScanResultCompat result = new ScanResultCompat();
            result.setRssi(rssi);
            result.setDevice(device);
            result.setScanRecord(record);


            Timber.d("Discovered bluetooth +" + device.getAddress() + " with name " + device.getName());
            //Log.d("onReceive", "Discovered bluetooth +" + device.getAddress() + " with name " + device.getName());
            if (service.addDiscoveredDevice(result)) {
                service.bluetoothAdapter.cancelDiscovery();
            }
        }
    }
}
