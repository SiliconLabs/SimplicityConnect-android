package com.siliconlabs.bledemo.utils

import android.bluetooth.BluetoothGattDescriptor
import java.util.*

/**
 * Enumerated Notification State Handler
 */
enum class Notifications(
    /**
     * Storage Field for Enabled bool passed from .CTor
     */
    val isEnabled: Boolean,
    val descriptorValue: ByteArray
) {
    DISABLED(false, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE),
    NOTIFY(true, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE),
    INDICATE(true, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);

    override fun toString(): String {
        return name.lowercase(Locale.ROOT)
    }
}
