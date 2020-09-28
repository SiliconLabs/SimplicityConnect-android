package com.siliconlabs.bledemo.Advertiser.Models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
class Service16Bit(val identifier: Int, val name: String) : Parcelable {

    fun getUUID(): UUID {
        val hexString = String.format("%04X",identifier)
        return UUID.fromString("0000".plus(hexString).plus("-0000-1000-8000-00805F9B34FB"))
    }

    fun getFullName(): String {
        val hexString: String = "(0x".plus(String.format("%04X",identifier).plus(")").toUpperCase(Locale.getDefault()))
        return name.plus(" ").plus(hexString)
    }

    override fun toString(): String {
        return "0x".plus(String.format("%04X",identifier)).plus(" - ").plus(name)
    }

}