package com.siliconlabs.bledemo.utils

import java.util.BitSet

fun ByteArray.toBitSet(): BitSet = BitSet.valueOf(this)

inline fun <reified T> MutableList<T>.addIf(condition: Boolean, item: () -> T) {
    if (condition) this.add(item())
}
