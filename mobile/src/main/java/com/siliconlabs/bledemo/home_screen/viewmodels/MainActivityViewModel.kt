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

    fun setIsBluetoothOn(isBluetoothOn: Boolean) {
        _isBluetoothOn.postValue(isBluetoothOn)
    }

    fun setIsLocationOn(isLocationOn: Boolean) {
        _isLocationOn.postValue(isLocationOn)
    }

    fun setIsLocationPermissionGranted(isGranted: Boolean) {
        _isLocationPermissionGranted.postValue(isGranted)
    }

    fun getIsBluetoothOn(): Boolean = _isBluetoothOn.value ?: false
    fun getIsLocationPermissionGranted(): Boolean = _isLocationPermissionGranted.value ?: false
}