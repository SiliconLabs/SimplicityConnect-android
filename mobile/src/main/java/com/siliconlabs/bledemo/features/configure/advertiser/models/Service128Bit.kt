package com.siliconlabs.bledemo.features.configure.advertiser.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Service128Bit(val uuid: UUID) : Parcelable {
    override fun toString(): String {
        return uuid.toString()
    }
}