package com.siliconlabs.bledemo.advertiser.activities

import com.siliconlabs.bledemo.advertiser.enums.AdvertisingMode
import com.siliconlabs.bledemo.advertiser.enums.DataMode
import com.siliconlabs.bledemo.advertiser.enums.Phy
import com.siliconlabs.bledemo.advertiser.models.AdvertiserData
import com.siliconlabs.bledemo.advertiser.models.DataPacket

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