package com.siliconlabs.bledemo.features.configure.advertiser.enums

enum class LimitType {
    NO_LIMIT,
    TIME_LIMIT,
    EVENT_LIMIT;

    fun isNoLimit(): Boolean {
        return this == NO_LIMIT
    }

    fun isTimeLimit(): Boolean {
        return this == TIME_LIMIT
    }

    fun isEventLimit(): Boolean {
        return this == EVENT_LIMIT
    }
}