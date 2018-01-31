package com.siliconlabs.bledemo.activity;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Pair;
import android.view.MenuItem;
import android.widget.Toast;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.ble.BlueToothService;
import com.siliconlabs.bledemo.ble.GattCharacteristic;
import com.siliconlabs.bledemo.ble.GattService;
import com.siliconlabs.bledemo.ble.TimeoutGattCallback;
import com.siliconlabs.bledemo.fragment.SelectDeviceDialog;
import com.siliconlabs.bledemo.models.TemperatureReading;
import com.siliconlabs.bledemo.utils.BLEUtils;

import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class HealthThermometerActivity extends BaseActivity {
    private BlueToothService service;
    private boolean serviceHasBeenSet;
    private BlueToothService.Binding bluetoothBinding;
    private HealthThermometerActivityFragment healthThermometerActivityFragment;
    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    private final TimeoutGattCallback gattCallback = new TimeoutGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onDeviceDisconnect();
                    }
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            List<BluetoothGattService> services = gatt.getServices();

            BLEUtils.SetNotificationForCharacteristic(gatt, GattService.HealthThermometer, GattCharacteristic.Temperature, BLEUtils.Notifications.INDICATE);
        }

        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (GattCharacteristic.fromUuid(characteristic.getUuid()) == GattCharacteristic.Temperature) {
                final TemperatureReading reading = TemperatureReading.fromCharacteristic(characteristic);
                String deviceName = gatt.getDevice().getName();
                if (TextUtils.isEmpty(deviceName)) {
                    deviceName = gatt.getDevice().getAddress();
                }
                final String finalDeviceName = deviceName;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        healthThermometerActivityFragment.setCurrentReading(reading);
                        healthThermometerActivityFragment.setDeviceName(finalDeviceName);
                    }
                });
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_thermometer);
        ButterKnife.inject(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        healthThermometerActivityFragment = (HealthThermometerActivityFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        bluetoothBinding = new BlueToothService.Binding(this) {
            @Override
            protected void onBound(final BlueToothService service) {
                HealthThermometerActivity.this.service = service;
                serviceHasBeenSet = true;
                if (!service.isGattConnected()) {
                    Toast.makeText(HealthThermometerActivity.this, R.string.toast_htm_gatt_conn_failed, Toast.LENGTH_LONG).show();

                    if (service != null) {
                        service.clearGatt();
                    }
                    bluetoothBinding.unbind();

                    finish();
                } else {
                    service.registerGattCallback(true, gattCallback);
                    service.discoverGattServices();
                }
            }
        };
        BlueToothService.bind(bluetoothBinding);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //get out if the service has stopped, or if the gatt connection is dead
        if ((serviceHasBeenSet && service == null) || (service != null && !service.isGattConnected())) {
            onDeviceDisconnect();
        }
    }

    private void onDeviceDisconnect() {
        if (!isFinishing()) {
            Toast.makeText(HealthThermometerActivity.this, R.string.device_has_disconnected, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //When you switch thermometers, this activity should get a new intent - at this point, we hide the dialog
        SelectDeviceDialog dialog = (SelectDeviceDialog) getSupportFragmentManager().findFragmentByTag("select_device_tag");
        if (dialog != null && dialog.isVisible()) {
            dialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (service != null) {
            service.clearGatt();
        }
        bluetoothBinding.unbind();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_change_bt) {
            onChangeButtonClick();
            return true;
        }

        switch (item.getItemId()) {
            case android.R.id.home:
                if (service != null && service.getConnectedGatt() != null)
                    BLEUtils.SetNotificationForCharacteristic(service.getConnectedGatt(), GattService.HealthThermometer, GattCharacteristic.Temperature,
                                                              BLEUtils.Notifications.DISABLED);
                
                finish();
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onChangeButtonClick() {
        SelectDeviceDialog dialog = SelectDeviceDialog.newDialog(R.string.demo_thermometer_title, R.string.demo_thermometer_text,
                Arrays.asList(new Pair<>(R.string.htp_title, R.string.htp_id)), BlueToothService.GattConnectType.THERMOMETER);
        dialog.show(getSupportFragmentManager(), "select_device_tag");
    }
}
