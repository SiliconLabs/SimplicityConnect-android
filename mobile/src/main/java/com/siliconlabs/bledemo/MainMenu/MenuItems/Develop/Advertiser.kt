package com.siliconlabs.bledemo.MainMenu.MenuItems.Develop

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import com.siliconlabs.bledemo.Advertiser.Activities.AdvertiserActivity
import com.siliconlabs.bledemo.Advertiser.Utils.AdvertiserStorage
import com.siliconlabs.bledemo.MainMenu.MenuItems.MainMenuItem
import com.siliconlabs.bledemo.R

class Advertiser(imageResId: Int, title: String, description: String) : MainMenuItem(imageResId, title, description) {

    override fun permissionGranted(context: Context) {
        val intent = Intent(context, AdvertiserActivity::class.java)
        startActivity(context, intent, null)
    }

    override fun permissionDenied(context: Context) {
        Toast.makeText(context, context.getString(R.string.toast_bluetooth_not_enabled), Toast.LENGTH_SHORT).show()
    }

    override fun checkPermission(context: Context): Boolean {
        return BluetoothAdapter.getDefaultAdapter().isEnabled || AdvertiserStorage(context).isAdvertisingBluetoothInfoChecked()
    }

}