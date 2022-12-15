package com.siliconlabs.bledemo.bluetooth.ble

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Handler
import android.os.Looper
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import timber.log.Timber

/** Callback returning bluetooth devices of types: LE, dual, unrecognized. Used when the scanning
 * device supports LE feature.
 */
class BleScanCallback(private val service: BluetoothService) : ScanCallback() {
    private val handler: Handler = Handler(Looper.getMainLooper())

    override fun onScanResult(callbackType: Int, result: ScanResult) {
        Timber.d( "onScanResult: $result")
        service.handleScanCallback(ScanResultCompat.from(result))
    }

    override fun onBatchScanResults(results: List<ScanResult>) {
        results.forEach {
            Timber.d("onBatchScanResults: $it")
            service.handleScanCallback(ScanResultCompat.from(it))
        }
    }

    override fun onScanFailed(errorCode: Int) {
        if (errorCode != SCAN_FAILED_ALREADY_STARTED) {
            handler.post { service.onDiscoveryFailed(BluetoothService.ScanError.ScannerError, errorCode) }
        }
    }

}