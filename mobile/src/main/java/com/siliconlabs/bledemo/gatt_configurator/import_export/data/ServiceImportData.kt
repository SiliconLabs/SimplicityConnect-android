package com.siliconlabs.bledemo.gatt_configurator.import_export.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ServiceImportData(
        val attributes: MutableMap<String, String> = AttributeMap(),
        val capabilities: MutableSet<String> = mutableSetOf(),
        val simpleElements: MutableMap<String, String> = ElementMap(),
        val include: MutableMap<String, String> = mutableMapOf() // <id value, sourceId value>
) : Parcelable