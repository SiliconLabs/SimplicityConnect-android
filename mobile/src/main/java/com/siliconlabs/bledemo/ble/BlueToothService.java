package com.siliconlabs.bledemo.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.siliconlabs.bledemo.utils.LocalService;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import timber.log.Timber;

/**
 * Service handling Bluetooth (regular and BLE) communcations.
 */
public class BlueToothService extends LocalService<BlueToothService> {
    // Discovery (of all devices) is cancelled after DISCOVERY_MAX_TIMEOUT milliseconds.
    private static final int DISCOVERY_MAX_TIMEOUT = 10000;
    // If connection is not successfully established or error message received after CONNECTION_TIMEOUT milliseconds.
    public static final int CONNECTION_TIMEOUT = 15000;
    // If no additional device is discovered, cancel discovery after DISCOVERY_NO_NEW_DISCOVERIES_TIMEOUT milliseconds.
    private static final int DISCOVERY_NO_NEW_DISCOVERIES_TIMEOUT = 2000;
    // If a scan of one device's services takes more than SCAN_DEVICE_TIMEOUT milliseconds, cancel it.
    private static final int SCAN_DEVICE_TIMEOUT = 4000;

    private static final String PREF_KEY_SAVED_DEVICES = "_pref_key_saved_devs_";
    private static final String TAG = BlueToothService.class.getSimpleName();

    public static abstract class Binding extends LocalService.Binding<BlueToothService> {
        public Binding(Context context) {
            super(context);
        }

        @Override
        protected Class<BlueToothService> getServiceClass() {
            return BlueToothService.class;
        }
    }

    public enum GattConnectType {
        THERMOMETER, LIGHT
    }

    public static class Receiver extends BroadcastReceiver {
        static final AtomicInteger currentState = new AtomicInteger(0);
        static final List<BlueToothService> registeredServices = new ArrayList<>();

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                synchronized (currentState) {
                    currentState.set(state);

                    for (BlueToothService blueToothService : registeredServices) {
                        blueToothService.notifyBluetoothStateChange(state);
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                boolean restart = false;

                for (BlueToothService blueToothService : registeredServices) {
                    if (blueToothService.notifyDiscoverFinished()) {
                        restart = true;
                    }
                }

                if (restart) {
                    registeredServices.get(0).bluetoothAdapter.startDiscovery();
                }
            }
        }
    }

    public interface Listener {
        /**
         * If this returns a non-empty list, scanned devices will be matched by the
         * return filters. A device will be scanned and recognized if it advertisement matches
         * one of more filters returned by this method.
         * <p/>
         * An empty list (or null) will match every bluetooth device, i.e. no filtering.
         *
         * @return The filters by which the BLE scanning will be filtered.
         */
        List<ScanFilterCompat> getScanFilters();

        /**
         * This method should get the user's permission (at least the first time around) to
         * enable the bluetooth automatically when necessary.
         *
         * @return True only if the user allows the code to automatically enable the bluetooth adapter.
         */
        boolean askForEnablingBluetoothAdapter();

        /**
         * Called when the Bluetooth-adapter state changes.
         *
         * @param bluetoothAdapterState State of the adapter.
         */
        void onStateChanged(int bluetoothAdapterState);

        /**
         * Called when a discovery of bluetooth devices has started.
         */
        void onScanStarted();

        /**
         * Called when a new bluetooth device has ben discovered or when an already discovered bluetooth
         * device's information has been updated.
         *
         * @param devices           List of all bluetooth devices currently discovered by the scan since {@link #onScanStarted()}.
         * @param changedDeviceInfo Indicates which device in 'devices' is new or updated (can be ignored).
         */
        void onScanResultUpdated(List<BluetoothDeviceInfo> devices, BluetoothDeviceInfo changedDeviceInfo);

        /**
         * Called when the current discovery process has ended.
         * Note that this method may be called more than once after a call to {@link #onScanStarted()}.
         */
        void onScanEnded();

        /**
         * Called when a interesting device is ready to be used for communication (it is connected and ready).
         * It is possible that after this method is called with a non-null device parameter, it can be called
         * with a null device parameter value later when the device gets disconnected or some other
         * error occurs.
         *
         * @param device Device that is the currently selected device or null if something went wrong.
         */
        void onDeviceReady(BluetoothDevice device, boolean isInteresting);

        /**
         * Called when a device is connected and one of its characteristics has changed.
         *
         * @param characteristic The characteristic.
         * @param value          The new value of the characteristic.
         */
        void onCharacteristicChanged(GattCharacteristic characteristic, Object value);
    }

    boolean isDestroyed;

    Handler handler;
    BluetoothAdapter bluetoothAdapter;
    BluetoothManager bluetoothManager;
    final AtomicReference<BluetoothDevice> knownDevice = new AtomicReference<>();

    final Map<String, BluetoothDeviceInfo> discoveredDevices = new LinkedHashMap<>();
    final Map<String, BluetoothDeviceInfo> interestingDevices = new LinkedHashMap<>();

    final AtomicInteger currentState = Receiver.currentState;
    int prevBluetoothState;
    final Listeners listeners = new Listeners();

    @SuppressWarnings("PointlessBooleanExpression")
    boolean useBLE = true && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2);

    BluetoothServer bluetoothServer;
    BluetoothClient bluetoothClient;
    BluetoothLEGatt bluetoothLEGatt;
    BluetoothGatt bluetoothGatt;
    TimeoutGattCallback extraCallback;

    private final BroadcastReceiver mReceiver = new BScanCallback(this);
    private Object bleScannerCallback;

    private boolean discoveryStarted;
    private final Runnable discoveryTimeout = new Runnable() {
        @Override
        public void run() {
            stopDiscovery();
            if (!scanDiscoveredDevices()) {
                onScanningCanceled();
            }
        }
    };
    private final Runnable newDiscoveryTimeout = new Runnable() {
        @Override
        public void run() {
            discoveryTimeout.run();
        }
    };
    private final Runnable scanTimeout = new Runnable() {
        @Override
        public void run() {
            stopScanning();
        }
    };
    private final static long RSSI_UPDATE_FREQ = 2000;
    private final Runnable rssiUpdate = new Runnable() {
        @Override
        public void run() {
            if (bluetoothGatt != null) {
                bluetoothGatt.readRemoteRssi();
                handler.postDelayed(this, RSSI_UPDATE_FREQ);
            }
        }
    };

    private final Runnable connectionTimeout = new Runnable() {
        @Override
        public void run() {
            if (bluetoothGatt != null) {
                Log.d("timeout","called");
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
                if (extraCallback != null) {
                    extraCallback.onTimeout();
                }
            }
        }
    };

    private boolean isBTAdapterAlreadyBeingEnabled;

    /**
     * Preference whose {@link #PREF_KEY_SAVED_DEVICES} key will have a set addresses of known devices.
     * Note that this list only grows... since we don't expect a phone/device to come into contact with many
     * interesting devices, this is fine. In the future we may want to back this by a LRU list or something similar to purge
     * device-addresses that have been used a long time ago.
     */
    SharedPreferences savedInterestingDevices;

    @Override
    public void onCreate() {
        super.onCreate();

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            // TODO Device does not support bluetooth at all.
            stopSelf();
            return;
        }

        if (useBLE) {
            useBLE = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        }

        savedInterestingDevices = PreferenceManager.getDefaultSharedPreferences(this);

        handler = new Handler();

        knownDevice.set(null);
        discoveredDevices.clear();
        interestingDevices.clear();

        synchronized (currentState) {
            currentState.set(bluetoothAdapter.getState());
        }

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        Receiver.registeredServices.add(this);
    }

    @Override
    public void onDestroy() {
        isDestroyed = true;

        handler.removeCallbacks(scanTimeout);
        stopScanning();
        handler.removeCallbacks(newDiscoveryTimeout);
        handler.removeCallbacks(discoveryTimeout);
        stopDiscovery();
        clearGatt();

        unregisterReceiver(mReceiver);
        Receiver.registeredServices.remove(this);

        if (bluetoothClient != null) {
            bluetoothClient.cancel();
        }

        if (bluetoothServer != null) {
            bluetoothServer.cancel();
        }

        if (bluetoothLEGatt != null) {
            bluetoothLEGatt.cancel();
        }

        super.onDestroy();
    }

    public void addListener(Listener listener) {
        synchronized (currentState) {
            if (!listeners.contains(listener)) {
                notifyInitialStateForListener(listener);
                listeners.add(listener);
            }
        }
    }

    private void notifyInitialStateForListener(final Listener listener) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                int state = Receiver.currentState.get();

                if (state == BluetoothAdapter.STATE_ON) {
                    listener.onStateChanged(state);

                    if (discoveryStarted) {
                        listener.onScanStarted();
                    }
                } else {
                    listener.onStateChanged(state);
                }
            }
        });
    }

    public void removeListener(Listener listener) {
        synchronized (currentState) {
            listeners.remove(listener);
        }
    }

    /**
     * Reads the value of a characteristic of the currently connected device.
     *
     * @param characteristic Characteristic whose value will be read.
     */
    public void read(GattCharacteristic characteristic) {
        if (bluetoothLEGatt == null) {
            listeners.onCharacteristicChanged(characteristic, null);
            return;
        }

        bluetoothLEGatt.read(characteristic.number);
    }

    /**
     * This method discovers which devices of interest are available.
     * <p/>
     * If this method returns true (discovery started), the caller should wait for the onScanXXX methods of
     * {@link BlueToothService.Listener}.
     *
     * @param clearCache True if the cache/list of the currently discovered devices should be cleared.
     * @return True if discovery started.
     */
    public boolean discoverDevicesOfInterest(boolean clearCache) {
        if (!bluetoothAdapter.isEnabled()) {
//            if (!isBTAdapterAlreadyBeingEnabled && listeners.askForEnablingBluetoothAdapter()) {
//                isBTAdapterAlreadyBeingEnabled = bluetoothAdapter.enable();
//            }
//            return false;
            return false;
        }

        isBTAdapterAlreadyBeingEnabled = false;

        discoveryStarted = true;
        listeners.onScanStarted();

        if (clearCache) {
            synchronized (discoveredDevices) {
                discoveredDevices.clear();
                interestingDevices.clear();
            }
        }

        startDiscovery();
        return true;
    }

    public void clearCache(){ //TODO Created to clean services cache

        synchronized (discoveredDevices) {
            discoveredDevices.clear();
            interestingDevices.clear();
        }

    }

    /**
     * This method either starts the device if the device is known or discovers which devices of interest are available.
     * <p/>
     * If this method returns true (device is known), the caller should wait for the
     * {@link BlueToothService.Listener#notifyDeviceReady(BluetoothDevice, boolean)} callback.
     * If this method return false (discovery is started), the caller should wait for the onScanXXX methods of
     * {@link BlueToothService.Listener}.
     *
     * @param clearCache True if the cache/list of the currently discovered devices should be cleared.
     * @return True if device is known and is starting. False if it is not known and discovery is started.
     */
    public boolean startOrDiscoverDeviceOfInterest(boolean clearCache) {
        if (!bluetoothAdapter.isEnabled()) {
//            if (!isBTAdapterAlreadyBeingEnabled && listeners.askForEnablingBluetoothAdapter()) {
//                isBTAdapterAlreadyBeingEnabled = bluetoothAdapter.enable();
//            }
//            return false;
            return false;
        }

        isBTAdapterAlreadyBeingEnabled = false;

        if (!restartInterestingDevice()) {
            discoveryStarted = true;
            listeners.onScanStarted();

            if (clearCache) {
                synchronized (discoveredDevices) {
                    discoveredDevices.clear();
                    interestingDevices.clear();
                }
            }

            startDiscovery();
            return false;
        }
        return true;
    }

    public void stopDiscoveringDevices(boolean clearCache) {
        if (discoveryStarted) {
            if (clearCache) {
                synchronized (discoveredDevices) {
                    discoveredDevices.clear();
                    interestingDevices.clear();
                }
            }
            handler.removeCallbacks(newDiscoveryTimeout);
            handler.removeCallbacks(discoveryTimeout);
            discoveryTimeout.run();
        }
    }


    /**
     * If the call {@link #startOrDiscoverDeviceOfInterest(boolean)} returned true, a device is currently connected or about to be connected.
     * This method will disconnected from the currently connected device or about any ongoing attempt to connect.
     */
    public void stopConnectedDevice() {
        if (bluetoothLEGatt != null) {
            bluetoothLEGatt.cancel();
            bluetoothLEGatt = null;
        }
    }

    private void startDiscovery() {
        if (bluetoothAdapter == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        handler.removeCallbacks(scanTimeout);
        handler.removeCallbacks(newDiscoveryTimeout);
        handler.removeCallbacks(discoveryTimeout);
        handler.postDelayed(discoveryTimeout, DISCOVERY_MAX_TIMEOUT);

        if (useBLE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ScanCallback scannerCallback = new BLEScanCallbackLollipop(this);
                bleScannerCallback = scannerCallback;
                ScanSettings settings = new ScanSettings.Builder()
                        .setReportDelay(0)
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                bluetoothAdapter.getBluetoothLeScanner().startScan((List<ScanFilter>) listeners.getScanFilterL(), settings, scannerCallback);
            } else {
                BluetoothAdapter.LeScanCallback scannerCallback = new BLEScanCallbackJB2(this);
                bleScannerCallback = scannerCallback;
                //noinspection deprecation
                if (!bluetoothAdapter.startLeScan(listeners.getScanUuids(), scannerCallback)) {
                    onDiscoveryCanceled();
                }
            }
        } else {
            if (!bluetoothAdapter.startDiscovery()) {
                onDiscoveryCanceled();
            }
        }
    }

    void onDiscoveryCanceled() {
        handler.removeCallbacks(newDiscoveryTimeout);
        handler.removeCallbacks(discoveryTimeout);
        if (discoveryStarted) {
            discoveryStarted = false;
            listeners.onScanEnded();
        }
    }

    private void stopDiscovery() {
        if (bluetoothAdapter == null) {
            return;
        }

        if (useBLE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (bluetoothAdapter.getBluetoothLeScanner() == null) {
                    return;
                }

                if (bleScannerCallback != null) {
                    bluetoothAdapter.getBluetoothLeScanner().stopScan((ScanCallback) bleScannerCallback);
                }
            } else {
                if (bleScannerCallback != null) {
                    //noinspection deprecation
                    bluetoothAdapter.stopLeScan((BluetoothAdapter.LeScanCallback) bleScannerCallback);
                }
            }
        } else if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
    }

    private void stopScanning() {
        Collection<BluetoothLEGatt> leGattsToClose;
        synchronized (discoveredDevices) {
            leGattsToClose = new ArrayList<>(discoveredDevices.size());
            for (BluetoothDeviceInfo devInfo : discoveredDevices.values()) {
                leGattsToClose.add((BluetoothLEGatt) devInfo.gattHandle);
            }
        }

        BluetoothLEGatt.cancelAll(leGattsToClose);
    }

    public void setKnownDevice(BluetoothDevice device) {
        knownDevice.set(device);
    }

    /**
     * If the device of interest is known, this method will try to restart it (reconnect to it and get the appropriate services).
     * If this method returns true, the caller should wait for the {@link BlueToothService.Listener#onDeviceReady(BluetoothDevice, boolean)} callback.
     *
     * @return True if the device is known. False if the device is not known.
     */
    public boolean restartInterestingDevice() { //It was private
        final BluetoothDevice btDevice = knownDevice.get();

        if (btDevice == null) {
            return false;
        }

        if (useBLE && (bluetoothLEGatt != null) && bluetoothLEGatt.representsConnectedDevice(btDevice)) {
            if (!discoveryStarted) {
                listeners.onScanStarted();
                listeners.onScanEnded();
                listeners.onDeviceReady(btDevice, bluetoothLEGatt.isOfInterest);
            }
            return true;
        }

        discoveryStarted = true;
        listeners.onScanStarted();

        if (useBLE) {
            if (bluetoothLEGatt != null) {
                bluetoothLEGatt.cancel();
            }

            bluetoothLEGatt = new BluetoothLEGatt(this, btDevice) {
                private BluetoothDeviceInfo deviceInfo;

                {
                    deviceInfo = getBluetoothDeviceInfo(btDevice);
                    deviceInfo.areServicesBeingDiscovered = true;
                    notifyUpdateDevices(deviceInfo);
                }

                @Override
                protected void setGattServices(List<BluetoothGattService> services) {
                    super.setGattServices(services);

                    updateDiscoveredDevice(deviceInfo, interestingServices, false);

                    if (isOfInterest) {
                        notifyDeviceReady(device, true);
                    } else {
                        close();
                        notifyDeviceReady(device, false);
                    }
                }

                @Override
                protected void notifyCharacteristicChanged(final int characteristicID, final Object value) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            final GattCharacteristic characteristic = GATT_CHARACTER_DESCS.get(characteristicID);
                            listeners.onCharacteristicChanged(characteristic, value);
                        }
                    });
                }
            };
        } else {
            // TODO get service-uuid from string-resource.
            bluetoothClient = new BluetoothClient(bluetoothAdapter, btDevice, UUID.fromString("276b2885-3d2e-403d-a36d-d40374bfbc52"));
            bluetoothClient.start();
        }

        return true;
    }

    void notifyBluetoothStateChange(final int newState) {
        if (newState == BluetoothAdapter.STATE_TURNING_OFF) {
            stopScanning();
            stopConnectedDevice();
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (prevBluetoothState != newState) {
                    if (newState == BluetoothAdapter.STATE_OFF) {
                        if (discoveryStarted) {
                            discoveryStarted = false;
                            listeners.onScanEnded();
                        }

                        synchronized (discoveredDevices) {
                            discoveredDevices.clear();
                            interestingDevices.clear();
                        }

                        setKnownDevice(null);
                        listeners.onDeviceReady(null, false);
                        listeners.onStateChanged(newState);
                    } else if (newState == BluetoothAdapter.STATE_ON) {
                        listeners.onStateChanged(newState);
                        isBTAdapterAlreadyBeingEnabled = false;
                    } else {
                        listeners.onStateChanged(newState);
                    }

                    prevBluetoothState = newState;
                }
            }
        });
    }

    boolean notifyDiscoverFinished() {
        if (useBLE) {
            return false;
        }

        boolean continueScanning = !isDestroyed && (knownDevice.get() == null);

        if (!continueScanning) {
            if (discoveryStarted) {
                discoveryStarted = false;
                listeners.onScanEnded();
            }
        }

        return continueScanning;
    }

    void notifyDeviceReady(final BluetoothDevice device, final boolean isInteresting) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                setKnownDevice(isInteresting ? device : null);
                listeners.onDeviceReady(device, isInteresting);
            }
        });
    }

    boolean addDiscoveredDevice(ScanResultCompat result) {
        if (knownDevice.get() != null) {
            return true;
        }

        final ArrayList<BluetoothDeviceInfo> listenerResult;
        final BluetoothDeviceInfo listenerChanged;
        final boolean postNewDiscoveryTimeout;

        BluetoothDevice device = result.getDevice();
        BluetoothDeviceInfo devInfo;
        synchronized (discoveredDevices) {
            final String address = device.getAddress();

            devInfo = discoveredDevices.get(address);
            if (devInfo == null) {
                devInfo = new BluetoothDeviceInfo();
                devInfo.device = device;
                discoveredDevices.put(address, devInfo);
                postNewDiscoveryTimeout = true;
            } else {
                devInfo.device = device;
                postNewDiscoveryTimeout = false;
            }
            devInfo.scanInfo = result;

            if (isDeviceInteresting(device)) {
                devInfo.isNotOfInterest = false;
                devInfo.isOfInterest = true;
                if (!interestingDevices.containsKey(address)) {
                    interestingDevices.put(address, devInfo);
                }
            } else if (isDeviceNotInteresting(device)) {
                devInfo.isNotOfInterest = true;
                devInfo.isOfInterest = false;
            }

            if (!listeners.isEmpty()) {
                listenerResult = new ArrayList<>(discoveredDevices.size());
                listenerChanged = devInfo.clone();
                for (BluetoothDeviceInfo di : discoveredDevices.values()) {
                    if (!isDeviceNotInteresting(di.device)) {
                        listenerResult.add(di.clone());
                    }
                }
            } else {
                listenerResult = null;
                listenerChanged = null;
            }
        }

        if (listenerResult != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    listeners.onScanResultUpdated(listenerResult, listenerChanged);
                    if (postNewDiscoveryTimeout) {
                        handler.removeCallbacks(newDiscoveryTimeout);
                        handler.postDelayed(newDiscoveryTimeout, DISCOVERY_NO_NEW_DISCOVERIES_TIMEOUT);
                    }
                }
            });
        }

        return false;
    }

    /**
     * Returns true if device is of interest.
     * Returns false when not sure.
     *
     * @param device The device
     * @return True if device is a of interest.
     */
    private boolean isDeviceInteresting(BluetoothDevice device) {
        if (true) return true;

        Set<String> devices = savedInterestingDevices.getStringSet(PREF_KEY_SAVED_DEVICES, null);
        if ((devices != null) && devices.contains(device.getAddress())) {
            return true;
        }

        String name = device.getName();
        return (!TextUtils.isEmpty(name) && name.toLowerCase().contains("blue gecko"));
    }

    /**
     * Returns true if device is certainly not interesting
     * Returns false when not sure.
     *
     * @param device The device
     * @return True if the device is not interesting.
     */
    private boolean isDeviceNotInteresting(BluetoothDevice device) {
        if (true) return false;

        String name = device.getName();
        return (TextUtils.isEmpty(name) || !name.toLowerCase().contains("blue gecko"));
    }

    boolean scanDiscoveredDevices() {
        Timber.d("scanDiscoveredDevices called.");
        Log.d("scanDiscoveredDevices", "called");
        handler.removeCallbacks(newDiscoveryTimeout);
        handler.removeCallbacks(discoveryTimeout);
        handler.removeCallbacks(scanTimeout);
        handler.postDelayed(scanTimeout, SCAN_DEVICE_TIMEOUT);

        if (knownDevice.get() != null) {
            return false;                       //TODO It was commented to try to clean Services Cache
        }

        BluetoothDeviceInfo devInfo = null;
        synchronized (discoveredDevices) {
            final Collection<BluetoothDeviceInfo> devices = discoveredDevices.values();
            for (BluetoothDeviceInfo di : devices) {
                if (di.isUnDiscovered()) {
                    devInfo = di;
                    break;
                }
            }

            if (devInfo == null) {
                Timber.d("scanDiscoveredDevices called: Nothing left!");
                Log.d("scanDiscoveredDevices", "called: Nothing left!");
                return false;
            }

            final BluetoothDeviceInfo devInfoForDiscovery = devInfo;
            devInfoForDiscovery.discover(new BluetoothLEGatt(this, devInfoForDiscovery.device) {
                @Override
                protected void setGattServices(List<BluetoothGattService> services) {
                    super.setGattServices(services);

                    close();

                    updateDiscoveredDevice(devInfoForDiscovery, interestingServices, true);
                }
            });
        }

        Timber.d("scanDiscoveredDevices called: Next up is " + devInfo.device.getAddress());
        Log.d("scanDiscoveredDevices"," called: Next up is " + devInfo.device.getAddress());
        return true;
    }

    void onScanningCanceled() {
        handler.removeCallbacks(scanTimeout);
        if (discoveryStarted) {
            discoveryStarted = false;
            listeners.onScanEnded();
        }
    }

    void notifyUpdateDevices(BluetoothDeviceInfo devInfo) {
        final ArrayList<BluetoothDeviceInfo> listenerResult;
        final BluetoothDeviceInfo listenerChanged;

        synchronized (discoveredDevices) {
            if (!listeners.isEmpty()) {
                listenerResult = new ArrayList<>(discoveredDevices.size());
                listenerChanged = devInfo.clone();
                for (BluetoothDeviceInfo di : discoveredDevices.values()) {
                    if (!isDeviceNotInteresting(di.device)) {
                        listenerResult.add(di.clone());
                    }
                }
            } else {
                listenerResult = null;
                listenerChanged = null;
            }
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (listenerResult != null) {
                    listeners.onScanResultUpdated(listenerResult, listenerChanged);
                }
            }
        });
    }

    void updateDiscoveredDevice(BluetoothDeviceInfo devInfo, List<BluetoothGattService> services, final boolean keepScanning) {
        final ArrayList<BluetoothDeviceInfo> listenerResult;
        final BluetoothDeviceInfo listenerChanged;

        synchronized (discoveredDevices) {
            devInfo.gattHandle = null;

            if (services == null) {
                devInfo.serviceDiscoveryFailed = true;
            } else {
                devInfo.serviceDiscoveryFailed = false;
                devInfo.isOfInterest = !services.isEmpty();
                devInfo.isNotOfInterest = services.isEmpty();
            }
            devInfo.areServicesBeingDiscovered = false;

            if (devInfo.isOfInterest) {
                interestingDevices.put(devInfo.device.getAddress(), devInfo);

                Set<String> devices = savedInterestingDevices.getStringSet(PREF_KEY_SAVED_DEVICES, null);
                Set<String> knownDevices = (devices == null) ? new HashSet<String>() : new HashSet<>(devices);
                knownDevices.add(devInfo.device.getAddress());
                savedInterestingDevices.edit().putStringSet(PREF_KEY_SAVED_DEVICES, knownDevices).apply();
            }

            if (!listeners.isEmpty()) {
                listenerResult = new ArrayList<>(discoveredDevices.size());
                listenerChanged = devInfo.clone();
                for (BluetoothDeviceInfo di : discoveredDevices.values()) {
                    if (!isDeviceNotInteresting(di.device)) {
                        listenerResult.add(di.clone());
                    }
                }
            } else {
                listenerResult = null;
                listenerChanged = null;
            }
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                boolean resumeScanning = !isDestroyed && keepScanning;
                if (resumeScanning) {
                    resumeScanning = scanDiscoveredDevices();
                }

                if (listenerResult != null) {
                    listeners.onScanResultUpdated(listenerResult, listenerChanged);
                }
                if (!resumeScanning) {
                    onScanningCanceled();
                }
            }
        });
    }

    BluetoothDeviceInfo getBluetoothDeviceInfo(BluetoothDevice device) {
        BluetoothDeviceInfo devInfo = null;
        synchronized (discoveredDevices) {
            for (BluetoothDeviceInfo deviceInfo : discoveredDevices.values()) {
                if (deviceInfo.device.getAddress().equals(device.getAddress())) {
                    devInfo = deviceInfo;
                    break;
                }
            }

            if (devInfo == null) {
                devInfo = new BluetoothDeviceInfo();
                devInfo.device = device;
                devInfo.scanInfo = new ScanResultCompat();
                devInfo.scanInfo.setDevice(device);
                ScanRecordCompat record = new ScanRecordCompat();
                record.setTxPowerLevel(Integer.MIN_VALUE);
                record.setAdvertiseFlags(-1);
                devInfo.scanInfo.setScanRecord(record);

                discoveredDevices.put(device.getAddress(), devInfo);
            }
        }

        return devInfo;
    }

    public void clearGatt() {
        handler.removeCallbacks(rssiUpdate);
        handler.removeCallbacks(connectionTimeout);
        if (bluetoothGatt != null) {
            Log.d("clearGatt","called");
            bluetoothGatt.disconnect();
            // You must call close in the state callback so we will do so there.
        }
    }

    public void registerGattCallback(boolean requestRssiUpdates, TimeoutGattCallback callback) {
        if (requestRssiUpdates) {
            handler.post(rssiUpdate);
        } else {
            handler.removeCallbacks(rssiUpdate);
        }
        extraCallback = callback;
    }

    public void discoverGattServices() {
        if (bluetoothGatt != null) {
            bluetoothGatt.discoverServices();
        }
    }

    public void refreshGattServices() {
        if (bluetoothGatt != null) {
            refreshGattDB(bluetoothGatt);
        }
    }

    private void refreshGattDB(final BluetoothGatt gatt)
    {
        while(!refreshDeviceCache(gatt));
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                gatt.discoverServices();
            }
        },500);

    }

    private boolean refreshDeviceCache(BluetoothGatt gatt) {
        try {
            Log.d("refreshDevice", "Called");
            Method localMethod = gatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(gatt, new Object[0])).booleanValue();
                Log.d("refreshDevice", "bool: " + bool);
                return bool;
            }
        } catch (Exception localException) {
            Log.e("refreshDevice", "An exception occured while refreshing device");
        }
        return false;
    }

    public boolean isGattConnected() {
        return bluetoothGatt != null && bluetoothManager.getConnectionState(bluetoothGatt.getDevice(), BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED;
    }

    public BluetoothGatt getConnectedGatt() {
        return bluetoothGatt;
    }

    public boolean writeGattCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothGatt != null) {
            return bluetoothGatt.writeCharacteristic(characteristic);
        }
        return false;
    }

    public boolean connectGatt(BluetoothDevice device, boolean requestRssiUpdates, @Nullable TimeoutGattCallback callback) {
        stopDiscovery();
        List<BluetoothDevice> devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        if (callback != null) {
            extraCallback = callback;
        }
        if (devices.contains(device)){
            if (bluetoothGatt != null) {
                if (bluetoothGatt.getDevice().equals(device)) {
                    if (requestRssiUpdates) {
                        handler.post(rssiUpdate);
                    } else {
                        handler.removeCallbacks(rssiUpdate);
                    }
                }
            }
            return true;
        }
        clearGatt();

        bluetoothGatt = device.connectGatt(this, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                handler.removeCallbacks(connectionTimeout);
                if (extraCallback != null) {
                    extraCallback.onConnectionStateChange(gatt, status, newState);
                }

                if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                    try {
                        bluetoothGatt.close();
                    } catch (Exception e) {
                        Log.d(TAG, "close ignoring: " + e);
                    }
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (extraCallback != null) {
                    extraCallback.onServicesDiscovered(gatt, status);
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                if (extraCallback != null) {
                    extraCallback.onCharacteristicWrite(gatt, characteristic, status);
                }
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorRead(gatt, descriptor, status);
                if (extraCallback != null) {
                    extraCallback.onDescriptorRead(gatt, descriptor, status);
                }
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);
                if (extraCallback != null) {
                    extraCallback.onDescriptorWrite(gatt, descriptor, status);
                }
            }

            @Override
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                super.onReliableWriteCompleted(gatt, status);
                if (extraCallback != null) {
                    extraCallback.onReliableWriteCompleted(gatt, status);
                }
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                super.onReadRemoteRssi(gatt, rssi, status);
                if (extraCallback != null) {
                    extraCallback.onReadRemoteRssi(gatt, rssi, status);
                }
            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                super.onMtuChanged(gatt, mtu, status);
                if (extraCallback != null) {
                    extraCallback.onMtuChanged(gatt, mtu, status);
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                if (extraCallback != null) {
                    extraCallback.onCharacteristicRead(gatt, characteristic, status);
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                if (extraCallback != null) {
                    extraCallback.onCharacteristicChanged(gatt, characteristic);
                }
            }
        });

        handler.postDelayed(connectionTimeout, CONNECTION_TIMEOUT);

        if (bluetoothGatt != null) {
            if (requestRssiUpdates) {
                handler.post(rssiUpdate);
            }
            return true;
        }
        return false;
    }

    private static class Listeners extends ArrayList<Listener> implements Listener {
        List<?> getScanFilterL() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                return null;
            }

            List<ScanFilterCompat> scanFiltersCompat = getScanFilters();
            List<ScanFilter> scanFilters = (scanFiltersCompat != null) ? new ArrayList<ScanFilter>(scanFiltersCompat.size()) : null;
            if (scanFiltersCompat != null) {
                for (ScanFilterCompat scanFilterCompat : scanFiltersCompat) {
                    scanFilters.add(scanFilterCompat.createScanFilter());
                }
                return scanFilters.isEmpty() ? null : scanFilters;
            } else {
                return null;
            }
        }

        UUID[] getScanUuids() {
            List<ScanFilterCompat> scanFiltersCompat = getScanFilters();
            if ((scanFiltersCompat == null) || scanFiltersCompat.isEmpty()) {
                return null;
            }

            Set<UUID> uuids = new HashSet<>();
            for (ScanFilterCompat scanFilterCompat : scanFiltersCompat) {
                ParcelUuid serviceUuid = scanFilterCompat.getServiceUuid();
                if (serviceUuid != null) {
                    uuids.add(serviceUuid.getUuid());
                }
            }

            if (uuids.isEmpty()) {
                return null;
            }

            UUID[] retVal = new UUID[uuids.size()];
            return uuids.toArray(retVal);
        }

        @Override
        public List<ScanFilterCompat> getScanFilters() {
            List<ScanFilterCompat> result = new ArrayList<>();
            for (Listener listener : this) {
                List<ScanFilterCompat> scanFilters = listener.getScanFilters();
                if (scanFilters != null) {
                    for (ScanFilterCompat scanFilter : scanFilters) {
                        if (!result.contains(scanFilter)) {
                            result.add(scanFilter);
                        }
                    }
                }
            }
            return result.isEmpty() ? null : result;
        }

        @Override
        public boolean askForEnablingBluetoothAdapter() {
            for (Listener listener : this) {
                if (listener.askForEnablingBluetoothAdapter()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onStateChanged(int bluetoothAdapterState) {
            for (Listener listener : this) {
                listener.onStateChanged(bluetoothAdapterState);
            }
        }

        @Override
        public void onScanStarted() {
            for (Listener listener : this) {
                listener.onScanStarted();
            }
        }

        @Override
        public void onScanResultUpdated(List<BluetoothDeviceInfo> devices, BluetoothDeviceInfo changedDeviceInfo) {
            for (Listener listener : this) {
                listener.onScanResultUpdated(devices, changedDeviceInfo);
            }
        }

        @Override
        public void onScanEnded() {
            for (Listener listener : this) {
                listener.onScanEnded();
            }
        }

        @Override
        public void onDeviceReady(BluetoothDevice device, boolean isInteresting) {
            for (Listener listener : this) {
                listener.onDeviceReady(device, isInteresting);
            }
        }

        @Override
        public void onCharacteristicChanged(GattCharacteristic characteristic, Object value) {
            for (Listener listener : this) {
                listener.onCharacteristicChanged(characteristic, value);
            }
        }
    }
}
