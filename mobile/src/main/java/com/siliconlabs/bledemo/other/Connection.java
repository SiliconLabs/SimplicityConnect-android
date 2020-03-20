package com.siliconlabs.bledemo.other;

public class Connection {
    private String deviceName;

    public Connection(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
}
