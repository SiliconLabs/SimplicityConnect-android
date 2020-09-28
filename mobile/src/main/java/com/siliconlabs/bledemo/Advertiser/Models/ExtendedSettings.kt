package com.siliconlabs.bledemo.Advertiser.Models

import android.os.Parcelable
import com.siliconlabs.bledemo.Advertiser.Enums.Phy
import kotlinx.android.parcel.Parcelize

@Parcelize
class ExtendedSettings(val includeTxPower: Boolean = false, val anonymous: Boolean = false, val primaryPhy: Phy? = null, val secondaryPhy: Phy? = null) : Parcelable {

}