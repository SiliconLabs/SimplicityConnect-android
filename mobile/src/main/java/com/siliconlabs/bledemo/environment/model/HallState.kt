package com.siliconlabs.bledemo.environment.model

import androidx.annotation.IntDef

@Retention(AnnotationRetention.SOURCE)
@IntDef(HallState.CLOSED, HallState.OPENED, HallState.TAMPERED)
annotation class HallState {
    companion object {
        const val CLOSED = 0
        const val OPENED = 1
        const val TAMPERED = 2
    }
}