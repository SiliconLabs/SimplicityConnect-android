package com.siliconlabs.bledemo.utils

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import java.util.BitSet

fun ByteArray.toBitSet(): BitSet = BitSet.valueOf(this)

inline fun <reified T> MutableList<T>.addIf(condition: Boolean, item: () -> T) {
    if (condition) this.add(item())
}

inline fun <reified T> List<T>.indexOrNull(item: T): Int? = indexOf(item).takeIf { it != -1 }

inline fun <reified T> List<T>.indexOrNull(predicate: (T) -> Boolean): Int? =
    indexOfFirst(predicate).takeIf { it != -1 }

fun DialogFragment.showOnce(
    fragmentManager: FragmentManager,
    tag: String = this.javaClass.simpleName,
) {
    fragmentManager.executePendingTransactions()
    val oldDialog = fragmentManager.findFragmentByTag(tag)

    if (oldDialog == null && !isAdded && !isVisible) {
        show(fragmentManager, tag)
    }
}
