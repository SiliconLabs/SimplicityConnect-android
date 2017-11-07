package com.siliconlabs.bledemo.interfaces;

import com.siliconlabs.bledemo.ble.BluetoothDeviceInfo;

public interface DebugModeCallback {
    void callbackSelected(BluetoothDeviceInfo device);

    void showAdvertisementDialog(BluetoothDeviceInfo device);

    void connectToDevice(BluetoothDeviceInfo device);

    void disconnectFromDevice(BluetoothDeviceInfo device);
}
