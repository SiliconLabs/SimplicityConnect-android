package com.siliconlabs.bledemo.utils;

import android.content.res.Resources;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.activity.BeaconScanActivityFragment;


public enum Proximity {
    UNKNOWN(R.string.unknown),
    IMMEDIATE(R.string.immediate),
    NEAR(R.string.near),
    FAR(R.string.far);

    private int stringId;
    private final static double NEAR_THRESHOLD = 0.5;
    private final static double FAR_THRESHOLD = 1.0;
    private final static int defaultTxPower = -60;

    Proximity(int stringId) {
        this.stringId = stringId;
    }

    public static double getDistance(Integer rssi, Integer txPower) {
        if (rssi == null || rssi == 0 || txPower == null || txPower == 0) {
            return -1.0; // if we cannot determine distance, return -1.
        }
        if (txPower == Integer.MIN_VALUE) {
            txPower = defaultTxPower;
        }

        double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
            return (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
        }
    }

    public static double getDistance(BeaconScanActivityFragment.SimplifiedAdvertisement advertisement) {
        return getDistance(advertisement.rssi, advertisement.txPower);
    }

    public static Proximity getProximity(Integer rssi, Integer txPower) {
        double distance = getDistance(rssi, txPower);
        if (distance <= 0) {
            return UNKNOWN;
        } else if (distance < NEAR_THRESHOLD) {
            return IMMEDIATE;
        } else if (distance < FAR_THRESHOLD) {
            return NEAR;
        }
        return FAR;
    }

    public static String getProximityString(Resources res, Integer rssi, Integer txPower) {
        return res.getString(getProximity(rssi, txPower).stringId);
    }

    public static String getProximityStringUppercase(Resources res, Integer rssi, Integer txPower) {
        return getProximityString(res, rssi, txPower).toUpperCase();
    }

    public int getStringId() {
        return stringId;
    }
}
