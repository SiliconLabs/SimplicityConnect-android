package com.siliconlabs.bledemo.bluetooth.parsing

enum class Property {
    READ,
    WRITE,
    WRITE_WITHOUT_RESPONSE,
    SIGNED_WRITE,
    RELIABLE_WRITE,
    NOTIFY,
    INDICATE,
    WRITABLE_AUXILIARIES,
    BROADCAST;

    enum class Type {
        OPTIONAL,
        MANDATORY,
        EXCLUDED
    }
}