package com.siliconlabs.bledemo.bluetooth.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.os.Build
import com.siliconlabs.bledemo.bluetooth.parsing.ScanRecordParser
import com.siliconlabs.bledemo.utils.Constants.NA
import com.siliconlabs.bledemo.utils.Objects.toString
import java.util.*

/**
 * Represents a compatible version of [ScanResult] from Lollipop or higher.
 */
class ScanResultCompat {
    var device: BluetoothDevice? = null
    var rssi = 0
    var scanRecord: ScanRecordCompat? = null
    var timestampNanos: Long = 0
    lateinit var advertData: ArrayList<String?>

    var isConnectable = false
    var isLegacy = true

    // Data for no legacy devices -- Bluetooth 5 advertising extension --
    var dataStatus = 0
    var primaryPhy = 0
    var secondaryPhy = 0
    var advertisingSetID = 0
    var txPower = 0
    var periodicAdvertisingInterval = 0

    fun getDisplayName(): String {
        val name = device?.name
        return name ?: NA
    }

    override fun toString(): String {
        return ("ScanResult{" + "device=" + device + ", scanRecord="
                + toString(scanRecord) + ", rssi=" + rssi + ", timestampNanos="
                + timestampNanos + '}')
    }

    companion object {
        fun from(lollipopScanResult: Any?): ScanResultCompat? {
            if (lollipopScanResult == null) {
                return null
            }

            val sr = lollipopScanResult as ScanResult

            return ScanResultCompat().apply {
                device = sr.device
                rssi = sr.rssi
                scanRecord = ScanRecordCompat.from(sr.scanRecord)
                advertData = ScanRecordParser.getAdvertisements(sr.scanRecord?.bytes)
                timestampNanos = sr.timestampNanos

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    isConnectable = sr.isConnectable
                    isLegacy = sr.isLegacy
                    dataStatus = sr.dataStatus
                    primaryPhy = sr.primaryPhy
                    secondaryPhy = sr.secondaryPhy
                    advertisingSetID = sr.advertisingSid
                    txPower = sr.txPower
                    periodicAdvertisingInterval = sr.periodicAdvertisingInterval
                } else {
                    isLegacy = true
                    isConnectable = true
                }
            }
        }
    }
}