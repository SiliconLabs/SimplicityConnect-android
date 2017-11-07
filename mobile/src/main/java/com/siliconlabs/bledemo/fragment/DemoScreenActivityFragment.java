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
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.dialogs.Dialogs;
import com.siliconlabs.bledemo.interfaces.DemoPageLauncher;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;

import static com.siliconlabs.bledemo.fragment.DemoItemProvider.ICONS;
import static com.siliconlabs.bledemo.fragment.DemoItemProvider.LAUNCHERS;
import static com.siliconlabs.bledemo.fragment.DemoItemProvider.PROFILES;
import static com.siliconlabs.bledemo.fragment.DemoItemProvider.TEXTS;
import static com.siliconlabs.bledemo.fragment.DemoItemProvider.TITLES;

public class DemoScreenActivityFragment extends Fragment {
    DemoItemProvider demoItemProvider = new DemoItemProvider();

    private Dialog bluetoothNotSupportedDialog;
    Dialog helpDialog;
    private boolean bleIsSupported = true;
    public static final int BlUETOOTH_SETTINGS_REQUEST_CODE = 100;


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

    private final ButterKnife.Setter<View, Boolean> VISIBLE = new ButterKnife.Setter<View, Boolean>() {
        @Override
        public void set(View view, Boolean value, int index) {
            init(view, value);
        }

        private void init(final View itemView, boolean visible) {
            final int viewId = itemView.getId();
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

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (bluetoothNotEnabled()) {
                        return;
                    }

                    DemoPageLauncher launcher = LAUNCHERS.get(viewId);
                    startApplicationMode(launcher, itemView);
                }
            });
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

    private DialogFragment selectDeviceDialog;

    public DemoScreenActivityFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // handle bluetooth adapter on/off state
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        getActivity().registerReceiver(bluetoothAdapterStateChangeListener, filter);

        View view = inflater.inflate(R.layout.fragment_demo_items, container, false);

        ButterKnife.inject(this, view);
        ButterKnife.inject(demoItemProvider, view);

        ButterKnife.apply(demoItemProvider.demoItems, VISIBLE, true);

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

    private boolean bluetoothNotEnabled() {
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            bleIsSupported = false;
            bluetoothNotSupportedDialog = Dialogs.showAlert(this.getText(R.string.app_name),
                                                            this.getText(R.string.ble_not_supported),
                                                            getActivity(),
                                                            getText(android.R.string.ok),
                                                            null,
                                                            new DialogInterface.OnClickListener() {
                                                                public void onClick(final DialogInterface dialog,
                                                                                    final int id) {
                                                                    bluetoothNotSupportedDialog.dismiss();
                                                                }
                                                            },
                                                            null);
            return true;
        }
        return false;
    }

    private void startApplicationMode(DemoPageLauncher launcher, View view) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            bluetoothNotSupported();
        } else if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(getActivity(), R.string.toast_bluetooth_not_enabled, Toast.LENGTH_SHORT).show();
        } else if (bluetoothAdapter.isEnabled()) {
            selectDeviceDialog = launcher.launchPage(view, getFragmentManager());
        }
    }

    @Optional
    @OnClick(R.id.demo_help)
    public void onHelpClicked(View view) {
        helpDialog.show();
    }

    private void showDialogIfBluetoothLeNotSupported() {
        // Check if Bluetooth Low Energy technology is supported on device
        if (bluetoothNotEnabled()) {
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
