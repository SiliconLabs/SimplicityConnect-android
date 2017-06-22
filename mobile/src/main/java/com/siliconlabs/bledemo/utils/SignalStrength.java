package com.siliconlabs.bledemo.utils;

import android.content.Context;

import com.siliconlabs.bledemo.R;


public enum SignalStrength {
    WEAK(R.string.keyfob_signal_strength_weak),
    AVERAGE(R.string.keyfob_signal_strength_average),
    GOOD(R.string.keyfob_signal_strength_good),
    UNKNOWN(R.string.keyfob_signal_strength_unknown);

    private int stringId;

    SignalStrength(int stringId) {
        this.stringId = stringId;
    }

    public static String getStringLabelFromStrength(Context context, SignalStrength signalStrength) {
        switch (signalStrength) {
            case WEAK:
                return context.getString(R.string.keyfob_signal_strength_weak);
            case AVERAGE:
                return context.getString(R.string.keyfob_signal_strength_average);
            case GOOD:
                return context.getString(R.string.keyfob_signal_strength_good);
            case UNKNOWN:
                return context.getString(R.string.keyfob_signal_strength_unknown);
            default:
                return context.getString(R.string.keyfob_signal_strength_unknown);
        }
    }

    /**
     * @param rssiOrig     Received signal strength indicator, weakest signal is at -100, strongest signal is at 0
     * @param distanceOrig Distance in meters (non negative)
     * @return the signal strength based on rssi/distance threshold values
     */
    public static SignalStrength calculateSignalStrengthUsingRssiToDistanceRatio(float rssiOrig, float distanceOrig) {
        // Threshold ratios are based on statistics from the tests reported at the following link
        // http://www.irishapps.org/examine-the-characteristics-of-the-bluetooth-rssi-signal-in-an-open-unobstructed-space-at-different-distances/
        float rssi = 0;

        // convert to positive values where 0 is weakest and 100 is strongest
        if (rssiOrig < -100) {
            rssi = 0; // set to weakest value
        } else if (rssiOrig > 0) {
            rssi = 100; // set to strongest value
        } else {
            rssi = 100 + rssiOrig; // convert to positive scale
        }

        // rssi/distance ratio at 5 meters
        float thresholdRatioStrong = 7f;

        // values at 15 meters
        float thresholdRatioAverage = 1.6f;

        float signalStrengthRatio = rssi / distanceOrig;
        if (signalStrengthRatio > thresholdRatioStrong) {
            return SignalStrength.GOOD;
        } else if (signalStrengthRatio > thresholdRatioAverage) {
            return SignalStrength.AVERAGE;
        } else {
            return SignalStrength.WEAK;
        }
    }

    /**
     * @param rssiOrig Received signal strength indicator, weakest signal is at -100, strongest signal is at 0
     * @return the signal strength based on rssi/distance threshold values
     */
    public static SignalStrength calculateSignalStrengthUsingRssi(float rssiOrig) {
        // Threshold ratios are based on statistics from the tests reported at the following link
        // http://www.irishapps.org/examine-the-characteristics-of-the-bluetooth-rssi-signal-in-an-open-unobstructed-space-at-different-distances/
        float rssi = 0;

        // convert to positive values where 0 is weakest and 100 is strongest
        if (rssiOrig < -100) {
            rssi = 0; // set to weakest value
        } else if (rssiOrig > 0) {
            rssi = 100; // set to strongest value
        } else {
            rssi = 100 + rssiOrig; // convert to positive scale
        }

        // rssi/distance ratio at 5 meters
        float thresholdRatioStrong = 45;

        // values at 15 meters
        float thresholdRatioAverage = 25;

        if (rssi > thresholdRatioStrong) {
            return SignalStrength.GOOD;
        } else if (rssi > thresholdRatioAverage) {
            return SignalStrength.AVERAGE;
        } else if (rssi >= 0) {
            return SignalStrength.WEAK;
        } else {
            return SignalStrength.UNKNOWN;
        }
    }

    public int getStringId() {
        return stringId;
    }
}
