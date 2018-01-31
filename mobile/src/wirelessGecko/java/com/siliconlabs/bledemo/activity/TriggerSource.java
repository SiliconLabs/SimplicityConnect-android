package com.siliconlabs.bledemo.activity;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.siliconlabs.bledemo.R;

public enum TriggerSource {
    UNKNOWN(-1, android.R.color.transparent),
    BLUETOOTH(0, R.drawable.icon_bluetooth),
    ZIGBEE(1, R.drawable.icon_zigbee),
    BUTTON(2, android.R.color.transparent),
    PROPRIETARY(5, R.drawable.icon_proprietary);

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
