package com.siliconlabs.bledemo.fragment;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.activity.BeaconScanActivity;
import com.siliconlabs.bledemo.activity.KeyFobsActivity;
import com.siliconlabs.bledemo.activity.MainActivityDebugMode;
import com.siliconlabs.bledemo.ble.BlueToothService;
import com.siliconlabs.bledemo.dialogs.Dialogs;

import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectViews;
import butterknife.OnClick;
import butterknife.Optional;

public class DemoScreenActivityFragment extends Fragment {
    @InjectViews({R.id.demo_health_thermometer, R.id.demo_retail_beacon, R.id.demo_smart_watch, R.id.demo_key_fobs, R.id.demo_remote_keyboard, R.id.blue_giga_debug, R.id.demo_help})
    public List<View> demoItems;

    private Dialog bluetoothNotSupportedDialog;
    Dialog helpDialog;
    private boolean bleIsSupported = true;
    public static final int BlUETOOTH_SETTINGS_REQUEST_CODE = 100;

    static final SparseIntArray ICONS = new SparseIntArray();
    static final SparseIntArray TITLES = new SparseIntArray();
    static final SparseIntArray TEXTS = new SparseIntArray();
    static final SparseArray<List<Pair<Integer, Integer>>> PROFILES = new SparseArray<>();

    private final BroadcastReceiver bluetoothAdapterStateChangeListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                final String action = intent.getAction();

                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_ON:
                            // reconnect?
                            break;
                        default:
                            if (selectDeviceDialog != null) {
                                selectDeviceDialog.dismiss();
                                Toast.makeText(getActivity(), R.string.toast_bluetooth_not_enabled, Toast.LENGTH_SHORT).show();
                            }
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    static final ButterKnife.Setter<View, Boolean> VISIBLE = new ButterKnife.Setter<View, Boolean>() {
        @Override
        public void set(View view, Boolean value, int index) {
            switch (view.getId()) {
                case R.id.demo_health_thermometer:
                case R.id.demo_retail_beacon:
                case R.id.demo_key_fobs:
                    init(view, value);
                    break;
                case R.id.blue_giga_debug:
                    init(view, value);
                    break;
                case R.id.demo_help:
                    break;
                default:
                    view.setVisibility(View.GONE);
            }
        }

        private void init(View itemView, boolean visible) {
            int viewId = itemView.getId();
            ImageView iconView = ButterKnife.findById(itemView, android.R.id.icon);
            TextView titleView = ButterKnife.findById(itemView, android.R.id.title);
            TextView textView = ButterKnife.findById(itemView, android.R.id.text1);

            itemView.setVisibility(visible ? View.VISIBLE : View.GONE);

            if (visible) {
                iconView.setImageResource(ICONS.get(viewId));
                titleView.setText(TITLES.get(viewId));
                textView.setText(TEXTS.get(viewId));
            }

            ViewGroup profilesUsed = ButterKnife.findById(itemView, R.id.profiles_used);
            if ((profilesUsed != null) && (profilesUsed.getChildCount() == 0)) {
                List<Pair<Integer, Integer>> profiles = PROFILES.get(viewId);
                if (profiles != null) {
                    LayoutInflater li = LayoutInflater.from(itemView.getContext());
                    for (Pair<Integer, Integer> profile : profiles) {
                        initProfileUsed(profilesUsed, li, profile);
                    }
                }
            }
        }

        private void initProfileUsed(ViewGroup profilesUsed, LayoutInflater li, Pair<Integer, Integer> profile) {
            View valuesItem = li.inflate(R.layout.demo_item_value, profilesUsed, false);
            TextView titleView = ButterKnife.findById(valuesItem, android.R.id.text1);
            TextView idView = ButterKnife.findById(valuesItem, android.R.id.text2);

            titleView.setText(profile.first);
            idView.setText(profile.second);

            profilesUsed.addView(valuesItem);
        }
    };

    static {
        ICONS.put(R.id.demo_health_thermometer, R.drawable.home_thermometer);
        ICONS.put(R.id.demo_retail_beacon, R.drawable.home_retail_beacon);
        ICONS.put(R.id.demo_key_fobs, R.drawable.home_key_fob);
        ICONS.put(R.id.demo_smart_watch, R.drawable.home_smart_watch);
        ICONS.put(R.id.demo_remote_keyboard, R.drawable.home_remote_keyboard);
        ICONS.put(R.id.blue_giga_debug, R.drawable.home_debug_phone);

        TITLES.put(R.id.demo_health_thermometer, R.string.demo_thermometer_title);
        TITLES.put(R.id.demo_retail_beacon, R.string.demo_beacon_title);
        TITLES.put(R.id.demo_key_fobs, R.string.demo_fob_title);
        TITLES.put(R.id.blue_giga_debug, R.string.demo_item_debug_mode_title);

        TEXTS.put(R.id.demo_health_thermometer, R.string.demo_thermometer_text);
        TEXTS.put(R.id.demo_retail_beacon, R.string.demo_beacon_text);
        TEXTS.put(R.id.demo_key_fobs, R.string.demo_fob_text);
        TEXTS.put(R.id.blue_giga_debug, R.string.demo_item_debug_mode_description);

        PROFILES.put(R.id.demo_health_thermometer, Arrays.asList(new Pair<>(R.string.htp_title, R.string.htp_id)));
        PROFILES.put(R.id.demo_key_fobs, Arrays.asList(new Pair<>(R.string.fmp_title, R.string.fmp_id)));
    }

    private SelectDeviceDialog selectDeviceDialog;

    public DemoScreenActivityFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // handle bluetooth adapter on/off state
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        getActivity().registerReceiver(bluetoothAdapterStateChangeListener, filter);

        View view = inflater.inflate(R.layout.fragment_demo_items, container, false);

        ButterKnife.inject(this, view);

        ButterKnife.apply(demoItems, VISIBLE, true);

        helpDialog = new Dialog(getActivity());
        helpDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        helpDialog.setContentView(R.layout.dialog_help_demo_item);
        View okButton = helpDialog.findViewById(R.id.help_ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helpDialog.dismiss();
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
        getActivity().unregisterReceiver(bluetoothAdapterStateChangeListener);
    }

    @OnClick({R.id.demo_health_thermometer, R.id.demo_remote_keyboard})
    public void onDemoItemClicked(View view) {
        // Check if Bluetooth Low Energy technology is supported on device
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            bleIsSupported = false;
            bluetoothNotSupportedDialog = Dialogs.showAlert(this.getText(R.string.app_name), this.getText(R.string.ble_not_supported),
                    getActivity(), getText(android.R.string.ok), null, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            bluetoothNotSupportedDialog.dismiss();
                        }
                    }, null);
            return;
        }

        startApplicationMode(AppMode.HTM, view);
    }

    @OnClick(R.id.demo_retail_beacon)
    public void onBeaconClicked(View view) {
        // Check if Bluetooth Low Energy technology is supported on device
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            bleIsSupported = false;
            bluetoothNotSupportedDialog = Dialogs.showAlert(this.getText(R.string.app_name), this.getText(R.string.ble_not_supported),
                    getActivity(), getText(android.R.string.ok), null, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            bluetoothNotSupportedDialog.dismiss();
                        }
                    }, null);
            return;
        }

        startApplicationMode(AppMode.BEACON, view);
    }

    @OnClick(R.id.demo_key_fobs)
    public void onKeyFobsClicked(View view) {
        // Check if Bluetooth Low Energy technology is supported on device
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            bleIsSupported = false;
            bluetoothNotSupportedDialog = Dialogs.showAlert(this.getText(R.string.app_name), this.getText(R.string.ble_not_supported),
                    getActivity(), getText(android.R.string.ok), null, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            bluetoothNotSupportedDialog.dismiss();
                        }
                    }, null);
            return;
        }

        startApplicationMode(AppMode.KEY_FOB, view);
    }

    @OnClick(R.id.blue_giga_debug)
    public void onBlueGigaDebugClick(View view) {
        // Check if Bluetooth Low Energy technology is supported on device
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            bleIsSupported = false;
            bluetoothNotSupportedDialog = Dialogs.showAlert(this.getText(R.string.app_name), this.getText(R.string.ble_not_supported),
                    getActivity(), getText(android.R.string.ok), null, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            bluetoothNotSupportedDialog.dismiss();
                        }
                    }, null);
            return;
        }

        startApplicationMode(AppMode.DEBUG, view);
    }

    private enum AppMode {
        HTM, BEACON, KEY_FOB, DEBUG
    }

    public void startApplicationMode(AppMode appMode, View view) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            bluetoothNotSupported();
        } else if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(getActivity(), R.string.toast_bluetooth_not_enabled, Toast.LENGTH_SHORT).show();
        } else if (bluetoothAdapter.isEnabled()) {
            Intent intentAppMode = new Intent(getActivity(), MainActivityDebugMode.class);
            switch (appMode) {
                case HTM:
                    int title = TITLES.get(view.getId());
                    int description = TEXTS.get(view.getId());
                    List<Pair<Integer, Integer>> profilesInfo = PROFILES.get(view.getId());

                    BlueToothService.GattConnectType connectType = view.getId() == R.id.demo_health_thermometer ? BlueToothService.GattConnectType.THERMOMETER : null;
                    selectDeviceDialog = SelectDeviceDialog.newDialog(title, description, profilesInfo, connectType);
                    selectDeviceDialog.show(getFragmentManager(), "select_device_tag");
                    break;
                case BEACON:
                    intentAppMode = new Intent(getActivity(), BeaconScanActivity.class);
                    break;
                case KEY_FOB:
                    intentAppMode = new Intent(getActivity(), KeyFobsActivity.class);
                    break;
                case DEBUG:
                    intentAppMode = new Intent(getActivity(), MainActivityDebugMode.class);
                    break;
            }

            if (appMode != AppMode.HTM) {
                getActivity().startActivity(intentAppMode);
            }
        }
    }

    @Optional
    @OnClick(R.id.demo_help)
    public void onHelpClicked(View view) {
        helpDialog.show();
    }

    private void showDialogIfBluetoothLeNotSupported() {
        // Check if Bluetooth Low Energy technology is supported on device
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            bleIsSupported = false;
            bluetoothNotSupportedDialog = Dialogs.showAlert(this.getText(R.string.app_name), this.getText(R.string.ble_not_supported),
                    getActivity(), getText(android.R.string.ok), null, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            bluetoothNotSupportedDialog.dismiss();
                        }
                    }, null);
            return;
        }
    }

    private void bluetoothNotSupported() {
        bluetoothNotSupportedDialog = Dialogs.showAlert(getText(R.string.app_name), getText(R.string.bluetooth_not_supported), getActivity(),
                getText(android.R.string.ok), null, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().finish();
                    }
                }, null);
    }

    // Displays dialog and request user to enable Bluetooth
    private void bluetoothEnable() {
        bluetoothNotSupportedDialog = Dialogs.showAlert(getActivity().getText(R.string.no_bluetooth_dialog_title_text), getActivity()
                        .getText(R.string.no_bluetooth_dialog_text), getActivity(), getText(android.R.string.ok),
                getText(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        Intent intentBluetooth = new Intent();
                        intentBluetooth.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                        getActivity().startActivityForResult(intentBluetooth, BlUETOOTH_SETTINGS_REQUEST_CODE);
                    }
                }, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        bluetoothNotSupportedDialog.dismiss();
                    }
                });
    }
}
