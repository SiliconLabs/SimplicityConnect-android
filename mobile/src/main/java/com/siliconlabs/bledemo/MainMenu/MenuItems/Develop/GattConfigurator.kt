package com.siliconlabs.bledemo.MainMenu.MenuItems.Develop

import android.content.Context
import com.siliconlabs.bledemo.gatt_configurator.activities.GattConfiguratorActivity
import com.siliconlabs.bledemo.MainMenu.MenuItems.MainMenuItem

class GattConfigurator(imageResId: Int, title: String, description: String) : MainMenuItem(imageResId, title, description) {

    override fun permissionGranted(context: Context) {
        GattConfiguratorActivity.startActivity(context)
    }

    override fun permissionDenied(context: Context) {
    }

    override fun checkPermission(context: Context): Boolean {
        return true
    }

}