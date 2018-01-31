package com.siliconlabs.bledemo.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class Discovery implements BlueToothService.Listener {

    public interface BluetoothDiscoveryHost {
        boolean isReady();

        void reDiscover();

        void onAdapterDisabled();

        void onAdapterEnabled();
    }

    public interface DeviceContainer<T extends BluetoothDeviceInfo> {
        void flushContainer();

        void updateWithDevices(List<T> devices);
    }

    private BluetoothDiscoveryHost host;

    final DeviceContainer container;

    final List<GattService> SERVICES = new ArrayList<>();

    private BlueToothService.Binding bluetoothBinding;
    private BlueToothService blueToothService;

    private boolean executeDiscovery;

    private int bluetoothState;
    private boolean isBluetoothEnabled, isScanning, isDeviceStarted;
    private Boolean isDeviceReady;


    public Discovery(DeviceContainer container, BluetoothDiscoveryHost host) {
        this.container = container;
        this.host = host;
    }

    public void connect(Context context) {
        bluetoothBinding = new BlueToothService.Binding(context) {
            @Override
            protected void onBound(final BlueToothService service) {
                if (blueToothService != null) {
                    blueToothService.removeListener(Discovery.this);
                }

                blueToothService = service;

                if (blueToothService != null) {
                    if (executeDiscovery) {
                        executeDiscovery = false;

                        discoverDevices(true);
                        handleScanResults();
                    }
                } else {
                    // TODO
                }
            }
        };
        BlueToothService.bind(bluetoothBinding);
    }

    public void disconnect() {
        stopDiscovery(true);

        if (bluetoothBinding != null) {
            bluetoothBinding.unbind();
            bluetoothBinding = null;
        }
        blueToothService = null;
    }

    public void startDiscovery(boolean clearCachedDiscoveries, GattService... services) {
        if (clearCachedDiscoveries) {
            container.flushContainer();
        }

        SERVICES.clear();
        if (services != null) {
            for (GattService service : services) {
                SERVICES.add(service);
            }
        }

        if (blueToothService != null) {
            executeDiscovery = false;

            discoverDevices(true);
            handleScanResults();
        } else {
            executeDiscovery = true;
        }
    }

    public void stopDiscovery(boolean clearCachedDiscoveries) {
        if (blueToothService != null) {
            isScanning = false;
            blueToothService.removeListener(this);
            blueToothService.stopDiscoveringDevices(true);
        }

        if (clearCachedDiscoveries) {
            container.flushContainer();
        }
    }

    private void discoverDevices(boolean clearCache) {
        blueToothService.addListener(Discovery.this);
        isDeviceStarted = blueToothService.discoverDevicesOfInterest(clearCache);
        if (isDeviceStarted) {
            // TODO Device is found and we're waiting for a onDeviceReady callback.
        } else {

        }
    }

    private void handleScanResults() {
        if (!isDeviceStarted) {
            if (isScanning) {

            } else {
                // TODO
            }
        } else {
            // TODO
        }
    }

    @Override
    public List<ScanFilterCompat> getScanFilters() {
        List<ScanFilterCompat> filters = new ArrayList<>();
        for (GattService service : SERVICES) {
            ScanFilterCompat filter = new ScanFilterCompat();
            filter.setServiceUuid(new ParcelUuid(service.number));
            filter.setServiceUuidMask(new ParcelUuid(GattService.UUID_MASK));
            filters.add(filter);
        }
        return filters;
    }

    @Override
    public boolean askForEnablingBluetoothAdapter() {
        boolean allowAutoRestart = true; // TODO make configurable
        if (!allowAutoRestart) {

        }
        return allowAutoRestart;
    }

    @Override
    public void onStateChanged(int bluetoothAdapterState) {
        bluetoothState = bluetoothAdapterState;

        if (bluetoothAdapterState == BluetoothAdapter.STATE_OFF) {
            isScanning = isDeviceStarted = false;
            isDeviceReady = null;

            if (isBluetoothEnabled) {
                isBluetoothEnabled = false;
                host.onAdapterDisabled();
                // Adapter was off, but became turned on:
                // Allow restarting the adapter (askForEnablingBluetoothAdapter will be called at some point)
            }
        } else if (bluetoothAdapterState == BluetoothAdapter.STATE_ON) {
            if (!isBluetoothEnabled) {
                isBluetoothEnabled = true;
                // The adapter was off and now turned on again. Re-start discovery to recover.
                //discoverDevices(false);
                //handleScanResults();
                host.onAdapterEnabled();
            }
        }
    }

    @Override
    public void onScanStarted() {
        isScanning = true;
    }

    @Override
    public void onScanResultUpdated(List<BluetoothDeviceInfo> devices, BluetoothDeviceInfo changedDeviceInfo) {
        container.updateWithDevices(devices);
    }

    @Override
    public void onScanEnded() {
        isScanning = false;
        if (host.isReady()) {
            host.reDiscover();
        }
    }

    @Override
    public void onDeviceReady(BluetoothDevice device, boolean isInteresting) {
        boolean deviceIsConnected = (device != null) && isInteresting;

        if ((isDeviceReady != null) && (isDeviceReady == deviceIsConnected)) {
            return;
        }

        isDeviceReady = deviceIsConnected;
    }

    @Override
    public void onCharacteristicChanged(GattCharacteristic characteristicID, Object value) {
        Timber.d("onCharacteristicChanged: " + characteristicID + "=" + value);
        Log.d("onCharacteristicChanged", "" + characteristicID + "=" + value);
    }
}
