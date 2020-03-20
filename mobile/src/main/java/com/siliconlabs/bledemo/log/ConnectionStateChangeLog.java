package com.siliconlabs.bledemo.log;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;

import com.siliconlabs.bledemo.other.LogType;

public class ConnectionStateChangeLog extends Log {

    public ConnectionStateChangeLog(BluetoothGatt gatt, final int status, final int newState) {
        setLogTime(getTime());
        setLogInfo(gatt.getDevice().getAddress() + "(" + getDeviceName(gatt.getDevice().getName()) + "): "
                + parseNewState(newState) + ". Status: " + parseStatus(status));
        setLogType(LogType.INFO); //malo wazne
        setDeviceAddress(gatt.getDevice().getAddress());
    }

    private String parseNewState(int newState) {
        switch (newState) {
            case BluetoothProfile.STATE_DISCONNECTED:
                return "State Disconnected";
            case BluetoothProfile.STATE_CONNECTING:
                return "State Connecting";
            case BluetoothProfile.STATE_CONNECTED:
                return "Successful connect to device";
            case BluetoothProfile.STATE_DISCONNECTING:
                return "State Disconnecting";
            default:
                return String.valueOf(newState);
        }
    }

    private String parseStatus(int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            return "Success";
        }
        return String.valueOf(status);
    }
}