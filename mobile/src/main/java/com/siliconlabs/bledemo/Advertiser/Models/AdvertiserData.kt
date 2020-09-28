package com.siliconlabs.bledemo.Advertiser.Models

import android.bluetooth.le.AdvertiseSettings
import android.os.Parcelable
import com.siliconlabs.bledemo.Advertiser.Enums.AdvertisingMode
import com.siliconlabs.bledemo.Advertiser.Enums.LimitType
import kotlinx.android.parcel.Parcelize

@Parcelize
class AdvertiserData(
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

    fun includesCompleteLocalName(): Boolean {
        return advertisingData.includeCompleteLocalName
    }

    fun getAdvertisingTime(): Long {
        return when (limitType) {
            LimitType.NO_LIMIT -> 0
            LimitType.TIME_LIMIT -> timeLimit.toLong()
            LimitType.EVENT_LIMIT -> (eventLimit * advertisingIntervalMs).toLong()
        }
    }

    fun setEffectiveTxPowerLowApi(value: Int?) {
        txPower = when (value) {
            AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW -> -21
            AdvertiseSettings.ADVERTISE_TX_POWER_LOW -> -15
            AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM -> -7
            else -> 1
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
}