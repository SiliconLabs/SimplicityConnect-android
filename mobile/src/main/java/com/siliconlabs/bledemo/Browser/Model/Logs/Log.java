package com.siliconlabs.bledemo.Browser.Model.Logs;

import com.siliconlabs.bledemo.Browser.Model.LogType;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Log {
    private String logTime;
    private String logInfo;
    private LogType logType;
    private String deviceAddress;

    public String getLogTime() {
        return logTime;
    }

    public void setLogTime(String logTime) {
        this.logTime = logTime;
    }

    public String getLogInfo() {
        return logInfo;
    }

    public void setLogInfo(String logInfo) {
        this.logInfo = logInfo;
    }

    public LogType getLogType() {
        return logType;
    }

    public void setLogType(LogType logType) {
        this.logType = logType;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public static String getTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
        return formatter.format(calendar.getTime());
    }

    public static String getDeviceName(String name) {
        if (name == null) {
            return "N/A";
        }
        return name;
    }

}