package com.siliconlabs.bledemo.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;

public abstract class TimeoutGattCallback extends BluetoothGattCallback {
    public void onTimeout() { }
}
