package com.siliconlabs.bledemo.Browser.Model.Logs;

import android.bluetooth.BluetoothGatt;

import com.siliconlabs.bledemo.Browser.Model.LogType;

public class ServicesDiscoveredLog extends Log {

    public ServicesDiscoveredLog(BluetoothGatt gatt, final int status) {
        setLogTime(getTime());
        setLogInfo(gatt.getDevice().getAddress() + "(" + getDeviceName(gatt.getDevice().getName()) + "): "
                + parseStatus(status));
        setLogType(LogType.INFO); //malo wazne
        setDeviceAddress(gatt.getDevice().getAddress());
    }

    private static String parseStatus(int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            return "Successfully discovered services";
        }
        return "Unsuccessfully discovered services with status: " + status;
    }
}