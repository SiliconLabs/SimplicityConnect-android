package com.siliconlabs.bledemo.Browser.Model.Logs;

import com.siliconlabs.bledemo.Browser.Model.LogType;

public class DisconnectByButtonLog extends Log {

    public DisconnectByButtonLog(String deviceAddress) {
        setLogTime(getTime());
        setLogInfo(deviceAddress + " Disconnected on UI");
        setLogType(LogType.INFO); //malo wazne
        setDeviceAddress(deviceAddress);
    }

}