package com.siliconlabs.bledemo.features.configure.advertiser.models

import android.os.Parcelable
import com.google.gson.Gson
import com.siliconlabs.bledemo.features.configure.advertiser.enums.AdvertisingMode
import com.siliconlabs.bledemo.features.configure.advertiser.enums.LimitType
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AdvertiserData(
        var name: String = "New adv",
        var isLegacy: Boolean = true,
        var mode: AdvertisingMode = AdvertisingMode.CONNECTABLE_SCANNABLE,
        var settings: ExtendedSettings = ExtendedSettings(),
        val advertisingData: DataPacket = DataPacket(),
        val scanResponseData: DataPacket = DataPacket(),
        var advertisingIntervalMs: Int = 250,
        var txPower: Int = -7,
        var limitType: LimitType = LimitType.NO_LIMIT,
        var timeLimit: Int = 10000,
        var eventLimit: Int = 20) : Parcelable {

    fun getAdvertisingTime(): Long {
        return when (limitType) {
            LimitType.NO_LIMIT -> 0
            LimitType.TIME_LIMIT -> timeLimit.toLong()
            LimitType.EVENT_LIMIT -> (eventLimit * advertisingIntervalMs).toLong()
        }
    }

    fun isExtended(): Boolean {
        return !isLegacy
    }

    fun isAdvertisingData(): Boolean {
        return !(mode == AdvertisingMode.NON_CONNECTABLE_SCANNABLE && isExtended())
    }

    fun isScanRespData(): Boolean {
        return mode == AdvertisingMode.NON_CONNECTABLE_SCANNABLE || mode == AdvertisingMode.CONNECTABLE_SCANNABLE
    }

    fun deepCopy(): AdvertiserData {
        val dataCopy = Gson().toJson(this)
        return Gson().fromJson(dataCopy, AdvertiserData::class.java)
    }
}