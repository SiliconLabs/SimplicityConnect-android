package com.siliconlabs.bledemo.Utils

import java.util.*

object UuidConsts {
    val OTA_SERVICE: UUID = UUID.fromString("1d14d6ee-fd63-4fa1-bfa4-8f47b42119f0")
    val OTA_CONTROL: UUID = UUID.fromString("f7bf3564-fb6d-4e53-88a4-5e37e0326063")
    val OTA_DATA: UUID = UUID.fromString("984227f3-34fc-4045-a5d0-2c581f81a153")
    val APP_LOADER_VERSION: UUID = UUID.fromString("4f4a2368-8cca-451e-bfff-cf0e2ee23e9f")
    val OTA_VERSION: UUID = UUID.fromString("4cc07bcf-0868-4b32-9dad-ba4cc41e5316")
    val GECKO_BOOTLOADER_VERSION: UUID = UUID.fromString("25f05c0a-e917-46e9-b2a5-aa2be1245afe")
    val APPLICATION_VERSION: UUID = UUID.fromString("0d77cc11-4ac1-49f2-bfa9-cd96ac7a92f8")

    val HOMEKIT_SERVICE: UUID = UUID.fromString("0000003e-0000-1000-8000-0026bb765291")
    val HOMEKIT_DESCRIPTOR: UUID = UUID.fromString("dc46f0fe-81d2-4616-b5d9-6abdd796939a")
}
