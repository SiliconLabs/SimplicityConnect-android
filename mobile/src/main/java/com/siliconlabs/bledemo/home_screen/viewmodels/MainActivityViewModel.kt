package com.siliconlabs.bledemo.home_screen.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainActivityViewModel : ViewModel() {

    private val _isBluetoothOn: MutableLiveData<Boolean> = MutableLiveData()
    val isBluetoothOn: LiveData<Boolean> = _isBluetoothOn
    private val _isLocationOn: MutableLiveData<Boolean> = MutableLiveData()
    val isLocationOn: LiveData<Boolean> = _isLocationOn
    private val _isLocationPermissionGranted: MutableLiveData<Boolean> = MutableLiveData()
    val isLocationPermissionGranted: LiveData<Boolean> = _isLocationPermissionGranted
    private val _areBluetoothPermissionsGranted: MutableLiveData<Boolean> = MutableLiveData()
    val areBluetoothPermissionGranted: LiveData<Boolean> = _areBluetoothPermissionsGranted

    fun setIsBluetoothOn(isBluetoothOn: Boolean) {
        _isBluetoothOn.postValue(isBluetoothOn)
    }

    fun setIsLocationOn(isLocationOn: Boolean) {
        _isLocationOn.postValue(isLocationOn)
    }

    fun setIsLocationPermissionGranted(isGranted: Boolean) {
        _isLocationPermissionGranted.postValue(isGranted)
    }

    fun setAreBluetoothPermissionsGranted(areGranted: Boolean) {
        _areBluetoothPermissionsGranted.postValue(areGranted)
    }

    fun getIsBluetoothOn(): Boolean = _isBluetoothOn.value ?: false
    fun getIsLocationPermissionGranted(): Boolean = _isLocationPermissionGranted.value ?: false
    fun getAreBluetoothPermissionsGranted(): Boolean = _areBluetoothPermissionsGranted.value ?: false
}