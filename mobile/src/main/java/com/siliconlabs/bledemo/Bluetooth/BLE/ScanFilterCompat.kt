package com.siliconlabs.bledemo.Bluetooth.BLE

import android.annotation.TargetApi
import android.bluetooth.le.ScanFilter
import android.os.Build
import android.os.ParcelUuid
import com.siliconlabs.bledemo.utils.Objects.deepEquals
import com.siliconlabs.bledemo.utils.Objects.equals
import com.siliconlabs.bledemo.utils.Objects.hash
import com.siliconlabs.bledemo.utils.Objects.toString

/**
 * Represents a compatible version of [ScanFilter] from Lollipop or higher.
 */
class ScanFilterCompat {
    var serviceData: ByteArray? = null
    var serviceDataMask: ByteArray? = null
    var manufacturerData: ByteArray? = null
    var manufacturerDataMask: ByteArray? = null
    var deviceName: String? = null
    var serviceUuid: ParcelUuid? = null
    var serviceUuidMask: ParcelUuid? = null
    var deviceAddress: String? = null
    var serviceDataUuid: ParcelUuid? = null
    var manufacturerId = -1

    override fun toString(): String {
        return ("BluetoothLeScanFilter [mDeviceName=" + deviceName + ", mDeviceAddress="
                + deviceAddress
                + ", mUuid=" + serviceUuid + ", mUuidMask=" + serviceUuidMask
                + ", mServiceDataUuid=" + toString(serviceDataUuid) + ", mServiceData="
                + serviceData?.contentToString() + ", mServiceDataMask="
                + serviceDataMask?.contentToString() + ", mManufacturerId=" + manufacturerId
                + ", mManufacturerData=" + manufacturerData?.contentToString()
                + ", mManufacturerDataMask=" + manufacturerDataMask?.contentToString() + "]")
    }

    override fun hashCode(): Int {
        return hash(deviceName, deviceAddress, manufacturerId, manufacturerData,
                manufacturerDataMask, serviceDataUuid, serviceData, serviceDataMask,
                serviceUuid, serviceUuidMask)
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj !is ScanFilterCompat) {
            return false
        }
        val other = obj
        return equals(deviceName, other.deviceName) &&
                equals(deviceAddress, other.deviceAddress) &&
                manufacturerId == other.manufacturerId &&
                deepEquals(manufacturerData, other.manufacturerData) &&
                deepEquals(manufacturerDataMask, other.manufacturerDataMask) &&
                deepEquals(serviceDataUuid, other.serviceDataUuid) &&
                deepEquals(serviceData, other.serviceData) &&
                deepEquals(serviceDataMask, other.serviceDataMask) &&
                equals(serviceUuid, other.serviceUuid) &&
                equals(serviceUuidMask, other.serviceUuidMask)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun createScanFilter(): ScanFilter {
        val builder = ScanFilter.Builder()
                .setDeviceAddress(deviceAddress)
                .setDeviceName(deviceName)
        if (serviceUuid != null) {
            builder.setServiceUuid(serviceUuid, serviceUuidMask)
        }
        if (serviceDataUuid != null) {
            builder.setServiceData(serviceDataUuid, serviceData, serviceDataMask)
        }
        if (manufacturerId >= 0) {
            builder.setManufacturerData(manufacturerId, manufacturerData, manufacturerDataMask)
        }
        return builder.build()
    }
}
