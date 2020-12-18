package com.siliconlabs.bledemo.MainMenu.MenuItems

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.RangeTest.Activities.RangeTestActivity

class RangeTest(imageResId: Int, title: String, description: String) : MainMenuItem(imageResId, title, description) {

    override fun onClick(context: Context) {
        if (checkPermission()) {
            val intent = Intent(context, RangeTestActivity::class.java)
            ContextCompat.startActivity(context, intent, null)
        } else {
            Toast.makeText(context, context.getString(R.string.toast_bluetooth_not_enabled), Toast.LENGTH_SHORT).show()
        }
    }

    override fun checkPermission(): Boolean {
        return BluetoothAdapter.getDefaultAdapter().isEnabled
    }

}