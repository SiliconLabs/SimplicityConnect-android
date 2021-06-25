package com.siliconlabs.bledemo.gatt_configurator.models

enum class Property {
    READ,
    WRITE,
    WRITE_WITHOUT_RESPONSE,
    RELIABLE_WRITE,
    NOTIFY,
    INDICATE;

    fun isWriteProperty(): Boolean {
        return this == WRITE || this == WRITE_WITHOUT_RESPONSE || this == RELIABLE_WRITE
    }

    enum class Type {
        AUTHENTICATED,
        BONDED,
        ENCRYPTED
    }
}