package com.siliconlabs.bledemo.adapters;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.siliconlabs.bledemo.ble.BluetoothDeviceInfo;
import com.siliconlabs.bledemo.ble.Discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class ScannedDevicesAdapter<T extends BluetoothDeviceInfo> extends RecyclerView.Adapter<DeviceInfoViewHolder>
        implements Discovery.DeviceContainer {

    private final static long MAX_AGE = 16000; //Original 15000
    private final static long PERIOD_UPDATE_REMOVE_OUTDATED_HTM = 3000;
    private final static long DISCOVERY_UPDATE_PERIOD = 2000; //10000

    private final List<T> mostRecentDevicesInfo = new ArrayList<>();
    private final Map<T, Long> mostRecentInfoAge = new HashMap<>();
    private final List<T> devicesInfo = new ArrayList<>();
    HashMap<String, String> deviceMacAddressToName = new HashMap<>();

    private boolean runUpdater = true;
    private final Handler handler = new Handler();
    private final Comparator<T> reverseItemsComparator = new Comparator<T>() {
        @Override
        public int compare(T lhs, T rhs) {
            final String lName = lhs.scanInfo.getDisplayName(true);
            final String rName = rhs.scanInfo.getDisplayName(true);
            return rName.compareTo(lName);
        }
    };
    boolean isThermometerMode = false;
    boolean isBlueGeckoTabSelected = true;
    private int comp = 2;
    private String search = null;
    private final Comparator<T> rssiComparator = new Comparator<T>() {
        @Override
        public int compare(T lhs, T rhs) {
            final int lrssi = lhs.scanInfo.getRssi();
            final int rrssi = rhs.scanInfo.getRssi();
            return ((Integer) rrssi).compareTo(lrssi);
        }
    };
    public BluetoothDeviceInfo debugModeConnectingDevice;
    private final Comparator<T> reverseRssiComparator = new Comparator<T>() {
        @Override
        public int compare(T lhs, T rhs) {
            final int lrssi = lhs.scanInfo.getRssi();
            final int rrssi = rhs.scanInfo.getRssi();
            return ((Integer) rrssi).compareTo(lrssi);
        }
    };

    private boolean mostRecentDevicesInfoIsDirty;
    private DeviceInfoViewHolder.Generator generator;
    private boolean updatePending;

    private final Comparator<T> itemsComparator = new Comparator<T>() {
        @Override
        public int compare(T lhs, T rhs) {
            final String lName = lhs.scanInfo.getDisplayName(true);
            final String rName = rhs.scanInfo.getDisplayName(true);
            return lName.compareTo(rName);
        }
    };
    private final Runnable delayedUpdater = new Runnable() {
        @Override
        public void run() {
            updateDevicesInfo();
        }
    };
    private boolean isDebugMode = false;
    private Timer timer = new Timer();

    public void sort(int comparator) {
        comp = comparator;
        switch (comparator) {
            case 0:
                sortDevices(itemsComparator);
                break;
            case 1:
                sortDevices(reverseItemsComparator);
                break;
            case 2:
                sortDevices(rssiComparator);
                break;
            case 3:
                sortDevices(reverseRssiComparator);
                break;
            default:
                break;
        }
    }

    public ScannedDevicesAdapter(DeviceInfoViewHolder.Generator generator) {
        this.generator = generator;
    }

    @Override
    public int getItemCount() {
        return devicesInfo.size();
    }

    @Override
    public long getItemId(int position) {
        final BluetoothDeviceInfo info = devicesInfo.get(position);
        return info.device.getAddress().hashCode();
    }

    @Override
    public DeviceInfoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return generator.generate(parent);
    }

    @Override
    public void onBindViewHolder(DeviceInfoViewHolder holder, int position) { //Creates holder
        if (BluetoothAdapter.getDefaultAdapter() != null && !BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            return;
        }

        final T info = devicesInfo.get(position); //TODO It was final
        holder.setData(info, position);

        if (isDebugMode) {
            ((DebugModeDeviceAdapter.ViewHolder) holder).connectingSpinner.setVisibility(View.GONE);
        }

        if (isDebugMode && debugModeConnectingDevice != null) {
            String addressListItem = info.scanInfo.getDevice().getAddress();
            String addressConnectingItem = debugModeConnectingDevice.getAddress();
            if (TextUtils.equals(addressListItem, addressConnectingItem)) {
                ((DebugModeDeviceAdapter.ViewHolder) holder).startConnectingSpinnerAnim();
            } else {
                ((DebugModeDeviceAdapter.ViewHolder) holder).stopConnectingSpinnerAnim();
            }
        }
    }

    public void setDebugMode() {
        isDebugMode = true;
    }

    public void setThermometerMode() {
        isThermometerMode = true;

        timer.cancel();
        timer = new Timer();
        // This timertask is used to ensure that discovered devices that have exceeded MAX_AGE
        // will be removed, even if the updatedevices method doesn't receive a proper callback
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                removeOldDevices();
            }
        }, 0, PERIOD_UPDATE_REMOVE_OUTDATED_HTM);
    }

    public void setBlueGeckoTabSelected(boolean isBlueGeckoMode) {
        isBlueGeckoTabSelected = isBlueGeckoMode;
    }

    public void updateWith(List<T> devicesInfo) {
        Long now = System.currentTimeMillis(); //TODO Deleting here
        mostRecentDevicesInfoIsDirty = true;

        if ((devicesInfo != null) && !devicesInfo.isEmpty()) {
            for (T devInfo : devicesInfo) {
                // ignore/show blue gecko thermometer based on selected tab and thermo mode
                /*
                if (isThermometerMode) {
                    String deviceAddress = devInfo.getAddress();
                    String deviceName = deviceMacAddressToName.get(deviceAddress);
                    deviceName = devInfo.getName();
                    deviceMacAddressToName.put(deviceAddress, deviceName);

                    boolean isBlueGecko = (!TextUtils.isEmpty(deviceName)) && (deviceName.toUpperCase().contains("BLUE GECKO") || deviceName.toUpperCase().startsWith("BG"));
                    boolean ignoreBlueGecko = (!isBlueGeckoTabSelected) && isBlueGecko;
                    boolean ignoreOtherThermometers = isBlueGeckoTabSelected && (!isBlueGecko);
                    if (ignoreBlueGecko || ignoreOtherThermometers) {
                        continue;
                    }
                }
                */

                T clone = (T) devInfo.clone();
                clone.isOfInterest = true;
                clone.isNotOfInterest = false;
                clone.serviceDiscoveryFailed = false;
                int index = mostRecentDevicesInfo.indexOf(clone);
                if (index >= 0) {
                    BluetoothDeviceInfo cachedInfo = mostRecentDevicesInfo.get(index);
                    long timestampDiff = clone.scanInfo.getTimestampNanos() - cachedInfo.scanInfo.getTimestampNanos();
                    if (timestampDiff != 0) {
                        //Log.i("Time Diff", "Updated " + cachedInfo.scanInfo.getDisplayName(false));
                        //Log.d("Timediff","" + timestampDiff);
                        cachedInfo.scanInfo.getDisplayName(false);
                        mostRecentDevicesInfo.set(index, clone);
                        mostRecentInfoAge.put(devInfo, now);
                    }
                } else {
                    mostRecentDevicesInfo.add(devInfo);
                    mostRecentInfoAge.put(devInfo, now);
                }
            }
        }

        //Cleaning duplicated items ----------------------------------------------------------------------
        Set<T> dedupedDeviceInfo = new HashSet<>(mostRecentDevicesInfo);
        mostRecentDevicesInfo.clear();
        mostRecentDevicesInfo.addAll(dedupedDeviceInfo);
        //-----------------------------------------------------------------------------------------------------
        if (this.devicesInfo.isEmpty()) {
            updateDevicesInfo();
        } else if (!updatePending && runUpdater) {
            updatePending = true;
            handler.postDelayed(delayedUpdater, DISCOVERY_UPDATE_PERIOD);

        }
    }

    private void removeOldDevices() {
        Long now = System.currentTimeMillis();
        final Iterator<Map.Entry<T, Long>> iter = mostRecentInfoAge.entrySet().iterator();
        while (iter.hasNext()) {
            final Map.Entry<T, Long> ageEntry = iter.next();
            long age = now.longValue() - ageEntry.getValue().longValue();
            if (age > MAX_AGE) {
                mostRecentDevicesInfo.remove(ageEntry.getKey());
                iter.remove();
            }
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                updateDevicesInfo();
            }
        });
    }

    public void updateWith(List<T> devicesInfo, String string) {
        mostRecentDevicesInfoIsDirty = true;
        if ((devicesInfo != null) && !devicesInfo.isEmpty()) {
            for (T devInfo : devicesInfo) {
                if (devInfo.toString().contains(string)) {
                    Log.i("Filtered ", "" + devInfo.toString());
                    T clone = (T) devInfo.clone();
                    clone.isOfInterest = true;
                    clone.isNotOfInterest = false;
                    clone.serviceDiscoveryFailed = false;

                    int index = mostRecentDevicesInfo.indexOf(clone);
                    if (index >= 0) {
                        BluetoothDeviceInfo cachedInfo = mostRecentDevicesInfo.get(index);
                        if (clone.scanInfo.toString().contains(string)) {
                            Log.i("Time Diff", "Updated " + cachedInfo.scanInfo.getDisplayName(false));
                            mostRecentDevicesInfo.add(devInfo);
                        }
                    }
                } else {
                    mostRecentDevicesInfo.remove(devInfo);
                }
            }
            for (int i = 0; i < mostRecentDevicesInfo.size(); i++) {
                for (int j = i + 1; j < mostRecentDevicesInfo.size(); j++) {
                    if (mostRecentDevicesInfo.get(j).toString().equals(mostRecentDevicesInfo.get(i).toString())) {
                        mostRecentDevicesInfo.remove(mostRecentDevicesInfo.get(j));
                        j--;
                    }
                }
            }
            if (mostRecentDevicesInfo.isEmpty()) {
                //clear();
                return;
            }
        }
        handler.postDelayed(delayedUpdater, 0);//DISCOVERY_UPDATE_PERIOD);
    }

    public void updateWithBonded(List<T> devicesInfo) { //TODO - Double item some times?
        mostRecentDevicesInfoIsDirty = true;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if ((devicesInfo != null) && !devicesInfo.isEmpty()) {
            for (T devInfo : devicesInfo) {
                T clone = (T) devInfo.clone();
                clone.isOfInterest = true;
                clone.isNotOfInterest = false;
                clone.serviceDiscoveryFailed = false;
                int index = mostRecentDevicesInfo.indexOf(clone);
                if (index >= 0) {
                    BluetoothDeviceInfo cachedInfo = mostRecentDevicesInfo.get(index);
                    Log.i("Time Diff", "Updated " + cachedInfo.scanInfo.getDisplayName(false));
                    mostRecentDevicesInfo.add(devInfo);
                }
            }
            for (int i = 0; i < mostRecentDevicesInfo.size(); i++) {
                boolean add = false;
                for (BluetoothDevice device : pairedDevices) {
                    if (mostRecentDevicesInfo.get(i).getAddress().equals(device.getAddress())) {
                        add = true;
                    }
                }
                if (!add) {
                    Log.i("updateWithBonded()", "rm " + mostRecentDevicesInfo.get(i).getAddress());
                    mostRecentDevicesInfo.remove(mostRecentDevicesInfo.get(i));
                    i--;
                }
            }
            for (int i = 0; i < mostRecentDevicesInfo.size(); i++) {
                for (int j = i + 1; j < mostRecentDevicesInfo.size(); j++) {
                    if (mostRecentDevicesInfo.get(j).toString().equals(mostRecentDevicesInfo.get(i).toString())) {
                        mostRecentDevicesInfo.remove(mostRecentDevicesInfo.get(j));
                        j--;
                    }
                }
            }
        }
        handler.postDelayed(delayedUpdater, DISCOVERY_UPDATE_PERIOD);

    }

    public void setRunUpdater(boolean runUpdater) {
        this.runUpdater = runUpdater;
    }

    private void updateDevicesInfo() {
        updatePending = false;
        mostRecentDevicesInfoIsDirty = true;

        sort(comp);

        notifyDataSetChanged();
    }

    private void resetDeviceList() {
        if (devicesInfo.isEmpty()) {
            devicesInfo.addAll(mostRecentDevicesInfo);
        } else {
            for (T device : mostRecentDevicesInfo) {
                T btinfo = mostRecentDevicesInfo.get(mostRecentDevicesInfo.indexOf(device));
                if (devicesInfo.contains(btinfo)) {
                    int index = devicesInfo.indexOf(device);
                    devicesInfo.remove(index);
                    devicesInfo.add(index, btinfo);
                } else {
                    devicesInfo.add(devicesInfo.size(), btinfo);
                    Log.d("filter", "" + device.getAddress() + " added");
                }
            }

            Iterator<T> iter = devicesInfo.iterator();
            while (iter.hasNext()) {
                T btinfo = iter.next();
                if (!mostRecentDevicesInfo.contains(btinfo)) {
                    iter.remove();
                }
            }
        }
    }

    public void sortDevices(Comparator<T> comparator) {
        resetDeviceList();
        if (this.devicesInfo.size() > 0) {
            Collections.sort(this.devicesInfo, comparator);
        }
    }

    public void filterDevices(String name,
                              boolean filterName,
                              int rssi,
                              boolean filterRssi) {
        resetDeviceList();
        for (Iterator<T> deviceIterator = devicesInfo.iterator(); deviceIterator.hasNext(); ){
            T device = deviceIterator.next();
            if (filterName && (device.getName() == null || !device.getName().toLowerCase().contains((name.toLowerCase())))) {
                deviceIterator.remove();
            } else if (filterRssi && device.getRssi() < rssi) {
                deviceIterator.remove();
            }
        }
    }

    public void clear() {
        mostRecentDevicesInfo.clear();
        mostRecentInfoAge.clear();

        if (!devicesInfo.isEmpty()) {
            int size = devicesInfo.size();
            Log.d("clear_devicesInfo", "Called");
            devicesInfo.clear();
            notifyItemRangeRemoved(0, size);
        }
    }

    @Override
    public void flushContainer() {
        clear();
    }

    @Override
    public void updateWithDevices(List devices) {
        updateWith(devices);
    }

    public List<T> getDevicesInfo() {
        return devicesInfo;
    }
}
