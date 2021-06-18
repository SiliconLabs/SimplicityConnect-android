package com.siliconlabs.bledemo.advertiser.models

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertisingSetCallback
import android.os.Build
import android.os.Parcelable
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.siliconlabs.bledemo.advertiser.enums.AdvertisingMode
import com.siliconlabs.bledemo.advertiser.enums.LimitType
import kotlinx.android.parcel.Parcelize
import kotlin.math.ceil

@Parcelize
class Advertiser(var data: AdvertiserData = AdvertiserData(), var isRunning: Boolean = false) : Parcelable {
    lateinit var callback: Any
    lateinit var runnable: Runnable
    var displayDetailsView: Boolean = false

    fun isRunnableInitialized(): Boolean {
        return this::runnable.isInitialized
    }

    fun startLowApi(callback: AdvertiseCallback, errorCallback: ErrorCallback) {
        Log.d("Advertiser", "Start Low Api...")
        this.callback = callback

        val advertiser = BluetoothAdapter.getDefaultAdapter().bluetoothLeAdvertiser
        val settings = AdvertiserSettings(this.data).getAdvertiseSettings()
        val advData = data.advertisingData.getAdvertiseData()
        val scanRespData = data.scanResponseData.getAdvertiseData()

        try {
            if (data.mode == AdvertisingMode.NON_CONNECTABLE_NON_SCANNABLE) advertiser.startAdvertising(settings, advData, callback)
            else advertiser.startAdvertising(settings, advData, scanRespData, callback)
        } catch (e: IllegalArgumentException) {
            errorCallback.onErrorHandled(e.message.toString())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun start(callback: AdvertisingSetCallback, errorCallback: ErrorCallback) {
        Log.d("Advertiser", "Start...")
        this.callback = callback

        val advertiser = BluetoothAdapter.getDefaultAdapter().bluetoothLeAdvertiser
        val settings = AdvertiserSettings(this.data).getAdvertisingSetParameters()
        val advData = data.advertisingData.getAdvertiseData()
        val scanRespData = data.scanResponseData.getAdvertiseData()

        val duration = if (data.limitType == LimitType.NO_LIMIT) 0 else (ceil(data.timeLimit / 10.0).toInt())
        val events = if (data.limitType == LimitType.EVENT_LIMIT) data.eventLimit else 0

        try {
            if (data.isLegacy) {
                if (data.mode == AdvertisingMode.NON_CONNECTABLE_NON_SCANNABLE) advertiser.startAdvertisingSet(settings, advData, null, null, null, duration, events, callback)
                else advertiser.startAdvertisingSet(settings, advData, scanRespData, null, null, duration, events, callback)
            } else {
                if (data.mode == AdvertisingMode.NON_CONNECTABLE_SCANNABLE) advertiser.startAdvertisingSet(settings, null, scanRespData, null, null, duration, events, callback)
                else advertiser.startAdvertisingSet(settings, advData, null, null, null, duration, events, callback)
            }
        } catch (e: IllegalArgumentException) {
            errorCallback.onErrorHandled(e.message.toString())
        }
    }

    fun stop() {
        resetSettingsData()
        val advertiser = BluetoothAdapter.getDefaultAdapter().bluetoothLeAdvertiser

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("Advertiser", "Stop...")
            if (this::callback.isInitialized) advertiser?.stopAdvertisingSet(callback as AdvertisingSetCallback)
        } else {
            Log.d("Advertiser", "Stop Low Api...")
            if (this::callback.isInitialized) advertiser?.stopAdvertising(callback as AdvertiseCallback)
        }
    }

    private fun resetSettingsData() {
        isRunning = false
    }

    fun deepCopy(): Advertiser {
        val dataCopy = Gson().toJson(data)
        return Advertiser(Gson().fromJson(dataCopy, AdvertiserData::class.java))
    }

    interface ErrorCallback {
        fun onErrorHandled(message: String)
    }
}