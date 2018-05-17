package com.siliconlabs.bledemo.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.activity.BaseActivity;
import com.siliconlabs.bledemo.activity.LightActivity;
import com.siliconlabs.bledemo.adapters.DeviceInfoViewHolder;
import com.siliconlabs.bledemo.adapters.ScannedDevicesAdapter;
import com.siliconlabs.bledemo.ble.BlueToothService;
import com.siliconlabs.bledemo.ble.BluetoothDeviceInfo;
import com.siliconlabs.bledemo.ble.Discovery;
import com.siliconlabs.bledemo.ble.GattService;
import com.siliconlabs.bledemo.ble.ScanResultCompat;
import com.siliconlabs.bledemo.ble.TimeoutGattCallback;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import timber.log.Timber;

public class SelectDeviceDialog extends DialogFragment implements Discovery.BluetoothDiscoveryHost {

    private static final String TITLE_INFO = "_title_info_";
    private static final String DESC_INFO = "_desc_info_";
    private static final String PROFILES_INFO = "_profiles_info_";
    private static final String CONN_TYPE_INFO = "_conn_type_info_";

    public static final int MAX_RETRY_ATTEMPTS = 2;
    public static final int RETRY_DELAY = 1000;

    public static SelectDeviceDialog newDialog(int titleInfo, int descriptionInfo, List<Pair<Integer, Integer>> profilesInfo, BlueToothService.GattConnectType connectType) {
        SelectDeviceDialog dialog = new SelectDeviceDialog();

        Bundle args = new Bundle();
        args.putInt(TITLE_INFO, titleInfo);
        args.putInt(DESC_INFO, descriptionInfo);

        ArrayList<Integer> profilesInfoList = new ArrayList<>();
        if (profilesInfo != null) {
            for (Pair<Integer, Integer> profileInfo : profilesInfo) {
                profilesInfoList.add(profileInfo.first);
                profilesInfoList.add(profileInfo.second);
            }
        }
        args.putIntegerArrayList(PROFILES_INFO, profilesInfoList);
        if (connectType != null) {
            args.putInt(CONN_TYPE_INFO, connectType.ordinal());
        }

        dialog.setArguments(args);
        return dialog;
    }

    @InjectView(android.R.id.title)
    TextView titleView;
    @InjectView(R.id.blue_gecko_tab)
    TextView blueGeckoTab;

    @InjectView(android.R.id.list)
    RecyclerView listView;

    @Optional
    @InjectView(android.R.id.text1)
    TextView descriptionView;
    @Optional
    @InjectView(R.id.profiles_used)
    ViewGroup profilesUsed;

    private GridLayoutManager layout;
    private RecyclerView.ItemDecoration itemDecoration;

    private int titleInfo;
    private int descriptionInfo;
    private List<Integer> profilesInfo;

    TimeoutGattCallback timeoutGattCallback = new TimeoutGattCallback() {
        @Override
        public void onTimeout() {
            ((BaseActivity) getActivity()).dismissModalDialog();
            Timber.d("Connection timeout");
            Toast.makeText(getActivity(), "Connection Timed Out", Toast.LENGTH_SHORT).show();
            bluetoothBinding.unbind();
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Timber.d("Connection State Change");
            Timber.d("Status: " + status);
            Timber.d("New State: " + newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                ((BaseActivity) getActivity()).dismissModalDialog();
                Intent intent = getIntent(connectType, getActivity());
                if(intent != null) {
                    bluetoothBinding.unbind();
                    startActivity(intent);
                }
            } else if (retryAttempts < MAX_RETRY_ATTEMPTS) {
                retryConnectionAttempt();
            } else {
                ((BaseActivity) getActivity()).dismissModalDialog();
                bluetoothBinding.unbind();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), "Connection Failed", Toast.LENGTH_SHORT).show();
                    }
                });
                reDiscover(true);
            }
        }
    };

    final ScannedDevicesAdapter adapter = new ScannedDevicesAdapter(new DeviceInfoViewHolder.Generator(R.layout.device_item) {
        @Override
        public DeviceInfoViewHolder generate(View itemView) {
            final ViewHolder holder = new ViewHolder(itemView);
            holder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int adapterPos = holder.getAdapterPosition();
                    if (adapterPos != RecyclerView.NO_POSITION) {
                        BluetoothDeviceInfo devInfo = (BluetoothDeviceInfo) adapter.getDevicesInfo().get(adapterPos);
                        connect(devInfo);
                    }
                }
            });
            return holder;
        }

    });

    final Discovery discovery = new Discovery(adapter, this);
    BluetoothGatt bluetoothGatt;
    private BlueToothService.GattConnectType connectType;
    private BlueToothService.Binding bluetoothBinding;
    private BlueToothService service;
    private BluetoothDeviceInfo deviceInfo;
    private int retryAttempts;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        adapter.setThermometerMode();
        adapter.setBlueGeckoTabSelected(true);

        discovery.connect(activity);

        layout = new GridLayoutManager(activity, activity.getResources().getInteger(R.integer.device_selection_columns), LinearLayoutManager.VERTICAL, false);
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
    public void onDetach() {
        discovery.disconnect();
        if (bluetoothBinding != null) {
            bluetoothBinding.unbind();
        }
        super.onDetach();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
        setStyle(STYLE_NO_TITLE, getTheme());

        Bundle arguments = getArguments();
        titleInfo = arguments.getInt(TITLE_INFO);
        descriptionInfo = arguments.getInt(DESC_INFO);
        profilesInfo = arguments.getIntegerArrayList(PROFILES_INFO);
        connectType = BlueToothService.GattConnectType.values()[arguments.getInt(CONN_TYPE_INFO, 0)];
        adapter.setHasStableIds(true);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_select_device, container, false);

        ButterKnife.inject(this, view);

        listView.setLayoutManager(layout);
        listView.addItemDecoration(itemDecoration);
        listView.setHasFixedSize(true);
        listView.setAdapter(adapter);

        titleView.setText(titleInfo);
        if (descriptionView != null) {
            descriptionView.setText(descriptionInfo);
        }

        if (profilesUsed != null) {
            if (profilesInfo.isEmpty()) {
                profilesUsed.setVisibility(View.GONE);
            } else {
                for (int i = 0; i < profilesInfo.size(); i += 2) {
                    View valuesItem = inflater.inflate(R.layout.demo_item_value, profilesUsed, false);
                    TextView titleView = ButterKnife.findById(valuesItem, android.R.id.text1);
                    TextView idView = ButterKnife.findById(valuesItem, android.R.id.text2);

                    titleView.setText(profilesInfo.get(i));
                    idView.setText(profilesInfo.get(i + 1));

                    profilesUsed.addView(valuesItem);
                }
            }
        }

        blueGeckoTab.setText(R.string.tab_blue_geckos);
        blueGeckoTab.setSelected(true);

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isEnabled() && getDialog() != null && getDialog().getWindow() != null) {
            int maxWidth = getResources().getDimensionPixelSize(R.dimen.device_selection_max_width);
            int maxHeight = getResources().getDimensionPixelSize(R.dimen.device_selection_max_height);
            if (maxWidth > 0 || maxHeight > 0) {
                WindowManager.LayoutParams attributes = getDialog().getWindow().getAttributes();
                attributes.gravity = Gravity.NO_GRAVITY;

                if (maxWidth > 0 && attributes.width < 0) {
                    attributes.width = maxWidth;
                }

                if (maxHeight > 0 && attributes.height < 0) {
                    attributes.height = maxHeight;
                }
                getDialog().getWindow().setAttributes(attributes);
            }

            reDiscover(true);
        } else {
            dismiss();
        }
    }

    private void reDiscover(boolean clearCachedDiscoveries) {
        if (BluetoothAdapter.getDefaultAdapter() != null && !BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            return;
        }
        startDiscovery(discovery, clearCachedDiscoveries);
    }

    private void startDiscovery(Discovery discovery, boolean clearCachedDiscoveries){
        discovery.startDiscovery(clearCachedDiscoveries, GattService.ZigbeeLightService, GattService.ProprietaryLightService);
    }

    @Override
    public void onPause() {
        super.onPause();
        discovery.stopDiscovery(true);
    }

    private void connect(final BluetoothDeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
        retryAttempts = 0;
        bluetoothBinding = new BlueToothService.Binding(getActivity()) {
            @Override
            protected void onBound(final BlueToothService service) {
                ((BaseActivity) getActivity()).showModalDialog(BaseActivity.ConnectionStatus.CONNECTING, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        service.clearGatt();
                    }
                });
                SelectDeviceDialog.this.service = service;
                service.connectGatt(deviceInfo.device, false, timeoutGattCallback);
            }
        };
        BlueToothService.bind(bluetoothBinding);
    }

    private void retryConnectionAttempt() {
        retryAttempts++;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        service.connectGatt(deviceInfo.device, false, timeoutGattCallback);
                    }
                }, RETRY_DELAY);
            }
        });



    }

    private Intent getIntent(BlueToothService.GattConnectType connectType, FragmentActivity activity) {
        if (connectType == BlueToothService.GattConnectType.LIGHT) {
            return (new Intent(activity, LightActivity.class));
        }
        return null;
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

    }

    @Override
    public void onAdapterEnabled() {

    }

    public static class ViewHolder extends DeviceInfoViewHolder {
        @InjectView(android.R.id.icon2)
        ImageView protocolIcon;
        @InjectView(android.R.id.icon)
        ImageView icon;
        @InjectView(android.R.id.title)
        TextView title;

        BluetoothDeviceInfo btInfo;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
        }

        @Override
        public void setData(BluetoothDeviceInfo btInfo, int position) {
            this.btInfo = btInfo;

            ScanResultCompat scanInfo = btInfo.scanInfo;

            String displayName = scanInfo.getDisplayName(false);
            title.setText(displayName);

            int rssi = Math.max(0, scanInfo.getRssi() + 80);
            icon.setImageLevel(rssi);

            if (scanInfo.getScanRecord().getServiceUuids().contains(new ParcelUuid(GattService.ZigbeeLightService.number))) {
                protocolIcon.setImageResource(R.drawable.icon_zigbee);
            } else if (scanInfo.getScanRecord().getServiceUuids().contains(new ParcelUuid(GattService.ProprietaryLightService.number))) {
                protocolIcon.setImageResource(R.drawable.icon_proprietary);
            }

            itemView.setOnClickListener(this);
        }
    }

}
