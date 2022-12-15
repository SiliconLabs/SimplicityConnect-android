package com.siliconlabs.bledemo.utils

import com.siliconlabs.bledemo.bluetooth.beacon_utils.BleFormat

data class FilterDeviceParams(
        val name: String?,
        val rssiValue: Int,
        val isRssiFlag: Boolean,
        val bleFormats: List<BleFormat>,
        val isOnlyFavourite: Boolean,
        val isOnlyConnectable: Boolean,
        val isOnlyBonded: Boolean
) {

    val isEmpty: Boolean
        get() = name == null
                && !isRssiFlag
                && !isOnlyFavourite
                && !isOnlyConnectable
                && !isOnlyBonded
                && bleFormats.isEmpty()

}
