package com.siliconlabs.bledemo.features.demo.channel_sounding.utils

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.ranging.oob.TransportHandle
import androidx.annotation.RequiresApi
import com.siliconlabs.bledemo.features.demo.channel_sounding.interfaces.BleConnection
import timber.log.Timber
import java.io.IOException
import java.util.Arrays
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
class OobBleServer(
    context: Context,
    private val bleConnection: BleConnection,
    private val bluetoothDevice: BluetoothDevice
) : TransportHandle {
    private val appContext = context.applicationContext
    private val bluetoothManager: BluetoothManager =
        appContext.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val socketCountDownLatch = CountDownLatch(1)
    private val serverThread = OobBleServerThread()

    @Volatile
    private var socket: BluetoothSocket? = null
    private var receiveCallback: TransportHandle.ReceiveCallback? = null
    private var receiveExecutor: Executor? = null
    override fun registerReceiveCallback(
        executor: Executor,
        callback: TransportHandle.ReceiveCallback
    ) {
        receiveExecutor = executor
        receiveCallback = callback
    }

    override fun sendData(data: ByteArray) {
        try {
            socket?.outputStream?.write(data)
        } catch (e: IOException) {
            Timber.tag(TAG).e("Failed to send data $e")
            receiveExecutor?.execute { receiveCallback?.onSendFailed() }
        }
    }

    fun waitForSocketCreation(): Boolean {
        val success = try {
            socketCountDownLatch.await(1, TimeUnit.MINUTES)
        } catch (e: InterruptedException) {
            Timber.tag(TAG).e("Failed to wait for socket $e"); false
        }
        if (!success) Timber.tag(TAG).d("Timed out waiting for socket creation")
        return success
    }

    override fun close() {
        serverThread.interrupt()
    }

    init {
        serverThread.start()
    }

    private inner class OobBleServerThread : Thread() {
        private var serverSocket: BluetoothServerSocket? = null
        private var connectionThread: OobBleServerConnectionThread? = null

        @SuppressLint("MissingPermission")
        override fun run() {
            if (appContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Timber.tag(TAG)
                    .e("BLUETOOTH_CONNECT permission not granted; aborting server thread")
                return
            }
            while (!isInterrupted) {
                try {
                    val adapter = bluetoothAdapter ?: return
                    serverSocket = adapter.listenUsingInsecureL2capChannel()
                    val psm = serverSocket?.psm ?: -1
                    Timber.tag(TAG)
                        .d("Listening on l2cap socket for ${bluetoothDevice.address} on $psm")
                    bleConnection.notifyPsm(psm)
                    val accepted = serverSocket?.accept() ?: continue
                    Timber.tag(TAG).d("Accepted connection from ${accepted.remoteDevice.address}")
                    if (bluetoothDevice == accepted.remoteDevice) {
                        connectionThread =
                            OobBleServerConnectionThread(accepted).also { it.start() }
                        socketCountDownLatch.countDown()
                    } else {
                        try {
                            accepted.close()
                        } catch (_: IOException) {
                        }
                    }
                } catch (e: IOException) {
                    Timber.tag(TAG).e("Failed to accept connection $e")
                    return
                }
            }
            Timber.tag(TAG).d("Server thread interrupted")
            connectionThread?.interrupt()
        }
    }

    private inner class OobBleServerConnectionThread(private val connSocket: BluetoothSocket) :
        Thread() {
        private val maxDataSize = 1024

        init {
            socket = connSocket
        }

        override fun run() {
            while (!isInterrupted) {
                try {
                    val data = ByteArray(maxDataSize)
                    val size = connSocket.inputStream.read(data)
                    Timber.tag(TAG).d("Received data size: $size")
                    if (size > 0) {
                        val copy = Arrays.copyOf(data, size)
                        receiveExecutor?.execute { receiveCallback?.onReceiveData(copy) }
                    }
                } catch (e: IOException) {
                    Timber.tag(TAG).e("Failed to read data $e")
                }
            }
            Timber.tag(TAG).d("Server connection thread interrupted")
            try { connSocket.close() } catch (e: IOException) { Timber.tag(TAG).d("Failed to close socket $e") }
            receiveExecutor?.execute { receiveCallback?.onClose() }
        }
    }
    companion object {
        private val TAG = OobBleServer::class.java.simpleName.toString()
    }
}