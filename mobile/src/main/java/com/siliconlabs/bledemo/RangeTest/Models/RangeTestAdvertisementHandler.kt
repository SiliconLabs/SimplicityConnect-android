package com.siliconlabs.bledemo.RangeTest.Models

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log

/**
 * @author Comarch S.A.
 */
abstract class RangeTestAdvertisementHandler(context: Context, address: String?) {
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
            val advertisementListener = RangeTestAdvertisementListenerPost21()
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
            val advertisementListener = listener as RangeTestAdvertisementListenerPost21?
            bluetoothAdapter.bluetoothLeScanner.stopScan(advertisementListener)
        }
        listener = null
    }

    protected abstract fun handleAdvertisementRecord(manufacturerData: Int, companyId: Int, structureType: Int, rssi: Int, packetCount: Int, packetReceived: Int)

    @Synchronized
    private fun handleDeviceAdvertisement(device: BluetoothDevice, record: ByteArray) {
        val address = device.address
        if (this.address == address) {
            decodeAndNotify(record)
        }
    }

    private fun decodeAndNotify(record: ByteArray) {
        val manufacturerData = record[13].toInt()
        val companyId = unsignedIntFromLittleEndian(record[15], record[14])
        val structureType = record[16].toInt()
        val rssi = record[17].toInt()
        val packetCount = unsignedIntFromLittleEndian(record[19], record[18])
        val packetReceived = unsignedIntFromLittleEndian(record[21], record[20])
        Log.d("RangeAdvData", String.format("Address: %s, M: %d, CID: %d, T: %d, RSSI: %d, PC: %d, PR: %d",
                address, manufacturerData, companyId, structureType, rssi, packetCount, packetReceived))
        handleAdvertisementRecord(manufacturerData, companyId, structureType, rssi, packetCount, packetReceived)
    }

    private fun unsignedIntFromLittleEndian(vararg bytes: Byte): Int {
        var value = 0
        for (b in bytes) {
            value = value shl 8 or (b.toInt() and 0xFF)
        }
        return value
    }

    private fun getBluetoothAdapter(context: Context): BluetoothAdapter {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter
    }

    private inner class RangeTestAdvertisementListenerPost21 : ScanCallback() {
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
        requireNotNull(address) { "Address cannot be null" }
        bluetoothAdapter = getBluetoothAdapter(context)
        this.address = address
    }
}