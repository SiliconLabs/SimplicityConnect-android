package com.siliconlabs.bledemo.utils

import java.util.*

object UuidConsts {
    val OTA_SERVICE: UUID = UUID.fromString("1d14d6ee-fd63-4fa1-bfa4-8f47b42119f0")
    val OTA_CONTROL: UUID = UUID.fromString("f7bf3564-fb6d-4e53-88a4-5e37e0326063")
    val OTA_DATA: UUID = UUID.fromString("984227f3-34fc-4045-a5d0-2c581f81a153")

    val HOMEKIT_SERVICE: UUID = UUID.fromString("0000003e-0000-1000-8000-0026bb765291")
    val HOMEKIT_DESCRIPTOR: UUID = UUID.fromString("dc46f0fe-81d2-4616-b5d9-6abdd796939a")

    val CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
}
