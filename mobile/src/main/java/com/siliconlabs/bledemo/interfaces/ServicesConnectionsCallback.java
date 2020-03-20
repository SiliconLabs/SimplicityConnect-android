package com.siliconlabs.bledemo.interfaces;

import com.siliconlabs.bledemo.ble.BluetoothDeviceInfo;

public interface ServicesConnectionsCallback {
    void onDisconnectClicked(BluetoothDeviceInfo deviceInfo);

    void onDeviceClicked(BluetoothDeviceInfo device);
}
