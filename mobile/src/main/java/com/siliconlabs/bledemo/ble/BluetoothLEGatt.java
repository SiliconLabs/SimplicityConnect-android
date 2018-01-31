package com.siliconlabs.bledemo.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import com.siliconlabs.bledemo.BuildConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Handles the BLE communication by serializing connections, discoveries, requests and responses.
 */
class BluetoothLEGatt {
    static final SparseArray<GattService> GATT_SERVICE_DESCS = new SparseArray<>();
    static final SparseArray<GattCharacteristic> GATT_CHARACTER_DESCS = new SparseArray<>();

    // If a gatt's service scan request is pending, cancel it after SCAN_CONNECTION_TIMEOUT milliseconds if there is no answer/response yet.
    private static final int SCAN_CONNECTION_TIMEOUT = 10000; //TODO It was 10000
    // When reading a remote value from a gatt device, never wait longer than GATT_READ_TIMEOUT to obtain a value.
    private static final int GATT_READ_TIMEOUT = 8000; //TODO It was 2

    private static ScheduledExecutorService SYNCHRONIZED_GATT_ACCESS_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    protected final BluetoothDevice device;
    protected List<BluetoothGattService> interestingServices;
    protected volatile boolean isOfInterest;

    private final Context context;
    private final BluetoothGattCallback gattCallback;
    private final Runnable gattScanTimeout = new Runnable() {
        @Override
        public void run() {
            setGattServices(null);
            close();
        }
    };
    private ScheduledFuture<?> scanTimeoutFuture;

    private BluetoothGatt gatt;

    private final Characteristics characteristics = new Characteristics();

    static void cancelAll(final Collection<BluetoothLEGatt> leGatts) {
        SYNCHRONIZED_GATT_ACCESS_EXECUTOR.submit(new Runnable() {
            @Override
            public void run() {
                for (BluetoothLEGatt leGatt : leGatts) {
                    if (leGatt != null) {
                        leGatt.close();
                    }
                }
            }
        });
    }

    BluetoothLEGatt(final Context context, BluetoothDevice pDevice) {
        this.context = context;
        this.device = pDevice;

        gattCallback = new BluetoothGattCallback() {
            // for Gatt Status codes:
            // https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/android-4.3_r1.1/stack/include/gatt_api.h

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.i("onConnectionStateChange","failed with status: " + status + " (newState =" + newState + ") " + device.getAddress());
                    Timber.w("onConnectionStateChange failed with status " + status + " (newState =" + newState + ") " + device.getAddress());
                    SYNCHRONIZED_GATT_ACCESS_EXECUTOR.submit(new Runnable() {
                        @Override
                        public void run() {
                            setGattServices(null);

                        }
                    });
                    return;
                }
                Log.i("onConnectionStateChange","(newState =" + newState + ") " + device.getAddress());
                Timber.w("onConnectionStateChange (newState =" + newState + ") " + device.getAddress());
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        SYNCHRONIZED_GATT_ACCESS_EXECUTOR.submit(new Runnable() {
                            @Override
                            public void run() {
                                discoverServices();
                            }
                        });
                        break;

                    case BluetoothProfile.STATE_DISCONNECTED:
                        SYNCHRONIZED_GATT_ACCESS_EXECUTOR.submit(new Runnable() {
                            @Override
                            public void run() {
                                setGattServices(null);
                            }
                        });
                        break;
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i("onServicesDiscovered","onServicesDiscovered " + device.getAddress());
                    Timber.w("onServicesDiscovered " + device.getAddress());
                    SYNCHRONIZED_GATT_ACCESS_EXECUTOR.submit(new Runnable() {
                        @Override
                        public void run() {
                            displayGattServices();
                        }
                    });
                } else {
                    Log.i("onServicesDiscovered","onServicesDiscovered received: " + status + " " + device.getAddress());
                    Timber.w("onServicesDiscovered received: " + status + " " + device.getAddress());
                    SYNCHRONIZED_GATT_ACCESS_EXECUTOR.submit(new Runnable() {
                        @Override
                        public void run() {
                            setGattServices(null);
                        }
                    });
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i("onCharacteristicRead","Char " + getCharacteristicName(characteristic.getUuid()) + " <- " + getCharacteristicsValue(characteristic));
                    Timber.d("Char " + getCharacteristicName(characteristic.getUuid()) + " <- " + getCharacteristicsValue(characteristic));
                    characteristics.onRead(getIdentification(characteristic.getUuid()), getCharacteristicsValue(characteristic), true);
                } else {
                    Timber.w("onCharacteristicRead received: " + status);
                    Log.i("onCharacteristicRead","onCharacteristicRead received: " + status);
                    characteristics.onRead(getIdentification(characteristic.getUuid()), null, false);
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    //Log.d("onCharacteristcWrite","Char " + getCharacteristicsValue(characteristic) + " -> " + getCharacteristicName(characteristic.getUuid()));
                    Timber.d("Char " + getCharacteristicsValue(characteristic) + " -> " + getCharacteristicName(characteristic.getUuid()));
                } else {
                    //Log.e("onCharacteristcWrite","onCharacteristicWrite received: " + status);
                    Timber.w("onCharacteristicWrite received: " + status);
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                notifyCharacteristicChanged(getIdentification(characteristic.getUuid()), getCharacteristicsValue(characteristic));
            }

        };

        /*
         * Connect --OK--> Discover --OK--> displayServices & addCharacteristics
         *         |                |
         *         |                +-Err-> reportNoServices
         *         |
         *         +-Err-> reportNoServices
         *
         * Disconnect ---> reportNoServices
         */
        SYNCHRONIZED_GATT_ACCESS_EXECUTOR.submit(new Runnable() {
            @Override
            public void run() {
                connect();
            }
        });
    }

    private void connect() {
        final boolean connectionAttemptFailed;
        synchronized (this) {
            if (gatt != null) {
                return;
            }
            Log.e("connect()","Gatt connect for " + device.getAddress());
            Timber.d("Gatt connect for " + device.getAddress());
            scanTimeoutFuture = SYNCHRONIZED_GATT_ACCESS_EXECUTOR.schedule(gattScanTimeout, SCAN_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);

            gatt = device.connectGatt(context, false, gattCallback);
            connectionAttemptFailed = (gatt == null);
        }

        if (connectionAttemptFailed) {
            setGattServices(null);
        }
    }

    private boolean discoverServices() {
        final boolean hasServicesCached;
        final boolean discoverStartSucceeded;

        synchronized (this) {
            if (gatt == null) {
                return false;
            }

            //hasServicesCached = !gatt.getServices().isEmpty(); //TODO Uncomment - Trying to awayes discover services
            discoverStartSucceeded = /*hasServicesCached ||*/ gatt.discoverServices();
        }
        /*
        if (hasServicesCached) {
            displayGattServices();
            return true;
        }*/

        if (!discoverStartSucceeded) {
            setGattServices(null);
            return false;
        }

        return true;
    }

    private void displayGattServices() {
        List<BluetoothGattService> gattServices;
        synchronized (this) {
            if (gatt == null) {
                return;
            }

            gattServices = gatt.getServices();
            if (gattServices == null) {
                gattServices = new ArrayList<>();
            }

            boolean hasGattServices = !gattServices.isEmpty();
            if (hasGattServices) {
                characteristics.clearCharacteristics();

                // Loops through available GATT Services.
                for (BluetoothGattService gattService : gattServices) {
                    Timber.d("Device has service: " + getServiceName(gattService.getUuid()));
                    Log.e("displayGattServices()","Device has service: " + getServiceName(gattService.getUuid()));
                    characteristics.addCharacteristics(gatt, gattService.getCharacteristics());
                    Timber.d("==========================\n");
                    Log.e("displayGattServices()","==========================\n");
                }
            }
        }

        setGattServices(gattServices);
    }

    protected void setGattServices(List<BluetoothGattService> services) {
        Timber.d("setGattServices for " + device.getAddress());
        //Log.d("displayGattServices()","setGattServices for " + device.getAddress());

        if (scanTimeoutFuture != null) {
            scanTimeoutFuture.cancel(false);
            scanTimeoutFuture = null;
        }

        if (services == null) {
            interestingServices = null;
            isOfInterest = false;
            return;
        }

        interestingServices = new ArrayList<>();
        for (BluetoothGattService service : services) {
            interestingServices.add(service);
        }
        isOfInterest = !interestingServices.isEmpty();
    }

    protected void notifyCharacteristicChanged(int characteristicID, Object value) {
    }

    synchronized BluetoothGatt getGatt() {
        return gatt;
    }

    protected synchronized void close() {
        if (gatt == null) {
            return;
        }

        try {
            gatt.close();
            gatt = null;
        } catch (Exception e) {
            Timber.w(e, "Error closing Gatt");
            Log.e("displayGattServices()","Error closing Gatt: " + e);

        }
    }

    public void cancel() {
        SYNCHRONIZED_GATT_ACCESS_EXECUTOR.submit(new Runnable() {
            @Override
            public void run() {
                close();
            }
        });
    }

    void read(final int characteristicID) {
        SYNCHRONIZED_GATT_ACCESS_EXECUTOR.submit(new Runnable() {
            @Override
            public void run() {
                Object value = characteristics.read(characteristicID);
                notifyCharacteristicChanged(characteristicID, value);
            }
        });
    }

    synchronized boolean representsConnectedDevice(BluetoothDevice btDevice) {
        return (gatt != null) && device.equals(btDevice);
    }

    private static String getServiceName(UUID uuid) {
        GattService service = GATT_SERVICE_DESCS.get(getIdentification(uuid));
        return (service != null)? service.type : uuid.toString();
    }

    private static String getCharacteristicName(UUID uuid) {
        GattCharacteristic characteristic = GATT_CHARACTER_DESCS.get(getIdentification(uuid));
        return (characteristic != null)? characteristic.type : uuid.toString();
    }

    private static Object getCharacteristicsValue(BluetoothGattCharacteristic gattCharacteristic) {
        if (gattCharacteristic.getValue() == null) {
            return null;
        }

        GattCharacteristic characteristic = GATT_CHARACTER_DESCS.get(getIdentification(gattCharacteristic.getUuid()));
        if (characteristic == null) {
            return gattCharacteristic.getValue();
        }

        final int format = characteristic.format;
        switch(format) {
            case BluetoothGattCharacteristic.FORMAT_UINT8:
            case BluetoothGattCharacteristic.FORMAT_UINT16:
            case BluetoothGattCharacteristic.FORMAT_UINT32:
            case BluetoothGattCharacteristic.FORMAT_SINT8:
            case BluetoothGattCharacteristic.FORMAT_SINT16:
            case BluetoothGattCharacteristic.FORMAT_SINT32:
                return gattCharacteristic.getIntValue(format, 0);

            case BluetoothGattCharacteristic.FORMAT_FLOAT:
            case BluetoothGattCharacteristic.FORMAT_SFLOAT:
                return gattCharacteristic.getFloatValue(format, 0);

            case 0:
                final String value = gattCharacteristic.getStringValue(0);
                final int firstNullCharPos = value.indexOf('\u0000');
                return (firstNullCharPos >= 0)? value.substring(0, firstNullCharPos) : value;

            default:
                return characteristic.createValue(gattCharacteristic);
        }
    }

    private static int getIdentification(UUID uuid) {
        return (int)(uuid.getMostSignificantBits() >>> 32);
    }

    /**
     * Handles reading of characteristic values. All its methods, except the {@link #onRead(int, Object, boolean)} method, must
     * be executed on the {@link #SYNCHRONIZED_GATT_ACCESS_EXECUTOR}, so access to the gatt is serialized.
     */
    class Characteristics {
        final SparseArray<BluetoothGattCharacteristic> gattCharacteristics = new SparseArray<>();
        final SparseArray<Object> values = new SparseArray<>();

        void clearCharacteristics() {
            gattCharacteristics.clear();
            values.clear();
        }

        void addCharacteristics(BluetoothGatt gatt, List<BluetoothGattCharacteristic> gattCharacteristics) {
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic characteristic : gattCharacteristics) {
                if (BuildConfig.DEBUG) {
                    String characteristicName = getCharacteristicName(characteristic.getUuid());
                    Timber.d("    char: " + characteristicName);
                    //Log.d("addCharacteristics"," char: " + characteristicName);
                }

                final int characteristicID = getIdentification(characteristic.getUuid());
                this.gattCharacteristics.put(characteristicID, characteristic);
                this.values.put(characteristicID, null);

                int properties = characteristic.getProperties();
                if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                    gatt.setCharacteristicNotification(characteristic, true);
                }
            }
        }

        /**
         * Instructs the gatt to request the remote device to send a value of the given characteristic.
         * It waits until the gatt's callback calls {@link #onRead(int, Object, boolean)} containing the value.
         * It will not wait longer than 1000 milliseconds. If it takes longer, the current value stored in memory
         * will be returned.
         *
         * @param characteristicID ID of the characteristic whose remote value is requested.
         * @return The value of the characteristic.
         */
        Object read(int characteristicID) {
            final BluetoothGatt gatt = getGatt();
            if (gatt == null) {
                return null;
            }

            final BluetoothGattCharacteristic gattCharacteristic = gattCharacteristics.get(characteristicID);
            if (gattCharacteristic == null) {
                return null;
            }

            int properties = gattCharacteristic.getProperties();
            if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) == 0) {
                return null;
            }

            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (gattCharacteristic) {
                final boolean didRead = gatt.readCharacteristic(gattCharacteristic);
                if (didRead) {
                    try {
                        gattCharacteristic.wait(GATT_READ_TIMEOUT);
                    } catch (InterruptedException ignored) {
                    }
                }

                return values.get(characteristicID);
            }
        }

        void onRead(int characteristicID, Object value, boolean success) {
            final BluetoothGattCharacteristic gattCharacteristic = gattCharacteristics.get(characteristicID);

            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (gattCharacteristic) {
                if (success) {
                    values.put(characteristicID, value);
                }
                gattCharacteristic.notifyAll();
            }
        }
    }
}
