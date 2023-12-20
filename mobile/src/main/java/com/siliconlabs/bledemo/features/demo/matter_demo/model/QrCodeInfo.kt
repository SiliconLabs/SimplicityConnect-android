package com.siliconlabs.bledemo.features.demo.matter_demo.model

import android.os.Parcelable
import chip.setuppayload.OptionalQRCodeInfo
import kotlinx.android.parcel.Parcelize

@Parcelize
data class QrCodeInfo(
    val tag: Int,
    val type: OptionalQRCodeInfo.OptionalQRCodeInfoType,
    val data: String,
    val intDataValue: Int
):Parcelable
