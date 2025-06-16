package com.siliconlabs.bledemo.features.configure.gatt_configurator.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Value(
        var value: String = "",
        var type: Type? = null,
        var length: Int = 0,
        var variableLength: Boolean = false
) : Parcelable {

    fun getAsFormattedText(): String {
        return when (type) {
            Type.HEX -> "0x".plus(value.uppercase(Locale.ROOT))
            else -> value
        }
    }

    fun getValueAsArrayOfBytes(): ByteArray {
        return if (type == Type.UTF_8) {
            value.toByteArray()
        } else {
            value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        }
    }

    enum class Type {
        UTF_8,
        HEX,
        USER
    }
}