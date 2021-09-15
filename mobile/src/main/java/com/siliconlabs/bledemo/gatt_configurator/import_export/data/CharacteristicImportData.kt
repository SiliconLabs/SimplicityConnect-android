package com.siliconlabs.bledemo.gatt_configurator.import_export.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CharacteristicImportData(
        val attributes: MutableMap<String, String> = AttributeMap(),
        val capabilities: MutableSet<String> = mutableSetOf(),
        val simpleElements: MutableMap<String, String> = ElementMap(),
        var propertiesAttributes: MutableMap<String, String> = AttributeMap(),
        var aggregate: MutableList<String?> = mutableListOf() /* First entry is <aggregate>
        attribute, the rest are <attribute> entries attributes */
) : Parcelable