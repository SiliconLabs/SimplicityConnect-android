package com.siliconlabs.bledemo.features.scan.browser.utils

import android.bluetooth.BluetoothGattCharacteristic
import com.siliconlabs.bledemo.bluetooth.data_types.Characteristic
import com.siliconlabs.bledemo.utils.UuidUtils

object GlucoseManagement {
    /* See BTAPP-1438, BTAPP-1439, BTAPP-1440 */

    fun isRecordAccessControlPoint(characteristic: Characteristic?) : Boolean {
        return characteristic?.let {
            UuidUtils.convert128to16UUID(it.uuid.toString()) == RECORD_ACCESS_CONTROL_POINT_UUID
        } ?: false
    }

    fun isCgmSpecificOpsControlPoint(characteristic: Characteristic?) : Boolean {
        return characteristic?.let {
            UuidUtils.convert128to16UUID(it.uuid.toString()) == CGM_SPECIFIC_OPS_CONTROL_POINT
        } ?: false
    }

    fun isNumberOfRecordsResponse(bluetoothChar: BluetoothGattCharacteristic?, response: ByteArray) : Boolean {
        return bluetoothChar?.let {
            UuidUtils.convert128to16UUID(it.uuid.toString()) == RECORD_ACCESS_CONTROL_POINT_UUID
                    && response[0] == 0x05.toByte()
        } ?: false
    }

    fun updateValueToWrite(value: ByteArray) : ByteArray {
        return if (value[2] == 0x00.toByte()) value.copyOfRange(0, 2)
        else value.copyOfRange(0, 3)
    }

    private const val RECORD_ACCESS_CONTROL_POINT_UUID = "2a52"
    private const val CGM_SPECIFIC_OPS_CONTROL_POINT = "2aac"
}