package com.siliconlabs.bledemo.bluetooth.ble

import android.bluetooth.BluetoothDevice
import android.util.Log
import com.siliconlabs.bledemo.beacon_utils.BleFormat
import com.siliconlabs.bledemo.beacon_utils.BleFormat.Companion.getFormat
import java.util.*
import kotlin.math.min

open class BluetoothDeviceInfo : Cloneable {
    lateinit var device: BluetoothDevice

    protected var hasAdvertDetails = false
    private var connected = false
    var isConnectable = false
    var serviceDiscoveryFailed = false
    var areServicesBeingDiscovered = false
    var isNotOfInterest = false
    var isOfInterest = false

    private var bleFormat: BleFormat? = null
    var scanInfo: ScanResultCompat? = null
    var rawData: String? = null
    var gattHandle: Any? = null

    var intervalNanos = 0L
    var count = 0
    var timestampLast: Long = 0


    fun hasUnknownStatus(): Boolean {
        return !serviceDiscoveryFailed && !isNotOfInterest && !isOfInterest
    }

    fun isUnDiscovered(): Boolean {
        return gattHandle == null && hasUnknownStatus()
    }

    fun discover(bluetoothLEGatt: BluetoothLEGatt?) {
        isOfInterest = false
        isNotOfInterest = isOfInterest
        serviceDiscoveryFailed = isNotOfInterest
        gattHandle = bluetoothLEGatt
    }

    public override fun clone(): BluetoothDeviceInfo {
        val retVal: BluetoothDeviceInfo
        try {
            retVal = super.clone() as BluetoothDeviceInfo
            retVal.device = device
            retVal.scanInfo = scanInfo
            retVal.isOfInterest = isOfInterest
            retVal.isNotOfInterest = isNotOfInterest
            retVal.serviceDiscoveryFailed = serviceDiscoveryFailed
            retVal.bleFormat = bleFormat
            retVal.gattHandle = null
            retVal.connected = connected
            retVal.isConnectable = isConnectable
            retVal.rawData = rawData
            retVal.hasAdvertDetails = hasAdvertDetails
            retVal.areServicesBeingDiscovered = areServicesBeingDiscovered
            return retVal
        } catch (e: CloneNotSupportedException) {
            Log.e("clone", "Could not clone$e")
        }
        return BluetoothDeviceInfo()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is BluetoothDeviceInfo) {
            return false
        }
        return device == other.device && isOfInterest == other.isOfInterest && isNotOfInterest == other.isNotOfInterest && serviceDiscoveryFailed == other.serviceDiscoveryFailed
    }

    override fun hashCode(): Int {
        return device.hashCode()
    }

    override fun toString(): String {
        return scanInfo.toString()
    }

    fun getBleFormat(): BleFormat {
        if (bleFormat == null) {
            bleFormat = getFormat(this)
        }
        return bleFormat!!
    }

    fun setIntervalIfLower(intervalNanos: Long) {
        if (intervalNanos <= 0L) return
        if (this.intervalNanos == 0L) this.intervalNanos = intervalNanos
        else if (intervalNanos < this.intervalNanos * 0.7 && count < 10)
            this.intervalNanos = intervalNanos
        else if (intervalNanos < this.intervalNanos + 3000000) {
            val limitedCount = min(count, 10)
            this.intervalNanos = (this.intervalNanos * (limitedCount - 1) + intervalNanos) / limitedCount
        } else if (intervalNanos < this.intervalNanos * 1.4) {
            this.intervalNanos = (this.intervalNanos * 29 + intervalNanos) / 30
        }
    }


    val advertData: ArrayList<String?>
        get() = if (scanInfo != null) {
            scanInfo?.advertData!!
        } else ArrayList()

    fun setConnected(connected: Boolean) {
        this.connected = connected
    }

    var rssi: Int
        get() = scanInfo?.rssi!!
        set(rssi) {
            scanInfo?.rssi = rssi
        }

    val name: String?
        get() = device.name

    val address: String?
        get() = device.address

}