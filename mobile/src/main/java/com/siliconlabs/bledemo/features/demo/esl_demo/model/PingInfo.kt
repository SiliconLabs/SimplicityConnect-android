package com.siliconlabs.bledemo.features.demo.esl_demo.model

import com.siliconlabs.bledemo.utils.toBitSet

data class PingInfo(
    val eslId: Int,
    val groupId: String,
    val tlvResponseBasicState: Int,
    val serviceNeeded: Boolean,
    val synchronized: Boolean,
    val activeLed: Boolean,
    val pendingLedUpdate: Boolean,
    val pendingDisplayUpdate: Boolean,
    val ledIndex: Int,
) {
    companion object {
        fun parse(characteristicData: ByteArray): PingInfo = characteristicData.let {
            val eslId = it[0].toInt()
            val groupId = it[1].toString()
            val tlvResponseBasicState = it[2].toInt()

            val basicStateResponseData = if (tlvResponseBasicState == CORRECT_TLV_RESP_BASIC_STATE) {
                BasicStateResponseData.parse(it.copyOfRange(3, it.indices.last))
            } else BasicStateResponseData.parse(ByteArray(BasicStateResponseData.FIELDS_COUNT))

            val ledIndex = if (tlvResponseBasicState == CORRECT_TLV_RESP_BASIC_STATE) {
                it.last().toInt()
            } else 0
            
            return PingInfo(
                eslId,
                groupId,
                tlvResponseBasicState,
                basicStateResponseData.serviceNeeded,
                basicStateResponseData.synchronized,
                basicStateResponseData.activeLed,
                basicStateResponseData.pendingLedUpdate,
                basicStateResponseData.pendingDisplayUpdate,
                ledIndex,
            )
        }

        const val CORRECT_TLV_RESP_BASIC_STATE = 0x10
    }

    private data class BasicStateResponseData(
        val serviceNeeded: Boolean,
        val synchronized: Boolean,
        val activeLed: Boolean,
        val pendingLedUpdate: Boolean,
        val pendingDisplayUpdate: Boolean,
    ) {
        companion object {
            fun parse(responseData: ByteArray): BasicStateResponseData {
                val responseDataBitSet = responseData.toBitSet()

                return BasicStateResponseData(
                    responseDataBitSet[0],
                    responseDataBitSet[1],
                    responseDataBitSet[2],
                    responseDataBitSet[3],
                    responseDataBitSet[4],
                )
            }

            const val FIELDS_COUNT = 5
        }
    }
}
