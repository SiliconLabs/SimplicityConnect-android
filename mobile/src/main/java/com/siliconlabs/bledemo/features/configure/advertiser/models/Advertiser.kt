package com.siliconlabs.bledemo.features.configure.advertiser.models

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertisingSetCallback
import android.os.Parcelable
import com.google.gson.Gson
import com.siliconlabs.bledemo.features.configure.advertiser.enums.AdvertisingMode
import com.siliconlabs.bledemo.features.configure.advertiser.enums.LimitType
import kotlinx.android.parcel.Parcelize
import timber.log.Timber
import kotlin.math.ceil

@Parcelize
class Advertiser(var data: AdvertiserData = AdvertiserData(), var isRunning: Boolean = false) : Parcelable {
    @Transient lateinit var callback: AdvertisingSetCallback
    @Transient lateinit var runnable: Runnable
    var displayDetailsView: Boolean = false

    fun isRunnableInitialized(): Boolean {
        return this::runnable.isInitialized
    }

    fun start(callback: AdvertisingSetCallback, errorCallback: ErrorCallback) {
        Timber.d("Advertiser start")
        this.callback = callback

        val advertiser = BluetoothAdapter.getDefaultAdapter().bluetoothLeAdvertiser
        val settings = AdvertiserSettings(this.data).getAdvertisingSetParameters()
        val advData = data.advertisingData.getAdvertiseData()
        val scanRespData = data.scanResponseData.getAdvertiseData()

        val duration = if (data.limitType == LimitType.NO_LIMIT) 0 else (ceil(data.timeLimit / 10.0).toInt())
        val events = if (data.limitType == LimitType.EVENT_LIMIT) data.eventLimit else 0

        try {
            if (data.isLegacy) {
                if (data.mode == AdvertisingMode.NON_CONNECTABLE_NON_SCANNABLE) {
                    advertiser.startAdvertisingSet(settings, advData, null, null, null, duration, events, callback)
                } else {
                    advertiser.startAdvertisingSet(settings, advData, scanRespData, null, null, duration, events, callback)
                }
            } else {
                if (data.mode == AdvertisingMode.NON_CONNECTABLE_SCANNABLE) {
                    advertiser.startAdvertisingSet(settings, null, scanRespData, null, null, duration, events, callback)
                } else {
                    advertiser.startAdvertisingSet(settings, advData, null, null, null, duration, events, callback)
                }
            }
        } catch (e: IllegalArgumentException) {
            errorCallback.onErrorHandled(e.message.toString())
        } catch (e: SecurityException) {
            errorCallback.onErrorHandled(e.message.toString())
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        Timber.d("Advertiser stop")
        isRunning = false

        val advertiser = BluetoothAdapter.getDefaultAdapter().bluetoothLeAdvertiser
        if (this::callback.isInitialized) advertiser?.stopAdvertisingSet(callback)
    }

    fun deepCopy(): Advertiser {
        val dataCopy = Gson().toJson(data)
        return Advertiser(Gson().fromJson(dataCopy, AdvertiserData::class.java))
    }

    interface ErrorCallback {
        fun onErrorHandled(message: String)
    }
}