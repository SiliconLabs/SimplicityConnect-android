package com.siliconlabs.bledemo.Bluetooth.BLE

import com.siliconlabs.bledemo.Bluetooth.Parsing.Converters

object ErrorCodes {
    private fun getTwoOctetsErrorCodeHexAsString(status: Int): String {
        return "0x" + Converters.getHexValue((status shr 8).toByte()) + Converters.getHexValue(status.toByte())
    }

    @JvmStatic
    fun getOneOctetErrorCodeHexAsString(status: Int): String {
        return "0x" + Converters.getHexValue(status.toByte())
    }

    private fun getConnectionErrorFlagAsString(status: Int): String {
        return when (status) {
            0x00 -> "SUCCESS"
            0x01 -> "GATT CONN L2C FAILURE"
            0x08 -> "GATT CONN TIMEOUT"
            0x13 -> "GATT CONN TERMINATE PEER USER"
            0x16 -> "GATT CONN TERMINATE LOCAL HOST"
            0x3E -> "GATT CONN FAIL ESTABLISH"
            0x22 -> "GATT CONN LMP TIMEOUT"
            0x0100 -> "GATT CONN CANCEL"
            0x0085 -> "GATT ERROR"
            else -> "UNKNOWN ERROR"
        }
    }

    private fun getATTErrorFlagAsString(status: Int): String {
        return when (status) {
            0x01 -> "INVALID HANDLE"
            0x02 -> "READ NOT PERMITTED"
            0x03 -> "WRITE NOT PERMITTED"
            0x04 -> "INVALID PDU"
            0x05 -> "INSUFFICIENT AUTHENTICATION"
            0x06 -> "REQUEST NOT SUPPORTED"
            0x07 -> "INVALID OFFSET"
            0x08 -> "INSUFFICIENT AUTHORIZATION"
            0x09 -> "PREPARE QUEUE FULL"
            0x0A -> "ATTRIBUTE NOT FOUND"
            0x0B -> "ATTRIBUTE NOT LONG"
            0x0C -> "INSUFFICIENT ENCRYPTION KEY SIZE"
            0x0D -> "INVALID ATTRIBUTE VALUE LENGTH"
            0x0E -> "UNLIKELY ERROR"
            0x0F -> "INSUFFICIENT ENCRYPTION"
            0x10 -> "UNSUPPORTED GROUP TYPE"
            0x11 -> "INSUFFICIENT RESOURCES"
            0x12 -> "DATABASE OUT OF SYNC"
            0x13 -> "VALUE NOT ALLOWED"
            0x80 -> "GATT_NO_RESOURCES"
            0x81 -> "GATT_INTERNAL_ERROR"
            0x82 -> "GATT_WRONG_STATE"
            0x83 -> "GATT_DB_FULL"
            0x84 -> "GATT: BUSY"
            0x85 -> "GATT ERROR"
            0x86 -> "GATT CMD STARTED"
            0x87 -> "GATT ILLEGAL PARAMETER"
            else -> "UNKNOWN ERROR"
        }
    }

    private fun getATTErrorDescription(status: Int): String {
        return when (status) {
            0x01 -> "The attribute handle given was not valid on this server."
            0x02 -> "Tha attribute cannot be read."
            0x03 -> "The attribute cannot be written."
            0x04 -> "The attribute PDU was invalid."
            0x05 -> "The attribute requires authentication before it can be read or written."
            0x06 -> "Attribute server does not support the request received from the client."
            0x07 -> "Offset specified was past the end of the attribute."
            0x08 -> "The attribute requires authorization before it can be read or written."
            0x09 -> "Too many prepare writes have been queued."
            0x0A -> "No attribute found within the given attribute handle range."
            0x0B -> "The attribute cannot be read using the ATT_READ_BLOB_REQ PDU."
            0x0C -> "The Encryption Key Size used for encrypting this link is insufficient."
            0x0D -> "The attribute value length is invalid for the operation."
            0x0E -> "The attribute request that was requested has encountered an error that was" +
                    " unlikely, and therefore could not be completed as requested."
            0x0F -> "The attribute requires encryption before it can be read or written."
            0x10 -> "The attribute type is not a supported grouping attribute as defined by a higher layer specification."
            0x11 -> "Insufficient Resources to complete the request."
            0x12 -> "The server requests the client to rediscover the database."
            0x13 -> "The attribute parameter value was not allowed."
            0x80 -> "CRC check failed, or signature failure (if enabled)."
            0x81 -> "This error is returned if the OTA has not " +
                    "been started (by writing value 0x0 to the " +
                    "control endpoint) and the client tries to " +
                    "send data or terminate the update."
            0x82 -> "AppLoader has run out of buffer space."
            0x83 -> "New firmware image is too large to fit into flash, or it overlaps with AppLoader."
            0x84 -> "GBL file parsing failed. Potential causes " +
                    "are for example:<br/>" +
                    "1) Attempting a partial update from one " +
                    "SDK version to another (such as 2.3.0 to " +
                    "2.4.0)<br/>" +
                    "2) The file is not a valid GBL file (for example, client is sending an EBL file)"
            0x85 -> "The Gecko bootloader cannot erase or write flash as requested by AppLoader, for example " +
                    "if the download area is too small to fit the entire GBL image."
            0x86 -> "Wrong type of bootloader. For example, target device has UART DFU bootloader instead of OTA bootloader installed."
            0x87 -> "New application image is rejected because it would overlap with the AppLoader."
            else -> "No description"
        }
    }

    @JvmStatic
    fun getATTHTMLFormattedError(status: Int): String {
        return "<b>Error: </b>" + getOneOctetErrorCodeHexAsString(status) +
                " (" + getATTErrorFlagAsString(status) + ")" + "<br/><br/>" +
                "<b>Description: </b>" + getATTErrorDescription(status)
    }

    //Failed connecting to: <device_name>. Reason: <error code and reason>.
    @JvmStatic
    fun getFailedConnectingToDeviceMessage(deviceName: String?, status: Int): String {
        return StringBuilder().apply {
            append("Failed connecting to: ")
                    .append(deviceName).append(".\n")
                    .append("Reason: ")
                    .append(getTwoOctetsErrorCodeHexAsString(status)).append(" ")
                    .append(getConnectionErrorFlagAsString(status))
        }.toString()
    }

    //Device <device name> has disconnected. Reason: <error code and reason>.
    @JvmStatic
    fun getDeviceDisconnectedMessage(deviceName: String?, status: Int): String {
        return StringBuilder().apply {
            append("Device ")
                    .append(deviceName)
                    .append(" has disconnected.").append("\n")
                    .append("Reason: ")
                    .append(getTwoOctetsErrorCodeHexAsString(status)).append(" ")
                    .append(getConnectionErrorFlagAsString(status))
        }.toString()
    }

    fun getAdvertiserErrorMessage(errorCode: Int): String {
        val message = when (errorCode) {
            1 -> "Advertise data is too large"
            2 -> "Too many advertisers"
            3 -> "Advertiser has already started"
            4 -> "Internal error"
            5 -> "Feature unsupported"
            else -> "Invalid advertising parameters"
        }
        return "Error: $message"
    }
}