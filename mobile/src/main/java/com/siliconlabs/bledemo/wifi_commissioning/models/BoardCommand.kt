package com.siliconlabs.bledemo.wifi_commissioning.models

class BoardCommand {

    enum class Send(val value: Int) {
        SSID(2),
        SCAN(3),
        DISCONNECTION(4),
        SECURITY_TYPE(5),
        PASSWORD(6),
        CHECK_CONNECTION(7),
        GET_FIRMWARE_VERSION(8),
        UNKNOWN(0);
    }

    enum class Response(val value: Int) {

        CONNECTION(2),
        DISCONNECTION(4),
        CHECK_CONNECTION(7),
        FIRMWARE_VERSION(8),
        UNKNOWN(0);

        companion object {
            fun fromInt(code: Int): Response {
                for (command in values()) {
                    if (command.value == code) return command
                }
                return UNKNOWN
            }
        }
    }
}
