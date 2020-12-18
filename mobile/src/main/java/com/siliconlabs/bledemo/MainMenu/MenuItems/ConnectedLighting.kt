package com.siliconlabs.bledemo.MainMenu.MenuItems

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.widget.Toast
import com.siliconlabs.bledemo.Base.SelectDeviceDialog
import com.siliconlabs.bledemo.Bluetooth.BLE.BlueToothService
import com.siliconlabs.bledemo.MainMenu.Activities.MainMenuActivity
import com.siliconlabs.bledemo.R

class ConnectedLighting(imageResId: Int, title: String, description: String) : MainMenuItem(imageResId, title, description) {

    override fun onClick(context: Context) {
        if (checkPermission()) {
            val connectType = BlueToothService.GattConnectType.LIGHT
            val selectDeviceDialog = SelectDeviceDialog.newDialog(R.string.title_Connected_Lighting, R.string.main_menu_description_connected_lighting, null, connectType)
            selectDeviceDialog.show((context as MainMenuActivity).supportFragmentManager, "select_device_tag")
        } else {
            Toast.makeText(context, context.getString(R.string.toast_bluetooth_not_enabled), Toast.LENGTH_SHORT).show()
        }
    }

    override fun checkPermission(): Boolean {
        return BluetoothAdapter.getDefaultAdapter().isEnabled
    }

}