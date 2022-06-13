package com.siliconlabs.bledemo.motion.viewmodels

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.siliconlabs.bledemo.Bluetooth.BLE.GattCharacteristic
import com.siliconlabs.bledemo.Bluetooth.BLE.GattService
import com.siliconlabs.bledemo.utils.BLEUtils

class MotionViewModel : ViewModel() {

    val acceleration = MutableLiveData<FloatArray>()
    val orientation = MutableLiveData<FloatArray>()

    fun calibrate(gatt: BluetoothGatt?) {
        if (!startCalibration(gatt)) {
            Handler(Looper.getMainLooper()).postDelayed({ calibrate(gatt) }, 500)
        }
    }

    private fun startCalibration(bluetoothGatt: BluetoothGatt?): Boolean {
        return bluetoothGatt?.let { gatt ->
            BLEUtils.getCharacteristic(gatt, GattService.Motion, GattCharacteristic.Calibration)?.let {
                it.value = byteArrayOf(0x01)
                gatt.writeCharacteristic(it)
            } ?: false
        } ?: false
    }

    fun resetOrientation(bluetoothGatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic): Boolean {
        return bluetoothGatt?.let {
            characteristic.value = byteArrayOf(0x02)
            it.writeCharacteristic(characteristic)
        } ?: false
    }
}