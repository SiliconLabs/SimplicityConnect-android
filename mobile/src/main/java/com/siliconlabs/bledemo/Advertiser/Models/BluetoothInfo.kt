package com.siliconlabs.bledemo.advertiser.models

import android.bluetooth.BluetoothAdapter
import android.os.Build
import com.siliconlabs.bledemo.advertiser.enums.AdvertisingMode
import com.siliconlabs.bledemo.advertiser.enums.Phy

class BluetoothInfo {

    fun isExtendedAdvertisingSupported(): Boolean {
        return if (Build.VERSION.SDK_INT >= 26) BluetoothAdapter.getDefaultAdapter().isLeExtendedAdvertisingSupported
        else false
    }

    fun isLe2MPhySupported(): Boolean {
        return if (Build.VERSION.SDK_INT >= 26) BluetoothAdapter.getDefaultAdapter().isLe2MPhySupported
        else false
    }

    fun isLeCodedPhySupported(): Boolean {
        return if (Build.VERSION.SDK_INT >= 26) BluetoothAdapter.getDefaultAdapter().isLeCodedPhySupported
        else false
    }

    fun getLeMaximumAdvertisingDataLength(): Int {
        return if (Build.VERSION.SDK_INT >= 26) BluetoothAdapter.getDefaultAdapter().leMaximumAdvertisingDataLength
        else -1
    }

    fun getSupportedLegacyAdvertisingModes(): ArrayList<AdvertisingMode> {
        val list = ArrayList<AdvertisingMode>()
        if (Build.VERSION.SDK_INT < 26) {
            list.add(AdvertisingMode.CONNECTABLE_SCANNABLE)
            list.add(AdvertisingMode.NON_CONNECTABLE_SCANNABLE)
        } else {
            list.add(AdvertisingMode.CONNECTABLE_SCANNABLE)
            list.add(AdvertisingMode.NON_CONNECTABLE_SCANNABLE)
            list.add(AdvertisingMode.NON_CONNECTABLE_NON_SCANNABLE)
        }

        return list
    }

    fun getSupportedExtendedAdvertisingModes(isAdvertisingExtensionSupported: Boolean): ArrayList<AdvertisingMode> {
        val list = ArrayList<AdvertisingMode>()
        if (Build.VERSION.SDK_INT >= 26 && isAdvertisingExtensionSupported) {
            list.add(AdvertisingMode.CONNECTABLE_NON_SCANNABLE)
            list.add(AdvertisingMode.NON_CONNECTABLE_SCANNABLE)
            list.add(AdvertisingMode.NON_CONNECTABLE_NON_SCANNABLE)
        }
        return list
    }

    fun getSupportedPrimaryPhys(isLeCodedPhySupported: Boolean): ArrayList<Phy> {
        val list = ArrayList<Phy>()
        list.add(Phy.PHY_1M)
        if (isLeCodedPhySupported) list.add(Phy.PHY_LE_CODED)

        return list
    }

    fun getSupportedSecondaryPhys(isLe2MPhySupported: Boolean, isLeCodedPhySupported: Boolean): ArrayList<Phy> {
        val list = ArrayList<Phy>()
        list.add(Phy.PHY_1M)
        if (isLe2MPhySupported) list.add(Phy.PHY_2M)
        if (isLeCodedPhySupported) list.add(Phy.PHY_LE_CODED)

        return list
    }

    fun isTxPowerWholeRangeSupported(): Boolean {
        return Build.VERSION.SDK_INT >= 26
    }

    fun isAdvertisingIntervalWholeRangeSupported(): Boolean {
        return Build.VERSION.SDK_INT >= 26
    }

    fun isExtendedTimeLimitSupported(): Boolean {
        return Build.VERSION.SDK_INT >= 26
    }

}