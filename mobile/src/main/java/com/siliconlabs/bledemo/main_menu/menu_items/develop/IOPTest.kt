package com.siliconlabs.bledemo.main_menu.menu_items.develop

import android.bluetooth.BluetoothAdapter
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.siliconlabs.bledemo.iop_test.dialogs.IOPDeviceNameDialog
import com.siliconlabs.bledemo.main_menu.menu_items.MainMenuItem

class IOPTest(imageResId: Int, title: String, description: String) : MainMenuItem(imageResId, title, description) {

    override fun onClick(context: Context) {
        IOPDeviceNameDialog().show((context as AppCompatActivity).supportFragmentManager, "dialog_iop_device_name")
    }

    override fun checkPermission(context: Context): Boolean {
        return BluetoothAdapter.getDefaultAdapter().isEnabled
    }

    override fun permissionGranted(context: Context) {
    }

    override fun permissionDenied(context: Context) {
    }


}