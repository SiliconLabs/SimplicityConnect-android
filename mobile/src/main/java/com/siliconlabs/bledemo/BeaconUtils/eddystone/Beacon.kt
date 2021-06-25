package com.siliconlabs.bledemo.BeaconUtils.eddystone

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
class Beacon(private val deviceAddress: String, var rssi: Int) {
    var timestamp = System.currentTimeMillis()

    var uidServiceData: ByteArray? = null
    var tlmServiceData: ByteArray? = null

    inner class UidStatus {
        var uidValue: String? = null
        var txPower = 0
        var errTx: String? = null
        var errUid: String? = null
        var errRfu: String? = null
        val errors: String
            get() {
                val sb = StringBuilder()
                if (errTx != null) {
                    sb.append(errTx).append("\n")
                }
                if (errUid != null) {
                    sb.append(errUid).append("\n")
                }
                if (errRfu != null) {
                    sb.append(errRfu).append("\n")
                }
                return sb.toString().trim { it <= ' ' }
            }
    }

    inner class TlmStatus {
        var version: String? = null
        var voltage: String? = null
        var temp: String? = null
        var advCnt: String? = null
        var secCnt: String? = null
        var deciSecondsCntVal = 0.0
        var errIdentialFrame: String? = null
        var errVersion: String? = null
        var errVoltage: String? = null
        var errTemp: String? = null
        var errPduCnt: String? = null
        var errSecCnt: String? = null
        var errRfu: String? = null
        val errors: String
            get() {
                val sb = StringBuilder()
                if (errIdentialFrame != null) {
                    sb.append(errIdentialFrame).append("\n")
                }
                if (errVersion != null) {
                    sb.append(errVersion).append("\n")
                }
                if (errVoltage != null) {
                    sb.append(errVoltage).append("\n")
                }
                if (errTemp != null) {
                    sb.append(errTemp).append("\n")
                }
                if (errPduCnt != null) {
                    sb.append(errPduCnt).append("\n")
                }
                if (errSecCnt != null) {
                    sb.append(errSecCnt).append("\n")
                }
                if (errRfu != null) {
                    sb.append(errRfu).append("\n")
                }
                return sb.toString().trim { it <= ' ' }
            }

        override fun toString(): String {
            return errors
        }
    }

    inner class UrlStatus {
        var urlValue: String? = null
        var urlNotSet: String? = null
        var txPower: String? = null
        val errors: String
            get() {
                val sb = StringBuilder()
                if (txPower != null) {
                    sb.append(txPower).append("\n")
                }
                if (urlNotSet != null) {
                    sb.append(urlNotSet).append("\n")
                }
                return sb.toString().trim { it <= ' ' }
            }

        override fun toString(): String {
            val sb = StringBuilder()
            if (urlValue != null) {
                sb.append(urlValue).append("\n")
            }
            return sb.append(errors).toString().trim { it <= ' ' }
        }
    }

    inner class FrameStatus {
        var nullServiceData: String? = null
        var invalidFrameType: String? = null
        val errors: String
            get() {
                val sb = StringBuilder()
                if (nullServiceData != null) {
                    sb.append(nullServiceData).append("\n")
                }
                if (invalidFrameType != null) {
                    sb.append(invalidFrameType).append("\n")
                }
                return sb.toString().trim { it <= ' ' }
            }

        override fun toString(): String {
            return errors
        }
    }

    var hasUidFrame = false
    var uidStatus = UidStatus()
    var hasTlmFrame = false
    var tlmStatus = TlmStatus()
    var hasUrlFrame = false
    var urlStatus = UrlStatus()
    var frameStatus = FrameStatus()

    /**
     * Performs a case-insensitive contains test of s on the device address (with or without the
     * colon separators) and/or the UID value, and/or the URL value.
     */
    operator fun contains(s: String?): Boolean {
        return (s == null || s.isEmpty()
                || deviceAddress.replace(":", "").toLowerCase().contains(s.toLowerCase())
                || (uidStatus.uidValue != null
                && uidStatus.uidValue?.toLowerCase()?.contains(s.toLowerCase())!!)
                || (urlStatus.urlValue != null
                && urlStatus.urlValue?.toLowerCase()?.contains(s.toLowerCase())!!))
    }

}