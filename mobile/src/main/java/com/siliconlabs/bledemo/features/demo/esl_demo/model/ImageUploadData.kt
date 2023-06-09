package com.siliconlabs.bledemo.features.demo.esl_demo.model

import android.net.Uri
import com.siliconlabs.bledemo.utils.Constants

data class ImageUploadData(
        val uri: Uri,
        val filename: String,
        val data: ByteArray,
        val slotIndex: Int,
        val tagIndex: Int,
        val displayAfterUpload: Boolean,
        var packetSize: Int = with(Constants) { MIN_ALLOWED_MTU - ATT_HEADER_SIZE },
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageUploadData

        if (uri != other.uri) return false
        if (filename != other.filename) return false
        if (!data.contentEquals(other.data)) return false
        if (slotIndex != other.slotIndex) return false
        if (tagIndex != other.tagIndex) return false
        if (displayAfterUpload != other.displayAfterUpload) return false
        if (packetSize != other.packetSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + filename.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + slotIndex
        result = 31 * result + tagIndex
        result = 31 * result + displayAfterUpload.hashCode()
        result = 31 * result + packetSize
        return result
    }
}
