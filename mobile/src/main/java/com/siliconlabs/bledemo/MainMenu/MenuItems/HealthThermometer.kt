package com.siliconlabs.bledemo.MainMenu.MenuItems

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.util.Pair
import android.widget.Toast
import com.siliconlabs.bledemo.Base.SelectDeviceDialog
import com.siliconlabs.bledemo.Bluetooth.BLE.BlueToothService
import com.siliconlabs.bledemo.MainMenu.Activities.MainMenuActivity
import com.siliconlabs.bledemo.R
import java.util.*

class HealthThermometer(imageResId: Int, title: String, description: String) : MainMenuItem(imageResId, title, description) {

    override fun onClick(context: Context) {
        if(checkPermission()) {
            val connectType = BlueToothService.GattConnectType.THERMOMETER
            val profilesInfo = Arrays.asList(Pair(R.string.htp_title, R.string.htp_id))
            val selectDeviceDialog = SelectDeviceDialog.newDialog(R.string.title_Health_Thermometer, R.string.main_menu_description_thermometer, profilesInfo, connectType)
            selectDeviceDialog.show((context as MainMenuActivity).supportFragmentManager, "select_device_tag")
        } else {
            Toast.makeText(context,context.getString(R.string.toast_bluetooth_not_enabled), Toast.LENGTH_SHORT).show()
        }
    }

    override fun checkPermission(): Boolean {
        return BluetoothAdapter.getDefaultAdapter().isEnabled
    }

}