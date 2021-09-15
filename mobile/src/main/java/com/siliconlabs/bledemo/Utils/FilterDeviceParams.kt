package com.siliconlabs.bledemo.utils

import android.content.Context
import com.siliconlabs.bledemo.BeaconUtils.BleFormat
import com.siliconlabs.bledemo.utils.StringUtils.removeWhitespaceAndCommaIfNeeded
import java.lang.StringBuilder

class FilterDeviceParams(var filterName: String, var name: String?, var advertising: String?, var rssiValue: Int, var isRssiFlag: Boolean,
                         var bleFormats: List<BleFormat>?, var isOnlyFavourite: Boolean, var isOnlyConnectable: Boolean, var isOnlyBonded: Boolean) {

    val isEmptyFilter: Boolean
        get() = ((name == null || name == "")
                && (advertising == null || advertising == "")
                && !isRssiFlag
                && !isOnlyFavourite
                && !isOnlyConnectable
                && !isOnlyBonded
                && (bleFormats == null || bleFormats?.isEmpty()!!))

    fun getActiveFilterText(context: Context): String {
        val sb = StringBuilder()

        name?.let { name ->
            if (name.isNotEmpty()) {
                appendFilterData(sb, name)
            }
        }

        advertising?.let { advData ->
            if (advData.isNotEmpty()) {
                appendFilterData(sb, "0x".plus(advData))
            }
        }

        if (isRssiFlag) {
            appendFilterData(sb, "> ".plus(rssiValue).plus("dBm"))
        }

        bleFormats?.let { beacons ->
            if (beacons.isNotEmpty()) {
                appendFilterData(sb, getBeacons(beacons, context))
            }
        }

        if (isOnlyFavourite) {
            appendFilterData(sb, "favourites")
        }

        if (isOnlyConnectable) {
            appendFilterData(sb, "connectable")
        }

        if (isOnlyBonded) {
            appendFilterData(sb, "bonded")
        }

        removeWhitespaceAndCommaIfNeeded(sb)
        return sb.toString()
    }

    private fun appendFilterData(sb: StringBuilder, text: String) {
        sb.append(text).append(", ")
    }

    private fun getBeacons(bleFormats: List<BleFormat>, context: Context): String {
        val sb = StringBuilder()
        for (beacon in bleFormats) {
            sb.append(context.resources.getText(beacon.nameResId)).append(", ")
        }

        removeWhitespaceAndCommaIfNeeded(sb)
        return sb.toString()
    }


}
