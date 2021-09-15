package com.siliconlabs.bledemo.gatt_configurator.import_export.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class DescriptorImportData(
    val attributes: MutableMap<String, String> = AttributeMap(),
    val simpleElements: MutableMap<String, String> = ElementMap(),
    var propertiesAttributes: MutableMap<String, String> = AttributeMap()
) : Parcelable