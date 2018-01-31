package com.siliconlabs.bledemo.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    public static String millisToTimeAmPm(long millis) {
        return dateFormat.format(new Date(millis));
    }
}
