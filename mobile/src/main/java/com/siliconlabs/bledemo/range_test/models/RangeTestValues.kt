package com.siliconlabs.bledemo.range_test.models

import java.util.*

/**
 * @author Comarch S.A.
 */
class RangeTestValues {
    companion object {
        const val PACKET_COUNT_REPEAT = 65535

        var CHANNEL_LOOKUP: List<Int>
        var PACKET_COUNT_LOOKUP: List<Int>
        var ID_LOOKUP: List<Int>

        private const val CHANNELS_MIN = 0
        private const val CHANNELS_MAX = 19

        private val PACKET_COUNTS = arrayOf(500, 1000, 2500, 5000, 10000, 25000, 50000)

        private const val ID_MIN = 0
        private const val ID_MAX = 32

        init {
            val channelsLookupSize = CHANNELS_MAX - CHANNELS_MIN + 1
            val channelsLookup: ArrayList<Int> = ArrayList(channelsLookupSize)
            for (i in 0 until channelsLookupSize) {
                channelsLookup.add(CHANNELS_MIN + i)
            }
            CHANNEL_LOOKUP = Collections.unmodifiableList(channelsLookup)

            PACKET_COUNT_LOOKUP = Collections.unmodifiableList(listOf(*PACKET_COUNTS))

            val idLookupSize = ID_MAX - ID_MIN + 1
            val idLookup: ArrayList<Int> = ArrayList(idLookupSize)
            for (i in 0 until idLookupSize) {
                idLookup.add(ID_MIN + i)
            }

            ID_LOOKUP = Collections.unmodifiableList(idLookup)
        }
    }
}