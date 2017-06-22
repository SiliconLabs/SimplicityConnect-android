package com.siliconlabs.bledemo.adapters;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.ble.BluetoothDeviceInfo;
import com.siliconlabs.bledemo.ble.Discovery;
import com.siliconlabs.bledemo.ble.ScanResultCompat;
import com.siliconlabs.bledemo.interfaces.FindKeyFobCallback;
import com.siliconlabs.bledemo.utils.SignalStrength;
import com.siliconlabs.bledemo.views.KeyFobSignalStrength;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;

public class KeyFobsRecyclerViewAdapter extends RecyclerView.Adapter<KeyFobsRecyclerViewAdapter.CustomViewHolder> implements Discovery.DeviceContainer {
    private final static int TYPE_FOBS_HEADER = 0;
    private final static int TYPE_FOBS = 1;
    private final static int TYPE_OTHER_FOBS_HEADER = 2;
    private final static int TYPE_OTHER_FOBS = 3;
    final static long MAX_AGE = 10000;

    private int numbBlueGeckoFobs = 0;
    private boolean runHandler = true;
    boolean mostRecentDevicesInfoIsDirty;
    boolean updatePending;

    final List<BluetoothDeviceInfo> mostRecentDevicesInfo = new ArrayList<>();
    final Map<BluetoothDeviceInfo, Long> mostRecentInfoAge = new HashMap<>();
    private List<BluetoothDeviceInfo> devicesInfo = new ArrayList<>();
    private FindKeyFobCallback findKeyFobSelectedCallback;
    private Context context;
    final Handler handler = new Handler();

    final Runnable delayedUpdater = new Runnable() {
        @Override
        public void run() {
            updateDevicesInfo();
        }
    };

    final Comparator<BluetoothDeviceInfo> itemsComparator = new Comparator<BluetoothDeviceInfo>() {
        @Override
        public int compare(BluetoothDeviceInfo lhs, BluetoothDeviceInfo rhs) {
            if (lhs.isOfInterest && !rhs.isOfInterest) {
                return -1;
            } else if (!lhs.isOfInterest && rhs.isOfInterest) {
                return 1;
            }

            final String lName = getDisplayName(lhs.scanInfo, true);
            final String rName = getDisplayName(rhs.scanInfo, true);
            return lName.compareTo(rName);
        }
    };

    public KeyFobsRecyclerViewAdapter(Context context, FindKeyFobCallback findKeyFobSelectedCallback) {
        this.context = context;
        this.findKeyFobSelectedCallback = findKeyFobSelectedCallback;
        addHeaders();
    }

    public void stopUpdateHandler() {
        if (handler != null) {
            runHandler = false;
            handler.removeCallbacks(delayedUpdater);
        }
    }

    public void startUpdateHandler() {
        runHandler = true;
    }

    @Override
    public int getItemViewType(int position) {
        this.numbBlueGeckoFobs = getNumbBlueGeckoFobs();
        if (position == 0) {
            return TYPE_FOBS_HEADER;
        } else if (position <= this.numbBlueGeckoFobs) {
            return TYPE_FOBS;
        } else if (position == this.numbBlueGeckoFobs + 1) {
            return TYPE_OTHER_FOBS_HEADER;

        } else {
            return TYPE_OTHER_FOBS;
        }
    }

    @Override
    public CustomViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_fob, viewGroup, false);
        return new CustomViewHolder(itemView, viewGroup.getContext());
    }

    @Override
    public void onBindViewHolder(CustomViewHolder customViewHolder, int position) {
        this.numbBlueGeckoFobs = getNumbBlueGeckoFobs();

        int itemViewType = getItemViewType(position);
        final BluetoothDeviceInfo info = devicesInfo.get(position);

        switch (itemViewType) {
            case TYPE_FOBS_HEADER:
                // bluegecko fob header
                customViewHolder.setData(true, true, null);
                if (customViewHolder.cardView != null) {
                    customViewHolder.cardView.setBackgroundColor(context.getResources().getColor(android.R.color.white));
                }
                break;
            case TYPE_FOBS:
                // bluegecko fob item
                customViewHolder.setData(false, true, info);
                // following prevents a double separator from being drawn
                if (position == getNumbBlueGeckoFobs()) {
                    customViewHolder.itemDividerBelow.setVisibility(View.GONE);
                }
                if (customViewHolder.cardView != null) {
                    customViewHolder.cardView.setBackgroundColor(context.getResources().getColor(android.R.color.white));
                }
                break;
            case TYPE_OTHER_FOBS_HEADER:
                // other fobs header
                customViewHolder.setData(true, false, null);
                if (customViewHolder.cardView != null) {
                    customViewHolder.cardView.setBackgroundColor(context.getResources().getColor(R.color.transparent));
                }
                break;
            case TYPE_OTHER_FOBS:
                customViewHolder.setData(false, false, info);
                if (customViewHolder.cardView != null) {
                    customViewHolder.cardView.setBackgroundColor(context.getResources().getColor(R.color.transparent));
                }
                break;
            default:
                break;
        }
    }

    @Override
    public int getItemCount() {
        return ((null != devicesInfo ? devicesInfo.size() : 0));
    }

    @Override
    public long getItemId(int position) {
        //These ids are for the headers
        if (position == 0) {
            return -5L;
        } else if (position == getNumbBlueGeckoFobs() + 1) {
            return -6L;
        }

        final BluetoothDeviceInfo info = devicesInfo.get(position);
        return info.device.getAddress().hashCode();
    }

    @Override
    public void flushContainer() {
        clear();
    }

    @Override
    public void updateWithDevices(List devicesInfo) {
        final Long now = System.currentTimeMillis();

        mostRecentDevicesInfoIsDirty = true;

        if ((devicesInfo != null) && !devicesInfo.isEmpty()) {
            for (Object info : devicesInfo) {
                BluetoothDeviceInfo devInfo = (BluetoothDeviceInfo) info;
                BluetoothDeviceInfo clone = devInfo.clone();

                clone.isOfInterest = true;
                clone.isNotOfInterest = false;
                clone.serviceDiscoveryFailed = false;
                devInfo.isOfInterest = true;
                devInfo.isNotOfInterest = false;
                devInfo.serviceDiscoveryFailed = false;

                // the key fobs we are interested in are the blue geck ones
                String name = devInfo.device.getName();
                if (!TextUtils.isEmpty(name)) {
                    if (name.toUpperCase().contains("BLUE GECKO") || name.toUpperCase().contains("BG")) {
                        clone.isOfInterest = true;
                        clone.isNotOfInterest = false;
                        clone.serviceDiscoveryFailed = false;
                        devInfo.isOfInterest = true;
                        devInfo.isNotOfInterest = false;
                        devInfo.serviceDiscoveryFailed = false;
                    } else {
                        clone.isOfInterest = false;
                        clone.isNotOfInterest = true;
                        clone.serviceDiscoveryFailed = false;
                        devInfo.isOfInterest = false;
                        devInfo.isNotOfInterest = true;
                        devInfo.serviceDiscoveryFailed = false;
                    }
                }

                int index = mostRecentDevicesInfo.indexOf(clone);
                if (index >= 0) {
                    BluetoothDeviceInfo cachedInfo = mostRecentDevicesInfo.get(index);
                    long timestampDiff = clone.scanInfo.getTimestampNanos() - cachedInfo.scanInfo.getTimestampNanos();
                    if (timestampDiff != 0) {
                        mostRecentDevicesInfo.set(index, clone);
                        mostRecentInfoAge.put(devInfo, now);
                    }
                } else {
                    mostRecentDevicesInfo.add(devInfo);
                    mostRecentInfoAge.put(devInfo, now);
                }
            }
        }

        final Iterator<Map.Entry<BluetoothDeviceInfo, Long>> iter = mostRecentInfoAge.entrySet().iterator();
        while (iter.hasNext()) {
            final Map.Entry<BluetoothDeviceInfo, Long> ageEntry = iter.next();
            long age = now.longValue() - ageEntry.getValue().longValue();
            if (age > MAX_AGE) {
                mostRecentDevicesInfo.remove(ageEntry.getKey());
                iter.remove();
            }
        }

        if (this.devicesInfo.isEmpty()) {
            updateDevicesInfo();
        } else if (!updatePending && runHandler) {
            updatePending = true;
            handler.postDelayed(delayedUpdater, 2000);
        }
    }

    private void updateDevicesInfo() {
        updatePending = false;
        mostRecentDevicesInfoIsDirty = false;

        if (mostRecentDevicesInfo.isEmpty()) {
            clear();
            this.devicesInfo.clear();
            this.numbBlueGeckoFobs = 0;
            this.devicesInfo.add(this.numbBlueGeckoFobs, null);
            this.devicesInfo.add(0, null);
            return;
        }

        this.devicesInfo.clear();
        this.devicesInfo.addAll(mostRecentDevicesInfo);

        if (this.devicesInfo.size() > 1) {
            Collections.sort(this.devicesInfo, itemsComparator);
        }

        // the two headers
        this.numbBlueGeckoFobs = getNumbBlueGeckoFobs();
        addHeaders();
        notifyDataSetChanged();
    }

    private static String getDisplayName(ScanResultCompat scanInfo, boolean forSorting) {
        final String name = scanInfo.getScanRecord().getDeviceName();
        return !TextUtils.isEmpty(name) ? " " + name : scanInfo.getDevice().getAddress();
    }

    public void clear() {
        mostRecentDevicesInfo.clear();
        mostRecentInfoAge.clear();
        this.numbBlueGeckoFobs = 0;
        if (!devicesInfo.isEmpty()) {
            int size = devicesInfo.size();
            devicesInfo.clear();
            addHeaders();
            notifyItemRangeRemoved(0, size);
        }
    }

    private void addHeaders() {
        this.devicesInfo.add(this.numbBlueGeckoFobs, null);
        this.devicesInfo.add(0, null);
    }

    public int getNumbBlueGeckoFobs() {
        int numbBlueGeckoFobs = 0;
        for (BluetoothDeviceInfo deviceInfo : devicesInfo) {
            if (deviceInfo != null && deviceInfo.isOfInterest) {
                numbBlueGeckoFobs += 1;
            }
        }

        return numbBlueGeckoFobs;
    }

    public class CustomViewHolder extends RecyclerView.ViewHolder {
        boolean isHeader = false;
        boolean isBlueGecko = false;
        BluetoothDeviceInfo btInfo = null;
        Context context = null;

        @Optional
        @InjectView(R.id.card_view)
        LinearLayout cardView;

        @InjectView(R.id.list_item_outermost_container)
        LinearLayout container;

        @InjectView(R.id.list_item_entry_container)
        LinearLayout deviceContainer;
        @InjectView(R.id.signal_strength_graphic)
        KeyFobSignalStrength signalStrengthIcon;
        @InjectView(R.id.device_id_label)
        TextView deviceName;
        @InjectView(R.id.signal_strength_label)
        TextView signalStrengthText;
        @InjectView(R.id.find_btn)
        TextView findButton;
        @InjectView(R.id.list_item_entry_dividers)
        View itemDividerBelow;

        @InjectView(R.id.list_item_header_container)
        LinearLayout headerContainer;
        @InjectView(R.id.header_text)
        TextView headerText;
        @InjectView(R.id.header_divider_above)
        View headerDividerAbove;

        public CustomViewHolder(View view, Context context) {
            super(view);
            ButterKnife.inject(this, itemView);
            this.context = context;
        }

        void setData(boolean isHeader, boolean isBlueGecko, BluetoothDeviceInfo btInfo) {
            this.isHeader = isHeader;
            this.isBlueGecko = isHeader;
            this.btInfo = btInfo;
            if (isHeader) {
                setAsHeader(isBlueGecko);
            } else {
                setAsFobEntryItem(isBlueGecko, btInfo);
            }
        }

        private void setAsHeader(boolean isBlueGecko) {
            isHeader = true;
            deviceContainer.setVisibility(View.GONE);
            headerContainer.setVisibility(View.VISIBLE);

            int headerTextStringId = isBlueGecko ? R.string.fob_list_header_text : R.string.fob_list_other_fobs_header_text;
            headerText.setText(headerTextStringId);

            int headerContainerBackgroundColor = isBlueGecko ? Color.WHITE : context.getResources().getColor(R.color.other_fobs_header_background);
            headerContainer.setBackgroundColor(headerContainerBackgroundColor);

            itemDividerBelow.setVisibility(View.GONE);
        }

        private void setAsFobEntryItem(boolean isBlueGecko, BluetoothDeviceInfo btInfo) {
            isHeader = false;
            this.isBlueGecko = isBlueGecko;
            int devContainerColor = isBlueGecko ? Color.WHITE : context.getResources().getColor(R.color.other_fobs_header_background);
            deviceContainer.setBackgroundColor(devContainerColor);
            container.setBackgroundColor(devContainerColor);
            deviceContainer.setVisibility(View.VISIBLE);
            headerContainer.setVisibility(View.GONE);

            ScanResultCompat scanInfo = btInfo.scanInfo;
            String name = scanInfo.getDevice().getName();
            name = TextUtils.isEmpty(name) ? "" + scanInfo.getDevice().getAddress() : name;

            SignalStrength strength = SignalStrength.calculateSignalStrengthUsingRssi(scanInfo.getRssi());
            String stringLabelFromStrength = SignalStrength.getStringLabelFromStrength(context, strength);

            deviceName.setText(name);
            signalStrengthIcon.setSignalStrength(strength);
            signalStrengthText.setText(stringLabelFromStrength);

            findButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    findKeyFobSelectedCallback.findKeyFob(CustomViewHolder.this.btInfo);
                }
            });

            itemDividerBelow.setVisibility(View.VISIBLE);
        }
    }
}
