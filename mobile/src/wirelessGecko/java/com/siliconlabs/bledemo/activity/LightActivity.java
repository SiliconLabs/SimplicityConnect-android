package com.siliconlabs.bledemo.activity;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.ble.BlueToothService;
import com.siliconlabs.bledemo.ble.GattCharacteristic;
import com.siliconlabs.bledemo.ble.GattService;
import com.siliconlabs.bledemo.ble.TimeoutGattCallback;
import com.siliconlabs.bledemo.utils.BLEUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class LightActivity extends BaseActivity implements LightPresenter.BluetoothController {

    @InjectView(R.id.toolbar)
    Toolbar toolbar;
    private BlueToothService service;
    private boolean serviceHasBeenSet;
    private boolean updateDelayed;
    private BlueToothService.Binding bluetoothBinding;
    private GattService gattService;
    @Nullable
    private LightPresenter presenter;

    private static final int SOURCE_ADDRESS_LENGTH = 8;

    private boolean initSourceAddress = false;

    private final TimeoutGattCallback gattCallback = new TimeoutGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothGatt.STATE_DISCONNECTED || newState == BluetoothGatt.STATE_DISCONNECTING) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        disconnectWithModal();
                    }
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            BluetoothGattCharacteristic characteristic = getLightCharacteristic();

            if (characteristic != null) {
                boolean success = service.getConnectedGatt().readCharacteristic(characteristic);
                if (!success) {
                    disconnectWithModal();
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                onCharacteristicUpdate(characteristic);
            }
            if (GattCharacteristic.Light.uuid.equals(characteristic.getUuid())) {
                boolean success = gatt.readCharacteristic(characteristic.getService()
                                                                  .getCharacteristic(GattCharacteristic.TriggerSource.uuid));
                if (!success) {
                    disconnectWithModal();
                }
            } else if (GattCharacteristic.TriggerSource.uuid.equals(characteristic.getUuid())) {
                if (!initSourceAddress) {
                    initSourceAddress = true;
                    boolean success = gatt.readCharacteristic(characteristic.getService()
                                                                      .getCharacteristic(GattCharacteristic.SourceAddress.uuid));
                    if (!success) {
                        disconnectWithModal();
                    }
                }
            } else if (GattCharacteristic.SourceAddress.uuid.equals(characteristic.getUuid())) {
                boolean success = BLEUtils.SetNotificationForCharacteristic(gatt,
                                                                          gattService,
                                                                          GattCharacteristic.Light,
                                                                          BLEUtils.Notifications.INDICATE);
                if (!success) {
                    disconnectWithModal();
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (GattCharacteristic.Light.uuid.equals(descriptor.getCharacteristic().getUuid())) {
                boolean success = BLEUtils.SetNotificationForCharacteristic(gatt,
                                                                          gattService,
                                                                          GattCharacteristic.TriggerSource,
                                                                          BLEUtils.Notifications.INDICATE);
                if (!success) {
                    disconnectWithModal();
                }
            } else if (GattCharacteristic.TriggerSource.uuid.equals(descriptor.getCharacteristic().getUuid())) {
                boolean success = BLEUtils.SetNotificationForCharacteristic(gatt,
                                                                          gattService,
                                                                          GattCharacteristic.SourceAddress,
                                                                          BLEUtils.Notifications.INDICATE);
                if (!success) {
                    disconnectWithModal();
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (characteristic == getLightCharacteristic()) {
                Log.d("onCharacteristicWrite","" + status);
            }
        }

        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt,
                                            final BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            updateDelayed = true;
            onCharacteristicUpdate(characteristic);
        }

        private void onCharacteristicUpdate(final BluetoothGattCharacteristic characteristic) {
            GattCharacteristic gattCharacteristic = GattCharacteristic.fromUuid(characteristic.getUuid());
            if (gattCharacteristic == null) {
                return;
            }
            switch (gattCharacteristic) {
                case Light:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                            boolean isLightOn = value != 0;
                            if (presenter != null) {
                                presenter.onLightUpdated(isLightOn);
                            }
                        }
                    });
                    break;
                case TriggerSource:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                            if (presenter != null) {
                                TriggerSource triggerSource = TriggerSource.forValue(value);
                                presenter.onSourceUpdated(triggerSource);
                                if ((triggerSource != TriggerSource.BLUETOOTH) && updateDelayed)
                                {
                                    updateDelayed = false;
                                    presenter.getLightValueDelayed();
                                }
                            }
                        }
                    });
                    break;
                case SourceAddress:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String sourceAddress = "";
                            for (int i = 0; i < SOURCE_ADDRESS_LENGTH; i++) {
                                int value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, i);
                                sourceAddress = sourceAddress.concat(String.format((i < SOURCE_ADDRESS_LENGTH - 1) ? "%02x:" : "%02x", value));
                            }
                            if (presenter != null) {
                                presenter.onSourceAddressUpdated(sourceAddress);
                            }
                        }
                    });
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light);
        ButterKnife.inject(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        bluetoothBinding = new BlueToothService.Binding(this) {
            @Override
            protected void onBound(final BlueToothService service) {
                LightActivity.this.service = service;
                serviceHasBeenSet = true;
                if (!service.isGattConnected()) {
                    Toast.makeText(LightActivity.this, R.string.toast_htm_gatt_conn_failed, Toast.LENGTH_LONG).show();
                    service.clearGatt();
                    bluetoothBinding.unbind();

                    finish();
                } else {
                    service.registerGattCallback(true, gattCallback);
                    service.refreshGattServices();
                }
            }
        };
        BlueToothService.bind(bluetoothBinding);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (presenter != null) {
            presenter.cancelPeriodicReads();
        }
        if (service != null) {
            service.clearGatt();
            service.getConnectedGatt().close();
        }
        bluetoothBinding.unbind();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //get out if the service has stopped, or if the gatt connection is dead
        if ((serviceHasBeenSet && service == null) || (service != null && !service.isGattConnected())) {
            disconnectWithModal();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            BluetoothGatt gatt = service.getConnectedGatt();
            gatt.disconnect();
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void disconnectWithModal() {
        if (!isFinishing() && presenter != null) {
            presenter.showDeviceDisconnectedDialog();
        }
    }

    @Override
    public boolean setLightValue(boolean lightOn) {
        BluetoothGattCharacteristic characteristic = getLightCharacteristic();
        if (characteristic == null) {
            return false;
        }
        characteristic.setValue(lightOn ? 1 : 0, GattCharacteristic.Light.format, 0);
        return service.getConnectedGatt().writeCharacteristic(characteristic);
    }

    @Override
    public void setPresenter(@Nullable LightPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public boolean getLightValue() {
        BluetoothGattCharacteristic characteristic = getLightCharacteristic();
        return characteristic != null && service.getConnectedGatt().readCharacteristic(characteristic);
    }

    @Nullable
    private BluetoothGattCharacteristic getLightCharacteristic() {
        if (service == null) {
            return null;
        }
        if (!service.isGattConnected()) {
            return null;
        }
        BluetoothGatt gatt = service.getConnectedGatt();
        gattService = getGattService();
        if (gattService != null) {
            if (presenter != null) {
                presenter.setGattService(gattService);
            }
            return gatt.getService(gattService.number).getCharacteristic(GattCharacteristic.Light.uuid);
        }
        return null;
    }

    @Override
    public void leaveDemo() {
        finish();
    }

    private GattService getGattService() {
        BluetoothGatt gatt = service.getConnectedGatt();
        if (gatt.getService(GattService.ProprietaryLightService.number) != null) {
            return GattService.ProprietaryLightService;
        } else if (gatt.getService(GattService.ZigbeeLightService.number) != null) {
            return GattService.ZigbeeLightService;
        }
        return null;
    }
}
