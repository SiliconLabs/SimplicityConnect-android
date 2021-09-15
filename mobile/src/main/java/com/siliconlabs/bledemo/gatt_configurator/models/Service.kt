package com.siliconlabs.bledemo.gatt_configurator.models

import android.bluetooth.BluetoothGattService
import android.os.Parcelable
import com.google.gson.Gson
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.gatt_configurator.import_export.data.ServiceImportData
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Service(var name: String = "",
                   var uuid: Uuid? = null,
                   var type: Type = Type.PRIMARY,
                   val characteristics: ArrayList<Characteristic> = arrayListOf(),
                   var importedData: ServiceImportData = ServiceImportData()) : Parcelable
{

    fun getUuidWithName(): String {
        return uuid?.getAsFormattedText()?.plus(" - ").plus(name)
    }

    enum class Type(val textResId: Int) {
        PRIMARY(R.string.gatt_configurator_primary_service),
        SECONDARY(R.string.gatt_configurator_secondary_service);

        fun getBluetoothGattServiceType(): Int {
            return when (this) {
                PRIMARY -> BluetoothGattService.SERVICE_TYPE_PRIMARY
                else -> BluetoothGattService.SERVICE_TYPE_SECONDARY
            }
        }
    }

    fun deepCopy(): Service {
        val dataCopy = Gson().toJson(this)
        return Gson().fromJson(dataCopy, Service::class.java)
    }
}
