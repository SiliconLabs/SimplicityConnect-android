package com.siliconlabs.bledemo.Advertiser.Utils

import java.lang.IllegalArgumentException
import java.util.*

class Converter {
    companion object {
        private const val HEX_CHARS = "0123456789ABCDEF"

        fun isHexCorrect(text: String): Boolean {
            for (letter in text.toUpperCase(Locale.getDefault())) if (!HEX_CHARS.contains(letter)) return false
            return true
        }

        fun getHexStringAsByteArray(text: String): ByteArray {
            if (!isHexCorrect(text))
                throw IllegalArgumentException("Incorrect hexadecimal characters")

            val length = text.length
            val array = ByteArray(length / 2)

            for (i in 0 until length step 2) {
                val hexByte: String = StringBuilder().append(text[i]).append(text[i+1]).toString()
                val result = (hexByte).toInt(16)
                if (result > 127) array[i / 2] = (result - 256).toByte()
                else array[i/2] = result.toByte()
            }
            return array
        }

        fun getByteArrayAsHexString(bytes: ByteArray): String {
            val builder = StringBuilder()
            for(byte in bytes) { builder.append(String.format("%02x",byte))}
            return builder.toString()
        }
    }
}