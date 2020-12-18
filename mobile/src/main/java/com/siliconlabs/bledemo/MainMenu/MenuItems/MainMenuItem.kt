package com.siliconlabs.bledemo.MainMenu.MenuItems

import android.content.Context

abstract class MainMenuItem(val imageResId: Int, val title: String, val description: String) {

    abstract fun onClick(context: Context)

    abstract fun checkPermission(): Boolean
}