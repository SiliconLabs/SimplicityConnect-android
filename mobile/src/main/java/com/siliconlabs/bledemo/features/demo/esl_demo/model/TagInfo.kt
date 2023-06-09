package com.siliconlabs.bledemo.features.demo.esl_demo.model

import com.siliconlabs.bledemo.utils.Converters

data class TagInfo(
    val bleAddress: String,
    val eslId: Int,
    val groupId: String,
    val displayScreensNumber: Int,
    val maxImageIndex: Int,
    val sensorInfo: List<Int>,
    var isLedOn: Boolean,
    val pnpInfo: String,
    val lastStatus: Int,
) {

    /* Final format of TagInfo is not yet known */
    constructor(
        eslId: Int,
        maxImageIndex: Int,
        displayScreensNumber: Int,
        bleAddress: String,
        sensorInfo: List<Int>,
    ) : this(
        bleAddress,
        eslId,
        "placeholder",
        displayScreensNumber,
        maxImageIndex,
        sensorInfo,
        false,
        "placeholder",
        -1,
    )

    companion object {
        fun parse(rawCharacteristicValue: ByteArray) : TagInfo {
            val eslId = rawCharacteristicValue[0].toUByte().toInt()
            val maxImageIndex = (rawCharacteristicValue[1].toInt() - 1).coerceAtLeast(0)
            val displayStringsNumber = rawCharacteristicValue[2].toInt()
            val btAddress = rawCharacteristicValue.copyOfRange(3, 20) // address comes in string format :(
            val sensorsArray = rawCharacteristicValue.copyOfRange(20, rawCharacteristicValue.size) // sensors data too :(

            val sensorsList = prepareSensorsList(sensorsArray)

            return TagInfo(
                eslId,
                maxImageIndex,
                displayStringsNumber,
                String(btAddress),
                sensorsList,
            )
        }

        private fun prepareSensorsList(sensorsArray: ByteArray): List<Int> = buildList {
            for (idx in sensorsArray.indices step 2) {
                val sensorData = Converters.calculateDecimalValue(
                    array = sensorsArray.copyOfRange(idx, idx + 1).reversedArray(),
                    isBigEndian = false,
                )

                add(sensorData)
            }
        }
    }
}