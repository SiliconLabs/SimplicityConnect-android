package com.siliconlabs.bledemo.rangetest;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.*;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.Collections;
import java.util.List;

/**
 * @author Comarch S.A.
 */

public abstract class RangeTestAdvertisementHandler {

    private final BluetoothAdapter bluetoothAdapter;
    private final String address;

    private Object listener;

    public RangeTestAdvertisementHandler(Context context, String address) {
        if (address == null) throw new IllegalArgumentException("Address cannot be null");

        this.bluetoothAdapter = getBluetoothAdapter(context);
        this.address = address;
    }

    public synchronized void startListening() {
        if (listener != null) {
            stopListening();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ScanFilter filter = new ScanFilter.Builder()
                    .setDeviceAddress(address)
                    .build();

            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0)
                    .build();

            RangeTestAdvertisementListenerPost21 advertisementListener = new RangeTestAdvertisementListenerPost21();

            bluetoothAdapter.getBluetoothLeScanner().startScan(
                    Collections.singletonList(filter),
                    scanSettings,
                    advertisementListener
            );

            listener = advertisementListener;
        } else {
            RangeTestAdvertisementListenerPre21 advertisementListener = new RangeTestAdvertisementListenerPre21();
            bluetoothAdapter.startLeScan(advertisementListener);
            listener = advertisementListener;
        }
    }

    public synchronized void stopListening() {
        if (listener == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            RangeTestAdvertisementListenerPost21 advertisementListener = (RangeTestAdvertisementListenerPost21) listener;
            bluetoothAdapter.getBluetoothLeScanner().stopScan(advertisementListener);
        } else {
            RangeTestAdvertisementListenerPre21 advertisementListener = (RangeTestAdvertisementListenerPre21) listener;
            bluetoothAdapter.stopLeScan(advertisementListener);
        }

        listener = null;
    }

    public String getAddress() {
        return address;
    }

    protected abstract void handleAdvertisementRecord(int manufacturerData, int companyId, int structureType, int rssi, int packetCount, int packetReceived);

    private synchronized void handleDeviceAdvertisement(BluetoothDevice device, byte[] record) {
        String address = device.getAddress();

        if (this.address.equals(address)) {
            decodeAndNotify(record);
        }
    }

    private void decodeAndNotify(byte[] record) {
        int manufacturerData = record[13];
        int companyId = unsignedIntFromLittleEndian(record[15], record[14]);
        int structureType = record[16];

        int rssi = record[17];
        int packetCount = unsignedIntFromLittleEndian(record[19], record[18]);
        int packetReceived = unsignedIntFromLittleEndian(record[21], record[20]);

        Log.d("RangeAdvData", String.format("Address: %s, M: %d, CID: %d, T: %d, RSSI: %d, PC: %d, PR: %d",
                getAddress(), manufacturerData, companyId, structureType, rssi, packetCount, packetReceived));

        handleAdvertisementRecord(manufacturerData, companyId, structureType, rssi, packetCount, packetReceived);
    }

    private int unsignedIntFromLittleEndian(byte... bytes) {
        int value = 0;
        for (byte b : bytes) {
            value = (value << 8) | (b & 0xFF);
        }
        return value;
    }

    private BluetoothAdapter getBluetoothAdapter(Context context) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        if (bluetoothManager == null) {
            throw new UnsupportedOperationException();
        }

        return bluetoothManager.getAdapter();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private class RangeTestAdvertisementListenerPost21 extends ScanCallback {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            handleScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                handleScanResult(result);
            }
        }

        private void handleScanResult(ScanResult result) {
            BluetoothDevice device = result.getDevice();
            ScanRecord record = result.getScanRecord();

            if (record != null) {
                handleDeviceAdvertisement(device, record.getBytes());
            }
        }
    }

    private class RangeTestAdvertisementListenerPre21 implements BluetoothAdapter.LeScanCallback {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            handleDeviceAdvertisement(device, scanRecord);
        }
    }
}
