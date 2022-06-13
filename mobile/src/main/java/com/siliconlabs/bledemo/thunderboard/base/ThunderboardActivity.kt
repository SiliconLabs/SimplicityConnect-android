package com.siliconlabs.bledemo.thunderboard.base

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.FrameLayout
import com.siliconlabs.bledemo.Base.BaseActivity
import com.siliconlabs.bledemo.Bluetooth.BLE.GattCharacteristic
import com.siliconlabs.bledemo.Bluetooth.BLE.GattService
import com.siliconlabs.bledemo.Bluetooth.BLE.TimeoutGattCallback
import com.siliconlabs.bledemo.Bluetooth.Services.BluetoothService
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.thunderboard.model.ThunderBoardDevice
import com.siliconlabs.bledemo.thunderboard.utils.SensorChecker
import com.siliconlabs.bledemo.utils.GattQueue
import kotlinx.android.synthetic.main.actionbar.*
import kotlinx.android.synthetic.main.activity_thunderboard_base.*

abstract class ThunderboardActivity : BaseActivity() {

    private var bluetoothBinding: BluetoothService.Binding? = null
    protected var bluetoothService: BluetoothService? = null

    protected lateinit var gattQueue: GattQueue
    protected val sensorChecker = SensorChecker()
    protected var setup = true

    protected var mainSection: FrameLayout? = null
    protected lateinit var statusFragment: StatusFragment

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thunderboard_base)
        mainSection = main_section as FrameLayout
        statusFragment = supportFragmentManager.findFragmentById(R.id.bluegecko_status_fragment) as StatusFragment

        prepareToolbar()
        registerReceiver(bluetoothStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    private fun prepareToolbar() {
        setSupportActionBar(toolbar)
        iv_go_back.setOnClickListener { onBackPressed() }
    }

    protected fun bindBluetoothService(gattCallback: TimeoutGattCallback) {
        bluetoothBinding = object : BluetoothService.Binding(this) {
            override fun onBound(service: BluetoothService?) {
                service?.let {
                    if (!it.isGattConnected()) finish()
                    else {
                        bluetoothService = it
                        statusFragment.viewModel.thunderboardDevice.value = (ThunderBoardDevice(it.connectedGatt?.device!!))
                        statusFragment.viewModel.state.postValue(BluetoothProfile.STATE_CONNECTED)
                        gattQueue = GattQueue(it.connectedGatt)
                        it.registerGattCallback(gattCallback)
                        it.discoverGattServices()
                        showModalDialog(ConnectionStatus.READING_DEVICE_STATE)
                    }
                } ?: finish()
            }
        }
        bluetoothBinding?.bind()
    }

    private val bluetoothStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action != null && action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                if (BluetoothAdapter.STATE_OFF == state) finish()
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(bluetoothStateReceiver)
        bluetoothService?.clearConnectedGatt()
        bluetoothBinding?.unbind()
        super.onDestroy()
    }

    protected fun queueReadingDeviceCharacteristics() {
        gattQueue.let {
            if (statusFragment.viewModel.thunderboardDevice.value?.name == null) {
                it.queueRead(getDeviceCharacteristic(GattService.GenericAccess, GattCharacteristic.DeviceName))
            }
            it.queueRead(getDeviceCharacteristic(GattService.DeviceInformation, GattCharacteristic.ModelNumberString))
            it.queueRead(getDeviceCharacteristic(GattService.BatteryService, GattCharacteristic.BatteryLevel))
            it.queueRead(getDeviceCharacteristic(GattService.PowerSource, GattCharacteristic.PowerSource))
            it.queueRead(getDeviceCharacteristic(GattService.DeviceInformation, GattCharacteristic.FirmwareRevision))

            it.queueNotify(getDeviceCharacteristic(GattService.BatteryService, GattCharacteristic.BatteryLevel))
        }
    }

    private fun getDeviceCharacteristic(service: GattService, characteristic: GattCharacteristic): BluetoothGattCharacteristic? {
        return bluetoothService?.connectedGatt?.getService(service.number)?.
        getCharacteristic(characteristic.uuid)
    }

}