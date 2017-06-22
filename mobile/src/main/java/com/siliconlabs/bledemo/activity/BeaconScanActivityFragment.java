package com.siliconlabs.bledemo.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.ble.BluetoothDeviceInfo;
import com.siliconlabs.bledemo.ble.Discovery;
import com.siliconlabs.bledemo.ble.ScanRecordCompat;
import com.siliconlabs.bledemo.utils.Proximity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class BeaconScanActivityFragment extends Fragment implements Discovery.BluetoothDiscoveryHost, Discovery.DeviceContainer {
    @InjectView(R.id.beacon_scan_radar)
    ImageView radarImage;
    @InjectView(R.id.beacon_scan_overlay)
    View scanOverlay;
    @InjectView(R.id.beacon_header_text)
    TextView headerText;
    @InjectView(R.id.beacon_name_text)
    TextView nameText;
    @InjectView(R.id.beacon_comment_text)
    TextView commentText;
    @InjectView(R.id.beacon_debug_text)
    TextView debugText;

    private AtomicLong noneFoundTimestamp = new AtomicLong(-1);
    final private Handler handler = new Handler();
    final Discovery discovery = new Discovery(this, this);
    final IntListMap deviceMap = new IntListMap();

    //UUID of the devices we're looking for
    final byte[] UUID_BYTES = {(byte) 0xfe, (byte) 0xed, (byte) 0xab, (byte) 0xba,
            (byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef,
            (byte) 0xfe, (byte) 0xed, (byte) 0xab, (byte) 0xba,
            (byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef};

    private Comparator<BluetoothDeviceInfo> comparator = new Comparator<BluetoothDeviceInfo>() {
        @Override
        public int compare(BluetoothDeviceInfo lhs, BluetoothDeviceInfo rhs) {
            int p1 = lhs.scanInfo.getRssi();
            int p2 = rhs.scanInfo.getRssi();
            //note: these are all negative, so it looks backwards
            if (p1 < p2) {
                return 1;
            } else if (p2 < p1) {
                return -1;
            }
            return 0;
        }
    };

    private Runnable timerRunnable = new Runnable() {
        //update the UI every second
        @Override
        public void run() {
            if (handler != null) {
                handler.postDelayed(timerRunnable, 1000);
            }
            long timeSinceNoneFound = noneFoundTimestamp.get();
            if (timeSinceNoneFound > 0 && System.currentTimeMillis() - timeSinceNoneFound > 3000) {
                clearAndDisplayScanning();
            } else {
                displayBeacon();
            }

        }
    };

    private void clearAndDisplayScanning() {
        scanOverlay.setVisibility(View.VISIBLE);
        noneFoundTimestamp.set(-1);
    }

    private void displayBeacon() {
        SimplifiedAdvertisement advertisement = deviceMap.getNearest();
        if (advertisement != null) {
            Log.d("display", "called");
            scanOverlay.setVisibility(View.GONE);
            headerText.setText(String.format(getString(R.string.beacon_range),
                    Proximity.getProximityStringUppercase(getResources(), advertisement.rssi, advertisement.txPower)));
            nameText.setText(String.format(getString(R.string.beacon_title), advertisement.minorNumber));
            commentText.setText(String.format(getString(R.string.beacon_comment_message), advertisement.minorNumber));
            //debug
            debugText.setText(String.format("RSSI:%d Distance:%.2f", advertisement.rssi, Proximity.getDistance(advertisement)));
        }
    }

    public BeaconScanActivityFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        discovery.connect(activity);
    }

    @Override
    public void onDetach() {
        discovery.disconnect();
        super.onDetach();
    }

    private void startTimer() {
        handler.postDelayed(timerRunnable, 100);
    }

    private void stopTimer() {
        handler.removeCallbacks(timerRunnable);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_beacon_scan, container, false);
        ButterKnife.inject(this, view);
        radarImage.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.beacon_scan_anim));
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        clearAndDisplayScanning();
        startTimer();
        reDiscover(true);
    }

    private void reDiscover(boolean clearCachedDiscoveries) {
        discovery.startDiscovery(clearCachedDiscoveries);
    }

    @Override
    public boolean isReady() {
        return isResumed();
    }

    @Override
    public void reDiscover() {
        reDiscover(true);
    }

    @Override
    public void onAdapterDisabled() {

    }

    @Override
    public void onAdapterEnabled() {

    }

    @Override
    public void onPause() {
        super.onPause();
        stopTimer();
        discovery.stopDiscovery(true);
    }

    @Override
    public void flushContainer() {

    }

    @Override
    public void updateWithDevices(List devices) {
        //first, sort readings by rssi strength
        Collections.sort(devices, comparator);
        for (int i = 0; i < devices.size(); i++) {
            BluetoothDeviceInfo deviceInfo = (BluetoothDeviceInfo) devices.get(i);
            ScanRecordCompat scanRecord = deviceInfo.scanInfo.getScanRecord();
            byte[] bytes = scanRecord.getBytes();

            //if we find a device that meets the criteria, add its record to the deviceMap - this is the closest
            //the timerRunnable above will use the deviceMap to figure out the closest device over some interval, and update UI
            if (bytes.length > 30) {
                bytes = Arrays.copyOfRange(bytes, 0, 30);
                if (validateBytes(bytes)) {
                    noneFoundTimestamp.set(-1);
                    String name = deviceInfo.scanInfo.getDevice().getName();
                    //WARNING - NOT EVERY DEVICE READS THE NAME (Nexus 9)
                    deviceMap.add(name, deviceInfo.scanInfo.getRssi(), getMinorNumberFromBytes(bytes), (int) bytes[29]);
                    return;
                }
            }
        }

        //keep track any time we don't read any beacons
        noneFoundTimestamp.compareAndSet(-1, System.currentTimeMillis());
    }

    public boolean validateBytes(byte[] bytes) {
        //CURRENTLY ONLY CHECKING THE UUID - not the structure of the advertisement
        for (int i = 0; i < UUID_BYTES.length; i++) {
            if (bytes[i + 9] != UUID_BYTES[i]) {
                return false;
            }
        }
        return true;
    }

    public static int getMinorNumberFromBytes(byte[] bytes) {
        if (bytes.length < 30) {
            return -1;
        }
        int converted = (bytes[28] & 0xff) << 8;
        converted |= (bytes[27] & 0xff);
        return converted;
    }

    //USEFUL METHOD FOR DEBUGGING
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    //Used to package up all of the info needed by the UI
    public static class SimplifiedAdvertisement {
        public String deviceName;
        public Integer rssi;
        public Integer minorNumber;
        public Integer txPower;

        public SimplifiedAdvertisement(String deviceName, Integer rssi, Integer minorNumber, Integer txPower) {
            this.deviceName = deviceName;
            this.rssi = rssi;
            this.minorNumber = minorNumber;
            this.txPower = txPower;
        }
    }

    //Following data structures used to map lists of UUID readings to specific devices
    public static class ListMap<K, T> {
        protected HashMap<K, ArrayList<T>> map = new HashMap<>();

        public synchronized boolean add(K key, T item) {
            boolean alreadyIn = false;
            ArrayList<T> list = map.get(key);
            if (list == null) {
                list = new ArrayList<>();
                list.add(item);
                map.put(key, list);
            } else {
                list.add(item);
                alreadyIn = true;
            }
            return alreadyIn;
        }

        public void clear() {
            map.clear();
        }
    }

    public static class IntListMap extends ListMap<String, Integer> {
        private HashMap<String, Integer> minorMap = new HashMap<>();
        private HashMap<String, Integer> txMap = new HashMap<>();

        @Override
        public void clear() {
            minorMap.clear();
            txMap.clear();
            super.clear();
        }

        public synchronized boolean add(String key, Integer item, Integer minorNumber, Integer txPower) {
            minorMap.put(key, minorNumber);
            txMap.put(key, txPower);
            return add(key, item);
        }

        public synchronized SimplifiedAdvertisement getNearest() {
            int longestLength = 0;
            String longest = null;
            ArrayList<Integer> longestList = null;
            //find the longest list in the map - this device was closest, most often
            for (String key : map.keySet()) {
                ArrayList<Integer> list = map.get(key);
                if (list.size() > longestLength) {
                    longest = key;
                    longestLength = list.size();
                    longestList = list;
                }
            }

            if (longestLength == 0) {
                return null;
            }

            //average the rssi values
            long runningTotal = 0;
            for (Integer i : longestList) {
                runningTotal += i;
            }

            int finalTotal = (int) (runningTotal / longestLength);
            SimplifiedAdvertisement advertisement = new SimplifiedAdvertisement(longest, finalTotal, minorMap.get(longest), txMap.get(longest));
            clear();
            return advertisement;
        }
    }
}
