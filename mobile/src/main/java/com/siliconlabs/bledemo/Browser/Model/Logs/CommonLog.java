package com.siliconlabs.bledemo.Browser.Model.Logs;

import com.siliconlabs.bledemo.Browser.Model.LogType;

public class CommonLog extends Log {

    public CommonLog(String value, String deviceAddress) {
        setLogTime(getTime());
        setLogInfo(value);
        setLogType(LogType.INFO); //malo wazne
        setDeviceAddress(deviceAddress);
    }
}
