package com.siliconlabs.bledemo.utils;

public class UuidUtils {
    public static int parseIntFromUuidStart(String uuid) {
        return (int) Long.parseLong(uuid.split("-")[0], 16);
    }
}
