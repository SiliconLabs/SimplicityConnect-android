package com.siliconlabs.bledemo.features.demo.matter_demo.manager

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import chip.platform.BleCallback
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.ChipClient
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.coroutines.resume

class BluetoothManager : BleCallback {
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var bleGatt: BluetoothGatt? = null


    var connectionId = 0
        private set

    private fun getServiceData(discriminator: Int): ByteArray {
        val opcode = 0
        val version = 0
        val versionDiscriminator = ((version and 0xf) shl 12) or (discriminator and 0xfff)
        return intArrayOf(opcode, versionDiscriminator, versionDiscriminator shr 8)
            .map { it.toByte() }
            .toByteArray()
    }

    private fun getServiceDataMask(isShortDiscriminator: Boolean): ByteArray {
        val shortDiscriminatorMask = when (isShortDiscriminator) {
            true -> 0x00
            false -> 0xff
        }
        return intArrayOf(0xff, shortDiscriminatorMask, 0xff).map { it.toByte() }.toByteArray()
    }

    suspend fun getBluetoothDevice(context: Context, discriminator: Int): BluetoothDevice? {
        return getBluetoothDevice(context, discriminator, false)
    }

    @SuppressLint("MissingPermission")
    suspend fun getBluetoothDevice(
        context: Context,
        discriminator: Int,
        isShortDiscriminator: Boolean
    ): BluetoothDevice? {
        if (!bluetoothAdapter.isEnabled) {
            bluetoothAdapter.enable();
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner ?: run {
            Log.e(TAG, "No bluetooth scanner found")
            return null
        }


        return withTimeoutOrNull(10000) {
            callbackFlow {
                val scanCallback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        val device = result.device
                        Log.i(
                            TAG,
                            "Bluetooth Device Scanned Address: ${device.address}, Name ${device.name}"
                        )

                        val producerScope: ProducerScope<BluetoothDevice> = this@callbackFlow
                        if (producerScope.channel.isClosedForSend) {
                            Log.w(
                                TAG,
                                "Bluetooth device was scanned, but channel is already closed"
                            )
                        } else {
                            this@callbackFlow.trySend(device).isSuccess
                        }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        Log.e(TAG, "Scan failed $errorCode")
                    }
                }

                val serviceData = getServiceData(discriminator)
                val serviceDataMask = getServiceDataMask(isShortDiscriminator)

                val scanFilter =
                    ScanFilter.Builder()
                        .setServiceData(
                            ParcelUuid(UUID.fromString(CHIP_UUID)),
                            serviceData,
                            serviceDataMask
                        )
                        .build()

                val scanSettings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

                Log.i(TAG, "Starting Bluetooth scan")
                scanner.startScan(listOf(scanFilter), scanSettings, scanCallback)
                awaitClose { scanner.stopScan(scanCallback) }
            }.first()
        }
    }

    /**
     * Connects to a [BluetoothDevice] and suspends until [BluetoothGattCallback.onServicesDiscovered]
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(context: Context, device: BluetoothDevice): BluetoothGatt? {
        return suspendCancellableCoroutine { continuation ->
            val bluetoothGattCallback = getBluetoothGattCallback(context, continuation)

            Log.i(TAG, "Connecting")
            bleGatt = device.connectGatt(context, false, bluetoothGattCallback)

            connectionId =
                ChipClient.getAndroidChipPlatform(context).bleManager.addConnection(bleGatt)
            ChipClient.getAndroidChipPlatform(context).bleManager.setBleCallback(this)

            continuation.invokeOnCancellation { bleGatt?.disconnect() }
        }
    }

    private fun getBluetoothGattCallback(
        context: Context,
        continuation: CancellableContinuation<BluetoothGatt?>
    ): BluetoothGattCallback {
        return object : BluetoothGattCallback() {
            private val wrappedCallback =
                ChipClient.getAndroidChipPlatform(context).bleManager.callback;
            private val coroutineContinuation = continuation

            @SuppressLint("MissingPermission")
            override fun onConnectionStateChange(
                gatt: BluetoothGatt?,
                status: Int,
                newState: Int
            ) {
                super.onConnectionStateChange(gatt, status, newState)
                Log.i(
                    TAG,
                    "${gatt?.device?.name}.onConnectionStateChange status = $status, newState=$newState"
                )
                wrappedCallback.onConnectionStateChange(gatt, status, newState)

                if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i("$TAG|onConnectionStateChange", "Discovering Services...")
                    gatt?.discoverServices()
                }
            }

            @SuppressLint("MissingPermission")
            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                Log.d(TAG, "${gatt?.device?.name}.onServicesDiscovered status = $status")
                wrappedCallback.onServicesDiscovered(gatt, status)

                Log.i("$TAG|onServicesDiscovered", "Services Discovered")
                gatt?.requestMtu(247);
            }

            @SuppressLint("MissingPermission")
            override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
                Log.d(TAG, "${gatt?.device?.name}.onMtuChanged: connecting to CHIP device")
                super.onMtuChanged(gatt, mtu, status)
                wrappedCallback.onMtuChanged(gatt, mtu, status)
                if (coroutineContinuation.isActive) {
                    coroutineContinuation.resume(gatt)
                }
            }

            @SuppressLint("MissingPermission")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                Log.d(TAG, "${gatt.device.name}.onCharacteristicChanged: ${characteristic.uuid}")
                wrappedCallback.onCharacteristicChanged(gatt, characteristic)
            }

            @SuppressLint("MissingPermission", "LogNotTimber")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                Log.d(
                    TAG,
                    "${gatt.device.name}.onCharacteristicRead: ${characteristic.uuid} -> $status"
                )
                wrappedCallback.onCharacteristicRead(gatt, characteristic, status)
            }

            @SuppressLint("MissingPermission")
            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                Log.d(
                    TAG,
                    "${gatt.device.name}.onCharacteristicWrite: ${characteristic.uuid} -> $status"
                )
                wrappedCallback.onCharacteristicWrite(gatt, characteristic, status)
            }

            @SuppressLint("MissingPermission")
            override fun onDescriptorRead(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int
            ) {
                Log.d(TAG, "${gatt.device.name}.onDescriptorRead: ${descriptor.uuid} -> $status")
                wrappedCallback.onDescriptorRead(gatt, descriptor, status)
            }

            @SuppressLint("MissingPermission")
            override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int
            ) {
                Log.d(TAG, "${gatt.device.name}.onDescriptorWrite: ${descriptor.uuid} -> $status")
                wrappedCallback.onDescriptorWrite(gatt, descriptor, status)
            }

            @SuppressLint("MissingPermission")
            override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
                Log.d(TAG, "${gatt.device.name}.onReadRemoteRssi: $rssi -> $status")
                wrappedCallback.onReadRemoteRssi(gatt, rssi, status)
            }

            @SuppressLint("MissingPermission")
            override fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int) {
                Log.d(TAG, "${gatt.device.name}.onReliableWriteCompleted: $status")
                wrappedCallback.onReliableWriteCompleted(gatt, status)
            }
        }
    }

    companion object {
        private const val TAG = "chip.BluetoothManager"
        private const val CHIP_UUID = "0000FFF6-0000-1000-8000-00805F9B34FB"
    }

    override fun onCloseBleComplete(connId: Int) {
        connectionId = 0
        Log.d(TAG, "onCloseBleComplete")
    }

    @SuppressLint("MissingPermission")
    override fun onNotifyChipConnectionClosed(connId: Int) {
        bleGatt?.close();
        connectionId = 0
        Log.d(TAG, "onNotifyChipConnectionClosed")
    }
}