package com.siliconlabs.bledemo.gatt_configurator.models

import android.bluetooth.BluetoothGattDescriptor
import android.os.Parcelable
import com.google.gson.Gson
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Descriptor(
        var name: String = "",
        var uuid: Uuid? = null,
        val properties: HashMap<Property, HashSet<Property.Type>> = hashMapOf(Pair(Property.READ, hashSetOf())),
        var value: Value? = null,
        var isPredefined: Boolean = false
) : Parcelable {

    fun getBluetoothGattPermissions(): Int {
        var result = 0
        for ((key, value) in properties) {
            when(key) {
                Property.READ -> {
                    result = result or BluetoothGattDescriptor.PERMISSION_READ
                    if(value.contains(Property.Type.BONDED)) result = result or BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED
                    if(value.contains(Property.Type.AUTHENTICATED)) result = result or BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM
                }
                Property.WRITE -> {
                    result = result or BluetoothGattDescriptor.PERMISSION_WRITE
                    if(value.contains(Property.Type.BONDED)) result = result or BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED
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