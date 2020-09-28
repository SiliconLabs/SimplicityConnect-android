package com.siliconlabs.bledemo.activity;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;

import android.text.TextUtils;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.siliconlabs.bledemo.Base.Activities.BaseActivity;
import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.Bluetooth.BLE.BlueToothService;
import com.siliconlabs.bledemo.Bluetooth.BLE.GattCharacteristic;
import com.siliconlabs.bledemo.Bluetooth.BLE.GattService;
import com.siliconlabs.bledemo.Bluetooth.BLE.TimeoutGattCallback;
import com.siliconlabs.bledemo.fragment.SelectDeviceDialog;
import com.siliconlabs.bledemo.models.TemperatureReading;
import com.siliconlabs.bledemo.Utils.BLEUtils;

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
    private TemperatureReading.HtmType htmType = TemperatureReading.HtmType.UNKNOWN;
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
            boolean startNotificationForCharacteristicFromHere = true;
            List<BluetoothGattService> services = gatt.getServices();
            if (services != null) {
                for (BluetoothGattService s : services) {
                    if (s.getCharacteristics() != null) {
                        for (BluetoothGattCharacteristic ch : s.getCharacteristics()) {
                            if (GattCharacteristic.TemperatureType.uuid.equals(ch.getUuid())) {
                                startNotificationForCharacteristicFromHere = false;
                                gatt.readCharacteristic(ch);
                                break;
                            }

                        }
                    }
                }
            }

            if (startNotificationForCharacteristicFromHere) {
                BLEUtils.SetNotificationForCharacteristic(gatt, GattService.HealthThermometer,
                        GattCharacteristic.Temperature,
                        BLEUtils.Notifications.INDICATE);
            }

        }

        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (GattCharacteristic.fromUuid(characteristic.getUuid()) == GattCharacteristic.Temperature) {
                final TemperatureReading reading = TemperatureReading.fromCharacteristic(characteristic);
                reading.setHtmType(htmType);
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

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (GattCharacteristic.fromUuid(characteristic.getUuid()) == GattCharacteristic.TemperatureType) {
                htmType = TemperatureReading.HtmType.values()[characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)];

                BLEUtils.SetNotificationForCharacteristic(gatt, GattService.HealthThermometer,
                        GattCharacteristic.Temperature,
                        BLEUtils.Notifications.INDICATE);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thermometer);
        ButterKnife.inject(this);
        setSupportActionBar(toolbar);

        findViewById(R.id.iv_go_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

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

        if (item.getItemId() == android.R.id.home) {
            if (service != null && service.getConnectedGatt() != null)
                BLEUtils.SetNotificationForCharacteristic(service.getConnectedGatt(), GattService.HealthThermometer, GattCharacteristic.Temperature,
                        BLEUtils.Notifications.DISABLED);

            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onChangeButtonClick() {
        SelectDeviceDialog dialog = SelectDeviceDialog.newDialog(R.string.title_Health_Thermometer, R.string.main_menu_description_thermometer,
                Arrays.asList(new Pair<>(R.string.htp_title, R.string.htp_id)), BlueToothService.GattConnectType.THERMOMETER);
        dialog.show(getSupportFragmentManager(), "select_device_tag");
    }
}
