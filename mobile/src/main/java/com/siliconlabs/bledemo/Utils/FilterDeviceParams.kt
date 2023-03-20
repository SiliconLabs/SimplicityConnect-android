package com.siliconlabs.bledemo.utils

import android.content.Context
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.bluetooth.beacon_utils.BleFormat
import kotlinx.android.synthetic.main.fragment_filter.*

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

    fun buildDescription(context: Context?) : String? {
        if (context == null) return null

        val description = StringBuilder().apply {
            if (name?.isNotBlank() == true) append("\"$name\", ")
            if (isRssiFlag) append("> $rssiValue dBm, ")
            if (bleFormats.isNotEmpty()) {
                bleFormats.forEach { append(context.getString(it.nameResId)).append(", ") }
            }
            if (isOnlyFavourite) {
                append(context.getString(R.string.only_favorites)).append(", ")
            }
            if (isOnlyConnectable) {
                append(context.getString(R.string.only_connectible)).append(", ")
            }
            if (isOnlyBonded) {
                append(context.getString(R.string.only_bonded)).append(", ")
            }
        }.toString()

        return if (description.isEmpty()) null else description.substring(0, description.count() - 2)
    }

}
