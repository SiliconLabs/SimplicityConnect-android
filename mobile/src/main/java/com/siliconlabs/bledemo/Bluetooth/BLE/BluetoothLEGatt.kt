package com.siliconlabs.bledemo.bluetooth.ble

import android.bluetooth.*
import android.content.Context
import android.util.Log
import android.util.SparseArray
import com.siliconlabs.bledemo.BuildConfig
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Handles the BLE communication by serializing connections, discoveries, requests and responses.
 */
open class BluetoothLEGatt internal constructor(private val context: Context, protected val device: BluetoothDevice) {
    protected var interestingServices: List<BluetoothGattService>? = null

    @Volatile
    protected var isOfInterest = false
    private val gattCallback: BluetoothGattCallback
    private val gattScanTimeout = Runnable {
        setGattServices(null)
        close()
    }

    private var scanTimeoutFuture: ScheduledFuture<*>? = null

    @get:Synchronized
    private var gatt: BluetoothGatt? = null

    private val characteristics = Characteristics()

    private fun connect() {
        var connectionAttemptFailed: Boolean
        synchronized(this) {
            if (gatt != null) {
                return
            }
            Log.e("connect()", "Gatt connect for " + device.address)
            scanTimeoutFuture = SYNCHRONIZED_GATT_ACCESS_EXECUTOR.schedule(gattScanTimeout, SCAN_CONNECTION_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
            gatt = device.connectGatt(context, false, gattCallback)
            connectionAttemptFailed = gatt == null
        }
        if (connectionAttemptFailed) {
            setGattServices(null)
        }
    }

    private fun discoverServices(): Boolean {
        val hasServicesCached: Boolean
        var discoverStartSucceeded: Boolean
        synchronized(this) {
            if (gatt == null) {
                return false
            }
            discoverStartSucceeded =  /*hasServicesCached ||*/gatt?.discoverServices()!!
        }
        if (!discoverStartSucceeded) {
            setGattServices(null)
            return false
        }
        return true
    }

    private fun displayGattServices() {
        var gattServices: List<BluetoothGattService>?
        synchronized(this) {
            if (gatt == null) {
                return
            }
            gattServices = gatt?.services
            if (gattServices == null) {
                gattServices = ArrayList()
            }
            val hasGattServices = gattServices?.isNotEmpty()!!
            if (hasGattServices) {
                characteristics.clearCharacteristics()

                // Loops through available GATT Services.
                for (gattService in gattServices!!) {
                    Log.e("displayGattServices()", "Device has service: " + getServiceName(gattService.uuid))
                    characteristics.addCharacteristics(gatt!!, gattService.characteristics)
                    Log.e("displayGattServices()", "==========================\n")
                }
            }
        }
        setGattServices(gattServices)
    }

    open fun setGattServices(services: List<BluetoothGattService>?) {
        Log.d("displayGattServices()", "setGattServices for " + device.address)

        scanTimeoutFuture?.cancel(false)
        scanTimeoutFuture = null

        if (services == null) {
            interestingServices = null
            isOfInterest = false
            return
        }

        interestingServices = ArrayList(services)

        isOfInterest = interestingServices?.isNotEmpty()!!
    }

    protected fun notifyCharacteristicChanged(characteristicID: Int, value: Any?) {}

    @Synchronized
    protected fun close() {
        if (gatt == null) {
            return
        }
        try {
            gatt?.close()
            gatt = null
        } catch (e: Exception) {
            Log.e("displayGattServices()", "Error closing Gatt: $e")
        }
    }

    fun cancel() {
        SYNCHRONIZED_GATT_ACCESS_EXECUTOR.submit { close() }
    }

    fun read(characteristicID: Int) {
        SYNCHRONIZED_GATT_ACCESS_EXECUTOR.submit {
            val value = characteristics.read(characteristicID)
            notifyCharacteristicChanged(characteristicID, value)
        }
    }

    /**
     * Handles reading of characteristic values. All its methods, except the [.onRead] method, must
     * be executed on the [.SYNCHRONIZED_GATT_ACCESS_EXECUTOR], so access to the gatt is serialized.
     */
    internal inner class Characteristics {
        val gattCharacteristics = SparseArray<BluetoothGattCharacteristic>()
        val values = SparseArray<Any?>()
        fun clearCharacteristics() {
            gattCharacteristics.clear()
            values.clear()
        }

        fun addCharacteristics(gatt: BluetoothGatt, gattCharacteristics: List<BluetoothGattCharacteristic>) {
            // Loops through available Characteristics.
            for (characteristic in gattCharacteristics) {
                if (BuildConfig.DEBUG) {
                    val characteristicName = getCharacteristicName(characteristic.uuid)
                    Log.d("addCharacteristics", " char: $characteristicName")
                }
                val characteristicID = getIdentification(characteristic.uuid)
                this.gattCharacteristics.put(characteristicID, characteristic)
                values.put(characteristicID, null)
                val properties = characteristic.properties
                if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                    gatt.setCharacteristicNotification(characteristic, true)
                }
            }
        }

        /**
         * Instructs the gatt to request the remote device to send a value of the given characteristic.
         * It waits until the gatt's callback calls [.onRead] containing the value.
         * It will not wait longer than 1000 milliseconds. If it takes longer, the current value stored in memory
         * will be returned.
         *
         * @param characteristicID ID of the characteristic whose remote value is requested.
         * @return The value of the characteristic.
         */
        fun read(characteristicID: Int): Any? {
            val gatt = gatt ?: return null
            val gattCharacteristic = gattCharacteristics[characteristicID] ?: return null
            val properties = gattCharacteristic.properties
            if (properties and BluetoothGattCharacteristic.PROPERTY_READ == 0) {
                return null
            }
            synchronized(gattCharacteristic) {
                val didRead = gatt.readCharacteristic(gattCharacteristic)
                if (didRead) {
                    try {
                        (gattCharacteristic as Object).wait(GATT_READ_TIMEOUT.toLong())
                    } catch (ignored: InterruptedException) {
                    }
                }
                return values[characteristicID]
            }
        }

        fun onRead(characteristicID: Int, value: Any?, success: Boolean) {
            val gattCharacteristic = gattCharacteristics[characteristicID]
            synchronized(gattCharacteristic) {
                if (success) {
                    values.put(characteristicID, value)
                }
                (gattCharacteristic as Object).notifyAll()
            }
        }
    }

    companion object {
        val GATT_SERVICE_DESCS = SparseArray<GattService>()
        val GATT_CHARACTER_DESCS = SparseArray<GattCharacteristic>()

        // If a gatt's service scan request is pending, cancel it after SCAN_CONNECTION_TIMEOUT milliseconds if there is no answer/response yet.
        private const val SCAN_CONNECTION_TIMEOUT = 10000

        // When reading a remote value from a gatt device, never wait longer than GATT_READ_TIMEOUT to obtain a value.
        private const val GATT_READ_TIMEOUT = 8000
        private val SYNCHRONIZED_GATT_ACCESS_EXECUTOR = Executors.newSingleThreadScheduledExecutor()
        fun cancelAll(leGatts: Collection<BluetoothLEGatt?>) {
            SYNCHRONIZED_GATT_ACCESS_EXECUTOR.submit {
                for (leGatt in leGatts) {
                    leGatt?.close()
                }
            }
        }

        private fun getServiceName(uuid: UUID): String {
            val service = GATT_SERVICE_DESCS[getIdentification(uuid)]
            return service?.type ?: uuid.toString()
        }

        private fun getCharacteristicName(uuid: UUID): String {
            val characteristic = GATT_CHARACTER_DESCS[getIdentification(uuid)]
            return characteristic?.type ?: uuid.toString()
        }

        private fun getCharacteristicsValue(gattCharacteristic: BluetoothGattCharacteristic): Any? {
            if (gattCharacteristic.value == null) {
                return null
            }
            val characteristic = GATT_CHARACTER_DESCS[getIdentification(gattCharacteristic.uuid)]
                    ?: return gattCharacteristic.value
            val format = characteristic.format
            return when (format) {
                BluetoothGattCharacteristic.FORMAT_UINT8, BluetoothGattCharacteristic.FORMAT_UINT16, BluetoothGattCharacteristic.FORMAT_UINT32, BluetoothGattCharacteristic.FORMAT_SINT8, BluetoothGattCharacteristic.FORMAT_SINT16, BluetoothGattCharacteristic.FORMAT_SINT32 -> gattCharacteristic.getIntValue(format, 0)
                BluetoothGattCharacteristic.FORMAT_FLOAT, BluetoothGattCharacteristic.FORMAT_SFLOAT -> gattCharacteristic.getFloatValue(format, 0)
                0 -> {
                    val value = gattCharacteristic.getStringValue(0)
                    val firstNullCharPos = value.indexOf('\u0000')
                    if (firstNullCharPos >= 0) value.substring(0, firstNullCharPos) else value
                }
                else -> characteristic.createValue(gattCharacteristic)
            }
        }

        private fun getIdentification(uuid: UUID): Int {
            return (uuid.mostSignificantBits ushr 32).toInt()
        }
    }

    init {
        gattCallback = object : BluetoothGattCallback() {
            // for Gatt Status codes:
            // https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/android-4.3_r1.1/stack/include/gatt_api.h
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.i("onConnectionStateChange", "failed with status: " + status + " (newState =" + newState + ") " + device.address)
                    SYNCHRONIZED_GATT_ACCESS_EXECUTOR.submit { setGattServices(null) }
                    return
                }
                Log.i("onConnectionStateChange", "(newState =" + newState + ") " + device.address)
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> SYNCHRONIZED_GATT_ACCESS_EXECUTOR.submit { discoverServices() }
                    BluetoothProfile.STATE_DISCONNECTED -> SYNCHRONIZED_GATT_ACCESS_EXECUTOR.submit { setGattServices(null) }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i("onServicesDiscovered", "onServicesDiscovered " + device.address)
                    SYNCHRONIZED_GATT_ACCESS_EXECUTOR.submit { displayGattServices() }
                } else {
                    Log.i("onServicesDiscovered", "onServicesDiscovered received: " + status + " " + device.address)
                    SYNCHRONIZED_GATT_ACCESS_EXECUTOR.submit { setGattServices(null) }
                }
            }

            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i("onCharacteristicRead", "Char " + getCharacteristicName(characteristic.uuid) + " <- " + getCharacteristicsValue(characteristic))
                    characteristics.onRead(getIdentification(characteristic.uuid), getCharacteristicsValue(characteristic), true)
                } else {
                    Log.i("onCharacteristicRead", "onCharacteristicRead received: $status")
                    characteristics.onRead(getIdentification(characteristic.uuid), null, false)
                }
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("onCharacteristcWrite", "Char " + getCharacteristicsValue(characteristic) + " -> " + getCharacteristicName(characteristic.uuid))
                } else {
                    Log.e("onCharacteristcWrite", "onCharacteristicWrite received: $status")
                }
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                notifyCharacteristicChanged(getIdentification(characteristic.uuid), getCharacteristicsValue(characteristic))
            }
        }

        /*
         * Connect --OK--> Discover --OK--> displayServices & addCharacteristics
         *         |                |
         *         |                +-Err-> reportNoServices
         *         |
         *         +-Err-> reportNoServices
         *
         * Disconnect ---> reportNoServices
         */SYNCHRONIZED_GATT_ACCESS_EXECUTOR.submit { connect() }
    }
}