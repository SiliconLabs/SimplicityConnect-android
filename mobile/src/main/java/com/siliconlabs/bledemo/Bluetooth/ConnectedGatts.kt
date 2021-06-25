package com.siliconlabs.bledemo.bluetooth

import android.bluetooth.BluetoothGatt

object ConnectedGatts {
    val list = ArrayList<BluetoothGatt>()
    val pendingConnections = HashSet<String>()

    fun removeDeviceWithAddress(address: String) {
        synchronized(list) {
            for (gatt in list) {
                if (gatt.device.address == address) {
                    list.remove(gatt)
                    break
                }
            }
        }
    }

    fun clearGatt(address: String) {
        synchronized(list) {
            for (gatt in list) {
                if (gatt.device.address == address) {
                    gatt.apply {
                        disconnect()
                        close()
                    }
                    list.remove(gatt)
                    break
                }
            }
        }
    }

    fun contains(address: String): Boolean {
        return list.any { it.device.address == address }
    }

    fun addOrSwap(address: String, gatt: BluetoothGatt) {
        synchronized(list) {
            if (contains(address)) {
                swapGattWithAddress(address, gatt)
            } else {
                add(gatt)
            }
        }
    }

    fun getByAddress(address: String): BluetoothGatt? {
        for (gatt in list) {
            if (gatt.device.address == address) {
                return gatt
            }
        }
        return null
    }

    fun swapGattWithAddress(address: String, newGatt: BluetoothGatt) {
        synchronized(list) {
            for (gatt in list) {
                if (gatt.device.address == address) {
                    list[list.indexOf(gatt)] = newGatt
                    break
                }
            }
        }
    }

    fun isGattWithAddressConnected(address: String): Boolean {
        for (gatt in list) {
            if (gatt.device.address == address) {
                return true
            }
        }
        return false
    }

    fun isAddressInPendingConnections(address: String): Boolean {
        return pendingConnections.contains(address)
    }

    fun clearAllGatts() {
        synchronized(list) {
            for (gatt in list) {
                gatt.apply {
                    disconnect()
                    close()
                }
            }
            clear()
        }
    }

    fun clear() {
        list.clear()
        pendingConnections.clear()
    }

    fun size(): Int {
        return list.size
    }

    fun add(gatt: BluetoothGatt) {
        synchronized(list) {
            list.add(gatt)
        }
    }

    fun addPendingConnection(address: String) {
        synchronized(pendingConnections) {
            pendingConnections.add(address)
        }
    }

    fun removePendingConnection(address: String) {
        synchronized(pendingConnections) {
            for (device in pendingConnections) {
                if (device == address) {
                    pendingConnections.remove(device)
                }
            }
        }
    }

    fun isEmpty(): Boolean {
        return list.isEmpty()
    }
}