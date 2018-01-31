package com.siliconlabs.bledemo.beaconutils.ibeacon;

public class IBeaconInfo {
    public String uuid = "unknown";
    public int major = Integer.MAX_VALUE;
    public int minor = Integer.MIN_VALUE;

    public IBeaconInfo(String uuid, int major, int minor) {
        this.uuid = uuid;
        this.major = major;
        this.minor = minor;
    }

    public int getMinor() {
        return minor;
    }

    public int getMajor() {
        return major;
    }

    public String getUuid() {
        return uuid;
    }
}