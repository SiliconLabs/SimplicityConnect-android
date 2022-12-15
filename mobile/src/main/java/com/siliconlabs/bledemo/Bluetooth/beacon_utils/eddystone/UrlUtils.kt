package com.siliconlabs.bledemo.bluetooth.beacon_utils.eddystone

import android.util.Log
import android.util.SparseArray
import android.webkit.URLUtil
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
 * Helpers for Eddystone-URL frame validation. Copied from
 * https://github.com/google/uribeacon/android-uribeacon/uribeacon-library
 */
object UrlUtils {
    private val TAG = UrlUtils::class.java.simpleName
    private val URI_SCHEMES: SparseArray<String?> = object : SparseArray<String?>() {
        init {
            put(0, "http://www.")
            put(1, "https://www.")
            put(2, "http://")
            put(3, "https://")
            put(4, "urn:uuid:")
        }
    }
    private val URL_CODES: SparseArray<String?> = object : SparseArray<String?>() {
        init {
            put(0, ".com/")
            put(1, ".org/")
            put(2, ".edu/")
            put(3, ".net/")
            put(4, ".info/")
            put(5, ".biz/")
            put(6, ".gov/")
            put(7, ".com")
            put(8, ".org")
            put(9, ".edu")
            put(10, ".net")
            put(11, ".info")
            put(12, ".biz")
            put(13, ".gov")
        }
    }

    fun decodeUrl(serviceData: ByteArray): String? {
        val url = StringBuilder()
        var offset = 2
        val b = serviceData[offset++]
        val scheme = URI_SCHEMES[b.toInt()]
        if (scheme != null) {
            url.append(scheme)
            if (URLUtil.isNetworkUrl(scheme)) {
                return decodeUrl(serviceData, offset, url)
            } else if ("urn:uuid:" == scheme) {
                return decodeUrnUuid(serviceData, offset, url)
            }
        }
        return url.toString()
    }

    private fun decodeUrl(serviceData: ByteArray, offset: Int, urlBuilder: StringBuilder): String {
        var offset1 = offset
        while (offset1 < serviceData.size) {
            val b = serviceData[offset1++]
            val code = URL_CODES[b.toInt()]
            if (code != null) {
                urlBuilder.append(code)
            } else {
                urlBuilder.append(b.toChar())
            }
        }
        return urlBuilder.toString()
    }

    fun decodeUrnUuid(serviceData: ByteArray?, offset: Int, urnBuilder: StringBuilder): String? {
        val bb = ByteBuffer.wrap(serviceData)
        // UUIDs are ordered as byte array, which means most significant first
        bb.order(ByteOrder.BIG_ENDIAN)
        val mostSignificantBytes: Long
        val leastSignificantBytes: Long
        try {
            bb.position(offset)
            mostSignificantBytes = bb.long
            leastSignificantBytes = bb.long
        } catch (e: BufferUnderflowException) {
            Log.w(TAG, "decodeUrnUuid BufferUnderflowException!")
            return null
        }
        val uuid = UUID(mostSignificantBytes, leastSignificantBytes)
        urnBuilder.append(uuid.toString())
        return urnBuilder.toString()
    }
}