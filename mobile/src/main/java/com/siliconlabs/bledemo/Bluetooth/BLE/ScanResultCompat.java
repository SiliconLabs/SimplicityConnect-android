package com.siliconlabs.bledemo.Bluetooth.BLE;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.os.Build;
import android.text.TextUtils;

import com.siliconlabs.bledemo.Bluetooth.Parsing.ScanRecordParser;
import com.siliconlabs.bledemo.Utils.Objects;

import java.util.ArrayList;

import static com.siliconlabs.bledemo.Utils.Constants.NA;

/**
 * Represents a compatible version of {@link ScanResult} from Lollipop or higher.
 */
public class ScanResultCompat {
    private BluetoothDevice device;
    private int rssi;
    private ScanRecordCompat scanRecord;
    private long timestampNanos;
    private ArrayList<String> advertData;
    private boolean isConnectable;
    private boolean isLegacy;
    // Data for no legacy devices -- Bluetooth 5 advertising extension --
    private int dataStatus;
    private int primaryPhy;
    private int secondaryPhy;
    private int advertisingSetID;
    private int TxPower;
    private int periodicAdvertisingInterval;

    public static ScanResultCompat from(Object lollipopScanResult) {
        if (lollipopScanResult == null) {
            return null;
        }

        ScanResult sr = (ScanResult) lollipopScanResult;
        ScanResultCompat retVal = new ScanResultCompat();
        retVal.device = sr.getDevice();
        retVal.rssi = sr.getRssi();
        retVal.scanRecord = ScanRecordCompat.from(sr.getScanRecord());
        retVal.advertData = ScanRecordParser.getAdvertisements(sr.getScanRecord().getBytes());
        retVal.timestampNanos = sr.getTimestampNanos();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            retVal.isConnectable = sr.isConnectable();
            retVal.isLegacy = sr.isLegacy();
            retVal.dataStatus = sr.getDataStatus();
            retVal.primaryPhy = sr.getPrimaryPhy();
            retVal.secondaryPhy = sr.getSecondaryPhy();
            retVal.advertisingSetID = sr.getAdvertisingSid();
            retVal.TxPower = sr.getTxPower();
            retVal.periodicAdvertisingInterval = sr.getPeriodicAdvertisingInterval();
        } else {
            retVal.isLegacy = true;
            retVal.isConnectable = true;
        }
        return retVal;

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
        return !TextUtils.isEmpty(name) ? " " + name : NA; //TODO It was getDevice().getAddress()
    }

    public boolean isLegacy() {
        return isLegacy;
    }

    public ArrayList<String> getAdvertData() {
        return advertData;
    }

    public int getPeriodicAdvertisingInterval() {
        return periodicAdvertisingInterval;
    }

    public void setAdvertData(ArrayList<String> advertData) {
        this.advertData = advertData;
    }

    public boolean isConnectable() {
        return this.isConnectable;
    }

    public int getDataStatus() {
        return dataStatus;
    }

    public int getPrimaryPhy() {
        return primaryPhy;
    }

    public int getSecondaryPhy() {
        return secondaryPhy;
    }

    public int getAdvertisingSetID() {
        return advertisingSetID;
    }

    public int getTxPower() {
        return TxPower;
    }

    @Override
    public String toString() {
        return "ScanResult{" + "device=" + device + ", scanRecord="
                + Objects.toString(scanRecord) + ", rssi=" + rssi + ", timestampNanos="
                + timestampNanos + '}';
    }
}
