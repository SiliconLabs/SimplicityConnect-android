package com.siliconlabs.bledemo.Browser;

import com.siliconlabs.bledemo.Bluetooth.BLE.BluetoothDeviceInfo;

public interface DebugModeCallback {

    void connectToDevice(BluetoothDeviceInfo device);

    void addToFavorite(String deviceAddress);

    void removeFromFavorite(String deviceAddress);

    void addToTemporaryFavorites(String deviceAddress);

    void updateCountOfConnectedDevices();
}
