package com.siliconlabs.bledemo.adapters;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;

import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.ViewGroup;

import com.siliconlabs.bledemo.ble.BluetoothDeviceInfo;
import com.siliconlabs.bledemo.ble.Discovery;
import com.siliconlabs.bledemo.utils.FilterDeviceParams;
import com.siliconlabs.bledemo.utils.SharedPrefUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
    private final Context context;
    private List<T> currentDevicesInfo;
    HashMap<String, String> deviceMacAddressToName = new HashMap<>();

    private boolean runUpdater = true;
    private final Handler handler = new Handler();
    private final Comparator<T> reverseItemsComparator = new Comparator<T>() {
        @Override
        public int compare(T lhs, T rhs) {
            boolean lFav = favoriteDevices.contains(lhs.getAddress());
            boolean rFav = favoriteDevices.contains(rhs.getAddress());
            int sComp = Boolean.compare(rFav, lFav);
            if (sComp != 0) {
                return sComp;
            }

            final String lName = lhs.scanInfo.getDisplayName(true);
            final String rName = rhs.scanInfo.getDisplayName(true);
            return rName.compareTo(lName);
        }
    };
    boolean isThermometerMode = false;
    private int comp = 5;
    private String search = null;
    private final Comparator<T> rssiComparator = new Comparator<T>() {
        @Override
        public int compare(T lhs, T rhs) {
            boolean lFav = favoriteDevices.contains(lhs.getAddress());
            boolean rFav = favoriteDevices.contains(rhs.getAddress());
            int sComp = Boolean.compare(rFav, lFav);
            if (sComp != 0) {
                return sComp;
            }

            final int lrssi = lhs.scanInfo.getRssi();
            final int rrssi = rhs.scanInfo.getRssi();
            return ((Integer) rrssi).compareTo(lrssi);
        }
    };
    public BluetoothDeviceInfo debugModeConnectingDevice;
    private final Comparator<T> reverseRssiComparator = new Comparator<T>() {
        @Override
        public int compare(T lhs, T rhs) {
            boolean lFav = favoriteDevices.contains(lhs.getAddress());
            boolean rFav = favoriteDevices.contains(rhs.getAddress());
            int sComp = Boolean.compare(rFav, lFav);
            if (sComp != 0) {
                return sComp;
            }

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
            boolean lFav = favoriteDevices.contains(lhs.getAddress());
            boolean rFav = favoriteDevices.contains(rhs.getAddress());
            int sComp = Boolean.compare(rFav, lFav);
            if (sComp != 0) {
                return sComp;
            }

            final String lName = lhs.scanInfo.getDisplayName(true);
            final String rName = rhs.scanInfo.getDisplayName(true);
            return lName.compareTo(rName);
        }
    };

    private final Comparator<T> timeComparator = new Comparator<T>() {
        @Override
        public int compare(T lhs, T rhs) {
            boolean lFav = favoriteDevices.contains(lhs.getAddress());
            boolean rFav = favoriteDevices.contains(rhs.getAddress());
            int sComp = Boolean.compare(rFav, lFav);
            if (sComp != 0) {
                return sComp;
            }

            final Long lTimestampNanos = lhs.scanInfo.getTimestampNanos();
            final Long rTimestampNanos = rhs.scanInfo.getTimestampNanos();
            return lTimestampNanos.compareTo(rTimestampNanos);
        }
    };

    private final Comparator<T> onlyFavoriteComparator = new Comparator<T>() {
        @Override
        public int compare(T lhs, T rhs) {
            boolean lFav = favoriteDevices.contains(lhs.getAddress());
            boolean rFav = favoriteDevices.contains(rhs.getAddress());
            return Boolean.compare(rFav, lFav);
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

    private ListItemListener listItemListener;
    private FilterDeviceParams filterDeviceParams;
    private SharedPrefUtils sharedPrefUtils;
    private LinkedHashSet<String> favoriteDevices;

    public void sort(int comparator, boolean resetDeviceList) {
        this.favoriteDevices = sharedPrefUtils.getFavoritesDevices();
        comp = comparator;
        switch (comparator) {
            case 0:
                sortDevices(itemsComparator, resetDeviceList);
                break;
            case 1:
                sortDevices(reverseItemsComparator, resetDeviceList);
                break;
            case 2:
                sortDevices(rssiComparator, resetDeviceList);
                break;
            case 3:
                sortDevices(reverseRssiComparator, resetDeviceList);
                break;
            case 4:
                sortDevices(timeComparator, resetDeviceList);
                break;
            case 5:
                sortDevices(onlyFavoriteComparator, resetDeviceList);
                break;
            default:
                break;
        }
    }

    public ScannedDevicesAdapter(DeviceInfoViewHolder.Generator generator, Context context) {
        this.generator = generator;
        this.context = context;
        this.sharedPrefUtils = new SharedPrefUtils(context);
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
        holder.setData(info, position,getItemCount());

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

    public void updateWith(List<T> devicesInfo) {
        this.currentDevicesInfo = devicesInfo;
        if (this.devicesInfo.isEmpty()) {
            if (filterDeviceParams == null || filterDeviceParams.isEmptyFilter()) {
                updateDevicesInfo();
            } else if (!updatePending) {
                updatePending = true;
                handler.postDelayed(delayedUpdater, 1000);
            }
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
        preapareDevicesInfo(this.currentDevicesInfo);
        updatePending = false;
        mostRecentDevicesInfoIsDirty = true;
        resetDeviceList();
        sort(comp, false);
        if (filterDeviceParams != null) {
            filterDevices(filterDeviceParams, false);
        }
        if (listItemListener != null) {
            listItemListener.emptyItemList(this.devicesInfo.isEmpty());
        }

        notifyDataSetChanged();
    }

    private void preapareDevicesInfo(List<T> devicesInfo) {
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

    public void sortDevices(Comparator<T> comparator, boolean resetDeviceList) {
        if (resetDeviceList) {
            resetDeviceList();
        }
        if (this.devicesInfo.size() > 0) {
            Collections.sort(this.devicesInfo, comparator);
        }
    }

    private boolean isNameOrAddressContain(FilterDeviceParams filterDeviceParams, T device) {
        return device.getAddress().toLowerCase().contains(filterDeviceParams.getName().toLowerCase())
                || (device.getName() != null && device.getName().toLowerCase().contains(filterDeviceParams.getName().toLowerCase()));
    }

    public void filterDevices(FilterDeviceParams filterDeviceParams, boolean resetDeviceList) {
        this.filterDeviceParams = filterDeviceParams;
        if (resetDeviceList) {
            resetDeviceList();
        }
        if (this.filterDeviceParams.isEmptyFilter()) return;
        LinkedHashSet<String> favorites = sharedPrefUtils.getFavoritesDevices();
        LinkedHashSet<String> tmpFavorites = sharedPrefUtils.getTemporaryFavoritesDevices();
        for (Iterator<T> deviceIterator = devicesInfo.iterator(); deviceIterator.hasNext(); ) {
            T device = deviceIterator.next();
            if (!isNameOrAddressContain(filterDeviceParams, device)) {
                deviceIterator.remove();
                continue;
            }
            if (filterDeviceParams.isRssiFlag() && device.getRssi() < filterDeviceParams.getRssiValue()) {
                deviceIterator.remove();
                continue;
            }
            if (!filterDeviceParams.getBleFormats().isEmpty() && !filterDeviceParams.getBleFormats().contains(device.getBleFormat())) {
                deviceIterator.remove();
                continue;
            }

            if (filterDeviceParams.getAdvertising() != null && !filterDeviceParams.getAdvertising().equals("")) {
                if (device.scanInfo != null && device.scanInfo.getAdvertData() != null && !device.scanInfo.getAdvertData().isEmpty()) {

                    boolean containText = false;

                    if (device.rawData.toLowerCase().contains(filterDeviceParams.getAdvertising().toLowerCase())) {
                        containText = true;
                    }

                    if (device.getAddress() != null && !device.getAddress().equals("") && device.getAddress().toLowerCase().contains(filterDeviceParams.getAdvertising().toLowerCase())) {
                        containText = true;
                    }
                    if (!containText) {
                        deviceIterator.remove();
                        continue;
                    }
                }
            }

            if (filterDeviceParams.isOnlyFavourite() && !(favorites.contains(device.getAddress()) || tmpFavorites.contains(device.getAddress()))) {
                deviceIterator.remove();
                continue;
            }

            if (filterDeviceParams.isOnlyConnectable() && !device.isConnectable) {
                deviceIterator.remove();
                continue;
            }
        }
    }

    public void clear() {
        mostRecentDevicesInfo.clear();
        mostRecentInfoAge.clear();

        if (!devicesInfo.isEmpty()) {
            Log.d("clear_devicesInfo", "Called");
            devicesInfo.clear();
            notifyDataSetChanged();
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

    public void setListItemListener(ListItemListener listener) {
        this.listItemListener = listener;
    }

    public interface ListItemListener {
        void emptyItemList(boolean empty);
    }
}
