package com.siliconlabs.bledemo.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.ble.BlueToothService;
import com.siliconlabs.bledemo.ble.BluetoothDeviceInfo;
import com.siliconlabs.bledemo.ble.GattCharacteristic;
import com.siliconlabs.bledemo.ble.GattService;
import com.siliconlabs.bledemo.ble.TimeoutGattCallback;
import com.siliconlabs.bledemo.interfaces.FindKeyFobCallback;
import com.siliconlabs.bledemo.utils.BLEUtils;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class KeyFobsActivity extends BaseActivity implements FindKeyFobCallback {
    public static final int MAX_EXPECTED_FOB_DELTA = 18;
    public static final int MIN_EXPECTED_FOB_DELTA = -22;
    public static final int ALERT_TRANSITION_SMOOTHING = 4;
    public static final int PROXIMIT_DELTA_THRESHOLD = -1;
    private static int MODE_SHOWING_KEYFOBS_LIST = 0;
    private static int MODE_SHOWING_KEYFOB_BLINKING = 1;
    public static final String BLINKING_FRAGMENT_ID = "BLINKING_FRAGMENT_ID";

    private int keyfobMode = MODE_SHOWING_KEYFOBS_LIST;

    private KeyFobsActivityFragment keyFobActivityFragment;
    private KeyFobsActivityFindingDeviceFragment findingDeviceFragment;
    private KeyFobsActivityFindingDeviceFragment keyFobBlinkingFragment;
    private BluetoothDeviceInfo infoOfDeviceBeingTracked;
    private BlueToothService.Binding binding;
    private BluetoothGattCharacteristic txPowerLevelCharacteristic;
    private BluetoothGattCharacteristic immediateAlertCharacteristic;
    private BluetoothGattCharacteristic linkLossAlertCharacteristic;
    private BlueToothService service;
    private Integer lastRssiReading;
    private Integer txPower;
    private AlertLevelType currentAlertLevel;
    private String currentDeviceName;
    private boolean destroying = false;

    public enum AlertLevelType {
        NONE, MILD, HIGH
    }

    @InjectView(R.id.toolbar)
    android.support.v7.widget.Toolbar toolbar;
    @InjectView(R.id.fragment_container)
    FrameLayout fragmentContainer;
    @InjectView(R.id.foreground_veil)
    View foregroundVeilConnectingDialog;

    @InjectView(R.id.bluetooth_enable)
    RelativeLayout bluetoothEnableBar;
    @InjectView(R.id.bluetooth_enable_msg)
    TextView bluetoothEnableMsg;
    @InjectView(R.id.bluetooth_enable_btn)
    TextView bluetoothEnableBtn;

    boolean isBluetoothAdapterEnabled = false;
    BluetoothGatt mostRecentConnectedGatt = null;

    private final TimeoutGattCallback gattCallback = new TimeoutGattCallback() {
        @Override
        public void onTimeout() {
            dismissModalDialog();
            Toast.makeText(KeyFobsActivity.this, "Connection Timed Out", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mostRecentConnectedGatt = gatt;
                BluetoothDevice device = gatt.getDevice();
                currentDeviceName = TextUtils.isEmpty(device.getName()) ? device.getAddress() : device.getName();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissModalDialog();
                        gatt.discoverServices();
                        switchToBlinkFragment();
                    }
                });

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissModalDialog();
                        Toast.makeText(KeyFobsActivity.this, R.string.device_has_disconnected, Toast.LENGTH_SHORT).show();
                        gatt.disconnect();
                        gatt.close();
                        currentAlertLevel = null;
                        switchToListFragment();
                    }
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            List<BluetoothGattService> services = gatt.getServices();
            for (BluetoothGattService service : services) {
                boolean success = false;
//                // Removed link loss characteristic filter, Mate's feedback
//                if (linkLossAlertCharacteristic == null) {
//                    linkLossAlertCharacteristic = BLEUtils.getCharacteristic(service, GattService.LinkLoss, GattCharacteristic.AlertLevel);
//                    success = linkLossAlertCharacteristic != null;
//                }
                if (immediateAlertCharacteristic == null && !success) {
                    immediateAlertCharacteristic = BLEUtils.getCharacteristic(service, GattService.ImmediateAlert, GattCharacteristic.AlertLevel);
                    success = immediateAlertCharacteristic != null;

                    boolean written = BLEUtils.SetNotificationForCharacteristic(gatt, GattService.HealthThermometer, GattCharacteristic.Temperature, BLEUtils.Notifications.DISABLED);
                }



//                if (txPowerLevelCharacteristic == null && !success) {
//                    txPowerLevelCharacteristic = BLEUtils.getCharacteristic(service, GattService.TxPower, GattCharacteristic.TxPowerLevel);
//                    if (txPowerLevelCharacteristic != null) {
//                        gatt.readCharacteristic(txPowerLevelCharacteristic);
//                    }
//                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
//            if (GattCharacteristic.fromUuid(characteristic.getUuid()) == GattCharacteristic.TxPowerLevel) {
//                txPower = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 0);
//            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            lastRssiReading = rssi;
            if (shouldUpdateAlertLevel()) {
                updateAlertLevel();
            }
            updateBlinkingFragment();
        }
    };

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
                        if (bluetoothEnableBar.getVisibility() == View.GONE) {
                            showEnableBluetoothAdapterBar();
                        }

                        keyFobActivityFragment.getAdapter().flushContainer();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        isBluetoothAdapterEnabled = false;
                        break;
                    case BluetoothAdapter.STATE_ON:
                        if (!isBluetoothAdapterEnabled) {
                            Toast.makeText(KeyFobsActivity.this, "Bluetooth Enabled", Toast.LENGTH_SHORT).show();
                        }
                        isBluetoothAdapterEnabled = true;

                        if (BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                            bluetoothEnableBar.setVisibility(View.GONE);
                        }
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        isBluetoothAdapterEnabled = false;
                        break;
                }
            }
        }
    };

    private void updateAlertLevel() {
        if (shouldUpdateAlertLevel()) {
            AlertLevelType type = findAlertLevel();
            updateAlertLevel(type);
        }
    }

    private void updateAlertLevel(AlertLevelType type) {
        if (immediateAlertCharacteristic != null) {
            immediateAlertCharacteristic.setValue(type.ordinal(), BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            service.writeGattCharacteristic(immediateAlertCharacteristic);
            currentAlertLevel = type;
        }
    }

    private void updateBlinkingFragment() {
        float rangeLength = MAX_EXPECTED_FOB_DELTA - MIN_EXPECTED_FOB_DELTA;
        float difference = transmitReceiveDifference() - MIN_EXPECTED_FOB_DELTA;
        if (keyFobBlinkingFragment != null) {
            keyFobBlinkingFragment.setFadeAnimationDuration((int) ((difference / rangeLength) * MILLIS));
        }
    }

    private boolean shouldUpdateAlertLevel() {
        if (findAlertLevel() == currentAlertLevel || destroying) {
            return false;
        }
        if (currentAlertLevel == null || currentAlertLevel == AlertLevelType.NONE) {
            return true;
        }
        float thresholdData = transmitReceiveDifference() - PROXIMIT_DELTA_THRESHOLD;
        if (currentAlertLevel == AlertLevelType.MILD && thresholdData <= -ALERT_TRANSITION_SMOOTHING) {
            return true;
        }
        if (currentAlertLevel == AlertLevelType.HIGH && thresholdData >= ALERT_TRANSITION_SMOOTHING) {
            return true;
        }
        return false;
    }

    private AlertLevelType findAlertLevel() {
        return transmitReceiveDifference() > PROXIMIT_DELTA_THRESHOLD ? AlertLevelType.MILD : AlertLevelType.HIGH;
    }

    private float transmitReceiveDifference() {
        //int tx = txPower != null ? txPower : -1;
        // silabs requested tx power value to be hardcoded instead of retrieved from a characteristic
        //TODO Control keyfob distance
        int tx = -50;
        int rssi = lastRssiReading != null ? lastRssiReading : -1;

        float difference = tx - rssi;
        difference = Math.max(MIN_EXPECTED_FOB_DELTA, difference);
        difference = Math.min(MAX_EXPECTED_FOB_DELTA, difference);
        return difference;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_fobs);

        ButterKnife.inject(this, this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        keyFobActivityFragment = new KeyFobsActivityFragment();
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, keyFobActivityFragment);
        fragmentTransaction.commit();
        keyfobMode = MODE_SHOWING_KEYFOBS_LIST;

        keyFobBlinkingFragment = new KeyFobsActivityFindingDeviceFragment();

        // handle bluetooth adapter on/off state
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothAdapterStateChangeListener, filter);
        bluetoothEnableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeEnableBluetoothAdapterToConnecing();
                BluetoothAdapter.getDefaultAdapter().enable();
            }
        });

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
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroying = true;
        updateAlertLevel(AlertLevelType.NONE);
        if (service != null) {
            service.clearGatt();
        }
        if (binding != null) {
            binding.unbind();
        }
        unregisterReceiver(bluetoothAdapterStateChangeListener);
        destroying = false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        getSupportFragmentManager().popBackStack(BLINKING_FRAGMENT_ID, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_key_fobs, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case android.R.id.home:
                if (keyfobMode == MODE_SHOWING_KEYFOBS_LIST) {
                    // if showing list of keyfobs, finish this activity
                    finish();
                } else if (keyfobMode == MODE_SHOWING_KEYFOB_BLINKING) {
                    // if showing blinking fragment, pop fragment manager backstack
                    keyfobMode = MODE_SHOWING_KEYFOBS_LIST;
                    getSupportFragmentManager().popBackStack();
                }
                return (true);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void findKeyFob(BluetoothDeviceInfo fob) {
        infoOfDeviceBeingTracked = fob;
        bindToBtService(fob);
        keyfobMode = MODE_SHOWING_KEYFOB_BLINKING;
    }

    @Override
    public void triggerDisconnect() {
        updateAlertLevel(AlertLevelType.NONE);
        service.clearGatt();
    }

    @Override
    public String getDeviceName() {
        return this.currentDeviceName;
    }

    private void bindToBtService(final BluetoothDeviceInfo deviceInfo) {
        if (binding != null) {
            binding.unbind();
        }
        binding = new BlueToothService.Binding(this) {
            @Override
            protected void onBound(BlueToothService service) {
                KeyFobsActivity.this.service = service;
                showModalDialog(ConnectionStatus.CONNECTING);
                service.connectGatt(deviceInfo.device, true, gattCallback);
            }
        };
        BlueToothService.bind(binding);
    }

    private void switchToListFragment() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        }
    }

    private void switchToBlinkFragment() {
        if (findingDeviceFragment == null) {
            findingDeviceFragment = new KeyFobsActivityFindingDeviceFragment();
        }
        swapFragment(findingDeviceFragment);
    }

    private void swapFragment(Fragment fragment) {
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    public void showEnableBluetoothAdapterBar() {
        bluetoothEnableMsg.setText(R.string.bluetooth_adapter_bar_disabled);
        bluetoothEnableBar.setBackgroundColor(getResources().getColor(R.color.alizarin_crimson_darker));
        bluetoothEnableBtn.setVisibility(View.VISIBLE);
        bluetoothEnableBar.setVisibility(View.VISIBLE);
        Toast.makeText(KeyFobsActivity.this, R.string.toast_bluetooth_not_enabled, Toast.LENGTH_SHORT).show();
    }

    public void changeEnableBluetoothAdapterToConnecing() {
        BluetoothAdapter.getDefaultAdapter().enable();
        bluetoothEnableBtn.setVisibility(View.GONE);
        bluetoothEnableMsg.setText(R.string.bluetooth_adapter_bar_turning_on);
        bluetoothEnableBar.setBackgroundColor(getResources().getColor(R.color.cerulean));
    }
}


