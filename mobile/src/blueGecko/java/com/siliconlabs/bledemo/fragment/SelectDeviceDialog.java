package com.siliconlabs.bledemo.fragment;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.activity.BaseActivity;
import com.siliconlabs.bledemo.activity.HealthThermometerActivity;
import com.siliconlabs.bledemo.adapters.DeviceInfoViewHolder;
import com.siliconlabs.bledemo.adapters.ScannedDevicesAdapter;
import com.siliconlabs.bledemo.ble.BlueToothService;
import com.siliconlabs.bledemo.ble.BluetoothDeviceInfo;
import com.siliconlabs.bledemo.ble.Discovery;
import com.siliconlabs.bledemo.ble.GattService;
import com.siliconlabs.bledemo.ble.ScanResultCompat;
import com.siliconlabs.bledemo.ble.TimeoutGattCallback;
import com.siliconlabs.bledemo.log.TimeoutLog;
import com.siliconlabs.bledemo.utils.Constants;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Optional;

public class SelectDeviceDialog extends DialogFragment implements Discovery.BluetoothDiscoveryHost {

    private static final String TITLE_INFO = "_title_info_";
    private static final String DESC_INFO = "_desc_info_";
    private static final String PROFILES_INFO = "_profiles_info_";
    private static final String CONN_TYPE_INFO = "_conn_type_info_";

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

    @InjectView(android.R.id.list)
    RecyclerView listView;


    private GridLayoutManager layout;
    private RecyclerView.ItemDecoration itemDecoration;

    private int titleInfo;
    private int descriptionInfo;
    private List<Integer> profilesInfo;

    private ScannedDevicesAdapter adapter;

    private Discovery discovery;
    BluetoothGatt bluetoothGatt;
    private BlueToothService.GattConnectType connectType;
    private BlueToothService.Binding bluetoothBinding;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        adapter = new ScannedDevicesAdapter(new DeviceInfoViewHolder.Generator(R.layout.device_item) {
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

        }, context);

        discovery = new Discovery(adapter, this);

        adapter.setThermometerMode();

        discovery.connect(context);

        layout = new GridLayoutManager(context, context.getResources().getInteger(R.integer.device_selection_columns), LinearLayoutManager.VERTICAL, false);
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

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isEnabled() && getDialog() != null && getDialog().getWindow() != null) {
                int height = (int)(getResources().getDisplayMetrics().heightPixels * 0.50);
                getDialog().getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT,height);
                reDiscover(true);
        } else {
            dismiss();
        }
    }

    private void reDiscover(boolean clearCachedDiscoveries) {
        if (BluetoothAdapter.getDefaultAdapter() != null && !BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            return;
        }

        startDiscovery(discovery,clearCachedDiscoveries);
    }

    private void startDiscovery(Discovery discovery, boolean clearCachedDiscoveries) {
        discovery.clearFilters();
        discovery.addFilter(GattService.HealthThermometer);
        discovery.startDiscovery(clearCachedDiscoveries);
    }

    @Override
    public void onPause() {
        super.onPause();
        discovery.stopDiscovery(true);
    }

    @OnClick(R.id.button_cancel)
    public void onCancelButtonClicked(View view) {
        if(getDialog() != null) {
            getDialog().dismiss();
        }
    }

    private void connect(final BluetoothDeviceInfo deviceInfo) {
        bluetoothBinding = new BlueToothService.Binding(getActivity()) {
            @Override
            protected void onBound(final BlueToothService service) {
                ((BaseActivity) getActivity()).showModalDialog(BaseActivity.ConnectionStatus.CONNECTING, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        service.clearGatt();
                    }
                });

                service.connectGatt(deviceInfo.device, false, new TimeoutGattCallback() {

                    @Override
                    public void onTimeout() {
                        Constants.LOGS.add(new TimeoutLog(deviceInfo.device));
                        ((BaseActivity) getActivity()).dismissModalDialog();
                        Toast.makeText(getActivity(), "Connection Timed Out", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                        super.onConnectionStateChange(gatt, status, newState);
                        ((BaseActivity) getActivity()).dismissModalDialog();
                        if (newState == BluetoothGatt.STATE_CONNECTED) {
                            Intent intent = getIntent(connectType, getActivity());
                            if (intent != null) {
                                getActivity().startActivity(intent);
                            }
                        }
                    }
                });
            }
        };
        BlueToothService.bind(bluetoothBinding);
    }

    private Intent getIntent(BlueToothService.GattConnectType connectType, FragmentActivity activity) {
        if (connectType == BlueToothService.GattConnectType.THERMOMETER) {
            return (new Intent(activity, HealthThermometerActivity.class));
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
        public void setData(BluetoothDeviceInfo btInfo, int position, int size) {
            this.btInfo = btInfo;

            ScanResultCompat scanInfo = btInfo.scanInfo;

            String displayName = scanInfo.getDisplayName(false);
            title.setText(displayName);

            int rssi = Math.max(0, scanInfo.getRssi() + 80);
            icon.setImageLevel(rssi);

            itemView.setOnClickListener(this);
        }
    }

}
