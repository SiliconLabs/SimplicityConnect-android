package com.siliconlabs.bledemo.features.demo.matter_demo.model

import android.os.Parcelable
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.model.TemperatureScale
import kotlinx.android.parcel.Parcelize
import java.util.Locale

@Parcelize
data class MatterScannedResultModel(
    var matterName: String,
    var matterAddress: String,
    var matterType: Int,
    var deviceId: Long,
    var deviceType: Int,
    var isDeviceOnline: Boolean=false,
    var isLightAndLightSwitchItemSelected: Boolean = false,
    var isBindingInProgress:Boolean = false,
    var isBindingSuccessful:Boolean = false,
    var isAclWriteInProgress: Boolean = false,
    var isUnbindingInProgress: Boolean = false

    // val isDeleteIconVisible:Boolean
) : Parcelable {

/*    fun setDeleteIconVisible(b: Boolean) {

    }
    fun isDeleteIconVisible(): Boolean {
        return isDeleteIconVisible
    }*/

}




