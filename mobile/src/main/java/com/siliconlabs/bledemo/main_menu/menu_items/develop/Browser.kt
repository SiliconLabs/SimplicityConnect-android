package com.siliconlabs.bledemo.main_menu.menu_items.develop

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import com.siliconlabs.bledemo.browser.activities.BrowserActivity
import com.siliconlabs.bledemo.main_menu.menu_items.MainMenuItem
import com.siliconlabs.bledemo.R

class Browser(imageResId: Int, title: String, description: String) : MainMenuItem(imageResId, title, description) {

    override fun permissionGranted(context: Context) {
        val intent = Intent(context, BrowserActivity::class.java)
        startActivity(context, intent, null)
    }

    override fun permissionDenied(context: Context) {
        Toast.makeText(context, context.getString(R.string.toast_bluetooth_not_enabled), Toast.LENGTH_SHORT).show()
    }

    override fun checkPermission(context: Context): Boolean {
        return BluetoothAdapter.getDefaultAdapter().isEnabled
    }

}
