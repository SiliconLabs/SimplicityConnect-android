package com.siliconlabs.bledemo.utils

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class GattQueue(private val gatt: BluetoothGatt?) {

    private val commands: Queue<GattCommand> = LinkedList()
    private val lock: Lock = ReentrantLock()
    private var processing = false

    fun queueRead(characteristic: BluetoothGattCharacteristic?) {
        queue(GattCommand(GattCommand.Type.READ, gatt, characteristic))
    }

    fun queueWrite(characteristic: BluetoothGattCharacteristic?) {
        queue(GattCommand(GattCommand.Type.WRITE, gatt, characteristic))
    }

    fun queueIndicate(characteristic: BluetoothGattCharacteristic?) {
        queue(GattCommand(GattCommand.Type.INDICATE, gatt, characteristic))
    }

    fun queueNotify(characteristic: BluetoothGattCharacteristic?) {
        queue(GattCommand(GattCommand.Type.NOTIFY, gatt, characteristic))
    }

    fun queueCancelNotifications(characteristic: BluetoothGattCharacteristic?) {
        queue(GattCommand(GattCommand.Type.CANCEL_NOTIFICATIONS, gatt, characteristic))
    }

    fun clear() {
        commands.clear()
    }

    fun clearAllButLast() {
        lock.lock()
        try {
            while (commands.size > 1) commands.remove()
        } finally {
            lock.unlock()
        }
    }

    fun handleCommandProcessed() {
        lock.lock()
        try {
            if (commands.isEmpty()) {
                processing = false
            } else {
                processNextCommand()
            }
        } finally {
            lock.unlock()
        }
    }

    private fun processNextCommand() {
        var success = false
        val command = commands.poll()

        if (command?.gatt != null && command.characteristic != null) {
            val gatt = command.gatt
            val characteristic = command.characteristic

            success = when (command.type) {
                GattCommand.Type.READ -> gatt.readCharacteristic(characteristic)
                GattCommand.Type.WRITE -> gatt.writeCharacteristic(characteristic)
                GattCommand.Type.INDICATE -> setNotificationForCharacteristic(gatt, characteristic, Notifications.INDICATE)
                GattCommand.Type.NOTIFY -> setNotificationForCharacteristic(gatt, characteristic, Notifications.NOTIFY)
                GattCommand.Type.CANCEL_NOTIFICATIONS -> setNotificationForCharacteristic(gatt, characteristic, Notifications.DISABLED)
            }
        } else {
            handleCommandProcessed()
        }
        processing = success
    }

    private fun queue(command: GattCommand) {
        lock.lock()
        try {
            commands.add(command)
            if (!processing) {
                processNextCommand()
            }
        } finally {
            lock.unlock()
        }
    }

    private fun setNotificationForCharacteristic(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: Notifications
    ) : Boolean {
        gatt.setCharacteristicNotification(characteristic, value.isEnabled)
        return characteristic.getDescriptor(UUID.fromString(CCCD_DESCRIPTOR_UUID))?.let {
            it.value = value.descriptorValue
            gatt.writeDescriptor(it)
        } ?: false
    }

    internal class GattCommand(
            val type: Type,
            val gatt: BluetoothGatt?,
            val characteristic: BluetoothGattCharacteristic?
    ) {
        enum class Type {
            READ,
            WRITE,
            INDICATE,
            NOTIFY,
            CANCEL_NOTIFICATIONS
        }
    }

    companion object {
        private const val CCCD_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb"
    }
}