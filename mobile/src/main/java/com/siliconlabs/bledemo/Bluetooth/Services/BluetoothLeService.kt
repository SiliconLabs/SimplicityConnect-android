/*
 * Bluegiga’s Bluetooth Smart Android SW for Bluegiga BLE modules
 * Contact: support@bluegiga.com.
 *
 * This is free software distributed under the terms of the MIT license reproduced below.
 *
 * Copyright (c) 2013, Bluegiga Technologies
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files ("Software")
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF
 * ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A  PARTICULAR PURPOSE.
 */
package com.siliconlabs.bledemo.Bluetooth.Services

import android.app.Service
import android.bluetooth.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.siliconlabs.bledemo.Bluetooth.Parsing.Device
import com.siliconlabs.bledemo.Bluetooth.Parsing.Engine.Companion.instance

// BluetoothLeService - manages connections and data communication with given Bluetooth LE devices.
class BluetoothLeService : Service() {

    companion object {
        private val TAG = BluetoothLeService::class.java.simpleName

        // These constant members are used to sending and receiving broadcasts
        // between BluetoothLeService and rest parts of application
        const val ACTION_START_SCAN = "com.bluegiga.BLEDemo.ACTION_START_SCAN"
        const val ACTION_STOP_SCAN = "com.bluegiga.BLEDemo.ACTION_STOP_SCAN"
        const val ACTION_DEVICE_DISCOVERED = "com.bluegiga.BLEDemo.ACTION_DEVICE_DISCOVERED"
        const val ACTION_GATT_CONNECTED = "com.bluegiga.BLEDemo.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "com.bluegiga.BLEDemo.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_CONNECTION_STATE_ERROR = "com.bluegiga.BLEDemo.ACTION_GATT_CONNECTION_STATE_ERROR"
        const val ACTION_GATT_SERVICES_DISCOVERED = "com.bluegiga.BLEDemo.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE = "com.bluegiga.BLEDemo.ACTION_DATA_AVAILABLE"
        const val ACTION_DATA_WRITE = "com.bluegiga.BLEDemo.ACTION_DATA_WRITE"
        const val ACTION_READ_REMOTE_RSSI = "com.bluegiga.BLEDemo.ACTION_READ_REMOTE_RSSI"
        const val ACTION_DESCRIPTOR_WRITE = "com.bluegiga.BLEDemo.ACTION_DESCRIPTOR_WRITE"

        // These constant members are used to sending and receiving extras from
        // broadcast intents
        const val SCAN_PERIOD = "scanPeriod"
        const val DISCOVERED_DEVICE = "discoveredDevice"
        const val DEVICE = "device"
        const val DEVICE_ADDRESS = "deviceAddress"
        const val RSSI = "rssi"
        const val UUID_CHARACTERISTIC = "uuidCharacteristic"
        const val UUID_DESCRIPTOR = "uuidDescriptor"
        const val GATT_STATUS = "gattStatus"
        const val SCAN_RECORD = "scanRecord"
    }

    private var mBluetoothAdapter: BluetoothAdapter? = null
    private val mBinder: IBinder = LocalBinder()
    var connectedDevice: Device? = null
        private set

    // Implements callback methods for GATT events that the app cares about.
    // For example,
    // connection status has changed, services are discovered,etc...
    private val mGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        // Called when device has changed connection status and appropriate
        // broadcast with device address extra is sent
        // It can be either connected or disconnected state
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    val device = instance?.getDevice(gatt)
                    device?.setConnected(true)
                    connectedDevice = device
                    val updateIntent = Intent(ACTION_GATT_CONNECTED)
                    updateIntent.putExtra(DEVICE_ADDRESS, device?.address)
                    sendBroadcast(updateIntent)
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    val device = instance?.getDevice(gatt)
                    device?.setConnected(false)
                    val updateIntent = Intent(ACTION_GATT_DISCONNECTED)
                    updateIntent.putExtra(DEVICE_ADDRESS, device?.address)
                    if (device == connectedDevice) {
                        connectedDevice = null
                    }
                    sendBroadcast(updateIntent)
                }
            } else {
                val device = instance?.getDevice(gatt)
                val updateIntent = Intent(ACTION_GATT_CONNECTION_STATE_ERROR)
                updateIntent.putExtra(DEVICE_ADDRESS, device?.address)
                sendBroadcast(updateIntent)
            }
            Log.i("BLE service", "onConnectionStateChange - status: $status - new state: $newState")
        }

        // Called when services are discovered on remote device
        // If success broadcast with device address extra is sent
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val device = instance?.getDevice(gatt)
                val updateIntent = Intent(ACTION_GATT_SERVICES_DISCOVERED)
                updateIntent.putExtra(DEVICE_ADDRESS, device?.address)
                sendBroadcast(updateIntent)
            }
            Log.i("BLE service", "onServicesDiscovered - status: $status")
        }

        // Called when characteristic was read
        // Broadcast with characteristic uuid and status is sent
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            val updateIntent = Intent(ACTION_DATA_AVAILABLE)
            updateIntent.putExtra(UUID_CHARACTERISTIC, characteristic.uuid.toString())
            updateIntent.putExtra(GATT_STATUS, status)
            sendBroadcast(updateIntent)
            Log.i("BLE service", "onCharacteristicRead - status: " + status + "  - UUID: " + characteristic.uuid)
        }

        // Called when characteristic was written
        // Broadcast with characteristic uuid and status is sent
        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            val updateIntent = Intent(ACTION_DATA_WRITE)
            updateIntent.putExtra(UUID_CHARACTERISTIC, characteristic.uuid.toString())
            updateIntent.putExtra(GATT_STATUS, status)
            sendBroadcast(updateIntent)
            Log.i("BLE service", "onCharacteristicWrite - status: " + status + "  - UUID: " + characteristic.uuid)
        }

        // Called when remote device rssi was read
        // If success broadcast with device address extra is sent
        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val device = instance?.getDevice(gatt)
                device?.rssi = rssi
                val updateIntent = Intent(ACTION_READ_REMOTE_RSSI)
                updateIntent.putExtra(DEVICE_ADDRESS, device?.address)
                sendBroadcast(updateIntent)
            }
            Log.i("BLE service", "onReadRemoteRssi - status: $status")
        }

        // Called when descriptor was written
        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            val updateIntent = Intent(ACTION_DESCRIPTOR_WRITE)
            updateIntent.putExtra(GATT_STATUS, status)
            updateIntent.putExtra(UUID_DESCRIPTOR, descriptor.uuid)
            sendBroadcast(updateIntent)
            Log.i("BLE service", "onDescriptorWrite - status: " + status + "  - UUID: " + descriptor.uuid)
        }

        // Called when notification has been sent from remote device
        // Broadcast with characteristic uuid is sent
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val updateIntent = Intent(ACTION_DATA_AVAILABLE)
            updateIntent.putExtra(UUID_CHARACTERISTIC, characteristic.uuid.toString())
            sendBroadcast(updateIntent)
            Log.i("BLE service", "onCharacteristicChanged - status: " + "  - UUID: " + characteristic.uuid)
        }
    }

    inner class LocalBinder : Binder() {
        val service: BluetoothLeService
            get() = this@BluetoothLeService
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // After using a given device, you should make sure that
        // BluetoothGatt.close() is called
        // such that resources are cleaned up properly. In this particular
        // example, close() is
        // invoked when the UI is disconnected from the Service.
        close()
        return super.onUnbind(intent)
    }

    // -----------------------------------------------------------------------
    // Following methods are available from app and they operate tasks related
    // to Bluetooth Low Energy technology
    // -----------------------------------------------------------------------
    // Connects to given device
    fun connect(device: Device?): Boolean {
        if (mBluetoothAdapter == null || device == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.")
            return false
        }
        if (connectedDevice != null) {
            disconnect(connectedDevice!!)
        }

        // If BluetoothGatt object is null, creates new object
        // else calls connect function on this object
        if (device.bluetoothGatt == null) {
            val bluetoothGatt = device.getBluetoothDevice().connectGatt(this, false, mGattCallback)
            device.bluetoothGatt = bluetoothGatt
        } else {
            device.bluetoothGatt?.connect()
        }
        return true
    }

    // Disconnects from given device
    fun disconnect(device: Device) {
        device.bluetoothGatt?.disconnect()
    }

    // Close all established connections
    fun close() {
        for (device in instance?.devices!!) {
            device.bluetoothGatt?.close()
            device.bluetoothGatt = null
        }
    }
}