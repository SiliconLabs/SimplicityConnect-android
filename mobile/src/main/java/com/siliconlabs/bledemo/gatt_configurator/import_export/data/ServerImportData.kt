package com.siliconlabs.bledemo.gatt_configurator.import_export.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ServerImportData(
        var device: String? = null,
        var gattAttributes: Map<String, String> = AttributeMap(),
        var capabilities: Map<String, String> = mapOf() // <capability value, "enable" attribute value>
) : Parcelable