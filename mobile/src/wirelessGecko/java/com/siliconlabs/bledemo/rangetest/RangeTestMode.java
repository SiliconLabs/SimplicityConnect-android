package com.siliconlabs.bledemo.rangetest;

/**
 * @author Comarch S.A.
 */

public enum RangeTestMode {
    Rx(1), Tx(2);

    private final int code;

    RangeTestMode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static RangeTestMode fromCode(int code) {
        switch (code) {
            case 1:
                return Rx;
            case 2:
                return Tx;
            default:
                throw new IllegalArgumentException("No mode for code: " + code);
        }
    }
}
