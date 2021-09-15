package com.siliconlabs.bledemo.gatt_configurator.models

import android.os.Parcelable
import com.google.gson.Gson
import com.siliconlabs.bledemo.gatt_configurator.import_export.data.ServerImportData
import kotlinx.android.parcel.Parcelize

@Parcelize
data class GattServer(
        var name: String,
        val services: ArrayList<Service> = arrayListOf(),
        var isSwitchedOn: Boolean = false,
        var importedData: ServerImportData = ServerImportData()
) : Parcelable {


    @Transient
    var isViewExpanded: Boolean = false

    @Transient
    var isCheckedForExport = false

    fun deepCopy(): GattServer {
        val dataCopy = Gson().toJson(this)
        val gattServer = Gson().fromJson(dataCopy, GattServer::class.java)
        return GattServer(gattServer.name, gattServer.services, importedData = gattServer.importedData)
    }
}