package com.siliconlabs.bledemo.features.demo.matter_demo.utils

import android.content.SharedPreferences
import com.google.gson.Gson
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

    fun saveDevicesToPref(
        mPrefs: SharedPreferences,
        scannedDeviceList: ArrayList<MatterScannedResultModel>
    ) {
        val prefsEditor: SharedPreferences.Editor = mPrefs.edit()
        val gson = Gson()
        val json = gson.toJson(scannedDeviceList)
        prefsEditor.putString(ARG_ADDED_DEVICE_INFO_LIST, json)
        prefsEditor.apply()
    }

    fun updateDeviceAtIndex(
        mPrefs: SharedPreferences,
        index: Int,
        updatedDevice: MatterScannedResultModel
    ) {
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

    //Total energy
    fun saveDishwasherTotalEnergyConsumption(
        mPrefs: SharedPreferences,
        totalEnergyConsumed: Float
    ) {
        val prefsEditor: SharedPreferences.Editor = mPrefs.edit()
        prefsEditor.apply {
            putFloat(TOTAL_ENERGY_DISHWASHER, totalEnergyConsumed)
            apply()
        }

    }

    fun getDishwasherTotalEnergyConsumption(mPrefs: SharedPreferences): Float {

        return mPrefs.getFloat(TOTAL_ENERGY_DISHWASHER, 0f)
    }

    //average energy
    fun saveDishwasherAverageEnergyPerCycle(
        mPrefs: SharedPreferences,
        averageEnergyPerCycleConsumed: Float
    ) {
        val prefsEditor: SharedPreferences.Editor = mPrefs.edit()
        prefsEditor.apply {
            putFloat(AVERAGE_ENERGY_PER_CYCLE_DISHWASHER, averageEnergyPerCycleConsumed)
            apply()
        }

    }

    fun getDishwasherAverageEnergyPerCycle(mPrefs: SharedPreferences): Float{

        return mPrefs.getFloat(AVERAGE_ENERGY_PER_CYCLE_DISHWASHER, 0f)
    }

    //inCurrent cycle
    fun saveDishwasherInCurrentCycleEnergyConsumed(
        mPrefs: SharedPreferences,
        inCurrentCycleEnergyConsumed: Float
    ) {
        val prefsEditor: SharedPreferences.Editor = mPrefs.edit()
        prefsEditor.apply {
            putFloat(DISHWASHER_IN_CURRENT_CYCLE_CONSUMED, inCurrentCycleEnergyConsumed)
            apply()
        }
    }

    fun getDishwasherInCurrentCycleEnergyConsumed(mPrefs: SharedPreferences): Float {

        return mPrefs.getFloat(DISHWASHER_IN_CURRENT_CYCLE_CONSUMED, 0f)
    }

    //completedCycleCount
    fun saveDishwasherCompletedCycleCount(
        mPrefs: SharedPreferences,
        completedCycleCount: Int
    ) {
        val prefsEditor: SharedPreferences.Editor = mPrefs.edit()
        prefsEditor.apply {
            putInt(DISHWASHER_COMPLETED_CYCLE_COUNT, completedCycleCount)
            apply()
        }
    }

    fun getDishwasherCompletedCycleCount(mPrefs: SharedPreferences): Int {

        return mPrefs.getInt(DISHWASHER_COMPLETED_CYCLE_COUNT, 0)
    }


    fun saveDishwasherTimeLeftFormatted(mPrefs: SharedPreferences, timeLeftInMillSeconds: Long) {
        val prefsEditor: SharedPreferences.Editor = mPrefs.edit()
        prefsEditor.apply {
            putLong(DISHWASHER_TOTAL_TIME_LEFT, timeLeftInMillSeconds)
            apply()
        }
    }


    fun getDishwasherTimeLeftFormatted(mPrefs: SharedPreferences): Long {
        return mPrefs.getLong(DISHWASHER_TOTAL_TIME_LEFT, 600000)
    }

    fun saveDishwasherAppliedCycleStates(mPrefs: SharedPreferences, cycleStates: String) {
        val prefsEditor: SharedPreferences.Editor = mPrefs.edit()
        prefsEditor.apply {
            putString(
                DISHWASHER_CYCLE_STATES,
                cycleStates
            )
            apply()
        }
    }

    fun getDishwasherAppliedCycleStates(mPrefs: SharedPreferences): String? {

        return mPrefs.getString(DISHWASHER_CYCLE_STATES, null)
    }


    //SAVE progressBar
    fun saveDishwasherCompletedCycleProgressBar(
        mPrefs: SharedPreferences,
        completedProgressBarCount: Int
    ) {
        val prefsEditor: SharedPreferences.Editor = mPrefs.edit()
        prefsEditor.apply {
            putInt(DISHWASHER_COMPLETED_PROGRESS_BAR_COUNT, completedProgressBarCount)
            apply()
        }
    }

    fun getDishwasherCompletedCycleProgressBar(mPrefs: SharedPreferences): Int {
        return mPrefs.getInt(DISHWASHER_COMPLETED_PROGRESS_BAR_COUNT, 0)
    }

    fun clearDishwasherSharedPreferences(preferences: SharedPreferences) {
        preferences.edit().apply {
            clear()  // Clears all data in the SharedPreferences
            apply()  // Commit the changes asynchronously
        }
    }

    fun saveTheDishwasherStateIfTheUserIsInPauseState(mPrefs: SharedPreferences, savePauseOrStopState: Boolean): Boolean {
        val prefsEditor: SharedPreferences.Editor = mPrefs.edit()

        // Save the state and apply the changes
        prefsEditor.putBoolean(DISHWASHER_ON_CYCLE_PAUSE_RESUME_STATE, savePauseOrStopState)
        prefsEditor.apply()

        // Return true if the operation was successful (SharedPreferences doesn't give an explicit success/failure status)
        return true
    }

    fun getDishwasherStateIfUserIsInPauseState(mPrefs: SharedPreferences): Boolean {
        // Retrieve the state using the same key, with a default value of false if not found
        return mPrefs.getBoolean(DISHWASHER_ON_CYCLE_PAUSE_RESUME_STATE, false)
    }


    const val ARG_ADDED_DEVICE_INFO_LIST = "addedDeviceInfoList"
    const val TOTAL_ENERGY_DISHWASHER = "totalEnergyDishwasher"
    const val AVERAGE_ENERGY_PER_CYCLE_DISHWASHER = "averageEnergyPerCycle"
    const val DISHWASHER_IN_CURRENT_CYCLE_CONSUMED = "inCurrentCycle"
    const val DISHWASHER_COMPLETED_CYCLE_COUNT = "cycleCount"
    const val DISHWASHER_TOTAL_TIME_LEFT = "timeLeft"
    const val DISHWASHER_CYCLE_STATES = "dishwasherCycleStates"
    const val DISHWASHER_COMPLETED_PROGRESS_BAR_COUNT = "dishwasherCompletedProgressBarCount"
    const val DISHWASHER_ON_CYCLE_PAUSE_RESUME_STATE = "dishwasherOnPauseOrStopState"


}