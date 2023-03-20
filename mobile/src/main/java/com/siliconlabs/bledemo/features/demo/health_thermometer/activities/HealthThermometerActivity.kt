package com.siliconlabs.bledemo.features.demo.health_thermometer.activities

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import com.siliconlabs.bledemo.home_screen.dialogs.SelectDeviceDialog
import com.siliconlabs.bledemo.bluetooth.ble.GattCharacteristic
import com.siliconlabs.bledemo.bluetooth.ble.GattService
import com.siliconlabs.bledemo.bluetooth.ble.TimeoutGattCallback
import com.siliconlabs.bledemo.features.demo.health_thermometer.models.TemperatureReading
import com.siliconlabs.bledemo.features.demo.health_thermometer.models.TemperatureReading.HtmType
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.base.activities.BaseDemoActivity
import com.siliconlabs.bledemo.utils.BLEUtils.setNotificationForCharacteristic
import com.siliconlabs.bledemo.utils.Notifications
import kotlinx.android.synthetic.main.activity_thermometer.*

@SuppressLint("MissingPermission")
class HealthThermometerActivity : BaseDemoActivity() {
    private var htmType = HtmType.UNKNOWN
    private var serviceHasBeenSet = false

    private var currentReading: TemperatureReading? = null
    private var currentType: TemperatureReading.Type? = null

    private val gattCallback: TimeoutGattCallback = object : TimeoutGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                onDeviceDisconnected()
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
                        Notifications.INDICATE)
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
                runOnUiThread {
                    setCurrentReading(reading)
                    connection_bar_text.text = getString(R.string.demo_connected_to, deviceName)
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (GattCharacteristic.fromUuid(characteristic.uuid) == GattCharacteristic.TemperatureType) {
                htmType = HtmType.values()[characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)]
                setNotificationForCharacteristic(gatt, GattService.HealthThermometer,
                        GattCharacteristic.Temperature,
                        Notifications.INDICATE)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thermometer)

        thermo_large_temperature.setFontFamily("sans-serif-thin", Typeface.NORMAL)
        type_switch.setOnCheckedChangeListener { _, isChecked -> onTabClick(isChecked) }
    }

    override fun onResume() {
        super.onResume()
        if (serviceHasBeenSet && service == null || service != null && !service?.isGattConnected(connectionAddress)!!) {
            onDeviceDisconnected()
        }
    }

    private fun onTabClick(isFahrenheitUnit: Boolean) {
        currentType = if (isFahrenheitUnit) TemperatureReading.Type.FAHRENHEIT else TemperatureReading.Type.CELSIUS
        thermo_large_temperature.setCurrentType(currentType)
    }

    fun setCurrentReading(temperatureReading: TemperatureReading?) {
        currentReading = temperatureReading
        refreshUi()
    }

    private fun refreshUi() {
        if (currentReading != null) {
            thermo_large_temperature?.setTemperature(currentReading)
            thermo_type_value_text?.text = getString(currentReading?.htmType?.nameResId!!)
            thermo_type_value?.text = getString(R.string.temperature_type,
                    getString(currentReading?.htmType?.nameResId!!))
            thermo_large_time_text?.text = currentReading?.getFormattedTime()
        }
    }

    override fun onBluetoothServiceBound() {
        serviceHasBeenSet = true
        service?.registerGattCallback(true, gattCallback)
        gatt?.discoverServices()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        //When you switch thermometers, this activity should get a new intent - at this point, we hide the dialog
        val dialog = supportFragmentManager.findFragmentByTag("select_device_tag") as SelectDeviceDialog?
        if (dialog != null && dialog.isVisible) {
            dialog.dismiss()
        }
    }
}
