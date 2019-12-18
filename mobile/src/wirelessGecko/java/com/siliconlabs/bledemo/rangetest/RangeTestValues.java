package com.siliconlabs.bledemo.rangetest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Comarch S.A.
 */

public class RangeTestValues {

    public static final int PACKET_COUNT_REPEAT = 65535;

    public static final List<Integer> CHANNEL_LOOKUP;
    public static final List<Integer> PACKET_COUNT_LOOKUP;
    public static final List<Integer> ID_LOOKUP;

    private static final int CHANNELS_MIN = 0;
    private static final int CHANNELS_MAX = 19;

    private static final Integer[] PACKET_COUNTS = new Integer[]{500, 1000, 2500, 5000, 10000, 25000, 50000};

    private static final int ID_MIN = 0;
    private static final int ID_MAX = 32;

    static {
        int channelsLookupSize = CHANNELS_MAX - CHANNELS_MIN + 1;
        List<Integer> channelsLookup = new ArrayList<>(channelsLookupSize);
        for (int i = 0; i < channelsLookupSize; ++i) {
            channelsLookup.add(CHANNELS_MIN + i);
        }
        CHANNEL_LOOKUP = Collections.unmodifiableList(channelsLookup);

        PACKET_COUNT_LOOKUP = Collections.unmodifiableList(Arrays.asList(PACKET_COUNTS));

        int idLookupSize = ID_MAX - ID_MIN + 1;
        List<Integer> idLookup = new ArrayList<>(idLookupSize);
        for (int i = 0; i < idLookupSize; ++i) {
            idLookup.add(ID_MIN + i);
        }
        ID_LOOKUP = Collections.unmodifiableList(idLookup);
    }

    private RangeTestValues() {
    }
}
