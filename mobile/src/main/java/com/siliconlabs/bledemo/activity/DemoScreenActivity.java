package com.siliconlabs.bledemo.activity;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.siliconlabs.bledemo.BuildConfig;
import com.siliconlabs.bledemo.R;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


public class DemoScreenActivity extends BaseActivity {
    private Dialog helpDialog;
    private Dialog hiddenDebugDialog;
    private boolean isBluetoothAdapterEnabled = true;

    @InjectView(R.id.toolbar)
    Toolbar toolbar;
    @InjectView(R.id.help_button)
    Button helpButton;
    @InjectView(R.id.splash)
    View splash;

    @InjectView(R.id.bluetooth_enable)
    RelativeLayout bluetoothEnableBar;
    @InjectView(R.id.bluetooth_enable_msg)
    TextView bluetoothEnableMsg;
    @InjectView(R.id.bluetooth_enable_btn)
    TextView bluetoothEnableBtn;

    private final BroadcastReceiver bluetoothAdapterStateChangeListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        isBluetoothAdapterEnabled = false;
                        showEnableBluetoothAdapterBar();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        isBluetoothAdapterEnabled = false;
                        showEnableBluetoothAdapterBar();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        if (!isBluetoothAdapterEnabled) {
                            Toast.makeText(DemoScreenActivity.this, R.string.toast_bluetooth_enabled, Toast.LENGTH_SHORT).show();
                        }
                        isBluetoothAdapterEnabled = true;
                        bluetoothEnableBar.setVisibility(View.GONE);
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        isBluetoothAdapterEnabled = false;
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_screen);
        ButterKnife.inject(this);

        toolbar.setTitle(getResources().getString(R.string.title_activity_demo_screen));
        setSupportActionBar(toolbar);

        showSplash(savedInstanceState);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            isBluetoothAdapterEnabled = bluetoothAdapter.isEnabled();
        } else {
            isBluetoothAdapterEnabled = false;
        }

        if (!isBluetoothAdapterEnabled) {
            showEnableBluetoothAdapterBar();
        }

        // handle bluetooth adapter on/off state
        bluetoothEnableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeEnableBluetoothAdapterToConnecing();
            }
        });

        initHelpDialog();
        initHiddenDebugDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            isBluetoothAdapterEnabled = bluetoothAdapter.isEnabled();
        } else {
            isBluetoothAdapterEnabled = false;
        }

        if (!isBluetoothAdapterEnabled) {
            showEnableBluetoothAdapterBar();
        }

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothAdapterStateChangeListener, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(bluetoothAdapterStateChangeListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // no menu needed here
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.help_button)
    public void onHelpClick() {
        helpDialog.show();
    }

    public void initHelpDialog() {
        helpDialog = new Dialog(DemoScreenActivity.this);
        helpDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        helpDialog.setContentView(R.layout.dialog_help_demo_item);
        ((TextView) helpDialog.findViewById(R.id.dialog_help_version_text)).setText(getString(R.string.version_text,
                                                                                              BuildConfig.VERSION_NAME));
        View okButton = helpDialog.findViewById(R.id.help_ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helpDialog.dismiss();
            }
        });
    }

    public void initHiddenDebugDialog() {
        hiddenDebugDialog = new Dialog(DemoScreenActivity.this);
        hiddenDebugDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        hiddenDebugDialog.setContentView(R.layout.dialog_hidden_debug_calibration);
        View okButton = hiddenDebugDialog.findViewById(R.id.ok_btn);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hiddenDebugDialog.dismiss();
            }
        });
    }

    public void showEnableBluetoothAdapterBar() {
        bluetoothEnableMsg.setText(R.string.bluetooth_adapter_bar_disabled);
        bluetoothEnableBar.setBackgroundColor(getResources().getColor(R.color.alizarin_crimson_darker));
        bluetoothEnableBtn.setVisibility(View.VISIBLE);
        bluetoothEnableBar.setVisibility(View.VISIBLE);
        Toast.makeText(DemoScreenActivity.this, R.string.toast_bluetooth_not_enabled, Toast.LENGTH_SHORT).show();
    }

    public void changeEnableBluetoothAdapterToConnecing() {
        BluetoothAdapter.getDefaultAdapter().enable();
        bluetoothEnableBtn.setVisibility(View.GONE);
        bluetoothEnableMsg.setText(R.string.bluetooth_adapter_bar_turning_on);
        bluetoothEnableBar.setBackgroundColor(getResources().getColor(R.color.cerulean));
    }

    private void showSplash(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            splash.setVisibility(View.VISIBLE);
            splash.animate().alpha(0).setStartDelay(1000).setDuration(400).withEndAction(new Runnable() {
                @Override
                public void run() {
                    splash.setVisibility(View.GONE);
                }
            }).start();
        }
    }
}
