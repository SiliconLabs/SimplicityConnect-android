package com.siliconlabs.bledemo.ble;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.os.Build;
import android.text.TextUtils;

import com.siliconlabs.bledemo.bluetoothdatamodel.parsing.ScanRecordParser;
import com.siliconlabs.bledemo.utils.Objects;

import java.util.ArrayList;

/**
 * Represents a compatible version of {@link ScanResult} from Lollipop or higher.
 */
public class ScanResultCompat {
    private BluetoothDevice device;
    private int rssi;
    private ScanRecordCompat scanRecord;
    private long timestampNanos;
    private ArrayList<String> advertData;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static ScanResultCompat from(Object lollipopScanResult) {
        if (lollipopScanResult == null) {
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ScanResult sr = (ScanResult)lollipopScanResult;
            ScanResultCompat retVal = new ScanResultCompat();
            retVal.device = sr.getDevice();
            retVal.rssi = sr.getRssi();
            retVal.scanRecord = ScanRecordCompat.from(sr.getScanRecord());
            retVal.advertData = ScanRecordParser.getAdvertisements(sr.getScanRecord().getBytes());
            retVal.timestampNanos = sr.getTimestampNanos();
            return retVal;
        }
        else {
            throw new IllegalStateException("Current OS is not lollipop or higher.");
        }
    }

    public ScanResultCompat() {
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public ScanRecordCompat getScanRecord() {
        return scanRecord;
    }

    public void setScanRecord(ScanRecordCompat scanRecord) {
        this.scanRecord = scanRecord;
    }

    public long getTimestampNanos() {
        return timestampNanos;
    }

    public void setTimestampNanos(long timestampNanos) {
        this.timestampNanos = timestampNanos;
    }

    public String getDisplayName(boolean forSorting) {
        final String name = device.getName(); //it was - final String name = getScanRecord().getDeviceName();
        return !TextUtils.isEmpty(name) ? " " + name : "Unknown"; //TODO It was getDevice().getAddress()
    }

    public ArrayList<String> getAdvertData() {
        return advertData;
    }

    public void setAdvertData(ArrayList<String> advertData) {
        this.advertData = advertData;
    }

    @Override
    public String toString() {
        return "ScanResult{" + "device=" + device + ", scanRecord="
                + Objects.toString(scanRecord) + ", rssi=" + rssi + ", timestampNanos="
                + timestampNanos + '}';
    }
}
