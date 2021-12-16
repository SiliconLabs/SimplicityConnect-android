package com.siliconlabs.bledemo.thunderboard.model

import java.util.*

class NotificationEvent(val device: ThunderBoardDevice, val characteristicUuid: UUID,
                        val action: Int) {
    companion object {
        const val ACTION_NOTIFICATIONS_CLEAR = 1
        const val ACTION_NOTIFICATIONS_SET = 2
    }
}