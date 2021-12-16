package com.siliconlabs.bledemo.MainMenu.MenuItems.Demo

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.widget.Toast
import com.siliconlabs.bledemo.Base.SelectDeviceDialog
import com.siliconlabs.bledemo.Bluetooth.Services.BluetoothService
import com.siliconlabs.bledemo.MainMenu.Activities.MainMenuActivity
import com.siliconlabs.bledemo.MainMenu.MenuItems.MainMenuItem
import com.siliconlabs.bledemo.R

class WifiCommissioning(imageResId: Int, title: String, description: String) : MainMenuItem(imageResId, title, description) {

    override fun onClick(context: Context) {
        if (checkPermission(context)) {
            val connectType = BluetoothService.GattConnectType.WIFI_COMMISSIONING
            val selectDeviceDialog = SelectDeviceDialog.newDialog(R.string.wifi_commissioning_label, R.string
                    .wifi_commissioning_description, null, connectType)
            selectDeviceDialog.show((context as MainMenuActivity).supportFragmentManager, "select_device_tag")
        } else {
            Toast.makeText(context, context.getString(R.string.toast_bluetooth_not_enabled), Toast.LENGTH_SHORT).show()
        }
    }

    override fun checkPermission(context: Context): Boolean {
        return BluetoothAdapter.getDefaultAdapter().isEnabled
    }

    override fun permissionGranted(context: Context) {

    }

    override fun permissionDenied(context: Context) {

    }
}