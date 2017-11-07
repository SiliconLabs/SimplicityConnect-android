package com.siliconlabs.bledemo.activity;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.siliconlabs.bledemo.R;

public enum TriggerSource {
    UNKNOWN(-1, R.string.demo_light_changed_source_unknown, android.R.color.transparent),
    BLUETOOTH(0, R.string.demo_light_changed_source_bluetooth, R.drawable.icon_bluetooth),
    ZIGBEE(1, R.string.demo_light_changed_source_zigbee, R.drawable.icon_zigbee),
    BUTTON(2, R.string.demo_light_changed_source_button, android.R.color.transparent);

    @StringRes
    public final int labelId;

    @DrawableRes
    public final int iconId;

    public final int value;

    TriggerSource(int value, @StringRes int labelId, @DrawableRes int iconId) {
        this.value = value;
        this.labelId = labelId;
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
