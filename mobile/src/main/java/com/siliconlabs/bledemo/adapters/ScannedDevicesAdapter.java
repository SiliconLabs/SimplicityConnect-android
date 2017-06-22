package com.siliconlabs.bledemo.adapters;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.siliconlabs.bledemo.activity.MainActivityDebugMode;
import com.siliconlabs.bledemo.ble.BluetoothDeviceInfo;
import com.siliconlabs.bledemo.ble.Discovery;

public class ScannedDevicesAdapter<T extends BluetoothDeviceInfo> extends RecyclerView.Adapter<DeviceInfoViewHolder>
        implements Discovery.DeviceContainer {

    final static long MAX_AGE = 16000; //Original 15000
    final static long PERIOD_UPDATE_REMOVE_OUTDATED_HTM = 7000;
    final static long DISCOVERY_UPDATE_PERIOD = 2000; //10000

    final List<T> mostRecentDevicesInfo = new ArrayList<>();
    final Map<T, Long> mostRecentInfoAge = new HashMap<>();
    final List<T> devicesInfo = new ArrayList<>();
    HashMap<String, String> deviceMacAddressToName = new HashMap<>();

    private boolean runUpdater = true;
    boolean mostRecentDevicesInfoIsDirty;
    boolean updatePending;
    boolean isThermometerMode = false;
    boolean isBlueGeckoTabSelected = true;
    private int comp = 2;
    private String search = null;

    boolean isDebugMode = false;
    public BluetoothDeviceInfo debugModeConnectingDevice;

    Timer timer = new Timer();
    final Handler handler = new Handler();

    private DeviceInfoViewHolder.Generator generator;

    final Runnable delayedUpdater = new Runnable() {
        @Override
        public void run() {
            updateDevicesInfo();
        }
    };

    private final Comparator<T> itemsComparator = new Comparator<T>() {
        @Override
        public int compare( T lhs, T rhs) {
        final String lName = lhs.scanInfo.getDisplayName(true);
        final String rName = rhs.scanInfo.getDisplayName(true);
        return lName.compareTo(rName);
        }
    };

    private final Comparator<T> reverseitemsComparator = new Comparator<T>() {
        @Override
        public int compare( T lhs, T rhs) {
        final String lName = lhs.scanInfo.getDisplayName(true);
        final String rName = rhs.scanInfo.getDisplayName(true);
        return rName.compareTo(lName);
        }
    };

    private final Comparator<T> rssiCompatator = new Comparator<T>() {
        @Override
        public int compare(T lhs, T rhs) {
        final int lrssi = lhs.scanInfo.getRssi();
        final int rrssi = rhs.scanInfo.getRssi();
        return ((Integer)rrssi).compareTo(lrssi);
        }
    };

    private final Comparator<T> reverserssiCompatator = new Comparator<T>() {
        @Override
        public int compare(T lhs, T rhs) {
            final int lrssi = lhs.scanInfo.getRssi();
            final int rrssi = rhs.scanInfo.getRssi();
            return ((Integer)rrssi).compareTo(lrssi);
        }
    };

    public void filter(int filter){
        comp = filter;
        switch (filter){
            case 0:
                filterDevices(itemsComparator);
                break;
            case 1:
                filterDevices(reverseitemsComparator);
                break;
            case 2:
                filterDevices(rssiCompatator);
                break;
            case 3:
                filterDevices(reverserssiCompatator);
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
        if(BluetoothAdapter.getDefaultAdapter() != null && !BluetoothAdapter.getDefaultAdapter().isEnabled()) {
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
                final Iterator<Map.Entry<T, Long>> iter = mostRecentInfoAge.entrySet().iterator();
                while (iter.hasNext()) {
                    final Map.Entry<T, Long> ageEntry = iter.next();
                    final Long now = System.currentTimeMillis();
                    long age = now.longValue() - ageEntry.getValue().longValue();
                    if (age > MAX_AGE) {
                        Log.i("Time Diff", "Removed old " + ageEntry.getKey().scanInfo.getDisplayName(false));
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
        }, 0, PERIOD_UPDATE_REMOVE_OUTDATED_HTM);
    }

    public void setBlueGeckoTabSelected(boolean isBlueGeckoMode) {
        isBlueGeckoTabSelected = isBlueGeckoMode;
    }

    public void updateWith(List<T> devicesInfo) {
        updateWith(devicesInfo, true);
    }

    public void updateWith(List<T> devicesInfo, boolean removeOld) {
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

        if (removeOld) {
            final Iterator<Map.Entry<T, Long>> iter = mostRecentInfoAge.entrySet().iterator();
            while (iter.hasNext()) {
                final Map.Entry<T, Long> ageEntry = iter.next();
                long age = now.longValue() - ageEntry.getValue().longValue();
                if (age > MAX_AGE) {
                    Log.i("Time Diff", "Removed old " + ageEntry.getKey().scanInfo.getDisplayName(false));
                    mostRecentDevicesInfo.remove(ageEntry.getKey());
                    iter.remove();
                }
            }
        }

        //Cleaning duplicated items ----------------------------------------------------------------------
        for (int i = 0; i < mostRecentDevicesInfo.size(); i++) {
            for (int j = i + 1; j < mostRecentDevicesInfo.size(); j++) {
                if (mostRecentDevicesInfo.get(j).toString().equals(mostRecentDevicesInfo.get(i).toString())) {
                    mostRecentDevicesInfo.remove(mostRecentDevicesInfo.get(j));
                    j--;
                }
            }
        }
        //-----------------------------------------------------------------------------------------------------
        if (this.devicesInfo.isEmpty()) {
            updateDevicesInfo();
        } else if (!updatePending && runUpdater) {
            updatePending = true;
            handler.postDelayed(delayedUpdater, DISCOVERY_UPDATE_PERIOD);

        }
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
            for (int i = 0; i < mostRecentDevicesInfo.size(); i++){
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

        filter(comp);

        notifyDataSetChanged();
    }

    public void filterDevices(Comparator<T> comparator) {
        if (devicesInfo.isEmpty()){
            devicesInfo.addAll(mostRecentDevicesInfo);
        } else {
            for(T device : mostRecentDevicesInfo) {
                T btinfo = mostRecentDevicesInfo.get(mostRecentDevicesInfo.indexOf(device));
                if (devicesInfo.contains(btinfo)){
                    int index = devicesInfo.indexOf(device);
                    devicesInfo.remove(index);
                    devicesInfo.add(index,btinfo);
                } else {
                    devicesInfo.add(devicesInfo.size(),btinfo);
                    Log.d("filter","" + device.getAddress() + " added");
                }
            }

            if (devicesInfo.size()>mostRecentDevicesInfo.size()){
                for(T device : devicesInfo){
                    T btinfo = devicesInfo.get(devicesInfo.indexOf(device));
                    if(!devicesInfo.contains(btinfo)){
                        devicesInfo.remove(devicesInfo.indexOf(btinfo));
                        Log.d("filter","" + device.getAddress() + " removed");
                    }
                }
            }
        }


        if (this.devicesInfo.size() > 0) {
            Collections.sort(this.devicesInfo, comparator);
        }
    }

    public void clear() {
        mostRecentDevicesInfo.clear();
        mostRecentInfoAge.clear();

        if (!devicesInfo.isEmpty()) {
            int size = devicesInfo.size();
            Log.d("clear_devicesInfo","Called");
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
