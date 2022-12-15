package com.siliconlabs.bledemo.features.scan.browser.models

import android.bluetooth.BluetoothDevice
import com.siliconlabs.bledemo.bluetooth.ble.BluetoothDeviceInfo

data class ScannedDeviceInfo(
        var bluetoothInfo: BluetoothDeviceInfo,
        val graphInfo: GraphInfo,
        var isBluetoothInfoExpanded: Boolean = false
) {

    constructor(device: BluetoothDevice,
                isFavorite: Boolean,
                graphDataColor: Int
    ) : this(
            BluetoothDeviceInfo(device, isFavorite),
            GraphInfo(mutableListOf(), graphDataColor)
    )

}