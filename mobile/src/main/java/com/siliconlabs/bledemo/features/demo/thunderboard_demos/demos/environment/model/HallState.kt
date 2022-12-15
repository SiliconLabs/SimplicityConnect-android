package com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.model

enum class HallState(val value: Int) {
    CLOSED(0),
    OPENED(1),
    TAMPERED(2);

    companion object {
        fun fromValue(value: Int) : HallState? {
            return when (value) {
                0 -> CLOSED
                1 -> OPENED
                2 -> TAMPERED
                else -> null
            }
        }
    }
}