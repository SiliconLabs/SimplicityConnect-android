package com.siliconlabs.bledemo.bluetooth.ble

import android.annotation.TargetApi
import android.bluetooth.le.ScanRecord
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import android.util.SparseArray
import java.util.*

/**
 * Represents a compatible version of [ScanRecord] from Lollipop or higher.
 */
class ScanRecordCompat {
    var bytes: ByteArray? = null
    var advertiseFlags = 0
    var deviceName: String? = null
    var manufacturerSpecificData: SparseArray<ByteArray>? = null
    var serviceData: Map<ParcelUuid, ByteArray>? = null
    var serviceUuids: List<ParcelUuid>? = null
    var txPowerLevel = 0

    internal constructor()
    private constructor(serviceUuids: List<ParcelUuid>?,
                        manufacturerData: SparseArray<ByteArray>?,
                        serviceData: Map<ParcelUuid, ByteArray>?,
                        advertiseFlag: Int, txPowerLevel: Int,
                        deviceName: String?, bytes: ByteArray) {
        this.serviceUuids = serviceUuids
        this.manufacturerSpecificData = manufacturerData
        this.serviceData = serviceData
        this.advertiseFlags = advertiseFlag
        this.txPowerLevel = txPowerLevel
        this.deviceName = deviceName
        this.bytes = bytes
    }


    fun getManufacturerSpecificData(manufacturer: Int): ByteArray? {
        return sr?.getManufacturerSpecificData(manufacturer)
    }

    override fun toString(): String {
        return ("ScanRecord [advertiseFlags=" + advertiseFlags + ", serviceUuids=" + serviceUuids
                + ", manufacturerSpecificData=" + toString(manufacturerSpecificData)
                + ", serviceData=" + toString(serviceData)
                + ", txPowerLevel=" + txPowerLevel + ", deviceName=" + deviceName + "]")
    }

    companion object {
        // The following data type values are assigned by Bluetooth SIG.
        // For more details refer to Bluetooth 4.1 specification, Volume 3, Part C, Section 18.
        private const val DATA_TYPE_FLAGS = 0x01
        private const val DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL = 0x02
        private const val DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE = 0x03
        private const val DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL = 0x04
        private const val DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE = 0x05
        private const val DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL = 0x06
        private const val DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE = 0x07
        private const val DATA_TYPE_LOCAL_NAME_SHORT = 0x08
        private const val DATA_TYPE_LOCAL_NAME_COMPLETE = 0x09
        private const val DATA_TYPE_TX_POWER_LEVEL = 0x0A
        private const val DATA_TYPE_SERVICE_DATA = 0x16
        private const val DATA_TYPE_MANUFACTURER_SPECIFIC_DATA = 0xFF
        private var sr: ScanRecord? = null

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        fun from(lollipopScanRecord: Any?): ScanRecordCompat? {
            if (lollipopScanRecord == null) {
                return null
            }
            sr = lollipopScanRecord as ScanRecord?

            return ScanRecordCompat().apply {
                advertiseFlags = sr?.advertiseFlags!!
                bytes = sr?.bytes!!
                deviceName = sr?.deviceName
                manufacturerSpecificData = sr?.manufacturerSpecificData
                serviceData = sr?.serviceData
                serviceUuids = sr?.serviceUuids
                txPowerLevel = sr?.txPowerLevel!!
            }
        }

        fun parseFromBytes(scanRecord: ByteArray?): ScanRecordCompat? {
            if (scanRecord == null) {
                return null
            }

            var currentPos = 0
            var advertiseFlag = -1
            var serviceUuids: MutableList<ParcelUuid>? = ArrayList()
            var localName: String? = null
            var txPowerLevel = Int.MIN_VALUE

            val manufacturerData = SparseArray<ByteArray>()
            val serviceData: MutableMap<ParcelUuid, ByteArray> = HashMap()

            return try {
                while (currentPos < scanRecord.size) {
                    // length is unsigned int.
                    val length: Int = scanRecord[currentPos++].toInt() and 0xFF
                    if (length == 0) {
                        break
                    }
                    // Note the length includes the length of the field type itself.
                    val dataLength = length - 1
                    // fieldType is unsigned int.
                    val fieldType: Int = scanRecord[currentPos++].toInt() and 0xFF
                    when (fieldType) {
                        DATA_TYPE_FLAGS -> advertiseFlag = scanRecord[currentPos].toInt() and 0xFF
                        DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL, DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE -> parseServiceUuid(scanRecord, currentPos, dataLength, BluetoothUuid.UUID_BYTES_16_BIT, serviceUuids)
                        DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL, DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE -> parseServiceUuid(scanRecord, currentPos, dataLength, BluetoothUuid.UUID_BYTES_32_BIT, serviceUuids)
                        DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL, DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE -> parseServiceUuid(scanRecord, currentPos, dataLength, BluetoothUuid.UUID_BYTES_128_BIT, serviceUuids)
                        DATA_TYPE_LOCAL_NAME_SHORT, DATA_TYPE_LOCAL_NAME_COMPLETE -> localName = String(extractBytes(scanRecord, currentPos, dataLength))
                        DATA_TYPE_TX_POWER_LEVEL -> txPowerLevel = scanRecord[currentPos].toInt()
                        DATA_TYPE_SERVICE_DATA -> {
                            // The first two bytes of the service data are service data UUID in little
                            // endian. The rest bytes are service data.
                            val serviceUuidLength = BluetoothUuid.UUID_BYTES_16_BIT
                            val serviceDataUuidBytes = extractBytes(scanRecord, currentPos, serviceUuidLength)
                            val serviceDataUuid = BluetoothUuid.parseUuidFrom(serviceDataUuidBytes)
                            val serviceDataArray = extractBytes(scanRecord, currentPos + serviceUuidLength, dataLength - serviceUuidLength)
                            serviceData[serviceDataUuid] = serviceDataArray
                        }
                        DATA_TYPE_MANUFACTURER_SPECIFIC_DATA -> {
                            // The first two bytes of the manufacturer specific data are
                            // manufacturer ids in little endian.
                            val manufacturerId: Int = (scanRecord[currentPos + 1].toInt() and 0xFF shl 8) + (scanRecord[currentPos].toInt() and 0xFF)
                            val manufacturerDataBytes = extractBytes(scanRecord, currentPos + 2, dataLength - 2)
                            manufacturerData.put(manufacturerId, manufacturerDataBytes)
                        }
                        else -> {
                        }
                    }
                    currentPos += dataLength
                }
                if (serviceUuids?.isEmpty()!!) {
                    serviceUuids = null
                }
                ScanRecordCompat(serviceUuids, manufacturerData, serviceData, advertiseFlag, txPowerLevel, localName, scanRecord)
            } catch (e: Exception) {
                Log.e("parseFromBytes", "unable to parse scan record: " + Arrays.toString(scanRecord) + e)
                // As the record is invalid, ignore all the parsed results for this packet
                // and return an empty record with raw scanRecord bytes in results
                ScanRecordCompat(null, null, null, -1, Int.MIN_VALUE, null, scanRecord)
            }
        }

        // Parse service UUIDs.
        private fun parseServiceUuid(scanRecord: ByteArray, currentPos: Int, dataLength: Int,
                                     uuidLength: Int, serviceUuids: MutableList<ParcelUuid>?): Int {
            var tmpCurrentPos = currentPos
            var tmpDataLength = dataLength

            while (tmpDataLength > 0) {
                val uuidBytes = extractBytes(scanRecord, tmpCurrentPos, uuidLength)
                serviceUuids?.add(BluetoothUuid.parseUuidFrom(uuidBytes))
                tmpDataLength -= uuidLength
                tmpCurrentPos += uuidLength
            }
            return tmpCurrentPos
        }

        // Helper method to extract bytes from byte array.
        private fun extractBytes(scanRecord: ByteArray, start: Int, length: Int): ByteArray {
            val bytes = ByteArray(length)
            System.arraycopy(scanRecord, start, bytes, 0, length)
            return bytes
        }

        private fun toString(array: SparseArray<ByteArray>?): String {
            if (array == null) {
                return "null"
            }
            if (array.size() == 0) {
                return "{}"
            }
            val buffer = StringBuilder()
            buffer.append('{')
            for (i in 0 until array.size()) {
                buffer.append(array.keyAt(i)).append("=").append(Arrays.toString(array.valueAt(i)))
            }
            buffer.append('}')
            return buffer.toString()
        }

        private fun <T> toString(map: Map<T, ByteArray>?): String {
            if (map == null) {
                return "null"
            }
            if (map.isEmpty()) {
                return "{}"
            }
            val buffer = StringBuilder()
            buffer.append('{')
            val it = map.entries.iterator()
            while (it.hasNext()) {
                val entry = it.next()
                val key: T = entry.key
                buffer.append(key).append("=").append(Arrays.toString(map[key]))
                if (it.hasNext()) {
                    buffer.append(", ")
                }
            }
            buffer.append('}')
            return buffer.toString()
        }
    }
}