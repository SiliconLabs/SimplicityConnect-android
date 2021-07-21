package com.siliconlabs.bledemo.Advertiser.Presenters

import android.os.Build
import com.siliconlabs.bledemo.Advertiser.Activities.IAdvertiserConfigActivityView
import com.siliconlabs.bledemo.Advertiser.Enums.AdvertisingMode
import com.siliconlabs.bledemo.Advertiser.Enums.DataMode
import com.siliconlabs.bledemo.Advertiser.Enums.DataType
import com.siliconlabs.bledemo.Advertiser.Enums.LimitType
import com.siliconlabs.bledemo.Advertiser.Utils.AdvertiserStorage
import com.siliconlabs.bledemo.Advertiser.Models.*

class AdvertiserConfigActivityPresenter(private val view: IAdvertiserConfigActivityView, private val storage: AdvertiserStorage) : IAdvertiserConfigActivityPresenter {
    private lateinit var data: AdvertiserData
    private val bluetoothInfo = BluetoothInfo()

    override fun prepareAdvertisingTypes() {
        val isLegacy = !storage.isAdvertisingExtensionSupported()
        val legacyModes = bluetoothInfo.getSupportedLegacyAdvertisingModes()
        val extendedModes = bluetoothInfo.getSupportedExtendedAdvertisingModes(storage.isAdvertisingExtensionSupported())

        view.onAdvertisingTypesPrepared(isLegacy, legacyModes, extendedModes)
    }

    override fun preparePhyParameters() {
        val isLegacy = !storage.isAdvertisingExtensionSupported()
        val primaryPhys = bluetoothInfo.getSupportedPrimaryPhys(storage.isLeCodedPhySupported())
        val secondaryPhys = bluetoothInfo.getSupportedSecondaryPhys(storage.isLe2MPhySupported(), storage.isLeCodedPhySupported())

        view.onAdvertisingParametersPrepared(isLegacy, primaryPhys, secondaryPhys)
    }

    override fun prepareAdvertisingInterval() {
        view.onAdvertisingIntervalPrepared(bluetoothInfo.isAdvertisingIntervalWholeRangeSupported())
    }

    override fun prepareTxPower() {
        view.onTxPowerPrepared(bluetoothInfo.isTxPowerWholeRangeSupported())
    }

    override fun include16BitService(mode: DataMode, service: Service16Bit) {
        if (mode == DataMode.ADVERTISING_DATA) data.advertisingData.services16Bit.add(service)
        else data.scanResponseData.services16Bit.add(service)
        updateAvailableBytes()
    }

    override fun include128BitService(mode: DataMode, service: Service128Bit) {
        if (mode == DataMode.ADVERTISING_DATA) data.advertisingData.services128Bit.add(service)
        else data.scanResponseData.services128Bit.add(service)
        updateAvailableBytes()
    }

    override fun includeCompleteLocalName(mode: DataMode) {
        if (mode == DataMode.ADVERTISING_DATA) data.advertisingData.includeCompleteLocalName = true
        else data.scanResponseData.includeCompleteLocalName = true
        updateAvailableBytes()
    }

    override fun includeTxPower(mode: DataMode) {
        if (mode == DataMode.ADVERTISING_DATA) data.advertisingData.includeTxPower = true
        else data.scanResponseData.includeTxPower = true
        updateAvailableBytes()
    }

    override fun includeManufacturerSpecificData(mode: DataMode, manufacturer: Manufacturer) {
        if (mode == DataMode.ADVERTISING_DATA) data.advertisingData.manufacturers.add(manufacturer)
        else data.scanResponseData.manufacturers.add(manufacturer)
        updateAvailableBytes()
    }

    override fun setAdvertisingName(name: String) {
        data.name = name
    }

    override fun setAdvertisingType(isLegacy: Boolean, mode: AdvertisingMode) {
        data.mode = mode
        data.isLegacy = isLegacy
        updateAvailableBytes()
    }

    override fun setAdvertisingParams(settings: ExtendedSettings, interval: Int, txPower: Int) {
        data.settings = settings
        data.txPower = txPower
        data.advertisingIntervalMs = interval
    }

    override fun setAdvertisingLimit(limitType: LimitType, timeLimit: Int, eventLimit: Int) {
        data.limitType = limitType
        if (timeLimit != -1) data.timeLimit = timeLimit
        else if (eventLimit != -1) data.eventLimit = eventLimit
    }

    override fun setSupportedData(isLegacy: Boolean, mode: AdvertisingMode) {
        data.isLegacy = isLegacy
        data.mode = mode

        updateAvailableBytes()
        view.onSupportedDataPrepared(data.isAdvertisingData(), data.isScanRespData())
    }

    override fun exclude16BitService(mode: DataMode, service: Service16Bit) {
        if (mode == DataMode.ADVERTISING_DATA) data.advertisingData.services16Bit.remove(service)
        else data.scanResponseData.services16Bit.remove(service)
        updateAvailableBytes()
    }

    override fun exclude128BitService(mode: DataMode, service: Service128Bit) {
        if (mode == DataMode.ADVERTISING_DATA) data.advertisingData.services128Bit.remove(service)
        else data.scanResponseData.services128Bit.remove(service)
        updateAvailableBytes()
    }

    override fun excludeServices(mode: DataMode, type: DataType) {
        if (mode == DataMode.ADVERTISING_DATA) {
            if (type == DataType.COMPLETE_16_BIT) data.advertisingData.services16Bit.clear()
            if (type == DataType.COMPLETE_128_BIT) data.advertisingData.services128Bit.clear()
        } else {
            if (type == DataType.COMPLETE_16_BIT) data.scanResponseData.services16Bit.clear()
            if (type == DataType.COMPLETE_128_BIT) data.scanResponseData.services128Bit.clear()
        }
        updateAvailableBytes()
    }

    override fun excludeCompleteLocalName(mode: DataMode) {
        if (mode == DataMode.ADVERTISING_DATA) data.advertisingData.includeCompleteLocalName = false
        else data.scanResponseData.includeCompleteLocalName = false
        updateAvailableBytes()
    }

    override fun excludeTxPower(mode: DataMode) {
        if (mode == DataMode.ADVERTISING_DATA) data.advertisingData.includeTxPower = false
        else data.scanResponseData.includeTxPower = false
        updateAvailableBytes()
    }

    override fun excludeManufacturerSpecificData(mode: DataMode, manufacturer: Manufacturer) {
        if (mode == DataMode.ADVERTISING_DATA) data.advertisingData.manufacturers.remove(manufacturer)
        else data.scanResponseData.manufacturers.remove(manufacturer)
        updateAvailableBytes()
    }

    override fun loadData(mode: DataMode) {
        if (mode == DataMode.ADVERTISING_DATA) view.onDataLoaded(data.advertisingData, mode)
        else view.onDataLoaded(data.scanResponseData, mode)
        updateAvailableBytes()
    }

    override fun handleSave() {
        val isExtendedTimeLimit = bluetoothInfo.isExtendedTimeLimitSupported()
        view.onSaveHandled(isExtendedTimeLimit)
    }

    override fun onItemReceived(data: AdvertiserData, isAdvertisingExtensionSupported: Boolean) {
        this.data = data
        val isIntervalWholeRange = bluetoothInfo.isAdvertisingIntervalWholeRangeSupported()
        val isTxPowerWholeRange = bluetoothInfo.isTxPowerWholeRangeSupported()
        val isExtendedTimeLimit = bluetoothInfo.isExtendedTimeLimitSupported()
        val isAdvertisingEventSupported = isAdvertisingExtensionSupported

        view.populateUi(data, isIntervalWholeRange, isTxPowerWholeRange, isExtendedTimeLimit, isAdvertisingEventSupported)
        view.onSupportedDataPrepared(data.isAdvertisingData(), data.isScanRespData())
    }

    private fun updateAvailableBytes() {
        val includeFlags = data.mode.isConnectable()
        val maxPacketSize = if (data.isLegacy) DataPacket.LEGACY_BYTES_LIMIT else storage.getLeMaximumDataLength()
        val advDataBytes = data.advertisingData.getAvailableBytes(includeFlags, maxPacketSize)
        val scanResponseBytes = data.scanResponseData.getAvailableBytes(false, maxPacketSize)
        view.updateAvailableBytes(advDataBytes, scanResponseBytes, maxPacketSize, includeFlags)
    }

    fun getManufacturers(mode: DataMode): ArrayList<Manufacturer> {
        return if (mode == DataMode.ADVERTISING_DATA) data.advertisingData.manufacturers else data.scanResponseData.manufacturers
    }

    fun singleManufacturerSupported(): Boolean {
        return Build.VERSION.SDK_INT < 26
    }
}