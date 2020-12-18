package com.siliconlabs.bledemo.RangeTest.Models

/**
 * @author Comarch S.A.
 */
class TxPower(private val characteristicValue: Int) {
    fun asCharacteristicValue(): Int {
        return characteristicValue
    }

    fun asDisplayValue(): Float {
        return characteristicValue / 10f
    }

    override fun toString(): String {
        return asDisplayValue().toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val txPower = other as TxPower
        return characteristicValue == txPower.characteristicValue
    }

    override fun hashCode(): Int {
        return characteristicValue
    }

}