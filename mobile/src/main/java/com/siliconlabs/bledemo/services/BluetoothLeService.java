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

package com.siliconlabs.bledemo.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.siliconlabs.bledemo.bluetoothdatamodel.parsing.Device;
import com.siliconlabs.bledemo.bluetoothdatamodel.parsing.Engine;

// BluetoothLeService - manages connections and data communication with given Bluetooth LE devices.
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private boolean mScanning;
    private Handler mHandler;
    private final IBinder mBinder = new LocalBinder();
    private Device connectedDevice;

    // These constant members are used to sending and receiving broadcasts
    // between BluetoothLeService and rest parts of application
    public static final String ACTION_START_SCAN = "com.bluegiga.BLEDemo.ACTION_START_SCAN";
    public static final String ACTION_STOP_SCAN = "com.bluegiga.BLEDemo.ACTION_STOP_SCAN";
    public static final String ACTION_DEVICE_DISCOVERED = "com.bluegiga.BLEDemo.ACTION_DEVICE_DISCOVERED";
    public static final String ACTION_GATT_CONNECTED = "com.bluegiga.BLEDemo.ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_DISCONNECTED = "com.bluegiga.BLEDemo.ACTION_GATT_DISCONNECTED";
    public static final String ACTION_GATT_CONNECTION_STATE_ERROR = "com.bluegiga.BLEDemo.ACTION_GATT_CONNECTION_STATE_ERROR";
    public static final String ACTION_GATT_SERVICES_DISCOVERED = "com.bluegiga.BLEDemo.ACTION_GATT_SERVICES_DISCOVERED";
    public static final String ACTION_DATA_AVAILABLE = "com.bluegiga.BLEDemo.ACTION_DATA_AVAILABLE";
    public static final String ACTION_DATA_WRITE = "com.bluegiga.BLEDemo.ACTION_DATA_WRITE";
    public static final String ACTION_READ_REMOTE_RSSI = "com.bluegiga.BLEDemo.ACTION_READ_REMOTE_RSSI";
    public static final String ACTION_DESCRIPTOR_WRITE = "com.bluegiga.BLEDemo.ACTION_DESCRIPTOR_WRITE";

    // These constant members are used to sending and receiving extras from
    // broadcast intents
    public static final String SCAN_PERIOD = "scanPeriod";
    public static final String DISCOVERED_DEVICE = "discoveredDevice";
    public static final String DEVICE = "device";
    public static final String DEVICE_ADDRESS = "deviceAddress";
    public static final String RSSI = "rssi";
    public static final String UUID_CHARACTERISTIC = "uuidCharacteristic";
    public static final String UUID_DESCRIPTOR = "uuidDescriptor";
    public static final String GATT_STATUS = "gattStatus";
    public static final String SCAN_RECORD = "scanRecord";

    // Implements callback method for scan BLE devices
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        // Called when new BLE device is discovered
        // Broadcast intent is sent with following extras: device , rssi,
        // additional advertise data
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

            Intent broadcastIntent = new Intent(ACTION_DEVICE_DISCOVERED);
            broadcastIntent.putExtra(DISCOVERED_DEVICE, device);
            broadcastIntent.putExtra(RSSI, rssi);
            broadcastIntent.putExtra(SCAN_RECORD, scanRecord);

            sendBroadcast(broadcastIntent);
            Log.i("BLE service", "onLeScan - device: " + device.getAddress() + " - rssi: " + rssi);
        }
    };

    // Implements callback methods for GATT events that the app cares about.
    // For example,
    // connection status has changed, services are discovered,etc...
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        // Called when device has changed connection status and appropriate
        // broadcast with device address extra is sent
        // It can be either connected or disconnected state
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Device device = Engine.getInstance().getDevice(gatt);
                    device.setConnected(true);
                    connectedDevice = device;
                    Intent updateIntent = new Intent(ACTION_GATT_CONNECTED);
                    updateIntent.putExtra(DEVICE_ADDRESS, device.getAddress());
                    sendBroadcast(updateIntent);
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Device device = Engine.getInstance().getDevice(gatt);
                    device.setConnected(false);
                    Intent updateIntent = new Intent(ACTION_GATT_DISCONNECTED);
                    updateIntent.putExtra(DEVICE_ADDRESS, device.getAddress());
                    if (device.equals(connectedDevice)) {
                        connectedDevice = null;
                    }
                    sendBroadcast(updateIntent);
                }
            } else {
                Device device = Engine.getInstance().getDevice(gatt);
                Intent updateIntent = new Intent(ACTION_GATT_CONNECTION_STATE_ERROR);
                updateIntent.putExtra(DEVICE_ADDRESS, device.getAddress());
                sendBroadcast(updateIntent);
            }
            Log.i("BLE service", "onConnectionStateChange - status: " + status + " - new state: " + newState);

        }

        // Called when services are discovered on remote device
        // If success broadcast with device address extra is sent
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Device device = Engine.getInstance().getDevice(gatt);
                Intent updateIntent = new Intent(ACTION_GATT_SERVICES_DISCOVERED);
                updateIntent.putExtra(DEVICE_ADDRESS, device.getAddress());
                sendBroadcast(updateIntent);
            }
            Log.i("BLE service", "onServicesDiscovered - status: " + status);
        }

        // Called when characteristic was read
        // Broadcast with characteristic uuid and status is sent
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Intent updateIntent = new Intent(ACTION_DATA_AVAILABLE);
            updateIntent.putExtra(UUID_CHARACTERISTIC, characteristic.getUuid().toString());
            updateIntent.putExtra(GATT_STATUS, status);
            sendBroadcast(updateIntent);
            Log.i("BLE service", "onCharacteristicRead - status: " + status + "  - UUID: " + characteristic.getUuid());
        }

        // Called when characteristic was written
        // Broadcast with characteristic uuid and status is sent
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Intent updateIntent = new Intent(ACTION_DATA_WRITE);
            updateIntent.putExtra(UUID_CHARACTERISTIC, characteristic.getUuid().toString());
            updateIntent.putExtra(GATT_STATUS, status);
            sendBroadcast(updateIntent);
            Log.i("BLE service", "onCharacteristicWrite - status: " + status + "  - UUID: " + characteristic.getUuid());
        }

        // Called when remote device rssi was read
        // If success broadcast with device address extra is sent
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Device device = Engine.getInstance().getDevice(gatt);
                device.setRssi(rssi);
                Intent updateIntent = new Intent(ACTION_READ_REMOTE_RSSI);
                updateIntent.putExtra(DEVICE_ADDRESS, device.getAddress());
                sendBroadcast(updateIntent);
            }
            Log.i("BLE service", "onReadRemoteRssi - status: " + status);
        }

        // Called when descriptor was written
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Intent updateIntent = new Intent(ACTION_DESCRIPTOR_WRITE);
            updateIntent.putExtra(GATT_STATUS, status);
            updateIntent.putExtra(UUID_DESCRIPTOR, descriptor.getUuid());
            sendBroadcast(updateIntent);
            Log.i("BLE service", "onDescriptorWrite - status: " + status + "  - UUID: " + descriptor.getUuid());
        }

        // Called when notification has been sent from remote device
        // Broadcast with characteristic uuid is sent
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Intent updateIntent = new Intent(ACTION_DATA_AVAILABLE);
            updateIntent.putExtra(UUID_CHARACTERISTIC, characteristic.getUuid().toString());
            sendBroadcast(updateIntent);
            Log.i("BLE service", "onCharacteristicChanged - status: " + "  - UUID: " + characteristic.getUuid());
        }
    };

    // Starts scanning for new BLE devices
    public void startScanning(final int scanPeriod) {

        mHandler.postDelayed(new Runnable() {
            // Called after scanPeriod milliseconds elapsed
            // It stops scanning and sends broadcast
            @Override
            public void run() {
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);

                Intent broadcastIntent = new Intent(ACTION_STOP_SCAN);
                sendBroadcast(broadcastIntent);
            }
        }, scanPeriod);
        mScanning = true;
        mBluetoothAdapter.startLeScan(mLeScanCallback);

    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that
        // BluetoothGatt.close() is called
        // such that resources are cleaned up properly. In this particular
        // example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    // Initializes class members. It is called only once when application is
    // starting.
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter
        // through BluetoothManager.

        mHandler = new Handler();
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    // -----------------------------------------------------------------------
    // Following methods are available from app and they operate tasks related
    // to Bluetooth Low Energy technology
    // -----------------------------------------------------------------------

    // Connects to given device
    public boolean connect(Device device) {
        if (mBluetoothAdapter == null || device == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        if (connectedDevice != null) {
            disconnect(connectedDevice);
        }

        // If BluetoothGatt object is null, creates new object
        // else calls connect function on this object
        if (device.getBluetoothGatt() == null) {
            BluetoothGatt bluetoothGatt = device.getBluetoothDevice().connectGatt(this, false, mGattCallback);
            device.setBluetoothGatt(bluetoothGatt);
        } else {
            device.getBluetoothGatt().connect();
        }

        return true;
    }

    // Disconnects from given device
    public void disconnect(Device device) {
        device.getBluetoothGatt().disconnect();
    }

    // Reads value for given characteristic
    public void readCharacteristic(Device device, BluetoothGattCharacteristic charact) {
        device.getBluetoothGatt().readCharacteristic(charact);
    }

    // Writes value for given characteristic
    public boolean writeCharacteristic(Device device, BluetoothGattCharacteristic charact) {
        return device.getBluetoothGatt().writeCharacteristic(charact);
    }

    // Enables or disables characteristic notification
    public boolean setCharacteristicNotification(Device device, BluetoothGattCharacteristic charact, boolean enabled) {
        return device.getBluetoothGatt().setCharacteristicNotification(charact, enabled);
    }

    // Writes value for given descriptor
    public boolean writeDescriptor(Device device, BluetoothGattDescriptor descriptor) {
        return device.getBluetoothGatt().writeDescriptor(descriptor);
    }

    // Reads rssi for given device
    public boolean readRemoteRssi(Device device) {
        return device.getBluetoothGatt().readRemoteRssi();
    }

    // Close all established connections
    public void close() {
        for (Device device : Engine.getInstance().getDevices()) {
            if (device.getBluetoothGatt() != null) {
                device.getBluetoothGatt().close();
                device.setBluetoothGatt(null);
            }
        }
    }

    // Checks if service is currently scanning for new BLE devices
    public boolean isScanning() {
        return mScanning;
    }

    public Device getConnectedDevice() {
        return connectedDevice;
    }
}
