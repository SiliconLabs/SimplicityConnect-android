package com.siliconlabs.bledemo.Bluetooth.BLE

import android.os.ParcelUuid
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// THIS IS MODIFIED COPY OF THE "L" PLATFORM CLASS. BE CAREFUL ABOUT EDITS.
// THIS CODE SHOULD FOLLOW ANDROID STYLE.
/**
 * Static helper methods and constants to decode the ParcelUuid of remote devices.
 */
object BluetoothUuid {
    /*
     * See Bluetooth Assigned Numbers document - SDP section, to get the values of UUIDs for the
     * various services. The following 128 bit values are calculated as: uuid * 2^96 + BASE_UUID
     */
    val AudioSink = ParcelUuid.fromString("0000110B-0000-1000-8000-00805F9B34FB")
    val AudioSource = ParcelUuid.fromString("0000110A-0000-1000-8000-00805F9B34FB")
    val AdvAudioDist = ParcelUuid.fromString("0000110D-0000-1000-8000-00805F9B34FB")
    val HSP = ParcelUuid.fromString("00001108-0000-1000-8000-00805F9B34FB")
    val HSP_AG = ParcelUuid.fromString("00001112-0000-1000-8000-00805F9B34FB")
    val Handsfree = ParcelUuid.fromString("0000111E-0000-1000-8000-00805F9B34FB")
    val Handsfree_AG = ParcelUuid.fromString("0000111F-0000-1000-8000-00805F9B34FB")
    val AvrcpController = ParcelUuid.fromString("0000110E-0000-1000-8000-00805F9B34FB")
    val AvrcpTarget = ParcelUuid.fromString("0000110C-0000-1000-8000-00805F9B34FB")
    val ObexObjectPush = ParcelUuid.fromString("00001105-0000-1000-8000-00805f9b34fb")
    val Hid = ParcelUuid.fromString("00001124-0000-1000-8000-00805f9b34fb")
    val Hogp = ParcelUuid.fromString("00001812-0000-1000-8000-00805f9b34fb")
    val PANU = ParcelUuid.fromString("00001115-0000-1000-8000-00805F9B34FB")
    val NAP = ParcelUuid.fromString("00001116-0000-1000-8000-00805F9B34FB")
    val BNEP = ParcelUuid.fromString("0000000f-0000-1000-8000-00805F9B34FB")
    val PBAP_PSE = ParcelUuid.fromString("0000112f-0000-1000-8000-00805F9B34FB")
    val MAP = ParcelUuid.fromString("00001134-0000-1000-8000-00805F9B34FB")
    val MNS = ParcelUuid.fromString("00001133-0000-1000-8000-00805F9B34FB")
    val MAS = ParcelUuid.fromString("00001132-0000-1000-8000-00805F9B34FB")
    val BASE_UUID = ParcelUuid.fromString("00000000-0000-1000-8000-00805F9B34FB")

    /**
     * Length of bytes for 16 bit UUID
     */
    const val UUID_BYTES_16_BIT = 2

    /**
     * Length of bytes for 32 bit UUID
     */
    const val UUID_BYTES_32_BIT = 4

    /**
     * Length of bytes for 128 bit UUID
     */
    const val UUID_BYTES_128_BIT = 16
    val RESERVED_UUIDS = arrayOf(
            AudioSink, AudioSource, AdvAudioDist, HSP, Handsfree, AvrcpController, AvrcpTarget,
            ObexObjectPush, PANU, NAP, MAP, MNS, MAS)

    fun isAudioSource(uuid: ParcelUuid): Boolean {
        return uuid == AudioSource
    }

    fun isAudioSink(uuid: ParcelUuid): Boolean {
        return uuid == AudioSink
    }

    fun isAdvAudioDist(uuid: ParcelUuid): Boolean {
        return uuid == AdvAudioDist
    }

    fun isHandsfree(uuid: ParcelUuid): Boolean {
        return uuid == Handsfree
    }

    fun isHeadset(uuid: ParcelUuid): Boolean {
        return uuid == HSP
    }

    fun isAvrcpController(uuid: ParcelUuid): Boolean {
        return uuid == AvrcpController
    }

    fun isAvrcpTarget(uuid: ParcelUuid): Boolean {
        return uuid == AvrcpTarget
    }

    fun isInputDevice(uuid: ParcelUuid): Boolean {
        return uuid == Hid
    }

    fun isPanu(uuid: ParcelUuid): Boolean {
        return uuid == PANU
    }

    fun isNap(uuid: ParcelUuid): Boolean {
        return uuid == NAP
    }

    fun isBnep(uuid: ParcelUuid): Boolean {
        return uuid == BNEP
    }

    fun isMap(uuid: ParcelUuid): Boolean {
        return uuid == MAP
    }

    fun isMns(uuid: ParcelUuid): Boolean {
        return uuid == MNS
    }

    fun isMas(uuid: ParcelUuid): Boolean {
        return uuid == MAS
    }

    /**
     * Returns true if ParcelUuid is present in uuidArray
     *
     * @param uuidArray - Array of ParcelUuids
     * @param uuid
     */
    fun isUuidPresent(uuidArray: Array<ParcelUuid>?, uuid: ParcelUuid?): Boolean {
        if ((uuidArray == null || uuidArray.size == 0) && uuid == null) {
            return true
        }
        if (uuidArray == null) {
            return false
        }
        for (element in uuidArray) {
            if (element == uuid) {
                return true
            }
        }
        return false
    }

    /**
     * Returns true if there any common ParcelUuids in uuidA and uuidB.
     *
     * @param uuidA - List of ParcelUuids
     * @param uuidB - List of ParcelUuids
     */
    fun containsAnyUuid(uuidA: Array<ParcelUuid?>?, uuidB: Array<ParcelUuid?>?): Boolean {
        if (uuidA == null && uuidB == null) {
            return true
        }
        if (uuidA == null) {
            return uuidB?.size == 0
        }
        if (uuidB == null) {
            return uuidA.isEmpty()
        }
        val uuidSet = HashSet(Arrays.asList<ParcelUuid>(*uuidA))
        for (uuid in uuidB) {
            if (uuidSet.contains(uuid)) {
                return true
            }
        }
        return false
    }

    /**
     * Returns true if all the ParcelUuids in ParcelUuidB are present in ParcelUuidA
     *
     * @param uuidA - Array of ParcelUuidsA
     * @param uuidB - Array of ParcelUuidsB
     */
    fun containsAllUuids(uuidA: Array<ParcelUuid?>?, uuidB: Array<ParcelUuid?>?): Boolean {
        if (uuidA == null && uuidB == null) {
            return true
        }
        if (uuidA == null) {
            return uuidB?.isEmpty()!!
        }
        if (uuidB == null) {
            return true
        }
        val uuidSet = HashSet(Arrays.asList<ParcelUuid>(*uuidA))
        for (uuid in uuidB) {
            if (!uuidSet.contains(uuid)) {
                return false
            }
        }
        return true
    }

    /**
     * Extract the Service Identifier or the actual uuid from the Parcel Uuid. For example, if
     * 0000110B-0000-1000-8000-00805F9B34FB is the parcel Uuid, this function will return 110B
     *
     * @param parcelUuid
     * @return the service identifier.
     */
    fun getServiceIdentifierFromParcelUuid(parcelUuid: ParcelUuid): Int {
        val uuid = parcelUuid.uuid
        val value = uuid.mostSignificantBits and 0x0000FFFF00000000L ushr 32
        return value.toInt()
    }

    /**
     * Parse UUID from bytes. The `uuidBytes` can represent a 16-bit, 32-bit or 128-bit UUID,
     * but the returned UUID is always in 128-bit format. Note UUID is little endian in Bluetooth.
     *
     * @param uuidBytes Byte representation of uuid.
     * @return [ParcelUuid] parsed from bytes.
     * @throws IllegalArgumentException If the `uuidBytes` cannot be parsed.
     */
    fun parseUuidFrom(uuidBytes: ByteArray?): ParcelUuid {
        requireNotNull(uuidBytes) { "uuidBytes cannot be null" }
        val length = uuidBytes.size
        require(!(length != UUID_BYTES_16_BIT && length != UUID_BYTES_32_BIT && length != UUID_BYTES_128_BIT)) { "uuidBytes length invalid - $length" }
        // Construct a 128 bit UUID.
        if (length == UUID_BYTES_128_BIT) {
            val buf = ByteBuffer.wrap(uuidBytes).order(ByteOrder.LITTLE_ENDIAN)
            val msb = buf.getLong(8)
            val lsb = buf.getLong(0)
            return ParcelUuid(UUID(msb, lsb))
        }
        // For 16 bit and 32 bit UUID we need to convert them to 128 bit value.
        // 128_bit_value = uuid * 2^96 + BASE_UUID
        var shortUuid: Long
        if (length == UUID_BYTES_16_BIT) {
            shortUuid = uuidBytes[0].toLong() and 0xFF
            shortUuid += (uuidBytes[1].toLong() and 0xFF) shl 8
        } else {
            shortUuid = uuidBytes[0].toLong() and 0xFF
            shortUuid += (uuidBytes[1].toLong() and 0xFF) shl 8
            shortUuid += (uuidBytes[2].toLong() and 0xFF) shl 16
            shortUuid += (uuidBytes[3].toLong()) and 0xFF shl 24
        }
        val msb = BASE_UUID.uuid.mostSignificantBits + (shortUuid shl 32)
        val lsb = BASE_UUID.uuid.leastSignificantBits
        return ParcelUuid(UUID(msb, lsb))
    }

    /**
     * Check whether the given parcelUuid can be converted to 16 bit bluetooth uuid.
     *
     * @param parcelUuid
     * @return true if the parcelUuid can be converted to 16 bit uuid, false otherwise.
     */
    fun is16BitUuid(parcelUuid: ParcelUuid): Boolean {
        val uuid = parcelUuid.uuid
        return if (uuid.leastSignificantBits != BASE_UUID.uuid.leastSignificantBits) {
            false
        } else uuid.mostSignificantBits and -0xffff00000001L == 0x1000L
    }

    /**
     * Check whether the given parcelUuid can be converted to 32 bit bluetooth uuid.
     *
     * @param parcelUuid
     * @return true if the parcelUuid can be converted to 32 bit uuid, false otherwise.
     */
    fun is32BitUuid(parcelUuid: ParcelUuid): Boolean {
        val uuid = parcelUuid.uuid
        if (uuid.leastSignificantBits != BASE_UUID.uuid.leastSignificantBits) {
            return false
        }
        return if (is16BitUuid(parcelUuid)) {
            false
        } else uuid.mostSignificantBits and 0xFFFFFFFFL == 0x1000L
    }
}