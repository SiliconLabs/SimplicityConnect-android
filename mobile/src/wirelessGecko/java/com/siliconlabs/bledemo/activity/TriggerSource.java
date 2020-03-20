package com.siliconlabs.bledemo.activity;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.siliconlabs.bledemo.R;

public enum TriggerSource {
    UNKNOWN(-1, android.R.color.transparent),
    BLUETOOTH(0, R.drawable.icon_bluetooth),
    ZIGBEE(1, R.drawable.icon_zigbee),
    BUTTON(2, android.R.color.transparent),
    PROPRIETARY(5, R.drawable.icon_proprietary),
    CONNECT(6, R.drawable.icon_connect),
    THREAD(7, R.drawable.icon_thread);

    @DrawableRes
    public final int iconId;

    public final int value;

    TriggerSource(int value, @DrawableRes int iconId) {
        this.value = value;
        this.iconId = iconId;
    }

    @NonNull
    public static TriggerSource forValue(int value) {
        for (TriggerSource triggerSource : values()) {
            if (triggerSource.value == value) {
                return triggerSource;
            }
        }
        return UNKNOWN;
    }
}
