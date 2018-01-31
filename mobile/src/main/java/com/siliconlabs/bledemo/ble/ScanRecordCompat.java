package com.siliconlabs.bledemo.ble;

import android.annotation.TargetApi;
import android.bluetooth.le.ScanRecord;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Represents a compatible version of {@link ScanRecord} from Lollipop or higher.
 */
public class ScanRecordCompat {
    // The following data type values are assigned by Bluetooth SIG.
    // For more details refer to Bluetooth 4.1 specification, Volume 3, Part C, Section 18.
    private static final int DATA_TYPE_FLAGS = 0x01;
    private static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL = 0x02;
    private static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE = 0x03;
    private static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL = 0x04;
    private static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE = 0x05;
    private static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL = 0x06;
    private static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE = 0x07;
    private static final int DATA_TYPE_LOCAL_NAME_SHORT = 0x08;
    private static final int DATA_TYPE_LOCAL_NAME_COMPLETE = 0x09;
    private static final int DATA_TYPE_TX_POWER_LEVEL = 0x0A;
    private static final int DATA_TYPE_SERVICE_DATA = 0x16;
    private static final int DATA_TYPE_MANUFACTURER_SPECIFIC_DATA = 0xFF;

    private int advertiseFlags;
    private byte[] bytes;
    private static ScanRecord sr;
    private String deviceName;
    private SparseArray<byte[]> manufacturerSpecificData;
    private Map<ParcelUuid,byte[]> serviceData;
    private List<ParcelUuid> serviceUuids;
    private int txPowerLevel;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static ScanRecordCompat from(Object lollipopScanRecord) {
        if (lollipopScanRecord == null) {
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sr = (ScanRecord)lollipopScanRecord;
            ScanRecordCompat retVal = new ScanRecordCompat();
            retVal.advertiseFlags = sr.getAdvertiseFlags();
            retVal.bytes = sr.getBytes();
            retVal.deviceName = sr.getDeviceName();
            retVal.manufacturerSpecificData = sr.getManufacturerSpecificData();
            retVal.serviceData = sr.getServiceData();
            retVal.serviceUuids = sr.getServiceUuids();
            retVal.txPowerLevel = sr.getTxPowerLevel();
            return retVal;

        }
        else {
            throw new IllegalStateException("Current OS is not lollipop or higher.");
        }
    }

    public static ScanRecordCompat parseFromBytes(byte[] scanRecord) {
        if (scanRecord == null) {
            return null;
        }

        int currentPos = 0;
        int advertiseFlag = -1;
        List<ParcelUuid> serviceUuids = new ArrayList<>();
        String localName = null;
        int txPowerLevel = Integer.MIN_VALUE;

        SparseArray<byte[]> manufacturerData = new SparseArray<>();
        Map<ParcelUuid, byte[]> serviceData = new HashMap<>();

        try {
            while (currentPos < scanRecord.length) {
                // length is unsigned int.
                int length = scanRecord[currentPos++] & 0xFF;
                if (length == 0) {
                    break;
                }
                // Note the length includes the length of the field type itself.
                int dataLength = length - 1;
                // fieldType is unsigned int.
                int fieldType = scanRecord[currentPos++] & 0xFF;
                switch (fieldType) {
                    case DATA_TYPE_FLAGS:
                        advertiseFlag = scanRecord[currentPos] & 0xFF;
                        break;
                    case DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL:
                    case DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE:
                        parseServiceUuid(scanRecord, currentPos, dataLength, BluetoothUuid.UUID_BYTES_16_BIT, serviceUuids);
                        break;
                    case DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL:
                    case DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE:
                        parseServiceUuid(scanRecord, currentPos, dataLength, BluetoothUuid.UUID_BYTES_32_BIT, serviceUuids);
                        break;
                    case DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL:
                    case DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE:
                        parseServiceUuid(scanRecord, currentPos, dataLength, BluetoothUuid.UUID_BYTES_128_BIT, serviceUuids);
                        break;
                    case DATA_TYPE_LOCAL_NAME_SHORT:
                    case DATA_TYPE_LOCAL_NAME_COMPLETE:
                        localName = new String(extractBytes(scanRecord, currentPos, dataLength));
                        break;
                    case DATA_TYPE_TX_POWER_LEVEL:
                        txPowerLevel = scanRecord[currentPos];
                        break;
                    case DATA_TYPE_SERVICE_DATA:
                        // The first two bytes of the service data are service data UUID in little
                        // endian. The rest bytes are service data.
                        int serviceUuidLength = BluetoothUuid.UUID_BYTES_16_BIT;
                        byte[] serviceDataUuidBytes = extractBytes(scanRecord, currentPos, serviceUuidLength);
                        ParcelUuid serviceDataUuid = BluetoothUuid.parseUuidFrom(serviceDataUuidBytes);
                        byte[] serviceDataArray = extractBytes(scanRecord, currentPos + serviceUuidLength, dataLength - serviceUuidLength);
                        serviceData.put(serviceDataUuid, serviceDataArray);
                        break;
                    case DATA_TYPE_MANUFACTURER_SPECIFIC_DATA:
                        // The first two bytes of the manufacturer specific data are
                        // manufacturer ids in little endian.
                        int manufacturerId = ((scanRecord[currentPos + 1] & 0xFF) << 8) + (scanRecord[currentPos] & 0xFF);
                        byte[] manufacturerDataBytes = extractBytes(scanRecord, currentPos + 2, dataLength - 2);
                        manufacturerData.put(manufacturerId, manufacturerDataBytes);
                        break;
                    default:
                        // Just ignore, we don't handle such data type.
                        break;
                }
                currentPos += dataLength;
            }

            if (serviceUuids.isEmpty()) {
                serviceUuids = null;
            }
            return new ScanRecordCompat(serviceUuids, manufacturerData, serviceData, advertiseFlag, txPowerLevel, localName, scanRecord);
        } catch (Exception e) {
            Timber.e("unable to parse scan record: " + Arrays.toString(scanRecord), e);
            Log.e("parseFromBytes","unable to parse scan record: " + Arrays.toString(scanRecord) + e);
            // As the record is invalid, ignore all the parsed results for this packet
            // and return an empty record with raw scanRecord bytes in results
            return new ScanRecordCompat(null, null, null, -1, Integer.MIN_VALUE, null, scanRecord);
        }
    }

    ScanRecordCompat() {
    }

    private ScanRecordCompat(List<ParcelUuid> serviceUuids,
                             SparseArray<byte[]> manufacturerData,
                             Map<ParcelUuid,byte[]> serviceData,
                             int advertiseFlag, int txPowerLevel,
                             String deviceName, byte[] bytes) {
        this.serviceUuids = serviceUuids;
        this.manufacturerSpecificData = manufacturerData;
        this.serviceData = serviceData;
        this.advertiseFlags = advertiseFlag;
        this.txPowerLevel = txPowerLevel;
        this.deviceName = deviceName;
        this.bytes = bytes;
    }

    public int getAdvertiseFlags() {
        return advertiseFlags;
    }

    void setAdvertiseFlags(int advertiseFlags) {
        this.advertiseFlags = advertiseFlags;
    }

    public byte[] getBytes() {
        return bytes;
    }

    void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public String getDeviceName() {
        return deviceName;
    }

    void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public SparseArray<byte[]> getManufacturerSpecificData() {
        return manufacturerSpecificData;
    }

    public byte[] getManufacturerSpecificData(int manufacturer) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return sr.getManufacturerSpecificData(manufacturer);
        } else {
            return new byte[0];
        }
    }

    void setManufacturerSpecificData(SparseArray<byte[]> manufacturerSpecificData) {
        this.manufacturerSpecificData = manufacturerSpecificData;
    }

    public Map<ParcelUuid, byte[]> getServiceData() {
        return serviceData;
    }

    void setServiceData(Map<ParcelUuid, byte[]> serviceData) {
        this.serviceData = serviceData;
    }

    public List<ParcelUuid> getServiceUuids() {
        return serviceUuids;
    }

    void setServiceUuids(List<ParcelUuid> serviceUuids) {
        this.serviceUuids = serviceUuids;
    }

    public int getTxPowerLevel() {
        return txPowerLevel;
    }

    void setTxPowerLevel(int txPowerLevel) {
        this.txPowerLevel = txPowerLevel;
    }

    @Override
    public String toString() { //TODO Insert more items in scan record if necessary
        return "ScanRecord [advertiseFlags=" + advertiseFlags + ", serviceUuids=" + serviceUuids
                + ", manufacturerSpecificData=" + toString(manufacturerSpecificData)
                + ", serviceData=" + toString(serviceData)
                + ", txPowerLevel=" + txPowerLevel + ", deviceName=" + deviceName + "]";
    }

    // Parse service UUIDs.
    private static int parseServiceUuid(byte[] scanRecord, int currentPos, int dataLength,
                                        int uuidLength, List<ParcelUuid> serviceUuids) {
        while (dataLength > 0) {
            byte[] uuidBytes = extractBytes(scanRecord, currentPos, uuidLength);
            serviceUuids.add(BluetoothUuid.parseUuidFrom(uuidBytes));
            dataLength -= uuidLength;
            currentPos += uuidLength;
        }
        return currentPos;
    }

    // Helper method to extract bytes from byte array.
    private static byte[] extractBytes(byte[] scanRecord, int start, int length) {
        byte[] bytes = new byte[length];
        System.arraycopy(scanRecord, start, bytes, 0, length);
        return bytes;
    }

    private static String toString(SparseArray<byte[]> array) {
        if (array == null) {
            return "null";
        }
        if (array.size() == 0) {
            return "{}";
        }
        StringBuilder buffer = new StringBuilder();
        buffer.append('{');
        for (int i = 0; i < array.size(); ++i) {
            buffer.append(array.keyAt(i)).append("=").append(Arrays.toString(array.valueAt(i)));
        }
        buffer.append('}');
        return buffer.toString();
    }

    private static <T> String toString(Map<T, byte[]> map) {
        if (map == null) {
            return "null";
        }
        if (map.isEmpty()) {
            return "{}";
        }
        StringBuilder buffer = new StringBuilder();
        buffer.append('{');
        Iterator<Map.Entry<T, byte[]>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<T, byte[]> entry = it.next();
            Object key = entry.getKey();
            buffer.append(key).append("=").append(Arrays.toString(map.get(key)));
            if (it.hasNext()) {
                buffer.append(", ");
            }
        }
        buffer.append('}');
        return buffer.toString();
    }

}
