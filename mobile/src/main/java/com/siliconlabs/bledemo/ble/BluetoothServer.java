package com.siliconlabs.bledemo.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

import timber.log.Timber;

class BluetoothServer extends Thread {
    private final BluetoothServerSocket serverSocket;
    private volatile boolean closing;

    public BluetoothServer(BluetoothAdapter bluetoothAdapter, String serviceName, UUID serviceUUID) {
        // Use a temporary object that is later assigned to mmServerSocket,
        // because mmServerSocket is final
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code
            tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(serviceName, serviceUUID);
        } catch (IOException ignored) {
        }
        serverSocket = tmp;
    }

    public void run() {
        BluetoothSocket socket;
        // Keep listening until exception occurs or a socket is returned
        while (true) {
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                if (!closing) {
                    Log.e("BluetoothServer","Error in Bluetooth Server Thread" + e);
                    Timber.e(e, "Error in Bluetooth Server Thread");
                }
                break;
            }
            // If a connection was accepted
            if (socket != null) {
                // Do work to manage the connection (in a separate thread)
                manageConnectedSocket(socket);
                cancel();
                break;
            }
        }
    }

    /**
     * Will cancel the listening socket, and cause the thread to finish
     */
    public void cancel() {
        try {
            closing = true;
            serverSocket.close();
        } catch (IOException ignored) {
        }
    }

    @SuppressWarnings("UnusedParameters")
    private void manageConnectedSocket(BluetoothSocket socket) {
    }
}
