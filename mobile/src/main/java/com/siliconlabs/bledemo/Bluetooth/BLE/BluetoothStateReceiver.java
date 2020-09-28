package com.siliconlabs.bledemo.Bluetooth.BLE;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * @author Comarch S.A.
 */
public class BluetoothStateReceiver extends BroadcastReceiver {

    private final BluetoothStateListener bluetoothStateListener;


    public BluetoothStateReceiver(BluetoothStateListener bluetoothStateListener) {
        this.bluetoothStateListener = bluetoothStateListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

            switch (state) {
                case BluetoothAdapter.ERROR:
                case BluetoothAdapter.STATE_OFF:
                case BluetoothAdapter.STATE_TURNING_OFF:
                    notifyState(false);
                    break;
                case BluetoothAdapter.STATE_ON:
                    notifyState(true);
                    break;
            }
        }
    }

    private void notifyState(boolean enabled) {
        bluetoothStateListener.onBluetoothStateChanged(enabled);
    }

    public interface BluetoothStateListener {
        void onBluetoothStateChanged(boolean enabled);
    }
}
