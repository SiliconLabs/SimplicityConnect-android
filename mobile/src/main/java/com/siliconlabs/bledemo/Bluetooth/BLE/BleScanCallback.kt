package com.siliconlabs.bledemo.Bluetooth.BLE

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Handler
import android.os.Looper
import com.siliconlabs.bledemo.Bluetooth.BLE.ScanResultCompat.Companion.from
import com.siliconlabs.bledemo.Bluetooth.Services.BluetoothService
import timber.log.Timber

class BleScanCallback(private val service: BluetoothService) : ScanCallback() {
    private val handler: Handler = Handler(Looper.getMainLooper())

    override fun onScanResult(callbackType: Int, result: ScanResult) {
        Timber.d( "onScanResult: $result")
        service.addDiscoveredDevice(from(result)!!)
    }

    override fun onBatchScanResults(results: List<ScanResult>) {
        for (result in results) {
            Timber.d("onBatchScanResults: $result")
            service.addDiscoveredDevice(from(result)!!)
        }
    }

    override fun onScanFailed(errorCode: Int) {
        if (errorCode != SCAN_FAILED_ALREADY_STARTED) {
            handler.post { service.onDiscoveryCanceled() }
        }
    }

}