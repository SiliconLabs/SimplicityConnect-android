package com.siliconlabs.bledemo.log;

import com.siliconlabs.bledemo.other.LogType;

public class CommonLog extends Log {

    public CommonLog(String value, String deviceAddress) {
        setLogTime(getTime());
        setLogInfo(value);
        setLogType(LogType.INFO); //malo wazne
        setDeviceAddress(deviceAddress);
    }
}
