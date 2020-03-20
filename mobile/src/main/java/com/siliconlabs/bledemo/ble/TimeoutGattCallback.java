package com.siliconlabs.bledemo.ble;

import android.bluetooth.BluetoothGattCallback;

public abstract class TimeoutGattCallback extends BluetoothGattCallback {
    public void onTimeout() {
    }
}
