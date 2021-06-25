package com.siliconlabs.bledemo.iop_test.utils

class ErrorCodes {
    companion object {
        fun getErrorName(code: Int): String {
            when (code) {
                0x0001 -> return "GATT INVALID HANDLE"
                0x0002 -> return "GATT READ NOT PERMIT"
                0x0003 -> return "GATT WRITE NOT PERMIT"
                0x0004 -> return "GATT INVALID PDU"
                0x0005 -> return "GATT INSUF AUTHENTICATION"
                0x0006 -> return "GATT REQ NOT SUPPORTED"
                0x0007 -> return "GATT INVALID OFFSET"
                0x0008 -> return "GATT INSUF AUTHORIZATION"
                0x0009 -> return "GATT PREPARE Q FULL"
                0x000a -> return "GATT NOT FOUND"
                0x000b -> return "GATT NOT LONG"
                0x000c -> return "GATT INSUF KEY SIZE"
                0x000d -> return "GATT INVALID ATTR LEN"
                0x000e -> return "GATT ERR UNLIKELY"
                0x000f -> return "GATT INSUF ENCRYPTION"
                0x0010 -> return "GATT UNSUPPORT GRP TYPE"
                0x0011 -> return "GATT INSUF RESOURCE"
                0x0087 -> return "GATT ILLEGAL PARAMETER"
                0x0080 -> return "GATT_NO_RESOURCES"
                0x0081 -> return "GATT_INTERNAL_ERROR"
                0x0082 -> return "GATT_WRONG_STATE"
                0x0083 -> return "GATT_DB_FULL"
                0x0084 -> return "GATT: BUSY"
                0x0085 -> return "GATT ERROR"
                0x0086 -> return "GATT CMD STARTED"
                0x0088 -> return "GATT PENDING"
                0x0089 -> return "GATT AUTH FAIL"
                0x008a -> return "GATT MORE"
                0x008b -> return "GATT INVALID CFG"
                0x008c -> return "GATT SERVICE STARTED"
                0x008d -> return "GATT ENCRYPTED NO MITM"
                0x008e -> return "GATT NOT ENCRYPTED"
                0x008f -> return "GATT CONGESTED"
                0x00FD -> return "GATT CCCD CFG ERROR"
                0x00FE -> return "GATT PROCEDURE IN PROGRESS"
                0x00FF -> return "GATT VALUE OUT OF RANGE"
                0x0101 -> return "TOO MANY OPEN CONNECTIONS"
                else -> return "ERROR NOT HANDLED: $code"
            }
        }
    }
}