package com.siliconlabs.bledemo.Advertiser.Activities

import com.siliconlabs.bledemo.Advertiser.Enums.AdvertisingMode
import com.siliconlabs.bledemo.Advertiser.Enums.DataMode
import com.siliconlabs.bledemo.Advertiser.Enums.Phy
import com.siliconlabs.bledemo.Advertiser.Models.*

interface IAdvertiserConfigActivityView {
    fun onAdvertisingTypesPrepared(isLegacy: Boolean, legacyModes: List<AdvertisingMode>, extendedModes: List<AdvertisingMode>)
    fun onAdvertisingParametersPrepared(isLegacy: Boolean, primaryPhys: List<Phy>, secondaryPhys: List<Phy>)
    fun onAdvertisingIntervalPrepared(isWholeRange: Boolean)
    fun onTxPowerPrepared(isWholeRange: Boolean)
    fun onSupportedDataPrepared(isAdvertisingData: Boolean, isScanRespData: Boolean)
    fun onSaveHandled(isExtendedTimeLimitSupported: Boolean)
    fun populateUi(data: AdvertiserData, isIntervalWholeRange: Boolean, isTxPowerWholeRange: Boolean, isExtendedTimeLimitSupported: Boolean, isAdvertisingEventSupported: Boolean)
    fun onDataLoaded(data: DataPacket?, mode: DataMode)
    fun updateAvailableBytes(advDataBytes: Int, scanRespDataBytes: Int, maxPacketSize: Int, includeFlags: Boolean)
}