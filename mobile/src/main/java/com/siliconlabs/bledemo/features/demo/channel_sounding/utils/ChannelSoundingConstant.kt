package com.siliconlabs.bledemo.features.demo.channel_sounding.utils

import java.util.UUID

object ChannelSoundingConstant {

    enum class GattState {
        DISCONNECTED,
        SCANNING,
        CONNECTED,
    }

    enum class RangeSessionState {
        STARTING,
        STARTED,
        STOPPING,
        STOPPED,
    }

    val OOB_SERVICE: UUID = UUID.fromString("f81d4fae-7ccc-eeee-a765-aaaaaaaaaaaa")

    val OOB_PSM_CHARACTERISTICS: UUID = UUID.fromString("f81d4fae-7ccc-eeee-a765-0aaaaaaaaaaa")


}