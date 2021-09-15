package com.siliconlabs.bledemo.gatt_configurator.models

import android.bluetooth.BluetoothGattDescriptor
import android.os.Parcelable
import com.google.gson.Gson
import com.siliconlabs.bledemo.gatt_configurator.import_export.data.DescriptorImportData
import kotlinx.android.parcel.Parcelize
import java.util.*
import kotlin.collections.HashMap

@Parcelize
data class Descriptor(
        var name: String = "",
        var uuid: Uuid? = null,
        var properties: HashMap<Property, HashSet<Property.Type>> = hashMapOf(Pair(Property.READ, hashSetOf())),
        var value: Value? = null,
        var isPredefined: Boolean = false,
        var importedData: DescriptorImportData = DescriptorImportData()
) : Parcelable {

    constructor(
        name: String = "",
        uuid: Uuid? = null,
        properties: Set<Property>,
        value: Value? = null,
        isPredefined: Boolean = false
    ) : this(
        name,
        uuid,
        HashMap<Property, HashSet<Property.Type>>().apply {
            properties.forEach { put(it, hashSetOf()) }
        },
        value,
        isPredefined
    )

    fun getBluetoothGattPermissions(): Int {
        var result = 0
        for ((key, value) in properties) {
            when (key) {
                Property.READ -> {
                    result = result or BluetoothGattDescriptor.PERMISSION_READ
                    if (value.contains(Property.Type.BONDED)) result =
                        result or BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED
                    if (value.contains(Property.Type.AUTHENTICATED)) result =
                        result or BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM
                }
                Property.WRITE -> {
                    result = result or BluetoothGattDescriptor.PERMISSION_WRITE
                    if (value.contains(Property.Type.BONDED)) result =
                        result or BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED
                    if(value.contains(Property.Type.AUTHENTICATED)) result = result or BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM
                }
                else -> {}
            }
        }
        return result
    }

    fun deepCopy(): Descriptor {
        val dataCopy = Gson().toJson(this)
        return Gson().fromJson(dataCopy, Descriptor::class.java)
    }
}
