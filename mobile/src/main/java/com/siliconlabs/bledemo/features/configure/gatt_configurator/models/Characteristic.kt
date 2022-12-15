package com.siliconlabs.bledemo.features.configure.gatt_configurator.models

import android.bluetooth.BluetoothGattCharacteristic
import android.os.Parcelable
import com.google.gson.Gson
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.data.CharacteristicImportData
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Characteristic(
        var name: String = "",
        var uuid: Uuid? = null,
        val descriptors: ArrayList<Descriptor> = arrayListOf(),
        var properties: HashMap<Property, HashSet<Property.Type>> = hashMapOf(Pair(Property.READ, hashSetOf())),
        var value: Value? = null,
        var importedData: CharacteristicImportData = CharacteristicImportData()
) : Parcelable {

    fun getBluetoothGattProperties(): Int {
        var result = 0
        for ((key, _) in properties) {
            result = when (key) {
                Property.READ -> result or BluetoothGattCharacteristic.PROPERTY_READ
                Property.WRITE -> result or BluetoothGattCharacteristic.PROPERTY_WRITE
                Property.WRITE_WITHOUT_RESPONSE -> result or BluetoothGattCharacteristic.PROPERTY_WRITE
                Property.RELIABLE_WRITE -> result or BluetoothGattCharacteristic.PROPERTY_WRITE
                Property.INDICATE -> result or BluetoothGattCharacteristic.PROPERTY_INDICATE
                Property.NOTIFY -> result or BluetoothGattCharacteristic.PROPERTY_NOTIFY
                else -> { result }
            }
        }
        return result
    }

    fun getBluetoothGattPermissions(): Int {
        var result = 0
        for ((key, value) in properties) {
            if (key == Property.READ) {
                result = result or BluetoothGattCharacteristic.PERMISSION_READ
                if (value.contains(Property.Type.BONDED)) result = result or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
                if (value.contains(Property.Type.AUTHENTICATED)) result = result or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM
            } else if (key.isWriteProperty()) {
                result = result or BluetoothGattCharacteristic.PERMISSION_WRITE
                if (value.contains(Property.Type.BONDED)) result = result or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
                if (value.contains(Property.Type.AUTHENTICATED)) result = result or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM
            }
        }
        return result
    }

    fun deepCopy(): Characteristic {
        val dataCopy = Gson().toJson(this)
        return Gson().fromJson(dataCopy, Characteristic::class.java)
    }
}