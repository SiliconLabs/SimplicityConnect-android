package com.siliconlabs.bledemo.home_screen.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.siliconlabs.bledemo.base.viewmodels.ScannerViewModel
import com.siliconlabs.bledemo.bluetooth.ble.BluetoothDeviceInfo
import com.siliconlabs.bledemo.bluetooth.ble.ScanResultCompat


class SelectDeviceViewModel : ScannerViewModel() {

    private val _scannedDevices: MutableLiveData<MutableMap<String, BluetoothDeviceInfo>> = MutableLiveData(mutableMapOf())
    val scannedDevices: LiveData<MutableMap<String, BluetoothDeviceInfo>> = _scannedDevices
    private val _deviceToInsert: MutableLiveData<BluetoothDeviceInfo> = MutableLiveData()
    val deviceToInsert: LiveData<BluetoothDeviceInfo> = _deviceToInsert
    private val _numberOfDevices: MutableLiveData<Int> = MutableLiveData(0)
    val numberOfDevices: LiveData<Int> = _numberOfDevices

    override fun handleScanResult(result: ScanResultCompat) {
        synchronized(_scannedDevices) {
            _scannedDevices.value?.apply {
                val address = result.device?.address!!
                val isNewDevice = !keys.contains(address)

                getOrPut(address, { BluetoothDeviceInfo(result.device!!)}).also {
                    updateScanInfo(it, result)
                }

                if (isNewDevice) {
                    _deviceToInsert.value = this[address]
                    _numberOfDevices.value = size
                }
            }
            _isAnyDeviceDiscovered.value = _scannedDevices.value?.isNotEmpty() ?: false
        }
    }

    fun clearDevices() {
        _scannedDevices.postValue(mutableMapOf())
        _numberOfDevices.postValue(0)
    }

    fun getScannedDevicesList() : List<BluetoothDeviceInfo> {
        return _scannedDevices.value?.values?.toList() ?: listOf()
    }

}