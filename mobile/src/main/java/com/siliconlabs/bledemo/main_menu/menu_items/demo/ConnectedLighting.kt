package com.siliconlabs.bledemo.main_menu.menu_items.demo

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.widget.Toast
import com.siliconlabs.bledemo.base.SelectDeviceDialog
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import com.siliconlabs.bledemo.main_menu.activities.MainMenuActivity
import com.siliconlabs.bledemo.main_menu.menu_items.MainMenuItem
import com.siliconlabs.bledemo.R

class ConnectedLighting(imageResId: Int, title: String, description: String) : MainMenuItem(imageResId, title, description) {

    override fun permissionGranted(context: Context) {
        val connectType = BluetoothService.GattConnectType.LIGHT
        val selectDeviceDialog = SelectDeviceDialog.newDialog(R.string.title_Connected_Lighting, R.string.main_menu_description_connected_lighting, null, connectType)
        selectDeviceDialog.show((context as MainMenuActivity).supportFragmentManager, "select_device_tag")
    }

    override fun permissionDenied(context: Context) {
        Toast.makeText(context, context.getString(R.string.toast_bluetooth_not_enabled), Toast.LENGTH_SHORT).show()
    }

    override fun checkPermission(context: Context): Boolean {
        return BluetoothAdapter.getDefaultAdapter().isEnabled
    }
}