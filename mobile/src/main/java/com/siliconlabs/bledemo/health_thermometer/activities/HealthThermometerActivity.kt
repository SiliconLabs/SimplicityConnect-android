package com.siliconlabs.bledemo.health_thermometer.activities

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import com.siliconlabs.bledemo.base.BaseActivity
import com.siliconlabs.bledemo.base.SelectDeviceDialog
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import com.siliconlabs.bledemo.bluetooth.ble.GattCharacteristic
import com.siliconlabs.bledemo.bluetooth.ble.GattService
import com.siliconlabs.bledemo.bluetooth.ble.TimeoutGattCallback
import com.siliconlabs.bledemo.health_thermometer.fragments.HealthThermometerActivityFragment
import com.siliconlabs.bledemo.health_thermometer.models.TemperatureReading
import com.siliconlabs.bledemo.health_thermometer.models.TemperatureReading.HtmType
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.utils.BLEUtils
import com.siliconlabs.bledemo.utils.BLEUtils.setNotificationForCharacteristic
import kotlinx.android.synthetic.main.actionbar.*

class HealthThermometerActivity : BaseActivity() {
    private lateinit var healthThermometerActivityFragment: HealthThermometerActivityFragment
    private lateinit var bluetoothBinding: BluetoothService.Binding
    private var service: BluetoothService? = null
    private var htmType = HtmType.UNKNOWN
    private var serviceHasBeenSet = false

    private val bluetoothAdapterStateChangeListener: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_OFF) finish()
            }
        }
    }

    private val gattCallback: TimeoutGattCallback = object : TimeoutGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                runOnUiThread { onDeviceDisconnect() }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            var startNotificationForCharacteristicFromHere = true
            val services = gatt.services
            if (services != null) {
                for (s in services) {
                    if (s.characteristics != null) {
                        for (ch in s.characteristics) {
                            if (GattCharacteristic.TemperatureType.uuid == ch.uuid) {
                                startNotificationForCharacteristicFromHere = false
                                gatt.readCharacteristic(ch)
                                break
                            }
                        }
                    }
                }
            }
            if (startNotificationForCharacteristicFromHere) {
                setNotificationForCharacteristic(gatt, GattService.HealthThermometer,
                        GattCharacteristic.Temperature,
                        BLEUtils.Notifications.INDICATE)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            if (GattCharacteristic.fromUuid(characteristic.uuid) == GattCharacteristic.Temperature) {
                val reading = TemperatureReading.fromCharacteristic(characteristic)
                reading.htmType = htmType
                var deviceName = gatt.device.name
                if (TextUtils.isEmpty(deviceName)) {
                    deviceName = gatt.device.address
                }
                val finalDeviceName = deviceName
                runOnUiThread {
                    healthThermometerActivityFragment.setCurrentReading(reading)
                    healthThermometerActivityFragment.setDeviceName(finalDeviceName)
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (GattCharacteristic.fromUuid(characteristic.uuid) == GattCharacteristic.TemperatureType) {
                htmType = HtmType.values()[characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)]
                setNotificationForCharacteristic(gatt, GattService.HealthThermometer,
                        GattCharacteristic.Temperature,
                        BLEUtils.Notifications.INDICATE)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thermometer)
        prepareToolbar()

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothAdapterStateChangeListener, filter)

        healthThermometerActivityFragment = supportFragmentManager.findFragmentById(R.id.fragment) as HealthThermometerActivityFragment

        bluetoothBinding = object : BluetoothService.Binding(this) {
            override fun onBound(service: BluetoothService?) {
                this@HealthThermometerActivity.service = service
                serviceHasBeenSet = true
                if (!service?.isGattConnected()!!) {
                    showMessage(R.string.toast_htm_gatt_conn_failed)
                    service.clearConnectedGatt()
                    bluetoothBinding.unbind()
                    finish()
                } else {
                    service.registerGattCallback(true, gattCallback)
                    service.discoverGattServices()
                }
            }
        }

        bluetoothBinding?.bind()
    }

    private fun prepareToolbar() {
        setSupportActionBar(toolbar)
        findViewById<View>(R.id.iv_go_back).setOnClickListener { onBackPressed() }
    }

    override fun onResume() {
        super.onResume()
        if (serviceHasBeenSet && service == null || service != null && !service?.isGattConnected()!!) {
            onDeviceDisconnect()
        }
    }

    private fun onDeviceDisconnect() {
        if (!isFinishing) {
            showMessage(R.string.device_has_disconnected)
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        //When you switch thermometers, this activity should get a new intent - at this point, we hide the dialog
        val dialog = supportFragmentManager.findFragmentByTag("select_device_tag") as SelectDeviceDialog?
        if (dialog != null && dialog.isVisible) {
            dialog.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(bluetoothAdapterStateChangeListener)
        service?.clearConnectedGatt()
        bluetoothBinding.unbind()
    }
}