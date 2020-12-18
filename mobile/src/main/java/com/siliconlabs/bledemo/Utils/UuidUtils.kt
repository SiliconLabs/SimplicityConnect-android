package com.siliconlabs.bledemo.Utils

object UuidUtils {
    fun parseIntFromUuidStart(uuid: String): Int {
        return uuid.split("-".toRegex()).toTypedArray()[0].toLong(16).toInt()
    }
}