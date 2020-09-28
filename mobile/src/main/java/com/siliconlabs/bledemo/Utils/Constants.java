package com.siliconlabs.bledemo.Utils;

import android.view.MenuItem;

import com.siliconlabs.bledemo.Browser.Model.Logs.Log;

import java.util.LinkedList;
import java.util.List;

public class Constants {

    public static final String OTA_SERVICE = "OTA Service";
    public static final String NA = "N/A";
    public static final String BOTTOM_NAVI_DEVELOP = "Develop";
    public static final String BOTTOM_NAVI_DEMO = "Demo";

    public static List<Log> LOGS = new LinkedList<>();

    public static MenuItem ota_button;

    public static void clearLogs() {
        LOGS.clear();
    }
}
