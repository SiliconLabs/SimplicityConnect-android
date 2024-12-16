package com.siliconlabs.bledemo.home_screen.menu_items

import com.siliconlabs.bledemo.bluetooth.services.BluetoothService

//Sachith
class WiFiThroughput(imageResId: Int, title: String, description: String) : DemoMenuItem(imageResId, title, description) {
    override val connectType = BluetoothService.GattConnectType.WIFI_THROUGHPUT_TEST
}