package com.siliconlabs.bledemo.home_screen.viewmodels

import androidx.core.content.ContextCompat
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

    private val _isSetupFinished: MutableLiveData<Boolean> = MutableLiveData()
    val isSetupFinished: LiveData<Boolean> = _isSetupFinished

    private val _isNotificationOn: MutableLiveData<Boolean> = MutableLiveData()
    val isNotificationOn: LiveData<Boolean> = _isNotificationOn

    private val _isNotificationPermissionGranted = MutableLiveData<Boolean>()
    val isNotificationPermissionGranted: LiveData<Boolean> get() = _isNotificationPermissionGranted

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

    fun setIsSetupFinished(isSetupFinished: Boolean) {
        _isSetupFinished.postValue(isSetupFinished)
    }

    fun setIsNotificationOn(isNotificationOn: Boolean) {
        _isNotificationOn.postValue(isNotificationOn)
    }

    fun setIsNotificationPermissionGranted(isGranted: Boolean){
        _isNotificationPermissionGranted.postValue(isGranted)
    }


    fun getIsBluetoothOn(): Boolean = _isBluetoothOn.value ?: false
    fun getIsLocationPermissionGranted(): Boolean = _isLocationPermissionGranted.value ?: false
    fun getAreBluetoothPermissionsGranted(): Boolean = _areBluetoothPermissionsGranted.value ?: false
    fun getIsSetupFinished(): Boolean = _isSetupFinished.value ?: false


}