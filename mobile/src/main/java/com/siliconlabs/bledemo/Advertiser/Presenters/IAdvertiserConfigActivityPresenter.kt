package com.siliconlabs.bledemo.Advertiser.Presenters

import com.siliconlabs.bledemo.Advertiser.Enums.AdvertisingMode
import com.siliconlabs.bledemo.Advertiser.Enums.DataMode
import com.siliconlabs.bledemo.Advertiser.Enums.DataType
import com.siliconlabs.bledemo.Advertiser.Enums.LimitType
import com.siliconlabs.bledemo.Advertiser.Models.*

interface IAdvertiserConfigActivityPresenter {
    fun prepareAdvertisingTypes()
    fun preparePhyParameters()
    fun prepareAdvertisingInterval()
    fun prepareTxPower()

    fun include16BitService(mode: DataMode, service: Service16Bit)
    fun include128BitService(mode: DataMode, service: Service128Bit)
    fun includeCompleteLocalName(mode: DataMode)
    fun includeTxPower(mode: DataMode)
    fun includeManufacturerSpecificData(mode: DataMode, manufacturer: Manufacturer)

    fun exclude16BitService(mode: DataMode, service: Service16Bit)
    fun exclude128BitService(mode: DataMode, service: Service128Bit)
    fun excludeServices(mode: DataMode, type: DataType)
    fun excludeCompleteLocalName(mode: DataMode)
    fun excludeTxPower(mode: DataMode)
    fun excludeManufacturerSpecificData(mode: DataMode, manufacturer: Manufacturer)

    fun setAdvertisingName(name: String)
    fun setAdvertisingType(isLegacy: Boolean, mode: AdvertisingMode)
    fun setAdvertisingParams(settings: ExtendedSettings, interval: Int, txPower: Int)
    fun setAdvertisingLimit(limitType: LimitType, timeLimit: Int, eventLimit: Int)
    fun setSupportedData(isLegacy: Boolean, mode: AdvertisingMode)

    fun onItemReceived(data: AdvertiserData, isAdvertisingExtensionSupported: Boolean)
    fun loadData(mode: DataMode)
    fun handleSave()
}