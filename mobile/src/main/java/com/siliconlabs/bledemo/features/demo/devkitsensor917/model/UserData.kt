package com.siliconlabs.bledemo.features.demo.devkitsensor917.model

import com.google.gson.annotations.SerializedName

data class UserData(
    @SerializedName("red") val red: String?,
    @SerializedName("green") val green: String?,
    @SerializedName("blue") val blue: String?
)
