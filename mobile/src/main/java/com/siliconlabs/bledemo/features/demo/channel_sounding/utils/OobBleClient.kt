package com.siliconlabs.bledemo.features.demo.channel_sounding.utils

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
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
class OobBleClient(
    context: Context,
    private val bleConnection: BleConnection,
    private val bluetoothDevice: BluetoothDevice
) : TransportHandle {
    private val oobBleClientConnectionThread = OobBleClientConnectionThread()
    private val socketCountDownLatch = CountDownLatch(1)
    private var socket: BluetoothSocket? = null
    private var receiveCallback: TransportHandle.ReceiveCallback? = null
    private var receiveExecutor: Executor? = null

    init {
        oobBleClientConnectionThread.start()
    }

    fun waitForSocketCreation(): Boolean {
        val success = try {
            socketCountDownLatch.await(1, TimeUnit.MINUTES)
        } catch (e: InterruptedException) {
            Timber.tag(TAG).e("Failed to wait for socket $e"); false
        }
        if (!success)  Timber.tag(TAG).d("Timed out waiting for socket creation")
        return success
    }

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

    override fun close() {
        oobBleClientConnectionThread.interrupt()
    }

    inner class OobBleClientConnectionThread : Thread() {
        private val MAX_DATA_SIZE = 1024

        override fun run() {
            try {
                val psm = bleConnection.waitForPsm()
                if (psm == -1) return
                Timber.tag(TAG).d("Creating L2cap socket on $psm")
                socket = bluetoothDevice.createInsecureL2capChannel(psm)
                Timber.tag(TAG).d("Connecting on L2cap socket")
                socket?.connect()
                socketCountDownLatch.countDown()
                Timber.tag(TAG).d("Connected on L2cap socket ${socket?.remoteDevice?.address}")
            } catch (e: IOException) {
                Timber.tag(TAG).d("Failed to connect on L2cap $e")
            }
            while (!isInterrupted) {
                try {
                    val data = ByteArray(MAX_DATA_SIZE)
                    val dataSize = socket?.inputStream?.read(data) ?: -1
                    Timber.tag(TAG).d("Received data size: $dataSize")
                    if (dataSize > 0) {
                        receiveExecutor?.execute {
                            receiveCallback?.onReceiveData(Arrays.copyOf(data, dataSize))
                        }
                    }
                } catch (e: IOException) {
                    Timber.tag(TAG).e("Failed to read data $e")
                }
            }
            Timber.tag(TAG).d("Server connection thread interrupted")
            try {
                socket?.close()
            } catch (e: IOException) {
                Timber.tag(TAG).e("Failed to close socket $e")
            }
            receiveExecutor?.execute { receiveCallback?.onClose() }
        }

    }


    companion object {
        private const val TAG = "OobBleClient"
    }
}