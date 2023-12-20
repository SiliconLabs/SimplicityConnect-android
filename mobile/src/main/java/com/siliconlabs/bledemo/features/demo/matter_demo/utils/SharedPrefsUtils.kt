package com.siliconlabs.bledemo.features.demo.matter_demo.utils
import android.content.SharedPreferences
import com.google.gson.Gson
import android.content.Context
import com.google.gson.reflect.TypeToken
import com.siliconlabs.bledemo.features.demo.matter_demo.model.MatterScannedResultModel

object SharedPrefsUtils {
    fun retrieveSavedDevices(mPrefs: SharedPreferences): ArrayList<MatterScannedResultModel> {
        val json = mPrefs.getString("addedDeviceInfoList", null)
        val gson = Gson()

        val myListType = object : TypeToken<ArrayList<MatterScannedResultModel>>() {}.type
        val myList = gson.fromJson<ArrayList<MatterScannedResultModel>>(json, myListType)

        return myList ?: ArrayList()
    }
    fun saveDevicesToPref(mPrefs: SharedPreferences, scannedDeviceList: ArrayList<MatterScannedResultModel>) {
        val prefsEditor: SharedPreferences.Editor = mPrefs.edit()
        val gson = Gson()
        val json = gson.toJson(scannedDeviceList)
        prefsEditor.putString(ARG_ADDED_DEVICE_INFO_LIST, json)
        prefsEditor.apply()
    }

    fun updateDeviceAtIndex(mPrefs: SharedPreferences, index: Int, updatedDevice: MatterScannedResultModel) {
        val existingList = retrieveSavedDevices(mPrefs)
        if (index >= 0 && index < existingList.size) {
            existingList[index] = updatedDevice
            saveDevicesToPref(mPrefs, existingList)
        }
    }
    fun updateDeviceByDeviceId(mPrefs: SharedPreferences, deviceId: Long, newValue: Boolean) {
        val deviceList = retrieveSavedDevices(mPrefs)
        var updated = false

        for (device in deviceList) {
            if (device.deviceId == deviceId) { // Convert Long to String
                device.isDeviceOnline = newValue
                updated = true
                break
            }
        }

        if (updated) {
            saveDevicesToPref(mPrefs, deviceList)
        }
    }
        const val ARG_ADDED_DEVICE_INFO_LIST = "addedDeviceInfoList"

}