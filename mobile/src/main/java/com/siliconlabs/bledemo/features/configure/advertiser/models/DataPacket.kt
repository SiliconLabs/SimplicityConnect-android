package com.siliconlabs.bledemo.features.configure.advertiser.models

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseData
import android.os.ParcelUuid
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class DataPacket(
        val services16Bit: ArrayList<Service16Bit> = ArrayList(),
        val services128Bit: ArrayList<Service128Bit> = ArrayList(),
        var includeCompleteLocalName: Boolean = false,
        var includeTxPower: Boolean = false,
        val manufacturers: ArrayList<Manufacturer> = ArrayList()) : Parcelable {

    companion object {
        const val BASE = 2
        const val FLAGS = 1
        const val TX_POWER = 2
        const val SERVICE_16_BIT = 2
        const val SERVICE_128_BIT = 16
        const val MANUFACTURER_ID = 2
        const val LEGACY_BYTES_LIMIT = 31
    }

    private fun getFlagsSize(include: Boolean): Int {
        return if (include) BASE + FLAGS
        else 0
    }

    private fun get16BitServicesSize(): Int {
        if (services16Bit.size > 0) return BASE + services16Bit.size * SERVICE_16_BIT
        return 0
    }

    private fun get128BitServicesSize(): Int {
        if (services128Bit.size > 0) return BASE + services128Bit.size * SERVICE_128_BIT
        return 0
    }

    private fun getCompleteLocalNameSize(): Int {
        if (includeCompleteLocalName) return BASE + BluetoothAdapter.getDefaultAdapter().name.length
        return 0
    }

    private fun getTxPowerSize(): Int {
        if (includeTxPower) return BASE + TX_POWER
        return 0
    }

    private fun getManufacturerDataSize(): Int {
        var size = 0
        for (manufacturer in manufacturers) size += BASE + MANUFACTURER_ID + manufacturer.data.size

        return size
    }

    fun getAvailableBytes(includeFlags: Boolean, maxPacketSize: Int): Int {
        var availableBytes = maxPacketSize
        availableBytes -= getFlagsSize(includeFlags)
        availableBytes -= get16BitServicesSize()
        availableBytes -= get128BitServicesSize()
        availableBytes -= getCompleteLocalNameSize()
        availableBytes -= getTxPowerSize()
        availableBytes -= getManufacturerDataSize()
        return availableBytes
    }

    fun getAdvertiseData(): AdvertiseData {
        return AdvertiseData.Builder().apply {
            for (manufacturer in manufacturers)
                addManufacturerData(manufacturer.identifier, manufacturer.data)
            for (service16bit in services16Bit)
                addServiceUuid(ParcelUuid(service16bit.getUUID()))
            for (service128bit in services128Bit)
                addServiceUuid(ParcelUuid(service128bit.uuid))

            setIncludeDeviceName(includeCompleteLocalName)
            setIncludeTxPowerLevel(includeTxPower)

        }.build()
    }
}