package com.siliconlabs.bledemo.activity;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.adapters.BeaconScreenScannedDevicesAdapter;
import com.siliconlabs.bledemo.adapters.DeviceInfoViewHolder;
import com.siliconlabs.bledemo.beaconutils.BleFormat;
import com.siliconlabs.bledemo.ble.BlueToothService;
import com.siliconlabs.bledemo.ble.BluetoothDeviceInfo;
import com.siliconlabs.bledemo.ble.Discovery;
import com.siliconlabs.bledemo.ble.ScanResultCompat;
import com.siliconlabs.bledemo.utils.Proximity;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;

public class BeaconListFragment extends Fragment implements Discovery.BluetoothDiscoveryHost, Discovery.DeviceContainer {

    private BlueToothService.GattConnectType connectType;
    private BlueToothService.Binding bluetoothBinding;
    private GridLayoutManager layout;
    private RecyclerView.ItemDecoration itemDecoration;
    private boolean scan = false;

    @InjectView(android.R.id.list)
    RecyclerView listView;
    @InjectView(R.id.beacon_scan_radar_bar)
    LinearLayout radarBar;
    @InjectView(R.id.beacon_scan_radar)
    ImageView radarImage;
    @InjectView(R.id.beacon_radar_text)
    TextView radarText;

    final BeaconScreenScannedDevicesAdapter adapter = new BeaconScreenScannedDevicesAdapter(new DeviceInfoViewHolder.Generator(
            R.layout.beacon_device_item) {
        @Override
        public DeviceInfoViewHolder generate(View itemView) {
            final ViewHolder holder = new ViewHolder(itemView);
            holder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int adapterPos = holder.getAdapterPosition();
                    if (adapterPos != RecyclerView.NO_POSITION) {
                        BluetoothDeviceInfo devInfo = (BluetoothDeviceInfo) adapter.getDevicesInfo().get(adapterPos);
                        // Beacon selected, handle event here or in BeaconScreenScannedDevicesAdapter
                    }
                }
            });
            return holder;
        }
    });

    private Runnable timerRunnable = new Runnable() {
        //update the UI every second
        @Override
        public void run() {
            if (handler != null) {
                handler.postDelayed(timerRunnable, 1000);
                if (adapter.getItemCount() > 0) {
                    radarText.setText(R.string.scanning_for_additional_beacon);
                } else {
                    radarText.setText(R.string.scanning_for_beacons);
                }
            }
        }
    };

    final private Handler handler = new Handler();
    Discovery discovery = new Discovery(this, this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        adapter.setHasStableIds(true);
    }

    public BeaconListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        discovery.connect(activity);
        layout = new GridLayoutManager(activity,
                                       activity.getResources().getInteger(R.integer.device_selection_columns),
                                       LinearLayoutManager.VERTICAL,
                                       false);
        itemDecoration = new RecyclerView.ItemDecoration() {
            final int horizontalMargin = getResources().getDimensionPixelSize(R.dimen.item_margin);

            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                final int columns = layout.getSpanCount();
                if (columns == 1) {
                    outRect.set(0, 0, 0, 0);
                } else {
                    int itemPos = parent.getChildAdapterPosition(view);
                    if (itemPos % columns == columns - 1) {
                        outRect.set(0, 0, 0, 0);
                    } else {
                        outRect.set(0, 0, horizontalMargin, 0);
                    }
                }
            }
        };
    }

    @Override
    public void onPause() {
        super.onPause();
        stopTimer();
        discovery.stopDiscovery(true);
    }

    @Override
    public void onDetach() {
        discovery.disconnect();
        if (bluetoothBinding != null) {
            bluetoothBinding.unbind();
        }
        super.onDetach();
    }

    private void startTimer() {
        handler.postDelayed(timerRunnable, 100);
    }

    private void stopTimer() {
        handler.removeCallbacks(timerRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        startTimer();
        reDiscover(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_beacon_list, container, false);
        ButterKnife.inject(this, view);
        listView.setLayoutManager(layout);
        listView.addItemDecoration(itemDecoration);
        listView.setHasFixedSize(true);
        listView.setAdapter(adapter);
        radarImage.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.beacon_scan_anim));
        final Button scanner = (Button) view.findViewById(R.id.cancel_beacon_scanning);
        scanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (scan) {
                    startTimer();
                    radarImage.setVisibility(View.VISIBLE);
                    discovery.startDiscovery(true);
                    radarImage.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.beacon_scan_anim));
                    scanner.setBackgroundResource(R.drawable.cancel_scanning);
                    radarText.setText("Scanning beacons...");
                    scan = false;
                } else {
                    radarImage.clearAnimation();
                    radarImage.setVisibility(View.INVISIBLE);
                    radarText.setText("Scan for new beacons");
                    stopTimer();
                    scanner.setBackgroundResource(R.drawable.play_icon_24);
                    discovery.stopDiscovery(false);
                    scan = true;
                }
            }
        });
        return view;
    }

    @Override
    public boolean isReady() {
        return isResumed();
    }

    @Override
    public void reDiscover() {
        reDiscover(false);
    }

    @Override
    public void onAdapterDisabled() {
        radarBar.setVisibility(View.GONE);
    }

    @Override
    public void onAdapterEnabled() {
        adapter.clear();
        discovery.disconnect();
        discovery.connect(getActivity());
        reDiscover(true);
        radarBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void flushContainer() {
        adapter.flushContainer();
    }

    @Override
    public void updateWithDevices(List devices) {
        ArrayList<BluetoothDeviceInfo> filteredDevices = new ArrayList<>();
        for (Object device : devices) {
            BluetoothDeviceInfo deviceInfo = (BluetoothDeviceInfo) device;
            BleFormat format = deviceInfo.getBleFormat();
            if (format == BleFormat.I_BEACON || format == BleFormat.BLUE_GECKO
                    || format == BleFormat.EDDYSTONE || format == BleFormat.ALT_BEACON) {
                filteredDevices.add(deviceInfo);
            }
        }
        adapter.updateWithDevices(filteredDevices);
    }

    public void updateWithDevicesRSSI(List devices) {
        ArrayList<BluetoothDeviceInfo> filteredDevices = new ArrayList<>();
        for (Object device : devices) {
            BluetoothDeviceInfo deviceInfo = (BluetoothDeviceInfo) device;
            filteredDevices.add(deviceInfo);
        }
        adapter.updateWithDevices(filteredDevices);
    }

    public void reDiscover(boolean clearCachedDiscoveries) {
        discovery.startDiscovery(clearCachedDiscoveries);
    }

    public List getDevicesInfo() {
        return adapter.getDevicesInfo();
    }

    public static class ViewHolder extends DeviceInfoViewHolder {
        @Optional
        @InjectView(R.id.container)
        public RelativeLayout container;
        @Optional
        @InjectView(R.id.beacon_list_header_label)
        public TextView beaconListHeaderLabel;
        @Optional
        @InjectView(R.id.beacon_list_item_separator)
        public View beaconListItemSeparator;
        @InjectView(android.R.id.icon)
        public ImageView icon;
        @InjectView(android.R.id.title)
        public TextView title;
        @InjectView(R.id.beacon_type_text)
        public TextView beaconType;
        @InjectView(R.id.rssi_text)
        public TextView rssiText;
        @InjectView(R.id.tx_text)
        public TextView txText;
        @InjectView(R.id.tx_text_label)
        public TextView txLabel;
        @InjectView(R.id.beacon_type_img)
        public ImageView typeIcon;
        @InjectView(R.id.beacon_range_text)
        public TextView rangeText;
        @InjectView(R.id.device_mac_address)
        public TextView macAddress;
        // iBeacon and bluegecko
        @InjectView(R.id.ibeacon_info_container)
        public LinearLayout iBeaconInfoContainer;
        @InjectView(R.id.scan_info_uuid)
        public TextView scanInfoUuid;
        @InjectView(R.id.major_number)
        public TextView majorNumber;
        @InjectView(R.id.minor_number)
        public TextView minorNumber;
        // eddystone
        @InjectView(R.id.eddystone_info_container)
        public LinearLayout eddystoneInfoContainer;
        @InjectView(R.id.eddystone_url)
        public TextView eddystoneUrl;
        @InjectView(R.id.eddystone_uid)
        public TextView eddystoneUid;
        @InjectView(R.id.eddystone_tlm)
        public TextView eddystoneTlm;
        // AltBeacon
        @InjectView(R.id.alt_beacon_info_container)
        public LinearLayout altBeaconInfoContainer;
        @InjectView(R.id.alt_beacon_id)
        public TextView altBeaconId;
        @InjectView(R.id.manufacturer_id)
        public TextView altBeaconManufacturerId;
        @InjectView(R.id.reference_rssi)
        public TextView altBeaconReferenceRssi;

        BluetoothDeviceInfo btInfo;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
        }

        @Override
        public void setData(BluetoothDeviceInfo btInfo, int position) {
            this.btInfo = btInfo;

            ScanResultCompat scanInfo = btInfo.scanInfo;

            if (TextUtils.isEmpty(scanInfo.getScanRecord().getDeviceName())) {
                title.setText("Unknown");
            } else {
                String displayName = scanInfo.getDisplayName(false).trim();
                title.setText(displayName);
            }
            Proximity proximity = Proximity.getProximity(btInfo.getRssi(),
                                                         btInfo.scanInfo.getScanRecord()
                                                                 .getTxPowerLevel()); //Math.max(0, scanInfo.getRssi() + 80);
            if (btInfo.scanInfo.getScanRecord().getTxPowerLevel() > (-100)) {
                txText.setText("" + (btInfo.scanInfo.getScanRecord().getTxPowerLevel()));
            } else {
                txText.setVisibility(View.GONE);
                txLabel.setVisibility(View.GONE);
            }
            rssiText.setText("" + scanInfo.getRssi());
            beaconType.setText(btInfo.getBleFormat().getNameResId());
            typeIcon.setImageResource(btInfo.getBleFormat().getIconResId());
            icon.setImageLevel(proximity.ordinal());
            rangeText.setText(proximity.getStringId());
            macAddress.setText("MAC: " + btInfo.getAddress());
            itemView.setOnClickListener(this);
        }
    }
}
