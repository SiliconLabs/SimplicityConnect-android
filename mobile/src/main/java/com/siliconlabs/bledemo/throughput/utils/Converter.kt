package com.siliconlabs.bledemo.throughput.utils

import com.siliconlabs.bledemo.throughput.models.PhyStatus

class Converter {
    companion object {
        private const val CONNECTION_INTERVAL_STEP = 1.25
        private const val SLAVE_LATENCY_STEP = 1.25
        private const val SUPERVISION_TIMEOUT_STEP = 10

        fun getPhyStatus(value: Byte): PhyStatus {
            return when (value.toInt()) {
                0x01 -> PhyStatus.PHY_1M
                0x02 -> PhyStatus.PHY_2M
                0x04 -> PhyStatus.PHY_CODED_125K
                else -> PhyStatus.PHY_CODED_500K
            }
        }

        fun getInterval(value: ByteArray): Double {
            return byteArrayToInt(value) * CONNECTION_INTERVAL_STEP
        }

        fun getLatency(value: ByteArray): Double {
            return byteArrayToInt(value) * SLAVE_LATENCY_STEP
        }

        fun getSupervisionTimeout(value: ByteArray): Int {
            return byteArrayToInt(value) * SUPERVISION_TIMEOUT_STEP
        }

        fun getPduValue(value: Byte): Int {
            return value.toInt() and 0xFF
        }

        fun getMtuValue(value: Byte): Int {
            return value.toInt() and 0xFF
        }

        fun byteArrayToInt(bytes: ByteArray): Int {
            var result = 0
            for (i in bytes.indices) {
                result = result or ((bytes[i].toInt() and 0xFF) shl 8 * i)
            }
            return result
        }
    }
}