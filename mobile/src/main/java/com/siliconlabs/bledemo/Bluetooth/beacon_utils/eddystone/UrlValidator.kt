package com.siliconlabs.bledemo.bluetooth.beacon_utils.eddystone

import android.util.Log
import com.siliconlabs.bledemo.bluetooth.beacon_utils.eddystone.UrlUtils.decodeUrl
import com.siliconlabs.bledemo.utils.Converters
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
 * Basic validation of an Eddystone-URL frame.
 *
 *
 *
 * @see [URL frame specification](https://github.com/google/eddystone/eddystone-url)
 */
object UrlValidator {
    private val TAG = UrlValidator::class.java.simpleName

    fun validate(deviceAddress: String, serviceData: ByteArray, beacon: Beacon) {
        beacon.hasUrlFrame = true

        // Tx power should have reasonable values.
        val txPower = serviceData[1].toInt()
        if (txPower < Constants.MIN_EXPECTED_TX_POWER || txPower > Constants.MAX_EXPECTED_TX_POWER) {
            val err = String.format("Expected URL Tx power between %d and %d, got %d",
                    Constants.MIN_EXPECTED_TX_POWER, Constants.MAX_EXPECTED_TX_POWER, txPower)
            beacon.urlStatus.txPower = err
            logDeviceError(deviceAddress, err)
        }

        // The URL bytes should not be all zeroes.
        val urlBytes = Arrays.copyOfRange(serviceData, 2, 20)
        if (Converters.isZeroed(urlBytes)) {
            val err = "URL bytes are all 0x00"
            beacon.urlStatus.urlNotSet = err
            logDeviceError(deviceAddress, err)
        }
        beacon.urlStatus.urlValue = decodeUrl(serviceData)
    }

    private fun logDeviceError(deviceAddress: String, err: String) {
        Log.e(TAG, "$deviceAddress: $err")
    }
}
