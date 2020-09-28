package com.siliconlabs.bledemo.BeaconUtils.ibeacon;

public class IBeaconInfo {
    private String uuid = "unknown";
    private int major = Integer.MAX_VALUE;
    private int minor = Integer.MIN_VALUE;
    private int power;

    public IBeaconInfo(String uuid, int major, int minor, int power) {
        this.uuid = uuid;
        this.major = major;
        this.minor = minor;
        this.power = power;
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

    public int getPower() {
        return power;
    }
}