package com.siliconlabs.bledemo.Advertiser.Models

import android.os.Parcelable
import com.siliconlabs.bledemo.Utils.Converters
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
class Manufacturer(val identifier: Int, val data: ByteArray) : Parcelable {

    fun getCompanyIdentifierAsString(): String {
        return "0x".plus(String.format("%04X", identifier))
    }

    fun getCompanyDataAsString(): String {
        return "0x".plus(Converters.bytesToHex(data).toUpperCase(Locale.getDefault()))
    }

    fun getAsDescriptiveText(): String {
        return "Company Code: ".plus(getCompanyIdentifierAsString()).plus("\n").plus("Data: ").plus(getCompanyDataAsString())
    }

    override fun toString(): String {
        return getAsDescriptiveText()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Manufacturer) return false
        return other.hashCode() == this.hashCode()
    }

    override fun hashCode(): Int {
        var result = identifier
        result = 31 * result + data.contentHashCode()
        return result
    }

}