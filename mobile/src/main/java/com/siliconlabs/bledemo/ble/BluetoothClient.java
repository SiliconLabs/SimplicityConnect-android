package com.siliconlabs.bledemo.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import com.siliconlabs.bledemo.utils.IOUtils;
import timber.log.Timber;

class BluetoothClient extends Thread {
    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothSocket mSocket;
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final BluetoothDevice mDevice;

    private volatile boolean closing;
    private InputStream is;
    private OutputStream os;

    private byte[] buffer = new byte[1024];

    public BluetoothClient(BluetoothAdapter bluetoothAdapter, BluetoothDevice device, UUID serviceUUID) {
        this.bluetoothAdapter = bluetoothAdapter;

        // Use a temporary object that is later assigned to mSocket,
        // because mSocket is final
        BluetoothSocket tmp = null;
        mDevice = device;

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            tmp = device.createRfcommSocketToServiceRecord(serviceUUID);
        } catch (IOException ignored) {
        }
        mSocket = tmp;
    }

    public void run() {
        // Cancel discovery because it will slow down the connection
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            mSocket.connect();
            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mSocket);
        } catch (IOException connectException) {
            if (!closing) {
                Timber.e(connectException, "Error in Bluetooth Client Thread");
                Log.e("BluetoothClient", "Error in Bluetooth Client Thread: " + connectException);
            }
        } finally {
            IOUtils.closeStream(os);
            IOUtils.closeStream(is);
            cancel();
        }
    }

    public void write(byte[] bytes) {
        synchronized (this) {
            if (os == null) {
                return;
            }
        }

        try {
            os.write(bytes);
        } catch (IOException ignored) {
        }
    }

    /**
     * Will cancel an in-progress connection, and close the socket
     */
    public void cancel() {
        try {
            closing = true;
            mSocket.close();
        } catch (IOException ignored) {
        }
    }

    private void manageConnectedSocket(BluetoothSocket socket) throws IOException {
        synchronized (this) {
            os = socket.getOutputStream();
        }

        is = socket.getInputStream();
        //noinspection InfiniteLoopStatement
        while (true) {
            // While loop will terminate only upon an actual IOException
            // or upon a close of the socket that will also cause an IOException.
            int didRead = is.read(buffer);
            //noinspection StatementWithEmptyBody
            if (didRead > 0) {
                // TODO
            }
        }
    }
}
