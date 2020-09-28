package com.siliconlabs.bledemo.Advertiser.Models

import android.os.Parcelable
import com.siliconlabs.bledemo.Advertiser.Utils.Converter
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
class Manufacturer(val identifier: Int, val data: ByteArray) : Parcelable {

    fun getCompanyIdentifierAsString(): String {
        return "0x".plus(String.format("%04X",identifier))
    }

    fun getCompanyDataAsString(): String {
        return "0x".plus(Converter.getByteArrayAsHexString(data).toUpperCase(Locale.getDefault()))
    }

    fun getAsDescriptiveText(): String {
        return "Company Code: ".plus(getCompanyIdentifierAsString()).plus("\n").plus("Data: ").plus(getCompanyDataAsString())
    }

    override fun toString(): String {
        return getAsDescriptiveText()
    }
}