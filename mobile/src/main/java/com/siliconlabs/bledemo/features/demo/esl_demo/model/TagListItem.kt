package com.siliconlabs.bledemo.features.demo.esl_demo.model

data class TagListItem(
    val isLast: Boolean,
    val tagInfo: TagInfo?,
) {
    companion object {
        fun parse(characteristicData: ByteArray): TagListItem {
            val isLast = characteristicData[0].toInt() == LAST_ITEM
            val tagInfo = characteristicData.takeUnless { it.size == EMPTY_LIST_RESPONSE_LEN
                    && it[1].toInt() == EMPTY_TAG_INFO }
                ?.let { TagInfo.parse(it.copyOfRange(1, it.size)) }

            return TagListItem(
                isLast,
                tagInfo,
            )
        }

        private const val EMPTY_LIST_RESPONSE_LEN = 2
        private const val EMPTY_TAG_INFO = 0x00
        private const val LAST_ITEM = 0x01
    }
}