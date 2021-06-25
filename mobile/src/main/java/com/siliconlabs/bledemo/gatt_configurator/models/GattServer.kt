package com.siliconlabs.bledemo.gatt_configurator.models

import android.os.Parcelable
import com.google.gson.Gson
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

@Parcelize
data class GattServer(
        var name: String,
        val services: ArrayList<Service> = arrayListOf(),
        var isSwitchedOn: Boolean = false
) : Parcelable {

    @IgnoredOnParcel
    var isViewExpanded: Boolean = false

    fun deepCopy(): GattServer {
        val dataCopy = Gson().toJson(this)
        val gattServer = Gson().fromJson(dataCopy, GattServer::class.java)
        return GattServer(gattServer.name, gattServer.services)
    }
}