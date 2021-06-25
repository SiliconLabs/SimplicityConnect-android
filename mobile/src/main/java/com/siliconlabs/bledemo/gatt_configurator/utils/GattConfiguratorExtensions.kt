package com.siliconlabs.bledemo.gatt_configurator.utils

import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.siliconlabs.bledemo.gatt_configurator.dialogs.GattConfiguratorRemovalDialog

fun View.removeAsking(@StringRes name: Int, remove: () -> Unit) {
    if (GattConfiguratorStorage(context).shouldDisplayRemovalDialog()) {
        val activity = context as? AppCompatActivity ?: return
        GattConfiguratorRemovalDialog(name) {
            remove()
        }.show(activity.supportFragmentManager, null)
    } else {
        remove()
    }
}

fun AppCompatActivity.removeAsking(@StringRes name: Int, remove: () -> Unit) {
    if (GattConfiguratorStorage(this).shouldDisplayRemovalDialog()) {
        GattConfiguratorRemovalDialog(name) {
            remove()
        }.show(supportFragmentManager, null)
    } else {
        remove()
    }
}
