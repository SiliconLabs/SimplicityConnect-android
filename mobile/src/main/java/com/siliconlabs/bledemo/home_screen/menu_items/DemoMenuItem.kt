package com.siliconlabs.bledemo.home_screen.menu_items

import androidx.annotation.DrawableRes
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService

abstract class DemoMenuItem(@DrawableRes val imageResId: Int, val title: String, val description: String) {

    abstract val connectType: BluetoothService.GattConnectType
}