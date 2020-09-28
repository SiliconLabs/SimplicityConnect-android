package com.siliconlabs.bledemo.Browser;

import com.siliconlabs.bledemo.Bluetooth.BLE.BluetoothDeviceInfo;

public interface ServicesConnectionsCallback {
    void onDisconnectClicked(BluetoothDeviceInfo deviceInfo);

    void onDeviceClicked(BluetoothDeviceInfo device);
}
