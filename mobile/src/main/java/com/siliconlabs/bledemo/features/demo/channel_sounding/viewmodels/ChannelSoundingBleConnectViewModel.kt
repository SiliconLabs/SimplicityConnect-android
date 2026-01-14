package com.siliconlabs.bledemo.features.demo.channel_sounding.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.os.Build
import android.text.TextUtils
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.features.demo.channel_sounding.interfaces.BleConnection
import com.siliconlabs.bledemo.features.demo.channel_sounding.utils.ChannelSoundingConstant
import timber.log.Timber

@SuppressLint("MissingPermission")
class ChannelSoundingBleConnectViewModel(
    application: Application
) : AndroidViewModel(application), BleConnection {

    class Factory(val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChannelSoundingBleConnectViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ChannelSoundingBleConnectViewModel(app) as T
            }
            throw IllegalArgumentException("Unable to ChannelSoundingBleConnectViewModel construct viewmodel")
        }
    }

    private var targetBtAddress: String = ""
    val connectedDevices = LinkedHashSet<BluetoothDevice>()
    val connectedGatts = LinkedHashSet<BluetoothGatt>()
    private val _gattState = MutableLiveData(
        ChannelSoundingConstant.GattState.CONNECTED
    )
    private val gattState: LiveData<ChannelSoundingConstant.GattState> = _gattState

    private val _connectedDeviceAddresses = MutableLiveData<List<String>>()

    private val _targetDevice = MutableLiveData<BluetoothDevice?>()

    private val targetDevice: LiveData<BluetoothDevice?> = _targetDevice

    private var bluetoothAdapter: BluetoothAdapter
    private var bluetoothManager: BluetoothManager

    init {
        bluetoothManager =
            application.getSystemService(Application.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    fun getGattState(): LiveData<ChannelSoundingConstant.GattState> {
        return gattState
    }

    fun setGattState(state: ChannelSoundingConstant.GattState) {
        _gattState.postValue(state)
    }

    fun setConnectedDeviceAddresses(addresses: List<String>) {
        _connectedDeviceAddresses.postValue(addresses)
    }

    fun setTargetDevice(device: BluetoothDevice) {
        _targetDevice.postValue(device)
    }

    fun getTargetDevice(): LiveData<BluetoothDevice?> {
        return targetDevice
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun setBleTargetDevice(deviceAddressAndName: String?) {
        Timber.tag(TAG).d("CS set target address: $deviceAddressAndName")
        if (!TextUtils.isEmpty(deviceAddressAndName)) {
            targetBtAddress = deviceAddressAndName!!.substring(0, 17) // Remove the name appended
            _targetDevice.postValue(bluetoothAdapter.getRemoteDevice(targetBtAddress))
        } else {
            targetBtAddress = ""
            _targetDevice.postValue(null)
        }
    }

    companion object {
        private const val TAG = "ChannelSoundingBleConnectViewModel"

    }

}

