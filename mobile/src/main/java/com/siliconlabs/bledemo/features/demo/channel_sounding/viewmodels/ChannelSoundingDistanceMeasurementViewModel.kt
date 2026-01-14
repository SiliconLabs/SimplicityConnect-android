package com.siliconlabs.bledemo.features.demo.channel_sounding.viewmodels

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.features.demo.channel_sounding.interfaces.BleConnection
import com.siliconlabs.bledemo.features.demo.channel_sounding.managers.ChannelSoundingDistanceMeasurementManager
import com.siliconlabs.bledemo.features.demo.channel_sounding.utils.ChannelSoundingConstant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
class ChannelSoundingDistanceMeasurementViewModel(
    activity: Activity,
    bleConnection: BleConnection
) : AndroidViewModel(activity.application), BleConnection {
    data class DistanceResult(val distanceMeter: Double, val confidence: Int)

    private val sessionState =
        MutableLiveData(ChannelSoundingConstant.RangeSessionState.STOPPED)

    private val distanceResult = MutableStateFlow(DistanceResult(0.0, 0))

    private val startFailureReasons = MutableLiveData<List<String>>(emptyList())

    private var distanceMeasurementManger: ChannelSoundingDistanceMeasurementManager

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun setTargetDevice(targetDevice: BluetoothDevice?) {
        Timber.tag(TAG).d("CS DistanceMeasurementViewModel setTargetDevice: $targetDevice")
        if (targetDevice != null) {
            distanceMeasurementManger.setTargetDevice(targetDevice)
        }
    }

    fun getSessionState(): LiveData<ChannelSoundingConstant.RangeSessionState> = sessionState

    fun getDistanceResult(): StateFlow<DistanceResult> = distanceResult


    fun getSupportedTechnologies(): List<String> = distanceMeasurementManger.getSupportedTechnologies()

    fun getMeasurementFrequencies(): List<String> = distanceMeasurementManger.getMeasurementFrequencies()

    fun getMeasurementDurations(): List<String> = distanceMeasurementManger.getMeasureDurationsInIntervalRounds()

    fun getLocationTypes(): List<String> = distanceMeasurementManger.getLocationTypes()

    fun getSightType(): List<String> = distanceMeasurementManger.getSightType()

    fun getSensorFusionEnable(): List<String> = distanceMeasurementManger.getSensorFusionEnable()

    class Factory(
        val activity: Activity,
        val bleConnection: BleConnection
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChannelSoundingDistanceMeasurementViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ChannelSoundingDistanceMeasurementViewModel(activity, bleConnection) as T
            }
            throw IllegalArgumentException("Unable to ChannelSoundingDistanceMeasurementViewModel construct viewmodel")
        }
    }

    private val callback = object : ChannelSoundingDistanceMeasurementManager.Callback {
        override fun onStartSuccess() {
            startFailureReasons.postValue(emptyList())
            sessionState.postValue(ChannelSoundingConstant.RangeSessionState.STARTED)
        }

        override fun onStartFail(reasons: List<String>) {
            Timber.tag(TAG).e("CS Ranging start failed: $reasons")
            startFailureReasons.postValue(reasons)
            sessionState.postValue(ChannelSoundingConstant.RangeSessionState.STOPPED)
        }

        override fun onStop() {
            sessionState.postValue(ChannelSoundingConstant.RangeSessionState.STOPPED)
        }

        override fun onDistanceResult(distanceMeter: Double?, confidence: Integer?) {
            distanceResult.value = DistanceResult(distanceMeter ?: 0.0, (confidence ?: 0) as Int)
        }
    }

    fun channelSoundingStop(){
        val current = sessionState.value
        // Only stop if not already stopped or stopping
        if (current != ChannelSoundingConstant.RangeSessionState.STOPPED && 
            current != ChannelSoundingConstant.RangeSessionState.STOPPING) {
            sessionState.postValue(ChannelSoundingConstant.RangeSessionState.STOPPING)
            try {
                distanceMeasurementManger.stop()
            } catch (e: SecurityException) {
                Timber.tag(TAG).e("Missing Bluetooth permission: ${e.message}")
                sessionState.postValue(ChannelSoundingConstant.RangeSessionState.STOPPED)
            }
            // State will be updated to STOPPED by onStop() callback
        }
    }

    fun channelSoundingStart(technology: String, freq: String, duration: Int){
        val current = sessionState.value
        Timber.tag(TAG).d("CS toggleStartStop current state: $current")
        try {
            val success: Boolean =   distanceMeasurementManger.start(technology, freq, duration)
            if (success) {
                sessionState.postValue(ChannelSoundingConstant.RangeSessionState.STARTING)
            } else {
                sessionState.postValue(ChannelSoundingConstant.RangeSessionState.STOPPED)
            }
        }catch (e: SecurityException) {
            Timber.tag(TAG).e("Missing Bluetooth permission: ${e.message}")
        }
    }

    fun toggleStartStop(technology: String, freq: String, duration: Int) {
        val current = sessionState.value
        Timber.tag(TAG).d("CS toggleStartStop current state: $current")
        
        // Prevent duplicate operations
        if (current == ChannelSoundingConstant.RangeSessionState.STARTING) {
            Timber.tag(TAG).w("CS Already starting, ignoring duplicate start request")
            return
        }
        if (current == ChannelSoundingConstant.RangeSessionState.STOPPING) {
            Timber.tag(TAG).w("CS Already stopping, ignoring duplicate stop request")
            return
        }
        
        if (current == ChannelSoundingConstant.RangeSessionState.STOPPED || current == ChannelSoundingConstant.RangeSessionState.STOPPING) {
            Timber.tag(TAG).d("CS Starting distance measurement")
            Timber.tag(TAG).d("CS Technology: $technology, Freq: $freq, Duration: $duration")
            val success: Boolean =
                distanceMeasurementManger.start(
                    technology, freq, duration
                )
            if (success) {
                sessionState.postValue(ChannelSoundingConstant.RangeSessionState.STARTING)
            } else {
                sessionState.postValue(ChannelSoundingConstant.RangeSessionState.STOPPED)
            }
        } else if (current == ChannelSoundingConstant.RangeSessionState.STARTED
            || current == ChannelSoundingConstant.RangeSessionState.STARTING) {
            Timber.tag(TAG).d("CS Stopping distance measurement")
            sessionState.postValue(ChannelSoundingConstant.RangeSessionState.STOPPING)
            try {
                distanceMeasurementManger.stop()
            } catch (e: SecurityException) {
                Timber.tag(TAG).e("Missing Bluetooth permission: ${e.message}")
                sessionState.postValue(ChannelSoundingConstant.RangeSessionState.STOPPED)
            }
        }
    }

    init {
        distanceMeasurementManger = ChannelSoundingDistanceMeasurementManager(
            activity,
            bleConnection, false,
            callback
        )
    }

    companion object {
        private val TAG = "CS_DistanceMeasurementVM"
    }
}