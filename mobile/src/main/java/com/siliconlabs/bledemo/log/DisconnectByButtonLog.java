package com.siliconlabs.bledemo.log;

import com.siliconlabs.bledemo.other.LogType;

public class DisconnectByButtonLog extends Log {

    public DisconnectByButtonLog(String deviceAddress) {
        setLogTime(getTime());
        setLogInfo(deviceAddress + " Disconnected on UI");
        setLogType(LogType.INFO); //malo wazne
        setDeviceAddress(deviceAddress);
    }

}