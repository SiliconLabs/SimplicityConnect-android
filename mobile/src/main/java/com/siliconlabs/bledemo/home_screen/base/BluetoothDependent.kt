package com.siliconlabs.bledemo.home_screen.base

interface BluetoothDependent {

    fun onBluetoothStateChanged(isBluetoothOn: Boolean)
    fun onBluetoothPermissionsStateChanged(arePermissionsGranted: Boolean)
    fun refreshBluetoothDependentUi(isBluetoothOperationPossible: Boolean)
    fun setupBluetoothPermissionsBarButtons()
}