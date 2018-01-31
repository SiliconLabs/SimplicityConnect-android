package com.siliconlabs.bledemo.ble;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.siliconlabs.bledemo.beaconutils.BleFormat;

import java.util.ArrayList;

public class BluetoothDeviceInfo implements Cloneable {
    public BluetoothDevice device;
    public boolean isOfInterest;
    public boolean isNotOfInterest;
    public boolean serviceDiscoveryFailed;
    private BleFormat bleFormat = null;
    protected boolean hasAdvertDetails;
    public static int MAX_EXTRA_DATA = 3;
    private boolean connected;

    Object gattHandle;
    public boolean areServicesBeingDiscovered;

    public ScanResultCompat scanInfo;

    public BluetoothDeviceInfo() {

    }
    public boolean hasUnknownStatus() {
        return (!serviceDiscoveryFailed && !isNotOfInterest && !isOfInterest);
    }

    boolean isUnDiscovered() {
        return (gattHandle == null) && hasUnknownStatus();
    }

    void discover(BluetoothLEGatt bluetoothLEGatt) {
        serviceDiscoveryFailed = isNotOfInterest = isOfInterest = false;
        gattHandle = bluetoothLEGatt;
    }

    @Override
    public BluetoothDeviceInfo clone()  {
        final BluetoothDeviceInfo retVal;
        try {
            retVal = (BluetoothDeviceInfo)super.clone();
            retVal.device = device;
            retVal.scanInfo = scanInfo;
            retVal.isOfInterest = isOfInterest;
            retVal.isNotOfInterest = isNotOfInterest;
            retVal.serviceDiscoveryFailed = serviceDiscoveryFailed;
            retVal.bleFormat = bleFormat;
            retVal.gattHandle = null;
            retVal.connected = connected;
            retVal.hasAdvertDetails = hasAdvertDetails;
            retVal.areServicesBeingDiscovered = areServicesBeingDiscovered;
            return retVal;
        } catch (CloneNotSupportedException e) {
            Log.e("clone","Could not clone" + e);
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BluetoothDeviceInfo)) {
            return false;
        }

        final BluetoothDeviceInfo that = (BluetoothDeviceInfo) o;
        return device.equals(that.device) && (isOfInterest == that.isOfInterest) && (isNotOfInterest == that.isNotOfInterest) && (serviceDiscoveryFailed == that.serviceDiscoveryFailed);
    }

    @Override
    public int hashCode() {
        return device.hashCode();
    }

    @Override
    public String toString() {
        return scanInfo.toString();
    }

    public BleFormat getBleFormat() {
        if (bleFormat == null) {
            bleFormat = BleFormat.getFormat(this);
        }
        return bleFormat;
    }

    public void setBleFormat(BleFormat bleFormat) {
        this.bleFormat = bleFormat;
    }

    public void setAdvertData(ArrayList<String> advertisements) {
        if (scanInfo != null) {
            scanInfo.setAdvertData(advertisements);
        }
    }

    public ArrayList<String> getAdvertData() {
        if (scanInfo != null) {
            return scanInfo.getAdvertData();
        }
        return new ArrayList<>();
    }

    public boolean hasAdvertDetails() {
        return hasAdvertDetails || getAdvertData().size() > MAX_EXTRA_DATA;
    }
    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public int getRssi() {
        return scanInfo.getRssi();
    }

    public void setRssi(int rssi) {
        this.scanInfo.setRssi(rssi);
    }

    public String getName() {
        return device.getName();
    }

    public String getAddress() {
        return device.getAddress();
    }
}
