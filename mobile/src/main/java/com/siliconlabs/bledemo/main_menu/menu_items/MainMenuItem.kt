package com.siliconlabs.bledemo.main_menu.menu_items

import android.content.Context

abstract class MainMenuItem(val imageResId: Int, val title: String, val description: String) {

    open fun onClick(context: Context) {
        if (checkPermission(context)) {
            permissionGranted(context)
        } else {
            permissionDenied(context)
        }
    }

    abstract fun permissionGranted(context: Context)

    abstract fun permissionDenied(context: Context)

    abstract fun checkPermission(context: Context): Boolean
}