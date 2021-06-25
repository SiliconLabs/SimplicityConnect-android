package com.siliconlabs.bledemo.Advertiser.Models

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSetParameters
import android.os.Build
import androidx.annotation.RequiresApi
import com.siliconlabs.bledemo.Advertiser.Enums.AdvertisingMode
import com.siliconlabs.bledemo.Advertiser.Enums.LimitType
import com.siliconlabs.bledemo.Advertiser.Enums.Phy

class AdvertiserSettings(val data: AdvertiserData) {

    @RequiresApi(Build.VERSION_CODES.O)
    fun getAdvertisingSetParameters(): AdvertisingSetParameters {
        return AdvertisingSetParameters.Builder().apply {

            setConnectable(data.mode.isConnectable())
            setScannable(data.mode.isScannable())
            setInterval((data.advertisingIntervalMs / 0.625).toInt())
            setTxPowerLevel(data.txPower)
            setLegacyMode(data.isLegacy)

            if (data.isExtended()) {
                when (data.settings.primaryPhy) {
                    Phy.PHY_1M -> setPrimaryPhy(BluetoothDevice.PHY_LE_1M)
                    else -> setPrimaryPhy(BluetoothDevice.PHY_LE_CODED)
                }

                when (data.settings.secondaryPhy) {
                    Phy.PHY_1M -> setSecondaryPhy(BluetoothDevice.PHY_LE_1M)
                    Phy.PHY_2M -> setSecondaryPhy(BluetoothDevice.PHY_LE_2M)
                    else -> setSecondaryPhy(BluetoothDevice.PHY_LE_CODED)
                }

                if (data.mode == AdvertisingMode.NON_CONNECTABLE_NON_SCANNABLE) setAnonymous(data.settings.anonymous)
                setIncludeTxPower(data.settings.includeTxPower)
            }
        }.build()
    }

    fun getAdvertiseSettings(): AdvertiseSettings {
        return AdvertiseSettings.Builder().apply {
            when (data.advertisingIntervalMs) {
                100 -> setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                250 -> setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                1000 -> setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            }

            setConnectable(data.mode.isConnectable())
            setTimeout(if (data.limitType == LimitType.NO_LIMIT) 0 else data.timeLimit)

            when (data.txPower) {
                -21 -> setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW)
                -15 -> setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                -7 -> setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                1 -> setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            }
        }.build()
    }

}