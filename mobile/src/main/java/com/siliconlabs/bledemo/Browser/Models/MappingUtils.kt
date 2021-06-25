package com.siliconlabs.bledemo.browser.models

import androidx.fragment.app.Fragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.utils.UuidConsts
import java.util.*


fun Fragment.getOtaSpecificCharacteristicName(uuid: String): String {
    return when (UUID.fromString(uuid)) {
        UuidConsts.OTA_CONTROL -> "OTA Control Attribute"
        UuidConsts.OTA_DATA -> "OTA Data Attribute"
        UuidConsts.APP_LOADER_VERSION -> "AppLoader version"
        UuidConsts.OTA_VERSION -> "OTA version"
        UuidConsts.GECKO_BOOTLOADER_VERSION -> "Gecko Bootloader version"
        UuidConsts.APPLICATION_VERSION -> "Application version"
        else -> getString(R.string.unknown_characteristic_label)
    }
}
