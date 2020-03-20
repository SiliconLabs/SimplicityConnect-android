package com.siliconlabs.bledemo.interfaces;

import com.siliconlabs.bledemo.ble.BluetoothDeviceInfo;

public interface DebugModeCallback {

    void connectToDevice(BluetoothDeviceInfo device);

    void addToFavorite(String deviceAddress);

    void removeFromFavorite(String deviceAddress);

    void updateCountOfConnectedDevices();
}
