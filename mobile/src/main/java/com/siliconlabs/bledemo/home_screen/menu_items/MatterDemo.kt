package com.siliconlabs.bledemo.home_screen.menu_items

import androidx.annotation.DrawableRes
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService

class MatterDemo(
    @DrawableRes imageResId: Int,
    title: String,
    description: String
) : DemoMenuItem(imageResId, title, description) {
    override val connectType = BluetoothService.GattConnectType.MATTER_DEMO
}