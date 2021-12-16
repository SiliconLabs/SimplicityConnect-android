package com.siliconlabs.bledemo.blinky_thunderboard.activities

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import butterknife.BindView
import butterknife.ButterKnife
import com.siliconlabs.bledemo.Base.SelectDeviceDialog
import com.siliconlabs.bledemo.Bluetooth.BLE.GattCharacteristic
import com.siliconlabs.bledemo.Bluetooth.BLE.TimeoutGattCallback
import com.siliconlabs.bledemo.Bluetooth.Services.BluetoothService
import com.siliconlabs.bledemo.Bluetooth.Services.ThunderboardActivityCallback
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.blinky_thunderboard.control.ColorLEDControl
import com.siliconlabs.bledemo.blinky_thunderboard.control.ColorLEDControl.ColorLEDControlListener
import com.siliconlabs.bledemo.blinky_thunderboard.control.SwitchControl
import com.siliconlabs.bledemo.blinky_thunderboard.presenters.BlinkyThunderboardListener
import com.siliconlabs.bledemo.blinky_thunderboard.presenters.BlinkyThunderboardPresenter
import com.siliconlabs.bledemo.thunderboard.base.BaseActivity
import com.siliconlabs.bledemo.thunderboard.model.LedRGBState
import com.siliconlabs.bledemo.thunderboard.model.StatusEvent
import com.siliconlabs.bledemo.thunderboard.model.ThunderBoardDevice
import com.siliconlabs.bledemo.thunderboard.sensor.SensorBlinky
import timber.log.Timber
import java.util.*

class BlinkyThunderboardActivity : BaseActivity(),
        BlinkyThunderboardListener,
        CompoundButton.OnCheckedChangeListener,
        ColorLEDControlListener {
    var presenter: BlinkyThunderboardPresenter? = null

    @BindView(R.id.switch0)
    lateinit var switch0: SwitchControl

    @BindView(R.id.switch1)
    lateinit var switch1: SwitchControl

    @BindView(R.id.led0)
    lateinit var led0: SwitchCompat

    @BindView(R.id.led1)
    lateinit var led1: SwitchCompat

    @BindView(R.id.color_led_control)
    lateinit var colorLEDControl: ColorLEDControl

    @BindView(R.id.lightsTitle)
    lateinit var lightsTitle: TextView

    private var bluetoothBinding: BluetoothService.Binding? = null
    private var bluetoothService: BluetoothService? = null
    private var isAlreadyPrepared = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = LayoutInflater.from(this).inflate(R.layout.activity_blinky_thunderboard, null, false)
        colorLEDControl = view.findViewById(R.id.color_led_control) // make the view gone (if necessary) before it can be showed
        val powerSourceIntent = intent.getIntExtra(SelectDeviceDialog.POWER_SOURCE_EXTRA, 0)
        setPowerSource(ThunderBoardDevice.PowerSource.fromInt(powerSourceIntent))

        prepareToolbar()
        mainSection?.addView(view)
        ButterKnife.bind(this)

        presenter = BlinkyThunderboardPresenter()
        getDaggerComponent().inject(this)
        registerReceiver(bluetoothStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        bindBluetoothService()

        setInitialState()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStateReceiver)
        bluetoothService!!.clearConnectedGatt()
        bluetoothBinding!!.unbind()
        presenter?.clearViewListener()
    }

    private fun prepareToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        findViewById<View>(R.id.iv_go_back).setOnClickListener { view: View? -> onBackPressed() }
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

    private fun bindBluetoothService() {
        bluetoothBinding = object : BluetoothService.Binding(this) {
            override fun onBound(service: BluetoothService?) {
                service?.let {
                    if (!it.isGattConnected()) finish()
                    else {
                        bluetoothService = it
                        presenter?.bluetoothService = it
                        bluetoothService?.thunderboardDevice = ThunderBoardDevice(it.connectedGatt?.device!!)
                        bluetoothService?.thunderboardCallback = thunderboardActivityCallback
                        it.registerGattCallback(ioGattCallback)
                        it.discoverGattServices()
                        presenter?.loadStatusFragment(fragmentManager)
                        presenter?.showConnectionState()
                    }
                }
            }
        }
        bluetoothBinding?.bind()
    }

    var thunderboardActivityCallback = ThunderboardActivityCallback {
        if (!isAlreadyPrepared) {
            initControls()
            presenter?.prepareViewListener(this@BlinkyThunderboardActivity)
            isAlreadyPrepared = true
        }
    }

    private fun setInitialState() {
        setButton0State(STATE_NORMAL)
        setButton1State(STATE_NORMAL)
        led0.isChecked = false
        led0.setOnCheckedChangeListener(this)
        led1.isChecked = false
        led1.setOnCheckedChangeListener(this)
        colorLEDControl.setColorLEDControlListener(this)
    }


    override fun setButton0State(state: Int) {
        if (state == STATE_NORMAL) {
            switch0.setChecked(false)
        } else if (state == STATE_PRESSED) {
            switch0.setChecked(true)
        }
    }

    override fun setButton1State(state: Int) {
        if (state == STATE_NORMAL) {
            switch1.setChecked(false)
        } else if (state == STATE_PRESSED) {
            switch1.setChecked(true)
        }
    }

    /**
     * Turns the LED on or off while preserving the state of the other LED.
     *
     * @param buttonView
     * @param isChecked
     */
    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        val res = resources
        var action = 0
        if (buttonView === led0) {
            if (isChecked) action = res.getInteger(R.integer.led0_on)
            if (led1.isChecked) action = action or res.getInteger(R.integer.led1_on)
        } else if (buttonView === led1) {
            if (isChecked) action = res.getInteger(R.integer.led1_on)
            if (led0.isChecked) action = action or res.getInteger(R.integer.led0_on)
        }
        presenter?.ledAction(action)
    }

    override fun setLed0State(state: Int) {
        if (state == STATE_NORMAL) {
            led0.isChecked = false
        } else if (state == STATE_PRESSED) {
            led0.isChecked = true
        }
    }

    override fun setLed1State(state: Int) {
        if (state == STATE_NORMAL) {
            led1.isChecked = false
        } else if (state == STATE_PRESSED) {
            led1.isChecked = true
        }
    }

    override fun setColorLEDsValue(colorLEDsValue: LedRGBState) {
        // remove and reset listener to prevent repeated write commands
        colorLEDControl.setColorLEDControlListener(null)
        colorLEDControl.setColorLEDsUI(colorLEDsValue)
        colorLEDControl.setColorLEDControlListener(this)
    }

    override fun setPowerSource(powerSource: ThunderBoardDevice.PowerSource) {
        when (powerSource) {
            ThunderBoardDevice.PowerSource.COIN_CELL -> colorLEDControl.visibility = View.GONE
            else -> { }
        }
    }

    public override fun retrieveDemoPresenter() = presenter!!

    public override fun initControls() {
        runOnUiThread {
            if (presenter?.boardType != ThunderBoardDevice.Type.THUNDERBOARD_SENSE) {
                colorLEDControl.visibility = View.GONE
            }
        }
    }

    override fun updateColorLEDs(ledRGBState: LedRGBState?) {
        presenter?.setColorLEDs(ledRGBState)
    }

    private fun onDeviceDisconnect() {
        if (!this.isFinishing) {
            showMessage(R.string.device_has_disconnected)
            finish()
        }
    }

    val ioGattCallback: TimeoutGattCallback = object : TimeoutGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                onDeviceDisconnect()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status != BluetoothGatt.GATT_SUCCESS) return

            bluetoothService?.let {
                it.thunderboardDevice?.isServicesDiscovered = true
                it.readRequiredCharacteristics()
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt,
                                          characteristic: BluetoothGattCharacteristic,
                                          status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            Timber.d("onCharacteristicRead; characteristic = ${characteristic.uuid}, status = $status")
            Timber.d("Raw data = ${Arrays.toString(characteristic.value)}")
            if (status != BluetoothGatt.GATT_SUCCESS) return

            val device = bluetoothService?.thunderboardDevice
            val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid)

            when (gattCharacteristic) {
                GattCharacteristic.DeviceName -> device?.name = characteristic.getStringValue(0)
                GattCharacteristic.ModelNumberString -> device?.modelNumber = characteristic.getStringValue(0)
                GattCharacteristic.BatteryLevel -> {
                    device?.let {
                        it.batteryLevel = characteristic.getIntValue(gattCharacteristic.format, 0)
                        it.isBatteryConfigured = true
                    }
                }
                GattCharacteristic.PowerSource -> {
                    device?.let {
                        val powerSource = ThunderBoardDevice.PowerSource.fromInt(
                                characteristic.getIntValue(gattCharacteristic.format, 0))
                        it.powerSource = powerSource
                        it.isPowerSourceConfigured = true
                    }
                    bluetoothService?.let {
                        it.selectedDeviceStatusMonitor.onNext(StatusEvent(device))
                        it.selectedDeviceMonitor.onNext(device)
                    }
                }
                GattCharacteristic.FirmwareRevision -> {
                    device?.firmwareVersion = characteristic.getStringValue(0)
                    bluetoothService?.let {
                        it.selectedDeviceStatusMonitor.onNext(StatusEvent(device))
                        it.selectedDeviceMonitor.onNext(device)
                    }
                }
                GattCharacteristic.Digital -> {
                    device?.sensorBlinky ?: SensorBlinky().let {
                        device?.sensorBlinky = it
                        it.setLed(characteristic.value[0])
                        it.isSensorDataChanged = true
                    }
                    bluetoothService?.selectedDeviceMonitor?.onNext(device)
                }
                GattCharacteristic.RgbLeds -> {
                    val on = characteristic.getIntValue(gattCharacteristic.format, 0)
                    val red = characteristic.getIntValue(gattCharacteristic.format, 1)
                    val green = characteristic.getIntValue(gattCharacteristic.format, 2)
                    val blue = characteristic.getIntValue(gattCharacteristic.format, 3)
                    val ledState = LedRGBState(
                            on != null && on != 0,
                            red ?: 0,
                            green ?: 0,
                            blue ?: 0

                    )
                    val sensor = device?.sensorBlinky ?: SensorBlinky()
                    device?.sensorBlinky = sensor
                    device?.sensorBlinky?.colorLed = ledState
                    bluetoothService?.selectedDeviceMonitor?.onNext(device)
                }
                else -> { }
            }

            bluetoothService?.readRequiredCharacteristics()
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt,
                                           characteristic: BluetoothGattCharacteristic,
                                           status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Timber.d("onCharacteristicWrite; characteristic = ${characteristic.uuid}, status = $status")
            Timber.d("Raw data = ${Arrays.toString(characteristic.value)}")
            if (status != BluetoothGatt.GATT_SUCCESS) return

            if (characteristic.uuid == GattCharacteristic.Digital.uuid) {
                bluetoothService?.let {
                    it.thunderboardDevice?.sensorBlinky?.setLed(characteristic.value[0])
                    it.selectedDeviceMonitor?.onNext(it.thunderboardDevice)
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt,
                                             characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            Timber.d("onCharacteristicChanged; characteristic = ${characteristic.uuid}")
            Timber.d("Raw value = ${Arrays.toString(characteristic.value)}")

            val device = bluetoothService?.thunderboardDevice
            val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid)

            when (gattCharacteristic) {
                GattCharacteristic.BatteryLevel -> {
                    device?.let {
                        it.batteryLevel = characteristic.getIntValue(gattCharacteristic.format, 0)
                        it.isBatteryConfigured = true
                    }
                    bluetoothService?.selectedDeviceStatusMonitor?.onNext(StatusEvent(device))
                }
                GattCharacteristic.PowerSource -> {
                    device?.let {
                        val powerSource = ThunderBoardDevice.PowerSource.fromInt(
                                characteristic.getIntValue(gattCharacteristic.format, 0))
                        it.powerSource = powerSource
                        it.isPowerSourceConfigured = true
                    }
                    bluetoothService?.selectedDeviceStatusMonitor?.onNext(StatusEvent(device))
                }
                GattCharacteristic.Digital -> {
                    device?.sensorBlinky?.let {
                        it.isSensorDataChanged = true
                        it.setSwitch(characteristic.value[0])
                    }
                    bluetoothService?.selectedDeviceMonitor?.onNext(device)
                }
                else -> { }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor,
                                       status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            Timber.d("onDescriptorWrite; descriptor = ${descriptor.uuid}, status = $status")
            Timber.d("Raw data = ${Arrays.toString(descriptor.value)}")
            if (status != BluetoothGatt.GATT_SUCCESS) return

            val device = bluetoothService?.thunderboardDevice
            val characteristicUuid = descriptor.characteristic.uuid
            Timber.d("descriptor for characteristic: $characteristicUuid")

            when (characteristicUuid) {
                GattCharacteristic.BatteryLevel.uuid -> {
                    device?.isBatteryNotificationEnabled = true
                    bluetoothService?.let {
                        it.readRequiredCharacteristics()
                        it.selectedDeviceMonitor?.onNext(device)
                    }
                }
                GattCharacteristic.PowerSource.uuid -> {
                    device?.isPowerSourceNotificationEnabled = true
                    bluetoothService?.let {
                        it.readRequiredCharacteristics()
                        it.selectedDeviceMonitor?.onNext(device)
                    }
                }
                GattCharacteristic.Digital.uuid -> {
                    val sensor = device?.sensorBlinky ?: SensorBlinky()
                    device?.sensorBlinky = sensor
                    device?.sensorBlinky?.isNotificationEnabled = true
                    bluetoothService?.selectedDeviceMonitor?.onNext(device)
                }
                else -> { }
            }
        }
    }

    companion object {
        private const val STATE_NORMAL = 0
        private const val STATE_PRESSED = 1
    }
}