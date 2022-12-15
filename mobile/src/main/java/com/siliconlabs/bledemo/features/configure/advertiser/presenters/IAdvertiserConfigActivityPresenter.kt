package com.siliconlabs.bledemo.features.configure.advertiser.presenters

import com.siliconlabs.bledemo.features.configure.advertiser.enums.AdvertisingMode
import com.siliconlabs.bledemo.features.configure.advertiser.enums.DataMode
import com.siliconlabs.bledemo.features.configure.advertiser.enums.DataType
import com.siliconlabs.bledemo.features.configure.advertiser.enums.LimitType
import com.siliconlabs.bledemo.features.configure.advertiser.models.*

interface IAdvertiserConfigActivityPresenter {
    fun prepareAdvertisingTypes()
    fun preparePhyParameters()

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