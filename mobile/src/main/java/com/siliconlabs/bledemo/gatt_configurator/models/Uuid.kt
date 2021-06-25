package com.siliconlabs.bledemo.gatt_configurator.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Uuid(val uuid: String) : Parcelable {

    fun getAsFormattedText(): String {
        return if (uuid.matches(UUID_16BIT_PATTERN.toRegex())) {
            "0x".plus(uuid.toUpperCase(Locale.ROOT))
        } else {
            uuid.toLowerCase(Locale.ROOT)
        }
    }

    fun getAs128BitUuid():UUID {
        return if (uuid.matches(UUID_16BIT_PATTERN.toRegex())) {
           UUID.fromString("0000".plus(uuid).plus("-0000-1000-8000-00805F9B34FB"))
        } else {
            UUID.fromString(uuid)
        }
    }

    companion object {
        private const val UUID_16BIT_PATTERN = "[0-9a-fA-F]{4}"
    }
}