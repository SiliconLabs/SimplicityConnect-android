package com.siliconlabs.bledemo.features.demo.matter_demo.dishwasher_demo.repo

import android.content.Context
import android.content.SharedPreferences
import chip.devicecontroller.ChipClusters
import chip.devicecontroller.ChipClusters.OperationalStateCluster
import com.siliconlabs.bledemo.features.demo.matter_demo.dishwasher_demo.utils.DishWasherEnumConstants
import com.siliconlabs.bledemo.features.demo.matter_demo.dishwasher_demo.view.MatterDishwasherFragment.Companion.DISHWASHER_ELECTRICAL_POWER_MEASUREMENT_ENDPOINT
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.ChipClient
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.SharedPrefsUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatterDishWasherRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {

    fun saveTotalEnergyConsumption(totalEnergyConsumed: Float) {
        SharedPrefsUtils.saveDishwasherTotalEnergyConsumption(sharedPreferences, totalEnergyConsumed)
    }

    fun getTotalEnergyConsumption(): Float {
        return SharedPrefsUtils.getDishwasherTotalEnergyConsumption(sharedPreferences)
    }

    fun saveAverageEnergyPerCycle(averageEnergyPerCycleConsumed: Float) {
        SharedPrefsUtils.saveDishwasherAverageEnergyPerCycle(sharedPreferences, averageEnergyPerCycleConsumed)
    }

    fun getAverageEnergyPerCycle(): Float {
        return SharedPrefsUtils.getDishwasherAverageEnergyPerCycle(sharedPreferences)
    }

    fun saveInCurrentCycleEnergyConsumed(inCurrentCycleEnergyConsumed: Float) {
        SharedPrefsUtils.saveDishwasherInCurrentCycleEnergyConsumed(sharedPreferences, inCurrentCycleEnergyConsumed)
    }

    fun getInCurrentCycleEnergyConsumed(): Float {
        return SharedPrefsUtils.getDishwasherInCurrentCycleEnergyConsumed(sharedPreferences)
    }

    fun saveCompletedCycleCount(completedCycleCount: Int) {
        SharedPrefsUtils.saveDishwasherCompletedCycleCount(sharedPreferences, completedCycleCount)
    }

    fun getCompletedCycleCount(): Int {
        return SharedPrefsUtils.getDishwasherCompletedCycleCount(sharedPreferences)
    }

    fun saveTimeLeftFormatted(timeLeftInMillSeconds: Long) {
        SharedPrefsUtils.saveDishwasherTimeLeftFormatted(sharedPreferences, timeLeftInMillSeconds)
    }

    fun getTimeLeftFormatted(): Long {
        return SharedPrefsUtils.getDishwasherTimeLeftFormatted(sharedPreferences)
    }

    fun saveAppliedCycleStates(cycleStates: DishWasherEnumConstants) {
        SharedPrefsUtils.saveDishwasherAppliedCycleStates(sharedPreferences, cycleStates)
    }

    fun getAppliedCycleStates(): DishWasherEnumConstants? {
        return SharedPrefsUtils.getDishwasherAppliedCycleStates(sharedPreferences)
    }

    fun saveCompletedCycleProgressBar(completedProgressBarCount: Int) {
        SharedPrefsUtils.saveDishwasherCompletedCycleProgressBar(sharedPreferences, completedProgressBarCount)
    }

    fun getCompletedCycleProgressBar(): Int {
        return SharedPrefsUtils.getDishwasherCompletedCycleProgressBar(sharedPreferences)
    }


    suspend fun getEleDevMag(context: Context, deviceId: Long?): ChipClusters.ElectricalEnergyMeasurementCluster {
        return ChipClusters.ElectricalEnergyMeasurementCluster(
            ChipClient.getConnectedDevicePointer(context, deviceId!!),
            DISHWASHER_ELECTRICAL_POWER_MEASUREMENT_ENDPOINT
        )
    }

    suspend fun getDishwasherClusterForDevice(context: Context,deviceId: Long?,endPointId:Int): OperationalStateCluster {
        return OperationalStateCluster(
            ChipClient.getConnectedDevicePointer(context, deviceId!!), endPointId
        )
    }
}