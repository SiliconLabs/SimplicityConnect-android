package com.siliconlabs.bledemo.beaconutils.eddystone;
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

import android.util.Log;
import java.util.Arrays;


/**
 * Basic validation of an Eddystone-URL frame. <p>
 *
 * @see <a href="https://github.com/google/eddystone/eddystone-url">URL frame specification</a>
 */
public class UrlValidator {

    private static final String TAG = UrlValidator.class.getSimpleName();

    public  UrlValidator() {
    }

    public static void validate(String deviceAddress, byte[] serviceData, Beacon beacon) {
        beacon.hasUrlFrame = true;

        // Tx power should have reasonable values.
        int txPower = (int) serviceData[1];
        if (txPower < Constants.MIN_EXPECTED_TX_POWER || txPower > Constants.MAX_EXPECTED_TX_POWER) {
            String err = String.format("Expected URL Tx power between %d and %d, got %d",
                    Constants.MIN_EXPECTED_TX_POWER, Constants.MAX_EXPECTED_TX_POWER, txPower);
            beacon.urlStatus.txPower = err;
            logDeviceError(deviceAddress, err);
        }

        // The URL bytes should not be all zeroes.
        byte[] urlBytes = Arrays.copyOfRange(serviceData, 2, 20);
        if (Utils.isZeroed(urlBytes)) {
            String err = "URL bytes are all 0x00";
            beacon.urlStatus.urlNotSet = err;
            logDeviceError(deviceAddress, err);
        }

        beacon.urlStatus.urlValue = UrlUtils.decodeUrl(serviceData);
    }

    private static void logDeviceError(String deviceAddress, String err) {
        Log.e(TAG, deviceAddress + ": " + err);
    }
}
