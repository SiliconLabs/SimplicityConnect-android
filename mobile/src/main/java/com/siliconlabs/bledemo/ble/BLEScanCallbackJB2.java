package com.siliconlabs.bledemo.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import com.siliconlabs.bledemo.bluetoothdatamodel.parsing.ScanRecordParser;

class BLEScanCallbackJB2 implements BluetoothAdapter.LeScanCallback {
    final BlueToothService service;

    BLEScanCallbackJB2(BlueToothService service) {
        this.service = service;
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        ScanResultCompat result = new ScanResultCompat();
        result.setDevice(device);
        result.setRssi(rssi);
        result.setTimestampNanos(System.currentTimeMillis() * 1000);
        result.setScanRecord(ScanRecordCompat.parseFromBytes(scanRecord));
        result.setAdvertData(ScanRecordParser.getAdvertisements(scanRecord));

        if (service.addDiscoveredDevice(result)) {
            //noinspection deprecation
            service.bluetoothAdapter.stopLeScan(this);
        }
    }
}
