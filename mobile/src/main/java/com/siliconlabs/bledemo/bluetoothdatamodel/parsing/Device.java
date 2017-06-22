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
package com.siliconlabs.bledemo.bluetoothdatamodel.parsing;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import com.siliconlabs.bledemo.ble.BluetoothDeviceInfo;
import com.siliconlabs.bledemo.ble.ScanRecordCompat;
import com.siliconlabs.bledemo.ble.ScanResultCompat;

// Device - it's wrapper for BLE device object
public class Device extends BluetoothDeviceInfo {
    private BluetoothGatt bluetoothGatt;

    public Device() {

    }

    public Device(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
        this.device = bluetoothDevice;
        this.hasAdvertDetails = false;
        this.bluetoothGatt = null;
        this.isOfInterest = true;
        ScanResultCompat result = new ScanResultCompat();
        result.setDevice(device);
        result.setRssi(rssi);
        result.setTimestampNanos(System.currentTimeMillis() * 1000);
        result.setScanRecord(ScanRecordCompat.parseFromBytes(scanRecord));
        result.setAdvertData(ScanRecordParser.getAdvertisements(scanRecord));
        this.scanInfo = result;
    }

    @Override
    public BluetoothDeviceInfo clone() {
        final Device retVal;
        retVal = (Device) super.clone();
        retVal.bluetoothGatt = bluetoothGatt;
        retVal.hasAdvertDetails = hasAdvertDetails;
        return retVal;
    }

    public BluetoothDevice getBluetoothDevice() {
        return device;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.device = bluetoothDevice;
    }

    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    public void setBluetoothGatt(BluetoothGatt bluetoothGatt) {
        this.bluetoothGatt = bluetoothGatt;
    }
}
