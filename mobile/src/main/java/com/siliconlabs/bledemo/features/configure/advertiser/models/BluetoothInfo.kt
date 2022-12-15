package com.siliconlabs.bledemo.features.configure.advertiser.models

import android.bluetooth.BluetoothAdapter
import com.siliconlabs.bledemo.features.configure.advertiser.enums.AdvertisingMode
import com.siliconlabs.bledemo.features.configure.advertiser.enums.Phy

class BluetoothInfo {

    fun isExtendedAdvertisingSupported(): Boolean {
        return BluetoothAdapter.getDefaultAdapter().isLeExtendedAdvertisingSupported
    }

    fun isLe2MPhySupported(): Boolean {
        return BluetoothAdapter.getDefaultAdapter().isLe2MPhySupported
    }

    fun isLeCodedPhySupported(): Boolean {
        return BluetoothAdapter.getDefaultAdapter().isLeCodedPhySupported
    }

    fun getLeMaximumAdvertisingDataLength(): Int {
        return BluetoothAdapter.getDefaultAdapter().leMaximumAdvertisingDataLength
    }

    fun getSupportedLegacyAdvertisingModes(): ArrayList<AdvertisingMode> {
        return arrayListOf(
                AdvertisingMode.CONNECTABLE_SCANNABLE,
                AdvertisingMode.NON_CONNECTABLE_SCANNABLE,
                AdvertisingMode.NON_CONNECTABLE_NON_SCANNABLE
        )
    }

    fun getSupportedExtendedAdvertisingModes(isAdvertisingExtensionSupported: Boolean): ArrayList<AdvertisingMode> {
        val list = ArrayList<AdvertisingMode>()
        if (isAdvertisingExtensionSupported) {
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

}