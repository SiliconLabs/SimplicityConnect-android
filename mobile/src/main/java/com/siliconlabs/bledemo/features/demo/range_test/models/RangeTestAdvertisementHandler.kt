package com.siliconlabs.bledemo.features.demo.range_test.models

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import com.siliconlabs.bledemo.utils.Converters
import timber.log.Timber

/**
 * @author Comarch S.A.
 */
@SuppressLint("MissingPermission")
abstract class RangeTestAdvertisementHandler(context: Context, address: String) {
    private val bluetoothAdapter: BluetoothAdapter
    val address: String
    private var listener: Any? = null

    @Synchronized
    fun startListening() {
        if (listener != null) {
            stopListening()
        }
        run {
            val filter = ScanFilter.Builder()
                    .setDeviceAddress(address)
                    .build()
            val scanSettings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0)
                    .build()
            val advertisementListener = AdvertisementListener()
            bluetoothAdapter.bluetoothLeScanner.startScan(listOf(filter),
                    scanSettings,
                    advertisementListener
            )
            listener = advertisementListener
        }
    }

    @Synchronized
    fun stopListening() {
        if (listener == null) {
            return
        }
        run {
            val advertisementListener = listener as AdvertisementListener?
            bluetoothAdapter.bluetoothLeScanner.stopScan(advertisementListener)
        }
        listener = null
    }

    protected abstract fun handleAdvertisementRecord(manufacturerData: Int, companyId: Int, structureType: Int, rssi: Int, packetCount: Int, packetReceived: Int)

    @Synchronized
    private fun handleDeviceAdvertisement(device: BluetoothDevice, record: ByteArray) {
        if (this.address == device.address) {
            decodeAndNotify(record)
        }
    }

    @SuppressLint("BinaryOperationInTimber")
    private fun decodeAndNotify(record: ByteArray) {
        val manufacturerData = record[13].toInt()
        val companyId = Converters.calculateDecimalValue(record.copyOfRange(14, 16), false)
        val structureType = record[16].toInt()
        val rssi = record[17].toInt()
        val packetCount = Converters.calculateDecimalValue(record.copyOfRange(18, 20), false)
        val packetReceived = Converters.calculateDecimalValue(record.copyOfRange(20, 22), false)

        Timber.d("DecodedAdvData; " +
                "Address = $address, " +
                "M = $manufacturerData, " +
                "CID = $companyId, " +
                "T = $structureType, " +
                "RSSI = $rssi, " +
                "PC = $packetCount, " +
                "PR = $packetReceived")
        handleAdvertisementRecord(manufacturerData, companyId, structureType, rssi, packetCount, packetReceived)
    }

    private fun getBluetoothAdapter(context: Context): BluetoothAdapter {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter
    }

    private inner class AdvertisementListener : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleScanResult(result)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            for (result in results) {
                handleScanResult(result)
            }
        }

        private fun handleScanResult(result: ScanResult) {
            val device = result.device
            val record = result.scanRecord
            if (record != null) {
                handleDeviceAdvertisement(device, record.bytes)
            }
        }
    }

    init {
        bluetoothAdapter = getBluetoothAdapter(context)
        this.address = address
    }
}