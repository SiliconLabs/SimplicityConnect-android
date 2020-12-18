package com.siliconlabs.bledemo.BeaconUtils

import com.siliconlabs.bledemo.Bluetooth.BLE.BluetoothDeviceInfo
import com.siliconlabs.bledemo.R

enum class BleFormat(val nameResId: Int, val iconResId: Int) {
    UNSPECIFIED(R.string.unspecified, R.drawable.ic_beacon_immediate),
    I_BEACON(R.string.ibeacon, R.drawable.ic_beacon_ibeacon),
    ALT_BEACON(R.string.alt_beacon, R.drawable.ic_beacon_alt),
    EDDYSTONE(R.string.eddystone, R.drawable.ic_beacon_eddystone);

    companion object {
        private val IBEACON_BYTES_1 = byteArrayOf(0x02, 0x01)
        private val IBEACON_BYTES_2 = byteArrayOf(0x1A, 0xFF.toByte(), 0x4C, 0x00, 0x02, 0x15)
        private val IBEACON_BYTES_3 = byteArrayOf(0x02, 0x15)
        private val ALT_BEACON_BYTES_1 = byteArrayOf(0x1B.toByte(), 0xFF.toByte())
        private val ALT_BEACON_BYTES_2 = byteArrayOf(0xBE.toByte(), 0xAC.toByte())
        private const val EDDYSTONE_SERVICE_UUID = "feaa"

        fun getFormat(deviceInfo: BluetoothDeviceInfo): BleFormat {
            try {
                if (isAltBeacon(deviceInfo)) {
                    return ALT_BEACON
                }
                if (isIBeacon(deviceInfo)) {
                    return I_BEACON
                }
                if (isEddyStone(deviceInfo)) {
                    return EDDYSTONE
                }
            } catch (e: Exception) {
                return UNSPECIFIED
            }
            return UNSPECIFIED
        }

        private val hexArray = "0123456789ABCDEF".toCharArray()
        fun bytesToHex(bytes: ByteArray): String {
            val hexChars = CharArray(bytes.size * 2)
            for (j in bytes.indices) {
                val v: Int = bytes[j].toInt() and 0xFF
                hexChars[j * 2] = hexArray[v ushr 4]
                hexChars[j * 2 + 1] = hexArray[v and 0x0F]
            }
            return String(hexChars)
        }

        fun isAltBeacon(deviceInfo: BluetoothDeviceInfo): Boolean {
            val bytes = deviceInfo.scanInfo?.scanRecord?.bytes!!
            for (i in ALT_BEACON_BYTES_1.indices) {
                if (bytes[i] != ALT_BEACON_BYTES_1[i]) {
                    return false
                }
            }
            // number of bytes from beginning of ad length to beacon code (alt beacon code is big endian 0xBEAC)
            val byteSpacing = 4
            for (i in ALT_BEACON_BYTES_2.indices) {
                if (bytes[i + byteSpacing] != ALT_BEACON_BYTES_2[i]) {
                    return false
                }
            }
            return true
        }

        fun isEddyStone(deviceInfo: BluetoothDeviceInfo): Boolean {
            val uuidList = deviceInfo.scanInfo?.scanRecord?.serviceUuids
            if (uuidList != null && !uuidList.isEmpty()) {
                for (parcelUuid in uuidList) {
                    val parcelString = parcelUuid.toString().substring(4, 8).toLowerCase()
                    if (EDDYSTONE_SERVICE_UUID == parcelString) {
                        return true
                    }
                }
            }
            return false
        }

        // Returns true for Blue Gecko-sourced iBeacons. For other iBeacons, checks using isOtherIBeacon
        fun isIBeacon(deviceInfo: BluetoothDeviceInfo): Boolean {
            val bytes = deviceInfo.scanInfo?.scanRecord?.bytes!!
            for (i in IBEACON_BYTES_1.indices) {
                if (bytes[i] != IBEACON_BYTES_1[i]) {
                    return isOtherIBeacon(deviceInfo)
                }
            }
            for (i in IBEACON_BYTES_2.indices) {
                if (bytes[i + 3] != IBEACON_BYTES_2[i]) {
                    return isOtherIBeacon(deviceInfo)
                }
            }
            return true
        }

        // Used to determine if a beacon is an iBeacon, in the case that it is not recognized as a Blue Gecko-sourced iBeacon
        private fun isOtherIBeacon(deviceInfo: BluetoothDeviceInfo): Boolean {
            val bytes = deviceInfo.scanInfo?.scanRecord?.bytes!!
            val bytes2 = deviceInfo.scanInfo?.scanRecord?.getManufacturerSpecificData(0x004C)
            if (bytes2 != null) {
                if (indexOf(bytes, bytes2, 0) != -1) {
                    val index = indexOf(bytes2, IBEACON_BYTES_3, 0)
                    return index != -1
                }
            }
            return false
        }

        private fun indexOf(outerArray: ByteArray, smallerArray: ByteArray, start: Int): Int {
            for (i in start until outerArray.size - smallerArray.size + 1) {
                var found = true
                for (j in smallerArray.indices) {
                    if (outerArray[i + j] != smallerArray[j]) {
                        found = false
                        break
                    }
                }
                if (found) {
                    return i
                }
            }
            return -1
        }
    }

}