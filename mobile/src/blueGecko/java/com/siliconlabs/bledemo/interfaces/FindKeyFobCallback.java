package com.siliconlabs.bledemo.interfaces;

import com.siliconlabs.bledemo.ble.BluetoothDeviceInfo;

public interface FindKeyFobCallback {
    void findKeyFob(BluetoothDeviceInfo fob);

    void triggerDisconnect();

    String getDeviceName();
}
