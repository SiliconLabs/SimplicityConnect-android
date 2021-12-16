package com.siliconlabs.bledemo.motion.model

import com.siliconlabs.bledemo.thunderboard.model.ThunderBoardDevice
import java.util.*

class MotionEvent {
    val device: ThunderBoardDevice?
    val characteristicUuid: UUID
    val action: Int?

    constructor(device: ThunderBoardDevice?, characteristicUuid: UUID) {
        this.device = device
        this.characteristicUuid = characteristicUuid
        action = null
    }

    constructor(device: ThunderBoardDevice?, characteristicUuid: UUID, action: Int) {
        this.device = device
        this.characteristicUuid = characteristicUuid
        this.action = action
    }

    companion object {
        const val ACTION_CALIBRATE = 1
        const val ACTION_CLEAR_ORIENTATION = 2
    }
}