package com.siliconlabs.bledemo.Browser.Model.Logs;

import android.bluetooth.BluetoothDevice;

import com.siliconlabs.bledemo.Browser.Model.LogType;

public class TimeoutLog extends Log {

    public TimeoutLog(BluetoothDevice device) {
        setLogTime(getTime());
        setLogInfo(device.getAddress() + "(" + getDeviceName(device.getName()) + "): "
                + "Connection timeout");
        setLogType(LogType.INFO); //malo wazne
        setDeviceAddress(device.getAddress());
    }

    public TimeoutLog() {
        setLogTime(getTime());
        setLogInfo("Connection timeout");
        setLogType(LogType.INFO); //malo wazne
    }

}