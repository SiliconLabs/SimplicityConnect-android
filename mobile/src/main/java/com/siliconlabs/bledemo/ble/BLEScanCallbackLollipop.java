package com.siliconlabs.bledemo.ble;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.util.List;

import timber.log.Timber;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class BLEScanCallbackLollipop extends ScanCallback {
    final BlueToothService service;
    final Handler handler;

    BLEScanCallbackLollipop(BlueToothService service) {
        this.service = service;
        this.handler = new Handler();
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        final BluetoothDevice device = result.getDevice();

        Timber.d("Discovered bluetoothLE +" + result.toString());
        //Log.d("onScanResult","Discovered bluetoothLE +" + result.toString());
        if (service.addDiscoveredDevice(ScanResultCompat.from(result))) {
            service.bluetoothAdapter.getBluetoothLeScanner().stopScan(this);
        }
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
        for (ScanResult result : results) {
            final BluetoothDevice device = result.getDevice();

            Timber.d("Discovered bluetoothLE +" + device.getAddress() + " with name " + device.getName());
            //Log.d("onBatchScanResults","Discovered bluetoothLE +" + device.getAddress() + " with name " + device.getName());
            if (service.addDiscoveredDevice(ScanResultCompat.from(result))) {
                service.bluetoothAdapter.getBluetoothLeScanner().stopScan(this);
                break;
            }
        }
    }

    @Override
    public void onScanFailed(int errorCode) {
        if (errorCode != SCAN_FAILED_ALREADY_STARTED) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    service.onDiscoveryCanceled();
                }
            });
        }
    }
}
