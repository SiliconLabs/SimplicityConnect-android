package com.siliconlabs.bledemo.beaconutils.altbeacon;

import com.siliconlabs.bledemo.beaconutils.BleFormat;

import java.util.Arrays;

import com.siliconlabs.bledemo.ble.BluetoothDeviceInfo;

public class AltBeacon {
    String manufacturerId;
    String altBeaconId;
    byte altBeaconReferenceRssi;
    String deviceAddress;
    int rssi;
    long timestamp = System.currentTimeMillis();
    BluetoothDeviceInfo deviceInfo;

    public AltBeacon(BluetoothDeviceInfo deviceInfo){
        this.deviceInfo = deviceInfo;
        this.deviceAddress = deviceInfo.getAddress();
        this.rssi = deviceInfo.getRssi();
        this.manufacturerId = parseManufacturerId(deviceInfo);
        this.altBeaconId = parseBeaconId(deviceInfo);
        this.altBeaconReferenceRssi = parseBeaconReferenceRssi(deviceInfo);
    }

    public String parseManufacturerId(BluetoothDeviceInfo deviceInfo){
        byte[] bytes = deviceInfo.scanInfo.getScanRecord().getBytes();
        int lengthFlags = bytes[0];
        // length is one byte, value of length is number of bytes to AD LENGTH field
        lengthFlags = lengthFlags + 1;
        int offset = lengthFlags;

        byte[] mfgIdBytes = Arrays.copyOfRange(bytes, offset + 2, offset + 4);

        // reverse the order of the bytes, data received in little endian
        byte lessSignificant = mfgIdBytes[0];
        mfgIdBytes[0] = mfgIdBytes[1];
        mfgIdBytes[1] = lessSignificant;

        String mfgId = BleFormat.bytesToHex(mfgIdBytes);
        return "0x" + mfgId;
    }

    public String parseBeaconId(BluetoothDeviceInfo deviceInfo){
        byte[] bytes = deviceInfo.scanInfo.getScanRecord().getBytes();
        int lengthFlags = bytes[0];
        // length is one byte, value of length is number of bytes to AD LENGTH field
        lengthFlags = lengthFlags + 1;
        int offset = lengthFlags;

        byte[] beaconIdBytes = Arrays.copyOfRange(bytes, offset + 6, offset + 26);

        String beaconId = BleFormat.bytesToHex(beaconIdBytes);
        return "0x" + beaconId;
    }

    public byte parseBeaconReferenceRssi(BluetoothDeviceInfo deviceInfo){
        byte[] bytes = deviceInfo.scanInfo.getScanRecord().getBytes();
        int lengthFlags = bytes[0];
        // length is one byte, value of length is number of bytes to AD LENGTH field
        lengthFlags = lengthFlags + 1;
        int offset = lengthFlags;

        return bytes[offset + 26];
    }

    public String getManufacturerId() {
        return manufacturerId;
    }

    public void setManufacturerId(String manufacturerId) {
        this.manufacturerId = manufacturerId;
    }

    public String getAltBeaconId() {
        return altBeaconId;
    }

    public void setAltBeaconId(String altBeaconId) {
        this.altBeaconId = altBeaconId;
    }

    public byte getAltBeaconReferenceRssi() {
        return altBeaconReferenceRssi;
    }

    public void setAltBeaconReferenceRssi(byte altBeaconReferenceRssi) {
        this.altBeaconReferenceRssi = altBeaconReferenceRssi;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
