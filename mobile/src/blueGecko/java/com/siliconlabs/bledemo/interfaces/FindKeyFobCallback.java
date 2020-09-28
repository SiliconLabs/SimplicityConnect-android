package com.siliconlabs.bledemo.interfaces;

import com.siliconlabs.bledemo.Bluetooth.BLE.BluetoothDeviceInfo;

public interface FindKeyFobCallback {
    void findKeyFob(BluetoothDeviceInfo fob);

    void triggerDisconnect();

    String getDeviceName();
}
