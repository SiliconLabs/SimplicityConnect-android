package com.siliconlabs.bledemo.ble;

import com.siliconlabs.bledemo.bluetoothdatamodel.parsing.Converters;

public class ErrorCodes {

    public static String getTwoOctetsErrorCodeHexAsString(int status) {
        return "0x" + Converters.getHexValue((byte) (status >> 8)) + Converters.getHexValue((byte) status);
    }

    public static String getOneOctetErrorCodeHexAsString(int status) {
        return "0x" + Converters.getHexValue((byte) status);
    }

    private static String getConnectionErrorFlagAsString(int status) {
        switch (status) {
            case 0x00:
                return "SUCCESS";
            case 0x01:
                return "GATT CONN L2C FAILURE";
            case 0x08:
                return "GATT CONN TIMEOUT";
            case 0x13:
                return "GATT CONN TERMINATE PEER USER";
            case 0x16:
                return "GATT CONN TERMINATE LOCAL HOST";
            case 0x3E:
                return "GATT CONN FAIL ESTABLISH";
            case 0x22:
                return "GATT CONN LMP TIMEOUT";
            case 0x0100:
                return "GATT CONN CANCEL";
            case 0x0085:
                return "GATT ERROR";
            default:
                return "UNKNOWN ERROR";
        }
    }

    public static String getATTErrorFlagAsString(int status) {
        switch (status) {
            case 0x01:
                return "INVALID HANDLE";
            case 0x02:
                return "READ NOT PERMITTED";
            case 0x03:
                return "WRITE NOT PERMITTED";
            case 0x04:
                return "INVALID PDU";
            case 0x05:
                return "INSUFFICIENT AUTHENTICATION";
            case 0x06:
                return "REQUEST NOT SUPPORTED";
            case 0x07:
                return "INVALID OFFSET";
            case 0x08:
                return "INSUFFICIENT AUTHORIZATION";
            case 0x09:
                return "PREPARE QUEUE FULL";
            case 0x0A:
                return "ATTRIBUTE NOT FOUND";
            case 0x0B:
                return "ATTRIBUTE NOT LONG";
            case 0x0C:
                return "INSUFFICIENT ENCRYPTION KEY SIZE";
            case 0x0D:
                return "INVALID ATTRIBUTE VALUE LENGTH";
            case 0x0E:
                return "UNLIKELY ERROR";
            case 0x0F:
                return "INSUFFICIENT ENCRYPTION";
            case 0x10:
                return "UNSUPPORTED GROUP TYPE";
            case 0x11:
                return "INSUFFICIENT RESOURCES";
            case 0x12:
                return "DATABASE OUT OF SYNC";
            case 0x13:
                return "VALUE NOT ALLOWED";
            case 0x80:
                return "GATT_NO_RESOURCES";
            case 0x81:
                return "GATT_INTERNAL_ERROR";
            case 0x82:
                return "GATT_WRONG_STATE";
            case 0x83:
                return "GATT_DB_FULL";
            case 0x84:
                return "GATT: BUSY";
            case 0x85:
                return "GATT ERROR";
            case 0x86:
                return "GATT CMD STARTED";
            case 0x87:
                return "GATT ILLEGAL PARAMETER";
            default:
                return "UNKNOWN ERROR";
        }
    }

    public static String getATTErrorDescription(int status) {
        switch (status) {
            case 0x01:
                return "The attribute handle given was not valid on this server.";
            case 0x02:
                return "Tha attribute cannot be read.";
            case 0x03:
                return "The attribute cannot be written.";
            case 0x04:
                return "The attribute PDU was invalid.";
            case 0x05:
                return "The attribute requires authentication before it can be read or written.";
            case 0x06:
                return "Attribute server does not support the request received from the client.";
            case 0x07:
                return "Offset specified was past the end of the attribute.";
            case 0x08:
                return "The attribute requires authorization before it can be read or written.";
            case 0x09:
                return "Too many prepare writes have been queued.";
            case 0x0A:
                return "No attribute found within the given attribute handle range.";
            case 0x0B:
                return "The attribute cannot be read using the ATT_READ_BLOB_REQ PDU.";
            case 0x0C:
                return "The Encryption Key Size used for encrypting this link is insufficient.";
            case 0x0D:
                return "The attribute value length is invalid for the operation.";
            case 0x0E:
                return "The attribute request that was requested has encountered an error that was" +
                        " unlikely, and therefore could not be completed as requested.";
            case 0x0F:
                return "The attribute requires encryption before it can be read or written.";
            case 0x10:
                return "The attribute type is not a supported grouping attribute as defined by a higher layer specification.";
            case 0x11:
                return "Insufficient Resources to complete the request.";
            case 0x12:
                return "The server requests the client to rediscover the database.";
            case 0x13:
                return "The attribute parameter value was not allowed.";
            case 0x80:
                return "CRC check failed, or signature failure (if enabled).";
            case 0x81:
                return "This error is returned if the OTA has not " +
                        "been started (by writing value 0x0 to the " +
                        "control endpoint) and the client tries to " +
                        "send data or terminate the update.";
            case 0x82:
                return "AppLoader has run out of buffer space.";
            case 0x83:
                return "New firmware image is too large to fit into flash, or it overlaps with AppLoader.";
            case 0x84:
                return "GBL file parsing failed. Potential causes " +
                        "are for example:<br/>" +
                        "1) Attempting a partial update from one " +
                        "SDK version to another (such as 2.3.0 to " +
                        "2.4.0)<br/>" +
                        "2) The file is not a valid GBL file (for example, client is sending an EBL file)";
            case 0x85:
                return "The Gecko bootloader cannot erase or write flash as requested by AppLoader, for example " +
                        "if the download area is too small to fit the entire GBL image.";
            case 0x86:
                return "Wrong type of bootloader. For example, target device has UART DFU bootloader instead of OTA bootloader installed.";
            case 0x87:
                return "New application image is rejected because it would overlap with the AppLoader.";
            default:
                return "No description";
        }
    }

    public static String getATTHTMLFormattedError(int status) {
        return "<b>Error: </b>" + getOneOctetErrorCodeHexAsString(status) +
                " (" + getATTErrorFlagAsString(status) + ")" + "<br/><br/>" +
                "<b>Description: </b>" + getATTErrorDescription(status);
    }

    //Failed connecting to: <device_name>. Reason: <error code and reason>.
    public static String getFailedConnectingToDeviceMessage(String deviceName, int status) {
        StringBuilder builder = new StringBuilder();

        builder.append("Failed connecting to: ").append(deviceName).append(".\n");
        builder.append("Reason: ").append(getTwoOctetsErrorCodeHexAsString(status)).append(" ");
        builder.append(getConnectionErrorFlagAsString(status));

        return builder.toString();
    }

    //Device <device name> has disconnected. Reason: <error code and reason>.
    public static String getDeviceDisconnectedMessage(String deviceName, int status) {
        StringBuilder builder = new StringBuilder();

        builder.append("Device ").append(deviceName).append(" has disconnected.").append("\n")
                .append("Reason: ").append(getTwoOctetsErrorCodeHexAsString(status)).append(" ")
                .append(getConnectionErrorFlagAsString(status));

        return builder.toString();
    }
}
