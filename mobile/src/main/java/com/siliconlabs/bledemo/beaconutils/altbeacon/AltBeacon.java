package com.siliconlabs.bledemo.beaconutils.altbeacon;

import com.siliconlabs.bledemo.beaconutils.BleFormat;
import com.siliconlabs.bledemo.ble.BluetoothDeviceInfo;

import java.util.Arrays;

public class AltBeacon {
    private String manufacturerId;
    private String altBeaconId;
    private byte altBeaconReferenceRssi;
    private String deviceAddress;
    private int rssi;
    private long timestamp = System.currentTimeMillis();
    private BluetoothDeviceInfo deviceInfo;

    public AltBeacon(BluetoothDeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
        this.deviceAddress = deviceInfo.getAddress();
        this.rssi = deviceInfo.getRssi();
        this.manufacturerId = parseManufacturerId(deviceInfo);
        this.altBeaconId = parseBeaconId(deviceInfo);
        this.altBeaconReferenceRssi = parseBeaconReferenceRssi(deviceInfo);
    }

    public String parseManufacturerId(BluetoothDeviceInfo deviceInfo) {
        byte[] bytes = deviceInfo.scanInfo.getScanRecord().getBytes();

        byte[] mfgIdBytes = Arrays.copyOfRange(bytes, 2, 4);

        // reverse the order of the bytes, data received in little endian
        byte lessSignificant = mfgIdBytes[0];
        mfgIdBytes[0] = mfgIdBytes[1];
        mfgIdBytes[1] = lessSignificant;

        String mfgId = BleFormat.bytesToHex(mfgIdBytes);
        return "0x" + mfgId;
    }

    public String parseBeaconId(BluetoothDeviceInfo deviceInfo) {
        byte[] bytes = deviceInfo.scanInfo.getScanRecord().getBytes();

        byte[] beaconIdBytes = Arrays.copyOfRange(bytes, 6, 26);

        String beaconId = BleFormat.bytesToHex(beaconIdBytes);
        return "0x" + beaconId;
    }

    public byte parseBeaconReferenceRssi(BluetoothDeviceInfo deviceInfo) {
        byte[] bytes = deviceInfo.scanInfo.getScanRecord().getBytes();

        return bytes[26];
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
