package com.siliconlabs.bledemo.BeaconUtils.eddystone

import android.util.Log
import com.siliconlabs.bledemo.Utils.Converters
import java.util.*

// Copyright 2015 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// see https://github.com/google/eddystone
/**
 * Basic validation of an Eddystone-UID frame.
 *
 *
 *
 * @see [UID frame specification](https://github.com/google/eddystone/eddystone-uid)
 */
object UidValidator {
    private val TAG = UidValidator::class.java.simpleName
    fun validate(deviceAddress: String, serviceData: ByteArray, beacon: Beacon) {
        beacon.hasUidFrame = true

        // Tx power should have reasonable values.
        val txPower = serviceData[1].toInt()
        beacon.uidStatus.txPower = txPower
        if (txPower < Constants.MIN_EXPECTED_TX_POWER || txPower > Constants.MAX_EXPECTED_TX_POWER) {
            val err = String.format("Expected UID Tx power between %d and %d, got %d", Constants.MIN_EXPECTED_TX_POWER,
                    Constants.MAX_EXPECTED_TX_POWER, txPower)
            beacon.uidStatus.errTx = err
            logDeviceError(deviceAddress, err)
        }

        // The namespace and instance bytes should not be all zeroes.
        val uidBytes = Arrays.copyOfRange(serviceData, 2, 18)
        beacon.uidStatus.uidValue = Converters.bytesToHex(uidBytes)
        if (Converters.isZeroed(uidBytes)) {
            val err = "UID bytes are all 0x00"
            beacon.uidStatus.errUid = err
            logDeviceError(deviceAddress, err)
        }

        // If we have a previous frame, verify the ID isn't changing.
        if (beacon.uidServiceData == null) {
            beacon.uidServiceData = serviceData.clone()
        } else {
            val previousUidBytes = Arrays.copyOfRange(beacon.uidServiceData, 2, 18)
            if (!Arrays.equals(uidBytes, previousUidBytes)) {
                val err = String.format("UID should be invariant.\nLast: %s\nthis: %s",
                        Converters.bytesToHex(previousUidBytes),
                        Converters.bytesToHex(uidBytes))
                beacon.uidStatus.errUid = err
                logDeviceError(deviceAddress, err)
                beacon.uidServiceData = serviceData.clone()
            }
        }

        // Last two bytes in frame are RFU and should be zeroed.
        val rfu = Arrays.copyOfRange(serviceData, 18, 20)
        if (rfu[0].toInt() != 0x00 || rfu[1].toInt() != 0x00) {
            val err = "Expected UID RFU bytes to be 0x00, were " + Converters.bytesToHex(rfu)
            beacon.uidStatus.errRfu = err
            logDeviceError(deviceAddress, err)
        }
    }

    private fun logDeviceError(deviceAddress: String, err: String) {
        Log.e(TAG, "$deviceAddress: $err")
    }
}