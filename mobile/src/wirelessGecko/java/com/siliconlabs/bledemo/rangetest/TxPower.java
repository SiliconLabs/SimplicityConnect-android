package com.siliconlabs.bledemo.rangetest;

import android.support.annotation.NonNull;

/**
 * @author Comarch S.A.
 */

public class TxPower {

    private final int characteristicValue;

    public TxPower(int characteristicValue) {
        this.characteristicValue = characteristicValue;
    }

    public int asCharacteristicValue() {
        return characteristicValue;
    }

    public float asDisplayValue() {
        return characteristicValue / 10f;
    }

    @NonNull
    @Override
    public String toString() {
        return String.valueOf(asDisplayValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TxPower txPower = (TxPower) o;

        return characteristicValue == txPower.characteristicValue;
    }

    @Override
    public int hashCode() {
        return characteristicValue;
    }
}
